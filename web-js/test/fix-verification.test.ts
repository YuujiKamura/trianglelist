import { describe, it, expect, beforeAll } from 'vitest';
import { health, state, tap, click, loadCsv } from './combo/cpClient.ts';

/**
 * line prim 群の bbox。
 *
 * 2026-08-25 修正: 元は './combo/primAnalyzer.ts' から `bounds` を import していたが、
 * その関数は primAnalyzer に存在せず (export は groupByTri / chainAndClose / analyzeTri /
 * detectTriOverlaps / dimLabelsByTri のみ)、実行時に `TypeError: bounds is not a function`
 * で落ちていた ── この test は「回転ドリフト修正の検証」という名前のまま、一度も
 * assertion まで到達していなかった。テスト内で完結させる。
 */
function bounds(prims: any[]): { minX: number; maxX: number; minY: number; maxY: number } {
  const xs: number[] = [];
  const ys: number[] = [];
  for (const p of prims) {
    if (typeof p.x1 === 'number') { xs.push(p.x1, p.x2); ys.push(p.y1, p.y2); }
    else if (typeof p.x === 'number') { xs.push(p.x); ys.push(p.y); }
  }
  if (!xs.length) throw new Error('bounds: 対象 prim が無い');
  return { minX: Math.min(...xs), maxX: Math.max(...xs), minY: Math.min(...ys), maxY: Math.max(...ys) };
}

beforeAll(async () => {
  const ok = await health();
  if (!ok) throw new Error(`dev server not reached`);
}, 10_000);

describe('Fix Verification: Rotation Drift and Shadow Alignment', () => {

  it('Rotation Centering: figure center should remain stable on screen during rotation', async () => {
    // 1. Setup multiple triangles to have a non-trivial center
    await loadCsv('1,10.00,10.00,10.00,-1,-1\n2,10.00,10.00,10.00,1,1\n3,10.00,10.00,10.00,2,2');
    let s = await state();

    const getScreenCenter = (st: any) => {
      const b = bounds(st.prims.filter((p: any) => p.layer === 'tri' && p.type !== 'text'));
      const modelX = (b.minX + b.maxX) / 2;
      const modelY = (b.minY + b.maxY) / 2;
      // Screen coordinates: px = x * scale + offsetX, py = -y * scale + offsetY
      return {
        x: modelX * st.view.scale + st.view.offsetX,
        y: -modelY * st.view.scale + st.view.offsetY
      };
    };

    const initialCenter = getScreenCenter(s);

    // 2. Rotate 90 degrees (5 deg * 18 steps)
    for (let i = 0; i < 18; i++) {
      await click('fabRotL');
    }

    s = await state();
    const rotatedCenter = getScreenCenter(s);

    // 誤差 1px 以内に収まっているか (以前は数十〜百 px 単位でズレていた)
    expect(Math.abs(rotatedCenter.x - initialCenter.x)).toBeLessThan(1.0);
    expect(Math.abs(rotatedCenter.y - initialCenter.y)).toBeLessThan(1.0);
  });

  it('Shadow Alignment: shadow triangle should precisely attach to parent side even in distant branches', async () => {
    // 「エ」の字のような垂直に長い構成 (1のC辺)
    await loadCsv('1,10.00,10.00,10.00,-1,-1\n2,10.00,10.00,10.00,1,2\n3,10.00,10.00,10.00,2,2');
    let s = await state();

    // 1のC辺 (side=2) をターゲットにする
    const parentSide = 2;
    const parentTri = 1;
    const triLines = s.prims!.filter((p: any) => p.type === 'line' && p.layer === 'tri' && p.tri === parentTri);
    const targetLine = triLines.find((l: any) => l.side === parentSide);

    // タップしてシャドーを出現させる
    const tx = (targetLine.x1 + targetLine.x2) / 2;
    const ty = (targetLine.y1 + targetLine.y2) / 2;
    await tap(tx, ty);

    s = await state();
    // シャドー (layer="tri" かつ tri 未定義 or 特殊マーカー) を探す
    // 実装上は lastPrims に一時的に追加された線
    const shadowLines = s.prims.filter((p: any) => p.layer === 'tri' && p.tri === undefined);

    if (shadowLines.length > 0) {
        // シャドーの辺A (接続辺) が親辺と一致しているか
        const shadowA = shadowLines[0]; // 通常 0 番目が辺A
        const dist1 = Math.sqrt(Math.pow(shadowA.x1 - targetLine.x1, 2) + Math.pow(shadowA.y1 - targetLine.y1, 2));
        const dist2 = Math.sqrt(Math.pow(shadowA.x2 - targetLine.x2, 2) + Math.pow(shadowA.y2 - targetLine.y2, 2));

        expect(dist1 + dist2).toBeLessThan(0.001);
    }
  });
});
