# TriangleList プロジェクト - イシュー現状レポート
**日付**: 2025年1月9日

## 📋 概要

TriangleListは測量・土木業界向けの面積計算・図面作成アプリケーションです。
- **Android版**: Kotlin Multiplatform + Jetpack Compose（メインアプリ）
- **Desktop版**: Kotlin Multiplatform + Compose（DXFプレビュー専用）
- **Web版**: Rust + WebAssembly + egui（新規実装中）

## ✅ 実装済みイシュー

### #6: [Feature] 座標計算・三角形モデル実装
**ステータス**: ✅ **実装済み（一部）**

- **Kotlin版**: ✅ 完全実装
  - `app/src/main/java/com/jpaver/trianglelist/editmodel/Triangle.kt`
  - `app/src/main/java/com/jpaver/trianglelist/editmodel/TriangleList.kt`
  - 座標計算、面積計算、接続情報など全機能実装済み

- **Rust版**: ✅ 基本構造実装済み
  - `rust-trilib/src/model/triangle.rs` - 基本Triangle構造体
  - `rust-trilib/src/model/triangle_list.rs` - TriangleList管理構造体
  - 座標計算、面積計算、内角計算などの基本機能実装済み
  - **不足**: 座標計算の完全な実装（接続時の座標計算など）

### #7: [Feature] CSVパーサー実装
**ステータス**: ✅ **実装済み**

- **Kotlin版**: ✅ 完全実装
  - `app/src/main/java/com/jpaver/trianglelist/datamanager/CsvLoader.kt`
  - 4カラム（基本）、6カラム（接続情報付き）、28カラム（完全形式）に対応

- **Rust版**: ✅ 実装済み
  - `trianglelist-web/src/csv/parser.rs`
  - 4カラム（基本）、6カラム（接続情報付き）に対応
  - エラーハンドリング、バリデーション実装済み

### #8: [Feature] egui描画エンジン（CAD同等テキスト）
**ステータス**: 🔄 **一部実装済み**

- **実装済み**:
  - `trianglelist-web/src/render/text.rs` - CAD互換テキスト描画
  - `trianglelist-web/src/render/canvas.rs` - 三角形・テキスト描画
  - 水平・垂直アライメント対応
  - DXF互換のテキスト配置

- **未対応**:
  - ❌ **回転テキスト**（#44参照）- eguiの制限により未実装（警告のみ）
  - コード内に `log::warn!("Text rotation is not fully supported in egui...")` が存在

### #9: [Feature] GitHub Pages デプロイ設定
**ステータス**: ✅ **実装済み**

- `.github/workflows/deploy.yml` - GitHub Actions設定完了
- `trianglelist-web/dist/` - ビルド成果物確認済み
- `trianglelist-web/index.html` - HTMLテンプレート実装済み
- Trunkビルド設定完備

### #10: [Feature] DXF生成・ダウンロード機能
**ステータス**: ✅ **実装済み**

- **Rust版（Web）**: ✅ 完全実装
  - `trianglelist-web/src/dxf/converter.rs` - Triangle→DXF変換
  - `trianglelist-web/src/dxf/download.rs` - ブラウザダウンロード機能
  - `trianglelist-web/src/app.rs` - UI統合済み

- **Kotlin版（Android）**: ✅ 実装済み
  - `app/src/main/java/com/jpaver/trianglelist/datamanager/DxfFileWriter.kt`
  - `common/src/commonMain/kotlin/com/jpaver/trianglelist/dxf/DxfWriter.kt`

### #12: [並列実装] GitHub Actions デプロイ設定
**ステータス**: ✅ **実装済み**

- `.github/workflows/deploy.yml` に実装済み
- #9と同一設定

### #21: [並列実装] Triangle 構造体定義
**ステータス**: ✅ **実装済み**

- `rust-trilib/src/model/triangle.rs` - 基本構造体実装済み
- テストコード含む（`rust-trilib/src/model/triangle.rs`内）
- 面積計算、内角計算、バリデーション機能実装済み

