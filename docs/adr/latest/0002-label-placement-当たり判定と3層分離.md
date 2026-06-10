# ADR 0002: ラベル配置は正確な当たり判定を土台に「測る・決める・覚える」を分離する

- 日付: 2026-06-10
- 状態: 採用 (yuuji 発話「ならそれでやっていくか」2026-06-10)
- 関連: ADR 0001 (textsize 2 path 構造)、vault `repos/trianglelist/LABEL_PLACEMENT_DESIGN.md` (全文) / `AUTO_FLAG_HISTORY.md` / `DIMENSION_PLACEMENT_HISTORY.md`

## Context

寸法値・番号サークル・控除名の配置 (重なり回避・自動旗揚げ) は 5 年 40+ commit で未決着。歴史分析で確定した構造的原因:

1. **正確な当たり判定の欠落** ── 自動判定は近接距離 (nearby 0.5→1.0)、面積 (≤3 と ≤5 で不一致)、辺長 (<0.5)、鋭角 (≤20°) とプロキシを変遷したが、いずれも「重なりそう」の推定で「重なっている」の事実ではなく、閾値調整が収束しなかった (c2a51a0 "autoalignHorizontal is difficult?" → b8eed01 "autoHorizontal default off")。yuuji 確言 (2026-06-10):「こうしないとダメだろうな、って思いつつやってないことが、正確な当たり判定を持たせることだ」
2. **包含関係の迷子** ── Triangle が形状とラベル配置状態を両方抱え (dim/dimOnPath/dimpoint/pointNumber、Triangle.kt:14-188)、Dims(this) の相互参照、dimHorizontalA/B/C と Dims.horizontal の二重持ち、同型 Flags の 2 箇所定義。
3. **接触はメッシュ全体の性質なのに判定が三角形単体 (+隣1個) に閉じている** ── 1 三角形で決められないものを 1 三角形のクラスに置いたから置き場所が定まらなかった。

業界の確立解: AutoCAD Fit options / DIMATFIT / DIMTMOVE は「入るか」を判定した上で 矢印外→文字外→引出線 と段階的にフォールバックする (https://help.autodesk.com/cloudhelp/2021/ENU/AutoCAD-Core/files/GUID-E2E21B42-82AF-4D46-B9DD-F0844D20F719.htm)。地図製図のラベル配置問題も「候補生成→衝突評価→選択」が基本形。

## Decision

**A. 3+1 層に分離する**

- Measurement: `LabelBox` (テキスト実寸の回転付き矩形) + `CollisionField` (メッシュ全体の障害物集合への衝突クエリ)。状態なし。
- Placement: `LabelPlacer` (pure)。段階的フォールバック: 定位置 → 内/外フリップ・鈍角側スライド → 旗揚げ (既存 HATAAGE 計算を流用) → 旗揚げ衝突なら外周へ積む。判定はすべて CollisionField に訊く。
- Override: `PlacementOverrides`。user 手動移動の集約 (散在する isMovedByUser を引き剥がす)。Placer は override 済みをスキップ = 「自動は初期配置、最終判断は人間」の構造化。
- Geometry (既存): Triangle/TriangleList は形状のみに戻す (最終段階)。

**B. ラベルの所有者は Triangle ではなく図面 (TriangleList 単位の LabelLayout)**。寸法値・番号サークル・控除名・測点名は同じ Label 抽象の kind 違い。

**C. 実装順は viewer-first (insight #35 の方針)**

1. common (`com.jpaver.trianglelist.label`) に LabelBox / CollisionField を純 Kotlin + unit test で新設
2. viewer Inspector に「重なり検出 N 件」を数値表示、CP inspector で観測可能に ── **まず測る、直すのは後**
3. viewer 上に LabelPlacer の配置案をオーバーレイして目視批評
4. 頃合いを見てアプリ側をフリップ (Dims/PointNumberManager/Deduction の判定置換、Flags→overrides 移行)。手動サイクル UI は override の入力手段として維持

段階 2 で止めても「重なりの定量観測」が 5 年問題の test 基盤として立つ。

## Consequences

### Positive
- プロキシ閾値 (面積/辺長/角度) が事実 (衝突クエリ) に置き換わり、調整の迷走が終わる
- Flags 3 箇所 → overrides 1 箇所、状態の二重持ち解消、Triangle ⇄ Dims 相互参照解消
- 重なり件数が数値観測できる = 配置変更の効果測定が test になる

### Negative / リスク
- テキスト実寸は platform 依存 ── LabelMetrics interface で注入、DXF は係数近似から始めて viewer 実測との乖離を Inspector で観測してから決める
- 旗揚げの二次衝突は密度に比例して難化 ── greedy (確定済みを障害物に足しながら順次配置) で始め、全体最適化 (annealing 等) には最初から行かない
- アプリ側フリップ (段階 4) は behavior change ── 段階 1-3 の観測と目視批評を通ってから別途判断

## Alternatives considered

- **A1. プロキシ閾値の再調整を続ける**: 5 年収束しなかった経験則に反する。却下
- **A2. 最初から全体最適化 (simulated annealing 等)**: 段階的フォールバックで足りるかを観測する前に複雑化する。却下
- **A3. アプリ側から直接書き換える**: viewer-first 方針 (理想形を viewer で演繹して頃合いでフリップ) に反し、5 年 stable な現挙動を観測なしに壊すリスク。却下

## 出典

- yuuji 発話 (2026-06-10): 「クラス関係をいろいろいじってて、最適な包含関係が良くわからなくなっていた」「自動で旗揚げするにはどうするか？を幾つか試行した形跡がある」「正確な当たり判定を持たせることだな」「ならそれでやっていくか」
- 歴史分析の領収書: vault `AUTO_FLAG_HISTORY.md` (commit c2a51a0 / e5ef179 / 599286c / b8eed01 / ff57df1 / 3fde56b、現役コード DimOnPath.kt:114-123 HATAAGE / Dims.kt:11 enableAutoHorizontal=false / PointNumberManager.kt:33-47)
- AutoCAD Fit options: Autodesk help (URL 本文中)
