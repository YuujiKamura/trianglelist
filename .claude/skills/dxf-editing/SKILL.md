# DXFファイル編集スキル

DXFファイルにエンティティを追加・編集する際の注意点と手順。

## エンティティハンドル（重要）

DXFファイル内の各エンティティは一意のハンドル（グループコード5）を持つ。

```
  5
82
```

### ハンドルのルール
- **16進数形式**で指定（例: 82, 1A, 400, FFFF）
- **既存ハンドルと重複禁止** - 重複するとビューワがクラッシュする
- 新規追加時は既存の最大値より大きい値を使う

### ハンドル確認方法
```python
# 既存ハンドルの最大値を確認
handles = []
for i, line in enumerate(lines):
    if line.strip() == '5' and i + 1 < len(lines):
        handle = lines[i+1].strip()
        try:
            val = int(handle, 16)
            handles.append(val)
        except:
            pass
max_handle = max(handles)  # 例: 348 (10進で840)
# 新規は 400 以上を使う
```

## セクション構造

DXFファイルのセクション構造:
```
SECTION HEADER    - ファイル情報
SECTION CLASSES   - クラス定義
SECTION TABLES    - テーブル（レイヤー、スタイル等）
SECTION BLOCKS    - ブロック定義
SECTION ENTITIES  - 図形エンティティ（LINE, TEXT, LWPOLYLINE等）
SECTION OBJECTS   - オブジェクト
EOF
```

### エンティティ追加位置
- **ENTITIESセクションのENDSEC直前**に追加
- OBJECTSセクションに追加しても表示されない

```python
# ENTITIESセクションの終了位置を探す
for i, line in enumerate(lines):
    if line.strip() == 'ENTITIES':
        ent_start = i
    if ent_start and line.strip() == 'ENDSEC':
        ent_end = i  # ここの直前に挿入
        break
```

## TEXTエンティティの構造

```
  0
TEXT
  5
400              <- ハンドル（16進数、一意）
330
1F               <- オーナーハンドル
100
AcDbEntity
  8
0                <- レイヤー名
 62
     5           <- 色番号
100              <- この行を消さないこと！
AcDbText
 10
55000            <- X座標
 20
136000           <- Y座標
 30
0.0              <- Z座標
 40
1994.3           <- テキスト高さ
  1
HELLO            <- テキスト内容
 50
270.0            <- 回転角度
 41
0.892            <- 幅係数
  7
STYLE1           <- テキストスタイル
 72
     1           <- 水平配置
 11
55000            <- 配置点X
 21
136000           <- 配置点Y
 31
0.0              <- 配置点Z
100
AcDbText
 73
     1           <- 垂直配置
```

### 重要な注意点
- グループコード`100`の行（AcDbEntity, AcDbText）は必須
- エンティティをコピーする際、ハンドル（5の値）だけを変更し、100の行を誤って変更しないこと
- 座標は10/20（挿入点）と11/21（配置点）の両方を変更

## エンティティ複製の正しい方法

```python
entity = lines[start:end]  # 既存エンティティをコピー
new_entity = []
handle_done = False

i = 0
while i < len(entity):
    line = entity[i]
    stripped = line.strip()

    # ハンドルは最初の1回だけ変更
    if stripped == '5' and not handle_done:
        new_entity.append(line)
        new_entity.append('400\n')  # 新しいハンドル
        handle_done = True
        i += 2
        continue

    # 座標変更
    if stripped in ['10', '11']:  # X座標
        new_entity.append(line)
        new_entity.append(f'{new_x}\n')
        i += 2
        continue

    if stripped in ['20', '21']:  # Y座標
        new_entity.append(line)
        new_entity.append(f'{new_y}\n')
        i += 2
        continue

    # テキスト内容変更
    if stripped == '1' and entity[i+1].strip() == '元のテキスト':
        new_entity.append(line)
        new_entity.append('新しいテキスト\n')
        i += 2
        continue

    new_entity.append(line)
    i += 1
```

## LWPOLYLINEエンティティの構造

```
  0
LWPOLYLINE
  5
401              <- ハンドル
330
1F
100
AcDbEntity
  8
道路標示          <- レイヤー
100
AcDbPolyline
 90
     4           <- 頂点数
 70
     1           <- 閉じたポリライン
 10
50000            <- 頂点1 X
 20
130000           <- 頂点1 Y
 10
50000            <- 頂点2 X
 20
135000           <- 頂点2 Y
...
```

## 道路標示の図形

