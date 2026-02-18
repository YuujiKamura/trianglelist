---
description: "CSV→DXF変換ツール（CsvToDxfMain）の仕様・配置ロジック・寸法テキスト制御。CSV、DXF、三角形、変換、求積、配置と言われた時に使用。"
---

# CsvToDxfMain スキル

## 概要
三角形CSVからDXF図面を生成するデスクトップツール。
`desktop/src/main/kotlin/CsvToDxfMain.kt`

## 実行方法
```bash
./gradlew :desktop:csvToDxf --args="path/to/input.csv"
# 出力: 同フォルダに {basename}_triangles.dxf
```

## CSV形式（v2）
```csv
#VERSION,2
#TRIANGULAR
number,lengthA,lengthB,lengthC,parent,connectionType,connectionSide,connectionLCR,name,color
1,6.42,9.1,6.4,-1,-1,0,0,Root,4
2,9.1,8.59,1,1,1,1,0,T2,4
```
- connectionType: -1=独立, 1=親B辺接続, 2=親C辺接続
- connectionSide: 未使用（互換用）
- color: DXF ACIカラー（4=Cyan, 5=Blue, 7=White）

## 三角形配置モデル（app準拠）

### 頂点と辺の対応
- point0(A), pointAB(B), pointBC(C)
- A辺: pointAB→point0（長さa）、B辺: pointBC→pointAB（長さb）、C辺: point0→pointBC（長さc）
- angle: A辺の方向（度数法）

### 接続ロジック
**connType=1（親のB辺に接続）**:
- child.point0 = parent.pointBC（直接コピー）
- child.pointAB = parent.pointAB（直接コピー・再計算しない）
- angle = atan2(ptAB.y - pt0.y, ptAB.x - pt0.x)

**connType=2（親のC辺に接続）**:
- child.point0 = parent.point0（直接コピー）
- child.pointAB = parent.pointBC（直接コピー・再計算しない）
- angle = atan2(ptAB.y - pt0.y, ptAB.x - pt0.x)

**重要**: 子のpointABは親の頂点を直接コピーする。三角関数で再計算すると浮動小数点誤差で共有辺の重複検出が壊れる。

### 第3頂点の計算
```
theta = atan2(point0.y - pointAB.y, point0.x - pointAB.x)
alpha = acos((a² + b² - c²) / (2ab))
pointBC = pointAB + b * (cos(theta+alpha), sin(theta+alpha))
```

### 角度継承
```kotlin
data class PlacedTriangle(...) {
    val angleMpAB: Double get() = angle + angleAB  // connType=1の子に渡す
    val angleMmCA: Double get() = angle - angleCA  // connType=2の子に渡す
}
```

## DXFレイヤー構成

| レイヤー | 内容 | color |
|---------|------|-------|
| TRI | 三角形の辺（LINE） | 7 (White) |
| LEN | 寸法値テキスト（TEXT） | 7 |
| NUM | 番号テキスト + 円 | 5 (Blue) |

## 寸法テキスト制御

### 外側配置（dimVerticalDxf）
- 辺の2頂点と対向頂点の外積で左右判定
- calcDimAngleの方向反転を考慮
- alignV=1(Bottom) or 3(Top) でテキストを三角形の外側に配置

### 鋭角頂点付近のずらし（shiftRatio）
- 内角 < 20° の頂点に隣接する辺は中点から50%ずらす
- `val H = 0.5` で制御

### 共有辺の重複防止
- edgeKey: 頂点座標を3桁フォーマットで正規化→Set管理
- 先に描いた方だけ残す

## DXFヘッダー
- INSUNITS=6（メートル）
- AcDbPlotSettings: A3横（420x297）
- EXTMIN/EXTMAX: 全エンティティのバウンディングボックス × 50倍（用紙マッピング）

## 関連Issue
- #65: CSV→DXF変換ツールの実装と改善
