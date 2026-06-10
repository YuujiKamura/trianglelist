// TriangleList Web 段階1 — Canvas 2D シェル (insight #61)。
// 座標計算・寸法配置・番号サークル位置は全部 Kotlin (wasmJs) 側で済んでいて、
// ここは JSON プリミティブを素朴に描くだけ。
// y 軸はモデル座標 (上向き) → 画面座標 (下向き) に反転する (MyView.makePath の -y と同じ向き)。

type LinePrim = { type: 'line'; layer: string; x1: number; y1: number; x2: number; y2: number };
type TextPrim = {
  type: 'text';
  layer: string;
  text: string;
  x: number;
  y: number;
  angle: number; // 度数法、モデル座標系で CCW (PointXY.calcDimAngle 由来)
  size: number; // モデル単位の文字高さ
  align: number; // DXF 垂直コード: 1=点が文字の下端, 2=中央, 3=点が文字の上端
};
type CirclePrim = { type: 'circle'; layer: string; cx: number; cy: number; r: number };
type Prim = LinePrim | TextPrim | CirclePrim;

// Kotlin wasmJs の ESM glue を静的 import してバンドルに乗せる (Vite の正規 module graph)。
// .wasm 本体は uninstantiated.mjs が fetch('./TriangleList-common-wasm-js.wasm') =
// document base 相対で読むため、sync-wasm が web/public/ 直下に置く
import { renderCsvToPrimitives, buildDxfText, buildSfcText } from '../wasm/TriangleList-common-wasm-js.mjs';
// DXF/SFC は既存 app の出力と同じ Shift_JIS。ブラウザ標準 TextEncoder は UTF-8 専用なので
// encoding-japanese (MIT, polygonplanet/encoding.js) で Unicode → SJIS バイト化する
import Encoding from 'encoding-japanese';

// 内蔵サンプル (desktop/sample/sample_triangles.csv と同形式)
const SAMPLE_CSV = `テスト工事
適当路線
適当業者
T-001
1,6.0,5.0,4.0,-1,-1
2,5.0,4.0,3.0,1,1
3,4.0,3.5,3.0,1,2
4,3.5,3.0,3.2,2,1
5,3.0,2.8,3.0,3,2
6,3.0,2.5,2.5,4,2
7,2.8,2.2,2.5,5,1
`;

const COLORS: Record<string, string> = {
  tri: '#222222',
  dim: '#1a7a1a',
  num: '#1f5fbf',
};

function setStatus(msg: string): void {
  const el = document.getElementById('status');
  if (el) el.textContent = msg;
}

function bounds(prims: Prim[]): { minX: number; minY: number; maxX: number; maxY: number } {
  let minX = Infinity;
  let minY = Infinity;
  let maxX = -Infinity;
  let maxY = -Infinity;
  const acc = (x: number, y: number) => {
    if (x < minX) minX = x;
    if (y < minY) minY = y;
    if (x > maxX) maxX = x;
    if (y > maxY) maxY = y;
  };
  for (const p of prims) {
    if (p.type === 'line') {
      acc(p.x1, p.y1);
      acc(p.x2, p.y2);
    } else if (p.type === 'circle') {
      acc(p.cx - p.r, p.cy - p.r);
      acc(p.cx + p.r, p.cy + p.r);
    } else {
      acc(p.x, p.y);
    }
  }
  if (!isFinite(minX)) {
    minX = 0; minY = 0; maxX = 1; maxY = 1;
  }
  return { minX, minY, maxX, maxY };
}

function draw(canvas: HTMLCanvasElement, prims: Prim[]): void {
  const ctx = canvas.getContext('2d');
  if (!ctx) return;
  ctx.clearRect(0, 0, canvas.width, canvas.height);

  const b = bounds(prims);
  const bw = Math.max(b.maxX - b.minX, 1e-6);
  const bh = Math.max(b.maxY - b.minY, 1e-6);
  const margin = 40;
  const s = Math.min((canvas.width - margin * 2) / bw, (canvas.height - margin * 2) / bh);
  const offX = (canvas.width - bw * s) / 2;
  const offY = (canvas.height - bh * s) / 2;

  // モデル (y 上向き) → 画面 (y 下向き)。MyView.makePath の y 反転と同じ向き
  const sx = (x: number) => (x - b.minX) * s + offX;
  const sy = (y: number) => (b.maxY - y) * s + offY;

  for (const p of prims) {
    const color = COLORS[p.layer] ?? '#000000';
    if (p.type === 'line') {
      ctx.strokeStyle = color;
      ctx.lineWidth = p.layer === 'tri' ? 1.5 : 1;
      ctx.beginPath();
      ctx.moveTo(sx(p.x1), sy(p.y1));
      ctx.lineTo(sx(p.x2), sy(p.y2));
      ctx.stroke();
    } else if (p.type === 'circle') {
      ctx.strokeStyle = color;
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.arc(sx(p.cx), sy(p.cy), p.r * s, 0, Math.PI * 2);
      ctx.stroke();
    } else {
      ctx.fillStyle = color;
      ctx.font = `${Math.max(p.size * s, 8)}px sans-serif`;
      ctx.textAlign = 'center';
      // DXF 垂直コード → canvas baseline。y 反転後も「1=文字が点の上に乗る」を保つ
      ctx.textBaseline = p.align === 1 ? 'bottom' : p.align === 3 ? 'top' : 'middle';
      ctx.save();
      ctx.translate(sx(p.x), sy(p.y));
      // モデル座標 CCW の角度は、y 反転した画面では時計回りに掛ける
      ctx.rotate((-p.angle * Math.PI) / 180);
      ctx.fillText(p.text, 0, 0);
      ctx.restore();
    }
  }
}

