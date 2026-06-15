// 連続接続テスト (user 確定 2026-06-15「最低でも10個のエンティティの連続、 すべて接続図形や
// 形態を網羅したテストでなければ意味がない」 + 「実運用では100とか200くらいまでは普通にあり得る」)。
//
// 構成:
//   - chain-shared: 全段辺共有、 depth ∈ {10, 100, 200}、 side ∈ {1, 2} = 6 chain
//     ── 実運用 200 段で描画破綻なしを pin
//   - chain10-focus: 14 種 × 注目位置 {2..10} = 126 chain、 1 段だけ注目 pattern を埋め込み
//     ── user 「最低 10 個」 ＆ 「14 種網羅」 を pin
//   - chain100-focus: 14 種 × 注目位置 50 = 14 chain、 実運用 100 段で 14 種が破綻しないことを pin
// 合計 ~146 chain。 各 chain で 各 tri の sideCount=3 + 砂時計なしを assert。

import { describe, it, expect, beforeAll } from 'vitest';
import { health, loadCsv, state } from './combo/cpClient.ts';
import {
  genTriangleChainSharedDeep,
  genTriangleChainFocus10,
  genTriangleChainFocus100,
  type ChainCase,
} from './combo/genChain.ts';
import { groupByTri, analyzeTri } from './combo/primAnalyzer.ts';

beforeAll(async () => {
  const ok = await health();
  if (!ok) {
    throw new Error(
      `dev サーバ (http://localhost:5173) に届かない。 別 terminal で "cd web && npm run dev" を立てておくこと`,
    );
  }
}, 10_000);

async function runChain(c: ChainCase): Promise<void> {
  const loaded = await loadCsv(c.csv);
  expect(loaded.ok, `load ${c.label}`).toBe(true);
  expect(loaded.rows, `rows count ${c.label}`).toBe(c.expectedRows);

  const s = await state();
  expect(s.rows.length, `state rows ${c.label}`).toBe(c.expectedRows);

  const byTri = groupByTri(s.prims ?? []);
  for (const { tri, sides } of c.expectedSideCounts) {
    const lines = byTri.get(tri) ?? [];
    expect(lines.length, `tri=${tri} sideCount ${c.label}`).toBe(sides);
    const a = analyzeTri(lines);
    expect(a.hourglass, `tri=${tri} 砂時計検出 ${c.label}`).toBe(false);
  }
}

describe('chain-shared: 全段辺共有 deep chain (depth=10/100/200)', () => {
  const cases = [...genTriangleChainSharedDeep()];
  it('chain 数 = 6 (depth 3 × side 2)', () => {
    expect(cases.length).toBe(6);
  });
  for (const c of cases) {
    it(c.label, async () => {
      await runChain(c);
    }, 30_000);
  }
});

describe('chain10-focus: 14 種 × 注目位置 (user 最低 10 個 + 14 種網羅)', () => {
  const cases = [...genTriangleChainFocus10()];
  it('chain 数 = 14 × 9 = 126', () => {
    expect(cases.length).toBe(126);
  });
  for (const c of cases) {
    it(c.label, async () => {
      await runChain(c);
    });
  }
});

describe('chain100-focus: 実運用 100 段 × 14 種', () => {
  const cases = [...genTriangleChainFocus100()];
  it('chain 数 = 14', () => {
    expect(cases.length).toBe(14);
  });
  for (const c of cases) {
    it(c.label, async () => {
      await runChain(c);
    }, 30_000);
  }
});
