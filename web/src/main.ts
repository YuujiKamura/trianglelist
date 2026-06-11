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
  // 段階2e: 識別 + 現在実効値 (dim テキストのみ tri/side/h/v、番号テキストは tri のみ)
  tri?: number;
  side?: number;
  h?: number; // horizontal 実効値 (0..4、>2 で旗揚げ)。W cycle はここから次値を計算
  v?: number; // vertical 実効値 (1=外/3=内)
};
type CirclePrim = { type: 'circle'; layer: string; cx: number; cy: number; r: number; tri?: number };
type Prim = LinePrim | TextPrim | CirclePrim;

// Kotlin wasmJs の ESM glue を静的 import してバンドルに乗せる (Vite の正規 module graph)。
// .wasm 本体は uninstantiated.mjs が fetch('./TriangleList-common-wasm-js.wasm') =
// document base 相対で読むため、sync-wasm が web/public/ 直下に置く
import {
  renderCsvToPrimitivesWithOverrides,
  buildDxfTextWithOverrides,
  buildSfcTextWithOverrides,
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

// ---- 段階2e: 手動 override (ADR 0003 の「式 ⊕ override」の override 側) ----
// W/H フリップ・番号サークル移動は CSV に乗らない (WebCsvReader は dim を round-trip
// しない) ので、結果値をここで保持して全 render/export 呼出しに JSON で渡す。
// 形式は common の WebOverrides.parse と対 (tri = 1-based、side = 0/1/2 = A/B/C)
type DimOverride = { tri: number; side: number; h?: number; v?: number };
type NumberOverride = { tri: number; x: number; y: number };
type Overrides = { dims: DimOverride[]; numbers: NumberOverride[] };

let overrides: Overrides = { dims: [], numbers: [] };

let selectedDim: { tri: number; side: number } | null = null; // W/H ボタンの対象
// 段階2f: 番号移動はアプリと同じ明示操作 (移動先をタップ → 旗 FAB)。
// 暗黙の移動モード (番号タップで遷移) は廃止し、最後のタップ位置だけ覚える
// (アプリ myview.pressedInModel 相当 — MainActivity.flagTriangle:1570)
let lastTapModel: { x: number; y: number } | null = null;

// 段階2g: 辺タップ選択 + シャドー三角形 (アプリ targetInTriMode lastTapSide 1/2 →
// autoConnection + MyView.drawShadowTriangle:675 の web 版)。
// edgeSel = タップされた親辺 (side: 1=B, 2=C)。shadowPrims はシャドーの 3 辺 [A,B,C] —
// TS で幾何を再計算せず、仮の行を足した CSV を common に描かせて最後の三角形を借りる
// (接続の向きが実際の追加結果と必ず一致する)
let edgeSel: { tri: number; side: 1 | 2 } | null = null;
let shadowPrims: LinePrim[] | null = null;

function overridesJson(): string {
  return JSON.stringify(overrides);
}

// 触った軸だけ記録する (h だけ/v だけ)。flag の立ち方が app の controlDim* と一致する
function upsertDimOverride(tri: number, side: number, patch: { h?: number; v?: number }): void {
  let o = overrides.dims.find((d) => d.tri === tri && d.side === side);
  if (!o) {
    o = { tri, side };
    overrides.dims.push(o);
  }
  if (patch.h !== undefined) o.h = patch.h;
  if (patch.v !== undefined) o.v = patch.v;
}

function upsertNumberOverride(tri: number, x: number, y: number): void {
  const o = overrides.numbers.find((n) => n.tri === tri);
  if (o) {
    o.x = x;
    o.y = y;
  } else {
    overrides.numbers.push({ tri, x, y });
  }
}

// 行削除で三角形番号が振り直される: 消えた番号の override は捨て、後続番号は -1 する
function shiftOverridesAfterDelete(n: number): void {
  overrides.dims = overrides.dims
    .filter((d) => d.tri !== n)
    .map((d) => (d.tri > n ? { ...d, tri: d.tri - 1 } : d));
  overrides.numbers = overrides.numbers
    .filter((o) => o.tri !== n)
    .map((o) => (o.tri > n ? { ...o, tri: o.tri - 1 } : o));
}

// 現在の lastPrims から (tri, side) の寸法テキストを引く (実効 h/v の読み出し元)
function findDimPrim(tri: number, side: number): TextPrim | undefined {
  return lastPrims.find(
    (p): p is TextPrim => p.type === 'text' && p.layer === 'dim' && p.tri === tri && p.side === side,
  );
}

function findNumberCircle(tri: number): CirclePrim | undefined {
  return lastPrims.find((p): p is CirclePrim => p.type === 'circle' && p.tri === tri);
}

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

  // 段階2g: シャドー三角形 (グレー塗り + B/C ラベル、MyView.drawShadowTriangle と同じ見せ方)。
  // ラベルの値は新規入力行の B/C (アプリの watchedB1_/watchedC1_ 相当) を都度読む
  if (shadowPrims && shadowPrims.length === 3) {
    const [sa, sbLine, scLine] = shadowPrims;
    ctx.fillStyle = 'rgba(128, 128, 128, 0.35)';
    ctx.beginPath();
    ctx.moveTo(sx(sa.x1), sy(sa.y1));
    ctx.lineTo(sx(sa.x2), sy(sa.y2));
    ctx.lineTo(sx(sbLine.x2), sy(sbLine.y2));
    ctx.closePath();
    ctx.fill();
    ctx.fillStyle = '#e67e22';
    // 寸法テキストと同じスケール感 (モデル単位の文字高さ × ズーム倍率) に合わせる
    const dimSize =
      lastPrims.find((q): q is TextPrim => q.type === 'text' && q.layer === 'dim')?.size ?? 0.25;
    ctx.font = `${Math.max(dimSize * s, 8)}px sans-serif`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    const bv = (document.getElementById('newB') as HTMLInputElement | null)?.value ?? '';
    const cv = (document.getElementById('newC') as HTMLInputElement | null)?.value ?? '';
    ctx.fillText(`B ${bv}`, sx((sbLine.x1 + sbLine.x2) / 2), sy((sbLine.y1 + sbLine.y2) / 2));
    ctx.fillText(`C ${cv}`, sx((scLine.x1 + scLine.x2) / 2), sy((scLine.y1 + scLine.y2) / 2));
  }

  // 段階2e: 選択中の寸法テキスト / 移動モード中の番号サークルをオレンジ破線で示す
  const marker = (cx: number, cy: number, r: number) => {
    ctx.strokeStyle = '#e67e22';
    ctx.lineWidth = 1.5;
    ctx.setLineDash([4, 3]);
    ctx.beginPath();
    ctx.arc(cx, cy, r, 0, Math.PI * 2);
    ctx.stroke();
    ctx.setLineDash([]);
  };
  if (selectedDim) {
    const p = prims.find(
      (q): q is TextPrim =>
        q.type === 'text' && q.layer === 'dim' && q.tri === selectedDim!.tri && q.side === selectedDim!.side,
    );
    if (p) marker(sx(p.x), sy(p.y), Math.max(p.size * s * 1.4, 14));
  }
}