function renderCsv(canvas: HTMLCanvasElement, csv: string, label: string): void {
  try {
    const json = renderCsvToPrimitives(csv, 1.0);
    const prims = JSON.parse(json) as Prim[];
    draw(canvas, prims);
    setStatus(`${label}: ${prims.length} primitives`);
  } catch (e) {
    setStatus(`render error: ${String(e)}`);
    console.error(e);
  }
}

// ---- 段階2a: 表編集 UI ----
// 状態の真実は「行の配列」。編集のたび CSV へ直列化して renderCsv() に流す。
// 接続・座標計算は common (WebCsvReader/WebPrimitiveRenderer) 側 — TS は表⇔CSV の糊だけ。

type Row = {
  a: string;
  b: string;
  c: string;
  parent: string;
  conn: string;
  extras: string[]; // 7列目以降 (測点名等) を生のまま保持して round-trip で落とさない
};

// WebCsvReader が三角形行として読まない行 (ヘッダ・Deduction 等) は
// 原文のまま保持し、直列化時に先頭へ戻す (どこにあっても reader は skip するので描画不変)
let headerLines: string[] = [];
let rows: Row[] = [];

// Kotlin の toIntOrNull と同じ判定 (parseInt は '1.5'→1 になるので regex で厳密に)
function intOrNull(s: string): number | null {
  return /^[+-]?\d+$/.test(s) ? parseInt(s, 10) : null;
}

function parseCsvToState(csv: string): void {
  headerLines = [];
  rows = [];
  for (const line of csv.split(/\r?\n/)) {
    if (line.trim() === '') continue;
    const chunks = line.split(',').map((s) => s.trim());
    const num = chunks.length >= 4 ? intOrNull(chunks[0]) : null;
    if (num === null || num < 0) {
      headerLines.push(line);
      continue;
    }
    rows.push({
      a: chunks[1] ?? '',
      b: chunks[2] ?? '',
      c: chunks[3] ?? '',
      parent: chunks[4] ?? '-1',
      conn: chunks[5] ?? '-1',
      extras: chunks.slice(6),
    });
  }
}

function serializeState(): string {
  const lines = [...headerLines];
  rows.forEach((r, i) => {
    lines.push([String(i + 1), r.a, r.b, r.c, r.parent, r.conn, ...r.extras].join(','));
  });
  return lines.join('\n') + '\n';
}

function redraw(canvas: HTMLCanvasElement): void {
  renderCsv(canvas, serializeState(), `table (${rows.length} rows)`);
}

const CONN_OPTIONS: Array<[string, string]> = [
  ['-1', '独立'],
  ['1', '親のB辺'],
  ['2', '親のC辺'],
];

function numberInput(value: string, step: string, onInput: (v: string) => void): HTMLInputElement {
  const input = document.createElement('input');
  input.type = 'number';
  input.step = step;
  input.value = value;
  input.addEventListener('input', () => onInput(input.value));
  return input;
}

