# 0007: 図形全体の回転は ListAngle 1 個 + recoverState で実現する

- 日付: 2026-06-11
- 状態: 採用
- 起点: user「概ねコレで三角形の編集機能は揃ったな。控除図形の入力はまだできないが、図形全体の回転ができれば一段落かな」

## アプリの回転の実態 (実コードで確定)

- rot_l/rot_r FAB (fabs.xml 左上横列 [resetView][rot_l][rot_r]) → `fabRotate(±5f)`
  (MainActivity.kt:1394-1400) → `TriangleList.rotate(PointXY(0,0), ±5°)` で全三角形を
  原点回りに回し、`TriangleList.angle` に累積 (TriangleList.kt:33-52)。
- 永続化は CSV の `ListAngle, <角度>` 行 1 つ (MainActivity.kt:2781 で保存、
  CsvLoader.readListParameter:396 で復元)。
- **ListAngle = 三角形 1 の絶対角度**。実 CSV (app/src/test/resources/4.11.csv) で
  ListAngle (-436.37372) と三角形 1 行の角度列 (列22) が完全一致することで裏取り。
- ロード時の適用: CsvLoader.buildTriangle は独立三角形を **180° 基底** で組む
  (CsvLoader.kt:212 `Triangle(..., 180f)`)。その後 `recoverState(PointXY(0,0))`
  (TriangleList.kt:340、MainActivity.setEditLists:2909 から呼ぶ) が **angle − 180** を
  全三角形に回して絶対角度へ戻す。
- 新規図面は角度 0 (MainActivity.createNew:2654 `Triangle(5,5,5, (0,0), 0f)`)。
  つまり 180° 基底 + recoverState(0−180 = −180°) = 新規と同じ向き、で帳尻が合う設計。

## 決定 — web は「listAngle 数値 1 個」だけ持つ

web は毎描画 CSV から全再構築する (ADR 0006) ので、アプリの in-place 回転
(control_rotate の連鎖) は要らない。回転の本体は:

1. **TS**: `listAngle` state 1 個。rot FAB は `listAngle ± 5` して redraw するだけ。
   serializeState が `ListAngle, <角度>` 行を書き、parseCsvToState が読む
   (undo・autosave・保存 CSV にそのまま乗る)。
2. **wasm (WebCsvReader)**: ListAngle 行を `trilist.angle` に入れ、構築後に
   `recoverState(PointXY(0,0))` — アプリのロード経路と同一コード。
3. UI はアプリ fabs.xml と同じ左上横列 [⛶][⟲][⟳]、符号も同じ (左=+5°)。

undo スナップは取らない (アプリも fabRotate では取らない。逆回転で戻せる)。
フロート三角形のみの部分回転 (separationFreeMode、TriangleList.kt:43) は
リスト全体角度に乗らない別系で、本段階のスコープ外。

## 副産物 — web は今まで全 CSV を 180° 逆さに描いていた

recoverState 導入前の web は 180° 基底のまま描画していた。アプリは同じ CSV を
recoverState (angle−180) で回すので、**同一 CSV の表示がアプリと web で 180° ずれていた**
(ListAngle なし CSV: アプリ=0°・web=180°)。本 ADR の適用でこのずれが消え、
新規/サンプル図面は「頂点が上・上に積む」向き (アプリの createNew と同じ) に変わる。

golden への影響:
- DXF/SFC golden (app 採取) と primitives-default golden は、in-test の Triangle 直組み
  (180° 基底・recoverState なし) から採取されたもの → fixture CSV に `ListAngle, 180` を
  明記して採取時の向きを指定、golden 自体は不変。
- primitives-sample golden は web シェル実物の向きを pin するもの → 0° 向きで再採取。

## 根拠 (実コード)

- MainActivity.kt:1394-1400 (fabRotate ±5f) / :2654 (createNew 角度 0) / :2781 (ListAngle 保存) / :2909 (setEditLists → recoverState)
- TriangleList.kt:33-52 (rotate) / :340-346 (recoverState = angle−180)
- CsvLoader.kt:212 (180° 基底) / :396 (ListAngle 読み)
- app/src/test/resources/4.11.csv (ListAngle = 三角形1 の列22 角度、完全一致)
- 検証: WebPrimitiveRendererTest.reader_applies_list_angle_via_recover_state (なし→0° / 180→180° / 90→90° の頂点座標 pin)
