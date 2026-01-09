# Claude Code セッション記録

## 三角形接続の仕様

### 辺の役割

三角形には3つの辺があり、それぞれ役割が異なる:

| 辺 | 役割 | 説明 |
|----|------|------|
| A辺 | 接続辺 | 親との共有辺。子の生成時に親の辺と一致する |
| B辺 | 自由辺 | 他の三角形が接続可能 |
| C辺 | 自由辺 | 他の三角形が接続可能 |

### CONNECTION_TYPE（CSV列5）の値

```
-1 = 独立（接続なし）
 1 = 親のB辺に接続
 2 = 親のC辺に接続
```

**重要**: A辺への接続は設計上存在しない。親のA辺は既にその親（祖父三角形）と共有済みのため。

### 最小形式CSV例

```csv
番号, 辺A, 辺B, 辺C, 親番号, 接続タイプ
1, 6.0, 5.0, 4.0, -1, -1        # 独立三角形
2, 5.0, 4.0, 3.0, 1, 1          # 親1のB辺(5.0)に接続
3, 4.0, 3.5, 3.0, 1, 2          # 親1のC辺(4.0)に接続
```

子の辺A長は親の接続先辺長と一致させる。

---

## 2025年1月8日 - Android 15 (API 35) 対応とlint警告調査

### セッション概要
- Google Play要件対応（Android 15/API 35必須）
- 隠れていた142個のlint警告の発見と分析
- 依存関係の更新とバージョン管理自動化
- pre-commit hooks導入

### 主な作業内容

#### 1. Android 15 (API 35) 対応
**問題**: Google Playから2025年8月31日までにtargetSdk = 35必須の通知

**対応**:
- `compileSdk = 35` → `36` に更新
- `targetSdk = 35` 全productFlavors（dev/free/full）で統一
- API 35エミュレータでのキーボード表示問題を確認（エミュレータ特有の既知問題）

**結果**: Google Play要件を満たし、ビルド正常完了

#### 2. lint警告の全面調査
**発見**: lint-baseline.xmlで160個の警告が隠蔽されていた

**調査手順**:
```kotlin
lint {
    // baseline = file("lint-baseline.xml")  // 一時的に無効化
    abortOnError = false
    warningsAsErrors = false
}
```

**分析結果**:
- 実際の警告数: 142個
- カテゴリ分類:
  - 🟢 ノイズ系: `ComposableNaming`, `CompositionLocalNaming`等
  - 🟡 検討必要: `CoroutineCreationDuringComposition`等
  - 🔴 修正必要: `ContextCastToActivity`, `BadConfigurationProvider`等

**成果物**: `LINT_ANALYSIS_2025-01-08.md`作成

#### 3. 依存関係更新
**更新内容**:
- androidx.navigation: 2.9.0 → 2.9.3
- io.mockk: 1.14.0 → 1.14.5
- org.mockito: 5.17.0 → 5.18.0
- robolectric: 4.14.1 → 4.15.1
- androidx.test.*: 各種最新版

**効果**: `ObsoleteLintCustomCheck`警告が解決

#### 4. pre-commit hooks導入
**設定**:
```bash
# .git/hooks/pre-commit
./gradlew lintDevDebug --quiet
```

**カスタムタスク**:
```kotlin
tasks.register("preCommitChecks") {
    dependsOn("lintDevDebug", "testDevDebugUnitTest")
}
```

#### 5. リリース準備自動化
**問題**: バージョン番号上げ忘れの頻発

**解決策**: `prepareRelease`タスク作成
```kotlin
tasks.register("prepareRelease") {
    // versionCode自動インクリメント
    // versionName手動変更リマインダー
}
```

**使用方法**:
```bash
./gradlew prepareRelease  # versionCode自動+1
# versionName手動変更
./gradlew assembleRelease
```

### 技術的な発見

#### SDKバージョン警告について
- 警告ID: `OldTargetApi`
- 内容: `targetSdk = 35`が最新でないという警告
- 判断: Android 16はまだ開発者プレビュー段階のため、`targetSdk = 35`が正解
- 対応: 警告無視が適切

#### lint-baselineの実態
- 1,675行のXMLファイル
- 160個の警告を「承認済み」として隠蔽
- 本来は段階的修正のためのツール
- 問題: 新しい問題の発見を困難にする

### 開発フロー改善

#### Before (手作業)
- lint警告160個が隠蔽状態
- バージョン上げ忘れでリリース失敗
- 依存関係警告を放置
- コミット時の警告に気づかず

#### After (自動化)
- pre-commit hookでlint自動実行
- `prepareRelease`でバージョン管理自動化
- 依存関係を最新版に統一
- 問題の可視化と記録

### 今後の課題と展望

#### 現在のClaude Codeの限界
- Logcatのリアルタイム監視不可
- エミュレータ画面の視覚認識不可
- マウス/キーボード操作自動化不可
- IDEリファクタツール連携不可

