// 図形組み合わせテストの軸定義。 user 2026-06-15「単純にリスト上で図形種別を表示する事と、
// 複合図形の生成が問題なくできることをかなりの個数の組み合わせでテスト」 + CLAUDE.md Rule 6
// 「軸を列挙して cartesian の積で全件 pin、 固定数字で書くな」。
//
// 新しい軸を足したい時はここに追加するだけで gen.ts/spec が自動展開する設計 (将来拡張優先)。

// 三角形の辺長サンプル ── 正三角 / 直角 / 鋭角 / 一般 / 鈍角寄り の代表 5 セット。
// 三角不等式を満たす範囲で「形を変える本質的なバリエーション」 を網羅 (cartesian の base)
export const TRIANGLE_SIDE_SAMPLES: ReadonlyArray<readonly [number, number, number]> = [
  [3, 3, 3],
  [3, 4, 5],
  [5, 5, 5],
  [4, 5, 6],
  [2, 3, 4],
] as const;

// 親=三角形での子の接続辺 (CSV 列5)。 親 A 辺接続は仕様上不可なので 2 種のみ
export const TRI_CONN_SIDES = [1, 2] as const; // 1=B, 2=C

// 形態 (cParam.type、 完全形式 CSV 列18): 0=辺共有 / 1=二重断面 / 2=フロート
export const CONN_TYPES = [0, 1, 2] as const;

// 起点 (cParam.lcr、 完全形式 CSV 列19): 0=左 / 1=中央 / 2=右
export const CONN_LCRS = [0, 1, 2] as const;

// 子A辺長と親辺長の比。 type=0 (辺共有) は強制で 1.0 のみ意味があるが、
// type=1 (二重断面) と type=2 (フロート) では 1.0 (= 親辺全体) と 0.5 (= 親辺の一部) で
// 描画が異なる ── 1.0 のみだと lcr の効果が見えず spec が tautological になる (2026-06-15)
export const CHILD_A_RATIOS = [1.0, 0.5] as const;

// 台形の (length, widthA, widthB) サンプル。 width 等差(正方形系) と非等差(平行四辺/直角台) を混ぜる
export const TRAP_SAMPLES: ReadonlyArray<readonly [number, number, number]> = [
  [3, 3, 3],   // 正方形 (length=widthA=widthB)
  [2, 3, 4],   // 一般台形
  [1.5, 3, 2.4],
  [3, 4, 4],   // 平行四辺形寄り
] as const;

// 台形の align (CSV 列8 = alignment、 起点に相当): 0/1/2/3
// 0=中央 1=右寄せ 2=左寄せ 3=台形子の自由配置 (Rectangle 仕様、 main.ts と整合)
export const TRAP_ALIGNS = [0, 1, 2] as const; // 3 は子配置用なので一旦保留

// 台形の接続辺 (TriTrap / 子 Trapezoid が乗る側): 0=底/1=右脚/2=上/3=左脚
// ただし side=0 (底辺) は親と共有済みのため、 子が乗れるのは 1/2/3 (現実は 1/2 が動線)
export const TRAP_CONN_SIDES = [1, 2] as const;

// 親種別 ── 親が居る場合の親 EditObject 種別
export type ParentKind = 'none' | 'triangle' | 'trapezoid';

export const PARENT_KINDS: ReadonlyArray<ParentKind> = ['none', 'triangle', 'trapezoid'] as const;

// 子種別 ── 親に接続して作る図形種別
export type ChildKind = 'triangle' | 'trapezoid' | 'tritrap';

export const CHILD_KINDS: ReadonlyArray<ChildKind> = ['triangle', 'trapezoid', 'tritrap'] as const;