### 配置の基本ルール

**1. 中心線基準の位置決め**
- 道路標示は**道路中心線（center_y）を基準**に配置
- 測点（No.0, No.1+5など）のX座標と中心線Yで位置を特定
- 例: No.3+5 → X = No.3のX + 5000mm, Y = center_y

**2. 図形と旗揚げは必ずセット**
道路標示を追加する際は、**図形単体ではなく旗揚げ（引出線+テキスト注釈）とセット**で追加する。

```
      図形（T字マーク等）
          │
          └─────────→ 引出線（LINE）
                          │
                      テキスト（TEXT）
                      「No.3+5? ～ W=30cm 交差点マーク L=3m」
```

**旗揚げの構成要素**:
- **LINE**: 図形から斜め下へ伸びる引出線
- **TEXT**: 標示の詳細情報
  - 測点（No.3+5? ～）
  - 幅（W=30cm）
  - 標示名（交差点マーク）
  - 長さ（L=3m）

**セットで追加するエンティティ**:
1. 図形本体（LWPOLYLINE×2など）
2. 引出線（LINE）
3. 注釈テキスト（TEXT）

### 交差点マークT字

T字型の道路標示。**2つの閉じたLWPOLYLINE（矩形）で構成**。
線（LINE）ではなく、塗りつぶし用の閉じた矩形で描く。

```
    ┌─────────────────────┐  ← 横棒 (2300mm x 300mm)
    │                     │
    └─────────┬───────────┘
              │
              │  ← 縦棒 (300mm x 1000mm)
              │
              └─┘
```

**サイズ**:
- 横棒: 幅2300mm x 高さ300mm
- 縦棒: 幅300mm x 高さ1000mm
- 縦棒は横棒の中央下から伸びる

**LWPOLYLINEの属性**:
```
  0
LWPOLYLINE
  5
17D              <- ハンドル
330
1F
100
AcDbEntity
  8
補助線            <- レイヤー
  6
Continuous       <- 線種
 62
     3           <- 色（3=緑）
370
    -3           <- 線の太さ
100
AcDbPolyline
 90
     4           <- 頂点数（矩形=4）
 70
     1           <- 閉じたポリライン（重要！）
 43
0.0
 10              <- 頂点1 X
113571.552
 20              <- 頂点1 Y
117416.064
 10              <- 頂点2 X
115871.552
 20              <- 頂点2 Y
117416.064
 10              <- 頂点3 X
115871.552
 20              <- 頂点3 Y
117716.064
 10              <- 頂点4 X
113571.552
 20              <- 頂点4 Y
117716.064
```

**生成例（Python）**:
```python
def create_t_mark(center_x, center_y, handle_base):
    """交差点マークT字を生成"""
    # サイズ
    h_width = 2300   # 横棒の幅
    h_height = 300   # 横棒の高さ
    v_width = 300    # 縦棒の幅
    v_height = 1000  # 縦棒の高さ

    # 横棒の座標（中央上部）
    h_left = center_x - h_width / 2
    h_right = center_x + h_width / 2
    h_bottom = center_y
    h_top = center_y + h_height

    # 縦棒の座標（中央から下へ）
    v_left = center_x - v_width / 2
    v_right = center_x + v_width / 2
    v_top = center_y
    v_bottom = center_y - v_height

    # 横棒のLWPOLYLINE
    horizontal = f"""  0
LWPOLYLINE
  5
{handle_base:X}
330
1F
100
AcDbEntity
  8
補助線
  6
Continuous
 62
     3
370
    -3
100
AcDbPolyline
 90
     4
 70
     1
 43
0.0
 10
{h_left}
 20
{h_bottom}
 10
{h_right}
 20
{h_bottom}
 10
{h_right}
 20
{h_top}
 10
{h_left}
 20
{h_top}
"""

    # 縦棒のLWPOLYLINE
    vertical = f"""  0
LWPOLYLINE
  5
{handle_base + 1:X}
330
1F
100
AcDbEntity
  8
補助線
  6
Continuous
 62
     3
370
    -3
100
AcDbPolyline
 90
     4
 70
     1
 43
0.0
 10
{v_left}
 20
{v_top}
 10
{v_right}
 20
{v_top}
 10
{v_right}
 20
{v_bottom}
 10
{v_left}
 20
{v_bottom}
"""
    return horizontal + vertical
```

### ダイヤマーク（横断歩道予告）

9頂点のLWPOLYLINEで構成されるひし形。
サイズ: 5000mm x 600mm

