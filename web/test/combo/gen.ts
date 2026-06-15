// 軸を cartesian で展開して CSV テキストを機械生成する。 軸ごとに gen 関数を分け、 spec が
// 必要な範囲だけ呼ぶ。 1 つ 1 つの fixture は「親 1 行 + 子 1 行」 の極小 CSV に絞り、
// renderer の振る舞いを軸単独で観察できるようにする (混在 chain は別 spec で組み立てる)。

import {
  TRIANGLE_SIDE_SAMPLES,
  TRI_CONN_SIDES,
  CONN_TYPES,
  CONN_LCRS,
  CHILD_A_RATIOS,
  TRAP_SAMPLES,
  TRAP_ALIGNS,
  TRAP_CONN_SIDES,
} from './axes.ts';

export interface ComboCase {
  /** 軸の値を全部畳んだ識別子。 fixture path / spec の it.name / failure 出力に使う */
  label: string;
  csv: string;
  /** 期待: kind ごとに何行できるか (state.rows.length と比較) */
  expectedRows: number;
  /** 期待: 各 tri 番号の sideCount (line layer=tri 数) — prim assertion 用 */
  expectedSideCounts: ReadonlyArray<{ tri: number; sides: number }>;
  /** 軸の生値 (debug / DB 化用)、 任意拡張可 */
  axes: Record<string, string | number>;
}

const fmt = (n: number): string => (Number.isInteger(n) ? String(n) : n.toFixed(2));

// 三角不等式の strict チェック。 退化 (a+b == c など等号成立で一直線) は false。
// fixture 生成側で退化組み合わせを skip するために使う ── 実装側の isValidLengthes が
// false を返して描画されない組み合わせを spec 期待値 (sides=3) で要求すると必ず落ちる。
const isValidTriangle = (a: number, b: number, c: number): boolean =>
  a + b > c && b + c > a && c + a > b;

/**
 * 親=三角形 × 子=三角形 の全軸。 約 |TRIANGLE_SAMPLES|² × |SIDES| × |TYPES| × |LCRS|。
 * 既定の 5 × 5 × 2 × 3 × 3 = 450 件 (TRIANGLE_SIDE_SAMPLES を絞れば 18-200 件レンジ)
 */
export function* genTriangleTriangle(): Generator<ComboCase> {
  for (let pi = 0; pi < TRIANGLE_SIDE_SAMPLES.length; pi++) {
    for (let ci = 0; ci < TRIANGLE_SIDE_SAMPLES.length; ci++) {
      const [pa, pb, pc] = TRIANGLE_SIDE_SAMPLES[pi];
      const [, cb, cc] = TRIANGLE_SIDE_SAMPLES[ci];
      for (const side of TRI_CONN_SIDES) {
        const parentEdgeLen = side === 1 ? pb : pc;
        for (const cType of CONN_TYPES) {
          for (const cLcr of CONN_LCRS) {
            for (const cAratio of CHILD_A_RATIOS) {
              // type=0 (辺共有) は子A=親辺長 強制 (= ratio 1.0)。 0.5 と組み合わせると
              // 「辺共有なのに子Aが短い」 矛盾 fixture になるので skip。
              if (cType === 0 && cAratio !== 1.0) continue;
              const ca = parentEdgeLen * cAratio;
              // 退化三角形 (a+b<=c の等号成立を含む) は実装側の isValidLengthes で
              // 描画されないので fixture から除外する (user 確定 2026-06-15)
              if (!isValidTriangle(ca, cb, cc)) continue;
              // 完全形式 CSV (CsvCodec.kt 列順): 列17=cp.side, 列18=cp.type, 列19=cp.lcr。
              // カンマ数: 列5=side(prefix末尾) → 列6..16 が空11個 → 列17=side, 列18=type, 列19=lcr
              const parentLine = `1,${fmt(pa)},${fmt(pb)},${fmt(pc)},-1,-1`;
              const childExtras = `,,,,,,,,,,,,${side},${cType},${cLcr}`;
              const childLine = `2,${fmt(ca)},${fmt(cb)},${fmt(cc)},1,${side}${childExtras}`;
              const csv = `${parentLine}\n${childLine}\n`;
              const ratioTag = cAratio === 1.0 ? 'full' : 'half';
              yield {
                label: `tri-tri/p${pi}c${ci}_s${side}_t${cType}_l${cLcr}_a${ratioTag}`,
                csv,
                expectedRows: 2,
                expectedSideCounts: [
                  { tri: 1, sides: 3 },
                  { tri: 2, sides: 3 },
                ],
                axes: { parent: `${pa}-${pb}-${pc}`, child: `${ca}-${cb}-${cc}`, side, type: cType, lcr: cLcr, ratio: cAratio },
              };
            }
          }
        }
      }
    }
  }
}

