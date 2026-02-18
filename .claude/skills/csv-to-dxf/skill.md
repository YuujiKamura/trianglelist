---
description: "手書き展開図の写真からAIでCSV生成→DXFをワンストップ出力。手書き、展開図、写真、スケッチ、求積、三角形、CSV、DXF、変換、image、画像と言われた時に使用。"
---

# 手書き展開図 → CSV → DXF ワンストップ変換

## ワークフロー
```
手書き展開図（写真/スキャン）
  → Gemini Vision で三角形データ読み取り
  → CSV v2 形式で保存
  → CsvToDxfMain で DXF 生成
  → ターゲットフォルダに出力
```

## Step 1: 手書き画像をGemini Visionに渡す

```bash
echo "@IMAGE_PATH この手書き展開図から三角形データを読み取り、以下のCSV形式で出力せよ。

## ルール
1. 三角形には丸数字で番号が振ってある。その番号順にCSVに記載
2. 各三角形の3辺の長さを読み取る（単位: メートル）
3. 隣接関係を読み取る:
   - 最初の三角形(番号1)は独立: parent=-1, connectionType=-1
   - 以降は親三角形の番号と、どの辺で接続しているかを判定
4. 接続タイプ: 親の辺Bに接続=1、親の辺Cに接続=2
5. 辺の対応: 子のA辺（1列目の辺長）= 親との共有辺の長さ

## 辺の命名規則
- A辺: 親との共有辺（子が親に接するときに一致する辺）
- B辺: A辺の終点から伸びる辺
- C辺: 残りの辺（A辺の始点に戻る）
- 親のB辺 = 親の2列目の辺長の辺
- 親のC辺 = 親の3列目の辺長の辺

## 出力CSV形式（ヘッダー含む、余分な説明不要）
#VERSION,2,,,,,,,,
#HEADER,,,,,,,,,
koujiname,求積図面,,,,,,,,
rosenname,,,,,,,,,
gyousyaname,,,,,,,,,
zumennum,1,,,,,,,,
,,,,,,,,,
#TRIANGULAR,,,,,,,,,
number,lengthA,lengthB,lengthC,parent,connectionType,connectionSide,connectionLCR,name,color
1,辺A,辺B,辺C,-1,-1,0,0,,7
2,辺A,辺B,辺C,親番号,接続タイプ,0,0,,7
...
,,,,,,,,,
#DEDUCTIONS,,,,,,,,,
number,name,type,width,height,x,y,angle,,
,,,,,,,,,
#PARAMETERS,,,,,,,,,
ListAngle,0,,,,,,,,
ListScale,1,,,,,,,,
TextSize,12,,,,,,,," | gemini -m gemini-2.5-pro --yolo -o text
```

### Geminiへのプロンプトの注意点
- **辺A = 共有辺**: 子の辺Aは親との接続辺。長さが親の対応辺と一致すること
- **接続方向の判定**: 手書き図で2つの三角形が共有する辺が、親の辺B(2列目)か辺C(3列目)かを見る
- **分岐**: 1つの親から2つの子が出る場合、一方がconnType=1(B辺)、他方がconnType=2(C辺)
- **色**: デフォルト7。グループ分けしたい場合は4(Cyan), 5(Blue)等

## Step 2: CSVを保存

Geminiの出力をファイルに保存:
```bash
# 出力をCSVファイルに保存
# ターゲットフォルダ例: 工事の実施数量フォルダ
```

## Step 3: CSV → DXF変換

```bash
cd /c/Users/yuuji/StudioProjects/trianglelist
./gradlew :desktop:csvToDxf --args="<CSVファイルパス>"
# → 同フォルダに {basename}_triangles.dxf が生成される
```

## Step 4: 出力先にコピー（必要に応じて）

```bash
cp <生成されたDXF> <ターゲットフォルダ>/
```

## CSV形式の詳細

### v2形式の全列
```
number,lengthA,lengthB,lengthC,parent,connectionType,connectionSide,connectionLCR,name,color
```
- number: 三角形番号（1始まり）
- lengthA/B/C: 辺の長さ（メートル）
- parent: 親の番号（独立=-1）
- connectionType: -1=独立, 1=親B辺接続, 2=親C辺接続
- connectionSide: 0（互換用、未使用）
- connectionLCR: 0（互換用、未使用）
- name: 任意の名前（空でも可）
- color: DXF ACIカラー（7=White/Black, 4=Cyan, 5=Blue）

### 接続の具体例
```
1, 6.42, 9.10, 6.40, -1,-1, 0,0, Root, 4   ← 独立（最初の三角形）
2, 9.10, 8.59, 1.00,  1, 1, 0,0, T2,   4   ← 親1のB辺(9.10)に接続、子のA辺=9.10
3, 6.40, 3.50, 4.00,  1, 2, 0,0, T3,   4   ← 親1のC辺(6.40)に接続、子のA辺=6.40
```

### 分岐パターン
```
T7 ── B辺(3.69) → T10（connType=1）
   └─ C辺(6.98) → T8 （connType=2）
```
T7から2方向に分岐。T10はT7のB辺に、T8はT7のC辺に接続。

## 三角形配置モデル（CsvToDxfMain内部）

### 頂点と辺の対応
```
point0(A) ──A辺(a)── pointAB(B)
   \                    /
   C辺(c)          B辺(b)
     \              /
      pointBC(C)
```

### 接続時の頂点コピー
**connType=1（親B辺接続）**:
- child.point0 = parent.pointBC（直接コピー）
- child.pointAB = parent.pointAB（直接コピー）

**connType=2（親C辺接続）**:
- child.point0 = parent.point0（直接コピー）
- child.pointAB = parent.pointBC（直接コピー）

**重要**: 浮動小数点誤差防止のため、子の頂点は親から直接コピー。再計算しない。

## DXFレイヤー

| レイヤー | 内容 | color |
|---------|------|-------|
| TRI | 三角形の辺（LINE） | 7 |
| LEN | 寸法値テキスト | 7 |
| NUM | 番号テキスト + 円 | 5 |

## トラブルシューティング

### Geminiが辺の対応を間違える場合
- 手書き図に辺A/B/Cのラベルがないと間違えやすい
- 対策: 「子のA辺 = 親との共有辺」を繰り返し強調
- CSVの辺A列が親の対応辺長と一致しているか手動チェック

### 三角形が扇状に広がる場合
- connTypeが全部1（または全部2）になっている可能性
- 分岐点で1と2を使い分けているか確認

### 寸法値が重なる場合
- shiftRatioを調整（CsvToDxfMain.kt の `val H = 0.5`）
- 鋭角閾値を調整（`val SHARP = 20.0`）