## 測点キャッシュの活用

大きなDXFファイルを毎回全部読むのは非効率。測点情報をJSONにキャッシュしておく:

```json
{
  "dxf_path": "path/to/file.dxf",
  "stations": {
    "No.0": {"x": 50114, "y": 132624},
    "No.1": {"x": 70114, "y": 132624},
    ...
  },
  "sections": {
    "ENTITIES": {"start": 2117, "end": 389572}
  },
  "center_y": 132624
}
```

## トラブルシューティング

### エンティティが表示されない
1. ENTITIESセクション内に追加されているか確認
2. ハンドルが16進数形式か確認
3. ハンドルが重複していないか確認
4. `100`の行（AcDbEntity, AcDbText等）が存在するか確認

### ビューワがクラッシュする
- ハンドルの重複が最も多い原因
- セクション構造の破損（ENDSECの欠落等）

## CADビューワ（Kotlin/Compose Desktop）

### 起動方法
```bash
cd trianglelist
./gradlew :desktop:run --args='"path/to/file.dxf"'
```

### 機能
- **ホットリロード**: ファイル変更を1秒ごとに監視、自動更新
- **ビューステート保存**: パン・ズーム位置を自動保存・復元
- **保存タイミング**: アプリ終了時、ファイル切り替え時のみ（操作ごとではない）
- **保存場所**: `~/.cadviewer/view_states.properties`

### 主要ファイル
- `desktop/src/main/kotlin/Main.kt` - エントリポイント、ホットリロード
- `desktop/src/main/kotlin/com/jpaver/trianglelist/cadview/CADView.kt` - 描画
- `desktop/src/main/kotlin/com/jpaver/trianglelist/cadview/ViewStateManager.kt` - ステート保存

### ビューステート保存の実装パターン
```kotlin
// 現在のステートを追跡（保存はしない）
var currentScale by remember { mutableStateOf<Float?>(null) }
var currentOffset by remember { mutableStateOf<Offset?>(null) }

// コールバックで更新のみ
onViewStateChanged = { scale, offset ->
    currentScale = scale
    currentOffset = offset
}

// 保存タイミング: DisposableEffect（破棄時）とファイル切り替え時
DisposableEffect(Unit) {
    onDispose { saveCurrentViewState() }
}
```

## 横断歩道（Crosswalk）

### 構成
- **ストライプ**: 道路を横断する方向に延びる白い帯
- **配置**: センターラインを軸に左右対称
  - 例: 7本の場合 → 左3本 + 中央1本 + 右3本

### 仕様（標準）
- ストライプ幅: 450mm（道路方向）
- ストライプ長さ: 4000mm（横断方向、センターライン軸に±2000mm）
- 本数: 7本
- 間隔: 450mm

### 図解
```
    道路方向 →

    ┌────────────────────────────────────┐ ← ストライプ（4000mm長、450mm幅）
    └────────────────────────────────────┘
              450mm間隔
    ┌────────────────────────────────────┐
    └────────────────────────────────────┘
              ...
    ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  ← センターライン
              ...
    ┌────────────────────────────────────┐
    └────────────────────────────────────┘
              450mm間隔
    ┌────────────────────────────────────┐
    └────────────────────────────────────┘

    ←──────── 7本 × 450mm + 6間隔 × 450mm = 5850mm ────────→
```

### CrosswalkGenerator（Kotlin）

```kotlin
val generator = CrosswalkGenerator()

// 中心線でフィルタリング
val centerlines = generator.filterCenterlinesByLayer(dxfResult.lines, "中心線")

// 横断歩道生成
val crosswalkLines = generator.generateCrosswalk(
    centerlineLines = centerlines,
    startOffset = 11000.0,    // 測点オフセット（mm）
    stripeLength = 4000.0,    // 横断方向の長さ（mm）
    stripeWidth = 450.0,      // 道路方向の幅（mm）
    stripeCount = 7,          // 本数
    stripeSpacing = 450.0,    // 間隔（mm）
    layer = "横断歩道"
)
```

### DXFへの直接追加（Python）

