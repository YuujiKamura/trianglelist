// TriangleList Web 段階1 — Canvas 2D シェル (insight #61)。
// 座標計算・寸法配置・番号サークル位置は全部 Kotlin (wasmJs) 側で済んでいて、
// ここは JSON プリミティブを素朴に描くだけ。
// y 軸はモデル座標 (上向き) → 画面座標 (下向き) に反転する (MyView.makePath の -y と同じ向き)。

type LinePrim = { type: 'line'; layer: string; x1: number; y1: number; x2: number; y2: number; ded?: number; tri?: number; side?: number };
type TextPrim = {
  type: 'text';
  layer: string;
  text: string;
  x: number;
  y: number;
  angle: number; // 度数法、モデル座標系で CCW (PointXY.calcDimAngle 由来)
  size: number; // モデル単位の文字高さ
  align: number; // DXF 垂直コード: 1=点が文字の下端, 2=中央, 3=点が文字の上端
  alignH?: number; // 水平: 0=左寄せ (控除の infoStr、DXF writeTextAndLine 準拠)。省略時は中央
  // 段階2e: 識別 + 現在実効値 (dim テキストのみ tri/side/h/v、番号テキストは tri のみ)
  tri?: number;
  side?: number;
  h?: number; // horizontal 実効値 (0..4、>2 で旗揚げ)。W cycle はここから次値を計算
  v?: number; // vertical 実効値 (1=外/3=内)
  ded?: number; // 控除番号 (ded layer のみ)。選択中の控除の色替えに使う
  field?: string; // 図面枠タイトル欄の識別子 (frame layer のみ、WebFrame.fieldAt)。空欄でも prim が出る
};
type CirclePrim = { type: 'circle'; layer: string; cx: number; cy: number; r: number; tri?: number; ded?: number };
// Rectangle 専用メタ: 描画対象外 (perpFrom 等の識別子)。 main.ts:384 で skip される
type MetaPrim = { type: 'meta'; kind: 'perp'; tri: number; perpFrom: 'bl' | 'tl' };
// 三角形の塗り (アプリ MyView.drawEntities:572-576 の写し)。color は CSV 列 10 の index、
// 実色は FILL_PALETTE が解決する
type FillPrim = {
  type: 'fill';
  layer: string;
  x1: number;
  y1: number;
  x2: number;
  y2: number;
  x3: number;
  y3: number;
  color: number;
  tri?: number;
};
type Prim = LinePrim | TextPrim | CirclePrim | FillPrim | MetaPrim;

// Kotlin wasmJs の ESM glue を静的 import してバンドルに乗せる (Vite の正規 module graph)。
// .wasm 本体は uninstantiated.mjs が fetch('./TriangleList-common-wasm-js.wasm') =
// document base 相対で読むため、sync-wasm が web/public/ 直下に置く
import {
  renderCsvToPrimitivesWithOverrides,
  buildDxfTextNumReverse,
  buildSfcTextNumReverse,
  buildCsvTextWithOverrides,
  hitTriangle,
  placeDeduction,
  renderFrame,
  renderFrameWithMargin,
  rotateDeductionLine,
  rotateDeductionShape,
} from '../wasm/TriangleList-common-wasm-js.mjs';
// DXF/SFC は既存 app の出力と同じ Shift_JIS。ブラウザ標準 TextEncoder は UTF-8 専用なので
// encoding-japanese (MIT, polygonplanet/encoding.js) で Unicode → SJIS バイト化する
import Encoding from 'encoding-japanese';
import { safeOpenUrl } from './url-safety';

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

// すべての色定数を 1 箇所に集中させる (user 2026-06-14「別々に色管理するな、統一管理しろ」)。
// 同じ色を 2 つ以上の名前で持たないこと — 例えば「shadow ラベルのオレンジ」と
// 「選択中の控除のオレンジ」は同じ #e67e22 なので accentOrange 一本。
const COLORS: Record<string, string> = {
  // base レイヤ (prim.layer 名と一致)
  tri: '#222222',
  dim: '#1a7a1a',
  num: '#1f5fbf',
  guide: '#9aa0a6', // 補助線 (台形の延長=垂線ガイド等) — 点線で薄いグレー
  ra: '#555555',   // 直角マーカー (垂線起点の小正方形) — 実線細い
  ded: '#c0392b', // 控除 = 赤系 (アプリ/DXF の RED 準拠)
  frame: '#8a8a8a', // 図面枠の line (= 罫線) = 薄グレー、 図形より控えめ
  frameText: '#000000', // 図面枠の text (= タイトル / cell 値 / url) = 黒、 読みやすく濃く
  // 選択/オーバーレイ系 (UI 状態を示す)
  selectFill: 'rgba(31, 95, 191, 0.25)', // 選択三角形の塗り (半透明青)
  selectYellow: '#e6b800', // 選択辺・番号リング・寸法ボックス (app paintYellow 相当)
  accentOrange: '#e67e22', // 仮表示 (shadow ラベル / 選択中の控除) — 黄とは別の「注目色」
  haloDark: '#5c4a00', // halo (暗縁取り) — shadow ラベルと選択辺 ABC 注記の共通輪郭
  shadowFill: 'rgba(128, 128, 128, 0.35)', // シャドー三角形/台形のグレー塗り
};

// 三角形の塗りパレット (index = CSV 列 10 = Triangle.mycolor、既定 4)。
// 色値はアプリの resColors (MainActivity.kt:218-224 → res/values/colors.xml:9-14) と同一 —
// FAB に出る「ユーザーが認識する色」。アプリの画面塗り (MyView.darkColors_:71-75) は
// 黒背景向けの暗色版で、白背景の web ではこの明色版が同じ見え方になる
const FILL_PALETTE = [
  '#FF99AA', // 0 colorPink
  '#FFAA99', // 1 colorOrange
  '#FFFF99', // 2 colorYellow
  '#AAFFAA', // 3 colorLime
  '#88CCFF', // 4 colorSky (既定)
];

function setStatus(msg: string): void {
  const el = document.getElementById('status');
  if (!el) return;
  el.textContent = msg;
  // ⚠ 始まり = 警告 (三角形不成立等)。ヘッダ上で目に入るよう色を変える
  el.style.color = msg.startsWith('⚠') ? '#ffb3b3' : '';
  el.style.fontWeight = msg.startsWith('⚠') ? '700' : '';
}

