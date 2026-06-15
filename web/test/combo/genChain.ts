// 連続接続テストの fixture 生成 (user 確定 2026-06-15「最低でも10個のエンティティの連続、
// すべて接続図形や形態を網羅したテストでなければ意味がない」 + 「実運用では100とか200くらいまで
// は普通にあり得る」)。
//
// 設計: chain は depth N (N=10/100/200) の鎖。 各 chain で 1 段だけ「注目 pattern」 (= 14 種の
// いずれか) を当て、 残りは辺共有 (s1, type=0) で繋いで scaling せず長さを保つ。 これにより:
//   - depth 200 まで実運用相当 (user 確定)
//   - 注目 pattern × 段位置 の cartesian で 14 種が全部試される
//   - 残段は辺共有なので辺長が縮まず、 描画破綻なく深く繋がる

import { TRI_CONN_14, type ConnPattern } from './connPatterns.ts';

export interface ChainCase {
  label: string;
  csv: string;
  expectedRows: number;
  expectedSideCounts: ReadonlyArray<{ tri: number; sides: number }>;
  axes: Record<string, string | number>;
}

const fmt = (n: number): string => (Number.isInteger(n) ? String(n) : n.toFixed(2));
const isValidTriangle = (a: number, b: number, c: number): boolean =>
  a + b > c && b + c > a && c + a > b;

const DEFAULT_SHARED: ConnPattern = { childKind: 'triangle', side: 1, type: 0, lcr: 2, label: 'shared' };

/** 親 = 正三角形 (1, 1, 1) を起点とする chain build。 各段の (a, b, c) を triEdges に追跡。 */
function buildChainCsv(depth: number, patternAt: (d: number) => ConnPattern): { csv: string; valid: boolean } {
  const lines: string[] = ['1,1.00,1.00,1.00,-1,-1'];
  const triEdges: Array<[number, number, number]> = [[1, 1, 1]];
  for (let d = 2; d <= depth; d++) {
    const p = patternAt(d);
    const parentTri = triEdges[d - 2];
    const parentEdgeLen = p.side === 1 ? parentTri[1] : parentTri[2];
    const aRatio = p.type === 0 ? 1.0 : 0.5;
    const ca = parentEdgeLen * aRatio;
    const cb = ca; // 正三角形維持 (辺共有 chain で辺長一定)
    const cc = ca;
    if (!isValidTriangle(ca, cb, cc)) return { csv: '', valid: false };
    triEdges.push([ca, cb, cc]);
    const childExtras = `,,,,,,,,,,,,${p.side},${p.type},${p.lcr}`;
    lines.push(`${d},${fmt(ca)},${fmt(cb)},${fmt(cc)},${d - 1},${p.side}${childExtras}`);
  }
  return { csv: lines.join('\n') + '\n', valid: true };
}

/** 全段 辺共有 chain。 depth = 10/100/200 で 1 chain ずつ × side 2 種 = 6 chain。
 *  user 「実運用では 100 とか 200 くらい」 を直接 pin する deep chain。 */
export function* genTriangleChainSharedDeep(): Generator<ChainCase> {
  for (const depth of [10, 100, 200] as const) {
    for (const startSide of [1, 2] as const) {
      const pat: ConnPattern = { childKind: 'triangle', side: startSide, type: 0, lcr: 2, label: `shared-s${startSide}` };
      const { csv, valid } = buildChainCsv(depth, () => pat);
      if (!valid) continue;
      yield {
        label: `chain-shared/s${startSide}_d${depth}`,
        csv,
        expectedRows: depth,
        expectedSideCounts: Array.from({ length: depth }, (_, i) => ({ tri: i + 1, sides: 3 })),
        axes: { startSide, depth },
      };
    }
  }
}

/** 14 種 × 注目位置 の chain。 depth=10 (user 最低 10 個) で 1 段だけ注目 pattern を埋め込み。
 *  14 種 × {pos 2..10} = 126 chain。 残段は辺共有で辺長一定。 */
export function* genTriangleChainFocus10(): Generator<ChainCase> {
  for (let pIdx = 0; pIdx < TRI_CONN_14.length; pIdx++) {
    const focus = TRI_CONN_14[pIdx];
    for (let focusPos = 2; focusPos <= 10; focusPos++) {
      const { csv, valid } = buildChainCsv(10, (d) => (d === focusPos ? focus : DEFAULT_SHARED));
      if (!valid) continue;
      yield {
        label: `chain10-focus/${focus.label}_pos${focusPos}`,
        csv,
        expectedRows: 10,
        expectedSideCounts: Array.from({ length: 10 }, (_, i) => ({ tri: i + 1, sides: 3 })),
        axes: { pattern: focus.label, pIdx, focusPos, depth: 10 },
      };
    }
  }
}

/** depth=100 で各 14 種を 1 度ずつ注目位置に置く chain (位置は 50 = 中央付近に固定)。
 *  実運用 100 段相当で全 14 種が破綻しないことを保証する。 */
export function* genTriangleChainFocus100(): Generator<ChainCase> {
  for (let pIdx = 0; pIdx < TRI_CONN_14.length; pIdx++) {
    const focus = TRI_CONN_14[pIdx];
    const focusPos = 50;
    const { csv, valid } = buildChainCsv(100, (d) => (d === focusPos ? focus : DEFAULT_SHARED));
    if (!valid) continue;
    yield {
      label: `chain100-focus/${focus.label}`,
      csv,
      expectedRows: 100,
      expectedSideCounts: Array.from({ length: 100 }, (_, i) => ({ tri: i + 1, sides: 3 })),
      axes: { pattern: focus.label, pIdx, focusPos, depth: 100 },
    };
  }
}