```python
center_x = 120721.55  # センターライン上のX座標
center_y = 117566.06  # センターラインのY座標

stripe_length = 4000.0   # X方向（道幅）
stripe_width = 450.0     # Y方向（ストライプ幅）
stripe_count = 7
stripe_spacing = 450.0

half_length = stripe_length / 2
total_width = stripe_count * stripe_width + (stripe_count - 1) * stripe_spacing
half_total = total_width / 2

lines = []
for i in range(stripe_count):
    # センターから左右対称に配置
    stripe_bottom = center_y - half_total + i * (stripe_width + stripe_spacing)
    
    x1 = center_x - half_length
    x2 = center_x + half_length
    y1 = stripe_bottom
    y2 = stripe_bottom + stripe_width
    
    # 矩形の4辺
    lines.append((x1, y1, x2, y1))  # 下辺
    lines.append((x2, y1, x2, y2))  # 右辺
    lines.append((x2, y2, x1, y2))  # 上辺
    lines.append((x1, y2, x1, y1))  # 左辺

# DXFエンティティ生成
for idx, (x1, y1, x2, y2) in enumerate(lines):
    entity = f"""  0
LINE
  5
CW{idx:02X}
100
AcDbEntity
  8
横断歩道
 62
     7
100
AcDbLine
 10
{x1:.6f}
 20
{y1:.6f}
 30
0.0
 11
{x2:.6f}
 21
{y2:.6f}
 31
0.0"""
```

### 注意点
- **センターライン軸**: ストライプはセンターラインを中心に左右対称に配置
- **道路方向**: ストライプは道路を横断する方向に延びる（道路と垂直）
- **総幅計算**: 7本×450mm + 6間隔×450mm = 5850mm

## 自然言語による区画線配置

### 概要
「左車線の真ん中にダイヤ」のような自然言語指示から区画線を配置するシステム。

### 幅員データソース
スプレッドシートで幅員を管理:
```
https://docs.google.com/spreadsheets/d/1pCAAepgnAdenGtpi6IEtNYC53F6KvQQKHP2kcV4SASU
```

**シート1（面積計算書）**:
| 測点 | 左幅員(mm) | 右幅員(mm) |
|------|-----------|-----------|
| No.3+11 | 3150 | 3550 |
| No.4 | 3150 | 3350 |
| No.5 | 3150 | 3200 |

**区画線調査シート**:
| 種類 | 起点 | 終点 | 延長 | 本数 | 備考 |
|------|------|------|------|------|------|
| 横断歩道 | No.3+11 | No.3+15 | 4m | 7 | 施5号 W=45cm |
| ダイヤマーク | - | - | - | 2 | 5000×1500mm |

### 車線中央の位置計算

**実務的なルール（キリの良い数字）**:
- 左車線中央 = 中心線Y + **1500mm**
- 右車線中央 = 中心線Y - **1500mm**

※ 厳密計算（左幅員3150mm÷2=1575mm）ではなく、実務的に1500mmで統一

### 位置指定の例

| 指示 | X座標 | Y座標 |
|------|-------|-------|
| No.3+11 左車線中央 | 120721 | center_y + 1500 |
| No.4 右車線中央 | 129721 | center_y - 1500 |
| No.5 中心線上 | 149721 | center_y |

### コマンド変換フロー

```
自然言語: "No.3+11から4m先まで横断歩道"
    ↓
1. DXFから測点座標取得: No.3+11 → X=120721
2. 起点=120721, 終点=120721+4000=124721
3. 横断歩道エンティティ生成
```

```
自然言語: "No.5 左車線中央にダイヤマーク"
    ↓
1. DXFから測点座標取得: No.5 → X=149721
2. シートから左幅員取得: 3150mm（参考）
3. 配置位置: X=149721, Y=center_y+1500
4. ダイヤマーク（5000×1500mm）生成
```

## 既存エンティティの修正

### 座標シフト（横断歩道の例）

横断歩道が測点を中央にしている場合、測点を起点に修正:

```kotlin
// DxfCrosswalkFixer.kt
val shiftX = 2000.0  // +2000mm シフト

// グループコード10（X座標）を探す
if (line == "10" && i + 1 < lines.size) {
    val xValue = lines[i + 1].trim().toDoubleOrNull()
    if (xValue != null && xValue >= 118721.0 - 1 && xValue <= 122721.0 + 1) {
        // レイヤー確認（路面標示 or 横断）
        var isInCrosswalk = false
        for (j in maxOf(0, i - 50)..minOf(lines.size - 1, i + 10)) {
            if (lines[j].trim() == "8" && j + 1 < lines.size) {
                val layerName = lines[j + 1].trim()
                if (layerName.contains("路面標示") || layerName.contains("横断")) {
                    isInCrosswalk = true
                    break
                }
            }
        }

        if (isInCrosswalk) {
            val newX = xValue + shiftX
            lines[i + 1] = newX.toString()
        }
    }
}
```

