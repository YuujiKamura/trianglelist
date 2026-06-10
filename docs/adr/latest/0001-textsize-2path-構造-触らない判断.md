# ADR 0001: DXF text-size の 2 path 構造を触らない、整理候補は behavior unchanged な refactor のみ

- 日付: 2026-06-10
- 状態: 採用
- 関連: vault episodes
  - 2026-06-10-trianglelist-textsize-全貌-3worker合流.md
  - 2026-06-10-trianglelist-worker-A-業界CADのtext-paper扱い比較.md
  - 2026-06-10-trianglelist-worker-B-1f-hardcode-の-git歴.md
  - 2026-06-10-trianglelist-worker-C-MainActivity係数0_016と20と0_5の出所.md
  - 2026-06-10-trianglelist-textsize-実用域はpaper1mm級-jis3_5mmではない.md

## Context

trianglelist の DXF text 書き出しは **2 path 構造**:

- タイトル枠 (`writeDrawingFrame` / `writeTopTitle`): `unitscale_ *= printscale_` してから書き出し → 全 drawingScale で **paper 2.5mm 固定**。
- 寸法値・三角形本体 (`writeTextHV` ほか): `unitscale_ = 1000f` (reset) してから書き出し → **常に model 250mm 固定**、paper は drawingScale 逆比例。

業界 default (AutoCAD annotative / Jw_cad / BricsCAD / V-nas / JIS Z 8313 / 国交省 CAD 製図基準 / SXF) は **paper mm 一定 1 機構**。trianglelist の 2 path 構造は業界平均と異なる。

ただし 2 path 構造は **Initial commit (dafabbd) からの構造**、後の事故で入ったものではない。 さらに 2021-09-20 (commit 219cb90 "textsize changes to reflect the content") で TriangleList.getPrintTextScale 経路を **明示的に放棄**、MainActivity:2567/2589/2609 で「画面 px × 形式別係数 (0.016 / 20 / 0.5)」 に切り替えた。 元の物理意図値 0.014 / 14 / 0.4 は JIS 製図 0.35mm CAD / 3.5mm 印刷 にドンピシャだったが、 user が actual 出力を目視して「大きめが見やすい」 と +14〜43% 押し上げた経験値が現値。

user 実用域 (yuuji 発話 2026-06-10、同日 2 回目の発話で補正): **最多用は 1/200〜1/300**、最大 1/500、1/600 は実用外。 実 test 数値:

| drawingScale | textscale_ | 寸法値 paper mm |
|---|---|---|
| 1/50  | 0.25 | 5.00 |
| 1/150 | 0.25 | 1.67 |
| 1/200 | 0.35 | 1.75 |
| 1/250 | 0.35 | 1.40 |
| 1/450 | 0.25 | 0.56 |
| 1/600 | 0.25 | 0.42 |

JIS 最低 (CAD 用 1.8mm) から見れば 1/200 以下は違反域だが、user 業務 (公共工事の数量展開図、 PDF プレビュー中心) で paper 1mm 級は 5 年 stable に受け入れられている。

**設計制約 ── 寸法値サイズの上限は紙ではなく三角形の幾何で決まる** (yuuji 2026-06-10):

> 「最も多用するのが200分の1から300分の1くらいで、そこで最低限視認性が保たれるサイズにしてる。なぜかっていうと、小さな三角形の場合、寸法値をでかくすると他と接触するっていう本質的な問題があるからだ」

数量展開図は小さい三角形が密集するメッシュで、寸法値を paper 基準 (JIS 3.5mm 等) に拡大すると**隣接する寸法値・辺と接触する**。業界 CAD (annotative) は「紙上の読みやすさ」を保証するが「model 上の密集回避」は保証せず、重なりは人間が引出線で逃がす前提 ── trianglelist は自動生成なので、でかくすると自動配置が破綻する。つまり paper 一定 fix は業界標準に揃えても**この図面種別では答えにならない**。model 250mm 固定は「最多用域 1/200〜1/300 で視認でき、かつ接触しない」バランスを user が目視で選んだ経験値。なお寸法値配置の重なり問題自体は 5 年 40+ commit の未決問題 (vault `DIMENSION_PLACEMENT_HISTORY.md` 参照) で、サイズと配置は同じ「接触回避」問題の 2 つの顔。

## Decision

**A. 2 path 構造 (タイトル枠 / 寸法値) はそのまま維持する**。

paper-一定の annotative 風設計に揃え直す (= 寸法値 path も `unitscale_ *= printscale_` 適用) のは behavior change で、 user 体感 baseline (paper 1mm 級) を壊す。 user が明示要求した時点で別 ADR で再決定する。

**B. MainActivity の係数 0.016 / 20 / 0.5 は維持する**。

