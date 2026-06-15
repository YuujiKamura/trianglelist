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

/** chain 内 default の接続パターン: side を 1/2 交互で出して chain がジグザグに展開する形にする。
 *  正三角形 (1,1,1) を全段辺共有で繋ぐと 60° ずつ回転して 6 段で 1 周してしまい重なる ── これを
 *  避けるために 同じ三角形でも side を交互にすることで 重ならず深い chain を維持。 */
function alternatingDefault(d: number, startSide: 1 | 2 = 1): ConnPattern {
  const side: 1 | 2 = ((d - 2) % 2 === 0 ? startSide : (startSide === 1 ? 2 : 1));
  return { childKind: 'triangle', side, type: 0, lcr: 2, label: `alt-s${side}` };
}

/** 親 = 直角三角形 (3, 4, 5) を起点とする chain build。 各段の (a, b, c) を triEdges に追跡。
 *  patternAt(d) が null を返した段は alternatingDefault (side 交互辺共有) を使い、 重ならず
 *  深く繋がる。 patternAt が ConnPattern を返した段はそれを使う (focus 用)。 */
function buildChainCsv(
  depth: number,
  patternAt: (d: number) => ConnPattern | null,
  startSide: 1 | 2 = 1,
): { csv: string; valid: boolean } {
  const lines: string[] = ['1,3.00,4.00,5.00,-1,-1'];
  const triEdges: Array<[number, number, number]> = [[3, 4, 5]];
  for (let d = 2; d <= depth; d++) {
    const p = patternAt(d) ?? alternatingDefault(d, startSide);
    const parentTri = triEdges[d - 2];
    const parentEdgeLen = p.side === 1 ? parentTri[1] : parentTri[2];
    const aRatio = p.type === 0 ? 1.0 : 0.5;
    const ca = parentEdgeLen * aRatio;
    // 子の b/c は親の b/c を直接継承 (= 辺長保持 chain)。 比率 scaling だと a が指数増加して
    // 200 段で 1e35 オーダー → 浮動小数 / 描画 canvas が破綻するため。 b/c 直接継承なら 200 段でも
    // 辺長は (3, 4, 5) 周辺で安定。 valid: parent edge (= 4 or 5) + 4 > 5、 5 + 4 > 4 ── 常に OK。
    const cb = parentTri[1];
    const cc = parentTri[2];
    if (!isValidTriangle(ca, cb, cc)) return { csv: '', valid: false };
    triEdges.push([ca, cb, cc]);
    const childExtras = `,,,,,,,,,,,,${p.side},${p.type},${p.lcr}`;
    lines.push(`${d},${fmt(ca)},${fmt(cb)},${fmt(cc)},${d - 1},${p.side}${childExtras}`);
  }
  return { csv: lines.join('\n') + '\n', valid: true };
}

/** 全段 default (side 交互辺共有) chain。 depth = 10/100/200 で 1 chain ずつ × startSide 2 種 = 6 chain。
 *  user 「実運用では 100 とか 200 くらい」 を直接 pin する deep chain。 重ならずジグザグ展開。 */
export function* genTriangleChainSharedDeep(): Generator<ChainCase> {
  for (const depth of [10, 100, 200] as const) {
    for (const startSide of [1, 2] as const) {
      const { csv, valid } = buildChainCsv(depth, () => null, startSide);
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
 *  14 種 × {pos 2..10} = 126 chain。 残段は side 交互辺共有 で 重ならない。 */
export function* genTriangleChainFocus10(): Generator<ChainCase> {
  for (let pIdx = 0; pIdx < TRI_CONN_14.length; pIdx++) {
    const focus = TRI_CONN_14[pIdx];
    for (let focusPos = 2; focusPos <= 10; focusPos++) {
      const { csv, valid } = buildChainCsv(10, (d) => (d === focusPos ? focus : null));
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

/** 親=台形 + 子=三角形 (TriTrap) chain。 台形親の 接続辺 B/C/D = side {1, 2, 3} で各 chain。
 *  TriTrap は実装上 type/lcr が無く 辺共有のみ。 各段同 side で繋ぐ ── 「台形 BCD 3 種網羅」。
 *  depth 10 で user 「最低 10 個」 を満たし、 親台形 1 個 + 子三角 (= TriTrap) 9 個。
 *
 *  CSV schema: 親 = Trapezoid 行 (Trapezoid,num,length,widthA,widthB,parent,side,align,parentKind)、
 *  子 = 通常三角形行で parent=三角形数 + 台形 idx の混在通し番号 (旧 TriTrap タグは内部で同等)。
 */
export function* genTrapezoidTritrapChain(): Generator<ChainCase> {
  for (const side of [1, 2, 3] as const) {
    // 親台形 = 正方形相当 (length=1, widthA=1, widthB=1)
    const lines: string[] = ['Trapezoid,1,1.00,1.00,1.00,-1,0,0,0'];
    // 各 TriTrap の (b, c) = (1, 1) で 親辺長 1 と一致 (辺共有 chain で長さ保持)
    let parentMixedNum = 1; // 親台形は 混在通し番号 1
    for (let d = 2; d <= 10; d++) {
      // 子三角行: n,a,b,c,parent_mixed,side。 type/lcr は TriTrap で無効 (空に明示しない)
      lines.push(`${d},1.00,1.00,1.00,${parentMixedNum},${side}`);
      parentMixedNum = d; // 次の親は 1 つ前の TriTrap (= 自身 = d)
    }
    yield {
      label: `chain-tritrap/s${side}_d10`,
      csv: lines.join('\n') + '\n',
      expectedRows: 10,
      // tri=1 は台形 (sideCount=4)、 tri=2..10 は TriTrap (= 三角形 sideCount=3)
      expectedSideCounts: [
        { tri: 1, sides: 4 },
        ...Array.from({ length: 9 }, (_, i) => ({ tri: i + 2, sides: 3 })),
      ],
      axes: { kind: 'trap-tritrap', side, depth: 10 },
    };
  }
}

/** depth=100 で各 14 種を 1 度ずつ注目位置に置く chain (位置は 50 = 中央付近に固定)。
 *  実運用 100 段相当で全 14 種が破綻しないことを保証する。 */
export function* genTriangleChainFocus100(): Generator<ChainCase> {
  for (let pIdx = 0; pIdx < TRI_CONN_14.length; pIdx++) {
    const focus = TRI_CONN_14[pIdx];
    const focusPos = 50;
    const { csv, valid } = buildChainCsv(100, (d) => (d === focusPos ? focus : null));
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
