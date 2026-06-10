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
import {
  renderCsvToPrimitives,
  buildDxfText,
  buildSfcText,
  hitTriangle,
} from '../wasm/TriangleList-common-wasm-js.mjs';
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

const SELECT_FILL = 'rgba(31, 95, 191, 0.25)'; // 選択三角形の塗り (半透明青)

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

// ---- 段階2c: ズーム・パン + タップ選択 ----
// 変換は ViewTransform 1 つに集約し、draw (モデル→画面) と hit (画面→モデル) の
// 両方が同じものを使う (逆関数の二重実装で食い違うのが典型バグ、brief 設計)。
//   px = x * scale + offsetX
//   py = -y * scale + offsetY   (y 反転込み)
type ViewTransform = { scale: number; offsetX: number; offsetY: number };

let view: ViewTransform | null = null; // null = 次の draw で bounds-fit を計算し直す
let lastPrims: Prim[] = []; // ズーム・パン時に wasm を呼び直さず再描画するためのキャッシュ
let selected = 0; // 選択中の三角形番号 (1-based、0 = 非選択)

function fitTransform(canvas: HTMLCanvasElement, prims: Prim[]): ViewTransform {
  const b = bounds(prims);
  const bw = Math.max(b.maxX - b.minX, 1e-6);
  const bh = Math.max(b.maxY - b.minY, 1e-6);
  const margin = 40;
  const s = Math.min((canvas.width - margin * 2) / bw, (canvas.height - margin * 2) / bh);
  // 旧 fit 描画 (sx = (x-minX)*s + offX, sy = (maxY-y)*s + offY) と同じ絵になる offset
  return {
    scale: s,
    offsetX: (canvas.width - bw * s) / 2 - b.minX * s,
    offsetY: (canvas.height - bh * s) / 2 + b.maxY * s,
  };
}

function toModel(v: ViewTransform, px: number, py: number): { x: number; y: number } {
  return { x: (px - v.offsetX) / v.scale, y: (v.offsetY - py) / v.scale };
}

function zoomAt(px: number, py: number, factor: number): void {
  const v = view;
  if (!v) return;
  // 倍率は fit の 1/100〜1000 倍相当で十分。極端な値で float が壊れるのだけ防ぐ
  const next = Math.min(Math.max(v.scale * factor, 1e-3), 1e6);
  const f = next / v.scale;
  v.scale = next;
  // カーソル位置 (px,py) を不動点に: モデル点が同じ画面位置に留まるよう offset を補正
  v.offsetX = px - (px - v.offsetX) * f;
  v.offsetY = py - (py - v.offsetY) * f;
}