これらは 2021-09 に user が目視で確定した経験値。 JIS 物理基準 (0.014 / 14 / 0.4) に戻すのは user 体感を壊す。

**C. 以下の整理候補は behavior unchanged refactor として実施可**:

1. `app/src/main/java/com/jpaver/trianglelist/datamanager/PdfWriter.kt:27`
   `override var textscale_ = triangleList_.getPrintTextScale( 1f, "pdf")` は MainActivity:2609 で上書きされる dead init。 削除可。
2. `app/src/main/java/com/jpaver/trianglelist/datamanager/DxfFileWriter.kt:44`
   コメント `//setScale(drawingLength)` は 5 年放置の残骸 (= 旧 getPrintScale 設計の名残)。 削除可。
3. MainActivity:2567/2589/2609 のコメント `25 *…` sanity check は `MyView.textSize = 25f` 当時の前提、 現在 `textSize = 30f` で更新漏れ。 30 ベースに直すか、 sanity check 自体を消すか。
4. 「scale」 用語の整理 (drawingScale / myScale / drawingScale_ / printScale_ / 1f / drawingLength の混在を rename)。
5. writer 4 ヶ所の `getPrintTextScale(1f, ...)` / `getPrintScale(1f)` の `1f` を **デフォルト引数化** (`fun getPrintScale(drawingScale: Float = 1f)`) して call site から `1f` を消す。

**D. viewmodel.TextScaleCalculator は削除しない**。

writer init で参照されており、 削除すると compile が落ちる。 整理候補 1 (PdfWriter dead init 削除) を進めると参照箇所が 1 つ減る。 また将来「paper 一定 fix」 を選ぶ場合の素材として残しておく。

## Consequences

### Positive

- user 体感 baseline (paper 1mm 級) を維持。
- 5 年 stable な動作を変えない。
- 整理候補 1-5 は behavior unchanged、 test 通れば安全。
- 「2 path はあえて維持」 を repo に永続化し、 将来の AI / 人間が「business 整合 (= AutoCAD annotative に揃える) で fix しよう」 と再導出するのを止める。

### Negative

- JIS Z 8313-5 最低 2.5mm、 国交省 CAD 製図基準 最低 1.8mm を user 実用域で違反。 第三者納品で問題化する可能性。
- 業界 default (AutoCAD annotative 等) に統合された CAD viewer で見ると text が小さく出る (実印刷で目視きつい)。
- 「scale」 用語の混在 (D-3) を残すと future engineering で混乱継続。

### 残課題 (= 別 ADR / 別タスク)

- viewer Inspector で 1/200 / 1/250 / 1/500 を目視批評する手段の修正 (cad-cp の capture が main session terminal を捕まえる現象、 windows-mcp 経由か viewer 側 AlwaysOnTop 強化が必要)。
- 「公共工事の納品時に JIS 違反域で trouble になった」 incident が起きたら、 この ADR を再評価し、 paper-一定 fix を採用する別 ADR を立てる。

## Alternatives considered

**A1. paper 一定 fix を採用**: 寸法値 path も `unitscale_ *= printscale_` に揃え、 JIS / 国交省 基準 (paper 1.8mm 以上) に整える。 → 却下: user 体感を壊す方向、 user の明示要求なし。

**A2. 全廃 + AutoCAD annotative 互換実装**: TriangleList.getPrintTextScale を annotative scale 機能に置き換え。 → 却下: 大規模変更、 trianglelist のシンプルさを失う、 user の業務に対する benefit が見えない。

**A3. 何もしない (整理候補も skip)**: 触らず維持。 → 却下: dead init / コメント残骸 / 用語混在は 5 年蓄積した技術的負債、 触らないと engineering hygiene が下がる。

## 出典

- yuuji 発話 (2026-06-10): 「だいたい200分の1から250くらいまでが、 実用域かなと思ってるが、 最大で500くらいまでかな」
- yuuji 発話 (2026-06-10): 「観測が全てだ」
- yuuji 発話 (2026-06-10): 「最も多用するのが200分の1から300分の1くらいで、そこで最低限視認性が保たれるサイズにしてる。なぜかっていうと、小さな三角形の場合、寸法値をでかくすると他と接触するっていう本質的な問題があるからだ」
- commit `219cb90` (2021-09-20): "textsize changes to reflect the content"
- commit `dafabbd` (Initial): writer 4 ヶ所の `1f` hardcode 初出
- 業界比較: vault `worker-A` の出典リスト (AutoCAD / Jw_cad / BricsCAD / V-nas / JIS / 国交省 / SXF) を参照
- ADR 形式は Michael Nygard "Documenting Architecture Decisions" (https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions) に倣う、 運用形 (latest/archive 分離) は竹内一真氏/FIXER の ascii.jp 記事「Claude Code の Plan mode をやめてみる」 (2026-06-04) に倣う。
