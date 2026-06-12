# 0010: 作図 writer の単位モデルと基底集約 (DXF/SFC/PDF/Web)

- 日付: 2026-06-12
- 状態: 段1-3 採用済 / 段4 (単位 flip + 控除集約) は保留 (要判断)
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
- **段4 (保留): SFC を「単位後掛け」流儀へ flip + writeDeduction を基底へ。**
  控除を DXF に揃えるには、控除の円半径・矩形サイズの ×1000 が SFC 全体の単位焼込みの
  一部なので、SFC を実単位 (m) で持ち primitive で ×unitscale する DXF 流儀へ全面 flip する
  必要がある (控除だけの部分 flip は primitive 共有のため不可)。

## 段4 を保留した理由 (引き算判断)

flip は SFC の textscale・trilist/dedlist scale・center・枠 scale・3 primitive まで
綺麗にマッピングできるが、**計算書 (writeCalcSheet) が三角形と別の入れ子スケール**
(`scale=1000` + `textscale/unitscale` + trilist を一旦 m に戻す) で動いており、DXF
(`scale=1` + `textscale`) と単純対応しない。解析だけでは文字高に ×1000 の不確かさが残り、
「出力同値」を一発保証できず golden 駆動の反復が要る。

harm class (c) dev tool、かつ user が「最近観てない」未使用フォーマットの深い入れ子 flip に
反復を投じる価値は、user が決めるコスト (CLAUDE.md「引き算原則」「様々だ」)。段1-3 で
一元化の主要部 (paper・writeTriangle) と安全網は入った。段4 は golden 駆動で実装可能だが
着手は user 判断待ち。

## 検証 (2026-06-12)

- 段1-3 各コミットで app writer 系 (SFC/DXF golden 各 3 + DxfFileWriter 12 + DrawingWriter 3
  + TrilistDim 2) 0 failed、common desktopTest 緑、wasmJs compile OK。
- DXF は段1-3 全てで byte/意味不変。SFC は段2 で縦揃えのみ DXF 収束 (golden 再採取)。

## 関連

- ADR 0009 (DXF 縮尺はペーパー空間ビューポート)。本 ADR の「縮尺は紙の側」は 0009 の続き。
- コミット: 24bb15a (段1)、0092dba (段2)、01ccee6 (段3)。