function draw(canvas: HTMLCanvasElement, prims: Prim[]): void {
  const ctx = canvas.getContext('2d');
  if (!ctx) return;
  ctx.clearRect(0, 0, canvas.width, canvas.height);

  if (!view) view = fitTransform(canvas, prims);
  const v = view;
  const s = v.scale;
  const sx = (x: number) => x * s + v.offsetX;
  const sy = (y: number) => -y * s + v.offsetY; // モデル (y 上向き) → 画面 (y 下向き)

  // 選択三角形の塗り (線より下に敷く)。WebPrimitiveRenderer.render は三角形ごとに
  // 'tri' layer の line を 3 本連続で出す (WebPrimitiveRenderer.kt:62-65) ので、
  // 番号 n の頂点は tri 線群の [3(n-1), 3n) から拾える — 表示専用の導出で幾何判定はしない
  if (selected > 0) {
    const triLines = prims.filter((p): p is LinePrim => p.type === 'line' && p.layer === 'tri');
    const base = (selected - 1) * 3;
    if (base + 2 < triLines.length) {
      ctx.fillStyle = SELECT_FILL;
      ctx.beginPath();
      ctx.moveTo(sx(triLines[base].x1), sy(triLines[base].y1));
      ctx.lineTo(sx(triLines[base].x2), sy(triLines[base].y2));
      ctx.lineTo(sx(triLines[base + 1].x2), sy(triLines[base + 1].y2));
      ctx.closePath();
      ctx.fill();
    }
  }

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
    lastPrims = prims;
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

// ---- 段階2c: 選択 state (図⇔表の双方向ハイライト) ----
// 選択番号 1 つだけ持つ (複数選択は不要)。行の追加・削除は番号が振り直されるので
// 選択は素直に解除 (引き算: 追従しない)。辺長等の値編集は番号不変なので選択維持。

function updateRowHighlight(): void {
  const tbody = document.getElementById('triRows');
  if (!tbody) return;
  Array.from(tbody.children).forEach((tr, i) => {
    tr.classList.toggle('selected', i + 1 === selected);
  });
}

function selectTriangle(canvas: HTMLCanvasElement, n: number): void {
  selected = n;
  updateRowHighlight();
  draw(canvas, lastPrims);
  setStatus(n > 0 ? `selected: ${n}` : 'selection cleared');
}

function clearSelection(): void {
  selected = 0;
  updateRowHighlight();
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
    if (i + 1 === selected) tr.classList.add('selected');
    // 行クリック → 図の三角形と同じ選択 state を更新 (双方向の表側)。
    // セル内 input のクリックも「その行を選ぶ」操作なので止めない
    tr.addEventListener('click', () => selectTriangle(canvas, i + 1));

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
    del.addEventListener('click', (e) => {
      e.stopPropagation(); // 行クリック選択を発火させない
      rows.splice(i, 1);
      clearSelection(); // 番号が振り直されるので選択解除
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
  clearSelection(); // 行構成が変わるので選択解除
  buildTable(canvas);
  redraw(canvas);
}

// ---- 段階2c: Canvas 入力 (wheel ズーム / drag パン / pinch / クリック選択) ----
// 慣性・アニメは不要 (引き算)。再描画は lastPrims の全量描き直しで十分
// (insight #55: 描画コストは draw call 律速、この規模では問題ない)。

function canvasPx(canvas: HTMLCanvasElement, e: { clientX: number; clientY: number }): { x: number; y: number } {
  // CSS 表示サイズと canvas 内部解像度のずれを吸収して内部 px に揃える
  const rect = canvas.getBoundingClientRect();
  return {
    x: ((e.clientX - rect.left) * canvas.width) / rect.width,
    y: ((e.clientY - rect.top) * canvas.height) / rect.height,
  };
}

function wireCanvasEvents(canvas: HTMLCanvasElement): void {
  // wheel: カーソル位置を不動点にズーム
  canvas.addEventListener(
    'wheel',
    (e) => {
      e.preventDefault();
      const p = canvasPx(canvas, e);
      zoomAt(p.x, p.y, e.deltaY < 0 ? 1.2 : 1 / 1.2);
      draw(canvas, lastPrims);
    },
    { passive: false },
  );

  // pointer events で mouse / touch を一本化。1 本 = パン or クリック、2 本 = ピンチ
  const pointers = new Map<number, { x: number; y: number }>();
  let downAt: { x: number; y: number } | null = null; // クリック判定の基準点
  let moved = false; // 5px 超動いたら drag、クリック選択は発火させない
  let pinchDist = 0;

  canvas.addEventListener('pointerdown', (e) => {
    canvas.setPointerCapture(e.pointerId);
    const p = canvasPx(canvas, e);
    pointers.set(e.pointerId, p);
    if (pointers.size === 1) {
      downAt = p;
      moved = false;
    } else {
      moved = true; // 2 本目が触れた時点でクリック扱いにしない
      if (pointers.size === 2) {
        const [a, b] = [...pointers.values()];
        pinchDist = Math.hypot(a.x - b.x, a.y - b.y);
      }
    }
  });

  canvas.addEventListener('pointermove', (e) => {
    const prev = pointers.get(e.pointerId);
    if (!prev) return;
    const cur = canvasPx(canvas, e);
    pointers.set(e.pointerId, cur);
    const v = view;
    if (!v) return;

    if (pointers.size === 1) {
      // drag パン: offset の平行移動
      v.offsetX += cur.x - prev.x;
      v.offsetY += cur.y - prev.y;
      if (downAt && Math.hypot(cur.x - downAt.x, cur.y - downAt.y) > 5) moved = true;
      draw(canvas, lastPrims);
    } else if (pointers.size === 2) {
      // pinch: 2 点の中点を不動点に距離比でズーム
      const [a, b] = [...pointers.values()];
      const d = Math.hypot(a.x - b.x, a.y - b.y);
      if (pinchDist > 0 && d > 0) {
        zoomAt((a.x + b.x) / 2, (a.y + b.y) / 2, d / pinchDist);
        draw(canvas, lastPrims);
      }
      pinchDist = d;
    }
  });

  const release = (e: PointerEvent) => {
    if (!pointers.has(e.pointerId)) return;
    const isClick = pointers.size === 1 && !moved && downAt !== null;
    pointers.delete(e.pointerId);
    pinchDist = 0;
    if (isClick && view) {
      // クリック選択: px → モデル座標 (draw と同じ ViewTransform の逆) → common の当たり判定。
      // 何もない場所は 0 が返り選択解除になる
      const m = toModel(view, downAt!.x, downAt!.y);
      selectTriangle(canvas, hitTriangle(serializeState(), m.x, m.y));
    }
    downAt = null;
  };
  canvas.addEventListener('pointerup', release);
  canvas.addEventListener('pointercancel', release);

  // ダブルクリックで全体 fit に戻す
  canvas.addEventListener('dblclick', () => {
    view = null;
    draw(canvas, lastPrims);
  });
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
  view = null; // 新しいデータは全体 fit から
  clearSelection();
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
  wireCanvasEvents(canvas);

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