function renderCsv(canvas: HTMLCanvasElement, csv: string, label: string): void {
  try {
    const json = renderCsvToPrimitivesWithOverrides(csv, 1.0, overridesJson());
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

// 測点名は CSV 列6 (CsvLoader.kt:242 のカラム表で確定)。extras[0] がそれに当たる。
// WebCsvReader は列6を読まない (描画に出ない) が、round-trip と書き出しで保持する
function nameOf(row: Row): string {
  return row.extras[0] ?? '';
}

function setName(row: Row, v: string): void {
  if (row.extras.length === 0 && v === '') return; // 列を増やさない (round-trip 不変)
  row.extras[0] = v;
}

// Kotlin の toIntOrNull と同じ判定 (parseInt は '1.5'→1 になるので regex で厳密に)
function intOrNull(s: string): number | null {
  return /^[+-]?\d+$/.test(s) ? parseInt(s, 10) : null;
}

// 段階2f: 辺長の表示は小数 2 桁に統一 (step=0.01 と対)。CSV 由来の "3"/"3.0" 混在を
// 表示と保存値の両方で揃える。数値でない文字列はそのまま (壊さない)
function fmt2(s: string): string {
  const n = parseFloat(s);
  return Number.isFinite(n) ? n.toFixed(2) : s;
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

// ---- 段階2d: autosave ----
// アプリの autosave (MainActivity.kt:1235-1255、全編集 FAB の後に CSV を private へ保存) の web 版。
// redraw() は「編集の後」しか呼ばれない (ズーム・パンは draw() 直呼び) ので、ここに挟むと
// 全編集経路が漏れなく保存される
const AUTOSAVE_KEY = 'trianglelist.web.autosave.csv';

// 段階2e: overrides も一緒に封筒 JSON で保存する ({csv, overrides})。
// 旧形式 (raw CSV 文字列) は restore 側がフォールバック読みする
function autosave(): void {
  try {
    localStorage.setItem(AUTOSAVE_KEY, JSON.stringify({ csv: serializeState(), overrides }));
  } catch {
    // private mode 等で localStorage が使えなくても編集自体は続行できる
  }
}

function redraw(canvas: HTMLCanvasElement): void {
  renderCsv(canvas, serializeState(), `table (${rows.length} rows)`);
  autosave();
}

// ---- 段階2c: 選択 state (図⇔表の双方向ハイライト) ----
// 選択番号 1 つだけ持つ (複数選択は不要)。行の追加・削除は番号が振り直されるので
// 選択は素直に解除 (引き算: 追従しない)。辺長等の値編集は番号不変なので選択維持。

// ---- 段階2d: カレント行 (アプリ EditList.retrieveCurrent 相当) ----
// 3 行フォームの「現在」が指す行番号 (1-based、0 = なし)。タップ・行クリック・上下 FAB で動く
let current = 0;

function el<T extends HTMLElement>(id: string): T {
  return document.getElementById(id) as T;
}

const input = (id: string) => el<HTMLInputElement>(id);
const select = (id: string) => el<HTMLSelectElement>(id);

// 3 行フォームを state から書き直す (アプリ EditorTable.lineRewrite + scroll 相当)。
// 新規入力行の値はユーザーの書きかけなので触らない — 番号プリセットだけ更新する
function syncForm(): void {
  input('newNum').value = String(rows.length + 1);

  const cur = current >= 1 ? rows[current - 1] : null;
  input('curNum').value = cur ? String(current) : '';
  input('curName').value = cur ? nameOf(cur) : '';
  input('curA').value = cur ? fmt2(cur.a) : '';
  input('curB').value = cur ? fmt2(cur.b) : '';
  input('curC').value = cur ? fmt2(cur.c) : '';
  input('curParent').value = cur ? cur.parent : '';
  select('curConn').value = cur ? cur.conn : '-1';

  const prev = current >= 2 ? rows[current - 2] : null;
  input('prevNum').value = prev ? String(current - 1) : '';
  input('prevName').value = prev ? nameOf(prev) : '';
  input('prevA').value = prev ? fmt2(prev.a) : '';
  input('prevB').value = prev ? fmt2(prev.b) : '';
  input('prevC').value = prev ? fmt2(prev.c) : '';
  input('prevParent').value = prev ? prev.parent : '';
  // 前行は read-only 表示なので接続は select でなくラベル文字列で揃える
  input('prevConn').value = prev ? (CONN_OPTIONS.find(([v]) => v === prev.conn)?.[1] ?? prev.conn) : '';
}

function updateRowHighlight(): void {
  const tbody = document.getElementById('triRows');
  if (!tbody) return;
  Array.from(tbody.children).forEach((tr, i) => {
    tr.classList.toggle('selected', i + 1 === selected);
  });
}

// タップ/行クリック = 選択 + カレント行切替 + 新規入力行の親番号セット
// (アプリ handleTriangleTap (MainActivity.kt:2107) + setEditTextContent (2124) 相当)
function selectTriangle(canvas: HTMLCanvasElement, n: number): void {
  selected = n;
  selectedDim = null; // 三角形を選び直したら寸法選択は解除
  edgeSel = null;
  shadowPrims = null;
  if (n > 0) {
    current = n;
    input('newParent').value = String(n);
  }
  updateRowHighlight();
  syncForm();
  draw(canvas, lastPrims);
  setStatus(n > 0 ? `selected: ${n}` : 'selection cleared');
}

function clearSelection(): void {
  selected = 0;
  selectedDim = null;
  edgeSel = null;
  shadowPrims = null;
  updateRowHighlight();
}

const CONN_OPTIONS: Array<[string, string]> = [
  ['-1', '独立'],
  ['1', '親のB辺'],
  ['2', '親のC辺'],
];

function numberInput(value: string, step: string, onInput: (v: string) => void): HTMLInputElement {
  const inp = document.createElement('input');
  inp.type = 'number';
  inp.step = step;
  inp.value = value;
  inp.addEventListener('input', () => onInput(inp.value));
  return inp;
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

    // 測点名 (CSV 列6 = extras[0])。描画には出ないが round-trip・書き出しで保持する
    const tdName = document.createElement('td');
    const nameInp = document.createElement('input');
    nameInp.type = 'text';
    nameInp.value = nameOf(row);
    nameInp.addEventListener('input', () => {
      setName(row, nameInp.value);
      autosave(); // 描画は変わらないので redraw 不要、保存だけ
    });
    tdName.appendChild(nameInp);
    tr.appendChild(tdName);

    for (const key of ['a', 'b', 'c'] as const) {
      const td = document.createElement('td');
      // 表示も保存値も fmt2 で 2 桁に揃える (入力途中の文字列は input イベントでは触らない)
      const inp = numberInput(fmt2(row[key]), '0.01', (v) => {
        row[key] = v;
        redraw(canvas);
      });
      inp.addEventListener('change', () => {
        inp.value = fmt2(inp.value);
        row[key] = inp.value;
        redraw(canvas);
      });
      td.appendChild(inp);
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
    const sel = document.createElement('select');
    for (const [value, label] of CONN_OPTIONS) {
      const opt = document.createElement('option');
      opt.value = value;
      opt.textContent = label;
      sel.appendChild(opt);
    }
    if (!CONN_OPTIONS.some(([v]) => v === row.conn)) {
      // 既存 CSV に -1/1/2 以外 (例: 0) が来ても値を壊さず保持する
      const opt = document.createElement('option');
      opt.value = row.conn;
      opt.textContent = row.conn;
      sel.appendChild(opt);
    }
    sel.value = row.conn;
    sel.addEventListener('change', () => {
      row.conn = sel.value;
      redraw(canvas);
    });
    tdConn.appendChild(sel);
    tr.appendChild(tdConn);

    const tdDel = document.createElement('td');
    const del = document.createElement('button');
    del.type = 'button';
    del.className = 'del';
    del.textContent = '削除';
    del.addEventListener('click', (e) => {
      e.stopPropagation(); // 行クリック選択を発火させない
      rows.splice(i, 1);
      shiftOverridesAfterDelete(i + 1); // 番号が振り直されるので override も追従
      clearSelection(); // 番号が振り直されるので選択解除
      if (current > rows.length) current = rows.length;
      buildTable(canvas); // 番号が振り直されるので組み直し
      syncForm();
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
  syncForm();
  redraw(canvas);
}

// ---- 段階2d: FAB 群 (アプリ fabs.xml + fabController 相当) ----
// undo は 1 段だけ: 編集 FAB の直前 CSV を丸ごと持つ (アプリ trilistUndo = trianglelist.clone()
// (MainActivity.kt:1278,1615) の CSV 版)。一覧のインライン編集はキーストローク粒度になるので
// 対象外 — アプリも undo を取るのは fabReplace と performDelete だけ。
// 段階2e: overrides も一緒に snapshot する (削除で tri 番号が shift するため CSV だけでは戻らない)
type UndoSnap = { csv: string; overrides: Overrides };
let undoSnap: UndoSnap | null = null;

function takeUndoSnap(): void {
  undoSnap = { csv: serializeState(), overrides: JSON.parse(JSON.stringify(overrides)) as Overrides };
}

// setB/setC (MainActivity.kt:1418-1428 + autoConnection:1999): 新規入力行の接続を立てて
// 辺B にフォーカス。親番号が空ならカレント (タップ済み番号、無ければ末尾 = setTriListNumber:1996)。
// 辺A は親行の接続先辺長を文字列コピーでプリセット — WebCsvReader.kt:33 は辺A 空の行を
// skip するため必須 (アプリは影プレビュー経由で決まるが、影はスコープ外)
function autoConnection(side: 1 | 2): void {
  select('newConn').value = String(side);
  const parentInput = input('newParent');
  if (parentInput.value.trim() === '') {
    parentInput.value = String(current > 0 ? current : rows.length);
  }
  const p = rows[parseInt(parentInput.value, 10) - 1];
  if (p) input('newA').value = side === 1 ? p.b : p.c;
  input('newB').focus();
}

// replace (MainActivity.kt:1614 fabReplace → processTriEditMode:1652):
// 新規行の B が空 → カレント行の書換え / C だけ空 → 何もしない / 両方あり → 追加。
// この 3 分岐の判定をそのまま写す (幾何の知識は CSV 再構築側 = common が持つ)
function fabReplace(canvas: HTMLCanvasElement): void {
  const newB = input('newB').value.trim();
  const newC = input('newC').value.trim();

  if (newB === '') {
    // Edit attempt (processTriEditMode: strAddLineB.isEmpty)
    if (rows.length === 0) {
      setStatus('Cannot edit: リストが空です。先に追加してください');
      return;
    }
    if (current < 1) {
      setStatus('Cannot edit: カレント行がありません。図か一覧で選択してください');
      return;
    }
    takeUndoSnap();
    const r = rows[current - 1];
    r.a = input('curA').value;
    r.b = input('curB').value;
    r.c = input('curC').value;
    const cp = input('curParent').value.trim();
    r.parent = cp === '' ? '-1' : cp;
    r.conn = select('curConn').value;
    setName(r, input('curName').value);
    buildTable(canvas);
    syncForm();
    redraw(canvas);
    setStatus(`Rewrite Triangle ${current}`);
    return;
  }
  if (newC === '') return; // アプリと同じ: B だけでは何もしない (strAddLineC.isEmpty -> return)

  // Add (アプリ addTriangleBy 相当 — 行を足して CSV 再構築に任せる)
  takeUndoSnap();
  const conn = select('newConn').value;
  let parent = input('newParent').value.trim();
  if (conn === '-1') {
    parent = '-1';
  } else if (parent === '') {
    parent = String(current > 0 ? current : rows.length);
  }
  let a = input('newA').value.trim();
  if ((conn === '1' || conn === '2') && a === '') {
    // 辺A 空のまま CSV に流すと WebCsvReader が行ごと skip する — 親の接続先辺長を写す
    const p = rows[parseInt(parent, 10) - 1];
    if (p) a = conn === '1' ? p.b : p.c;
  }
  const name = input('newName').value;
  rows.push({ a, b: newB, c: newC, parent, conn, extras: name !== '' ? [name] : [] });

  // 新規入力行をリセットして次の入力へ (アプリ finalizeReplace + editorResetBy 相当)
  input('newName').value = '';
  input('newA').value = '';
  input('newB').value = '';
  input('newC').value = '';
  input('newParent').value = '';
  select('newConn').value = '-1';

  selected = rows.length;
  current = rows.length;
  edgeSel = null; // 追加が確定したのでシャドーは消す
  shadowPrims = null;
  buildTable(canvas);
  syncForm();
  redraw(canvas);
  setStatus(`Add Triangle ${rows.length}`);
}

// 削除 FAB (MainActivity.kt:1331-1350): 2 タップ確認式。1 回目で赤点灯 + 3 秒タイマー、
// 2 回目で performDelete (1275) — カレント (lastTapNumber 相当) を消す
let deleteArmed = false;
let deleteTimer: ReturnType<typeof setTimeout> | undefined;

function disarmDelete(): void {
  deleteArmed = false;
  el<HTMLButtonElement>('fabMinus').classList.remove('armed');
  if (deleteTimer !== undefined) clearTimeout(deleteTimer);
  deleteTimer = undefined;
}

function fabMinus(canvas: HTMLCanvasElement): void {
  if (!deleteArmed) {
    deleteArmed = true;
    el<HTMLButtonElement>('fabMinus').classList.add('armed');
    deleteTimer = setTimeout(disarmDelete, 3000);
    setStatus('もう一度タップで削除');
    return;
  }
  disarmDelete();
  if (rows.length === 0) return;
  const n = current > 0 ? current : rows.length;
  takeUndoSnap();
  rows.splice(n - 1, 1);
  shiftOverridesAfterDelete(n); // 番号が振り直されるので override も追従
  clearSelection();
  current = Math.min(n, rows.length); // 消した位置の次 (なければ末尾) をカレントに
  buildTable(canvas);
  syncForm();
  redraw(canvas);
  setStatus(`Delete Triangle ${n}`);
}

// undo FAB (MainActivity.kt:1352-1365): 1 段だけ戻して undo バッファを空にする
function fabUndo(canvas: HTMLCanvasElement): void {
  if (undoSnap === null) {
    setStatus('undo: 戻る先がありません');
    return;
  }
  const snap = undoSnap;
  undoSnap = null; // trilistUndo.trilist.clear() と同じ: 1 段で使い切り
  overrides = snap.overrides;
  loadCsv(canvas, snap.csv, 'undo');
  autosave();
}

// リスト上下 FAB (アプリ fab_up/down → EditorTable.scroll(±1) + moveTrilist:1691):
// カレント行を ±1 して図の選択も追従させる
function moveCurrent(canvas: HTMLCanvasElement, delta: number): void {
  if (rows.length === 0) return;
  const base = current > 0 ? current : rows.length;
  const n = Math.min(Math.max(base + delta, 1), rows.length);
  current = n;
  selected = n;
  updateRowHighlight();
  syncForm();
  draw(canvas, lastPrims);
  setStatus(`current: ${n}`);
}

// ---- 段階2e: 寸法 W/H フリップ (アプリ MainViewModel.kt:47-48 の "W"/"H" と同じ語彙) ----
// W = horizontal cycle (次の値へ、HORIZONTAL_OPTIONMAX=4 で 0..4 を一周。>2 で旗揚げ)、
// H = vertical flip (1=外 ⇔ 3=内)。override は結果値で記録する (cycle/flip の再生は
// 状態依存で非冪等 — Dims.kt:130 cycleIncrement / :155 flipVertical)。
// 現在実効値はプリミティブ JSON の h/v フィールドから読む (接続構造依存の初期値を推測しない)
function flipDim(canvas: HTMLCanvasElement, axis: 'W' | 'H'): void {
  if (!selectedDim) {
    setStatus(`${axis}: 先に寸法値をタップで選択`);
    return;
  }
  const prim = findDimPrim(selectedDim.tri, selectedDim.side);
  if (!prim) {
    setStatus(`${axis}: 選択中の寸法が見つからない (再選択して)`);
    selectedDim = null;
    return;
  }
  if (axis === 'W') {
    const next = ((prim.h ?? 0) + 1) % 5;
    upsertDimOverride(selectedDim.tri, selectedDim.side, { h: next });
    redraw(canvas);
    setStatus(`W: 三角形 ${selectedDim.tri} ${['A', 'B', 'C'][selectedDim.side]} 辺 → ${next}${next > 2 ? ' (旗揚げ)' : ''}`);
  } else {
    const next = (prim.v ?? 1) === 1 ? 3 : 1;
    upsertDimOverride(selectedDim.tri, selectedDim.side, { v: next });
    redraw(canvas);
    setStatus(`H: 三角形 ${selectedDim.tri} ${['A', 'B', 'C'][selectedDim.side]} 辺 → ${next === 1 ? '外' : '内'}`);
  }
}

// 旗 FAB (アプリ fab_flag → flagTriangle、MainActivity.kt:1568-1582 と同じ明示操作):
// 移動先を先にタップしておき、これを押すとカレント行の番号サークルがそこへ動く。
// 移動の実体は override (ADR 0003 の式⊕override 二層) — 遠すぎる点は common 側の
// BORDER 判定で無視されるので、動いたかは描画後のサークル位置で判定して伝える
function fabFlag(canvas: HTMLCanvasElement): void {
  const n = current > 0 ? current : 0;
  if (n < 1) {
    setStatus('旗揚げ: 先に三角形を選択してください');
    return;
  }
  if (!lastTapModel) {
    setStatus('旗揚げ: 移動先をタップしてから押してください');
    return;
  }
  const dest = lastTapModel;
  upsertNumberOverride(n, dest.x, dest.y);
  redraw(canvas);
  const c = findNumberCircle(n);
  const moved = c && Math.hypot(c.cx - dest.x, c.cy - dest.y) < 1e-6;
  setStatus(moved ? `番号 ${n} を移動した` : `番号 ${n}: 中心から遠すぎるため移動せず`);
}

// 新規作成 (段階2f): 現在の図を捨て、最初の 1 個 (独立 3.00/3.00/3.00) を生成して
// 全体フィットから始める (空画面でなく即編集に入れる形)。誤爆は confirm + undo 1 段で守る
function newDrawing(canvas: HTMLCanvasElement): void {
  if (rows.length > 0 && !window.confirm('現在の図を破棄して新規作成しますか?')) return;
  takeUndoSnap();
  headerLines = ['無題工事', '新規路線', '', ''];
  rows = [{ a: '3.00', b: '3.00', c: '3.00', parent: '-1', conn: '-1', extras: [] }];
  overrides = { dims: [], numbers: [] };
  lastTapModel = null;
  clearSelection();
  selected = 1;
  current = 1;
  view = null; // 次の描画で 1 個目に全体フィット
  buildTable(canvas);
  syncForm();
  syncRosenName();
  redraw(canvas);
  setStatus('新規作成 — 三角形 1 を生成。3行フォームと B/C ボタンで継ぎ足し');
}

function wireFabs(canvas: HTMLCanvasElement): void {
  el<HTMLButtonElement>('fabSetB').addEventListener('click', () => autoConnection(1));
  el<HTMLButtonElement>('fabSetC').addEventListener('click', () => autoConnection(2));
  el<HTMLButtonElement>('fabDimW').addEventListener('click', () => flipDim(canvas, 'W'));
  el<HTMLButtonElement>('fabDimH').addEventListener('click', () => flipDim(canvas, 'H'));
  el<HTMLButtonElement>('fabFlag').addEventListener('click', () => fabFlag(canvas));
  el<HTMLButtonElement>('fabReplace').addEventListener('click', () => fabReplace(canvas));
  el<HTMLButtonElement>('fabUndo').addEventListener('click', () => fabUndo(canvas));
  el<HTMLButtonElement>('fabMinus').addEventListener('click', () => fabMinus(canvas));
  // resetView FAB: 既存 dblclick fit と同じ動作のボタン化
  el<HTMLButtonElement>('fabResetView').addEventListener('click', () => {
    view = null;
    draw(canvas, lastPrims);
  });
  el<HTMLButtonElement>('fabUp').addEventListener('click', () => moveCurrent(canvas, -1));
  el<HTMLButtonElement>('fabDown').addEventListener('click', () => moveCurrent(canvas, 1));
}

// 路線名: アプリの editor_table の路線名欄に相当。sample_triangles.csv 形式では
// ヘッダ 2 行目 (工事名/路線名/業者名/測点) — 描画には出ないが round-trip で保持する
function syncRosenName(): void {
  input('rosenName').value = headerLines[1] ?? '';
}

function wireRosenName(): void {
  input('rosenName').addEventListener('input', () => {
    while (headerLines.length < 2) {
      // ヘッダの無い最小 CSV に路線名を書いたら工事名行から補う (空行は再読込で落ちるため)
      headerLines.push(headerLines.length === 0 ? '無題' : '');
    }
    headerLines[1] = input('rosenName').value;
    autosave();
  });
}

// ---- 段階2c: Canvas 入力 (wheel ズーム / drag パン / pinch / クリック選択) ----
// 慣性・アニメは不要 (引き算)。再描画は lastPrims の全量描き直しで十分
// (insight #55: 描画コストは draw call 律速、この規模では問題ない)。

// ---- 段階2e: タップ判定 (寸法テキスト / 番号サークル / 三角形) ----
// 寸法テキスト・番号サークルの当たり判定は、プリミティブ JSON の text/circle 座標に
// 対する画面 px の最近傍判定 (brief 設計: 新しい facade 関数は増やさない)。
// 優先順位: 番号移動モードの移動先 > 番号サークル > 寸法テキスト > 三角形 (common の isCollide)

function nearestDimText(px: number, py: number): { prim: TextPrim; d: number } | null {
  const v = view;
  if (!v) return null;
  let best: { prim: TextPrim; d: number } | null = null;
  for (const p of lastPrims) {
    if (p.type !== 'text' || p.layer !== 'dim' || p.tri === undefined || p.side === undefined) continue;
    const d = Math.hypot(p.x * v.scale + v.offsetX - px, -p.y * v.scale + v.offsetY - py);
    const threshold = Math.max(p.size * v.scale * 1.4, 16); // 文字高さ依存 + 最低 16px
    if (d <= threshold && (!best || d < best.d)) {
      best = { prim: p, d };
    }
  }
  return best;
}

// 画面 px での点と線分の距離 (辺タップ・シャドー辺タップの当たり判定)
function distToSegmentPx(px: number, py: number, l: LinePrim): number {
  const v = view;
  if (!v) return Infinity;
  const x1 = l.x1 * v.scale + v.offsetX;
  const y1 = -l.y1 * v.scale + v.offsetY;
  const x2 = l.x2 * v.scale + v.offsetX;
  const y2 = -l.y2 * v.scale + v.offsetY;
  const dx = x2 - x1;
  const dy = y2 - y1;
  const len2 = dx * dx + dy * dy;
  let t = len2 > 0 ? ((px - x1) * dx + (py - y1) * dy) / len2 : 0;
  t = Math.max(0, Math.min(1, t));
  return Math.hypot(px - (x1 + t * dx), py - (y1 + t * dy));
}

// 最寄りの三角形辺を探す。WebPrimitiveRenderer は三角形ごとに [A,B,C] の順で
// 'tri' line を出す (point[0]→pointAB, pointAB→pointBC, pointBC→point[0]) ので
// index % 3 がそのまま side (0=A, 1=B, 2=C)
const EDGE_TAP_PX = 14;
function nearestEdge(px: number, py: number): { tri: number; side: 0 | 1 | 2; d: number } | null {
  const triLines = lastPrims.filter((p): p is LinePrim => p.type === 'line' && p.layer === 'tri');
  let best: { tri: number; side: 0 | 1 | 2; d: number } | null = null;
  for (let i = 0; i < triLines.length; i++) {
    const d = distToSegmentPx(px, py, triLines[i]);
    if (d <= EDGE_TAP_PX && (!best || d < best.d)) {
      best = { tri: Math.floor(i / 3) + 1, side: (i % 3) as 0 | 1 | 2, d };
    }
  }
  return best;
}

// シャドー三角形を組む: 仮の行 (a=親辺長, b=c=親辺長*0.75 — MyView.drawShadowTriangle:683 と
// 同じ比率) を足した CSV を common に描かせ、最後の三角形の 3 辺 [A,B,C] を借りる
function buildShadow(): void {
  shadowPrims = null;
  if (!edgeSel) return;
  const p = rows[edgeSel.tri - 1];
  if (!p) return;
  const aStr = edgeSel.side === 1 ? p.b : p.c;
  const L = parseFloat(aStr);
  if (!Number.isFinite(L) || L <= 0) return;
  const leg = (L * 0.75).toFixed(2);
  const candidate = serializeState() + `${rows.length + 1},${aStr},${leg},${leg},${edgeSel.tri},${edgeSel.side}\n`;
  try {
    const json = renderCsvToPrimitivesWithOverrides(candidate, 1.0, overridesJson());
    const prims = JSON.parse(json) as Prim[];
    const triLines = prims.filter((q): q is LinePrim => q.type === 'line' && q.layer === 'tri');
    const base = rows.length * 3; // 仮三角形 (rows.length+1 番) の線は最後の 3 本
    if (base + 2 < triLines.length) {
      shadowPrims = [triLines[base], triLines[base + 1], triLines[base + 2]];
    }
  } catch {
    shadowPrims = null;
  }
}

// 辺タップ (アプリ targetInTriMode → autoConnection(lastTapSide) 相当):
// FAB を押さなくても新規行に接続・親・辺A がプリセットされ、シャドーが出る
function selectEdge(canvas: HTMLCanvasElement, tri: number, side: 1 | 2): void {
  selected = tri;
  current = tri;
  // アプリの lastTapSide と同じく、タップした辺はそのまま W/H フリップの対象にもなる
  selectedDim = { tri, side };
  input('newParent').value = String(tri);
  select('newConn').value = String(side);
  const p = rows[tri - 1];
  if (p) input('newA').value = fmt2(side === 1 ? p.b : p.c);
  edgeSel = { tri, side };
  buildShadow();
  updateRowHighlight();
  syncForm();
  draw(canvas, lastPrims);
  setStatus(`三角形 ${tri} の ${side === 1 ? 'B' : 'C'} 辺 — 新規行にプリセット済み。B/C を入力して ✎ で追加`);
  input('newB').focus();
}

function handleTap(canvas: HTMLCanvasElement, px: number, py: number): void {
  if (!view) return;
  const m = toModel(view, px, py);
  // 段階2f: 全タップの位置を覚える (アプリ pressedInModel 相当)。旗 FAB がこれを移動先に使う
  lastTapModel = m;

  // シャドーの B/C 辺タップ → 対応する入力にフォーカス (アプリ shadowTapMode:2050 相当、最優先)
  if (shadowPrims && shadowPrims.length === 3) {
    if (distToSegmentPx(px, py, shadowPrims[1]) <= EDGE_TAP_PX) {
      input('newB').focus();
      input('newB').select();
      setStatus('シャドーの B 辺 — 長さを入力');
      return;
    }
    if (distToSegmentPx(px, py, shadowPrims[2]) <= EDGE_TAP_PX) {
      input('newC').focus();
      input('newC').select();
      setStatus('シャドーの C 辺 — 長さを入力');
      return;
    }
  }

  // 寸法テキストと辺は近接して並ぶ (寸法は辺から dimHeight 分オフセット) ので
  // 固定優先でなく「実際に近い方」を選ぶ。線の上 = 辺タップ、文字の上 = 寸法選択
  const dim = nearestDimText(px, py);
  const edge = nearestEdge(px, py);

  // 寸法値タップは「その辺をタップした」のと同じ扱いに統一 (段階2e の独自発明だった
  // 寸法単独選択は廃止 — アプリは lastTapSide 一本で接続プリセットと W/H 対象を兼ねる)
  if (dim && (!edge || dim.d <= edge.d)) {
    const p = dim.prim;
    if (p.tri !== undefined && p.side !== undefined) {
      if (p.side === 1 || p.side === 2) {
        selectEdge(canvas, p.tri, p.side);
        return;
      }
      // A 辺の寸法: 接続には使えないが W/H の対象にはなる
      selected = p.tri;
      current = p.tri;
      selectedDim = { tri: p.tri, side: 0 };
      edgeSel = null;
      shadowPrims = null;
      updateRowHighlight();
      syncForm();
      draw(canvas, lastPrims);
      setStatus(`三角形 ${p.tri} の A 辺 — W/H でフリップ、旗で番号移動`);
      return;
    }
  }

  // 辺タップ (B/C は接続プリセット + シャドー、A は選択のみ + 辺名表示)
  if (edge && edge.side > 0) {
    selectEdge(canvas, edge.tri, edge.side as 1 | 2);
    return;
  }
  if (edge && edge.side === 0) {
    selectTriangle(canvas, edge.tri);
    setStatus(`三角形 ${edge.tri} の A 辺 (接続辺なので追加先には使えない)`);
    return;
  }

  // 三角形 (common の isCollide)。何もない場所は 0 が返り選択解除になる
  selectTriangle(canvas, hitTriangle(serializeState(), m.x, m.y));
}

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
      handleTap(canvas, downAt!.x, downAt!.y);
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
  // 段階2e: overrides 付き経路に切替 — W/H フリップ・番号移動が図面ファイルにも乗る
  document.getElementById('saveDxf')?.addEventListener('click', () => {
    exportFile('DXF', 'triangles.dxf', () => toSjisBlob(buildDxfTextWithOverrides(serializeState(), overridesJson())));
  });
  document.getElementById('saveSfc')?.addEventListener('click', () => {
    exportFile('SFC', 'triangles.sfc', () =>
      toSjisBlob(buildSfcTextWithOverrides(serializeState(), 'triangles.sfc', overridesJson())),
    );
  });
}

function loadCsv(canvas: HTMLCanvasElement, csv: string, label: string): void {
  parseCsvToState(csv);
  view = null; // 新しいデータは全体 fit から
  clearSelection();
  current = rows.length > 0 ? rows.length : 0; // カレントは末尾 (アプリの初期 retrieveCurrent 相当)
  buildTable(canvas);
  syncForm();
  syncRosenName();
  renderCsv(canvas, serializeState(), label);
}

function main(): void {
  const canvas = document.getElementById('cv') as HTMLCanvasElement | null;
  const fileInput = document.getElementById('file') as HTMLInputElement | null;
  const addBtn = document.getElementById('addRow');
  if (!canvas || !fileInput) return;

  // autosave があればそこから復元、無ければ内蔵サンプル (アプリ起動時の private CSV 復元と同じ)。
  // 段階2e の封筒 JSON ({csv, overrides}) を先に試し、旧形式 (raw CSV) はフォールバック
  const saved = localStorage.getItem(AUTOSAVE_KEY);
  let savedCsv: string | null = saved;
  if (saved !== null) {
    try {
      const env = JSON.parse(saved) as { csv?: string; overrides?: Overrides };
      if (typeof env.csv === 'string') {
        savedCsv = env.csv;
        overrides = env.overrides ?? { dims: [], numbers: [] };
      }
    } catch {
      // 旧形式 raw CSV — そのまま使う (overrides は空のまま)
    }
  }
  loadCsv(canvas, savedCsv ?? SAMPLE_CSV, saved !== null ? 'autosave' : 'sample');

  addBtn?.addEventListener('click', () => addRow(canvas));
  document.getElementById('newDrawing')?.addEventListener('click', () => newDrawing(canvas));
  wireExportButtons();
  wireCanvasEvents(canvas);
  wireFabs(canvas);
  wireRosenName();

  fileInput.addEventListener('change', () => {
    const file = fileInput.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      // 注: 既存 app の CSV は Shift_JIS の場合があるが、段階1 同様 UTF-8 読みのみ
      overrides = { dims: [], numbers: [] }; // 新しいファイルに前データの override を持ち越さない
      loadCsv(canvas, String(reader.result ?? ''), file.name);
      autosave(); // 読み込んだファイルも次回リロードで復元できるように
    };
    reader.readAsText(file);
  });
}

main();

// ---- 段階2f: dev CP (control protocol) ----
// vite.config.ts の tlcp プラグインと対。CLI から
//   curl http://localhost:5173/__tlcp/capture -o shot.png  (canvas のピクセル)
//   curl http://localhost:5173/__tlcp/state                (rows/selected/current/overrides/csv)
// が取れる検証口。import.meta.hot は dev 限定なので prod build には一切乗らない
if (import.meta.hot) {
  const hot = import.meta.hot;
  hot.on('tlcp:capture-req', (data: { id: string }) => {
    const canvas = document.getElementById('cv') as HTMLCanvasElement | null;
    hot.send('tlcp:capture-res', { id: data.id, png: canvas?.toDataURL('image/png') ?? '' });
  });
  hot.on('tlcp:state-req', (data: { id: string }) => {
    hot.send('tlcp:state-res', {
      id: data.id,
      state: { rows, selected, current, overrides, csv: serializeState(), view, prims: lastPrims },
    });
  });
  // タップ注入 (モデル座標)。辺タップ・シャドー・選択の UX を CLI から再現検証する口
  hot.on('tlcp:tap-req', (data: { id: string; x?: number; y?: number }) => {
    const canvas = document.getElementById('cv') as HTMLCanvasElement | null;
    const v = view;
    if (canvas && v && typeof data.x === 'number' && typeof data.y === 'number') {
      handleTap(canvas, data.x * v.scale + v.offsetX, -data.y * v.scale + v.offsetY);
      hot.send('tlcp:tap-res', { id: data.id, state: { ok: true, selected, current, edgeSel } });
    } else {
      hot.send('tlcp:tap-res', { id: data.id, state: { ok: false } });
    }
  });
}
