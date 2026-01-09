# 三角形の基線座標と角度の連続性問題の調査

**日時**: 2025年8月13日  
**目的**: 三角形の基線座標と角度が操作によって連続しなくなる問題の調査

## 対話の流れ

### 初期の問題認識

**ユーザー**: 操作の仕方によっては三角形の基線座標と角度が連続しなくなる時があるみたいなんだけど、調査して

### 調査の開始

調査タスクを整理：
1. 三角形の基線座標と角度の連続性に関するコードを調査
2. 座標計算と角度計算のロジックを分析
3. 不連続が発生する操作パターンを特定
4. 問題の原因を特定し修正案を提案

### コード調査の結果

#### 主要なファイルの発見
- `Triangle.kt`: 基本的な三角形クラス
- `TriangleExtensions.kt`: 座標計算と角度計算の拡張関数
- `TriangleSetters.kt`: 三角形の設定と接続処理
- `TriangleList.kt`: 三角形の管理クラス

#### calcPointsメソッドの重要性
`TriangleExtensions.kt`の`calcPoints`メソッドが座標計算の核心：

```kotlin
fun Triangle.calcPoints(basepoint: PointXY = this.point[0], _angle: Float = this.angle, isArrangeDims: Boolean = false) {
    pointAB = basepoint.offset(length[0], _angle)
    pointBC = calculatePointBC(basepoint)
    calculateInternalAngles()
    calculatePointCenter()
    arrangeDims(isArrangeDims)
    if (!pointNumber.flag.isMovedByUser) pointnumber = pointcenter
    setBoundaryBox()
}
```

#### 角度正規化処理の発見
`TriangleExtensions.kt:203-204, 236-237`で角度正規化：

```kotlin
if (angle < 0) angle += 360f
if (angle > 360) angle -= 360f
```

### 初期仮説: 角度境界での不連続

最初の分析では、360度と0度の境界で角度がジャンプする問題と仮定：
- 359.9° → 0.1° の急激な変化
- 接続三角形の角度計算で`nodeB!!.angle + 180f`の加算
- 回転操作での累積誤差

**修正案として提案**: 連続性を考慮した角度正規化関数

### ユーザーの疑問と方向転換

**ユーザー**: ほんとうにそんなことだろうか。

この疑問を受けて、より深い調査を実施。

### より詳細な調査

#### rotateLCRメソッドの発見
テストコードで`resetByParent`の連続呼び出しパターンを発見：

```kotlin
tri3.resetByParent(tri2.rotateLCRandGet(), tri3.cParam_)  // 1回目
tri3.resetByParent(tri2.rotateLCRandGet(), tri3.cParam_)  // 2回目
tri3.resetByParent(tri2.rotateLCRandGet(), tri3.cParam_)  // 3回目
```

`rotateLCR()`メソッドの構造：
```kotlin
fun Triangle.rotateLCR(): PointXY {
    connectionLCR_--                    // 2→1→0→2 の循環
    if (connectionLCR_ < 0) connectionLCR_ = 2
    cParam_.lcr = connectionLCR_
    setParentBCFromCLCR()               // 接続方法変更
    return setBasePoint(cParam_)        // 座標再計算
}
```

### 真の問題の特定

#### 親子角度関係の発見
`Triangle.kt:88-91`の定義：

```kotlin
val angleMmCA: Float
    get() = angle - angleCA      // 親の基準角度 - 内角CA
val angleMpAB: Float  
    get() = angle + angleAB      // 親の基準角度 + 内角AB
```

`TriangleSetters.kt`での子の角度設定：
```kotlin
case 1: angle = parent.angleMpAB    // parent.angle + angleAB
case 2: angle = parent.angleMmCA    // parent.angle - angleCA  
```

#### 問題の核心
1. **再構築時の角度依存**: 親三角形が編集されると`angleCA`, `angleAB`が変わる
2. **子の角度の再計算**: `setOn`で子の`angle = parent.angleMmCA`が再設定される  
3. **循環依存**: 親の内角が変わる → 子の基準角度が変わる → 子の座標がジャンプする

具体例：
```
1. 親三角形(3,4,5) → angleCA = 53.13°
2. 子を追加: angle = parent.angleMmCA = 180° - 53.13° = 126.87°
3. 親を(3,4,6)に変更 → angleCA = 41.41°に変化  
4. 再構築: 子のangle = 180° - 41.41° = 138.59° (11.72°ジャンプ!)
```

### ユーザーからの現実的な反応

**ユーザー**: うん、それだと常に親を触ると子の角度が変わることになるけど、じっさい変わりつつ伝播していく仕組みになっていて、それが期待する振る舞いで、概ねそうなっている。

**重要な訂正**: 親を触ったときに子の角度が変わるのは正常な動作で、「伝播していく仕組み」として設計されている。

### 実際の問題の明確化

**ユーザー**: 再現が難しいんだけど、今実際にいくつかやってみてるけど、接続が途切れるという現象は起きないな。

**実際の問題**:
- **視覚的に三角形同士が離れて見える**現象が稀に発生
- 何年かテストしながら使用していて「ごく偶に」起こる
- 困ることはなかったレベルのレアケース
- 「初学者の試行錯誤の産物で設計がめっちゃくちゃ」だが動いている

### 設計改善の議論

#### 双方向連結リストへの変更案