function buildTable(canvas: HTMLCanvasElement): void {
  const tbody = document.getElementById('triRows');
  if (!tbody) return;
  tbody.textContent = '';

  rows.forEach((row, i) => {
    const tr = document.createElement('tr');

    const tdNum = document.createElement('td');
    tdNum.className = 'num';
    tdNum.textContent = String(i + 1); // 自動採番、read-only
    tr.appendChild(tdNum);

    for (const key of ['a', 'b', 'c'] as const) {
      const td = document.createElement('td');
      td.appendChild(
        numberInput(row[key], '0.1', (v) => {
          row[key] = v;
          redraw(canvas);
        }),
      );
      tr.appendChild(td);
    }

    const tdParent = document.createElement('td');
    tdParent.appendChild(
      numberInput(row.parent, '1', (v) => {
        row.parent = v;
        redraw(canvas);
      }),
    );
    tr.appendChild(tdParent);

    const tdConn = document.createElement('td');
    const select = document.createElement('select');
    for (const [value, label] of CONN_OPTIONS) {
      const opt = document.createElement('option');
      opt.value = value;
      opt.textContent = label;
      select.appendChild(opt);
    }
    if (!CONN_OPTIONS.some(([v]) => v === row.conn)) {
      // 既存 CSV に -1/1/2 以外 (例: 0) が来ても値を壊さず保持する
      const opt = document.createElement('option');
      opt.value = row.conn;
      opt.textContent = row.conn;
      select.appendChild(opt);
    }
    select.value = row.conn;
    select.addEventListener('change', () => {
      row.conn = select.value;
      redraw(canvas);
    });
    tdConn.appendChild(select);
    tr.appendChild(tdConn);

    const tdDel = document.createElement('td');
    const del = document.createElement('button');
    del.type = 'button';
    del.className = 'del';
    del.textContent = '削除';
    del.addEventListener('click', () => {
      rows.splice(i, 1);
      buildTable(canvas); // 番号が振り直されるので組み直し
      redraw(canvas);
    });
    tdDel.appendChild(del);
    tr.appendChild(tdDel);

    tbody.appendChild(tr);
  });
}

function addRow(canvas: HTMLCanvasElement): void {
  if (rows.length === 0) {
    rows.push({ a: '3.0', b: '3.0', c: '3.0', parent: '-1', conn: '-1', extras: [] });
  } else {
    // デフォルトは「直前の行の B 辺に接続」。辺A は親の接続先辺長と一致させる仕様
    // (CLAUDE.md 三角形接続の仕様) なので、親行の B 値を写すだけ — 計算はしない
    const parent = rows[rows.length - 1];
    rows.push({ a: parent.b, b: '3.0', c: '3.0', parent: String(rows.length), conn: '1', extras: [] });
  }
  buildTable(canvas);
  redraw(canvas);
}

// ---- 段階2b: 書き出し配線 (CSV 保存 + DXF/SFC ダウンロード) ----
// 図面の組み立ては common (WebDrawingExport、golden 同値テストで app と同一出力を固定済み)。
// TS は「文字列を受けてバイト化してダウンロード」の糊だけ。

function downloadBlob(filename: string, blob: Blob): void {
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = filename;
  a.click();
  URL.revokeObjectURL(a.href);
}

function toSjisBlob(text: string): Blob {
  const sjis = Encoding.convert(Encoding.stringToCode(text), { to: 'SJIS', from: 'UNICODE' });
  return new Blob([new Uint8Array(sjis)], { type: 'application/octet-stream' });
}

function exportFile(label: string, filename: string, build: () => Blob): void {
  try {
    downloadBlob(filename, build());
    setStatus(`${label} saved: ${filename}`);
  } catch (e) {
    setStatus(`${label} error: ${String(e)}`);
    console.error(e);
  }
}

function wireExportButtons(): void {
  // CSV は UTF-8 のまま (読込側 FileReader.readAsText の UTF-8 想定と対称)
  document.getElementById('saveCsv')?.addEventListener('click', () => {
    exportFile('CSV', 'triangles.csv', () => new Blob([serializeState()], { type: 'text/csv' }));
  });
  document.getElementById('saveDxf')?.addEventListener('click', () => {
    exportFile('DXF', 'triangles.dxf', () => toSjisBlob(buildDxfText(serializeState())));
  });
  document.getElementById('saveSfc')?.addEventListener('click', () => {
    exportFile('SFC', 'triangles.sfc', () => toSjisBlob(buildSfcText(serializeState(), 'triangles.sfc')));
  });
}

function loadCsv(canvas: HTMLCanvasElement, csv: string, label: string): void {
  parseCsvToState(csv);
  buildTable(canvas);
  renderCsv(canvas, serializeState(), label);
}

function main(): void {
  const canvas = document.getElementById('cv') as HTMLCanvasElement | null;
  const fileInput = document.getElementById('file') as HTMLInputElement | null;
  const addBtn = document.getElementById('addRow');
  if (!canvas || !fileInput) return;

  // デフォルトで内蔵サンプルを表に展開して描画
  loadCsv(canvas, SAMPLE_CSV, 'sample');

  addBtn?.addEventListener('click', () => addRow(canvas));
  wireExportButtons();

  fileInput.addEventListener('change', () => {
    const file = fileInput.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      // 注: 既存 app の CSV は Shift_JIS の場合があるが、段階1 同様 UTF-8 読みのみ
      loadCsv(canvas, String(reader.result ?? ''), file.name);
    };
    reader.readAsText(file);
  });
}

main();