### Gradleタスク
```bash
# 横断歩道修正ツール実行
./gradlew :desktop:fixCrosswalk
```

### 修正前後の確認
```bash
# DXF分析ツールで座標確認
./gradlew :desktop:analyzeDxf
```

修正例:
- 修正前: X=118721~122721（測点を中央）
- 修正後: X=120721~124721（測点を起点に4m）

## DXF区画線→シート同期

DXF内の区画線情報をGoogle Sheetsに自動同期するシステム。

### スクリプト
```
cursor_tools/scripts/sync_dxf_markings_to_sheet.py
```

### 使用方法
```bash
python sync_dxf_markings_to_sheet.py [dxf_path]
# デフォルト: H:/マイドライブ/.../面積展開図_南千反畑町第１号線.dxf
```

### 抽出対象
TEXTエンティティから以下の区画線タイプを検出:

| 種類 | 検出キーワード | 備考 |
|------|--------------|------|
| 横断歩道 | 路面標示レイヤーのLINE | 施5号 W=45cm |
| 中央線 | 中央線 | W=15cm 白 |
| 車線分離線 | 車線分離線 | W=15cm 白 |
| 右折・左折標示 | 右折標示, 左折標示 | W=15cm換算 矢印 |
| ダイヤマーク | ダイヤマーク, 横断歩道予告 | 5000×1500mm |
| 交差点マーク | 交差点マーク | W=30cm T字型 |
| 停止線 | 停止線 | W=45cm 白 |
| 停止禁止文字 | 停止禁止 | W=15cm 白 |
| 停車禁止枠 | 停車禁止枠 | W=15cm 白 実線 |
| 停車禁止ゼブラ | 停車禁止ゼブラ, ゼブラ+停車 | W=15cm 白 |

### 測点範囲の関連付け

DXF内では測点範囲と区画線説明が**別々のTEXTエンティティ**として配置されている。
同じX座標で、Y座標が説明テキストの約2600mm上にある測点範囲テキストを関連付ける。

```
テキスト配置パターン（Y座標で並べ替え）:

X=61021, Y=145000 | No.0 ～ No.2+15 ?         ← 測点範囲（上）
X=61021, Y=142375 | 中央線 白 W=15cm L=55m   ← 説明（約2600mm下）
X=61021, Y=139954 | No.0+2 ～ No.1+12 ?       ← 次の測点範囲
X=61021, Y=137329 | 車線分離線 白 W=15cm L=30m ← 次の説明
```

### 測点範囲検索ロジック

```python
def find_station_range_above(x, y, all_texts):
    """同じX座標（±500mm）、Y座標が上（+1000〜+5000mm）のテキストを検索"""
    for tx, ty, text in all_texts:
        if abs(tx - x) < 500 and 1000 < (ty - y) < 5000:
            # "No.X ～ No.Y" パターンを検索
            range_match = re.search(r'No\.[\d+.]+\s*[～~]\s*No\.[\d+.]+', text)
            if range_match:
                parts = re.findall(r'No\.[\d+.]+', range_match.group(0))
                return parts[0], parts[1]  # start, end
    return '', ''
```

### 同期先シート
```
スプレッドシートID: 1pCAAepgnAdenGtpi6IEtNYC53F6KvQQKHP2kcV4SASU
シート名: 区画線調査
```

### 出力形式
| No | 種類 | 起点 | 車線 | 終点 | 延長(m) | 本数 | 備考 |
|----|------|------|------|------|---------|------|------|
| 1 | 横断歩道 | No.3+11 | | No.3+15 | 4 | 7 | 施5号 W=45cm |
| 2 | 停止線 | No.0+2 | 左車線 | | 3 | 1 | W=45cm 白 |
| 3 | 車線分離線 | No.0+2 | 右車線右折/右車線第一 | No.2 | 45 | 1 | W=15cm 白 |

### Kotlinツール（代替）
```bash
./gradlew :desktop:extractMarkings
```
- `DxfMarkingSyncTool.kt`: Kotlin版の区画線抽出ツール
- Python版より機能は限定的（横断歩道のみ）

## 区画線の角度仕様

区画線の「幅」「長さ」は、道路中心線方向に対する角度で定義する。

### 角度の基準

- **0°**: 中心線方向と平行
- **90°**: 中心線方向と垂直（道路を横断）
- **45°**: ゼブラパターン用