**ユーザー**: これは三角形の連結の表現を、リーナスが言っていた双方向連結リストの形に作り変えたいなあと、そうすれば、特殊ケースを排除することでもっとノードごとの管理がわかり易くならない感と

現在の構造：
```kotlin
// Triangle.kt
var nodeA: Triangle? = null  // 親
var nodeB: Triangle? = null  // 子（B辺）  
var nodeC: Triangle? = null  // 子（C辺）
var parentnumber = -1        // 親の番号
var connectionSide = -1      // 接続辺
```

提案された改良案：
```kotlin
// 各辺に対する双方向参照
data class EdgeConnection(
    var neighbor: Triangle?,     // 隣接する三角形
    var thisSide: Int,          // 自分のどの辺か
    var neighborSide: Int       // 相手のどの辺か
)

class Triangle {
    val edges = Array(3) { EdgeConnection(null, it, -1) }
    
    fun connectTo(other: Triangle, thisSide: Int, otherSide: Int) {
        edges[thisSide].neighbor = other
        edges[thisSide].neighborSide = otherSide
        other.edges[otherSide].neighbor = this  
        other.edges[otherSide].neighborSide = thisSide
    }
}
```

利点：
- **整合性保証**: 接続時に両側を自動更新
- **特殊ケース削除**: connectionSide, parentnumber不要
- **明確な関係**: どの辺がどの辺と繋がっているか明確

#### 責任の分散化

**ユーザー**: TrigangleListっていうのが、マネージャーの役割、ファクトリーパターン？とかっていうのか、知らんけど、グレアム先生に言わせると、こういうパターンがあるときは、構造の抽象化が十分にできていないサインだと

現在の`TriangleList`の問題：
- **コンテナ** (三角形を保存)
- **ファクトリー** (三角形を生成)
- **マネージャー** (接続関係を管理)
- **コーディネーター** (番号付けや更新の調整)

複数の責任を持ちすぎている状態。

理想的な分散案：
```kotlin
// 純粋なデータ構造
class TriangleMesh {
    private val triangles = mutableSetOf<Triangle>()
    
    fun addTriangle(triangle: Triangle)
    fun removeTriangle(triangle: Triangle)
    fun getConnectedComponent(triangle: Triangle): Set<Triangle>
}

// 三角形の接続関係のみ管理
class ConnectionManager {
    fun connect(tri1: Triangle, side1: Int, tri2: Triangle, side2: Int)
    fun disconnect(tri: Triangle, side: Int)
}
```

#### 設計哲学の洞察

**ユーザー**: クラスというものがこうで、状態を持って、っていう、テキストでならうことを使いながらやると、最初の形になる。でもこれは、コレが駄目って言うわけではなくて、クラスがない世界で構築しようとするとすげえ煩雑になることも感じたので、一歩すすむかどうかは、なんつーか、もっとやりたいことがあるかどうかによるんだなと。

重要な気づき：
- クラスベースOOPから入ると自然にこうなる
- クラスがない世界（C言語等）だと更に煩雑になる
- 改善するかどうかは「もっとやりたいことがあるか」次第

#### 複雑化の経緯

**ユーザー**: Dimsっていう、各辺の寸法値を配置するクラスを作ったり、いろんな状態を追加していくうちに、もはやちょっとわけわからんことになりつつあって、考え直すときに来ているが、人の手では限界があるかも

典型的な「機能追加による複雑化」パターン：
最初は単純だった`Triangle`に寸法表示、接続管理、番号管理、色管理等が積み重なり巨大クラスに。

### リファクタリング戦略

#### Google式の段階的アプローチ

**ユーザー**: 気付いたらそうなっていた、という事で行くなら、Googleがやっているらしい事のように、沢山のUI実装、を数を作り直していって、俯瞰して考えるというのが、単純化に有効な気がしている。レガシーコードをいっぱい作るってことだが

提案された戦略：
```
初回実装 → 問題A, B, Cを発見
2回目実装 → 問題Bは解決、新たに問題D発見  
3回目実装 → 問題C, D解決、でも問題Eが...
4回目実装 → ようやく本質が見えてくる
```

各バージョンは「レガシー」になるが、そのプロセス自体が価値を生む。

段階的アプローチ例：
- 1回目：「とりあえず動く」（現在のバージョン）
- 2回目：「接続管理に集中」
- 3回目：「描画と計算を分離」  
- 4回目：「完全に双方向リスト化」

各回で**一つの側面に集中**して作り直すことで、最終的に理想的な設計が見えてくる。

## まとめ

### 当初の問題認識の変化
- **当初**: 角度の360度境界での数値ジャンプ問題
- **実際**: 視覚的な接続の途切れが稀に発生する問題

### 設計上の課題
1. **複雑な状態管理**: 現在の片方向参照による整合性維持の困難
2. **責任の集中**: `TriangleList`が多すぎる責任を持っている
3. **機能追加による複雑化**: `Dims`等の追加で巨大クラス化

### 改善の方向性
1. **双方向連結リスト**: 接続の整合性を自動保証
2. **責任の分散**: 各クラスの役割を明確化
3. **段階的リファクタリング**: Google式の「数を作って俯瞰する」アプローチ

### 現実的な判断
- 現在のシステムは「何年も使われている動くシステム」
- 改善するかどうかは「もっとやりたいことがあるか」次第
- 「人の手では限界があるかも」だが、問題を認識できている時点で初心者レベルは超えている