#### 近未来の可能性
- 1年以内にLogcat統合とIDE連携が実現予想
- コスト対効果の改善により商用化進展
- 現在は「心眼の剣豪状態」だが、制約が集中力を生んでいる

### コミット履歴
- `lint警告の全面調査と依存関係更新` (d2fbcb4)
- `リリース準備自動化タスクを追加` (7ef17c5)

### 成果物
- `LINT_ANALYSIS_2025-01-08.md`: 詳細な警告分析レポート
- `COMMIT_WORKFLOW.md`: pre-commit hookの使用方法
- `prepareRelease`タスク: バージョン管理自動化

### 最終状態
- ✅ Google Play要件（API 35）対応完了
- ✅ lint警告の実態把握完了
- ✅ 依存関係最新化完了
- ✅ 開発フロー自動化完了
- ✅ 全変更をGitにプッシュ済み

---

## 2025年1月8日 続き - 音響測距技術の探究

### セッション概要
- リリース自動化の完成とリリース実行
- スマホIMUによる距離測定の可能性検討
- 音響測距技術の詳細な技術検討
- 環境音を活用した空間認識アイデア

### リリース作業の完了

#### keystoreファイルの復旧
- 問題: リリースビルド時にkeystoreパス設定が消失
- 解決: Googleドライブに保存していたkeystoreファイルを使用
- IDEポリシー変更: App Signing by Google Playがデフォルト化
- 結果: 審査提出まで手作業で完了、versionCode 1346, versionName "7.58"

#### バージョン管理自動化の実装
**問題認識**: バージョン番号上げ忘れの頻発で「しょうもないところから直したい」

**実装**: `prepareRelease`タスクの作成
```kotlin
tasks.register("prepareRelease") {
    group = "release"
    description = "リリース準備: versionCode自動更新"
    
    doLast {
        // 現在のバージョン表示
        // versionCode自動+1
        // versionName手動変更リマインダー
        // 次手順の案内
    }
}
```

### スマホ音響測距技術の探究

#### IMUによる距離測定の限界
**初期アイデア**: IMUで0.1mm精度の距離測定
**技術的課題**:
- 加速度積分による累積誤差の爆発的増加
- 重力補正の困難さ
- 温度ドリフト、振動ノイズ

**宇宙技術との比較**:
- 航空宇宙級IMU: $100,000〜 光ファイバージャイロ等
- スマホIMU: $5のMEMSチップ
- 精度差: 10⁻⁶ g vs 10⁻³ g
- 航空宇宙でも単体使用せず（GPS、スタートラッカー等併用）

#### 音響測距の技術的可能性

**基本原理**: 音の往復時間から距離計算
```
distance = (time_of_flight * sound_speed) / 2
```

**環境補正の重要性**:
```kotlin
// 温度補正（最重要）
soundSpeed = 331.3 + 0.606 * temperature
// 湿度補正
soundSpeed += 0.124 * humidity / 100
// 気圧補正
soundSpeed *= sqrt(pressure / 101325.0)
```

**スマホ環境センサーの活用**:
- 温度センサー: ほぼ全機種搭載
- 気圧センサー: 多くの機種に搭載
- 湿度センサー: 一部機種のみ

#### 最適な測距音の特性

**チープ信号（Chirp）が最強**:
```kotlin
// 線形周波数変調
frequency(t) = f_start + (f_end - f_start) * t / duration
// 例: 18kHz → 22kHz を100ms間で変化
```

**利点**:
- 相互相関で亜サンプル精度
- ドップラー効果に強い
- ノイズの中でも検出可能
- 人間に聞こえにくい超音波域使用

#### 2台スマホによる測距システム

**システム構成**:
```
スマホA（音源）: 固定基準点
- 既知座標からビーコン音発信
- 複数周波数で位置特定用信号

スマホB（測距）: 移動測定点  
- 音源Aからの距離・方向測定
- IMUで相対移動記録
```

**10m範囲での期待精度**:
- 音響測距理論値: 1ms誤差 = 34cm誤差
- スマホ音響: ~1ms精度期待 → 30cm程度の誤差
- 継ぎ足し測定で長距離対応可能

**公共事業での精度要求との乖離**:
- 公共測量要求: ±1cm以内
- スマホ測距: ±30cm → 完全にアウト
- 用途限定: 概算見積もり、DIY、教育用途等

#### 同一筐体の利点活用

**革新的アイデア**: スピーカー・マイク固定配置の活用
```kotlin
// 筐体内音響特性を基準として使用
directSignal = knownInternalPath
externalReflection = receivedSound - internalSound
phaseDelay = measurePhaseShift(externalSignal, directSignal)
```

**メリット**:
- キャリブレーション不要
- 筐体という「制約」を「基準点」として活用
- 工場での一度測定で個体差対応

#### 環境音響による空間認識

