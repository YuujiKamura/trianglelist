# 0010: 作図 writer の単位モデルと基底集約 (DXF/SFC/PDF/Web)

- 日付: 2026-06-12
- 状態: 段1-4 採用済 (単位モデル統一・控除集約まで完了)
- 起点: user「SFC出力は DXF と細かい部分で違いが大きい。基底クラスで全く同じ品質の
  図面を出せるよう一元化したい、DXF を正に揃える」→ 続けて「単位系のマジックナンバーも
  何だかよく理解ってない事の反映で、本来どうあるべきか世の中の流儀に照らして考えてほしい」

## 診断 — マジックナンバーは「3 概念の混線」

`×1000` / `×printscale` / `21000` / `setBox(1000)` 対 `setBox(1.0)` / `unitscale` /
`textscale` といった散在数値は、本来分けるべき 3 つの概念が 1 つに潰れた症状だった。

| 概念 | 本来の姿 | 現状の潰れ方 |
|---|---|---|
| A. 実単位への変換 | mm に一度だけ揃える (m→mm = ×1000)。1 箇所 | `unitscale_=1000` を SFC は座標へ焼込み・DXF は entity で後掛け |
| B. 縮尺 (1/50) | 紙の側だけ (枠寸法・文字高・ビューポート)。図形座標には掛けない | おおむね正しい (図形は実寸、縮尺は枠とビューポート) |
| C. 出力単位の宣言 | ヘッダで 1 回 ($INSUNITS=4=mm、SXF は常に mm) | DXF は宣言済 ✓ |

## 世界標準 (出典)

