// キャップハイト ⇄ font-size (em) の変換。
//
// prim の size は「キャップハイト」(= 大文字の高さ)。DXF の TEXT group code 40 が仕様上
// キャップハイトで、モデル層はその量をそのまま持ち回している。一方 canvas / CSS の
// font-size は「em」で、これは別の量 ── sans-serif なら cap ≈ 0.74em なので、size を
// font-size にそのまま渡すと文字は 3 割小さく描かれる。
//
// 2026-08-25 user 指摘「補正とか掛けてる時点で抽象化が足りてない」への対応。大事なのは
// 「補正係数を正しく書く」ことではなく **2 つの量を混ぜないこと**:
//   capPx  … 描かれる大文字の物理高さ。当たり判定の箱・行間・baseline はこちらで組む
//   fontPx … canvas に渡す em。ctx.font 以外では使わない
// 以前は両方 `fh` という 1 変数に潰していたため、片方に合わせるともう片方が狂う構造だった。
//
// 比率は定数を置かず 'A' のインク高を実測する。フォントに固有の性質であって「補正値」では
// ないので、ハードコードした瞬間にフォントを変えた時に嘘になる (desktop の TextRenderer /
// PlatformTextMetrics も Skia / AWT で同じ実測をしている、ezdxf の
// make_font(font_name, cap_height) / QCAD RTextRenderer と同型)。
//
// main.ts から切り出してあるのは、renderer 層の契約 (「cap height H を頼んだら大文字が
// H の高さで描かれる」) を test から直接叩けるようにするため ── emit 層の突き合わせ
// (TextSizeBackendParityTest) はここを一切見ないので、分けておかないと gate が作れない。

const CAP_PROBE_PX = 100;
export const FONT_FAMILY = 'sans-serif';

let capRatioCache: number | null = null;

/** test 用: 実測キャッシュを捨てる (フォント読み込み前後で測り直したい時)。 */
export function resetCapHeightRatioCache(): void {
  capRatioCache = null;
}

/**
 * キャップハイト / em を 'A' のインク実高から実測する。
 *
 * actualBoundingBoxAscent は「現在の textBaseline からの距離」なので、baseline を固定せずに
 * 測ると直前の描画が残した値 (middle / top) に依存して答えが変わる。しかも結果はキャッシュ
 * されるので「いつ最初に測ったか」で図面全体の文字サイズが変わる ── 実装中に実際に踏んだ
 * (alphabetic で 0.74、middle 残りで 0.86)。フォントの性質を測るのだから、測定条件は
 * 呼び出し文脈から独立していなければならない。
 */
export function capHeightRatio(ctx: CanvasRenderingContext2D): number {
  if (capRatioCache !== null) return capRatioCache;
  const savedFont = ctx.font;
  const savedBaseline = ctx.textBaseline;
  ctx.textBaseline = 'alphabetic';
  ctx.font = `${CAP_PROBE_PX}px ${FONT_FAMILY}`;
  // 'A' はベースラインに乗る字なので ascent = キャップハイト
  const ink = ctx.measureText('A').actualBoundingBoxAscent;
  // 実測できない環境 (古い WebView 等で actualBoundingBox* が無い) は素通し = 従来挙動
  capRatioCache = Number.isFinite(ink) && ink > 0 ? ink / CAP_PROBE_PX : 1;
  ctx.font = savedFont;
  ctx.textBaseline = savedBaseline;
  return capRatioCache;
}

/** キャップハイト (px) → canvas に渡す font-size (px, em)。 */
export function fontPxForCapPx(ctx: CanvasRenderingContext2D, capPx: number): number {
  return capPx / capHeightRatio(ctx);
}

/** ctx.font を「大文字がこの高さで描かれる」ように設定する。 */
export function setFontForCapPx(ctx: CanvasRenderingContext2D, capPx: number, bold = false): void {
  ctx.font = `${bold ? 'bold ' : ''}${fontPxForCapPx(ctx, capPx)}px ${FONT_FAMILY}`;
}
