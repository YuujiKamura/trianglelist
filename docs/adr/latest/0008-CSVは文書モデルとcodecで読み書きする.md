# 0008: CSV は文書モデル (CsvDoc) + codec で読み書きする

- 日付: 2026-06-11
- 状態: 採用 (web 全面適用、アプリのローダ移行は次 ADR)
- 起点: user「CSVロードとか保存のコードが、なんかもう行き当たりばったりで、コンナンでいいのか？
  って思いながら放置してたが、本来どうするのが良いと思う？」「バグが出るってことは、何らかの
  構造化が足りないんだろうけど、発想が出てこない」→ 提案承認「では直してみて。手動配置の
  書き戻しは明らかにユーザー損失になるんでそれもやれ」

## 診断 — 欠けていた構造は「文書」という層

旧来は「ファイル ⇄ 生きた TriangleList の直結」: 1 行読むたびに半構築のリストへ副作用を
順番に当てる。バグが全部「順番」から出ていた (connectionSide が入る前に自動配置が走る
[ADR 0006 付記] / 手動値が後続の add に潰される / recoverState の位置 1 つで 180° 狂う)。

## 決定 — 3 層に分ける (common/datamanager/CsvCodec.kt)

1. **parse: text → CsvDoc** — 純データ。計算・構築をしない。未知の列・行は生のまま保持して
   書き戻す (schema evolution の定石: 位置を再利用しない・未知フィールドは保持、
   出典: dev3lop "Schema Evolution Patterns" / ACCU Overload "Automatic Object Versioning")
2. **build: CsvDoc → TriangleList** — named phases:
   ① 幾何構築 (180° 基底で add、自動配置込み) → ② 手動配置の復元 (**全行 add の後**。
   CsvLoader の行ごと finalize と違い、後続の子の add が先行行の保存値を潰す事故が構造的に
   起きない) → ③ recoverState でリスト回転 (ADR 0007)
3. **bake: TriangleList → CsvDoc (完全形式 28 列)** — アプリの保存 (MainActivity.writeCSV:
   2745-2792) と同列順・同値。serialize で text へ

ファイル形式は変えない (インストールベースに 28 列 Shift-JIS の実ファイルが居る)。
CSV は「バージョン付きレガシー codec」として封じ込め、導出値 (列 22-25 の幾何キャッシュ)
は読み側が無視して再計算する。

## 手動配置の書き戻し (ユーザー損失の解消)

web の W/H フリップ・番号移動は overrides (localStorage) 止まりで、保存 CSV に乗らず
アプリで開くと消えていた。保存ボタンは `buildCsvTextWithOverrides` (overrides を model に
適用 → bake) を通すようにし、列 7-9/11-16/20-21 に焼き込む。WebCsvReader 側の復元
(同日実装) と対で、CSV だけで往復が閉じる (= localStorage 非依存)。

あわせてエンコーディングの app 互換を修正:
- 書き: CSV も DXF/SFC と同じく Shift-JIS で出す (アプリ loadFileWithEncoding の既定)
- 読み: strict UTF-8 で試して化けるバイト列なら Shift-JIS として読み直す
  (旧 readAsText は UTF-8 固定で、アプリの CSV を開くと測点名等が U+FFFD に壊れていた)

## 検証

- CsvCodecTest: parse↔serialize の固定点・未知列/行の保持・build→bake→再build の描画同一・
  overrides の bake 往復 (commonTest 4 本)
- 既存スイート green (common 209/0、app 219/0 1skip)、tsc green
- CP e2e: 寸法フリップ → state.csvBaked の列 16=3 + 列 21=true を実証

## 残課題 (次 ADR)

- アプリの CsvLoader / MainActivity.writeCSV を本 codec に置換 (ADR 0006 決定 2 と同じく
  web で運用実績を積んでから)
- Deduction 行は現状 pre/postLines の素通し (web が控除をサポートする時に rows へ昇格)

## 教訓 (検証作業の事故 2 件、insights #109)

Windows の `curl | python -c "json.load(sys.stdin)"` はパイプが cp932 デコードして
UTF-8 を壊す (改行バイトまで 2 バイト文字に食われて行が glue する)。検証で 2 度
ユーザーの図面を壊した。CP の応答は必ず `curl -o file` + `open(file, encoding='utf-8')`。