// ヘッダ右上のビルド識別を埋める。値は vite.config.ts の define でビルド時に焼き込んだ
// コミット ID と生成時刻 (JST)。Pages は手動デプロイなので、これで反映済みか一目で分かる。
{
  const bi = document.getElementById('buildInfo');
  if (bi) bi.textContent = `${__BUILD_COMMIT__} · ${__BUILD_TIME__}`;
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
    if (p.type === 'meta') continue; // meta は識別子のみ、 座標を持たない
    if (p.type === 'line') {
      acc(p.x1, p.y1);
      acc(p.x2, p.y2);
    } else if (p.type === 'circle') {
      acc(p.cx - p.r, p.cy - p.r);
      acc(p.cx + p.r, p.cy + p.r);
    } else if (p.type === 'fill') {
      // 頂点は辺 line と同じ点なので bounds には効かないが、型として明示的に扱う
      acc(p.x1, p.y1);
      acc(p.x2, p.y2);
      acc(p.x3, p.y3);
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
// 台形の辺をタップして「ここに三角形を乗せる」待ち状態 (三角形モード)。edgeSel (三角形親) を汚さず
// 別に持つ — 追加経路 fabReplace の三角形親ガードを避け、RectChild 行を直に作る。trap=台形群index(1始まり)、
// side=getLine 側(1=左脚/2=上辺/3=右脚)。シャドーは三角形を台形辺に乗せた形で出る。
let pendingTrapParent: { parent: number; side: number } | null = null;

// ---- 控除 (Deduction) 編集 (アプリ deductionMode = flipDeductionMode:980 の web 版) ----
// SoT は CSV 行そのもの (13 列 "Deduction,..." 文字列の配列)。幾何 (配置・親判定・旗揚げ・
// 回転) は全部 common (WebDeduction / CsvCodec.buildDeductions) — TS は行の出し入れだけ。
// dedCursor = 控除モード中のキャンバスクリック位置 (アプリ myview.pressedInModel 相当、
// モデル座標 y 上向き)。アプリはタップ位置が見えないが、web は十字マーカーで可視化する
let dedLines: string[] = [];
let deductionMode = false;
let dedCursor: { x: number; y: number } | null = null;
let dedSelected = 0; // 選択中の控除番号 (1-based、0 = 非選択)

// ---- 台形 (Rectangle) 図形 (混在リスト段1+2: trap-design.md v2 の確定仕様) ----
// v1 の別state (trapLines) は廃止。台形は三角形と同じ rows[] に kind='rectangle' で混在する
// (trap-design.md「混在リストが土台」)。CSV contract は不変: "Rectangle,num,length,widthA,
// widthB,parent,side"。length=延長(辺B), widthA=底辺(辺A), widthB=上辺(辺C),
// parent=接続先三角形番号(-1=独立), side=接続辺(1=B,2=C, 独立=0)。common の buildRectangles
// (CsvCodec.kt:240) はこの行を読んで描く — web は CSV を出すだけ。三角形の出力は1ビット不変。
//
// 不変条件: rows[] は「三角形が連続 prefix + 台形が suffix」を常に保つ。これにより
// 既存コードの rows[n-1]==三角形n という前提 (current/selected/親探索/シャドー) が壊れず、
// 台形ゼロのリストでは全経路がバイト同一 = golden 不変。add は kind で splice 位置を分け、
// parse は台形行を三角形の後に積む。
// 新規入力で「追加」が作る図形種別。FAB fabFigureKind でトグル、syncForm がラベルを切替える
let figureKind: 'triangle' | 'rectangle' = 'triangle';

// Deduction CSV 行の表示用パース (列順は MainActivity.writeCSV:2795)。座標列 8/9 は
// モデル座標 (y 上向き、保存時に Y 反転済みの値) なのでそのまま hit/マーカーに使える
type DedView = { num: number; name: string; lenX: string; lenY: string; pn: string; type: string; x: number; y: number; fx: number; fy: number };

function parseDedLine(line: string): DedView | null {
  const c = line.split(',').map((s) => s.trim());
  if (c[0] !== 'Deduction' || c.length < 13) return null;
  return {
    num: intOrNull(c[1]) ?? 0,
    name: c[2] ?? '',
    lenX: c[3] ?? '',
    lenY: c[4] ?? '',
    pn: c[5] ?? '0',
    type: c[6] ?? '',
    x: parseFloat(c[8] ?? ''),
    y: parseFloat(c[9] ?? ''),
    fx: parseFloat(c[10] ?? ''),
    fy: parseFloat(c[11] ?? ''),
  };
}

// 削除・並び替え後の番号振り直し (DeductionList.remove:122-131 と同じ「詰めて renum」)
function renumberDedLines(): void {
  dedLines = dedLines.map((line, i) => {
    const c = line.split(',');
    if (c[0]?.trim() === 'Deduction' && c.length >= 2) c[1] = String(i + 1);
    return c.join(',');
  });
}

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
  // マージンは画面サイズ比で取る (固定 px だと大画面で「目一杯」になる)。
  // 片側 12% → 図形は短辺の約 3/4 に収まる (CAD の zoom-extents padding の感覚)
  const margin = Math.min(canvas.width, canvas.height) * 0.12;
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

// 暗縁取り (halo) + 塗りで「下地がグレー shadow / 既存図面のどんな色」でも文字が浮く描画。
// 選択辺の A/B/C 注記 (drawSelectedDim) と シャドーの 上辺/延長 ラベル で共通利用 (user 2026-06-14
// 「ばらけさせるな」)。caller は事前に font / textAlign / textBaseline / translate+rotate を設定する。
function haloText(ctx: CanvasRenderingContext2D, text: string, x: number, y: number, halo: string, fill: string): void {
  ctx.strokeStyle = halo;
  ctx.lineWidth = 3;
  ctx.lineJoin = 'round';
  ctx.strokeText(text, x, y);
  ctx.fillStyle = fill;
  ctx.fillText(text, x, y);
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

  // 三角形の塗り (アプリ MyView.drawEntities:572-576 と同じく「塗り全部 → 線・文字」の z-order)。
  // 選択ハイライトより下に敷くので最初に描く
  for (const p of prims) {
    if (p.type !== 'fill') continue;
    ctx.fillStyle = FILL_PALETTE[p.color] ?? FILL_PALETTE[4];
    ctx.beginPath();
    ctx.moveTo(sx(p.x1), sy(p.y1));
    ctx.lineTo(sx(p.x2), sy(p.y2));
    ctx.lineTo(sx(p.x3), sy(p.y3));
    ctx.closePath();
    ctx.fill();
  }

  // 選択図形の塗り (線より下に敷く)。各 line は WebPrimitiveRenderer から tri/side 識別子付きで
  // 出るので、selected 番号で filter して chainPath で外周を chain 連結する。
  // 三角形 (3 辺、 連続 chain) も台形 (4 辺、 段3 swap 後は side 0/2 並列 + 1/3 脚で非連続) も
  // chainPath で 1 本のパスに連結すれば「順に lineTo で対角線が走り砂時計 X クロス」を回避できる。
  const triLines = prims.filter((p): p is LinePrim => p.type === 'line' && p.layer === 'tri');
  if (selected > 0) {
    const own = triLines.filter((l) => l.tri === selected);
    if (own.length >= 3) {
      const path = chainPath(own);
      if (path.length >= 3) {
        ctx.fillStyle = COLORS.selectFill;
        ctx.beginPath();
        ctx.moveTo(sx(path[0].x), sy(path[0].y));
        for (let i = 1; i < path.length; i++) ctx.lineTo(sx(path[i].x), sy(path[i].y));
        ctx.closePath();
        ctx.fill();
      }
    }
  }

  for (const p of prims) {
    if (p.type === 'fill') continue; // 塗りは最初のパスで描画済み
    if (p.type === 'meta') continue; // meta prim は描画対象外 (perpFrom 識別子等の内部情報のみ)
    // 選択中の控除はプリミティブごと色替え (user 指定: 明示的に色替えして見分ける)
    const isSelectedDed = p.layer === 'ded' && dedSelected > 0 && p.ded === dedSelected;
    const isFrameText = p.type === 'text' && p.layer === 'frame';
    const color = isSelectedDed ? COLORS.accentOrange : isFrameText ? COLORS.frameText : (COLORS[p.layer] ?? '#000000');
    if (p.type === 'line') {
      ctx.strokeStyle = color;
      ctx.lineWidth = isSelectedDed ? 2 : p.layer === 'tri' ? 1.5 : 1;
      // 補助線 (guide) は点線。台形の延長=垂線ガイドなど「実辺でない寸法線」を実辺と見分ける
      if (p.layer === 'guide') ctx.setLineDash([4, 3]);
      ctx.beginPath();
      ctx.moveTo(sx(p.x1), sy(p.y1));
      ctx.lineTo(sx(p.x2), sy(p.y2));
      ctx.stroke();
      if (p.layer === 'guide') ctx.setLineDash([]);
    } else if (p.type === 'circle') {
      ctx.strokeStyle = color;
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.arc(sx(p.cx), sy(p.cy), p.r * s, 0, Math.PI * 2);
      ctx.stroke();
    } else {
      ctx.fillStyle = color;
      ctx.font = `${p.size * s}px sans-serif`;
      // alignH=0 (控除 infoStr、DXF writeTextAndLine の左寄せ) 以外は従来どおり中央
      ctx.textAlign = p.alignH === 0 ? 'left' : 'center';
      // frame layer は CAD 標準センタリング (= AutoCAD 73=2 / SXF 中心点) = glyph 物理 bbox を観て
      // anchor に揃える。 canvas の textBaseline='middle'/'top' は em-box ベースで漢字や英数字の
      // descender 空き分が ずれて見える、 measureText で actualBoundingBox{Ascent,Descent} を取り
      // baseline シフトで補正。 align=2 (middle) は glyph 中央 → anchor、 align=3 (top) は
      // glyph 物理上端 → anchor (= 例: tCredit url を外枠下辺ぴったりの下に物理整列)。
      const isFrameGlyph = isFrameText && (p.align === 2 || p.align === 3);
      if (isFrameGlyph) {
        ctx.textBaseline = 'alphabetic';
        const m = ctx.measureText(p.text);
        const shift = p.align === 2
          ? (m.actualBoundingBoxAscent - m.actualBoundingBoxDescent) / 2 // glyph 中央 → anchor
          : m.actualBoundingBoxAscent; // glyph 上端 → anchor (baseline は anchor の ascent 下)
        ctx.save();
        ctx.translate(sx(p.x), sy(p.y) + shift);
        ctx.rotate((-p.angle * Math.PI) / 180);
        ctx.fillText(p.text, 0, 0);
        ctx.restore();
      } else {
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

  // 段階2g: シャドー三角形 (グレー塗り + B/C ラベル、MyView.drawShadowTriangle と同じ見せ方)。
  // ラベルの値は新規入力行の B/C (アプリの watchedB1_/watchedC1_ 相当) を都度読む
  if (shadowPrims && shadowPrims.length === 3) {
    const [sa, sbLine, scLine] = shadowPrims;
    ctx.fillStyle = COLORS.shadowFill;
    ctx.beginPath();
    ctx.moveTo(sx(sa.x1), sy(sa.y1));
    ctx.lineTo(sx(sa.x2), sy(sa.y2));
    ctx.lineTo(sx(sbLine.x2), sy(sbLine.y2));
    ctx.closePath();
    ctx.fill();
    ctx.fillStyle = COLORS.accentOrange;
    // 寸法テキストと同じスケール感 (モデル単位の文字高さ × ズーム倍率) に合わせる
    const dimSize =
      lastPrims.find((q): q is TextPrim => q.type === 'text' && q.layer === 'dim')?.size ?? 0.25;
    ctx.font = `${dimSize * s}px sans-serif`;
    ctx.textAlign = 'center';
    // 入力中の値をそのまま表示 (アプリ watchedB1_/watchedC1_ と同じくタイプに追従。
    // 再描画は newB/newC の input イベントが起こす — wireNewRowEnter)
    const bv = (document.getElementById('newB') as HTMLInputElement | null)?.value ?? '';
    const cv = (document.getElementById('newC') as HTMLInputElement | null)?.value ?? '';
    // ラベルは他の寸法値と同じく辺に沿って傾ける (2026-06-12 user 要望)。
    // 上下が逆さにならないよう ±90° に正規化し、線から少し浮かせて重なりを避ける
    const edgeLabel = (line: LinePrim, label: string) => {
      const x1 = sx(line.x1);
      const y1 = sy(line.y1);
      const x2 = sx(line.x2);
      const y2 = sy(line.y2);
      let ang = Math.atan2(y2 - y1, x2 - x1);
      if (ang > Math.PI / 2) ang -= Math.PI;
      else if (ang < -Math.PI / 2) ang += Math.PI;
      ctx.save();
      ctx.translate((x1 + x2) / 2, (y1 + y2) / 2);
      ctx.rotate(ang);
      ctx.textBaseline = 'bottom';
      // halo (暗縁取り) + 塗り = 共通の haloText (選択辺 ABC 注記と同じ視認パターン、user 2026-06-14)
      haloText(ctx, label, 0, -3, COLORS.haloDark, COLORS.accentOrange);
      ctx.restore();
    };
    edgeLabel(sbLine, `B ${bv}`.trim());
    edgeLabel(scLine, `C ${cv}`.trim());
  }

  // 段5 R6: 台形シャドー (4 辺、グレー塗り + 延長/上辺ラベル)。台形モードで辺タップしたとき
  // buildShadow が末尾 4 辺を入れる。三角形シャドー (length===3) は上で処理済み・不変。
  if (shadowPrims && shadowPrims.length === 4) {
    ctx.fillStyle = COLORS.shadowFill;
    ctx.beginPath();
    // 段3 swap (commit 659b509) 以降、 4 辺 line の出力順は「side 0=底/1=右脚/2=上/3=左脚」の
    // 並列で出るため「前の終点 = 次の始点」 が成立しない (旧 renderRectangle は連続 chain で出して
    // いた)。 そのまま順に lineTo すると A→B→C→D の対角線が走り砂時計 X クロスに見えるため、
    // 端点グラフで chain 連結して外形パスを作り直す。
    const path = chainPath(shadowPrims);
    if (path.length >= 4) {
      ctx.moveTo(sx(path[0].x), sy(path[0].y));
      for (let i = 1; i < path.length; i++) ctx.lineTo(sx(path[i].x), sy(path[i].y));
    } else {
      ctx.moveTo(sx(shadowPrims[0].x1), sy(shadowPrims[0].y1));
      for (const ln of shadowPrims) ctx.lineTo(sx(ln.x2), sy(ln.y2));
    }
    ctx.closePath();
    ctx.fill();
    ctx.fillStyle = COLORS.accentOrange;
    const dimSize =
      lastPrims.find((q): q is TextPrim => q.type === 'text' && q.layer === 'dim')?.size ?? 0.25;
    ctx.font = `${dimSize * s}px sans-serif`;
    ctx.textAlign = 'center';
    const bv = input('newB').value;
    const cv = input('newC').value;
    const edgeLabel = (line: LinePrim, label: string) => {
      const x1 = sx(line.x1);
      const y1 = sy(line.y1);
      const x2 = sx(line.x2);
      const y2 = sy(line.y2);
      let ang = Math.atan2(y2 - y1, x2 - x1);
      if (ang > Math.PI / 2) ang -= Math.PI;
      else if (ang < -Math.PI / 2) ang += Math.PI;
      ctx.save();
      ctx.translate((x1 + x2) / 2, (y1 + y2) / 2);
      ctx.rotate(ang);
      ctx.textBaseline = 'bottom';
      // halo (暗縁取り) + 塗り = 共通の haloText (選択辺 ABC 注記と同じ視認パターン、user 2026-06-14)
      haloText(ctx, label, 0, -3, COLORS.haloDark, COLORS.accentOrange);
      ctx.restore();
    };
    edgeLabel(shadowPrims[2], `上辺 ${cv}`.trim()); // 上辺 (side 2)
    // 延長 (= 台形高さ rect.height) のラベルは「短辺起点から立てた垂線」上に出す。
    // shadowPrims[1] (= 左脚 bl→tl) は align ≠ 0 で斜辺になるため、そこに「延長」を貼ると
    // 「斜辺上に延長ラベル」になる (user 指摘 2026-06-14)。
    // Rectangle 規約: side 0=br->bl, 1=bl->tl, 2=tl->tr, 3=tr->br
    const br = { x: shadowPrims[0].x1, y: shadowPrims[0].y1 };
    const bl = { x: shadowPrims[0].x2, y: shadowPrims[0].y2 };
    const tl = { x: shadowPrims[2].x1, y: shadowPrims[2].y1 };
    const tr = { x: shadowPrims[2].x2, y: shadowPrims[2].y2 };
    const wA = Math.hypot(br.x - bl.x, br.y - bl.y);
    const wB = Math.hypot(tr.x - tl.x, tr.y - tl.y);
    const bottomShorter = wA <= wB;
    const baseStart = bottomShorter ? bl : tl;
    const baseEnd = bottomShorter ? br : tr;
    const opp = bottomShorter ? tl : bl; // 反対辺の同側端点 (左脚相当)
    const ex = baseEnd.x - baseStart.x;
    const ey = baseEnd.y - baseStart.y;
    const eLen = Math.hypot(ex, ey) || 1;
    const ux = ex / eLen, uy = ey / eLen;
    const dx = opp.x - baseStart.x;
    const dy = opp.y - baseStart.y;
    const proj = dx * ux + dy * uy;
    const nxR = dx - proj * ux;
    const nyR = dy - proj * uy;
    const perpFoot = { x: baseStart.x + nxR, y: baseStart.y + nyR };
    // 点線+ラベルとも shadow fill (rgba 128/128/128/0.35) と差がつくようオレンジで描く
    // (COLOR.guide は #9aa0a6 で fill とほぼ同色になり埋没する)
    const bvLabel = parseFloat(bv) > 0 ? bv : (Math.hypot(perpFoot.x - baseStart.x, perpFoot.y - baseStart.y)).toFixed(2);
    ctx.save();
    ctx.setLineDash([4, 3]);
    ctx.strokeStyle = COLORS.accentOrange;
    ctx.beginPath();
    ctx.moveTo(sx(baseStart.x), sy(baseStart.y));
    ctx.lineTo(sx(perpFoot.x), sy(perpFoot.y));
    ctx.stroke();
    ctx.restore();
    const lx1 = sx(baseStart.x), ly1 = sy(baseStart.y);
    const lx2 = sx(perpFoot.x), ly2 = sy(perpFoot.y);
    let angP = Math.atan2(ly2 - ly1, lx2 - lx1);
    if (angP > Math.PI / 2) angP -= Math.PI;
    else if (angP < -Math.PI / 2) angP += Math.PI;
    ctx.save();
    ctx.translate((lx1 + lx2) / 2, (ly1 + ly2) / 2);
    ctx.rotate(angP);
    ctx.textBaseline = 'bottom';
    haloText(ctx, `延長 ${bvLabel}`.trim(), 0, -3, COLORS.haloDark, COLORS.accentOrange);
    ctx.restore();
  }

  // 段階2g: 選択表示 (アプリ MyView.drawBlinkLine:794-818 の写し):
  //   1) 選択辺を黄色線で強調 (app: lastTapSide の辺を paintYellow で上書き)
  //   2) 番号サークルに黄色リング (app: pointnumber に paintTexS.textSize*0.8 の stroke circle)
  //   3) 寸法テキストは measureText の境界ボックス + 辺名 A/B/C を併記
  //      (旧オレンジ破線サークルは廃止 — user 指摘 2026-06-11「サークルを描く意味がない」)
  if (selectedDim) {
    const { tri, side } = selectedDim;
    ctx.strokeStyle = COLORS.selectYellow;

    // 1) 選択辺の黄色線。line は tri/side 識別子付きで出るので物理 side で直接取る。
    // 描画順と side 番号の不一致 (台形は bl→br→tr→tl で side 0→3→2→1) を上位で気にしない。
    const l = triLines.find((q) => q.tri === tri && q.side === side);
    if (l) {
      ctx.strokeStyle = COLORS.haloDark;
      ctx.lineWidth = 6;
      ctx.beginPath();
      ctx.moveTo(sx(l.x1), sy(l.y1));
      ctx.lineTo(sx(l.x2), sy(l.y2));
      ctx.stroke();
      ctx.strokeStyle = COLORS.selectYellow;
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
      const fh = p.size * s;
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
      ctx.font = `bold ${fh}px sans-serif`;
      ctx.textAlign = 'left';
      ctx.textBaseline = 'middle';
      // 辺名の文字もシャドーのグレーに溶けるので暗縁取り (シャドー上辺/延長と共通の haloText)
      haloText(ctx, ['A', 'B', 'C'][side] ?? '', w / 2 + pad + 4, top + fh / 2, COLORS.haloDark, COLORS.selectYellow);
      ctx.restore();
    }
  }

  // クリック位置の十字マーカー (アプリの pressedInModel は不可視 — web は「次の操作が
  // どこに効くか」を見える化する改善)。控除モード = 赤 (次の控除の置き場)、
  // 三角形編集モード = 青 (旗 FAB の番号移動先。2026-06-12 user 要望で常時 ON)
  const cross = deductionMode ? dedCursor : lastTapModel;
  if (cross) {
    const cx = sx(cross.x);
    const cy = sy(cross.y);
    ctx.strokeStyle = deductionMode ? COLORS.ded : COLORS.num;
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.moveTo(cx - 12, cy);
    ctx.lineTo(cx + 12, cy);
    ctx.moveTo(cx, cy - 12);
    ctx.lineTo(cx, cy + 12);
    ctx.stroke();
    ctx.beginPath();
    ctx.arc(cx, cy, 5, 0, Math.PI * 2);
    ctx.stroke();
  }

  // 選択中の控除はプリミティブごと COLORS.accentOrange で色替え済み (上の描画ループ)。
  // 旧黄色リングはそれに置換 (user 指定: 明示的に色替えしてやるとわかりやすい)
}

function renderCsv(canvas: HTMLCanvasElement, csv: string, label: string): void {
  try {
    const json = renderCsvToPrimitivesWithOverrides(csv, 1.0, overridesJson());
    let prims = JSON.parse(json) as Prim[];
    // 図面枠 (A3、DXF の writeDrawingFrame と同形)。lastPrims に足すので fit も枠込み —
    // 「目いっぱいでなくマージン」の図面らしい余白になる (2026-06-12 user 要望)
    if (frameVisible()) {
      try {
        prims = prims.concat(JSON.parse(renderFrameWithMargin(csv, frameMarginCm())) as Prim[]);
      } catch (e) {
        console.error('frame render failed', e);
      }
    }
    lastPrims = prims;
    draw(canvas, prims);
    setStatus(`${label}: ${prims.length} primitives`);
  } catch (e) {
    setStatus(`render error: ${String(e)}`);
    console.error(e);
  }
}

function frameVisible(): boolean {
  const cb = document.getElementById('frameToggle');
  return cb instanceof HTMLInputElement ? cb.checked : false;
}

// 外枠余白 cm を返す (2026-06-18 user 「マージン間隔は UI 上で user が選べるようにしたほうが
// 良いな」)。 marginSelect の選択値を localStorage に保存して round-trip、 未設定なら default 2.0cm。
function frameMarginCm(): number {
  const sel = document.getElementById('marginSelect');
  if (sel instanceof HTMLSelectElement && sel.value) return parseFloat(sel.value);
  const stored = localStorage.getItem('frameMarginCm');
  return stored ? parseFloat(stored) : 1.5; // default 15mm (= 2026-06-18 user 「デフォルト 15mm くらいが見やすい」)
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
  // kind='triangle': ea/eb/ec=辺A/B/C, conn=接続コード(CSV列5)。
  // kind='rectangle': ea=底辺(widthA), eb=延長(length), ec=上辺(widthB),
  //   parent=接続先の混在通し番号, conn=接続辺 side('0'独立/'1'=B/'2'=C/'3'=D)。
  // 辺プールは両者で共用。
  kind: 'triangle' | 'rectangle';
  ea: number; // 辺A の edges index (単純接続では親の eb/ec と同一値)
  eb: number;
  ec: number;
  parent: string;
  conn: string;
  extras: string[]; // 7列目以降 (測点名等) を生のまま保持して round-trip で落とさない
  // 台形のみ: 上辺アライメント (0左/1中/2右)。既存の起点(lcr)列を流用 (trap-design.md 段3)。
  // 三角形では未使用。省略時 0 で後方互換 (R1 の 7 列 Rectangle CSV は左寄せ)
  align?: number;
  // 台形のみ: 親の種別ヒント (0三角形/1台形)。CSV 9 列目 parentKind。
  // parent は常に混在通し番号。parentKind は UI 表示と旧 CSV 互換用で、モデル接続の主キーではない。
  // 三角形では未使用。省略時 0 で後方互換 (R2 の 8 列 Rectangle CSV は親=三角形のまま不変)
  parentKind?: number;
};

// 三角形として描画される行の数。
function triCount(): number {
  return rows.filter((r) => r.kind === 'triangle').length;
}

// 親の混在通し番号から「親が rectangle か」 を判定して parentKind (0=三角形/1=台形)
// を返す。 D 辺接続 option を出す条件はこの 1 関数で決まる ──
// connOptionsFor / syncForm の parentIsTrap 判定 / Row factory が全部これを通る。
// 2026-06-18 user 確定「内部動作が共通の動線を使ってるか、 使ってないなら負債」
function inferParentKind(parentNum: number): number {
  return parentNum >= 1 && parentNum <= rows.length && rows[parentNum - 1]?.kind === 'rectangle' ? 1 : 0;
}

// Row 構築 factory。 全 row 生成 path (CSV ロード / 新規行追加 / kind 切替) は
// これを通る、 object literal の重複 + parentKind 計算ミスを 1 か所に閉じる。
function createTriangleRow(args: {
  ea: number; eb: number; ec: number;
  parent: string; conn: string;
  extras?: string[];
}): Row {
  return {
    kind: 'triangle',
    ea: args.ea, eb: args.eb, ec: args.ec,
    parent: args.parent, conn: args.conn,
    extras: args.extras ?? [],
  };
}

function createRectangleRow(args: {
  ea: number; eb: number; ec: number;
  parent: string; conn: string;
  extras?: string[];
  align?: number;
  parentKind?: number; // 省略時は parent から自動推論
}): Row {
  const pk = args.parentKind ?? inferParentKind(parseInt(args.parent, 10));
  return {
    kind: 'rectangle',
    ea: args.ea, eb: args.eb, ec: args.ec,
    parent: args.parent, conn: args.conn,
    extras: args.extras ?? [],
    align: args.align ?? 0,
    parentKind: pk,
  };
}

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

// 寸法文字サイズ (アプリ MyView.textSize:117、view px 単位、初期値 30)。CSV では TextSize 行
// (writeCSV:2785、CsvLoader.readListParameter:404-407 が読む)。レンダラは比率
// (textSizeCsv / 30) でモデル単位文字高さへ写す (WebPrimitiveRenderer.renderCsv)。
// texplus/minus FAB ±5f (MainActivity.kt:1381-1392) の web 版がこれを書き換える
let textSizeCsv = 30;

// アプリ MyView.adjustTextSize:942-946 と同じクランプ (下限 8、上限 80)
function adjustTextSize(ts: number): number {
  if (ts <= 5) return 8;
  if (ts >= 80) return 80;
  return ts;
}

// 塗り色サイクルの現在 index (アプリ MainActivity.colorindex:217、初期値 4 = sky)。
// FAB を押すたび +1 で一周し、選択三角形の CSV 列 10 に書く (MainActivity.kt:1367-1377)
let colorIndex = 4;

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
  textSizeCsv = 30;
  dedLines = [];
  dedSelected = 0;
  dedCursor = null;
  // B-FORCE (2026-06-15): trapRows/recttriRows バケツ廃止。
  // user 確定 2026-06-16「トライトラップとか排除しろ、順序を維持しろ」
  // ── CSV 出現順に rows[] へ積み、parent (混在通し番号) で解決する
  for (const line of csv.split(/\r?\n/)) {
    if (line.trim() === '') continue;
    const chunks = line.split(',').map((s) => s.trim());
    // ListAngle 行は headerLines に残さず数値 state に取り出す (serializeState が書き戻す)
    if (chunks[0] === 'ListAngle') {
      listAngle = parseFloat(chunks[1] ?? '') || 0;
      continue;
    }
    // TextSize 行も同様に数値 state へ (アプリ CsvLoader.readListParameter:404-407 と同形)
    if (chunks[0] === 'TextSize') {
      textSizeCsv = parseFloat(chunks[1] ?? '') || 30;
      continue;
    }
    // Deduction 行は控除 state へ (アプリ CsvLoader.buildDeductions:369 と同じ判定)。
    // 幾何の読みは common (CsvCodec.buildDeductions)、ここは行の保持と一覧表示だけ
    if (chunks[0] === 'Deduction') {
      dedLines.push(line.trim());
      continue;
    }
    // 台形行 → 台形 Row。列 (contract): Rectangle, num, length(延長/辺B), widthA(底辺/辺A),
    // widthB(上辺/辺C), parent(三角形番号), side(1=B/2=C/0独立), align(0左/1中/2右)。num は再採番
    // するので捨てる (common も num は描画に使わず withIndex で振る)。辺は辺A=底辺/辺B=延長/辺C=上辺。
    if (chunks[0] === 'Rectangle') {
      const al = intOrNull(chunks[7] ?? '') ?? 0; // 8列目 align、省略時0で後方互換
      const pk = intOrNull(chunks[8] ?? '') ?? 0; // 9列目 parentKind (0三角形/1台形)、省略時0で後方互換
      rows.push(createRectangleRow({
        ea: newEdge(chunks[3] ?? ''), // 底辺(widthA)
        eb: newEdge(chunks[2] ?? ''), // 延長(length)
        ec: newEdge(chunks[4] ?? ''), // 上辺(widthB)
        parent: chunks[5] ?? '-1',
        conn: chunks[6] ?? '0', // side (1=B/2=C/3=D、3 は parentKind=1 の台形親のみ)
        align: al >= 0 && al <= 2 ? al : 0,
        parentKind: pk >= 0 && pk <= 1 ? pk : 0, // CSV 既存値を尊重 (auto 推論より優先)
      }));
      continue;
    }
    // 台形を親に持つ三角形。列: RectChild, num, ea(底辺/情報), eb(=B), ec(=C), parent(台形群index), side。
    // user 確定 2026-06-16「トライトラップとか排除しろ、 Triangle 行で統合しろ」
    // ── kind='triangle' として読み込み、parent を混在通し番号に変換して rows[] へ。
    if (chunks[0] === 'RectChild') {
      const trapIdx = intOrNull(chunks[5] ?? '') ?? 0;
      const tc = triCount();
      rows.push(createTriangleRow({
        ea: newEdge(chunks[2] ?? ''), // 底辺 (台形辺で上書きされる・情報)
        eb: newEdge(chunks[3] ?? ''), // B
        ec: newEdge(chunks[4] ?? ''), // C
        parent: String(tc + trapIdx), // 混在通し番号に変換
        conn: chunks[6] ?? '1',       // side (1=左脚/2=上辺/3=右脚)
      }));
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
    
    const pn = intOrNull(parent) ?? -1;
    const cn = intOrNull(conn);
    const pRow = pn > 0 ? rows[pn - 1] : undefined;
    let ea: number;
    // 辺A の共有判定 (親種別を問わず B/C 辺と一致すれば index 共有)
    if (
      pRow && (cn === 1 || cn === 2) && isSimpleConnection({ extras }) &&
      parseFloat(aStr) === parseFloat(edges[cn === 1 ? pRow.eb : pRow.ec])
    ) {
      ea = cn === 1 ? pRow.eb : pRow.ec;
    } else {
      ea = newEdge(aStr);
    }
    rows.push(createTriangleRow({
      ea,
      eb: newEdge(chunks[2] ?? ''),
      ec: newEdge(chunks[3] ?? ''),
      parent,
      conn,
      extras,
    }));
  }
}

function serializeState(): string {
  // ヘッダ 4 欄はアプリ完全形式と同じラベル付き (koujiname,<値>) で書く — 値が空白でも
  // 行が残るので round-trip で欄がずれない (空白デフォルト 2026-06-12)。
  // 5 行目以降の未知行は原文のまま保持 (CsvCodec の schema evolution 定石と同じ)
  const lines = HEADER_LABELS.map((label, i) => `${label},${headerValueAt(i)}`);
  lines.push(...headerLines.slice(4));
  // figureRows は rows[] の出現順そのままで出力する。 parser (parseCsvToState) は
  // バケツ廃止で順序維持に切り替え済 (B-FORCE 2026-06-15)、 wasm CsvCodec.buildMixed も
  // 混在通し番号 (= 出現順) で parent を解決する。
  // num 列は kind ごとのローカル連番、 parent はもとから混在通し番号を入れているのでそのまま。
  let triNum = 0, trapNum = 0;
  rows.forEach((r) => {
    if (r.kind === 'triangle') {
      triNum++;
      lines.push(
        [String(triNum), edgeVal(r, 'a'), edgeVal(r, 'b'), edgeVal(r, 'c'), r.parent, r.conn, ...r.extras].join(','),
      );
    } else if (r.kind === 'rectangle') {
      trapNum++;
      lines.push(
        `Rectangle,${trapNum},${edgeVal(r, 'b')},${edgeVal(r, 'a')},${edgeVal(r, 'c')},${r.parent},${r.conn},${r.align ?? 0},${r.parentKind ?? 0}`,
      );
    }
  });
  // figureRows の後に ListAngle / TextSize / 控除を書く (アプリ保存 MainActivity.kt:2781-2797 と同形)。
  // 旧版は Triangle 行の直後に挟んでいたが、 順序維持に切り替えたため混在 figureRows の末尾に揃える。
  lines.push(`ListAngle, ${listAngle}`);
  lines.push(`TextSize, ${textSizeCsv}`);
  lines.push(...dedLines);
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

// 台形の妥当性: 延長(辺B)・底辺(辺A)・上辺(辺C) はいずれも正の数 (三角不等式は無い)。
// 三角形と違い辺長の和の制約は無いので invalidTriangleReason は流用できない (独立の関門)
function invalidRectangleReason(length: number, widthA: number, widthB: number): string | null {
  if (!Number.isFinite(length) || !Number.isFinite(widthA) || !Number.isFinite(widthB)) {
    return '辺の値が数値ではありません';
  }
  if (length <= 0 || widthA <= 0 || widthB <= 0) return '延長・底辺・上辺はいずれも正の数が必要です';
  return null;
}

// 不成立の行を探す (混在対応)。reason は subject ("三角形 N" / "台形 N") を含むフル文言。
function findInvalidRow(rs: Row[]): { reason: string } | null {
  let triN = 0;
  let trapN = 0;
  for (const r of rs) {
    if (r.kind === 'rectangle') {
      trapN++;
      const reason = invalidRectangleReason(
        parseFloat(edgeVal(r, 'b')),
        parseFloat(edgeVal(r, 'a')),
        parseFloat(edgeVal(r, 'c')),
      );
      if (reason) return { reason: `台形 ${trapN}: ${reason}` };
      continue;
    }
    triN++;
    const reason = invalidTriangleReason(
      parseFloat(edgeVal(r, 'a')),
      parseFloat(edgeVal(r, 'b')),
      parseFloat(edgeVal(r, 'c')),
    );
    if (reason) return { reason: `三角形 ${triN}: ${reason}` };
  }
  return null;
}

function redraw(canvas: HTMLCanvasElement): void {
  const bad = findInvalidRow(rows);
  if (bad) {
    // キャンバスは直前の正しい状態のまま据え置き、autosave も汚さない。
    // 値を直せば次の入力イベントでそのまま再描画に戻る
    setStatus(`⚠ ${bad.reason} — 図は更新しません`);
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
  // 図形種別でフォームの辺ラベルを切替える (台形: 辺A=底辺, 辺B=延長, 辺C=上辺)。
  // ヘッダは FAB モード (figureKind) が「今から足す図形」の語彙を決める。
  const isTrap = figureKind === 'rectangle';
  // 新規番号は figureKind に追従 (三角形 = 次の三角形番号、台形 = 次の台形連番 T{k})
  input('newNum').value = String(rows.length + 1);
  el('thEdgeA').textContent = isTrap ? '底辺(A)' : '辺A';
  el('thEdgeB').textContent = isTrap ? '延長(B)' : '辺B';
  el('thEdgeC').textContent = isTrap ? '上辺(C)' : '辺C';

  const cur = current >= 1 ? rows[current - 1] : null;
  // 現在行の番号表示は行自身の種別に従う (三角形 = 番号、台形 = T{k})
  input('curNum').value = cur ? String(current) : '';
  input('curName').value = cur ? nameOf(cur) : '';
  input('curA').value = cur ? fmt2(edgeVal(cur, 'a')) : '';
  input('curB').value = cur ? fmt2(edgeVal(cur, 'b')) : '';
  input('curC').value = cur ? fmt2(edgeVal(cur, 'c')) : '';
  input('curParent').value = cur ? cur.parent : '';
  if (cur && cur.kind === 'rectangle') {
    // 親種別 (pk: 0三角形/1台形) は親番号から自動推論 (user 指摘 2026-06-14「親種別を形態列に書くな」)。
    // 形態(CType)列は三角形と同じ axis (辺共有/二重断面/フロート) を流用 (台形でも共通サポート方針)。
    // 起点(lcr)列は上辺寄せ(0左/1中/2右) — 常時有効化し Row.align を表示・編集する (段3)。
    // 接続辺(Conn)は side。親=台形(pk=1)なら D(3) も出す
    const pnum = intOrNull(cur.parent) ?? -1;
    const pk = (pnum >= 1 && pnum <= rows.length && rows[pnum - 1]?.kind === 'rectangle') ? 1 : 0;
    cur.parentKind = pk; // CSV 出力の整合 (col 9) を保つ
    setSelectOptions(select('curCType'), CTYPE_OPTIONS, cur.extras[12] || '0');
    select('curCType').disabled = false;
    setSelectOptions(select('curConn'), connOptionsFor('cur', pk));
    select('curConn').value = pnum >= 1 ? trapSideValue(cur.conn, pk) : '-1';
    select('curLcr').value = String(cur.align ?? 0);
    select('curLcr').disabled = false;
  } else {
    // 三角形: 形態(辺共有/二重断面/フロート)・接続辺(独立/B/C) を従来の選択肢に戻す (台形編集後の取り違え防止)
    setSelectOptions(select('curCType'), CTYPE_OPTIONS);
    select('curCType').disabled = false;
    setSelectOptions(select('curConn'), connOptionsFor('cur', 0));
    setConnSelects('cur', cur ? connPartsOf(cur) : null);
  }
  // 新規入力行: 親種別 (三角形親/台形親) は親番号から自動推論 (user 指摘 2026-06-14「台形の形態の
  // ところに親の種類を書いてること自体が間違い」)。形態列 (newCType) は本来の意味 (辺共有/二重断面/
  // フロート) を保ち、台形 mode では形態に意味が無いので disable する。
  const newParentN = intOrNull(input('newParent').value.trim());
  const parentIsTrap = newParentN !== null && newParentN >= 1 && newParentN <= rows.length
    && rows[newParentN - 1]?.kind === 'rectangle';
  // 接続辺 options: 親が台形なら D を含めて B/C/D、それ以外 B/C
  // 形態列 (newCType) は三角形・台形共通の axis (user 方針 2026-06-14「三角形と同じように
  // 二重断面やフロートを同一にサポートする」)。disable しない。
  setSelectOptions(select('newCType'), CTYPE_OPTIONS);
  select('newCType').disabled = false;
  setSelectOptions(select('newConn'), connOptionsFor('new', parentIsTrap ? 1 : 0));
  if (isTrap) select('newLcr').disabled = false; else syncLcrDisabled('new');
  updateReshapeFab(); // current 行に追従して reshape FAB を有効/無効・title 更新
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
  pendingTrapParent = null; // 三角形を選び直したら台形親プリセットは解除
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
  pendingTrapParent = null; // 選択解除で台形親プリセットも解除
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

// 台形フォームで「形態(CType)」列を親種別に流用する選択肢 (trap-design.md 段4-3 / R5)。
// R2 で起点列を align に流用したのと同じ思想 — 新しい UI コントロールを足さず既存3列を使い回す。
// 三角形では従来の CTYPE_OPTIONS (辺共有/二重断面/フロート) を使う
const PARENTKIND_OPTIONS: Array<[string, string]> = [
  ['0', '親:三角形'],
  ['1', '親:台形'],
];

// 接続辺(Conn)列の選択肢。親=三角形(parentKind=0)は B/C、親=台形(parentKind=1)は B/C/D (D=右脚 side=3)。
// 'cur' は独立 option を持つ (現在行は独立に戻せる)、'new' は持たない (新規は空親で独立を表す、従来どおり)
function connOptionsFor(prefix: 'new' | 'cur', parentKind: number): Array<[string, string]> {
  const opts: Array<[string, string]> = prefix === 'cur' ? [['-1', '独立']] : [];
  opts.push(['1', '親のB辺'], ['2', '親のC辺']);
  if (parentKind === 1) opts.push(['3', '親のD辺']);
  return opts;
}

// select の option を動的に入れ替える。index.html の静的 option を main.ts から書き換えるのは、
// 三角形⇄台形でフォーム列の意味 (形態 ⇄ 親種別、接続辺 B/C ⇄ B/C/D) が変わるため。kind ごとに
// option 集合を所有して取り違え (台形の親種別値で三角形を編集する等) を防ぐ。value を渡せば
// その値を選ぶ。新 option 集合に無ければ先頭に落とす (D→B/C 切替で値が消えても破綻しない)
function setSelectOptions(sel: HTMLSelectElement, opts: Array<[string, string]>, value?: string): void {
  const want = value ?? sel.value;
  sel.textContent = '';
  for (const [v, label] of opts) {
    const opt = document.createElement('option');
    opt.value = v;
    opt.textContent = label;
    sel.appendChild(opt);
  }
  if (opts.some(([v]) => v === want)) sel.value = want;
  else if (opts.length > 0) sel.value = opts[0][0];
}

// 台形の接続辺 side 値を option 集合に収める (1=B/2=C、parentKind=1 のときのみ 3=D を許す)
function trapSideValue(conn: string, parentKind: number): string {
  const s = intOrNull(conn) ?? 1;
  if (parentKind === 1 && s === 3) return '3';
  return s === 2 ? '2' : '1';
}

// 起点は二重断面/フロートのみ意味を持つ (辺共有は常に全長一致なので無効化)
function syncLcrDisabled(prefix: 'new' | 'cur'): void {
  select(`${prefix}Lcr`).disabled = select(`${prefix}CType`).value === '0';
}

// 形態(CType)列の change 時の追従。三角形では起点(lcr)の有効/無効を更新するが、台形では CType は
// 「親種別」なので、選んだ種別に応じて接続辺(Conn)の D(3) option を出し直す (起点=上辺寄せは据え置き)。
// 'new' の台形文脈は figureKind、'cur' は現在行の kind で判定
function onCTypeChange(prefix: 'new' | 'cur'): void {
  const isTrapCtx = prefix === 'new'
    ? figureKind === 'rectangle'
    : current >= 1 && rows[current - 1]?.kind === 'rectangle';
  if (isTrapCtx) {
    const pk = (intOrNull(select(`${prefix}CType`).value) ?? 0) === 1 ? 1 : 0;
    setSelectOptions(select(`${prefix}Conn`), connOptionsFor(prefix, pk));
  } else {
    syncLcrDisabled(prefix);
  }
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

// 台形行のセル群 (辺=底辺/延長/上辺、親番号、接続辺 side)。番号・測点名・削除は buildTable 側で共通。
// 段1+2: 辺共有 (side のみ) まで。形態/起点 (二重断面・アライメント) は段4 なので空セル 2 つ。
function buildTrapRowCells(tr: HTMLTableRowElement, row: Row, i: number, canvas: HTMLCanvasElement): void {
  // 辺セル (辺A=底辺/辺B=延長/辺C=上辺)。三角形と同じ辺プール機構 (syncEdgeInputs で他セル追従)
  for (const key of ['a', 'b', 'c'] as const) {
    const td = document.createElement('td');
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

  // 親番号 (三角形番号)。台形は辺A 共有の張り直し (relinkEdgeA) が無いので redraw だけ
  const tdParent = document.createElement('td');
  const parentInp = numberInput(row.parent, '1', (v) => {
    row.parent = v;
    redraw(canvas);
  });
  parentInp.addEventListener('change', () => {
    buildTable(canvas);
    syncForm();
    redraw(canvas);
  });
  tdParent.appendChild(parentInp);
  tr.appendChild(tdParent);

  // 接続辺 (side): 独立なら表示のみ、接続済みなら選択。親=三角形は B/C、親=台形(parentKind=1)は B/C/D (R5)。
  // 形態列は三角形と同じ CTYPE_OPTIONS (辺共有/二重断面/フロート) を流用 (user 2026-06-14: 親:三角形/親:台形
  // 表示は撤回、親種別は親番号から自動推論)。起点列は上辺寄せ(0左/1中/2右) — 独立・接続とも常時有効
  const pn = intOrNull(row.parent) ?? -1;
  const pk = (pn >= 1 && pn <= rows.length && rows[pn - 1]?.kind === 'rectangle') ? 1 : 0;
  row.parentKind = pk; // CSV 出力の整合 (col 9) を保つ
  if (pn < 1) {
    const tdInd = document.createElement('td');
    tdInd.textContent = '独立';
    tr.appendChild(tdInd);
  } else {
    const sideSel = document.createElement('select');
    sideSel.id = `sideCell-${i + 1}`; // tlcp 用 (option 列を test で pin するため)
    for (const [v, label] of connOptionsFor('new', pk)) {
      const opt = document.createElement('option');
      opt.value = v;
      opt.textContent = label;
      sideSel.appendChild(opt);
    }
    sideSel.value = trapSideValue(row.conn, pk);
    sideSel.addEventListener('change', () => {
      row.conn = sideSel.value;
      redraw(canvas);
    });
    const tdSide = document.createElement('td');
    tdSide.appendChild(sideSel);
    tr.appendChild(tdSide);
  }
  // 形態列 = CTYPE (辺共有/二重断面/フロート)。台形でも三角形と同じ axis を持たせる
  // (user 2026-06-14「親種別は形態列に書くな、親番号から自動推論」)。値は extras[12] (cp.type) に書く
  // — 三角形と同じ slot を流用、common 側で幾何適用が後段になっても UI 表現は揃える。
  const kindSel = document.createElement('select');
  for (const [v, label] of CTYPE_OPTIONS) {
    const opt = document.createElement('option');
    opt.value = v;
    opt.textContent = label;
    kindSel.appendChild(opt);
  }
  kindSel.value = row.extras[12] || '0';
  kindSel.addEventListener('change', () => {
    row.extras[12] = kindSel.value;
    redraw(canvas);
  });
  const tdKind = document.createElement('td');
  tdKind.appendChild(kindSel);
  tr.appendChild(tdKind);
  // 起点列 = 上辺寄せ (LCR_OPTIONS を流用: 値 2右/1中/0左 = align 整数と同値)
  const alignSel = document.createElement('select');
  for (const [v, label] of LCR_OPTIONS) {
    const opt = document.createElement('option');
    opt.value = v;
    opt.textContent = label;
    alignSel.appendChild(opt);
  }
  alignSel.value = String(row.align ?? 0);
  alignSel.addEventListener('change', () => {
    const a = intOrNull(alignSel.value) ?? 0;
    row.align = a >= 0 && a <= 2 ? a : 0;
    redraw(canvas);
  });
  const tdAlign = document.createElement('td');
  tdAlign.appendChild(alignSel);
  tr.appendChild(tdAlign);

  // 削除 (三角形と同じ deleteRow)
  const tdDel = document.createElement('td');
  const del = document.createElement('button');
  del.type = 'button';
  del.className = 'del';
  del.textContent = '削除';
  del.addEventListener('click', (e) => {
    e.stopPropagation();
    deleteRow(canvas, i + 1);
  });
  tdDel.appendChild(del);
  tr.appendChild(tdDel);
}

function buildTable(canvas: HTMLCanvasElement): void {
  const tbody = document.getElementById('triRows');
  if (!tbody) return;
  tbody.textContent = '';

  const tc = triCount(); // 台形の連番 (T{k}) 用。混在表は三角形 prefix + 台形 suffix
  rows.forEach((row, i) => {
    const tr = document.createElement('tr');
    if (i + 1 === selected) tr.classList.add('selected');
    // 行クリック → 図の三角形と同じ選択 state を更新 (双方向の表側)。
    // セル内 input のクリックも「その行を選ぶ」操作なので止めない
    tr.addEventListener('click', () => selectTriangle(canvas, i + 1));

    const tdNum = document.createElement('td');
    tdNum.className = 'num';
    // 三角形は三角形番号 (= i+1、prefix なので一致)、台形は台形連番 "T{k}" (canvas 表記と一致)
    tdNum.textContent = String(i + 1);
    tr.appendChild(tdNum);

    // 種別 (kind 一目視認): 三角=△ / 台形=□。 dataset.kind は CP / E2E で kind 別件数を数えるためにも使う。
    // click で △↔□ 切替 (2026-06-18 user 命令)。 side 0/1/2 (=A/B/C) は両形状で物理意味が共通
    // (commit 93dc388 で side 番号物理意味固定)、 side 3 (=D 右脚) は Rectangle 専属で
    // 自分の conn=3 か子が conn=3 のときは切替不可。
    const tdKind = document.createElement('td');
    tdKind.id = `kindCell-${i + 1}`; // tlcp 用 (click で kind 切替 test するため)
    tdKind.className = 'kind';
    tdKind.dataset.kind = row.kind;
    tdKind.textContent = row.kind === 'rectangle' ? '□' : '△';
    tdKind.title = row.kind === 'rectangle' ? '台形 (click で三角形に切替)' : '三角形 (click で台形に切替)';
    tdKind.style.cursor = 'pointer';
    tdKind.addEventListener('click', () => {
      const wouldBeTriangle = row.kind === 'rectangle';
      // 制約 a: 自分が rectangle で conn=3 (D 右脚に親接続) → triangle に変えると接続辺消失
      if (wouldBeTriangle && row.conn === '3') {
        alert('側面 3 (D 右脚) に親接続中、 三角形に変えられません');
        return;
      }
      // 制約 b: 自分の子に conn=3 がある → 形状切替で子の接続辺消失
      const myNum = i + 1;
      const hasD3child = rows.some((r) => r.parent === String(myNum) && r.conn === '3');
      if (hasD3child) {
        alert('子図形が側面 3 (D 右脚) に接続中、 種別変更不可');
        return;
      }
      // 切替は reshapeCurrent と同一動線を通る (= 既存「reshape FAB」 と同じ factory
      // 経由の row 再構築、 順序維持、 子参照 reject)。 click 動線で current を切替先に
      // セットしてから reshapeCurrent を呼ぶ。
      current = i + 1;
      reshapeCurrent(canvas);
      autosave();
    });
    tr.appendChild(tdKind);

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

    // 台形行は専用ブランチで描画 (辺=底辺/延長/上辺、接続辺=side のみ、形態/起点は段4)
    if (row.kind === 'rectangle') {
      buildTrapRowCells(tr, row, i, canvas);
      tbody.appendChild(tr);
      return;
    }

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
      inp.dataset.tri = String(i + 1);
      inp.dataset.key = key;
      inp.addEventListener('change', () => {
        inp.value = fmt2(inp.value);
        setEdgeVal(row, key, inp.value);
        syncEdgeInputs(row[EKEY[key]], inp);
        redraw(canvas);
      });
      // Enter で次の辺セルへ (a→b→c→次行a、最終行の c は新規行 A へ) — 新規行の
      // wireNewRowEnter と同じ actionNext 動線を既存行にも。blur で change が発火するので
      // 値の確定 (fmt2 + redraw) は既存の change ハンドラに任せる
      inp.addEventListener('keydown', (e) => {
        if (e.key !== 'Enter' || e.isComposing) return;
        e.preventDefault();
        const sel =
          key === 'a' ? `input[data-tri="${i + 1}"][data-key="b"]`
          : key === 'b' ? `input[data-tri="${i + 1}"][data-key="c"]`
          : `input[data-tri="${i + 2}"][data-key="a"]`;
        const t = document.querySelector<HTMLInputElement>(`#triRows ${sel}`)
          ?? document.querySelector<HTMLInputElement>('#newA');
        if (t) {
          t.focus();
          t.select();
        }
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
    rows.push(createTriangleRow({ ea: newEdge('3.0'), eb: newEdge('3.0'), ec: newEdge('3.0'), parent: '-1', conn: '-1' }));
    clearSelection();
    buildTable(canvas);
    syncForm();
    redraw(canvas);
    return;
  }
  // 親は「current 選択行」(無ければ末尾 = 従来挙動)。current は混在通し番号 = rows index + 1
  const parentIdx = current > 0 ? current : rows.length;
  const parentRow = rows[parentIdx - 1];
  // 通常 (三角形親 or 台形親): 末尾に挿入。辺A は親の B 辺 (台形なら延長) と同一の物理辺を共有 (辺プール)
  // user 2026-06-15「台形にくっついてる三角形からも派生したい」
  const ea = parentRow.kind === 'rectangle' ? parentRow.eb : parentRow.eb; // とりあえず B 辺共有
  rows.push(createTriangleRow({
    ea,
    eb: newEdge('3.0'),
    ec: newEdge('3.0'),
    parent: String(parentIdx),
    conn: '1',
  }));
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
  const child = isEdgeOccupied(parentN, side);
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

// カレント行フォームの値で current 三角形を書き換える (processTriEditMode の edit 側)。
// fabReplace (新規行 B が空のとき) と curC の Enter 確定の 2 動線から呼ばれる
function rewriteCurrent(canvas: HTMLCanvasElement): void {
  if (rows.length === 0) {
    setStatus('Cannot edit: リストが空です。先に追加してください');
    return;
  }
  if (current < 1) {
    setStatus('Cannot edit: カレント行がありません。図か一覧で選択してください');
    return;
  }
  // 台形カレント行の書換え (段1+2): 辺=底辺/延長/上辺、親+side のみ。relinkEdgeA/cp は無い
  if (rows[current - 1].kind === 'rectangle') {
    rewriteCurrentRectangle(canvas);
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
}

// 台形カレント行の書換え (段1+2)。辺A=底辺/辺B=延長/辺C=上辺、親 (三角形番号) と side のみ。
// 三角形のような辺A 共有 (relinkEdgeA) や cp 列 (writeCpExtras) は段4 なので無い
function rewriteCurrentRectangle(canvas: HTMLCanvasElement): void {
  const r = rows[current - 1];
  const editBad = invalidRectangleReason(
    parseFloat(input('curB').value), // 延長
    parseFloat(input('curA').value), // 底辺
    parseFloat(input('curC').value), // 上辺
  );
  if (editBad) {
    setStatus(`⚠ 台形: ${editBad} — 書換えを中止`);
    return;
  }
  // 親番号は混在通し番号。親種別は rows[parent-1] から表示・side 制限用にだけ推論する。
  const cp = input('curParent').value.trim();
  const pn = cp === '' ? -1 : (intOrNull(cp) ?? -1);
  let parent = -1;
  let side = 0;
  let parentKind: 0 | 1 = 0;
  if (pn >= 1) {
    const parentRow = rows[pn - 1];
    if (!parentRow || pn >= current || pn > rows.length) {
      setStatus(`⚠ 親番号 ${pn} の図形が存在しません — 書換えを中止`);
      return;
    }
    parentKind = parentRow.kind === 'rectangle' ? 1 : 0;
    parent = pn;
    const s = intOrNull(select('curConn').value) ?? 1;
    side = parentKind === 1 ? (s === 2 ? 2 : s === 3 ? 3 : 1) : (s === 2 ? 2 : 1);
  }
  // 上辺の寄せ = 起点(lcr)列を流用 (0左/1中/2右)。台形カレント行では常時有効 (syncForm)
  const align = intOrNull(select('curLcr').value) ?? 0;
  takeUndoSnap();
  setEdgeVal(r, 'a', fmt2(input('curA').value));
  setEdgeVal(r, 'b', fmt2(input('curB').value));
  setEdgeVal(r, 'c', fmt2(input('curC').value));
  r.parent = String(parent);
  r.conn = String(side);
  r.align = align >= 0 && align <= 2 ? align : 0;
  r.parentKind = parent >= 1 ? parentKind : 0;
  setName(r, input('curName').value);
  buildTable(canvas);
  syncForm();
  redraw(canvas);
  setStatus(`Rewrite 台形 #${current}`);
}

// replace (MainActivity.kt:1614 fabReplace → processTriEditMode:1652):
// 新規行の B が空 → カレント行の書換え / C だけ空 → 何もしない / 両方あり → 追加。
// この 3 分岐の判定をそのまま写す (幾何の知識は CSV 再構築側 = common が持つ)
function fabReplace(canvas: HTMLCanvasElement): void {
  // 控除モード: 選択控除の書換え (アプリ fabReplace → processDedEditMode:1670 の edit 側。
  // 追加はアプリと違い専用の「追加」ボタン (dedAdd) が既にあるので、FAB は書換え固定)
  if (deductionMode) {
    dedReplace(canvas);
    return;
  }
  // 台形モード: 「追加」は台形行を生成する (控除モード分岐と同じ構え)
  if (figureKind === 'rectangle') {
    addRectangle(canvas);
    return;
  }
  const newB = input('newB').value.trim();
  const newC = input('newC').value.trim();

  if (newB === '') {
    // Edit attempt (processTriEditMode: strAddLineB.isEmpty)
    const r = rows[current - 1];
    if (r) {
      if (r.kind === 'rectangle') {
        rewriteCurrentRectangle(canvas);
      } else {
        rewriteCurrent(canvas);
      }
    }
    return;
  }
  if (newC === '') return; // アプリと同じ: B だけでは何もしない (strAddLineC.isEmpty -> return)

  // 台形辺タップ済 (三角形を台形に乗せる)
  if (pendingTrapParent) {
    addTriOnTrap(canvas, newB, newC);
    return;
  }

  // Add (アプリ addTriangleBy 相当)。
  let newParts = readConnParts('new');
  let conn = newParts ? String(encodeConn(newParts)) : '-1';
  let parent = input('newParent').value.trim();
  if (rows.length === 0) {
    // 最初の 1 個は必ず独立 (スマホ版同様、図面に 1 リスト)
    newParts = null;
    conn = '-1';
    parent = '-1';
    input('newParent').value = '';
  }
  if (conn === '-1') {
    parent = '-1';
  } else if (parent === '') {
    // 最後に足した行を親にする (現在の混在リストの末尾)
    parent = String(current > 0 ? current : rows.length);
  }
  const connCode = intOrNull(conn) ?? -1;
  // 親の実在関門
  if (connCode >= 1) {
    const pn = parseInt(parent, 10);
    if (!(pn >= 1 && pn <= rows.length)) {
      setStatus(`⚠ 親番号 ${parent} の図形が存在しません — 追加を中止`);
      return;
    }
  }
  let a = input('newA').value.trim();
  if ((conn === '1' || conn === '2') && a === '') {
    const p = rows[parseInt(parent, 10) - 1];
    if (p) a = edgeVal(p, conn === '1' ? 'b' : 'c');
  }
  if (connCode >= 3 && a === '') {
    setStatus('⚠ 二重断面/フロートは辺A (断面幅) の入力が必要です');
    input('newA').focus();
    return;
  }
  const addBad = invalidTriangleReason(parseFloat(a), parseFloat(newB), parseFloat(newC));
  if (addBad) {
    setStatus(`⚠ 新規行: ${addBad} — 追加を中止`);
    return;
  }
  // 既接続辺への二重接続も行を足す前に却下
  if (conn === '1' || conn === '2') {
    const pn = parseInt(parent, 10);
    const child = isEdgeOccupied(pn, conn === '1' ? 1 : 2);
    if (child > 0) {
      setStatus(`⚠ 図形 #${pn} の ${conn === '1' ? 'B' : 'C'} 辺 — 図形 #${child} が接続済みのため追加を中止`);
      return;
    }
  }
  takeUndoSnap();
  const name = input('newName').value;
  // 単純接続は親の辺 index を共有、独立は自分の辺を作る
  const pRow = rows[parseInt(parent, 10) - 1];
  const ea =
    pRow && (conn === '1' || conn === '2') ? (conn === '1' ? pRow.eb : pRow.ec) : newEdge(a);
  const newRow = createTriangleRow({ ea, eb: newEdge(newB), ec: newEdge(newC), parent, conn, extras: name !== '' ? [name] : [] });
  writeCpExtras(newRow, newParts);
  // リストの末尾に積む (B-FORCE 2026-06-16: 順序維持一本化)
  rows.push(newRow);
  const newNum = rows.length;

  // 新規入力行をリセットして次の入力へ
  input('newName').value = '';
  input('newA').value = '';
  input('newB').value = '';
  input('newC').value = '';
  input('newParent').value = '';
  select('newConn').value = '-1';

  selected = newNum;
  current = newNum;
  edgeSel = null;
  shadowPrims = null;
  view = null;
  buildTable(canvas);
  syncForm();
  redraw(canvas);
  setStatus(`Add Triangle #${newNum}`);
}

// 台形の追加 (混在リスト段1+2): 新規入力欄を台形語彙で読み、台形 Row を rows[] 末尾 (台形 suffix)
// に積む。辺A=底辺(widthA), 辺B=延長(length), 辺C=上辺(widthB)。幾何は common が CSV から再構築。
function addRectangle(canvas: HTMLCanvasElement): void {
  // ラベルは台形語彙だが input の id は三角形と共用 (newA/newB/newC)
  let widthAStr = input('newA').value.trim();   // 底辺 (親=台形なら親辺長で上書き、共有辺)
  const lengthStr = input('newB').value.trim(); // 延長
  const widthBStr = input('newC').value.trim(); // 上辺

  const t = triCount();
  // 親=台形のスロット (pendingTrapParent) があるなら、parent/side/parentKind はスロットから直に取る。
  // 底辺は親台形辺長で上書き (RectChild と同じ思想、共有辺なので 0/未入力でも親辺長で埋める)。
  const pend = pendingTrapParent;
  let parent: number;
  let side: number;
  let parentKind: 0 | 1;
  if (pend) {
    // pend.parent はすでに混在通し番号 (1 始まり)。
    parent = pend.parent;
    side = pend.side;
    parentKind = rows[parent - 1]?.kind === 'rectangle' ? 1 : 0;
    const baseLen = trapEdgeLen(pend.parent, pend.side);
    if (baseLen > 0) widthAStr = baseLen.toFixed(2);
  } else {
    // 親種別は「親番号 → rows[parent-1].kind」で自動推論する (user 指摘 2026-06-14「台形の形態の
    // ところに親の種類を書いてること自体が間違い」)。形態列 (newCType) は形態専用、親種別は
    // 親番号から逆引きで決まるので UI 列を消費しない。
    const parentStr = input('newParent').value.trim();
    parent = parentStr === '' ? -1 : (intOrNull(parentStr) ?? -1);
    side = intOrNull(select('newConn').value) ?? 0;
    if (parent >= 1 && parent <= rows.length) {
      parentKind = rows[parent - 1].kind === 'rectangle' ? 1 : 0;
      if (parentKind === 1) {
        if (side !== 1 && side !== 2 && side !== 3) side = 1;
      } else if (side !== 1 && side !== 2) side = 1;
    } else if (parent >= 1) {
      // 親番号が範囲外 (= 存在しない親) — renderer が行を落とす前に止める
      setStatus(`⚠ 親番号 ${parent} の図形が存在しません — 追加を中止`);
      return;
    } else {
      parent = -1;
      side = 0;
      parentKind = 0;
    }
  }

  // 正の数の関門 (三角不等式は無い、台形専用判定)。pend の場合 widthA は親辺長で確定済み
  const bad = invalidRectangleReason(parseFloat(lengthStr), parseFloat(widthAStr), parseFloat(widthBStr));
  if (bad) {
    setStatus(`⚠ 台形: ${bad} — 追加を中止`);
    return;
  }
  // 上辺の寄せ = 起点(lcr)列を流用 (0左/1中/2右)。台形モードで常時有効化済 (syncForm)
  const align = intOrNull(select('newLcr').value) ?? 0;
  takeUndoSnap();
  // 台形 Row を suffix 末尾へ (辺A=底辺/辺B=延長/辺C=上辺、conn=side、align=上辺寄せ、parentKind=親種別)
  rows.push(createRectangleRow({
    ea: newEdge(widthAStr),
    eb: newEdge(lengthStr),
    ec: newEdge(widthBStr),
    parent: String(parent),
    conn: String(side),
    align: align >= 0 && align <= 2 ? align : 0,
    parentKind: parent >= 1 ? parentKind : 0,
  }));
  const num = rows.length - t; // 台形内連番 (suffix での位置)
  // 新規入力行をリセット (三角形追加と同じ後始末)
  input('newName').value = '';
  input('newA').value = '';
  input('newB').value = '';
  input('newC').value = '';
  input('newParent').value = '';
  select('newConn').value = '1';
  // 親=台形のスロットを消費 + シャドー解除 (addTriOnTrap と同じ後始末)
  pendingTrapParent = null;
  edgeSel = null;
  shadowPrims = null;
  selected = rows.length;
  current = rows.length;
  view = null; // 行を増やしたので全体 fit に戻す (削除側 main.ts:2484 と対称)
  buildTable(canvas);
  syncForm();
  redraw(canvas);
  const sideLabel = side === 3 ? 'D' : side === 2 ? 'C' : 'B';
  setStatus(`台形 ${num} を追加 (${parent > 0 ? `${parentKind === 1 ? '台形' : '三角形'} ${parent} の ${sideLabel} 辺` : '独立'})`);
}

// FAB の表示を現在の図形種別に合わせる (押す前から「今どちらを追加するか」が見える)。
// 44px 円にはグリフ 1 文字だけ (△/▱)。語は title と status に出す。台形モードは橙点灯
function updateFigureKindFab(): void {
  const btn = document.getElementById('fabFigureKind');
  if (!btn) return;
  const isTrap = figureKind === 'rectangle';
  btn.textContent = isTrap ? '▱' : '△';
  btn.classList.toggle('trapon', isTrap);
  btn.setAttribute('title', `新規図形を 三角形⇄台形 で切替 (現在: ${isTrap ? '台形' : '三角形'})`);
}

// reshape FAB の disable + 動的 title。current が無い or 子参照されてる時は不可。
// syncForm から毎回呼ぶ (current 変更で必ず通る)。
function updateReshapeFab(): void {
  const btn = document.getElementById('fabReshape') as HTMLButtonElement | null;
  if (!btn) return;
  const cur = current >= 1 ? rows[current - 1] : null;
  if (!cur) {
    btn.disabled = true;
    btn.setAttribute('title', 'カレント行を選んでから (種別反転 三角形⇄台形)');
    return;
  }
  const referred = rows.findIndex((other, i) => i !== current - 1 && intOrNull(other.parent) === current);
  if (referred >= 0) {
    btn.disabled = true;
    btn.setAttribute('title', `#${current} は #${referred + 1} の親 — 解除してから反転できます`);
    return;
  }
  btn.disabled = false;
  const target = cur.kind === 'triangle' ? '台形' : '三角形';
  btn.setAttribute('title', `#${current} を ${target} に変換 (辺長 a/b/c はそのまま流用)`);
}

// 新規図形の種別をトグル (三角形⇄台形)。ラベル・FAB 表示を更新して setStatus
function toggleFigureKind(): void {
  figureKind = figureKind === 'triangle' ? 'rectangle' : 'triangle';
  // pendingTrapParent / edgeSel は維持 (タップ済の親スロットは残してシャドーだけ新しい図形種別で
  // 組み直す)。形態列 (newCType) は意味が三角形/台形共通になったのでリセットしない
  // (user 方針 2026-06-14「親種別を形態列に書かない / 二重断面・フロートを台形でも共通サポート」)。
  shadowPrims = null;
  updateFigureKindFab();
  buildShadow(); // モード切替で「次に足す図形」が変わるのでシャドーを組み直す
  syncForm();   // 辺ラベル (底辺/延長/上辺 ⇄ 辺A/B/C) を切替える
  const canvas = document.getElementById('cv') as HTMLCanvasElement | null;
  if (canvas) draw(canvas, lastPrims); // シャドー切替を即描画反映
  setStatus(figureKind === 'rectangle' ? '新規図形: 台形 (辺A=底辺 / 辺B=延長 / 辺C=上辺)' : '新規図形: 三角形');
}

// カレント行 (=既存図形) の種別を反転する (三角形⇄台形)。user 2026-06-14:
// 「既存図形の形状を変化させたい (= 削除+新規作成の代用)」動線。fabFigureKind は
// 「次に追加する図形」の切替なので意図混同を避けるため別 FAB (fabReshape) で受ける。
// 辺長 a/b/c はそのまま流用 — triangle a/b/c ↔ rectangle widthA/length/widthB は edge 配置が一致。
// 子参照がある (= 他行が親番号として自分を参照) 場合は番号振り直しが要るので簡易版では reject、
// status で解除を促す (将来: 子の parent も同期して書換える)。
function reshapeCurrent(canvas: HTMLCanvasElement): void {
  const idx = current - 1;
  const r = current >= 1 ? rows[idx] : null;
  if (!r) {
    setStatus('カレント行が無いので種別反転できません (行を選択してから)');
    return;
  }
  const myNum = idx + 1; // 混在通し番号 = rows-index + 1
  const referredBy = rows.findIndex((other, i) => i !== idx && intOrNull(other.parent) === myNum);
  if (referredBy >= 0) {
    setStatus(`#${myNum} を親として参照する行 (#${referredBy + 1}) があるので種別反転できません — 先に解除してください`);
    return;
  }
  // 制約 (D 辺は Rectangle 専属): 自分が rectangle で conn=3 (= D 右脚に親接続中) を
  // triangle 化すると親辺消失。 子に conn=3 がある場合は形状切替で子の接続辺消失。
  // 2026-06-18 「click 種別切替」 動線を reshapeCurrent に統合した際に移植。
  if (r.kind === 'rectangle' && r.conn === '3') {
    setStatus(`#${myNum} は側面 3 (D 右脚) に親接続中、 三角形に変えられません`);
    return;
  }
  const hasD3child = rows.some((c) => c.parent === String(myNum) && c.conn === '3');
  if (hasD3child) {
    setStatus(`#${myNum} の子図形が側面 3 (D 右脚) に接続中、 種別変更不可`);
    return;
  }
  takeUndoSnap();
  // factory 経由で新 row を組み立て、 旧 row を抜いて挿入位置に積み直す。
  // align/parentKind の set/delete を直接 mutate せず factory に閉じ込めて、
  // 「click 種別切替」 と「reshape FAB」 が同一 path を通る (2026-06-18 user 「内部動作共通」)。
  let newR: Row;
  if (r.kind === 'triangle') {
    newR = createRectangleRow({
      ea: r.ea, eb: r.eb, ec: r.ec,
      parent: r.parent, conn: r.conn, extras: r.extras,
    });
    rows.splice(idx, 1);
    rows.push(newR);
  } else {
    newR = createTriangleRow({
      ea: r.ea, eb: r.eb, ec: r.ec,
      parent: r.parent, conn: r.conn, extras: r.extras,
    });
    const tc = rows.filter((x, i) => i !== idx && x.kind === 'triangle').length;
    rows.splice(idx, 1);
    rows.splice(tc, 0, newR);
  }
  current = rows.indexOf(newR) + 1;
  selected = current;
  // 種別反転で図形の bounds が変わる (三角形⇄台形は辺数も形状も違う)。 既存 view (反転前の fit)
  // のままだと描画位置がズレ「センタリングが外れる」(user 報告 2026-06-16)。 loadCsv/addRectangle/
  // addTriOnTrap と同じく view=null で次の draw に全体 fit を任せる。
  view = null;
  buildTable(canvas);
  syncForm();
  redraw(canvas);
  setStatus(`#${current} の種別を ${r.kind === 'triangle' ? '三角形' : '台形'} に切替`);
}

// 行削除の共通処理 (行の削除ボタン / fabMinus の両動線)。番号の振り直しに合わせて
// 残存行の親参照も振り直す — これを怠ると「存在しない親に繋がる行」が残り、
// WebCsvReader が行ごと skip して『何を足しても描画されない』連鎖になる
// (2026-06-11 user 報告: 全削除→ペンシル/行追加で表示されない、の根因)
function deleteRow(canvas: HTMLCanvasElement, n: number): void {
  if (n < 1 || n > rows.length) return;
  takeUndoSnap();
  rows.splice(n - 1, 1);
  // 全ての図形について、削除行より後ろの親参照をずらす
  for (const r of rows) {
    const pn = intOrNull(r.parent);
    if (pn === null) continue;
    if (pn === n) {
      // 親を失った子は独立に落とす (現値を保ったまま辺A を自分の辺に切り出す)
      r.parent = '-1';
      r.conn = r.kind === 'rectangle' ? '0' : '-1';
      relinkEdgeA(r);
    } else if (pn > n) {
      r.parent = String(pn - 1);
    }
  }
  shiftOverridesAfterDelete(n);
  clearSelection();
  current = Math.min(n, rows.length);
  // 新規行フォームの残留プリセット (消えた図形への親番号/接続) を掃除する
  const np = intOrNull(input('newParent').value.trim());
  if (np !== null && (np < 1 || np > rows.length)) {
    input('newParent').value = '';
    select('newConn').value = '-1';
    input('newA').value = '';
  }
  buildTable(canvas);
  syncForm();
  redraw(canvas);
  setStatus(`Delete Row #${n}`);
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
  // 控除モード: 選択中の控除を削除 (アプリ fab_minus は getList(deductionMode).remove:1283)
  if (deductionMode) {
    dedDelete(canvas);
    return;
  }
  if (rows.length === 0) return;
  deleteRow(canvas, current > 0 ? current : rows.length);
}

// 回転 FAB (MainActivity.kt:1394-1400 → fabRotate(±5f)): 図形全体を原点 (0,0) 回りに 5° 刻みで
// 回す。実体は listAngle の加算 1 つで、幾何は redraw → wasm の recoverState が全再構築で適用する。
// アプリ同様 undo スナップは取らない (逆回転で戻せる)。view は据え置き (アプリも invalidate のみ)
function fabRotate(canvas: HTMLCanvasElement, degrees: number): void {
  // 控除モード: 選択中の Box 控除の形だけ ±5° (アプリ fabRotate の ded 分岐:1593-1600
  // current_deduction.rotateShape — Circle は common 側の type==Box ガードで無変化)
  if (deductionMode) {
    if (dedSelected < 1) {
      setStatus('回転: 先に控除を選択 (Box のみ回る)');
      return;
    }
    const d = parseDedLine(dedLines[dedSelected - 1] ?? '');
    if (d && d.type !== 'Box') {
      setStatus(`回転: 控除 ${dedSelected} は ${d.type || '円'} — 回転は Box のみ`);
      return;
    }
    dedLines[dedSelected - 1] = rotateDeductionShape(dedLines[dedSelected - 1] ?? '', degrees);
    buildDedTable(canvas);
    redraw(canvas);
    setStatus(`控除 ${dedSelected} を回転`);
    return;
  }
  if (rows.length === 0) return;
  // 回転の見かけの起点 = 図形全体 (図面枠を除く) の境界ボックス中央 (2026-06-12 user 要望)。
  // 幾何は listAngle → recoverState が三角形 1 基準で回すので、画面上では図形が振り回される —
  // 回転前後の bbox 中央が同じ画面点に写るよう view を平行移動して「中央起点の回転」に見せる。
  // 幾何・CSV・DXF には触れない view 層だけの補正
  const v = view;
  const before = v ? figureCenter(lastPrims) : null;
  listAngle += degrees;
  // シャドー三角形は親辺の現座標を借りて組むので、回転で親辺が動いたら組み直さないと
  // 旧座標のまま置き去りになる (2026-06-13 user 報告)。serializeState は更新後の listAngle を
  // 書くので、ここで buildShadow すれば回転後の親辺に乗る。edgeSel が立つ時だけ再構築
  if (edgeSel) buildShadow();
  // 控除の連動 (MainActivity.kt:1587 myDeductionList.rotate(origin, -degrees) 準拠)。
  // 三角形は listAngle → recoverState が回すが、控除の CSV 座標は絶対値なので
  // 行自体を common (rotateDeductionLine) で回して書き直す
  dedLines = dedLines.map((l) => rotateDeductionLine(l, degrees));
  buildDedTable(canvas);
  setStatus(`回転: ${listAngle}°`);
  redraw(canvas);
  if (v && before) {
    const after = figureCenter(lastPrims);
    v.offsetX += (before.x - after.x) * v.scale;
    v.offsetY += (after.y - before.y) * v.scale; // 画面 y は反転系 (py = -y*scale + offsetY)
    draw(canvas, lastPrims);
  }
}

// 図形全体 (図面枠を除く) の境界ボックス中央。枠は図形中心に追従して動くので
// 含めると補正が自己参照になる — 図形本体だけで取る
function figureCenter(prims: Prim[]): { x: number; y: number } {
  const b = bounds(prims.filter((p) => p.type !== 'meta' && p.layer !== 'frame'));
  return { x: (b.minX + b.maxX) / 2, y: (b.minY + b.maxY) / 2 };
}

// 押しっぱなしリピート: pointerdown で即 1 ステップ → 350ms 後から 120ms 間隔で連発。
// pointer capture で押下中にポインタがボタンを外れても up を取りこぼさない
function wireHoldRepeat(btn: HTMLButtonElement, step: () => void): void {
  let delay: number | null = null;
  let timer: number | null = null;
  const stop = () => {
    if (delay !== null) clearTimeout(delay);
    if (timer !== null) clearInterval(timer);
    delay = null;
    timer = null;
  };
  btn.addEventListener('pointerdown', (e) => {
    e.preventDefault();
    try {
      btn.setPointerCapture(e.pointerId);
    } catch {
      // 合成イベント (CP/E2E) は capture 不能でもリピート自体は成立する
    }
    step();
    delay = window.setTimeout(() => {
      timer = window.setInterval(step, 120);
    }, 350);
  });
  btn.addEventListener('pointerup', stop);
  btn.addEventListener('pointercancel', stop);
}

// 塗り色 FAB (MainActivity.kt:1367-1377 setCommonFabListener(fab_fillcolor) の写し):
// 押すたび colorIndex を一周 (0..4) させ、選択三角形の CSV 列 10 (= Triangle.mycolor、
// CsvCodec.applyRowMeta:206 が setColor で読む) に書く。FAB の背景も現在色に追従
// (アプリの backgroundTintList = resColors[colorindex]:1373 相当)。
// アプリ同様 undo スナップは取らない (もう一周で戻せる)、控除モード中は何もしない
const FILL_NAMES = ['ピンク', 'オレンジ', 'イエロー', 'ライム', 'スカイ'];

function syncFillColorFab(): void {
  const b = document.getElementById('fabFillColor');
  if (b instanceof HTMLButtonElement) b.style.background = FILL_PALETTE[colorIndex];
}

function fabFillColor(canvas: HTMLCanvasElement): void {
  if (deductionMode) return; // アプリの if(!deductionMode) ガード (MainActivity.kt:1368) と同じ
  const n = selected > 0 ? selected : current;
  const r = rows[n - 1];
  if (!r) {
    setStatus('色: 先に三角形を選択してください');
    return;
  }
  colorIndex = (colorIndex + 1) % FILL_PALETTE.length;
  // CSV 列 10 = extras[4] (extras は列 6 以降)。間の列 7-9 (番号サークル) は空文字で
  // 埋める — 空文字は CsvCodec.applyRowMeta が無視する (toFloatOrNull/toBoolean が効かない)
  while (r.extras.length < 5) r.extras.push('');
  r.extras[4] = String(colorIndex);
  syncFillColorFab();
  redraw(canvas);
  setStatus(`色: 三角形 ${n} → ${FILL_NAMES[colorIndex]} (${colorIndex})`);
}

// texplus/minus FAB (MainActivity.kt:1381-1392): textSize ±5f → setAllTextSize で全寸法文字に
// 反映。web は textSizeCsv (CSV の TextSize 行の SoT) を ±5 して再描画するだけ —
// 反映は serializeState → CsvCodec.parse → WebPrimitiveRenderer の比率計算が担う
function fabTexSize(canvas: HTMLCanvasElement, delta: number): void {
  if (rows.length === 0) return;
  textSizeCsv = adjustTextSize(textSizeCsv + delta);
  redraw(canvas);
  setStatus(`寸法文字サイズ: ${textSizeCsv} (アプリ初期値 30、8〜80)`);
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

// ズーム維持でモデル点を画面中央へ pan (アプリ MyView.resetView(point) 相当)。
// view が未確定 (null) なら次の draw の bounds-fit に任せる
function panTo(canvas: HTMLCanvasElement, x: number, y: number): void {
  const v = view;
  if (!v) {
    draw(canvas, lastPrims);
    return;
  }
  v.offsetX = canvas.width / 2 - x * v.scale;
  v.offsetY = canvas.height / 2 + y * v.scale;
  draw(canvas, lastPrims);
}

// 控除モードの上下 FAB: カレント控除を ±1 して、その控除へビューを寄せる
// (アプリ fab_up/down の控除分岐 = myEditor.scroll ± resetViewToCurrentDeduction:1446-1466)
function moveCurrentDed(canvas: HTMLCanvasElement, delta: number): void {
  if (dedLines.length === 0) return;
  const base = dedSelected > 0 ? dedSelected : dedLines.length;
  const n = Math.min(Math.max(base + delta, 1), dedLines.length);
  selectDed(canvas, n);
  const d = parseDedLine(dedLines[n - 1] ?? '');
  if (d && Number.isFinite(d.x) && Number.isFinite(d.y)) panTo(canvas, d.x, d.y);
  setStatus(`控除 current: ${n}`);
}

// リスト上下 FAB (アプリ fab_up/down → EditorTable.scroll(±1) + moveTrilist:1691):
// カレント行を ±1 して図の選択も追従させる
function moveCurrent(canvas: HTMLCanvasElement, delta: number): void {
  if (deductionMode) {
    moveCurrentDed(canvas, delta);
    return;
  }
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
  // 控除モード: 選択中の控除の名称フラッグ (pointFlag) をカーソル位置へ移動
  // (アプリ fabFlag:1575-1582 → flagDeduction:1558-1566 の分岐。pointflag = pressedInModel)
  if (deductionMode) {
    if (dedSelected < 1) {
      setStatus('旗揚げ: 先に控除を選択してください');
      return;
    }
    if (!dedCursor) {
      setStatus('旗揚げ: 移動先をクリックしてから押してください');
      return;
    }
    const c = (dedLines[dedSelected - 1] ?? '').split(',').map((s) => s.trim());
    if (c[0] !== 'Deduction' || c.length < 13) return;
    // 列 10/11 = pointFlag (dedLines はモデル座標、WebDeduction.serializeDeduction:74-79 が SoT)
    c[10] = String(dedCursor.x);
    c[11] = String(dedCursor.y);
    dedLines[dedSelected - 1] = c.join(',');
    redraw(canvas);
    setStatus(`控除 ${dedSelected} の旗揚げを移動した`);
    return;
  }
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
  // 移動先がカレント三角形の内側なら手動旗揚げを解除して自動配置に戻す
  // (2026-06-12 user 要望「内側にあるときは手動旗揚げが OFF」)。当たり判定は
  // handleTap と同じ common の isCollide (WebHitTest.hitTriangle)
  if (hitTriangle(serializeState(), dest.x, dest.y) === n) {
    overrides.numbers = overrides.numbers.filter((o) => o.tri !== n);
    // ロード CSV に焼き込まれた手動配置 (列 9 = 移動フラグ、CsvCodec.applyRowMeta:219-223
    // が true のとき列 7/8 を当てる) も倒す。列 7/8 の座標値はフラグ false なら読まれない
    const r = rows[n - 1];
    if (r && r.extras.length > 3) r.extras[3] = 'false';
    redraw(canvas);
    setStatus(`番号 ${n} の旗揚げを解除した (自動配置に戻る)`);
    return;
  }
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

// ---- 控除モード (アプリ flipDeductionMode:980-1035 の web 版) ----
// アプリ: 控除 FAB でモード切替 → タップ位置がカーソル (pressedInModel) → 寸法入力 →
// ペンシルで addDeductionBy。web: 控除 FAB でモード切替 → クリックで十字カーソル →
// フォーム (名称/寸法1/寸法2) → 追加。幾何は common の placeDeduction が全部やる

function toggleDeductionMode(canvas: HTMLCanvasElement): void {
  deductionMode = !deductionMode;
  el<HTMLButtonElement>('fabDeduction').classList.toggle('modeon', deductionMode);
  // FAB 群の見かけ変更 (アプリ updateFabAppearance:1028 / setDeductionFab:1037 の web 版)。
  // 色は CSS の body.dedmode が一括制御、旗アイコンはアプリ同様 flag_b ↔ flag を swap
  document.body.classList.toggle('dedmode', deductionMode);
  const flagImg = document.querySelector<HTMLImageElement>('#fabFlag img');
  if (flagImg)
    flagImg.src = deductionMode
      ? `${import.meta.env.BASE_URL}icons/flag.png` // 赤 FLAG (アプリ共通 PNG、白点灯背景に映える)
      : `${import.meta.env.BASE_URL}micons/flag.svg`; // Material flag (flag_b.png は縮小で潰れるため)
  // 回転 FAB アイコン swap (アプリ setDeductionFab:1049-1055 rot_dl/rot_dr ⇔ rot_l/rot_r)
  const rotL = document.querySelector<HTMLImageElement>('#fabRotL img');
  const rotR = document.querySelector<HTMLImageElement>('#fabRotR img');
  if (rotL) rotL.src = `${import.meta.env.BASE_URL}icons/${deductionMode ? 'rot_dl' : 'rot_l'}.png`;
  if (rotR) rotR.src = `${import.meta.env.BASE_URL}icons/${deductionMode ? 'rot_dr' : 'rot_r'}.png`;
  const details = document.getElementById('dedDetails') as HTMLDetailsElement | null;
  if (details && deductionMode) details.open = true;
  if (!deductionMode) {
    dedCursor = null;
    dedSelected = 0;
    updateDedHighlight();
  }
  draw(canvas, lastPrims);
  setStatus(
    deductionMode
      ? '控除モード ON — 図をクリックして位置を決め、名称と寸法を入れて「追加」(寸法2 空 or 0 → 円)'
      : '控除モード OFF',
  );
}

function updateDedHighlight(): void {
  const tbody = document.getElementById('dedRows');
  if (!tbody) return;
  Array.from(tbody.children).forEach((tr, i) => {
    tr.classList.toggle('selected', i + 1 === dedSelected);
  });
}

// 控除行クリック = 選択 + フォームへ読み込み (アプリの控除タップ → editorResetBy 相当)
function selectDed(canvas: HTMLCanvasElement, n: number): void {
  dedSelected = n;
  const d = n > 0 ? parseDedLine(dedLines[n - 1] ?? '') : null;
  if (d) {
    input('dedName').value = d.name;
    input('dedLenX').value = d.lenX;
    input('dedLenY').value = d.type === 'Box' ? d.lenY : '';
  }
  updateDedHighlight();
  draw(canvas, lastPrims);
  if (d) setStatus(`控除 ${n} を選択 (${d.type === 'Box' ? '長方形' : '円'} ${d.name})`);
}

function buildDedTable(canvas: HTMLCanvasElement): void {
  const tbody = document.getElementById('dedRows');
  if (!tbody) return;
  tbody.textContent = '';
  dedLines.forEach((line, i) => {
    const d = parseDedLine(line);
    if (!d) return;
    const tr = document.createElement('tr');
    tr.className = 'dedrow';
    if (i + 1 === dedSelected) tr.classList.add('selected');
    tr.addEventListener('click', () => selectDed(canvas, i + 1));
    const cells = [
      String(i + 1),
      d.name,
      fmt2(d.lenX),
      d.type === 'Box' ? fmt2(d.lenY) : '',
      d.type === 'Box' ? '長方形' : '円',
      d.pn === '0' ? '-' : d.pn,
    ];
    for (const c of cells) {
      const td = document.createElement('td');
      td.textContent = c;
      tr.appendChild(td);
    }
    // 明示的な選択ボタン (行クリックでも選択できるが、ボタンで意図を見える化 — user 指定)
    const tdSel = document.createElement('td');
    const sel = document.createElement('button');
    sel.type = 'button';
    sel.textContent = '選択';
    sel.addEventListener('click', (e) => {
      e.stopPropagation();
      selectDed(canvas, i + 1);
    });
    tdSel.appendChild(sel);
    tr.appendChild(tdSel);
    const tdDel = document.createElement('td');
    const del = document.createElement('button');
    del.type = 'button';
    del.className = 'del';
    del.textContent = '削除';
    del.addEventListener('click', (e) => {
      e.stopPropagation();
      dedDelete(canvas, i + 1);
    });
    tdDel.appendChild(del);
    tr.appendChild(tdDel);
    tbody.appendChild(tr);
  });
}

// validDeduction (MainActivity.kt:1190-1202) と同じ関門: 名前非空 + 寸法1>=0.1 +
// 長方形 (寸法2>0) は寸法2>=0.1。state を汚す前に弾く (三角形側の共通関門と同じ流儀)
function readDedForm(): { name: string; lenX: number; lenY: number } | null {
  const name = input('dedName').value.trim();
  const lenX = parseFloat(input('dedLenX').value);
  const lenYRaw = input('dedLenY').value.trim();
  const lenY = lenYRaw === '' ? 0 : parseFloat(lenYRaw);
  if (name === '') {
    setStatus('⚠ 控除: 名称を入力してください');
    return null;
  }
  if (!Number.isFinite(lenX) || lenX < 0.1) {
    setStatus('⚠ 控除: 寸法1 (円の直径 / 長方形の幅) は 0.1 以上が必要です');
    return null;
  }
  if (!Number.isFinite(lenY) || (lenY > 0 && lenY < 0.1)) {
    setStatus('⚠ 控除: 寸法2 は空 (=円) か 0.1 以上 (=長方形) にしてください');
    return null;
  }
  return { name, lenX, lenY };
}

// 追加 (アプリ addDeductionBy:1752): カーソル位置に配置。完成行は common が返す
function dedAdd(canvas: HTMLCanvasElement): void {
  const f = readDedForm();
  if (!f) return;
  if (!dedCursor) {
    setStatus('⚠ 控除: 先に図をクリックして位置を決めてください (アプリの「先にタップ」と同じ)');
    return;
  }
  const line = placeDeduction(serializeState(), dedCursor.x, dedCursor.y, f.name, f.lenX, f.lenY, dedLines.length + 1);
  if (line === '') {
    setStatus('⚠ 控除: 追加できません (パラメータを確認してください)');
    return;
  }
  takeUndoSnap();
  dedLines.push(line);
  dedSelected = dedLines.length;
  buildDedTable(canvas);
  redraw(canvas);
  const d = parseDedLine(line);
  setStatus(`控除 ${dedLines.length} を追加 (${d?.type === 'Box' ? '長方形' : '円'}${d && d.pn !== '0' ? `、三角形 ${d.pn} 内` : '、親なし'})`);
}

// 置換 (アプリ resetDeductionsBy:1822 = flagDeduction やり直し): 新しいカーソルが
// あればそこへ、無ければ既存位置のまま寸法・名称だけ更新
function dedReplace(canvas: HTMLCanvasElement): void {
  if (dedSelected < 1) {
    setStatus('⚠ 控除: 置換する控除を一覧で選択してください');
    return;
  }
  const f = readDedForm();
  if (!f) return;
  const old = parseDedLine(dedLines[dedSelected - 1] ?? '');
  const at = dedCursor ?? (old && Number.isFinite(old.x) && Number.isFinite(old.y) ? { x: old.x, y: old.y } : null);
  if (!at) {
    setStatus('⚠ 控除: 位置が決まっていません — 図をクリックしてください');
    return;
  }
  const line = placeDeduction(serializeState(), at.x, at.y, f.name, f.lenX, f.lenY, dedSelected);
  if (line === '') {
    setStatus('⚠ 控除: 置換できません (パラメータを確認してください)');
    return;
  }
  takeUndoSnap();
  dedLines[dedSelected - 1] = line;
  buildDedTable(canvas);
  redraw(canvas);
  setStatus(`控除 ${dedSelected} を置換`);
}

// 削除 (DeductionList.remove:122 と同じ詰め + renum)
function dedDelete(canvas: HTMLCanvasElement, n?: number): void {
  const target = n ?? dedSelected;
  if (target < 1 || target > dedLines.length) {
    setStatus('⚠ 控除: 削除する控除を一覧で選択してください');
    return;
  }
  takeUndoSnap();
  dedLines.splice(target - 1, 1);
  renumberDedLines();
  dedSelected = 0;
  buildDedTable(canvas);
  redraw(canvas);
  setStatus(`控除 ${target} を削除`);
}

// 控除モード中のクリック: 既存控除の上なら選択、それ以外はカーソル位置決め
function handleDedTap(canvas: HTMLCanvasElement, px: number, py: number, m: { x: number; y: number }): void {
  // 当たり判定はアプリ Deduction.getTap:144-150 準拠 (モデル単位、myscale=1):
  // 図形中心から lengthX 以内、または名称フラッグから 0.5 以内でその控除を選択
  for (let i = 0; i < dedLines.length; i++) {
    const d = parseDedLine(dedLines[i]);
    if (!d || !Number.isFinite(d.x) || !Number.isFinite(d.y)) continue;
    const range = Math.max(parseFloat(d.lenX) || 0, 0.3); // 極小図形でも掴める下限
    const hitShape = Math.hypot(d.x - m.x, d.y - m.y) <= range;
    const hitFlag = Number.isFinite(d.fx) && Number.isFinite(d.fy) && Math.hypot(d.fx - m.x, d.fy - m.y) <= 0.5;
    if (hitShape || hitFlag) {
      selectDed(canvas, i + 1);
      return;
    }
  }
  dedCursor = m;
  draw(canvas, lastPrims);
  setStatus(`控除カーソル: (${m.x.toFixed(2)}, ${m.y.toFixed(2)}) — 名称と寸法を入れて「追加」`);
}

// 新規作成 (段階2f): 現在の図を捨て、最初の 1 個 (独立 3.00/3.00/3.00) を生成して
// 全体フィットから始める (空画面でなく即編集に入れる形)。誤爆は confirm + undo 1 段で守る
function newDrawing(canvas: HTMLCanvasElement): void {
  if (rows.length > 0 && !window.confirm('現在の図を破棄して新規作成しますか?')) return;
  takeUndoSnap();
  // ヘッダはデフォルト空白 — 図面枠の欄を dblclick すればその場で書ける
  // (「無題工事とか書くより空白にしておいた方が良い」2026-06-12 user 指示)
  headerLines = ['koujiname,', 'rosenname,', 'gyousyaname,', 'zumennum,'];
  edges = [];
  rows = [{ kind: 'triangle', ea: newEdge('3.00'), eb: newEdge('3.00'), ec: newEdge('3.00'), parent: '-1', conn: '-1', extras: [] }];
  listAngle = 0; // アプリ createNew は TriangleList を作り直す = angle 0 (回転 FAB の残留を消す)
  overrides = { dims: [], numbers: [] };
  lastTapModel = null;
  dedLines = [];
  dedSelected = 0;
  dedCursor = null;
  // 台形は rows に統合済 — rows の作り直しで台形も破棄される (別 state は無い)
  buildDedTable(canvas);
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
  // 塗り色サイクル (アプリ fab_fillcolor) + 寸法文字 ± (アプリ fab_texplus/fab_texminus)
  el<HTMLButtonElement>('fabFillColor').addEventListener('click', () => fabFillColor(canvas));
  el<HTMLButtonElement>('fabTexPlus').addEventListener('click', () => fabTexSize(canvas, 5));
  el<HTMLButtonElement>('fabTexMinus').addEventListener('click', () => fabTexSize(canvas, -5));
  syncFillColorFab(); // FAB 背景を初期色 (index 4 = sky) に
  el<HTMLButtonElement>('fabReplace').addEventListener('click', () => fabReplace(canvas));
  el<HTMLButtonElement>('fabUndo').addEventListener('click', () => fabUndo(canvas));
  el<HTMLButtonElement>('fabMinus').addEventListener('click', () => fabMinus(canvas));
  // resetView FAB: 既存 dblclick fit と同じ動作のボタン化。控除モードで控除を選択中なら
  // その控除へ寄せる (アプリ fab_resetView の resetViewToCurrentDeduction:1440 分岐)
  el<HTMLButtonElement>('fabResetView').addEventListener('click', () => {
    if (deductionMode && dedSelected > 0) {
      const d = parseDedLine(dedLines[dedSelected - 1] ?? '');
      if (d && Number.isFinite(d.x) && Number.isFinite(d.y)) {
        panTo(canvas, d.x, d.y);
        return;
      }
    }
    view = null;
    draw(canvas, lastPrims);
  });
  // 回転 FAB: アプリ fabs.xml 左上横列 [resetView][rot_l][rot_r] と同じ並び・同じ符号 (左=+5°)
  // 回転は押しっぱなしで継続 (2026-06-12 user 要望)。pointerdown で 1 ステップ +
  // 長押しでリピート — click 配線だと離した時にもう 1 ステップ入るので click は使わない
  wireHoldRepeat(el<HTMLButtonElement>('fabRotL'), () => fabRotate(canvas, 5));
  wireHoldRepeat(el<HTMLButtonElement>('fabRotR'), () => fabRotate(canvas, -5));
  el<HTMLButtonElement>('fabUp').addEventListener('click', () => moveCurrent(canvas, -1));
  el<HTMLButtonElement>('fabDown').addEventListener('click', () => moveCurrent(canvas, 1));
  // 控除モード (アプリ fab_deduction → flipDeductionMode 相当)
  el<HTMLButtonElement>('fabDeduction').addEventListener('click', () => toggleDeductionMode(canvas));
  // 新規図形の種別トグル (三角形⇄台形)。単発クリックなので addEventListener でよい
  el<HTMLButtonElement>('fabFigureKind').addEventListener('click', () => toggleFigureKind());
  el<HTMLButtonElement>('fabReshape').addEventListener('click', () => reshapeCurrent(canvas));
  updateFigureKindFab(); // 初期表示を現在種別 (三角形) に
  el<HTMLButtonElement>('dedAdd').addEventListener('click', () => dedAdd(canvas));
  el<HTMLButtonElement>('dedReplace').addEventListener('click', () => dedReplace(canvas));
  el<HTMLButtonElement>('dedDelete').addEventListener('click', () => dedDelete(canvas));
}

// 新規行・カレント行の Enter ナビゲーション (アプリの EditText imeOptions=actionNext 相当):
// Enter で隣のセルへ移動し、辺C で Enter すると確定する — 新規行は三角形を追加、
// カレント行は current 三角形の書換え (rewriteCurrent)。
// IME 変換確定の Enter (isComposing) は無視する (測点名の日本語入力を壊さない)
function wireNewRowEnter(canvas: HTMLCanvasElement): void {
  // 親番号 input の値変更で syncForm 呼出 → 接続辺プルダウン (newConn / curConn) の
  // option 集合を親 kind で更新 (= 親が rectangle なら D 辺 option を加える、
  // 2026-06-18 user 報告「行を追加で三角形を生成する動線で、 親が rectangle なのに
  // D 辺の選択肢が出ない」)。 input event = keystroke ごと、 change event = blur 時。
  for (const id of ['newParent', 'curParent']) {
    const el = document.getElementById(id);
    if (el instanceof HTMLInputElement) {
      el.addEventListener('input', () => syncForm());
    }
  }
  // シャドーの B/C ラベルはタイプ中の値を写す (draw が newB/newC を都度読む) —
  // 入力のたび再描画してアプリの watched strings と同じライブ追従にする
  for (const id of ['newB', 'newC']) {
    input(id).addEventListener('input', () => {
      // 台形に乗せる三角形は実 B/C で形が決まる (底辺は台形辺固定) → タイプのたびシャドーを
      // 作り直してライブ追従させる (粘土をこねる操作感)。三角形辺の仮シャドーは固定形 (0.75 脚)
      // なので従来どおり再描画のみ — B/C はラベルだけ写す。
      if (pendingTrapParent) buildShadow();
      if (shadowPrims) draw(canvas, lastPrims);
    });
  }
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
    ['curName', 'curA'],
    ['curA', 'curB'],
    ['curB', 'curC'],
    ['curParent', 'curConn'],
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
  // カレント行の辺C で Enter = 書換え確定。fabReplace 経由だと新規行 B の残値で
  // 「追加」分岐に化けるので、書換えを直接呼ぶ
  input('curC').addEventListener('keydown', (e) => {
    if (e.key !== 'Enter' || e.isComposing) return;
    e.preventDefault();
    rewriteCurrent(canvas);
  });
}

// ---- ヘッダ 4 行 (工事名/路線名/業者名/図面番号) の読み書き ----
// 値の取り出しは WebDrawingExport.parseHeader と同じ二形式対応:
// web 最小形式 = 行全体が値、app 完全形式 = `koujiname,<値>` の 2 カラム目が値
const HEADER_LABELS = ['koujiname', 'rosenname', 'gyousyaname', 'zumennum'];

function headerValueAt(i: number): string {
  const line = headerLines[i] ?? '';
  const chunks = line.split(',').map((s) => s.trim());
  return HEADER_LABELS.includes(chunks[0]) ? (chunks[1] ?? '') : line.trim();
}

function setHeaderValueAt(i: number, v: string): void {
  while (headerLines.length <= i) {
    // ヘッダの無い最小 CSV に途中の欄を書いたらラベル付きの空欄で補う —
    // 空でも行が残るので round-trip で欄がずれない
    headerLines.push(`${HEADER_LABELS[headerLines.length]},`);
  }
  const chunks = (headerLines[i] ?? '').split(',').map((s) => s.trim());
  headerLines[i] = HEADER_LABELS.includes(chunks[0])
    ? `${chunks[0]},${v.replace(/,/g, ' ')}` // ラベル形式は区切りを壊す comma を空白へ
    : v;
}

// 路線名: アプリの editor_table の路線名欄に相当 (ヘッダ 2 行目)。
// 図面枠 ON のときは枠タイトルにも印字される (WebFrame.renderFrame) ので、
// タイプのたび redraw して図に追従させる (2026-06-12 user 要望)
function syncRosenName(): void {
  input('rosenName').value = headerValueAt(1);
}

function wireRosenName(canvas: HTMLCanvasElement): void {
  input('rosenName').addEventListener('input', () => {
    setHeaderValueAt(1, input('rosenName').value);
    redraw(canvas); // autosave は redraw 内で走る
  });
  input('rosenName').addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.isComposing) input('rosenName').blur();
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
  let best: { tri: number; side: 0 | 1 | 2; d: number } | null = null;
  for (const p of lastPrims) {
    if (p.type !== 'line' || p.layer !== 'tri') continue;
    const l = p as LinePrim;
    if (l.tri === undefined || l.side === undefined) continue;
    const r = rows[l.tri - 1];
    if (!r || r.kind !== 'triangle') continue; // 三角形のみ
    const d = distToSegmentPx(px, py, l);
    if (d <= EDGE_TAP_PX && (!best || d < best.d)) {
      best = { tri: l.tri, side: l.side as 0 | 1 | 2, d };
    }
  }
  return best;
}

// 台形の辺タップ。identifier base 化に伴い tside (= 描画順 index) は廃止し、最初から物理 side を
// 返す (handleTap 側の trapTsideToSide 経由を廃止、render 順と物理 side の写像が一箇所から消える)。
// trap = 台形群内の連番 (1 始まり)、global 番号 = triCount()+trap。
function nearestTrapEdge(px: number, py: number): { tri: number; side: 0 | 1 | 2 | 3; d: number } | null {
  let best: { tri: number; side: 0 | 1 | 2 | 3; d: number } | null = null;
  for (const p of lastPrims) {
    if (p.type !== 'line' || p.layer !== 'tri') continue;
    const l = p as LinePrim;
    if (l.tri === undefined || l.side === undefined) continue;
    const r = rows[l.tri - 1];
    if (!r || r.kind !== 'rectangle') continue; // 台形のみ
    const d = distToSegmentPx(px, py, l);
    if (d <= EDGE_TAP_PX && (!best || d < best.d)) {
      best = { tri: l.tri, side: l.side as 0 | 1 | 2 | 3, d };
    }
  }
  return best;
}

function trapSideName(side: number): string {
  return side === 0 ? '底辺' : side === 1 ? '左脚' : side === 2 ? '上辺' : side === 3 ? '右脚' : '辺';
}

// 台形の指定 side の辺長を lastPrims から引く。tri は混在通し番号 (1 始まり)。
function trapEdgeLen(tri: number, side: number): number {
  for (const p of lastPrims) {
    if (p.type !== 'line' || p.layer !== 'tri') continue;
    const l = p as LinePrim;
    if (l.tri === tri && l.side === side) {
      return Math.hypot(l.x2 - l.x1, l.y2 - l.y1);
    }
  }
  return 0;
}

// 台形を図形選択し、どの辺かをフィードバックする。tri は混在通し番号 (1 始まり)。
function selectRectangle(canvas: HTMLCanvasElement, tri: number, side: number): void {
  selected = tri;
  current = tri;
  // 黄色ハイライト (selectedDim) は三角形辺タップと共通動線。底辺 (接続不可) も視覚的に
  // どこを触ったか分かるよう立てる (user 指摘 2026-06-14「辺選択ガイドが出てない」)。
  selectedDim = { tri, side };
  edgeSel = null;
  shadowPrims = null;
  updateRowHighlight();
  syncForm();
  draw(canvas, lastPrims);
  const name = trapSideName(side);
  if (side <= 0) setStatus(`台形 #${tri} の ${name} (親の接続辺なので追加先には使えない)`);
  else setStatus(`台形 #${tri} の ${name}辺 (接続 side ${side}) を選択`);
}

// 台形の自由辺をタップ → 親=台形のスロット (pendingTrapParent) を立てる。
// tri は混在通し番号 (1 始まり)。
function presetTriOnTrap(canvas: HTMLCanvasElement, tri: number, side: number): void {
  // 既接続ガード (三角形版 selectEdge と対称)。
  const child = isEdgeOccupied(tri, side);
  if (child > 0) {
    selectRectangle(canvas, tri, side);
    const childKind = rows[child - 1]?.kind === 'rectangle' ? '台形' : '三角形';
    setStatus(`台形 #${tri} の ${trapSideName(side)}辺 — ${childKind} #${child} が接続済み (追加不可、選択のみ)`);
    return;
  }
  selected = tri;
  current = tri;
  // 三角形辺タップ (selectEdge) と同じプリセット動線:
  //   - selectedDim で黄色ハイライト (子の種別問わず立てる)
  //   - newA に親辺長 (台形親なら trapEdgeLen)
  //   - newConn / newCType を接続情報に反映 (子問わず、台形親 = newCType '1')
  // (user 指摘 2026-06-14「親の選択辺長が新規 A に入ってない/接続辺情報も切り替わらない」)
  selectedDim = { tri, side };
  edgeSel = null;
  pendingTrapParent = { parent: tri, side };
  input('newParent').value = String(tri);
  const baseLen = trapEdgeLen(tri, side);
  if (baseLen > 0) input('newA').value = fmt2(baseLen.toFixed(2));
  // 接続辺 options を再生成して B/C/D を含めてから value を assign する。親種別 (=台形)は
  // pendingTrapParent != null から自動推論されるので、形態列 (newCType) を「親種別」に流用しない
  // (user 指摘 2026-06-14「台形の形態のところに親の種類を書いてること自体が間違い」)。
  setSelectOptions(select('newConn'), connOptionsFor('new', 1));
  select('newConn').value = String(side);
  syncLcrDisabled('new');
  buildShadow();
  updateRowHighlight();
  syncForm();
  draw(canvas, lastPrims);
  const childWord = figureKind === 'rectangle' ? '台形' : '三角形';
  const inputHint = figureKind === 'rectangle' ? '延長/上辺' : 'B/C';
  setStatus(`台形 #${tri} の ${trapSideName(side)}辺 — ${inputHint} を入力して ✎ で${childWord}を乗せる`);
  input('newB').focus();
}

// pendingTrapParent (台形辺タップ済) のとき、新規行の B/C で三角形を末尾に積む。
// 追加経路 fabReplace の三角形親ガード (親番号 <= triCount) は台形親に通らないので、ここで直に作る。
function addTriOnTrap(canvas: HTMLCanvasElement, newB: string, newC: string): void {
  const pend = pendingTrapParent;
  if (!pend) return;
  if (!(parseFloat(newB) > 0) || !(parseFloat(newC) > 0)) {
    setStatus('⚠ 台形に乗せる三角形は B・C に正の値が必要です — 追加を中止');
    return;
  }
  takeUndoSnap();
  const name = input('newName').value;
  // 底辺A は親台形の対応辺と **同じ辺プール index** を共有させる (三角形→三角形と同じ規約)。
  // side 規約: 1=B左脚 (=延長)、2=C上辺、3=D右脚 (D は Rectangle 上の延長と同値)。
  const trapRow = rows[pend.parent - 1];
  const eaPoolIdx = trapRow && trapRow.kind === 'rectangle'
    ? (pend.side === 2 ? trapRow.ec : trapRow.eb)
    : newEdge(trapEdgeLen(pend.parent, pend.side).toFixed(2));
  const newRow = createTriangleRow({
    ea: eaPoolIdx,
    eb: newEdge(newB),
    ec: newEdge(newC),
    parent: String(pend.parent),
    conn: String(pend.side),
    extras: name !== '' ? [name] : [],
  });
  rows.push(newRow);
  pendingTrapParent = null;
  edgeSel = null;
  shadowPrims = null;
  input('newName').value = '';
  input('newA').value = '';
  input('newB').value = '';
  input('newC').value = '';
  input('newParent').value = '';
  select('newConn').value = '-1';
  selected = rows.length;
  current = rows.length;
  view = null; // 行を増やしたので全体 fit に戻す (削除側 main.ts:2484 と対称)
  buildTable(canvas);
  syncForm();
  redraw(canvas);
  setStatus(`台形 ${pend.parent} の ${trapSideName(pend.side)}辺に三角形を追加`);
}

// 仮挿入した行の line prim を「prim 並び順」 ではなく **tri 番号** で同定する。
// 段3 swap (commit 659b509、 WebPrimitiveRenderer.kt:92 新 render(list)) で混在 EditList を
// 1 ループで吐くようになり、 trilist→traps→trapTris の 3 群分離は消えた。
// SoT 一本化 段3g (commit 2b1c4e5): composeAll を廃止し buildMixed が figureRows 順で
// 混在 EditList<CycleShape> を 1 本構築、 render の forEachItemIndexed (:117/130) が
// 1-based の tri 番号を振る。 仮挿入は CSV 末尾 → tri 番号は最大値。
// 段3 前の slice(-4) / [tc*3 + offset] は壊れたので、 これに置換する。
function pickShadowLines(lines: LinePrim[]): LinePrim[] {
  let maxTri = 0;
  for (const l of lines) if ((l.tri ?? 0) > maxTri) maxTri = l.tri ?? 0;
  return maxTri > 0 ? lines.filter((l) => l.tri === maxTri) : [];
}

// line 集合を端点グラフから 1 本の閉路パスに連結する。 段3 swap で台形 4 辺の出力順が
// 「side 0=底/1=右脚/2=上/3=左脚」 の並列 (前の終点 = 次の始点 にならない) になったため、
// 順次 lineTo で外形を描くと対角線が走って砂時計 X クロスになるのを防ぐ。
function chainPath(lines: LinePrim[]): { x: number; y: number }[] {
  if (lines.length === 0) return [];
  const EPS = 1e-3;
  const eq = (a: number, b: number) => Math.abs(a - b) < EPS;
  const used = new Array(lines.length).fill(false);
  used[0] = true;
  const path: { x: number; y: number }[] = [
    { x: lines[0].x1, y: lines[0].y1 },
    { x: lines[0].x2, y: lines[0].y2 },
  ];
  while (path.length <= lines.length) {
    const last = path[path.length - 1];
    let advanced = false;
    for (let i = 0; i < lines.length; i++) {
      if (used[i]) continue;
      const ln = lines[i];
      if (eq(ln.x1, last.x) && eq(ln.y1, last.y)) {
        used[i] = true;
        path.push({ x: ln.x2, y: ln.y2 });
        advanced = true;
        break;
      }
      if (eq(ln.x2, last.x) && eq(ln.y2, last.y)) {
        used[i] = true;
        path.push({ x: ln.x1, y: ln.y1 });
        advanced = true;
        break;
      }
    }
    if (!advanced) break;
  }
  // 閉路: 末尾が始点と一致したら末尾点を除いて純粋な頂点列にする
  if (path.length > 1) {
    const head = path[0];
    const tail = path[path.length - 1];
    if (eq(head.x, tail.x) && eq(head.y, tail.y)) path.pop();
  }
  return path;
}

// シャドー (接続プレビュー) を組む。figureKind で「次に足す図形」が三角形か台形かを分ける (段5 R6)。
// 仮の行を足した CSV を common に描かせ、その図形の辺を借りる — TS で幾何を再計算せず、
// 接続の向き・形が実際の追加結果と必ず一致する。
//   三角形モード: 仮三角形 (a=親辺長, b=c=親辺長*0.75 — MyView.drawShadowTriangle:683) の 3 辺 [A,B,C]。
//   台形モード:  仮台形 (延長=newB, 底辺=親辺長, 上辺=newC, 寄せ=newLcr) の 4 辺。
function buildShadow(): void {
  shadowPrims = null;
  // 台形辺タップ済 (親=台形のスロット)。子の種別は figureKind で分岐:
  //   triangle: RectChild 仮行を足して三角形シャドー (tri 線末尾 3 本)
  //   rectangle: parentKind=1 の仮 Rectangle 行を足して台形シャドー (tri 線末尾 4 本)。
  //              底辺=親台形辺長で固定 (共有辺、CsvCodec.buildRectangles が initByParent で同値上書き)
  if (pendingTrapParent) {
    // 親辺長を基準にデフォルトを組む。これにより「台形辺タップした瞬間にシャドーが出る」UX を
    // 三角形辺タップ (edgeSel 経路) と揃える (user 指摘 2026-06-14「台形のシャドー描画は何時に
    // なったら / 台形辺上に三角形を追加したいときの三角形シャドーも出来てない」)。
    const baseLen = trapEdgeLen(pendingTrapParent.parent, pendingTrapParent.side);
    if (!(baseLen > 0)) return;
    const bv = input('newB').value;
    const cv = input('newC').value;
    const triLeg = (baseLen * 0.75).toFixed(2);   // 三角形シャドー脚 (= 三角形辺タップ path と同係数)
    const trapExt = (baseLen * 0.5).toFixed(2);   // 台形延長 (高さ) 既定
    const trapTop = (baseLen * 0.8).toFixed(2);   // 台形上辺 既定
    if (figureKind === 'rectangle') {
      const bvE = parseFloat(bv) > 0 ? bv : trapExt;
      const cvE = parseFloat(cv) > 0 ? cv : trapTop;
      const align = intOrNull(select('newLcr').value) ?? 0;
      const trapNum = rows.filter(r => r.kind === 'rectangle').length + 1;
      const candidate = serializeState() +
        `Rectangle,${trapNum},${bvE},${baseLen.toFixed(2)},${cvE},${pendingTrapParent.parent},${pendingTrapParent.side},${align},1\n`;
      try {
        const prims = JSON.parse(renderCsvToPrimitivesWithOverrides(candidate, 1.0, overridesJson())) as Prim[];
        const triLines = prims.filter((q): q is LinePrim => q.type === 'line' && q.layer === 'tri');
        const own = pickShadowLines(triLines);
        if (own.length === 4) shadowPrims = own;
      } catch {
        shadowPrims = null;
      }
      return;
    }
    // 子三角形を台形に乗せる。
    const bvE = parseFloat(bv) > 0 ? bv : triLeg;
    const cvE = parseFloat(cv) > 0 ? cv : triLeg;
    const mixedParent = pendingTrapParent.parent;
    const newNumS = rows.length + 1;
    const candidate = serializeState() +
      `${newNumS},${baseLen.toFixed(2)},${bvE},${cvE},${mixedParent},${pendingTrapParent.side}\n`;
    try {
      const prims = JSON.parse(renderCsvToPrimitivesWithOverrides(candidate, 1.0, overridesJson())) as Prim[];
      const triLines = prims.filter((q): q is LinePrim => q.type === 'line' && q.layer === 'tri');
      const own = pickShadowLines(triLines);
      if (own.length === 3) shadowPrims = own;
    } catch {
      shadowPrims = null;
    }
    return;
  }
  if (!edgeSel) return;
  const p = rows[edgeSel.tri - 1];
  if (!p) return;
  const aStr = edgeVal(p, edgeSel.side === 1 ? 'b' : 'c');
  const L = parseFloat(aStr);
  if (!Number.isFinite(L) || L <= 0) return;
  let candidate: string;
  let take: 'tri' | 'trap';
  if (figureKind === 'rectangle') {
    const bv = input('newB').value;
    const cv = input('newC').value;
    const trapExt = (L * 0.5).toFixed(2);
    const trapTop = (L * 0.8).toFixed(2);
    const bvE = parseFloat(bv) > 0 ? bv : trapExt;
    const cvE = parseFloat(cv) > 0 ? cv : trapTop;
    const align = intOrNull(select('newLcr').value) ?? 0;
    const trapNum = rows.filter(r => r.kind === 'rectangle').length + 1;
    candidate = serializeState() +
      `Rectangle,${trapNum},${bvE},${aStr},${cvE},${edgeSel.tri},${edgeSel.side},${align},0\n`;
    take = 'trap';
  } else {
    const leg = (L * 0.75).toFixed(2);
    candidate = serializeState() + `${rows.length + 1},${aStr},${leg},${leg},${edgeSel.tri},${edgeSel.side}\n`;
    take = 'tri';
  }
  try {
    const json = renderCsvToPrimitivesWithOverrides(candidate, 1.0, overridesJson());
    const prims = JSON.parse(json) as Prim[];
    const triLines = prims.filter((q): q is LinePrim => q.type === 'line' && q.layer === 'tri');
    const own = pickShadowLines(triLines);
    // 並び (台形): renderRectangle 順 0=底辺(bl→br) 1=右脚(br→tr) 2=上辺(tr→tl) 3=左脚/延長(tl→bl)。
    if (take === 'trap') {
      if (own.length === 4) shadowPrims = own;
    } else {
      if (own.length === 3) shadowPrims = own;
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
  if (r.kind !== 'triangle') return; // 台形は辺A共有を持たない (段4)
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
// 0 = 未接続、>0 = 接続中の子図形の番号 (1 始まり)。タップ選択・B/C FAB・追加実行の
// 各動線が共通で使う (アプリは占有判定を持たない — web 版の改善, 2026-06-11 user 指示)。
function isEdgeOccupied(tri: number, side: number): number {
  return rows.findIndex((r) => intOrNull(r.parent) === tri && intOrNull(r.conn) === side) + 1;
}

// 辺タップ (アプリ targetInTriMode → autoConnection(lastTapSide) 相当):
// FAB を押さなくても新規行に接続・親・辺A がプリセットされ、シャドーが出る。
// ただし既接続辺は追加遷移 (プリセット+シャドー+フォーカス) を起こさず、選択と W/H 対象のみ
function selectEdge(canvas: HTMLCanvasElement, tri: number, side: 1 | 2): void {
  pendingTrapParent = null; // 三角形辺を選んだら台形親プリセットは解除
  const child = isEdgeOccupied(tri, side);
  if (child > 0) {
    selected = tri;
    current = tri;
    selectedDim = { tri, side };
    edgeSel = null;
    shadowPrims = null;
    updateRowHighlight();
    syncForm();
    draw(canvas, lastPrims);
    const childKind = rows[child - 1]?.kind === 'rectangle' ? '台形' : '三角形';
    setStatus(`三角形 #${tri} の ${side === 1 ? 'B' : 'C'} 辺 — ${childKind} #${child} が接続済み (追加不可、W/H・旗は可)`);
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

// ---- キャンバス内テキストの in-place 編集 (2026-06-12 user 要望) ----
// dblclick したテキストの真上に境界ボックス (黄枠) 付き input を重ね、Enter/blur で確定、
// Esc で取消。書き戻し先はテキストの素性で分岐する:
//   寸法 (dim, tri/side) = 辺プール (旧「一覧セルへジャンプ」を包含 — 表は syncEdgeInputs 追従)
//   控除 (ded, num)      = dedLines の名前列 (infoStr の番号・寸法は導出値なので名前だけ)
//   図面枠 (frame)        = headerLines (工事名/路線名/業者名/図面番号と値一致した行)
// 番号サークルの数字は自動採番なので対象外
type TextEditTarget =
  | { kind: 'dim'; prim: TextPrim; tri: number; side: 0 | 1 | 2 }
  | { kind: 'ded'; prim: TextPrim; num: number }
  | { kind: 'header'; prim: TextPrim; field: number };

function dedLineIndexByNum(num: number): number {
  return dedLines.findIndex((l) => intOrNull(l.split(',')[1]?.trim() ?? '') === num);
}

function textEditTarget(p: Prim): TextEditTarget | null {
  if (p.type !== 'text') return null;
  if (p.layer === 'dim' && p.tri !== undefined && p.side !== undefined) {
    return { kind: 'dim', prim: p, tri: p.tri, side: p.side as 0 | 1 | 2 };
  }
  if (p.layer === 'ded' && p.ded !== undefined && dedLineIndexByNum(p.ded) >= 0) {
    return { kind: 'ded', prim: p, num: p.ded };
  }
  if (p.layer === 'frame' && p.field !== undefined) {
    // 枠のタイトル欄は WebFrame.fieldAt が field タグを付けて出す (値が空欄でも出る —
    // 「空白にしておいて枠内 dblclick で書換え」2026-06-12 user 要望)。
    // 固定ラベル (「工 事 名」等) はタグ無しなので対象外
    const i = HEADER_LABELS.indexOf(p.field);
    if (i >= 0) return { kind: 'header', prim: p, field: i };
  }
  return null;
}

// テキストの画面上の境界ボックス: アンカー画面 px + 回転前ローカル系での矩形。
// 幅は draw() と同じ font 設定の measureText (「描画が真実、箱はその鏡」)
function textScreenBox(
  canvas: HTMLCanvasElement,
  p: TextPrim,
): { ax: number; ay: number; left: number; top: number; w: number; fh: number } | null {
  const v = view;
  const ctx = canvas.getContext('2d');
  if (!v || !ctx) return null;
  const fh = Math.max(p.size * v.scale, 8);
  ctx.font = `${fh}px sans-serif`;
  // 空欄の枠タイトル (field タグ付き・text="") にも文字 3 つ分の当たり/編集ボックスを与える
  const w = Math.max(ctx.measureText(p.text).width, fh * 3);
  return {
    ax: p.x * v.scale + v.offsetX,
    ay: -p.y * v.scale + v.offsetY,
    left: p.alignH === 0 ? 0 : -w / 2,
    top: p.align === 1 ? -fh : p.align === 3 ? 0 : -fh / 2,
    w,
    fh,
  };
}

// dblclick 位置に重なる編集可能テキストを探す (回転テキストはローカル系に逆回転して矩形判定)
// 図面枠 url text の hit 判定。 frame layer + field="url" の prim を text box で当たり判定、
// 当たれば url 文字列を返す (= window.open で別タブ)。 編集はしない (= hitEditableText 対象外)。
function hitUrlPrim(canvas: HTMLCanvasElement, px: number, py: number): string | null {
  for (const p of lastPrims) {
    if (p.type !== 'text' || (p as Prim & {layer?: string}).layer !== 'frame') continue;
    if ((p as Prim & {field?: string}).field !== 'url') continue;
    const box = textScreenBox(canvas, p as TextPrim);
    if (!box) continue;
    const th = (((p as TextPrim).angle ?? 0) * Math.PI) / 180;
    const dx = px - box.ax;
    const dy = py - box.ay;
    const lx = dx * Math.cos(th) - dy * Math.sin(th);
    const ly = dx * Math.sin(th) + dy * Math.cos(th);
    const pad = 6;
    if (lx < box.left - pad || lx > box.left + box.w + pad) continue;
    if (ly < box.top - pad || ly > box.top + box.fh + pad) continue;
    return (p as TextPrim).text ?? null;
  }
  return null;
}

function hitEditableText(canvas: HTMLCanvasElement, px: number, py: number): TextEditTarget | null {
  let best: { t: TextEditTarget; d: number } | null = null;
  for (const p of lastPrims) {
    const t = textEditTarget(p);
    if (!t) continue;
    const box = textScreenBox(canvas, t.prim);
    if (!box) continue;
    // 描画は rotate(-angle) なので、逆変換 = +angle 回転でローカル系へ
    const th = (t.prim.angle * Math.PI) / 180;
    const dx = px - box.ax;
    const dy = py - box.ay;
    const lx = dx * Math.cos(th) - dy * Math.sin(th);
    const ly = dx * Math.sin(th) + dy * Math.cos(th);
    const pad = 6;
    if (lx < box.left - pad || lx > box.left + box.w + pad) continue;
    if (ly < box.top - pad || ly > box.top + box.fh + pad) continue;
    const d = Math.hypot(lx - (box.left + box.w / 2), ly - (box.top + box.fh / 2));
    if (!best || d < best.d) best = { t, d };
  }
  return best?.t ?? null;
}

let inplaceEditor: HTMLInputElement | null = null;

function closeInplaceEditor(): void {
  inplaceEditor?.remove();
  inplaceEditor = null;
}

function openTextEditor(canvas: HTMLCanvasElement, target: TextEditTarget): void {
  closeInplaceEditor();
  const wrap = document.getElementById('canvasWrap');
  const box = textScreenBox(canvas, target.prim);
  if (!wrap || !box) return;

  // 初期値と確定処理 (書き戻し先) を素性で決める
  let initial: string;
  let hint: string;
  let commit: (val: string) => void;
  if (target.kind === 'dim') {
    const row = rows[target.tri - 1];
    if (!row) return;
    const key = (['a', 'b', 'c'] as const)[target.side];
    initial = fmt2(edgeVal(row, key));
    hint = `三角形 ${target.tri} の ${['A', 'B', 'C'][target.side]} 辺`;
    commit = (val) => {
      setEdgeVal(row, key, fmt2(val));
      syncEdgeInputs(row[EKEY[key]]); // 一覧セル・カレント行フォームを追従
      redraw(canvas); // 不成立値は共通関門が図を据え置いて警告
    };
    // 編集対象の辺は図側でも黄色強調 (旧 focusListCell と同じ見せ方)
    selected = target.tri;
    current = target.tri;
    selectedDim = { tri: target.tri, side: target.side };
    edgeSel = null;
    shadowPrims = null;
    updateRowHighlight();
    syncForm();
    draw(canvas, lastPrims);
  } else if (target.kind === 'ded') {
    const li = dedLineIndexByNum(target.num);
    if (li < 0) return;
    const chunks = dedLines[li].split(',').map((s) => s.trim());
    initial = chunks[2] ?? '';
    hint = `控除 ${target.num} の名前`;
    commit = (val) => {
      chunks[2] = val.replace(/,/g, ' '); // CSV 区切りを壊す comma は空白へ
      dedLines[li] = chunks.join(',');
      buildDedTable(canvas);
      redraw(canvas);
    };
  } else {
    const field = target.field;
    initial = headerValueAt(field);
    hint = ['工事名', '路線名', '業者名', '図面番号'][field];
    commit = (val) => {
      setHeaderValueAt(field, val);
      if (field === 1) syncRosenName(); // テーブル側の路線名欄も追従
      redraw(canvas);
    };
  }

  // canvas 内部 px → CSS px (表示縮尺) に換算して、テキストの境界ボックスに重ねる。
  // 回転テキストは CSS transform で同じ角度に倒す (rotate → ローカル系 translate の順)
  const rect = canvas.getBoundingClientRect();
  const k = rect.width / canvas.width;
  const pad = 4;
  const inp = document.createElement('input');
  inp.type = 'text';
  inp.value = initial;
  inp.style.position = 'absolute';
  inp.style.left = `${canvas.offsetLeft + box.ax * k}px`;
  inp.style.top = `${canvas.offsetTop + box.ay * k}px`;
  inp.style.transformOrigin = '0 0';
  inp.style.transform = `rotate(${-target.prim.angle}deg) translate(${(box.left - pad) * k}px, ${(box.top - pad) * k}px)`;
  inp.style.width = `${Math.max((box.w + pad * 2) * k, 48) + 24}px`;
  inp.style.font = `${Math.max(box.fh * k, 11)}px sans-serif`;
  inp.style.padding = '2px 4px';
  inp.style.border = `2px solid ${COLORS.selectYellow}`;
  inp.style.borderRadius = '3px';
  inp.style.background = 'rgba(255, 255, 255, 0.95)';
  inp.style.color = '#222';
  inp.style.zIndex = '10';

  let done = false;
  const finish = (apply: boolean) => {
    if (done) return;
    done = true;
    const val = inp.value;
    closeInplaceEditor();
    if (apply && val !== initial) commit(val);
  };
  inp.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.isComposing) {
      e.preventDefault();
      finish(true);
    } else if (e.key === 'Escape') {
      e.preventDefault();
      finish(false);
    }
  });
  inp.addEventListener('blur', () => finish(true));
  // クリックが下の canvas に流れて選択/パンを起こさないように
  inp.addEventListener('pointerdown', (e) => e.stopPropagation());
  wrap.appendChild(inp);
  inplaceEditor = inp;
  inp.focus();
  inp.select();
  setStatus(`${hint} — Enter で確定、Esc で取消`);
}

function handleTap(canvas: HTMLCanvasElement, px: number, py: number): void {
  if (!view) return;
  const m = toModel(view, px, py);
  // 段階2f: 全タップの位置を覚える (アプリ pressedInModel 相当)。旗 FAB がこれを移動先に使う
  lastTapModel = m;

  // 控除モード: クリック = 控除選択 or カーソル位置決め (三角形選択には流さない —
  // アプリの deductionMode 分岐 MainActivity.kt:1578 と同じモード切り)
  if (deductionMode) {
    handleDedTap(canvas, px, py, m);
    return;
  }

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
  const edge = nearestEdge(px, py);          // 三角形の辺のみ (台形の辺は混ざらない)
  const trapEdge = nearestTrapEdge(px, py);  // 台形の辺のみ
  const tc = triCount();

  // 辺と寸法の両方を、属する図形の種別 (三角形 / 台形) で振り分けて最近接を比べる。
  // 以前の範囲計算 (triCount+idx) は廃止し、行 index から直接種別を引く (B-FORCE 2026-06-16)。
  const rowOfDim = (dim && dim.prim.tri) ? rows[dim.prim.tri - 1] : null;
  const dimTrap = rowOfDim?.kind === 'rectangle';
  const trapDimD = dimTrap ? dim!.d : Infinity;
  const triDimD = (dim && !dimTrap) ? dim.d : Infinity;
  const trapBest = Math.min(trapEdge ? trapEdge.d : Infinity, trapDimD);
  const triBest = Math.min(edge ? edge.d : Infinity, triDimD);

  // 台形が最近接のとき。図形選択 + 辺名フィードバックまでを確定で出す (接続配線は別段)。
  if (trapBest !== Infinity && trapBest <= triBest) {
    let parentIdx: number;
    let side: number;
    if (trapEdge && trapEdge.d <= trapDimD) {
      parentIdx = trapEdge.tri;
      side = trapEdge.side; // nearestTrapEdge は識別子直引き = 物理 side をそのまま返す
    } else {
      parentIdx = dim!.prim.tri as number;
      side = dim!.prim.side as number;          // 0=A底辺 / 1=B左脚 / 2=C上辺 (getLine 側)
    }
    // 台形の自由辺 (1=左脚/2=上辺/3=右脚) をタップ → 親スロット (pendingTrapParent) を立てる。
    // 子の種別 (三角形/台形) は figureKind で分岐 → presetOnTrap が新規行プリセットと
    // シャドーを figureKind に応じて組む。底辺(0)は親の接続辺なので不可、選択+辺名表示のみ。
    if (side >= 1) {
      presetTriOnTrap(canvas, parentIdx, side);
      return;
    }
    selectRectangle(canvas, parentIdx, side);
    return;
  }

  // 寸法値タップ (三角形) は「その辺をタップした」のと同じ扱いに統一 (段階2e の独自発明だった
  // 寸法単独選択は廃止 — アプリは lastTapSide 一本で接続プリセットと W/H 対象を兼ねる)
  if (dim && !dimTrap && (!edge || dim.d <= edge.d)) {
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
    // A 辺タップでも黄色ハイライトを立てる (どこを触ったか視覚的に分かるよう、辺種別問わず統一)。
    // selectTriangle が selectedDim=null にした後で上書きする必要あり (user 指摘 2026-06-14「全経路選択」網羅)。
    selectedDim = { tri: edge.tri, side: 0 };
    draw(canvas, lastPrims);
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
      // 図面枠の url text をクリックしたら別タブで開く (2026-06-18 user 「キャンバス上でクリックすると別タブで開く」)。
      // scheme 検証 (XSS 防御) は url-safety.ts の safeOpenUrl に切り出し済、 単体 test 可能。
      const url = hitUrlPrim(canvas, downAt!.x, downAt!.y);
      if (url) {
        const safe = safeOpenUrl(url);
        if (safe) window.open(safe, '_blank', 'noopener,noreferrer');
        downAt = null;
        return;
      }
      handleTap(canvas, downAt!.x, downAt!.y);
    }
    downAt = null;
  };
  canvas.addEventListener('pointerup', release);
  canvas.addEventListener('pointercancel', release);

  // ダブルクリック: テキストの上なら境界ボックス付き in-place 編集 (寸法/控除名/図面枠)、
  // それ以外は従来どおり全体 fit に戻す
  canvas.addEventListener('dblclick', (e) => {
    const p = canvasPx(canvas, e);
    const hit = hitEditableText(canvas, p.x, p.y);
    if (hit) {
      openTextEditor(canvas, hit);
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
  // 番号逆順チェック (アプリ保存ダイアログの NumReverse ボタン、MainActivity.kt:2293 の web 版)。
  // DXF/SFC のみ反映 — CSV はファイル仕様不変、XLSX もアプリの XlsxWriter().write
  // (MainActivity.kt:2694) が isReverse を受けないので対象外 (現物が正)
  const numReverse = () =>
    (document.getElementById('numReverse') as HTMLInputElement | null)?.checked ?? false;
  // 段階2e: overrides 付き経路に切替 — W/H フリップ・番号移動が図面ファイルにも乗る
  document.getElementById('saveDxf')?.addEventListener('click', () => {
    exportFile('DXF', 'triangles.dxf', () =>
      toSjisBlob(buildDxfTextNumReverse(serializeState(), overridesJson(), numReverse())),
    );
  });
  document.getElementById('saveSfc')?.addEventListener('click', () => {
    exportFile('SFC', 'triangles.sfc', () =>
      toSjisBlob(buildSfcTextNumReverse(serializeState(), 'triangles.sfc', overridesJson(), numReverse())),
    );
  });
  // XLSX: アプリの XlsxWriter と同じ面積計算書 (組み立ては xlsx-export.ts に独立、
  // ExcelJS は保存時だけ dynamic import してバンドルを太らせない)
  document.getElementById('saveXlsx')?.addEventListener('click', () => {
    void (async () => {
      try {
        const { buildXlsxBlob } = await import('./xlsx-export');
        downloadBlob('triangles.xlsx', await buildXlsxBlob(serializeState()));
        setStatus('XLSX saved: triangles.xlsx');
      } catch (e) {
        setStatus(`XLSX error: ${String(e)}`);
        console.error(e);
      }
    })();
  });
}

function loadCsv(canvas: HTMLCanvasElement, csv: string, label: string): void {
  parseCsvToState(csv);
  view = null; // 新しいデータは全体 fit から
  clearSelection();
  current = rows.length > 0 ? rows.length : 0; // カレントは末尾 (アプリの初期 retrieveCurrent 相当)
  buildTable(canvas);
  buildDedTable(canvas); // 控除一覧 (dedLines は parseCsvToState が更新済み)
  syncForm();
  syncRosenName();
  renderCsv(canvas, serializeState(), label);
  // 読込動線も共通関門に通す: 読込自体は止めない (既存ファイルは開けるべき) が、
  // 不成立行があれば編集前に気づけるよう警告だけ重ねる
  const bad = findInvalidRow(rows);
  if (bad) setStatus(`⚠ 読込: ${bad.reason}`);
}

function main(): void {
  const canvas = document.getElementById('cv') as HTMLCanvasElement | null;
  const fileInput = document.getElementById('file') as HTMLInputElement | null;
  const addBtn = document.getElementById('addRow');
  if (!canvas || !fileInput) return;

  // autosave があればそこから復元、無ければ新規作成と同じ 1 個スタート
  // (アプリ resumeCSV → file 無しなら createNew = 三角形 1 個、MainActivity.kt:2926-2940。
  //  内蔵サンプル 7 個を出すのは段階1 の描画パイプ確認の名残でアプリと違った)。
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
  if (savedCsv !== null) {
    loadCsv(canvas, savedCsv, 'autosave');
  } else {
    newDrawing(canvas); // rows が空なので confirm は出ない
  }

  addBtn?.addEventListener('click', () => addRow(canvas));
  document.getElementById('newDrawing')?.addEventListener('click', () => newDrawing(canvas));
  // 図面枠トグル: 枠の出入りで見える範囲が変わるので fit し直す
  document.getElementById('frameToggle')?.addEventListener('change', () => {
    view = null;
    redraw(canvas);
  });
  // 外枠余白セレクタ: user が UI で margin を切替 (= 2026-06-18 user 要望)。 frameToggle の
  // 親要素に <select> を動的注入、 選択値を localStorage に保存して round-trip。
  const ft = document.getElementById('frameToggle');
  if (ft && ft.parentElement && !document.getElementById('marginSelect')) {
    const wrap = document.createElement('span');
    wrap.style.marginLeft = '8px';
    wrap.style.fontSize = '12px';
    wrap.textContent = '余白:';
    const sel = document.createElement('select');
    sel.id = 'marginSelect';
    sel.style.marginLeft = '4px';
    const options: Array<[string, string]> = [
      ['0.75', '7.5mm (A3)'],
      ['1.0',  '10mm (A2)'],
      ['1.5',  '15mm'],
      ['2.0',  '20mm (A1)'],
    ];
    for (const [v, label] of options) {
      const o = document.createElement('option');
      o.value = v;
      o.textContent = label;
      sel.appendChild(o);
    }
    const stored = localStorage.getItem('frameMarginCm');
    sel.value = stored && options.some(([v]) => v === stored) ? stored : '1.5';
    sel.addEventListener('change', () => {
      localStorage.setItem('frameMarginCm', sel.value);
      view = null;
      redraw(canvas);
    });
    wrap.appendChild(sel);
    ft.parentElement.appendChild(wrap);
  }
  wireExportButtons();
  wireCanvasEvents(canvas);
  wireFabs(canvas);
  wireRosenName(canvas);
  wireNewRowEnter(canvas);
  // 形態 select の変化で追従: 三角形は起点 select の有効/無効、台形は親種別なので接続辺の D 有無 (onCTypeChange)。
  // 加えて、新規行フォームの各値変更を即座にシャドーへ反映 (user 2026-06-14「起点プルダウン選択を
  // トリガーにキャンバスが書き換わるのが本来の姿」)。buildShadow が newA/newB/newC/newLcr を読むので、
  // それらの change → buildShadow + draw が「即時反映」の入口になる (curXxx の rewriteCurrent と同思想)。
  select('newCType').addEventListener('change', () => {
    onCTypeChange('new');
    buildShadow();
    draw(canvas, lastPrims);
  });
  select('curCType').addEventListener('change', () => onCTypeChange('cur'));
  syncLcrDisabled('new');
  for (const id of ['newA', 'newB', 'newC', 'newParent', 'newConn', 'newLcr']) {
    document.getElementById(id)?.addEventListener('change', () => {
      buildShadow();
      draw(canvas, lastPrims);
    });
  }
  // 「現在」行フォームの編集を即時確定する (user 2026-06-12: 一覧フォームで二重断面や
  // 起点・A 辺を変えてもキャンバスに反映されない)。原因は formCur の接続 select と辺入力に
  // change handler が無く、確定が FAB / 辺C-Enter の 2 動線だけだったこと。一方すぐ下の
  // 一覧表はインライン編集が即時反映 (buildTable の apply / セルの change) なので、同じ表内で
  // 「現在」行だけ FAB 必須では UX が食い違う。変更の瞬間に rewriteCurrent (= FAB と同じ確定
  // 経路: 接続の張り直し relinkEdgeA + cp 列 writeCpExtras + 三角不等式関門 + redraw) を走らせて
  // 一覧表と同じ即時反映に揃える。辺C は Enter 確定 (1976) と二重になるが rewriteCurrent は冪等。
  for (const id of ['curName', 'curA', 'curB', 'curC', 'curParent', 'curConn', 'curCType', 'curLcr']) {
    document.getElementById(id)?.addEventListener('change', () => rewriteCurrent(canvas));
  }

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
  // ページ全体スクショ (FAB 等の DOM 込み)。snapDOM が DOM を SVG 経由でラスタライズする —
  // capture (canvas.toDataURL) と違い UI 全体の見た目検証ができる。dev 専用 dynamic import
  // なので prod バンドルには乗らない (出典: https://snapdom.dev/ — html2canvas 系の後継定石)
  hot.on('tlcp:page-req', (data: { id: string }) => {
    void (async () => {
      try {
        const { snapdom } = await import('@zumer/snapdom');
        const result = await snapdom(document.body, { scale: 1 });
        const img = await result.toPng();
        hot.send('tlcp:page-res', { id: data.id, png: img.src });
      } catch (e) {
        console.error('tlcp:page failed', e);
        hot.send('tlcp:page-res', { id: data.id, png: '' });
      }
    })();
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
      kind: r.kind, // 混在リスト: triangle / rectangle (CP 消費側が種別で出し分けられるように)
      a: edgeVal(r, 'a'),
      b: edgeVal(r, 'b'),
      c: edgeVal(r, 'c'),
      ea: r.ea,
      eb: r.eb,
      ec: r.ec,
      parent: r.parent,
      conn: r.conn,
      extras: r.extras,
      // 台形のみ: 上辺寄せ(align) と親種別(parentKind)。台形チェーン (parentKind=1) の検証口
      align: r.align,
      parentKind: r.parentKind,
    }));
    hot.send('tlcp:state-res', {
      id: data.id,
      state: {
        rows: rowsExpanded, edges, selected, current, listAngle, textSizeCsv, colorIndex, overrides,
        dedLines, deductionMode, dedCursor, dedSelected,
        csv: serializeState(),
        // 保存ボタンが書く実物 (overrides 焼き込み済み完全形式)。書き戻しの検証口
        csvBaked: buildCsvTextWithOverrides(serializeState(), overridesJson()),
        view, prims: lastPrims, fabs,
        // 4方向接続の検証口: 親スロット (edgeSel=三角形親, pendingTrapParent=台形親) と
        // figureKind (子の種別)、shadowPrims (シャドーの辺数=3=三角形 / 4=台形)
        figureKind,
        edgeSel,
        pendingTrapParent,
        selectedDim,
        // 辺タップ動線の検証口 (新規行プリセット): 親辺長 A / 接続 / 形態 / 起点
        newRow: {
          a: input('newA').value, b: input('newB').value, c: input('newC').value,
          parent: input('newParent').value,
          conn: select('newConn').value, ctype: select('newCType').value, lcr: select('newLcr').value,
        },
        shadowLen: shadowPrims ? shadowPrims.length : 0,
        // 図形ごとの幾何ビュー: 画面を見ずに頂点座標・辺長・接続状態をデバッグできる構造化口。
        // rows[i] が tri=(i+1) の prims と対応する (1-based tri 番号)。
        figures: rows.map((r, i) => {
          const n = i + 1;
          type Prim = { type: string; layer: string; tri: number; side?: number; x1: number; y1: number; x2: number; y2: number };
          const lines = (lastPrims as Prim[]).filter(p => p.type === 'line' && p.layer === 'tri' && p.tri === n)
            .sort((a, b) => (a.side ?? 0) - (b.side ?? 0));
          const verts = lines.map(l => ({ x: +l.x1.toFixed(6), y: +l.y1.toFixed(6) }));
          const edgeLengths = lines.map(l => {
            const dx = l.x2 - l.x1, dy = l.y2 - l.y1;
            return +(Math.sqrt(dx * dx + dy * dy).toFixed(6));
          });
          return { n, kind: r.kind, parent: r.parent, conn: r.conn, connected: r.parent !== '-1', verts, edgeLengths };
        }),
      },
    });
  });
  // キー注入: 要素に値をセットして keydown を流す (Enter ナビ等のキーボード UX の検証口)。
  // 合成イベントはブラウザ既定動作を起こさないが、こちらの handler は preventDefault 前提なので等価
  hot.on('tlcp:key-req', (data: { id: string; target?: string; key?: string; value?: string }) => {
    // target は要素 id か CSS selector (一覧の辺セルは id を持たず data-tri/data-key で指す)
    const sel = String(data.target ?? '');
    let t: Element | null = document.getElementById(sel);
    if (!t) {
      try {
        t = document.querySelector(sel);
      } catch {
        t = null;
      }
    }
    if (t instanceof HTMLInputElement || t instanceof HTMLSelectElement) {
      if (typeof data.value === 'string') {
        t.value = data.value;
        // プログラム的な value 代入は input/change を発火しない。実ブラウザの blur 確定や
        // select 選択と同じく編集系 change handler (formCur の即時確定等) を検証できるよう
        // 明示発火する。これがないと「現在」行フォームの change 駆動の確定を CLI から踏めない
        t.dispatchEvent(new Event('input', { bubbles: true }));
        t.dispatchEvent(new Event('change', { bubbles: true }));
      }
      t.focus();
      if (data.key) t.dispatchEvent(new KeyboardEvent('keydown', { key: data.key, bubbles: true, cancelable: true }));
      const a = document.activeElement;
      hot.send('tlcp:key-res', {
        id: data.id,
        state: {
          ok: true,
          active: a?.id ?? '',
          // Enter ナビの検証用: フォーカス先が辺セルなら data-tri/data-key で答える
          activeData: a instanceof HTMLElement ? { tri: a.dataset.tri ?? '', key: a.dataset.key ?? '' } : null,
          rows: rows.length,
        },
      });
    } else {
      hot.send('tlcp:key-res', { id: data.id, state: { ok: false } });
    }
  });
  // option 列挙: <select id="..."> の option value/text を返す (kind-toggle test 等)
  hot.on('tlcp:options-req', (data: { id: string; target?: string }) => {
    const t = document.getElementById(String(data.target ?? ''));
    if (t instanceof HTMLSelectElement) {
      const opts = Array.from(t.options).map((o) => ({ value: o.value, text: o.textContent ?? '' }));
      hot.send('tlcp:options-res', {
        id: data.id,
        state: { ok: true, value: t.value, options: opts },
      });
    } else {
      hot.send('tlcp:options-res', { id: data.id, state: { ok: false, options: [] } });
    }
  });
  // クリック注入: ボタン UX (FAB・削除等) の動線を CLI から踏む口
  hot.on('tlcp:click-req', (data: { id: string; target?: string }) => {
    const t = document.getElementById(String(data.target ?? ''));
    if (t instanceof HTMLElement) {
      t.click();
      // redraw 等の非同期処理が 1 フレーム程度かかる場合があるため、
      // 僅かに待ってから最新の状態を返す
      setTimeout(() => {
        hot.send('tlcp:click-res', {
          id: data.id,
          state: { 
            ok: true, 
            rows: rows.length, 
            status: document.getElementById('status')?.textContent ?? '' 
          },
        });
      }, 50);
    } else {
      hot.send('tlcp:click-res', { id: data.id, state: { ok: false } });
    }
  });
  // CSV 注入: ファイルダイアログを経ずに CSV を開く (loadCsv と同じ動線)。
  hot.on('tlcp:load-req', (data: { id: string; csv?: string }) => {
    const canvas = document.getElementById('cv') as HTMLCanvasElement | null;
    if (canvas && typeof data.csv === 'string') {
      overrides = { dims: [], numbers: [] };
      clearSelection();
      pendingTrapParent = null;
      edgeSel = null;
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
  // 選択注入: hitTriangle が拾わない図形 (台形等) を直接 selected に。selected 塗りの
  // 検証 (台形 fill が砂時計にならない等) で必要 ── 一覧表 click と同じ動線 (selectTriangle)
  hot.on('tlcp:select-req', (data: { id: string; n?: number }) => {
    const canvas = document.getElementById('cv') as HTMLCanvasElement | null;
    if (canvas && typeof data.n === 'number' && data.n >= 0 && data.n <= rows.length) {
      selectTriangle(canvas, data.n);
      hot.send('tlcp:select-res', { id: data.id, state: { ok: true, selected, current } });
    } else {
      hot.send('tlcp:select-res', { id: data.id, state: { ok: false } });
    }
  });
}