**パッシブ音響分析のアプローチ**:
```
「じっとしてるとどこになにがあるかわかる」（勝海舟的発想）
```

**実装コンセプト**:
```kotlin
// 静止状態での環境スキャンモード
if (imu.isStationary()) {
    ambientAnalysis.start()
    // エアコン音 → 天井高さ推定
    // 隣室音 → 壁位置・厚さ  
    // 交通音 → 窓方向・距離
    // 反響音 → 部屋形状
}
```

**3次元音響マッピング**:
```kotlin
// フェーズ1: 初期空間スキャン
for angle in 0..360 step 10 {
    emitDirectionalSound(angle)
    echoMap[angle] = recordEcho()
}

// フェーズ2: 移動後の照合
newEchoMap = scanCurrentPosition()
(deltaX, deltaY) = correlateEchoMaps(originalMap, newEchoMap)
```

#### 既存技術との関係

**類似技術の存在確認**:
- 無線測距: レーダー、GPS、Bluetooth/WiFi
- 音響測距: 医療エコー、魚群探知機、駐車センサー
- 研究レベル: EchoLocate、SoundWave、Acoustic SLAM
- 商用製品: Amazon Echo、Google Nest、ロボット掃除機

**スマホ普及が少ない理由**:
- 技術的制約: 計算量大、バッテリー消費、リアルタイム処理困難
- 市場問題: LiDAR/ARの方が簡単、一般用途には複雑

**生物模倣の優秀性**:
```
コウモリ → 研究 → ロボット → スマホ（現在地点）
動物模倣 = 何億年の最適化結果 = 最強のデザイン指針
```

#### ARCoreとの比較

**ARCoreが音を使わない理由**:
1. 計算コスト10-100倍
2. リアルタイム処理困難  
3. 視覚で十分な精度達成済み

**音響の回折特性**:
- 光: 直進性強い → 物体輪郭クッキリ
- 音: 回折しやすい → 障害物回り込み → 方向特定困難

**但し音響の独自価値**:
- 暗闇でも動作
- 透明物体も検出
- 材質の違いを識別
- 物体の内部構造推定

#### 実機での検証環境

**Galaxy A55での実験適性**:
- デュアルマイク構成
- Dolby Atmos対応（空間音響処理能力）
- Samsung独自音響処理チップ
- 期待精度: ±20-50cm（1-2m範囲）
- 最適周波数帯: 2-8kHz

**室内実験の利点と課題**:
- 利点: 温度安定、風なし、反射音制御可能
- 課題: 反響多い、音源近すぎ問題
- 騒音対策: 超音波使用（18kHz以上）

#### システムモデル設計

**物理モデル**:
```kotlin
data class AcousticMeasurement(
    val timeOfFlight: Float,
    val soundSpeed: Float,
    val signalStrength: Float,
    val noiseLevel: Float,
    val confidence: Float
)

data class EnvironmentalConditions(
    val temperature: Float,
    val humidity: Float?,
    val pressure: Float?,
    val ambientNoiseProfile: FrequencySpectrum
)
```

**3次元反響点群モデル**:
```kotlin
data class EchoPoint(
    val position: Vector3D,
    val reflectionStrength: Float,
    val material: MaterialType?,
    val confidence: Float
)

data class AcousticPointCloud(
    val echoPoints: List<EchoPoint>,
    val scanOrigin: Vector3D,
    val timestamp: Long
)
```

### 技術的洞察

#### AIによる新発見の可能性
**推論AIの潜在力**:
- 従来思考: 物理法則→限界→諦め
- AI推論: 大量データ→パターン発見→新手法創出
- 複数の「不可能」組み合わせで「可能」実現

**環境音響相関の活用**:
- 移動方向→ドップラー効果
- 移動速度→周波数変化率
- 距離→音の減衰率
- 環境→反響パターン

#### 制約の活用という逆転発想
**筐体制約の基準点化**:
- 従来: 筐体=制約
- 新発想: 筐体=既知基準点
- スピーカー・マイク固定関係の活用

#### 技術発展の自然な流れ
**生物模倣→技術化のパターン**:
```
動物能力発見 → 研究室実証 → ロボット実装 → 
民生化（スマホ・家電） → 当たり前技術
```

### セッション総括

**技術探究の成果**:
- スマホ単体での精密測距の限界と可能性を理解
- 音響測距技術の理論的基盤を構築
- 既存技術との差別化ポイントを発見
- 実装可能なシステムモデルを設計

**開発者としての気づき**:
- 「誰かがやっているかも」という懸念と「新しい組み合わせ」の価値
- 技術的制約を逆手に取る発想の重要性
- 生物模倣による設計指針の有効性

**現実的な次ステップ**:
- 机上実験による基礎検証
- Galaxy A55での実装プロトタイプ
- 段階的精度向上とユースケース開拓

---
*「思考実験をえんえんとやっててもお腹は膨らまない」- 技術アイデアから実装への転換点*