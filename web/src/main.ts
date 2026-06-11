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
  buildCsvTextWithOverrides,
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
const SELECT_YELLOW = '#e6b800'; // 選択辺・番号リング・寸法ボックス (app paintYellow 相当、白地で読める濃さ)

function setStatus(msg: string): void {
  const el = document.getElementById('status');
  if (!el) return;
  el.textContent = msg;
  // ⚠ 始まり = 警告 (三角形不成立等)。ヘッダ上で目に入るよう色を変える
  el.style.color = msg.startsWith('⚠') ? '#ffb3b3' : '';
  el.style.fontWeight = msg.startsWith('⚠') ? '700' : '';
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
  const triLines = prims.filter((p): p is LinePrim => p.type === 'line' && p.layer === 'tri');
  if (selected > 0) {
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

  // 段階2g: 選択表示 (アプリ MyView.drawBlinkLine:794-818 の写し):
  //   1) 選択辺を黄色線で強調 (app: lastTapSide の辺を paintYellow で上書き)
  //   2) 番号サークルに黄色リング (app: pointnumber に paintTexS.textSize*0.8 の stroke circle)
  //   3) 寸法テキストは measureText の境界ボックス + 辺名 A/B/C を併記
  //      (旧オレンジ破線サークルは廃止 — user 指摘 2026-06-11「サークルを描く意味がない」)
  if (selectedDim) {
    const { tri, side } = selectedDim;
    ctx.strokeStyle = SELECT_YELLOW;

    // 1) 選択辺の黄色線。tri 線群の並びは [A,B,C] (nearestEdge と同じ index 規約)
    const li = (tri - 1) * 3 + side;
    if (li < triLines.length) {
      const l = triLines[li];
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.moveTo(sx(l.x1), sy(l.y1));
      ctx.lineTo(sx(l.x2), sy(l.y2));
      ctx.stroke();
    }

    // 2) 番号サークルの黄色リング (サークル半径の 1.3 倍 ≈ app の textSize*0.8 / 0.6 比)
    const nc = prims.find((q): q is CirclePrim => q.type === 'circle' && q.tri === tri);
    if (nc) {
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.arc(sx(nc.cx), sy(nc.cy), nc.r * s * 1.3, 0, Math.PI * 2);
      ctx.stroke();
    }

    // 3) 寸法テキストの境界ボックス + 辺名 (テキストと同じ変換系で measureText を鏡にする —
    //    LabelMetrics の「描画が真実、箱はその鏡」の canvas 版)
    const p = prims.find(
      (q): q is TextPrim => q.type === 'text' && q.layer === 'dim' && q.tri === tri && q.side === side,
    );
    if (p) {
      const fh = Math.max(p.size * s, 8);
      ctx.font = `${fh}px sans-serif`;
      const w = ctx.measureText(p.text).width;
      const pad = 3;
      // fillText と同じ baseline 規約: align 1 = 点の上に文字 (box は -h..0)、3 = 下、2 = 中央
      const top = p.align === 1 ? -fh : p.align === 3 ? 0 : -fh / 2;
      ctx.save();
      ctx.translate(sx(p.x), sy(p.y));
      ctx.rotate((-p.angle * Math.PI) / 180);
      ctx.lineWidth = 1.5;
      ctx.strokeRect(-w / 2 - pad, top - pad, w + pad * 2, fh + pad * 2);
      ctx.fillStyle = SELECT_YELLOW;
      ctx.font = `bold ${fh}px sans-serif`;
      ctx.textAlign = 'left';
      ctx.textBaseline = 'middle';
      ctx.fillText(['A', 'B', 'C'][side] ?? '', w / 2 + pad + 4, top + fh / 2);
      ctx.restore();
    }
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

// ---- 辺プール (ADR 0006 改訂: リーナスの「表現で特殊ケースを消す」の適用) ----
// 1 本の物理辺の長さはプールに 1 個だけ持ち、行は辺への index を持つ。
// 単純接続の子の A 辺は親の B/C と**同じ index を共有**するので、
// 「親⇔子の伝播」はコードではなく構造が担う — 写す相手が存在しない。
// (DCEL/half-edge の本質 1 点取り。面巡回等のフル機構は木構造には過剰なので持たない)
let edges: string[] = [];

function newEdge(v: string): number {
  return edges.push(v) - 1;
}

type EdgeKey = 'a' | 'b' | 'c';
const EKEY: Record<EdgeKey, 'ea' | 'eb' | 'ec'> = { a: 'ea', b: 'eb', c: 'ec' };

type Row = {
  ea: number; // 辺A の edges index (単純接続では親の eb/ec と同一値)
  eb: number;
  ec: number;
  parent: string;
  conn: string;
  extras: string[]; // 7列目以降 (測点名等) を生のまま保持して round-trip で落とさない
};

function edgeVal(r: Row, k: EdgeKey): string {
  return edges[r[EKEY[k]]];
}

function setEdgeVal(r: Row, k: EdgeKey, v: string): void {
  edges[r[EKEY[k]]] = v;
}

// WebCsvReader が三角形行として読まない行 (ヘッダ・Deduction 等) は
// 原文のまま保持し、直列化時に先頭へ戻す (どこにあっても reader は skip するので描画不変)
let headerLines: string[] = [];
let rows: Row[] = [];
// 図形全体の回転角 (アプリ TriangleList.angle = 三角形1 の絶対角度、createNew は 0)。
// 幾何への適用は wasm 側 WebCsvReader → recoverState (180° 基底からの全再構築) が担い、
// TS はこの数値 1 個を持つだけ。CSV では ListAngle 行 (CsvLoader.readListParameter と同形)
let listAngle = 0;

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
  edges = [];
  listAngle = 0;
  for (const line of csv.split(/\r?\n/)) {
    if (line.trim() === '') continue;
    const chunks = line.split(',').map((s) => s.trim());
    // ListAngle 行は headerLines に残さず数値 state に取り出す (serializeState が書き戻す)
    if (chunks[0] === 'ListAngle') {
      listAngle = parseFloat(chunks[1] ?? '') || 0;
      continue;
    }
    const num = chunks.length >= 4 ? intOrNull(chunks[0]) : null;
    if (num === null || num < 0) {
      headerLines.push(line);
      continue;
    }
    const parent = chunks[4] ?? '-1';
    const conn = chunks[5] ?? '-1';
    const extras = chunks.slice(6);
    const aStr = chunks[1] ?? '';
    // 辺A の共有判定: 単純接続で数値が親辺と一致する時だけ束ねる。
    // 一致しない CSV (手書き・古いデータ) は従来どおり別の値として保持し描画を変えない
    const pn = intOrNull(parent) ?? -1;
    const cn = intOrNull(conn);
    const pRow = pn > 0 ? rows[pn - 1] : undefined;
    let ea: number;
    if (
      pRow && (cn === 1 || cn === 2) && isSimpleConnection({ extras }) &&
      parseFloat(aStr) === parseFloat(edges[cn === 1 ? pRow.eb : pRow.ec])
    ) {
      ea = cn === 1 ? pRow.eb : pRow.ec;
    } else {
      ea = newEdge(aStr);
    }
    rows.push({
      ea,
      eb: newEdge(chunks[2] ?? ''),
      ec: newEdge(chunks[3] ?? ''),
      parent,
      conn,
      extras,
    });
  }
}

function serializeState(): string {
  const lines = [...headerLines];
  rows.forEach((r, i) => {
    // 共有 index は CSV では従来どおり複製値に展開する (app との互換境界)
    lines.push(
      [String(i + 1), edgeVal(r, 'a'), edgeVal(r, 'b'), edgeVal(r, 'c'), r.parent, r.conn, ...r.extras].join(','),
    );
  });
  // アプリ保存 (MainActivity.kt:2781) と同じく三角形行の後に ListAngle を書く
  lines.push(`ListAngle, ${listAngle}`);
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

// ---- 三角形成立判定 (共通関門) ----
// 数値の組み合わせが三角形として成立しない場合の処理は動線が複数ある
// (一覧セル編集 / 3行フォームの書換え・追加 / 寸法ダブルクリック編集 / CSV 読込)。
// 編集系の動線は全て redraw() を通る設計なので、ここを単一の関門にして
// 「キャンバスは書き換えず警告を出す」を一括で守る。事前却下できる動線
// (fabReplace の書換え/追加) は state を汚す前にこの判定で弾く。
function invalidTriangleReason(a: number, b: number, c: number): string | null {
  if (!Number.isFinite(a) || !Number.isFinite(b) || !Number.isFinite(c)) return '辺の値が数値ではありません';
  if (a <= 0 || b <= 0 || c <= 0) return '辺の長さは正の数が必要です';
  if (a + b <= c || b + c <= a || c + a <= b) return '二辺の和が他の一辺以下です (三角形になりません)';
  return null;
}

function findInvalidRow(rs: Row[]): { n: number; reason: string } | null {
  for (let i = 0; i < rs.length; i++) {
    const reason = invalidTriangleReason(
      parseFloat(edgeVal(rs[i], 'a')),
      parseFloat(edgeVal(rs[i], 'b')),
      parseFloat(edgeVal(rs[i], 'c')),
    );
    if (reason) return { n: i + 1, reason };
  }
  return null;
}

function redraw(canvas: HTMLCanvasElement): void {
  const bad = findInvalidRow(rows);
  if (bad) {
    // キャンバスは直前の正しい状態のまま据え置き、autosave も汚さない。
    // 値を直せば次の入力イベントでそのまま再描画に戻る
    setStatus(`⚠ 三角形 ${bad.n}: ${bad.reason} — 図は更新しません`);
    return;
  }
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

// 2 行フォーム (新規 + カレント) を state から書き直す (アプリ EditorTable.lineRewrite + scroll 相当)。
// 前行は廃止 (user 2026-06-11: web は全行一覧があるので不要、現行スマホ版も 2 行に簡略化済)。
// 新規入力行の値はユーザーの書きかけなので触らない — 番号プリセットだけ更新する
function syncForm(): void {
  input('newNum').value = String(rows.length + 1);

  const cur = current >= 1 ? rows[current - 1] : null;
  input('curNum').value = cur ? String(current) : '';
  input('curName').value = cur ? nameOf(cur) : '';
  input('curA').value = cur ? fmt2(edgeVal(cur, 'a')) : '';
  input('curB').value = cur ? fmt2(edgeVal(cur, 'b')) : '';
  input('curC').value = cur ? fmt2(edgeVal(cur, 'c')) : '';
  input('curParent').value = cur ? cur.parent : '';
  setConnSelects('cur', cur ? connPartsOf(cur) : null);
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

// ---- 接続コード (CSV 列5 = アプリ ParentList spinner 位置) ⇔ 接続辺×形態×起点 ----
// UI はアプリの 11 択 spinner を 3 列に分解する (user 設計 2026-06-11: 「親のB辺/C辺」の
// 直感性を保ったまま、形態 [辺共有/二重断面/フロート] と起点 [右/中央/左] を列で持つ)。
// 写像表の SoT は common の ConnCode.kt (TriangleSetters.setCParamFromParentBC と同表)
type ConnParts = { side: number; type: number; lcr: number };

const CONN_DECODE: Record<number, ConnParts> = {
  1: { side: 1, type: 0, lcr: 2 },
  2: { side: 2, type: 0, lcr: 2 },
  3: { side: 1, type: 1, lcr: 2 },
  4: { side: 1, type: 1, lcr: 0 },
  7: { side: 1, type: 1, lcr: 1 },
  5: { side: 2, type: 1, lcr: 2 },
  6: { side: 2, type: 1, lcr: 0 },
  8: { side: 2, type: 1, lcr: 1 },
  9: { side: 1, type: 2, lcr: 2 },
  10: { side: 2, type: 2, lcr: 2 },
};

function decodeConn(conn: string): ConnParts | null {
  const code = intOrNull(conn);
  return code !== null ? (CONN_DECODE[code] ?? null) : null;
}

function encodeConn(p: ConnParts): number {
  if (p.type === 0) return p.side;
  if (p.type === 1) {
    if (p.lcr === 2) return p.side === 1 ? 3 : 5;
    if (p.lcr === 0) return p.side === 1 ? 4 : 6;
    return p.side === 1 ? 7 : 8;
  }
  return p.side === 1 ? 9 : 10;
}

const CONN_LABEL: Record<string, string> = {
  '-1': '独立',
  '1': '親のB辺',
  '2': '親のC辺',
  '3': 'B二重・右',
  '4': 'B二重・左',
  '7': 'B二重・中央',
  '5': 'C二重・右',
  '6': 'C二重・左',
  '8': 'C二重・中央',
  '9': 'Bフロート',
  '10': 'Cフロート',
};

const CTYPE_OPTIONS: Array<[string, string]> = [
  ['0', '辺共有'],
  ['1', '二重断面'],
  ['2', 'フロート'],
];
const LCR_OPTIONS: Array<[string, string]> = [
  ['2', '右起点'],
  ['1', '中央'],
  ['0', '左起点'],
];

// 起点は二重断面/フロートのみ意味を持つ (辺共有は常に全長一致なので無効化)
function syncLcrDisabled(prefix: 'new' | 'cur'): void {
  select(`${prefix}Lcr`).disabled = select(`${prefix}CType`).value === '0';
}

function setConnSelects(prefix: 'new' | 'cur', p: ConnParts | null): void {
  if (!p) {
    if (prefix === 'cur') select('curConn').value = '-1';
    select(`${prefix}CType`).value = '0';
    select(`${prefix}Lcr`).value = '2';
  } else {
    select(`${prefix}Conn`).value = String(p.side);
    select(`${prefix}CType`).value = String(p.type);
    select(`${prefix}Lcr`).value = String(p.lcr);
  }
  syncLcrDisabled(prefix);
}

function readConnParts(prefix: 'new' | 'cur'): ConnParts | null {
  const side = intOrNull(select(`${prefix}Conn`).value) ?? -1;
  if (side < 1) return null; // 独立
  const type = intOrNull(select(`${prefix}CType`).value) ?? 0;
  const lcr = intOrNull(select(`${prefix}Lcr`).value) ?? 2;
  return { side, type, lcr };
}

// フロート (コード 9/10) は単独で lcr を表せない — アプリも 6 列形式では表せず、
// 保存は完全形式の列 17-19 (cp.side/type/lcr) で持つ (CsvLoader.readCParamSafe)。
// web も同じ列を extras に書いて起点の SoT にする。WebCsvReader は列 17-19 を
// 優先して読むので、ここに書けば描画に効く。なおフロートの浮き距離は common の
// 固定値 (親辺から垂直 1.0 — getParentPointByType type2 の crossOffset(-1.0)) で app と同一
const CP_SIDE_X = 11; // CSV 列 17 - 6
const CP_TYPE_X = 12; // CSV 列 18 - 6
const CP_LCR_X = 13; // CSV 列 19 - 6

function writeCpExtras(r: Row, p: ConnParts | null): void {
  if (!p || (p.type === 0 && r.extras.length <= CP_LCR_X)) return; // 辺共有はコードだけで完結
  while (r.extras.length <= CP_LCR_X) r.extras.push('');
  r.extras[CP_SIDE_X] = String(p.side);
  r.extras[CP_TYPE_X] = String(p.type);
  r.extras[CP_LCR_X] = String(p.lcr);
}

// 行の実効 ConnParts: コードを基本に、cp 列 (extras[13]) があれば lcr を上書き
function connPartsOf(r: Row): ConnParts | null {
  const base = decodeConn(r.conn);
  if (!base) return null;
  const lcr = intOrNull(r.extras[CP_LCR_X] ?? '');
  return base.type !== 0 && lcr !== null && lcr >= 0 && lcr <= 2 ? { ...base, lcr } : base;
}

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
      // 表示も保存値も fmt2 で 2 桁に揃える (入力途中の文字列は input イベントでは触らない)。
      // 値は辺プールに 1 個 — 共有辺 (子の A = 親の B/C) は書いた瞬間に両者の実体が変わるので
      // 伝播処理は無い。同じ辺を表示している他のセルだけ syncEdgeInputs で追従させる
      const inp = numberInput(fmt2(edgeVal(row, key)), '0.01', (v) => {
        setEdgeVal(row, key, v);
        syncEdgeInputs(row[EKEY[key]], inp);
        redraw(canvas);
      });
      inp.dataset.edge = String(row[EKEY[key]]);
      inp.addEventListener('change', () => {
        inp.value = fmt2(inp.value);
        setEdgeVal(row, key, inp.value);
        syncEdgeInputs(row[EKEY[key]], inp);
        redraw(canvas);
      });
      td.appendChild(inp);
      tr.appendChild(td);
    }

    const tdParent = document.createElement('td');
    const parentInp = numberInput(row.parent, '1', (v) => {
      row.parent = v;
      relinkEdgeA(row); // 接続先が変わったので辺A の共有を張り直す
      redraw(canvas);
    });
    // 入力確定 (blur) で表を組み直して data-edge を張り直す。入力中の組み直しはフォーカスを壊す
    parentInp.addEventListener('change', () => {
      buildTable(canvas);
      syncForm();
      redraw(canvas);
    });
    tdParent.appendChild(parentInp);
    tr.appendChild(tdParent);

    // 接続辺 × 形態 × 起点 の 3 列 (独立行は表示のみ — 図面に 1 リストの形態なので
    // 既存行を独立に変える操作は提供しない、スマホ版同様)
    const parts = connPartsOf(row);
    if (!parts) {
      const tdInd = document.createElement('td');
      tdInd.textContent = CONN_LABEL[row.conn] ?? row.conn;
      tr.appendChild(tdInd);
      tr.appendChild(document.createElement('td'));
      tr.appendChild(document.createElement('td'));
    } else {
      const mkSel = (options: Array<[string, string]>, value: string): HTMLSelectElement => {
        const s = document.createElement('select');
        for (const [v, label] of options) {
          const opt = document.createElement('option');
          opt.value = v;
          opt.textContent = label;
          s.appendChild(opt);
        }
        s.value = value;
        return s;
      };
      const sideSel = mkSel([['1', '親のB辺'], ['2', '親のC辺']], String(parts.side));
      const typeSel = mkSel(CTYPE_OPTIONS, String(parts.type));
      const lcrSel = mkSel(LCR_OPTIONS, String(parts.lcr));
      lcrSel.disabled = parts.type === 0;
      const apply = () => {
        const np: ConnParts = {
          side: intOrNull(sideSel.value) ?? 1,
          type: intOrNull(typeSel.value) ?? 0,
          lcr: intOrNull(lcrSel.value) ?? 2,
        };
        row.conn = String(encodeConn(np));
        writeCpExtras(row, np); // フロートの lcr はコードに乗らないため列 17-19 が SoT
        relinkEdgeA(row); // 接続の形が変わったので辺A の共有を張り直す
        buildTable(canvas); // select は離散操作なので組み直してよい (data-edge 更新)
        syncForm();
        redraw(canvas);
      };
      for (const s of [sideSel, typeSel, lcrSel]) {
        s.addEventListener('change', apply);
        const td = document.createElement('td');
        td.appendChild(s);
        tr.appendChild(td);
      }
    }

    const tdDel = document.createElement('td');
    const del = document.createElement('button');
    del.type = 'button';
    del.className = 'del';
    del.textContent = '削除';
    del.addEventListener('click', (e) => {
      e.stopPropagation(); // 行クリック選択を発火させない
      deleteRow(canvas, i + 1);
    });
    tdDel.appendChild(del);
    tr.appendChild(tdDel);

    tbody.appendChild(tr);
  });
}

function addRow(canvas: HTMLCanvasElement): void {
  if (rows.length === 0) {
    rows.push({ ea: newEdge('3.0'), eb: newEdge('3.0'), ec: newEdge('3.0'), parent: '-1', conn: '-1', extras: [] });
  } else {
    // デフォルトは「直前の行の B 辺に接続」。辺A は親の B 辺と同一の物理辺なので
    // 値を写すのではなく同じ辺 index を共有する (辺プール)
    const parent = rows[rows.length - 1];
    rows.push({ ea: parent.eb, eb: newEdge('3.0'), ec: newEdge('3.0'), parent: String(rows.length), conn: '1', extras: [] });
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
  const parentInput = input('newParent');
  const parentN = parentInput.value.trim() === ''
    ? (current > 0 ? current : rows.length)
    : parseInt(parentInput.value, 10);
  // 既接続辺には追加遷移を起こさない (selectEdge と同じ共通判定)
  const child = edgeOccupiedBy(parentN, side);
  if (child > 0) {
    setStatus(`⚠ 三角形 ${parentN} の ${side === 1 ? 'B' : 'C'} 辺 — 三角形 ${child} が接続済みのため追加できません`);
    return;
  }
  // B/C FAB は辺共有接続のショートカット (アプリ setB/setC と同じ) — 形態は辺共有に戻す
  select('newConn').value = String(side);
  select('newCType').value = '0';
  syncLcrDisabled('new');
  parentInput.value = String(parentN);
  const p = rows[parentN - 1];
  if (p) input('newA').value = edgeVal(p, side === 1 ? 'b' : 'c');
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
    // 共通関門: 不成立の値は state を汚す前に却下 (undo スナップも消費しない)
    const editBad = invalidTriangleReason(
      parseFloat(input('curA').value),
      parseFloat(input('curB').value),
      parseFloat(input('curC').value),
    );
    if (editBad) {
      setStatus(`⚠ 三角形 ${current}: ${editBad} — 書換えを中止`);
      return;
    }
    takeUndoSnap();
    const r = rows[current - 1];
    const cp = input('curParent').value.trim();
    const newParentVal = cp === '' ? '-1' : cp;
    // 接続 (parent/conn) が変わる場合は先に辺A の共有を張り直してから値を書く
    const curParts = readConnParts('cur');
    const newConnVal = curParts ? String(encodeConn(curParts)) : '-1';
    if (newParentVal !== r.parent || newConnVal !== r.conn) {
      r.parent = newParentVal;
      r.conn = newConnVal;
      relinkEdgeA(r);
    }
    writeCpExtras(r, curParts); // フロートの lcr 変更はコードに乗らない (列 17-19 が SoT)
    setEdgeVal(r, 'a', input('curA').value); // 共有辺なら親の B/C も同時に変わる (実体が 1 個)
    setEdgeVal(r, 'b', input('curB').value);
    setEdgeVal(r, 'c', input('curC').value);
    setName(r, input('curName').value);
    buildTable(canvas);
    syncForm();
    redraw(canvas);
    setStatus(`Rewrite Triangle ${current}`);
    return;
  }
  if (newC === '') return; // アプリと同じ: B だけでは何もしない (strAddLineC.isEmpty -> return)

  // Add (アプリ addTriangleBy 相当 — 行を足して CSV 再構築に任せる)
  let newParts = readConnParts('new');
  let conn = newParts ? String(encodeConn(newParts)) : '-1';
  let parent = input('newParent').value.trim();
  if (rows.length === 0) {
    // 最初の 1 個は必ず独立 (スマホ版同様、図面に 1 リスト)。新規行に独立の選択肢は無く、
    // 空リストへの追加だけが独立になる。残留プリセットによる「存在しない親」も同時に防ぐ
    newParts = null;
    conn = '-1';
    parent = '-1';
    input('newParent').value = '';
  }
  if (conn === '-1') {
    parent = '-1';
  } else if (parent === '') {
    parent = String(current > 0 ? current : rows.length);
  }
  const connCode = intOrNull(conn) ?? -1;
  // 親の実在関門: 三角不等式・占有と同列の追加前チェック。存在しない親への接続行は
  // WebCsvReader が skip するので、リストに居るのに描画されない行が生まれてしまう
  if (connCode >= 1) {
    const pn = parseInt(parent, 10);
    if (!(pn >= 1 && pn <= rows.length)) {
      setStatus(`⚠ 親番号 ${parent} の三角形が存在しません — 追加を中止`);
      return;
    }
  }
  let a = input('newA').value.trim();
  if ((conn === '1' || conn === '2') && a === '') {
    // 辺A 空のまま CSV に流すと WebCsvReader が行ごと skip する — 親の接続先辺長を写す
    const p = rows[parseInt(parent, 10) - 1];
    if (p) a = edgeVal(p, conn === '1' ? 'b' : 'c');
  }
  if (connCode >= 3 && a === '') {
    // 二重断面/フロートは子 A = 断面幅が独立値 (親辺長と一致しない) なので手入力必須
    setStatus('⚠ 二重断面/フロートは辺A (断面幅) の入力が必要です');
    input('newA').focus();
    return;
  }
  // 共通関門: 不成立の値は行を足す前に却下 (undo スナップも消費しない)
  const addBad = invalidTriangleReason(parseFloat(a), parseFloat(newB), parseFloat(newC));
  if (addBad) {
    setStatus(`⚠ 新規行: ${addBad} — 追加を中止`);
    return;
  }
  // 既接続辺への二重接続も行を足す前に却下 (selectEdge / autoConnection と同じ共通判定)
  if (conn === '1' || conn === '2') {
    const pn = parseInt(parent, 10);
    const child = edgeOccupiedBy(pn, conn === '1' ? 1 : 2);
    if (child > 0) {
      setStatus(`⚠ 三角形 ${pn} の ${conn === '1' ? 'B' : 'C'} 辺 — 三角形 ${child} が接続済みのため追加を中止`);
      return;
    }
  }
  takeUndoSnap();
  const name = input('newName').value;
  // 単純接続は親の辺 index を共有 (辺A = 親辺は同一の物理辺)、独立は自分の辺を作る
  const pRow = rows[parseInt(parent, 10) - 1];
  const ea =
    pRow && (conn === '1' || conn === '2') ? (conn === '1' ? pRow.eb : pRow.ec) : newEdge(a);
  const newRow: Row = { ea, eb: newEdge(newB), ec: newEdge(newC), parent, conn, extras: name !== '' ? [name] : [] };
  writeCpExtras(newRow, newParts); // 二重断面/フロートは列 17-19 にも cp を書く (lcr の SoT)
  rows.push(newRow);

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

// 行削除の共通処理 (行の削除ボタン / fabMinus の両動線)。番号の振り直しに合わせて
// 残存行の親参照も振り直す — これを怠ると「存在しない親に繋がる行」が残り、
// WebCsvReader が行ごと skip して『何を足しても描画されない』連鎖になる
// (2026-06-11 user 報告: 全削除→ペンシル/行追加で表示されない、の根因)
function deleteRow(canvas: HTMLCanvasElement, n: number): void {
  if (n < 1 || n > rows.length) return;
  takeUndoSnap();
  rows.splice(n - 1, 1);
  for (const r of rows) {
    const pn = intOrNull(r.parent);
    if (pn === null) continue;
    if (pn === n) {
      // 親を失った子は独立に落とす (現値を保ったまま辺A を自分の辺に切り出す)
      r.parent = '-1';
      r.conn = '-1';
      relinkEdgeA(r);
    } else if (pn > n) {
      r.parent = String(pn - 1); // 後続番号が 1 ずれるので追従
    }
  }
  shiftOverridesAfterDelete(n); // 番号が振り直されるので override も追従
  clearSelection(); // 番号が振り直されるので選択解除
  current = Math.min(n, rows.length); // 消した位置の次 (なければ末尾) をカレントに
  // 新規行フォームの残留プリセット (消えた三角形への親番号/接続) を掃除する
  const np = intOrNull(input('newParent').value.trim());
  if (np !== null && (np < 1 || np > rows.length)) {
    input('newParent').value = '';
    select('newConn').value = '-1';
    input('newA').value = '';
  }
  buildTable(canvas); // 番号が振り直されるので組み直し
  syncForm();
  redraw(canvas);
  setStatus(`Delete Triangle ${n}`);
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
  deleteRow(canvas, current > 0 ? current : rows.length);
}

// 回転 FAB (MainActivity.kt:1394-1400 → fabRotate(±5f)): 図形全体を原点 (0,0) 回りに 5° 刻みで
// 回す。実体は listAngle の加算 1 つで、幾何は redraw → wasm の recoverState が全再構築で適用する。
// アプリ同様 undo スナップは取らない (逆回転で戻せる)。view は据え置き (アプリも invalidate のみ)
function fabRotate(canvas: HTMLCanvasElement, degrees: number): void {
  if (rows.length === 0) return;
  listAngle += degrees;
  setStatus(`回転: ${listAngle}°`);
  redraw(canvas);
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

// 二重 FAB (アプリ fab_nijyuualign → rotateCurrentTriLCR、MainActivity.kt:1322 相当):
// カレント行の起点を 右→中央→左 で巡回する。アプリは in-place の連鎖書き換え
// (TriangleList.kt:488-513) だが、web は conn コードを書き換えて再構築するだけ —
// 毎描画全再構築 + 辺プールの設計の配当
function fabNijyuu(canvas: HTMLCanvasElement): void {
  const n = current > 0 ? current : 0;
  const r = rows[n - 1];
  if (!r) {
    setStatus('二重: 先に三角形を選択してください');
    return;
  }
  const p = connPartsOf(r);
  if (!p || p.type === 0) {
    // アプリのガード (子A=親辺長なら何もしない) と同義 — web では形態で明示されている
    setStatus('二重: 対象は二重断面/フロートの行のみ (形態列で変更できます)');
    return;
  }
  takeUndoSnap();
  p.lcr = p.lcr === 0 ? 2 : p.lcr - 1; // アプリ rotateLCR と同じ巡回: 右(2)→中央(1)→左(0)→右
  r.conn = String(encodeConn(p));
  writeCpExtras(r, p); // フロートの lcr はコードに乗らない (列 17-19 が SoT)
  buildTable(canvas);
  syncForm();
  redraw(canvas);
  setStatus(`二重: 三角形 ${n} の起点 → ${['左', '中央', '右'][p.lcr]}`);
}

// 新規作成 (段階2f): 現在の図を捨て、最初の 1 個 (独立 3.00/3.00/3.00) を生成して
// 全体フィットから始める (空画面でなく即編集に入れる形)。誤爆は confirm + undo 1 段で守る
function newDrawing(canvas: HTMLCanvasElement): void {
  if (rows.length > 0 && !window.confirm('現在の図を破棄して新規作成しますか?')) return;
  takeUndoSnap();
  headerLines = ['無題工事', '新規路線', '', ''];
  edges = [];
  rows = [{ ea: newEdge('3.00'), eb: newEdge('3.00'), ec: newEdge('3.00'), parent: '-1', conn: '-1', extras: [] }];
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
  el<HTMLButtonElement>('fabNijyuu').addEventListener('click', () => fabNijyuu(canvas));
  el<HTMLButtonElement>('fabReplace').addEventListener('click', () => fabReplace(canvas));
  el<HTMLButtonElement>('fabUndo').addEventListener('click', () => fabUndo(canvas));
  el<HTMLButtonElement>('fabMinus').addEventListener('click', () => fabMinus(canvas));
  // resetView FAB: 既存 dblclick fit と同じ動作のボタン化
  el<HTMLButtonElement>('fabResetView').addEventListener('click', () => {
    view = null;
    draw(canvas, lastPrims);
  });
  // 回転 FAB: アプリ fabs.xml 左上横列 [resetView][rot_l][rot_r] と同じ並び・同じ符号 (左=+5°)
  el<HTMLButtonElement>('fabRotL').addEventListener('click', () => fabRotate(canvas, 5));
  el<HTMLButtonElement>('fabRotR').addEventListener('click', () => fabRotate(canvas, -5));
  el<HTMLButtonElement>('fabUp').addEventListener('click', () => moveCurrent(canvas, -1));
  el<HTMLButtonElement>('fabDown').addEventListener('click', () => moveCurrent(canvas, 1));
}

// 新規行の Enter ナビゲーション (アプリの EditText imeOptions=actionNext 相当):
// Enter で隣のセルへ移動し、辺C で Enter すると新規三角形を確定して追加・再描画する。
// IME 変換確定の Enter (isComposing) は無視する (測点名の日本語入力を壊さない)
function wireNewRowEnter(canvas: HTMLCanvasElement): void {
  const moveTo = (toId: string) => {
    const t = document.getElementById(toId);
    if (t instanceof HTMLInputElement || t instanceof HTMLSelectElement) {
      t.focus();
      if (t instanceof HTMLInputElement) t.select();
    }
  };
  const chain: Array<[string, string]> = [
    ['newName', 'newA'],
    ['newA', 'newB'],
    ['newB', 'newC'],
    ['newParent', 'newConn'],
  ];
  for (const [from, to] of chain) {
    input(from).addEventListener('keydown', (e) => {
      if (e.key !== 'Enter' || e.isComposing) return;
      e.preventDefault();
      moveTo(to);
    });
  }
  input('newC').addEventListener('keydown', (e) => {
    if (e.key !== 'Enter' || e.isComposing) return;
    e.preventDefault();
    if (input('newB').value.trim() === '') {
      // B 空のまま fabReplace を呼ぶと「カレント行の書換え」分岐に入ってしまうので先に弾く
      moveTo('newB');
      setStatus('辺B が空です — B を入力してから C のエンターで追加確定');
      return;
    }
    const before = rows.length;
    fabReplace(canvas);
    if (rows.length > before) moveTo('newB'); // 追加成功 → 次の入力へ (連続追加の流れ)
  });
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
  const aStr = edgeVal(p, edgeSel.side === 1 ? 'b' : 'c');
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

// 二重断面接続 (完全形式 CSV の cParam: type≠0 か lcr≠Center) は子 A ≠ 親辺長が正規の
// 状態なので、辺プールでは親と束ねず自分の辺を持つ —
// extras[12]=cp.type (列18), extras[13]=cp.lcr (列19)
function isSimpleConnection(r: { extras: string[] }): boolean {
  const cpType = r.extras[12];
  const cpLcr = r.extras[13];
  return (cpType === undefined || cpType === '' || cpType === '0') &&
    (cpLcr === undefined || cpLcr === '' || cpLcr === '2');
}

// 接続 (parent/conn) が編集で変わった時に辺A の共有先を張り直す。
// 単純接続 → 親の B/C と同じ辺を共有、独立/二重断面 → 現値を複製して自分の辺に切り出す
function relinkEdgeA(r: Row): void {
  const pn = intOrNull(r.parent) ?? -1;
  const conn = intOrNull(r.conn);
  const p = rows[pn - 1];
  if (p && p !== r && (conn === 1 || conn === 2) && isSimpleConnection(r)) {
    r.ea = conn === 1 ? p.eb : p.ec;
  } else {
    r.ea = newEdge(edges[r.ea] ?? '');
  }
}

// 同じ辺を表示している他の入力欄を追従させる (データは edges に 1 個、表示だけ複数)。
// 編集中のセルはフォーカスを壊さないため except で除く
function syncEdgeInputs(ei: number, except?: HTMLInputElement): void {
  document.querySelectorAll<HTMLInputElement>(`#triRows input[data-edge="${ei}"]`).forEach((o) => {
    if (o !== except) o.value = fmt2(edges[ei]);
  });
  syncForm(); // カレント行のフォーム表示も追従
}

// 既に子が接続されている辺か (parent=tri, conn=side の行があるか)。
// 0 = 未接続、>0 = 接続中の子三角形の番号。タップ選択・B/C FAB・追加実行の
// 3 動線が共通で使う (アプリは占有判定を持たない — web 版の改善, 2026-06-11 user 指示)
function edgeOccupiedBy(tri: number, side: 1 | 2): number {
  return rows.findIndex((r) => intOrNull(r.parent) === tri && intOrNull(r.conn) === side) + 1;
}

// 辺タップ (アプリ targetInTriMode → autoConnection(lastTapSide) 相当):
// FAB を押さなくても新規行に接続・親・辺A がプリセットされ、シャドーが出る。
// ただし既接続辺は追加遷移 (プリセット+シャドー+フォーカス) を起こさず、選択と W/H 対象のみ
function selectEdge(canvas: HTMLCanvasElement, tri: number, side: 1 | 2): void {
  const child = edgeOccupiedBy(tri, side);
  if (child > 0) {
    selected = tri;
    current = tri;
    selectedDim = { tri, side };
    edgeSel = null;
    shadowPrims = null;
    updateRowHighlight();
    syncForm();
    draw(canvas, lastPrims);
    setStatus(`三角形 ${tri} の ${side === 1 ? 'B' : 'C'} 辺 — 三角形 ${child} が接続済み (追加不可、W/H・旗は可)`);
    return;
  }
  selected = tri;
  current = tri;
  // アプリの lastTapSide と同じく、タップした辺はそのまま W/H フリップの対象にもなる
  selectedDim = { tri, side };
  input('newParent').value = String(tri);
  // 辺タップは辺共有接続のプリセット (シャドーも辺共有の形) — 形態は辺共有に戻す
  select('newConn').value = String(side);
  select('newCType').value = '0';
  syncLcrDisabled('new');
  const p = rows[tri - 1];
  if (p) input('newA').value = fmt2(edgeVal(p, side === 1 ? 'b' : 'c'));
  edgeSel = { tri, side };
  buildShadow();
  updateRowHighlight();
  syncForm();
  draw(canvas, lastPrims);
  setStatus(`三角形 ${tri} の ${side === 1 ? 'B' : 'C'} 辺 — 新規行にプリセット済み。B/C を入力して ✎ で追加`);
  input('newB').focus();
}

// 寸法ダブルクリック → 一覧の該当セル (辺A/B/C の input) にフォーカスして直接編集へ。
// 一覧の input は input/change → redraw() まで配線済みなので、フォーカスを移すだけで
// 「書き換えが図に反映される」(不成立値は redraw の共通関門が図を据え置いて警告)
function focusListCell(canvas: HTMLCanvasElement, tri: number, side: number): void {
  selectTriangle(canvas, tri);
  // どの辺を編集中かは図側でも黄色強調 (辺線 + 寸法ボックス + 辺名) で見え続けるようにする
  selectedDim = { tri, side };
  draw(canvas, lastPrims);
  const details = document.getElementById('listDetails') as HTMLDetailsElement | null;
  if (details) details.open = true;
  const tr = document.getElementById('triRows')?.children[tri - 1];
  if (!tr) return;
  // 行内の number input は [辺A, 辺B, 辺C, 親番号] の生成順 — side 0/1/2 がそのまま添字
  const inp = tr.querySelectorAll<HTMLInputElement>('input[type="number"]')[side];
  if (!inp) return;
  inp.focus();
  inp.select();
  inp.scrollIntoView({ block: 'nearest' });
  setStatus(`三角形 ${tri} の ${['A', 'B', 'C'][side]} 辺 — セルを直接編集 (入力すると図に即反映)`);
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

  // ダブルクリック: 寸法値の上なら一覧の該当セルへフォーカス (直接書き換え動線)、
  // それ以外は従来どおり全体 fit に戻す
  canvas.addEventListener('dblclick', (e) => {
    const p = canvasPx(canvas, e);
    const dim = nearestDimText(p.x, p.y);
    if (dim && dim.prim.tri !== undefined && dim.prim.side !== undefined) {
      focusListCell(canvas, dim.prim.tri, dim.prim.side);
      return;
    }
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
  // ADR 0008: CSV も overrides 焼き込み (完全形式 28 列) + Shift-JIS で書く —
  // アプリの保存 (writeCSV) と同列・同エンコーディングなので、アプリで開いても
  // W/H フリップ・番号移動・日本語測点名が失われない
  document.getElementById('saveCsv')?.addEventListener('click', () => {
    exportFile('CSV', 'triangles.csv', () =>
      toSjisBlob(buildCsvTextWithOverrides(serializeState(), overridesJson())),
    );
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
  // 読込動線も共通関門に通す: 読込自体は止めない (既存ファイルは開けるべき) が、
  // 不成立行があれば編集前に気づけるよう警告だけ重ねる
  const bad = findInvalidRow(rows);
  if (bad) setStatus(`⚠ 読込: 三角形 ${bad.n} は ${bad.reason}`);
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
  wireNewRowEnter(canvas);
  // 形態 select の変化で起点 select の有効/無効を追従 (辺共有では起点に意味が無い)
  select('newCType').addEventListener('change', () => syncLcrDisabled('new'));
  select('curCType').addEventListener('change', () => syncLcrDisabled('cur'));
  syncLcrDisabled('new');

  fileInput.addEventListener('change', () => {
    const file = fileInput.files?.[0];
    if (!file) return;
    // アプリの CSV は Shift-JIS、web 旧版の CSV は UTF-8。strict UTF-8 で試して
    // 化けるバイト列なら Shift-JIS として読み直す (アプリ loadFileWithEncoding の web 版)
    void file.arrayBuffer().then((buf) => {
      let text: string;
      try {
        text = new TextDecoder('utf-8', { fatal: true }).decode(buf);
      } catch {
        text = new TextDecoder('shift_jis').decode(buf);
      }
      overrides = { dims: [], numbers: [] }; // 新しいファイルに前データの override を持ち越さない
      loadCsv(canvas, text, file.name);
      autosave(); // 読み込んだファイルも次回リロードで復元できるように
    });
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
    // fabs: FAB の実測 DOM 矩形。canvas 外のレイアウト (配置順・間隔) を CLI から
    // 数値検証する口 (capture は canvas.toDataURL なので DOM が写らない)
    const fabs = Array.from(document.querySelectorAll<HTMLButtonElement>('.fabcol button')).map((b) => {
      const r = b.getBoundingClientRect();
      return { id: b.id, x: Math.round(r.x), y: Math.round(r.y), w: Math.round(r.width), h: Math.round(r.height) };
    });
    // rows は CP 消費側の互換のため a/b/c 展開形で出す (内部は辺 index)。edges も生で添える
    const rowsExpanded = rows.map((r) => ({
      a: edgeVal(r, 'a'),
      b: edgeVal(r, 'b'),
      c: edgeVal(r, 'c'),
      ea: r.ea,
      eb: r.eb,
      ec: r.ec,
      parent: r.parent,
      conn: r.conn,
      extras: r.extras,
    }));
    hot.send('tlcp:state-res', {
      id: data.id,
      state: {
        rows: rowsExpanded, edges, selected, current, listAngle, overrides,
        csv: serializeState(),
        // 保存ボタンが書く実物 (overrides 焼き込み済み完全形式)。書き戻しの検証口
        csvBaked: buildCsvTextWithOverrides(serializeState(), overridesJson()),
        view, prims: lastPrims, fabs,
      },
    });
  });
  // キー注入: 要素に値をセットして keydown を流す (Enter ナビ等のキーボード UX の検証口)。
  // 合成イベントはブラウザ既定動作を起こさないが、こちらの handler は preventDefault 前提なので等価
  hot.on('tlcp:key-req', (data: { id: string; target?: string; key?: string; value?: string }) => {
    const t = document.getElementById(String(data.target ?? ''));
    if (t instanceof HTMLInputElement || t instanceof HTMLSelectElement) {
      if (typeof data.value === 'string') t.value = data.value;
      t.focus();
      if (data.key) t.dispatchEvent(new KeyboardEvent('keydown', { key: data.key, bubbles: true, cancelable: true }));
      hot.send('tlcp:key-res', {
        id: data.id,
        state: { ok: true, active: document.activeElement?.id ?? '', rows: rows.length },
      });
    } else {
      hot.send('tlcp:key-res', { id: data.id, state: { ok: false } });
    }
  });
  // クリック注入: ボタン UX (FAB・削除等) の動線を CLI から踏む口
  hot.on('tlcp:click-req', (data: { id: string; target?: string }) => {
    const t = document.getElementById(String(data.target ?? ''));
    if (t instanceof HTMLElement) {
      t.click();
      hot.send('tlcp:click-res', {
        id: data.id,
        state: { ok: true, rows: rows.length, status: document.getElementById('status')?.textContent ?? '' },
      });
    } else {
      hot.send('tlcp:click-res', { id: data.id, state: { ok: false } });
    }
  });
  // CSV 注入: ファイルダイアログを経ずに CSV を開く (loadCsv と同じ動線)。
  // 完全形式 CSV (手動配置列 7-9/11-16/20-21、golden 比較) の e2e 検証口
  hot.on('tlcp:load-req', (data: { id: string; csv?: string }) => {
    const canvas = document.getElementById('cv') as HTMLCanvasElement | null;
    if (canvas && typeof data.csv === 'string' && data.csv.trim() !== '') {
      overrides = { dims: [], numbers: [] }; // ファイル開き (fileInput change) と同じ: 前データの override を持ち越さない
      loadCsv(canvas, data.csv, 'cp-load');
      hot.send('tlcp:load-res', { id: data.id, state: { ok: true, rows: rows.length, listAngle } });
    } else {
      hot.send('tlcp:load-res', { id: data.id, state: { ok: false } });
    }
  });
  // 編集注入: 一覧セル編集と同じ動線 (row 書換え → redraw) を踏む。
  // 三角不等式の共通関門が「図を据え置いて警告する」ことを CLI から実走検証する口
  hot.on('tlcp:edit-req', (data: { id: string; tri?: number; key?: string; value?: string }) => {
    const canvas = document.getElementById('cv') as HTMLCanvasElement | null;
    const r = typeof data.tri === 'number' ? rows[data.tri - 1] : undefined;
    if (canvas && r && (data.key === 'a' || data.key === 'b' || data.key === 'c') && typeof data.value === 'string') {
      const primsBefore = lastPrims;
      setEdgeVal(r, data.key, data.value); // 共有辺なら相手側も同時に変わる (実体 1 個)
      syncEdgeInputs(r[EKEY[data.key]]);
      redraw(canvas);
      hot.send('tlcp:edit-res', {
        id: data.id,
        state: {
          ok: true,
          rerendered: lastPrims !== primsBefore, // 関門が通れば新 prims、弾かれれば据え置き
          status: document.getElementById('status')?.textContent ?? '',
        },
      });
    } else {
      hot.send('tlcp:edit-res', { id: data.id, state: { ok: false } });
    }
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
