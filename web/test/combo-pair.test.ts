// 親 + 子 1 ペアの軸網羅 spec。 dev サーバ + 裏の headless chromium (vite tlcpPlugin) が
// 立ち上がっている前提で CP を叩く。 fixture は gen.ts の generator から bulk 列挙、
// 各件は (a) state.rows 件数 (b) 各 tri の sideCount (c) 砂時計検査 を assert。
//
// 失敗時は label を it.name に焼くので、 どの軸組み合わせで落ちたかが one liner で見える。

import { describe, it, expect, beforeAll } from 'vitest';
import { health, loadCsv, state } from './combo/cpClient.ts';
import { genTriangleTriangle, genTriangleTrapezoid, genTrapezoidTritrap, type ComboCase } from './combo/gen.ts';
import { groupByTri, analyzeTri, detectTriOverlaps } from './combo/primAnalyzer.ts';

beforeAll(async () => {
  const ok = await health();
  if (!ok) {
    throw new Error(
      `dev サーバ (http://localhost:5173) に届かない。 ` +
        `別 terminal で "cd web && npm run dev" を立てておくこと (TLCP_BASE 環境変数で URL 変更可)`
    );
  }
}, 10_000);

async function runOne(c: ComboCase): Promise<void> {
  const loaded = await loadCsv(c.csv);
  expect(loaded.ok, `load ${c.label}`).toBe(true);
  expect(loaded.rows, `rows count ${c.label}`).toBe(c.expectedRows);

  const s = await state();
  expect(s.rows.length, `state rows ${c.label}`).toBe(c.expectedRows);

  const byTri = groupByTri(s.prims ?? []);
  for (const { tri, sides } of c.expectedSideCounts) {
    const lines = byTri.get(tri) ?? [];
    expect(lines.length, `tri=${tri} sideCount ${c.label}\nCSV:\n${c.csv}`).toBe(sides);
    const a = analyzeTri(lines);
    expect(a.hourglass, `tri=${tri} 砂時計検出 ${c.label}\nCSV:\n${c.csv}`).toBe(false);
  }
  // 重なり検出 (user 確定 2026-06-15「重なっててOKなんて条件は基本ない」)。
  const overlaps = detectTriOverlaps(byTri);
  expect(
    overlaps.length,
    `重なり検出 ${c.label}: pairs=${JSON.stringify(overlaps)}\nCSV:\n${c.csv}`,
  ).toBe(0);
}

describe('combo: triangle + triangle (全軸)', () => {
  const cases = [...genTriangleTriangle()];
  it(`fixture 数 = 親辺長×子辺長×conn(2)×type(3)×lcr(3)`, () => {
    expect(cases.length).toBeGreaterThan(0);
  });
  for (const c of cases) {
    it(c.label, async () => {
      await runOne(c);
    });
  }
});

describe('combo: triangle + trapezoid (子=台形、 全軸)', () => {
  const cases = [...genTriangleTrapezoid()];
  it(`fixture 数 = 親辺長×台形辺長×conn(2)×align(3)`, () => {
    expect(cases.length).toBeGreaterThan(0);
  });
  for (const c of cases) {
    it(c.label, async () => {
      await runOne(c);
    });
  }
});

describe('combo: trapezoid + tritrap (親=台形、 全軸)', () => {
  const cases = [...genTrapezoidTritrap()];
  it(`fixture 数 = 台形辺長×align(3)×台形side(2)`, () => {
    expect(cases.length).toBeGreaterThan(0);
  });
  for (const c of cases) {
    it(c.label, async () => {
      await runOne(c);
    });
  }
});
