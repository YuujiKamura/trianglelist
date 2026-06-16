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

/** 親=台形 → 子=三角形 (TriTrap 1 段目) → 孫=三角形 (chain 2 段目) の混成 chain。
 *  user 確定 2026-06-16「台形に接続した三角形に三角形を接続すると図形が壊れる」 が指す動線。
 *  CSV schema: 全 Triangle 行は parent=混在通し番号 1 種類で表現 (TriTrap タグ廃止後)。
 *
 *    混在通し番号:
 *      1 = 台形 (figure[0])
 *      2 = 子三角 (figure[1]、 parent=1 で台形を指す)
 *      3 = 孫三角 (figure[2]、 parent=2 で子三角を指す)
 */
export function* genTrapezoidTritrapChain2(): Generator<ChainCase> {
  for (const side1 of [1, 2, 3] as const) {
    for (const side2 of [1, 2] as const) {
      const csv =
        `Trapezoid,1,1.00,1.00,1.00,-1,0,0,0\n` +
        `2,1.00,0.80,0.80,1,${side1}\n` +
        `3,0.80,0.60,0.60,2,${side2}\n`;
      yield {
        label: `trap-tritrap-chain2/p1s${side1}-p2s${side2}`,
        csv,
        expectedRows: 3,
        expectedSideCounts: [
          { tri: 1, sides: 4 }, // 台形
          { tri: 2, sides: 3 }, // 子三角 (1 段目)
          { tri: 3, sides: 3 }, // 孫三角 (2 段目)
        ],
        axes: { kind: 'trap-tritrap-chain2', side1, side2 },
      };
    }
  }
}

/** depth=4 混成 chain の cartesian 全網羅。
 *  user 確定 2026-06-14「全然足らん。 三、 台、 三、 三とか、 最低 4〜5 個までの各種連結と画面への
 *  反映状態のテストが必要」 への対応。
 *
 *  軸 (= CLAUDE.md「軸を列挙して generator で生成、 固定数字で書くな」):
 *    A. 段 2〜4 の図形種別: { triangle, trapezoid }^3 = 8 通り
 *    B. 親 side: 親が triangle なら {1, 2} (B/C)、 親が trapezoid なら {1, 2, 3} (B/C/D)
 *
 *  段 1 は triangle 固定 (root, parent=-1)。 各段は直前段に接続 (= 純粋 chain)。
 *  CSV schema は TriTrap タグ廃止後 (commit 0b8c51e/24cb249) の Triangle 1 種統合 + 通常 Trapezoid 行。
 *
 *  総数 = Σ (2 × s2 × s3) over kinds ∈ {tri,trap}^3、 ここで si = (2 if kinds[i-1]==='tri' else 3)。
 *  実数 = 2 × ((2×2+2×3+3×2+3×3) × 2) = 2 × 50 = 100 ケース。
 */
type Mix4Kind = 'tri' | 'trap';
function* mix4Cases(): Generator<{
  kinds: [Mix4Kind, Mix4Kind, Mix4Kind];
  sides: [number, number, number];
}> {
  const KINDS: Mix4Kind[] = ['tri', 'trap'];
  for (const k1 of KINDS) for (const k2 of KINDS) for (const k3 of KINDS) {
    const seq: [Mix4Kind, Mix4Kind, Mix4Kind] = [k1, k2, k3];
    // 段 1 (root) は tri 固定なので、 段 1 → 段 2 接続は side ∈ {1,2}
    // 段 i → 段 i+1 接続 (i = 1, 2, 3) の親 side は 親種別で {1,2} or {1,2,3}
    const sidesFor = (parentKind: Mix4Kind | 'rootTri'): number[] =>
      parentKind === 'trap' ? [1, 2, 3] : [1, 2];
    for (const s1 of sidesFor('rootTri'))
      for (const s2 of sidesFor(seq[0]))
        for (const s3 of sidesFor(seq[1])) {
          yield { kinds: seq, sides: [s1, s2, s3] };
        }
  }
}

function buildMix4Csv(kinds: [Mix4Kind, Mix4Kind, Mix4Kind], sides: [number, number, number]): string {
  // 段 1 = root triangle (通し 1)。
  let csv = `1,1.00,1.00,1.00,-1,-1\n`;
  let triCount = 1; // 三角形通し数
  let trapCount = 0; // 台形通し数
  // 直前段の (混在通し番号, kind) を保持
  let prevMixedNum = 1;
  let prevKind: Mix4Kind = 'tri';
  for (let step = 0; step < 3; step++) {
    const kind = kinds[step];
    const parentSide = sides[step];
    if (kind === 'tri') {
      triCount++;
      // Triangle 行 num はローカル三角形通し番号、 parent は混在通し番号 (commit 0b8c51e 統合)
      csv += `${triCount},1.00,0.80,0.80,${prevMixedNum},${parentSide}\n`;
    } else {
      trapCount++;
      // Trapezoid 行: 親が tri なら parent=三角形通し / parentKind=0、
      //               親が trap なら parent=trap ローカル通し / parentKind=1
      const parentForTrap = prevKind === 'tri' ? triCount : trapCount - 1;
      const parentKind = prevKind === 'tri' ? 0 : 1;
      csv += `Trapezoid,${trapCount},1.00,1.00,0.80,${parentForTrap},${parentSide},0,${parentKind}\n`;
    }
    prevMixedNum = step + 2; // 段 step+2 の混在通し (= figureRows 出現順)
    prevKind = kind;
  }
  csv += `ListAngle, 0\n`;
  return csv;
}

/** 4 段混成 chain (= 段 1 三角 + 段 2-4 が {三角, 台形}^3) の全 100 ケースを生成。
 *  各 case は state.rows.length = 4、 各 tri の sideCount は kind で決まる:
 *    tri → 3 辺 (Triangle.sideCount = 3)
 *    trap → 4 辺 (Rectangle.sideCount = 4)
 *  ChainCase の tri field は混在通し番号 (= 出現順 = 段番号)。
 */
export function* genMixedChain4(): Generator<ChainCase> {
  for (const c of mix4Cases()) {
    const csv = buildMix4Csv(c.kinds, c.sides);
    const sidesFor = (k: 'rootTri' | Mix4Kind): number => k === 'trap' ? 4 : 3;
    yield {
      label: `mix4/${['tri', ...c.kinds].join('-')}-s${c.sides.join('-')}`,
      csv,
      expectedRows: 4,
      expectedSideCounts: [
        { tri: 1, sides: sidesFor('rootTri') }, // 段 1 = tri 固定
        { tri: 2, sides: sidesFor(c.kinds[0]) },
        { tri: 3, sides: sidesFor(c.kinds[1]) },
        { tri: 4, sides: sidesFor(c.kinds[2]) },
      ],
      axes: {
        kind: 'mix4',
        kinds: c.kinds.join('-'),
        sides: c.sides.join('-'),
      },
    };
  }
}

// 旧 genTriangleChainFocus100 (depth=100 で 14 種を中央位置に置く) は廃止 (user 2026-06-15
// 「重なっててOKなんて条件は基本ない」)。 depth=100 chain は累積回転で円環化し、 道路測量
// アプリで ありえない描画になる。 累積回転 0 の linear pattern を再設計するまで停止。
