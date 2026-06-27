import { describe, it, expect, beforeAll } from 'vitest';
import { health, state, tap, key, click, loadCsv } from './combo/cpClient.ts';
import { genMixedChain4 } from './combo/genChain.ts';
import { groupByTri, analyzeTri, detectTriOverlaps } from './combo/primAnalyzer.ts';

beforeAll(async () => {
  const ok = await health();
  if (!ok) {
    throw new Error(`dev server not reached`);
  }
}, 10_000);

/**
 * 指定された行数と、特定の図形 (triIdx) の全辺 (tri=3辺, trap=4辺) が揃うまで待機する
 */
async function waitForState(expectedRows: number, triIdx?: number): Promise<any> {
  const start = Date.now();
  while (Date.now() - start < 10000) {
    const s = await state();
    if (s.rows.length !== expectedRows) {
      await new Promise(r => setTimeout(r, 200));
      continue;
    }
    if (triIdx !== undefined) {
      const triPrims = s.prims?.filter((p: any) => p.type === 'line' && p.layer === 'tri' && p.tri === triIdx) ?? [];
      const kind = s.rows[triIdx - 1]?.kind;
      const expectedSides = kind === 'rectangle' ? 4 : 3;
      if (triPrims.length < expectedSides) {
        await new Promise(r => setTimeout(r, 200));
        continue;
      }
    }
    return s;
  }
  return state();
}

async function buildChainInteractively(c: any): Promise<void> {
  // console.log(`\n=== Build Start: ${c.label} ===`);
  
  // 1. 初期化 (Num=1 の三角形)
  await loadCsv('1,3.00,3.00,3.00,-1,-1'); 
  let s = await waitForState(1, 1);

  const kinds = c.label.split('/')[1].split('-');
  const sides = c.label.split('-s')[1].split('-').map(Number);

  for (let i = 0; i < 3; i++) {
    const parentIdx = i + 1; 
    const childKind = kinds[i+1];
    const parentSide = sides[i];

    // 親の座標を確定
    s = await waitForState(i + 1, parentIdx);
    const triLines = s.prims!.filter((p: any) => p.type === 'line' && p.layer === 'tri' && p.tri === parentIdx);
    const targetLine = triLines.find((l: any) => l.side === parentSide);
    
    if (!targetLine) {
      throw new Error(`Parent #${parentIdx} Side ${parentSide} lost. Prims: ${JSON.stringify(triLines.map((l: any) => l.side))}`);
    }

    // タップ
    const tx = (targetLine.x1 + targetLine.x2) / 2;
    const ty = (targetLine.y1 + targetLine.y2) / 2;
    await tap(tx, ty);
    
    // プリセット反映待ち (newParent が親を指すまで)
    let presetOk = false;
    for (let r = 0; r < 20; r++) {
      const cur = await state();
      if (cur.newRow.parent === String(parentIdx)) {
        presetOk = true;
        break;
      }
      await new Promise(res => setTimeout(res, 50));
    }
    if (!presetOk) {
      const cur = await state();
      throw new Error(`UI Preset failed. Parent expected ${parentIdx}, got "${cur.newRow.parent}". Status: ${cur.status}`);
    }

    // 図形種別切替
    const curState = await state();
    const targetKind = childKind === 'trap' ? 'rectangle' : 'triangle';
    if (curState.figureKind !== targetKind) {
      await click('fabFigureKind');
      await new Promise(r => setTimeout(r, 100));
    }

    // 値入力 & 確定
    await key('newB', '2.00');
    await key('newC', '2.00');
    await click('fabReplace');
    
    // 行数増加を待つ
    s = await waitForState(i + 2);
  }
  
  expect(s.rows.length).toBe(4);
  
  const byTri = groupByTri(s.prims ?? []);
  const overlaps = detectTriOverlaps(byTri);
  expect(overlaps.length, `Overlap detected in built chain ${c.label}`).toBe(0);

  // 親番号の整合性検証
  for (let i = 1; i < 4; i++) {
    const row = s.rows[i];
    expect(row.parent, `Parent mismatch at row ${i+1} in ${c.label}`).toBe(String(i));
  }
}

describe('chain-interactive: 4 段混成チェーンの UI 操作ビルド検証 (全 100 ケース網羅)', () => {
  const cases = [...genMixedChain4()];
  
  for (const c of cases) {
    it(`UI Build: ${c.label}`, async () => {
      await buildChainInteractively(c);
    });
  }
});
