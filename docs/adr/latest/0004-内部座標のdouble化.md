# ADR 0004: 内部座標を Float から Double に移行する

- 日付: 2026-06-10
- 状態: 起草 (yuuji 発話「内部計算にFloatを使うっていうのが、実は初歩的な設計ミスだってのを後から気づいたんだが」2026-06-10)
- 関連: ADR 0003 (寸法オブジェクト)、insights #51 (同値性照合)

## Context

PointXY (`common/.../trilib/PointXY.kt`) の内部は Float (IEEE 754 単精度、有効精度 ≒7 桁)。model 座標 24000mm 級では ULP ≒ 0.002mm で、図面の意味精度 (寸法 0.01m 表示) には足りても、演算の検証・累積に対しては解像度が不足する。

業界標準は double: AutoCAD の内部表現は IEEE 64bit (仮数 52bit、有効 15.95 桁) で 32bit 時代から一貫して 64bit 精度 ([JTB World: Understanding Floating Point Precision in AutoCAD](https://blog.jtbworld.com/2008/04/understanding-floating-point-precision.html)、[Autodesk: Working with Large Coordinates](https://blogs.autodesk.com/autocad/working-large-coordinates-in-autocad/))。DXF の実数も 16 桁精度 ([Scan2CAD: DXF Technical Dissection](https://www.scan2cad.com/blog/dxf/technical-dissection/))。幾何モデルを float で持つ CAD は無く、float の出番は GPU 描画バッファ (表示専用) に限られる。本アプリで Float が選ばれたのは Android Canvas API (drawText/drawLine が Float) の型に editmodel が引っ張られた経緯と推定される。

**Float の代償は既に 3 箇所に刻まれている**:

1. `PointXY.kt` の getter 自体に `adjustForRoundingError(threshold=1e-5)` — 1e-5 未満を 0 に潰す場当たり補正。Float ノイズに過去困った形跡がコードに残っている
2. 当たり判定の EPS を 1e-4 から 1e-2 (mm) に緩めざるを得なかった (rev1、24000mm 級座標では 1e-4 が ULP 未満で偽 intrusion が出た)
3. ADR 0003 Phase 2a の golden diff で末桁差が露出 (`5600.0005` vs `5600.0` — dimpoint キャッシュの累積加算と式の再計算の float 経路差)

## Decision

**A. PointXY の内部を Double 化** (`_x/_y: Double`)。`adjustForRoundingError` は撤去する (Float 時代の補償であり、Double では閾値ごと意味が変わる。撤去で挙動が変わる箇所は検証装置で洗い出す)

**B. 「モデル = Double、描画 = Float」の役割分担** — Android Canvas / Compose DrawScope へ渡す縁でのみ `.toFloat()`。業界 (モデル double、GPU バッファ float) と同じ構図

**C. DXF / SFC writer は Double のまま文字列化** — 精度向上、`5600.0005` 系の累積ノイズは桁が深くなり実害消滅

**D. 実施タイミング: ADR 0003 Phase 2 (消費者切り替え) 完了直後**。理由: parity test (Phase 1) と golden 意味的 diff (Phase 2a) という**数値の同値性を機械検証する装置が今だけ生きている**。Double 化の前後で「図面の意味が変わっていない」を装置で証明できる。装置を使い捨てた後では同じ保証を再構築するコストがかかる

**E. Double 化後に EPS を再検討** — CollisionField の EPS 1e-2mm は Float の妥協なので、Double では 1e-6 級まで締められる (接触/侵入の分解能が上がる)

## 影響範囲 (2026-06-10 計測)

- PointXY 使用: 40 ファイル (app/common/desktop)
- editmodel の Float 宣言・リテラル: 204 箇所
- 型変更はコンパイラが全波及箇所を指すため作業は機械的。判断が要るのは (a) adjustForRoundingError 撤去の影響、(b) 描画縁の toFloat() 挿入位置、(c) テスト期待値の精度

## Consequences

- 当たり判定・配置式・DXF 出力の数値品質が CAD 標準と揃う
- Web 化 (wasmJs) に好都合 — JS の number は元々 double であり、Float 縛りの方が異物だった
- 既存図面への影響: 出力座標の末桁が変わり得る (より正確な方向)。Phase 2a と同じ意味的 diff (許容 1e-3) で「図面の意味不変」を検証して受領する
