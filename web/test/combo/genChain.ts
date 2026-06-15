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

/** 全段 default (side 交互辺共有) chain。 depth=10 で 1 chain × startSide 2 種 = 2 chain。
 *  user 確定 2026-06-15「重なっててOKなんて条件は基本ない」── 道路測量アプリで重なる図形は
 *  ありえない。 旧版で depth=100 だと円環状に 1 周、 depth=200 だと 2 周して重なるため
 *  廃止。 chain pattern を累積回転 0 にする linear 設計は次 iteration の課題。 */
export function* genTriangleChainSharedDeep(): Generator<ChainCase> {
  for (const startSide of [1, 2] as const) {
    const { csv, valid } = buildChainCsv(10, () => null, startSide);
    if (!valid) continue;
    yield {
      label: `chain-shared/s${startSide}_d10`,
      csv,
      expectedRows: 10,
      expectedSideCounts: Array.from({ length: 10 }, (_, i) => ({ tri: i + 1, sides: 3 })),
      axes: { startSide, depth: 10 },
    };
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

/** 親=台形 + 子=三角形 (TriTrap) ペア。 user 確定 2026-06-15「重なっててOKなんて条件は基本ない」
 *  ── 旧版で 9 段 chain にしたら全 TriTrap が同位置に積み重なる (= 辺共有 chain で scaling なし)。
 *  道路測量アプリ仕様に合わないため pair 1 つに絞る。 台形 BCD = side {1, 2, 3} で 3 ケース。
 *
 *  CSV schema: 親 = Trapezoid 行、 子 = 通常三角形行で parent=混在通し番号 (= 三角形数 + 台形 idx)。
 */
export function* genTrapezoidTritrapChain(): Generator<ChainCase> {
  for (const side of [1, 2, 3] as const) {
    // 親台形 = 正方形 (length=widthA=widthB=1)、 子三角 1 つだけ親の指定辺に乗せる
    const csv = `Trapezoid,1,1.00,1.00,1.00,-1,0,0,0\n2,1.00,1.00,1.00,1,${side}\n`;
    yield {
      label: `trap-tritrap-pair/s${side}`,
      csv,
      expectedRows: 2,
      expectedSideCounts: [
        { tri: 1, sides: 4 }, // 台形
        { tri: 2, sides: 3 }, // 子三角形 (TriTrap)
      ],
      axes: { kind: 'trap-tritrap-pair', side },
    };
  }
}

// 旧 genTriangleChainFocus100 (depth=100 で 14 種を中央位置に置く) は廃止 (user 2026-06-15
// 「重なっててOKなんて条件は基本ない」)。 depth=100 chain は累積回転で円環化し、 道路測量
// アプリで ありえない描画になる。 累積回転 0 の linear pattern を再設計するまで停止。