/**
 * 親=三角形 × 子=台形。 接続辺 (側 B/C) × 台形 3 辺サンプル × align。
 * parentKind=0 (親=三角形)。 台形には「形態」 「起点」 はないので軸数が少ない。
 */
export function* genTriangleTrapezoid(): Generator<ComboCase> {
  for (let pi = 0; pi < TRIANGLE_SIDE_SAMPLES.length; pi++) {
    const [pa, pb, pc] = TRIANGLE_SIDE_SAMPLES[pi];
    for (let ti = 0; ti < TRAP_SAMPLES.length; ti++) {
      const [length, widthA, widthB] = TRAP_SAMPLES[ti];
      for (const side of TRI_CONN_SIDES) {
        // 子台形の widthA は親辺長 (共有) に揃える。 length=延長 / widthB=上辺は自由
        const parentEdge = side === 1 ? pb : pc;
        for (const align of TRAP_ALIGNS) {
          const parentLine = `1,${fmt(pa)},${fmt(pb)},${fmt(pc)},-1,-1`;
          const trapLine = `Trapezoid,1,${fmt(length)},${fmt(parentEdge)},${fmt(widthB)},1,${side},${align},0`;
          const csv = `${parentLine}\n${trapLine}\n`;
          yield {
            label: `tri-trap/p${pi}t${ti}_s${side}_a${align}`,
            csv,
            expectedRows: 2,
            expectedSideCounts: [
              { tri: 1, sides: 3 },
              { tri: 2, sides: 4 },
            ],
            axes: { parent: `${pa}-${pb}-${pc}`, trap: `${length}-${widthA}-${widthB}`, side, align },
          };
        }
      }
    }
  }
}

/**
 * 独立台形 × 親=台形子三角形 (TriTrap)。 台形辺長 × align × TriTrap 接続辺 (右脚/上辺)。
 * TriTrap 自身の b/c は親辺長の 0.75 倍を採用 (buildShadow と同係数)
 */
export function* genTrapezoidTritrap(): Generator<ComboCase> {
  for (let ti = 0; ti < TRAP_SAMPLES.length; ti++) {
    const [length, widthA, widthB] = TRAP_SAMPLES[ti];
    for (const align of TRAP_ALIGNS) {
      for (const trapSide of TRAP_CONN_SIDES) {
        // TriTrap の親辺長 = 台形の指定 side 長 (右脚 length = 上辺 widthB)
        const parentLen = trapSide === 1 ? length : widthB;
        const leg = parentLen * 0.75;
        // 退化三角形チェック (leg=0.75*parentLen で常に valid だが保険で揃える)
        if (!isValidTriangle(parentLen, leg, leg)) continue;
        const trapLine = `Trapezoid,1,${fmt(length)},${fmt(widthA)},${fmt(widthB)},-1,0,${align},0`;
        const tritrap = `TriTrap,2,0,${fmt(leg)},${fmt(leg)},1,${trapSide}`;
        const csv = `${trapLine}\n${tritrap}\n`;
        yield {
          label: `trap-tritrap/t${ti}_a${align}_s${trapSide}`,
          csv,
          expectedRows: 2,
          expectedSideCounts: [
            { tri: 1, sides: 4 },
            { tri: 2, sides: 3 },
          ],
          axes: { trap: `${length}-${widthA}-${widthB}`, align, trapSide },
        };
      }
    }
  }
}

/** 全軸を 1 本に束ねた generator。 spec が flatMap で回す用 */
export function* genAll(): Generator<ComboCase> {
  yield* genTriangleTriangle();
  yield* genTriangleTrapezoid();
  yield* genTrapezoidTritrap();
}