### 区画線タイプ別の角度

| 区画線 | 寸法例 | 角度 | 備考 |
|--------|--------|------|------|
| 中央線 | W=150mm | 0° | 道路方向に延びる |
| 車線分離線 | W=150mm | 0° | 道路方向に延びる |
| ダイヤマーク | 5000×1500mm | 0° | 長辺が道路方向 |
| 矢印 | - | 0° | 進行方向に向く |
| 横断歩道 | W=450mm | 0° | ストライプが道路方向 |
| 交差点マーク(横棒) | 2300mm | 0° | T字の横部分 |
| 交差点マーク(縦棒) | 1000mm | 90° | T字の縦部分 |
| **停止線** | W=450mm L=3000mm | **90°** | 道路を横断 |
| ゼブラ | - | 45° | 斜線パターン |

### 停止線の例

```
角度0°（間違い）:        角度90°（正しい）:
   ←道路方向→              ←道路方向→
   ████████               │
   ████████               │ 3000mm
   （道路方向に            │
    伸びてしまう）         └─┘450mm
```

### 道路曲線への対応

道路が図面内で曲がっている場合でも、処理は単純:

1. 該当測点区間の中心線LINEの角度(θ)を取得
2. 標示の相対角度を加算
3. 合計角度で描画

```
例: No.3+5 に停止線を配置
  - No.3〜No.4 区間の中心線角度: θ = 20°
  - 停止線の相対角度: 90°
  - 描画角度: 20° + 90° = 110°
```

各区間は直線とみなし、その区間の中心線角度を参照するだけ。
複雑な曲線追従計算は不要。

### 中心線角度の取得方法

```python
import math

def get_centerline_angle(x: float, centerlines: list) -> float:
    """該当X座標付近の中心線LINEから角度を取得"""
    for line in centerlines:
        if line['x1'] <= x <= line['x2'] or line['x2'] <= x <= line['x1']:
            dx = line['x2'] - line['x1']
            dy = line['y2'] - line['y1']
            return math.degrees(math.atan2(dy, dx))
    return 0.0  # デフォルト
```

## 車線指定の仕様

### 基本構造

シートに「車線」列を追加し、区画線の配置位置を指定する。

| No | 種類 | 起点 | 車線 | 終点 | 延長(m) | 本数 | 備考 |
|----|------|------|------|------|---------|------|------|

### 単純な2車線道路

```
─────────────────────── 道路端（左）
    【左車線】← 対向車線
═══════════════════════ 中央線
    【右車線】← 自車線
─────────────────────── 道路端（右）
```

| 車線指定 | 意味 | Y座標 |
|---------|------|-------|
| 左車線 | 左車線（対向） | CENTER_Y + 1500mm |
| 右車線 | 右車線 | CENTER_Y - 1500mm |
| (空欄) | 中央線上 | CENTER_Y |

### 右折レーンがある道路

中央線から数えて：右折レーン → 第一走行帯 の順。

```
─────────────────────── 道路端（左）
    【左車線】← 対向車線
═══════════════════════ 中央線
    【右折レーン】← 2.5m幅、中央線に接する
- - - - - - - - - - - - 車線分離線
    【第一走行帯】← 残り幅、外側
─────────────────────── 道路端（右）
```

| 車線指定 | 意味 | Y座標 |
|---------|------|-------|
| 左車線 | 左車線（対向） | CENTER_Y + 1500mm |
| 右車線右折 | 右折レーン中央 | CENTER_Y - 1250mm (2500/2) |
| 右車線第一 | 第一走行帯中央 | CENTER_Y - 2500 - 残り幅/2 |
| 右車線右折/右車線第一 | 車線分離線の位置 | CENTER_Y - 2500mm |

### 同一測点に複数の標示

同じ測点で車線ごとに行を分ける：

| No | 種類 | 起点 | 車線 |
|----|------|------|------|
| 1 | 右折矢印 | No.3+5 | 右車線右折 |
| 2 | 左折矢印 | No.3+5 | 右車線第一 |

または直進左折の場合：

| No | 種類 | 起点 | 車線 |
|----|------|------|------|
| 1 | 右折矢印 | No.3+5 | 右車線右折 |
| 2 | 直進左折 | No.3+5 | 右車線第一 |

### 注意点

- **左車線は対向車線** - 自車線方向の標示は置かない
- 右折矢印 → 右折レーン（右車線右折）
- 左折矢印/直進左折 → 第一走行帯（右車線第一）