### #22: [並列実装] TriangleList 管理構造体
**ステータス**: ✅ **実装済み**

- `rust-trilib/src/model/triangle_list.rs` - 管理構造体実装済み
- 自動番号付け、追加・取得機能実装済み

## 🚧 未実装・進行中イシュー

### #44: [Feature] 回転テキスト対応
**ステータス**: 🔄 **プラットフォーム別で部分実装**

**実装状況**:
- **Android版**: ✅ **実装済み**
  - `MyView.kt` で `canvas.drawTextOnPath()` を使用してパス上にテキスト描画
  - DXF書き込み時も回転対応済み（`DxfFileWriter.kt`）
- **Desktop版**: ✅ **実装済み**
  - `TextRenderer.kt` で Compose Multiplatform の `transform.rotate()` を使用
  - テストウィジェットでも回転テキスト実装済み
- **Web版**: ❌ **未実装**
  - `trianglelist-web/src/render/text.rs` でeguiの制限により未実装
  - 警告のみ出力して回転なしで描画

**問題点**:
- eguiは直接的なテキスト回転をサポートしていない
- Web版での実装が課題

**関連イシュー**: #47（egui以外の描画ライブラリへの切り替え検討）

### #45: [Feature] 寸法線表示機能
**ステータス**: 🔄 **プラットフォーム別で部分実装**

**実装状況**:
- **Android版**: ✅ **実装済み**
  - `DimOnPath.kt` - 寸法線のパス計算・配置管理
  - `Dims.kt` - 寸法配置の管理（垂直・水平配置）
  - `MyView.kt` - `drawDigits()` で寸法線とテキストを描画
  - `DxfFileWriter.kt` - DXFへの寸法線書き込み対応
- **Desktop版**: ❓ **確認必要**（DXFプレビュー専用のため、寸法線表示の有無要確認）
- **Web版**: ❌ **未実装**
  - `trianglelist-web/src/render/canvas.rs` に寸法線描画機能なし

**必要な実装（Web版）**:
- 寸法線の描画（線 + 矢印 + テキスト）
- 寸法値の計算・表示
- UI統合

### #46: [Feature] UIコンポーネント拡張
**ステータス**: 🔄 **プラットフォーム別で部分実装**

**実装状況**:
- **Android版**: ✅ **実装済み**
  - 編集機能、設定UI、各種ダイアログなど充実
  - `MainActivity.kt`, `EditorFragment.kt` など多数のUI実装
- **Desktop版**: ✅ **基本UI実装済み**
  - DXFプレビュー用のUI実装済み（`Main.kt`）
  - デバッグモード、ホットリロード機能など
- **Web版**: 🔄 **基本UIのみ実装済み**
  - `trianglelist-web/src/app.rs` - 基本的なUI実装済み
  - ファイルドロップゾーン、CSV入力、DXFダウンロード機能は実装済み
  - **不足**: 詳細な設定UI、表示オプション、編集機能UI

**必要な拡張（Web版）**:
- 詳細な設定UI
- 表示オプション（グリッド、スナップなど）
- 編集機能UI

### #47: [Research] egui以外の描画ライブラリへの切り替え検討
**ステータス**: 🔄 **調査中**

**背景**:
- eguiは回転テキストの直接サポートがない（#44）
- CAD同等のテキスト描画機能が制限されている

**検討が必要なライブラリ**:
- **lyon**: 2Dベクターグラフィックス（パス操作）
- **raqote**: 2Dレンダリングエンジン
- **piet**: 2Dグラフィックス抽象化
- **WebGPU/WGPU**: 低レベルグラフィックスAPI
- **Canvas 2D API**: 直接Canvas API使用（WASM）

**関連イシュー**: #44（回転テキスト対応）

## 📊 実装状況サマリー（プラットフォーム別）