- モデル空間は実寸 1:1・実単位で描き、縮尺は紙 (ペーパー空間/プロット) で与える
  ([AutoCAD model/paper space](https://autocadtips.com/blog/model-space-vs-paper-space-the-practical-difference/))
- 土木 SXF は明文規定: 「作図単位を mm とした場合、1m のものは 1000mm として定義。
  尺度 1:200 なら出力時の用紙上長さは 5mm」= 座標は実寸 mm、縮尺は出力時に効く
  ([SXF 技術者リファレンスブック 第3章](http://www.ocf.or.jp/sxf/pdf/referencebook/2011/reference_book_03_2011.pdf)、
  [国交省 CAD 製図基準運用ガイドライン](https://www.mlit.go.jp/tec/it/cals/050831/img/05.pdf))
- DXF はヘッダ `$INSUNITS` で単位宣言 (mm=4)
  ([ezdxf DXF Units](https://ezdxf.readthedocs.io/en/stable/concepts/units.html))

## 実態確認 — 出力は既にほぼ揃っている

実際の出力数値を照合した結果 (golden):
- DXF/SFC とも三角形の座標は **完全同一の実寸 mm** (10m → `15500..5500` = 10000mm)。
  「SFC は ×1000 焼込み / DXF は後掛け」は**コードの経路が違うだけで合流先の数値は同じ**。
- 枠は両者とも model 空間で paper×denominator (A3 1/50 で 21000mm)。SFC は加えて
  物理用紙 `420×297` を `drawing_sheet_feature` で宣言 (SXF 用紙座標の作法)。
- DXF ヘッダ `$INSUNITS=4` (mm) 済。

つまりモデルは既に世界標準 (実寸 mm + 縮尺はビューポート=ADR 0009 + mm 宣言) にほぼ乗って
いる。残る差は (a) 控除の旗線長式 (DXF `len*ts+0.3` 対 SFC `len*ts*0.7`)・テキストオフセット、
(b) SFC 文字の ×1.2、(c) 寸法/測点の縦揃え符号、の 3 点に絞られていた。

## 決定 — DXF の「単位後掛け」を北極星に、段階集約

writer は 4 つ (DxfFileWriter / SfcWriter / PdfWriter / WebFrame、いずれも基底
DrawingFileWriter)。PDF は Android Canvas で別世界、Web は枠のみで既に基底へ委譲済。
本丸は SFC↔DXF。

- **段1 (済): 用紙寸法を `paper` 一点に集約。** paperWcm/Hcm/Name を派生 getter 化し、
  DXF の手動コピーと SFC の死んだ size override を削除。A3 byte 不変。(commit 24bb15a)
- **段2 (済): writeTriangle + 寸法ヘルパーを基底へ。** DXF 版を正として移植。DXF byte 不変、
  SFC は寸法/測点の縦揃えが verticalDxf に収束 (text align 8→2、座標不変)。(commit 0092dba)
- **段3 (済): 控除入り golden を DXF/SFC に追加 (安全網)。** writeDeduction を触る前に
  現挙動を pin (円半径 115・矩形・旗線・情報テキスト)。(commit 01ccee6)
- **段4 (済): SFC を「単位後掛け」流儀へ flip + writeDeduction/writeDedRect を基底へ。**
  (commit 後述)。user 確定「双方の食い違いに前向きな意図は一切ない、全部 DXF に揃えていい」
  を受け、SFC 側の偶発的な澱を保存せず DXF クリーンロジックへ収束。
  - SFC を「モデル座標は実寸、mm 変換は primitive で ×unitscale、縮尺は枠の scale 引数だけ」の
    DXF 流儀へ flip: textscale=getPrintTextScale (旧 ×1.2 ×unitscale 廃止)、trilist は scale せず、
    dedlist は 1/viewscale のみ、center=paperWcm/2×ps、frame=writeDrawingFrame(printscale, textscale)、
    calcSheet=writeCalcSheet(1f, textscale)、primitive 3 種 (writeLine/writeCircle/writeTextA9) で ×unitscale。
  - writeDeduction / writeDedRect を基底へ移動 (DXF をそのまま)。DXF は継承で byte 不変。
    SFC は writeTextAndLine を DXF ロジック (オフセット textsize/textsize*0.2、RED) に置換、
    旧 (200,100) 固定オフセット・旗線長 ×0.7・circle×1000/setBox(1000) を廃止。
  - textscale 外部上書きを統一: MainActivity.saveSFC の `textSize*20` (ADR 0001 の SFC 専用 JIS 較正)
    と WebDrawingExport.buildSfcText の `500` 直値を撤去、内部 getPrintTextScale (=DXF) に。

## 段4 の効果 — SFC が DXF と同質に

flip 前後の SFC golden 差分は全て「偶発的な澱が DXF 値に直った」もの:
- 三角形座標は不変 (10500 等)。寸法文字 500→250 (=DXF getPrintTextScale×unitscale)。
  枠文字 175→125 (=DXF、旧 175 は ×0.35 由来)。
- **計算書のバグ修正**: 旧 SFC は文字高 0.5・列間隔 3 で全列が重なって読めなかった
  (22500/22503/22506/22509)。flip で文字高 250・列間隔 1500 の正常表示に
  (22500/24000/25500/27000) = DXF と同一。
- 控除は基底 (DXF) ロジックに収束: 円半径 115 (=lengthX/2×unitscale)、Box は setBox(1.0)、
  旗テキストオフセット DXF 式。

## 段5 (済) — 図面上中央タイトルの欠落 + writeTopTitle の textsize 規約統一

user 指摘 (2026-06-12 Pages 目視)「SFC だと図面上部センターのタイトルが無い。タイトル枠
テーブルのテキストサイズもまだ食い違う」。実コードで切り分けた:

- **欠落の根因**: 上中央タイトル `writeTopTitle` (図面名+路線名+下線) は DxfFileWriter:225 だけが
  呼び、SfcWriter は呼んでいなかった → SFC だけ丸ごと欠落。SfcWriter.writeEntities に
  `writeTopTitle(printscale_, textscale_)` を追加 (枠と同じ scale 規約)。
- **サイズ食い違いの根因**: `writeTopTitle` は文字高に生の `textsize` を渡していた。
  `writeDrawingFrame` は `frameTextSize = textsize*scale` で渡すのに、`writeTopTitle` だけ
  ×scale が無い。DXF は scale=1f (printscale は unitscale 側) なので顕在化しないが、SFC は
  scale=printscale_・unitscale=1000 据え置きなので上中央タイトルだけ printscale 分小さくなる。
  → `writeTopTitle` を `titleTextSize = textsize*scale` に統一 (writeDrawingFrame と同規約)。
  DXF は scale=1f なので byte 不変、SFC は枠内テキストと同じ実効サイズに収束。
- **検証 (現物照合)**: DXF golden と SFC golden の全テキストを座標で突き合わせ、
  「同一座標でサイズが異なるテキスト = 0 件」を確認 (サイズは 125/250 の 2 値に両者収束)。
  上中央タイトル 2 本が DXF と同一座標 (10500,13000)/(10500,13550) に追加されたことも確認。

## 段A (済) — DrawPrim による frontend/backend 分離の foundation

user 指摘「Kotlin の機能で『何を何処に描く』を 1 か所で管理できる気がする。今後形式を増やしたり
調整しやすい形にしたい」。外部の定石 ([ezdxf](https://ezdxf.readthedocs.io/en/stable/addons/drawing.html)
の frontend/backend、[retained mode](https://grokipedia.com/page/Retained_mode)) に倣い、Kotlin の
[sealed class](https://kotlinlang.org/docs/sealed-classes.html) で描画プリミティブをデータ化する第一歩を入れた。

- **`DrawPrim` (sealed interface)**: Line / Rect / Circle / Text。実寸座標 + スタイルだけのデータで
  形式非依存 (`DrawPrim.kt` 新規)。
- **`drawScene(prims)` (基底)**: prim を 1:1 で既存プリミティブ (writeLine/writeRect/writeCircle/
  writeTextHV) へディスパッチ。sealed の網羅 when なので prim 追加漏れはコンパイルエラーで止まる。
- **図枠の frontend 化**: `writeOuterFrame` / `writeTopTitle` / `writeDrawingFrame` を「prim の
  リストを組んで drawScene に流す」形へ。これで「何を何処に」が純粋なデータとして 1 か所に集まる。
  旧 `writeTextWithKaigyou` / `splitAndWriteText` は `kaigyouPrims` (DrawPrim.Text を返す純粋版) に
  置換し削除 (分割ロジックの 2 重持ちを解消)。
- **バイト不変の根拠**: drawScene は同じプリミティブを同じ順序で呼ぶだけなので、リスト化しても
  出力は不変。DXF golden (byte) / SFC golden (semantic) / DxfFileWriterTest が**再生成なしで pass**、
  common desktopTest・wasmJs compile も green。
- **次段 (段B、任意)**: writeTriangle / writeDeduction / writeCalcSheet も prim 化すれば「何を何処に」
  が完全に 1 か所のデータになり、形式追加が backend 1 個で済む。ただし**現状バグは無い** (位置ズレは
  誤検出だった、下記)。段B はバグ修正ではなく純粋なアーキ整理なので、必要が生じた時 (新形式追加・
  大きな調整) に着手すれば十分。class (c) dev tool に予防的多層化を積まない「引き算」判断。

## 段5 の「位置オフセット」は誤検出だった (撤回)

段5 で「DXF と SFC で寸法値・計算書テキストの座標が 7 本ズレている」と記録したが、これは
**照合に使ったアドホックな Python パーサのバグによる幽霊**だった。信頼できる方法で数え直すと:

- DXF golden の TEXT 実体 = **43 本** (`awk 'prev=="0"' | grep '^TEXT$'` で確定)
- SFC golden の text_string_feature = **43 本** (行単位カウントで確定)
- **数が一致**。レイアウトは基底の共有コード (writeTriangle / writeTextDimension / writeCalcSheet) で
  両形式同一、単位モデルも段4で統一済み、golden も両者 pass。

パーサの誤りは 2 つ: (a) SFC 側は `text_string_feature\(.*?\)` の非貪欲マッチが本文中の括弧
(`1/50 (A3)`・`(m)`・`(m2)`) で途中で切れ 6 本を取りこぼした、(b) DXF 側は整列付き TEXT の
座標 (code 11/21) と code 40 の状態追跡を誤り 7 本を取りこぼした。両側の取りこぼしが
「片方にしか無いテキスト」という偽の差を生んだ。**実在の位置ズレは無い** (撤回)。
- **旗揚げを DXF DIMENSION 実体へ**: user 指摘「DXF の旗揚げも本来 Dimensions で描かれるべき」。
  現状は手書き LINE+TEXT。将来 DIMENSION エンティティ化する案 (本 ADR スコープ外)。
- ADR 0001 の SFC 専用 textscale 較正 (textSize*20) は本 ADR 段4 で廃止 (DXF 内部値に統一)。

## 管理方法 (一元化の現状)

図面枠・表題欄・上中央タイトルの**座標・文字高・レイアウト定数は全て基底 DrawingFileWriter の
`writeDrawingFrame` / `writeTopTitle` / `writeOuterFrame` に 1 か所だけある** (single source)。
DXF/SFC の各 writer はこれらを呼ぶだけで、独自の枠描画は持たない。両者の違いは「縮尺をどこに乗せるか」
の 1 点 (DXF=unitscale に畳む scale=1f、SFC=scale 引数=printscale_) に集約され、基底メソッド側は
`textsize*scale` 規約で両者を同じ実効値に落とす。よって枠・タイトルの文字や配置を変えたいときは
基底メソッド 1 か所を直せば DXF/SFC 両方に反映される。

## 検証 (2026-06-12)

- 段1-3 各コミットで app writer 系 (SFC/DXF golden 各 3 + DxfFileWriter 12 + DrawingWriter 3
  + TrilistDim 2) 0 failed、common desktopTest 緑、wasmJs compile OK。
- DXF は段1-3 全てで byte/意味不変。SFC は段2 で縦揃えのみ DXF 収束 (golden 再採取)。

## 関連

- ADR 0009 (DXF 縮尺はペーパー空間ビューポート)。本 ADR の「縮尺は紙の側」は 0009 の続き。
- コミット: 24bb15a (段1)、0092dba (段2)、01ccee6 (段3)、9392971 (本 ADR 初版)、段4 (後続)。
