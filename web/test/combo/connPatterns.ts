// 接続パターンの全列挙 (user 確定 2026-06-15「三角形は BC × {辺共有1 + 二重断面3 + フロート3}
// = 14 種、 台形は BCD × 7 = 21 種、 合計 35 + 三↔台 遷移 ≈ 100 種」)。
// 連続接続テスト (genChain) で 1 段あたりこのリストの 1 要素を当てる ── 14 段以上の chain で
// 全パターンが少なくとも 1 度出現する rotation で網羅。

export type Kind = 'triangle' | 'rectangle';
export type ConnType = 0 | 1 | 2; // 0=辺共有 / 1=二重断面 / 2=フロート
export type Lcr = 0 | 1 | 2;       // 0=左起点 / 1=中央 / 2=右起点

export interface ConnPattern {
  /** 子の図形種別 (triangle or trapezoid) */
  childKind: Kind;
  /** 親の接続辺 (1=B, 2=C, 3=D — D は台形親のみ) */
  side: 1 | 2 | 3;
  /** 形態 */
  type: ConnType;
  /** 起点 (type=0 のとき意味なし — 値を 2 で固定) */
  lcr: Lcr;
  /** 一意ラベル */
  label: string;
}

function* enumeratePatterns(childKind: Kind, sides: ReadonlyArray<1 | 2 | 3>): Generator<ConnPattern> {
  for (const side of sides) {
    yield { childKind, side, type: 0, lcr: 2, label: `${childKind[0]}-s${side}t0` };
    for (const lcr of [0, 1, 2] as const) {
      yield { childKind, side, type: 1, lcr, label: `${childKind[0]}-s${side}t1l${lcr}` };
    }
    for (const lcr of [0, 1, 2] as const) {
      yield { childKind, side, type: 2, lcr, label: `${childKind[0]}-s${side}t2l${lcr}` };
    }
  }
}

/** 三角形親 → 子: BC × 7 = 14 種 (user 「BCに辺共有と二重断面三種、 フロート三種の計14種」) */
export const TRI_CONN_14: ReadonlyArray<ConnPattern> = [...enumeratePatterns('triangle', [1, 2])];

/** 台形親 → 子: BCD × 7 = 21 種 (user 「台形ならBCDに以下同文で21種」)
 *  注: 現実装の TRAP_CONN_SIDES = [1, 2] で D(=3) は動線上未使用。 spec で D を含めるかは
 *  実装側のサポート確認後に決める ── まず BC のみ (14 種) で動かし、 D 対応は次の iteration */
export const TRAP_CONN_14: ReadonlyArray<ConnPattern> = [...enumeratePatterns('rectangle', [1, 2])];

/** 三↔台 遷移含む全パターン (= 三角形親 14 + 台形親 14、 子の kind は別軸で振る) */
export const ALL_PARENT_CONN_PATTERNS: ReadonlyArray<ConnPattern> = [
  ...TRI_CONN_14,
  ...TRAP_CONN_14,
];