| イシュー番号 | タイトル | Android | Desktop | Web | 優先度 |
|------------|---------|---------|---------|-----|--------|
| #6 | 座標計算・三角形モデル実装 | ✅ | ✅ | 🔄 | 高 |
| #7 | CSVパーサー実装 | ✅ | ✅ | ✅ | - |
| #8 | egui描画エンジン | - | - | 🔄 | 高 |
| #9 | GitHub Pages デプロイ設定 | - | - | ✅ | - |
| #10 | DXF生成・ダウンロード機能 | ✅ | ✅ | ✅ | - |
| #12 | GitHub Actions デプロイ設定 | - | - | ✅ | - |
| #21 | Triangle 構造体定義 | ✅ | ✅ | ✅ | - |
| #22 | TriangleList 管理構造体 | ✅ | ✅ | ✅ | - |
| #44 | 回転テキスト対応 | ✅ | ✅ | ❌ | 中 |
| #45 | 寸法線表示機能 | ✅ | ❓ | ❌ | 中 |
| #46 | UIコンポーネント拡張 | ✅ | ✅ | 🔄 | 低 |
| #47 | egui以外の描画ライブラリ検討 | - | - | 🔄 | 中 |

**凡例**:
- ✅: 実装済み
- 🔄: 一部実装 / 進行中
- ❌: 未実装
- ❓: 要確認
- -: 該当なし / 未対応プラットフォーム

## 📊 総合実装状況

**完全実装**: 5件（#7, #9, #10, #12, #21, #22）
**部分実装（プラットフォーム別差異あり）**: 5件（#6, #8, #44, #45, #46）
**調査中**: 1件（#47）

## 🔍 技術スタック現状

### Web版（trianglelist-web）
- **言語**: Rust
- **GUI**: egui 0.29
- **ビルド**: Trunk
- **ターゲット**: WebAssembly (wasm32-unknown-unknown)
- **依存関係**:
  - `dxf` (ローカル `rust-dxf`)
  - `serde`, `serde_json`
  - `wasm-bindgen`, `web-sys`

### Android版
- **言語**: Kotlin 2.0.0
- **UI**: Jetpack Compose
- **プラットフォーム**: Android SDK 35
- **アーキテクチャ**: Multiplatform (common, app, desktop)

### Desktop版
- **言語**: Kotlin 2.0.0
- **UI**: Compose Multiplatform 1.7.0
- **機能**: DXFプレビュー専用

## 📝 次のステップ提案

### 優先度: 高
1. **#44 回転テキスト対応（Web版）**
   - Android/Desktop版は実装済み、Web版のみ未実装
   - eguiでの実装方法を調査・試行
   - または #47 の調査結果に基づき描画ライブラリを切り替え

2. **#6 座標計算の完全実装（Web版）**
   - Android版は完全実装済み、Rust版は基本実装のみ
   - Rust版での接続時の座標計算を実装
   - Kotlin版との互換性確認

3. **#8 egui描画エンジンの完全実装**
   - 回転テキスト対応（#44と関連）
   - 寸法線描画機能（#45と関連）

### 優先度: 中
4. **#47 描画ライブラリ調査（Web版）**
   - 各ライブラリの比較検討（lyon, raqote, piet, WebGPU等）
   - プロトタイプ実装で検証
   - パフォーマンス・機能性の評価

5. **#45 寸法線表示機能（Web版）**
   - Android版の実装を参考に設計
   - Web版での実装開始
   - `DimOnPath`, `Dims` のRust版実装

### 優先度: 低
6. **#46 UIコンポーネント拡張（Web版）**
   - Android版のUIを参考に要件定義
   - 段階的実装
   - 設定UI、表示オプション、編集機能の追加

## 🔗 関連リソース

- GitHub Issues: https://github.com/YuujiKamura/trianglelist/issues
- プロジェクト構成: `README.md`
- Web版実装: `trianglelist-web/`
- Rust コアライブラリ: `rust-trilib/`, `rust-core/`

---
*このレポートは2025年1月9日時点の情報に基づいています。*
