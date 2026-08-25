import { describe, it, expect, beforeAll, afterAll } from 'vitest';
import { chromium, type Browser, type Page } from 'playwright';

/**
 * renderer 層の gate: 「キャップハイト H を頼んだら、大文字が実際に H の高さで描かれる」。
 *
 * 2026-08-25 user 指摘「補正とか掛けてる時点で抽象化が足りてない」の物理層。
 *
 * emit 層 (common の TextSizeBackendParityTest) は「DXF と web で同じ文字が同じ実寸で出る」
 * を見るが、そこから先 ── prim size を canvas の font-size にどう渡すか ── は一切見ない。
 * 実際、web は prim size (キャップハイト) を font-size (em) にそのまま渡していて、全テキストが
 * 0.74 倍で描かれていた。emit 層の test は両方 green のまま素通りする穴だった。
 *
 * ここは実ブラウザ (playwright + dev server の module graph) で src/text-metrics.ts を直接
 * 叩き、描かれたインクを measureText で測り返して契約を確認する。ピクセル走査ではなく
 * 「頼んだ量と描かれた量」を突き合わせる形なので、フォントが変わっても壊れない。
 *
 * 前提: dev server (npm run dev) が localhost:5173 で動いていること (他の統合 test と同じ)。
 */
const BASE = 'http://localhost:5173';

describe('renderer: キャップハイトの契約', () => {
  let browser: Browser;
  let page: Page;

  beforeAll(async () => {
    browser = await chromium.launch({ headless: true });
    page = await browser.newPage();
    await page.goto(BASE, { waitUntil: 'domcontentloaded' });
    // vite dev はソースを ES module として配るので production コードをそのまま import できるが、
    // page.evaluate に渡す関数の中に import() を直接書くと vitest の SSR 変換が
    // __vite_ssr_dynamic_import__ に書き換えてしまい、ブラウザ側で ReferenceError になる。
    // 文字列越しに Function で組み立てて変換を回避し、page 側の global に置く。
    await page.addScriptTag({
      content:
        "window.loadTextMetrics = () => (new Function('u', 'return import(u)'))('/src/text-metrics.ts');",
    });
  }, 60_000);

  afterAll(async () => {
    await browser?.close();
  });

  /** 指定キャップハイトで 'A' を描かせ、実際のインク高を測って返す。 */
  async function measuredCapHeights(requested: number[]): Promise<number[]> {
    return page.evaluate(async (caps: number[]) => {
      const m = await loadTextMetrics();
      const cv = document.createElement('canvas');
      const ctx = cv.getContext('2d')!;
      return caps.map((capPx) => {
        // わざと汚しておく: baseline が呼び出し文脈に依存しないことも同時に確認する
        ctx.textBaseline = 'middle';
        m.setFontForCapPx(ctx, capPx);
        ctx.textBaseline = 'alphabetic';
        return ctx.measureText('A').actualBoundingBoxAscent;
      });
    }, requested);
  }

  it('頼んだキャップハイトで大文字が描かれる', async () => {
    const requested = [8, 12, 20, 40, 100];
    const measured = await measuredCapHeights(requested);
    for (let i = 0; i < requested.length; i++) {
      // actualBoundingBoxAscent はラスタライズ後のインク範囲なので 1px 単位に量子化される
      // (headless chromium 実測: 8/12/20/40 は完全一致、100 だけ 99)。許容はその量子化幅。
      // 素通し (= 修正前の挙動) なら 0.74 倍 = 26% ずれるので、この幅でも確実に落ちる。
      const tolerance = Math.max(1, requested[i] * 0.02);
      expect(Math.abs(measured[i] - requested[i])).toBeLessThanOrEqual(tolerance);
    }
  });

  it('font-size (em) はキャップハイトより大きい', async () => {
    // cap < em はどのフォントでも成立する。 等しくなっていたら変換が抜けている
    const { capPx, fontPx, ratio } = await page.evaluate(async () => {
      const m = await loadTextMetrics();
      const ctx = document.createElement('canvas').getContext('2d')!;
      const capPx = 100;
      return { capPx, fontPx: m.fontPxForCapPx(ctx, capPx), ratio: m.capHeightRatio(ctx) };
    });
    expect(fontPx).toBeGreaterThan(capPx);
    expect(ratio).toBeGreaterThan(0.6);
    expect(ratio).toBeLessThan(0.95);
  });

  it('キャップハイト比の実測は直前の textBaseline に依存しない', async () => {
    // actualBoundingBoxAscent は現在の textBaseline からの距離なので、測定側で baseline を
    // 固定していないと呼び出し文脈で答えが変わる (実装中に 0.74 / 0.86 に割れた)。
    // しかも結果はキャッシュされるので「いつ最初に測ったか」で図面全体の文字サイズが変わる。
    const ratios = await page.evaluate(async () => {
      const m = await loadTextMetrics();
      const ctx = document.createElement('canvas').getContext('2d')!;
      const out: number[] = [];
      for (const b of ['alphabetic', 'top', 'middle', 'bottom', 'hanging'] as const) {
        m.resetCapHeightRatioCache();
        ctx.textBaseline = b;
        out.push(m.capHeightRatio(ctx));
      }
      return out;
    });
    for (const r of ratios) expect(Math.abs(r - ratios[0])).toBeLessThan(1e-9);
  });

  it('実測は元の font と textBaseline を壊さない', async () => {
    // 計測は副作用であってはならない (描画ループの途中から呼ばれる)
    const { fontBefore, fontAfter, baselineBefore, baselineAfter } = await page.evaluate(async () => {
      const m = await loadTextMetrics();
      const ctx = document.createElement('canvas').getContext('2d')!;
      ctx.font = '17px sans-serif';
      ctx.textBaseline = 'top';
      const fontBefore = ctx.font;
      const baselineBefore = ctx.textBaseline;
      m.resetCapHeightRatioCache();
      m.capHeightRatio(ctx);
      return { fontBefore, fontAfter: ctx.font, baselineBefore, baselineAfter: ctx.textBaseline };
    });
    expect(fontAfter).toBe(fontBefore);
    expect(baselineAfter).toBe(baselineBefore);
  });
});

// page 側 global (beforeAll の addScriptTag で注入) の型宣言
declare function loadTextMetrics(): Promise<typeof import('../src/text-metrics')>;
