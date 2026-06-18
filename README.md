# TriangleList

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Desktop](https://img.shields.io/badge/Platform-Desktop-blue.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-Private-red.svg)]()
[![Live Demo](https://img.shields.io/badge/Live_Demo-GitHub_Pages-orange.svg)](https://yuujikamura.github.io/trianglelist/)

測量・土木業界向けの面積計算・図面作成アプリケーション

## ▶ ブラウザで今すぐ試す（インストール不要）

### https://yuujikamura.github.io/trianglelist/

三角形をつないで土地の面積を作図し、そのまま **DXF / SFC / PDF / Excel** に出力。
辺の長さを直接いじると図形も寸法もその場で連動して動く ── 粘土をこねるような対話編集が、ブラウザだけで完結します。

## 概要

TriangleListは三角形分割による高精度な面積計算と、CAD図面の表示・編集機能を提供するAndroidアプリケーションです。測量業務で必要な各種ファイル形式の入出力をサポートし、Desktop版はDXFファイルのプレビュー表示専用ツールとして提供されています。

## 主な機能

### 📐 面積計算
- 三角形分割による高精度面積計算
- 測量データの入力・編集
- 座標計算と図形解析

### 📁 ファイル対応形式
- **DXF**: CAD図面の読み込み・書き出し
- **CSV**: 測量データの入出力
- **PDF**: 図面・帳票の出力
- **Excel**: データ集計・レポート作成

### 🎨 CAD機能
- 図面表示・編集
- DXFテキスト描画
- ジオメトリ精度制御
- デバッグ可視化

### 📱 プラットフォーム対応
- **Android**: メインアプリケーション（面積計算・図面編集）
- **Desktop**: DXFプレビュー専用ツール
- **Web**: ブラウザ版ビューア

### 🌐 Web版（GitHub Pages・インストール不要）
**https://yuujikamura.github.io/trianglelist/**

- CSV読み込み、三角形リストの対話編集（辺・接続・二重断面・控除）
- 辺を直接いじると図形・寸法がその場で連動する操作感
- DXF / SFC / PDF / Excel 出力、A3図面枠プレビュー

## 技術スタック

### コア技術
- **Kotlin Multiplatform**: プラットフォーム共通コード
- **Jetpack Compose**: モダンUI フレームワーク
- **Android SDK 35**: 最新Android対応

### 主要ライブラリ
- **Apache POI**: Excel ファイル処理
- **Material Design**: UIコンポーネント
- **Navigation Component**: 画面遷移管理

### 開発環境
- **Kotlin Multiplatform**: 2.x (common Kotlin/Wasm + Android + Desktop)
- **Android SDK**: compileSdk 36 / targetSdk 35 (Google Play 2025 要件対応)
- **JDK**: 21 (CI ビルド用) / 17 以降 (ローカル開発)
- **Vite**: web/ のバンドラ (Kotlin/Wasm distribution を import)

## プロジェクト構成

```
trianglelist/
├── app/                   # Android アプリケーション (面積計算・図面編集)
├── common/                # Kotlin Multiplatform 共通コア
│   └── src/commonMain/   # editmodel (Triangle / Rectangle / EditObject 基底) +
│                         #   datamanager (DXF/SFC/PDF writer 基底) + web (描画/hit test)
├── desktop/               # Desktop DXF プレビュー専用ツール
├── web/                   # ブラウザ版 (Vite + Kotlin/Wasm) ← 現行 GitHub Pages 配信
├── docs/adr/              # アーキテクチャ決定記録 (ADR、設計判断の履歴)
├── doc/                   # 設計メモ・調査ドキュメント
├── trianglelist-web/      # (legacy) 旧 Rust + Trunk 版。web/ に置き換え済
└── rust-core, rust-dxf,   # Rust 検証部品 (sub-LLM による golden 照合用、本流ビルドからは独立)
    rust-renderer, rust-trilib
```

editmodel 側は `EditObject` 基底に `vertices()` / `centroid()` / `containsPoint()` /
`pointNumberAnchor()` / `emitDimensionSpecs()` 等の共通契約を持ち、`Triangle` (3 辺) と
`Rectangle` (4 辺の台形) がそれを実装する。writer 側は `DrawingFileWriter` 基底に
`DrawPrim` (Line / Rect / Circle / Text) のリストを組む frontend と、各形式
(DXF / SFC / PDF / Web) が同じリストを各々のプリミティブに翻訳する backend に分かれる
(ezdxf 流の frontend/backend 分離、ADR 0010)。

## ビルド設定

### 開発環境要件
- **Android Studio**: Koala 以降推奨
- **JDK**: 17 以降 (CI は 21)
- **Android SDK**: compileSdk 36 / targetSdk 35
- **Node.js**: 22 (web/ ビルド・dev server 用)

### ビルドバリエーション
- **dev**: 開発版
- **free**: 無料版
- **full**: フル機能版

### ビルドコマンド

```bash
# Android 版ビルド
./gradlew assembleDevDebug

# Desktop DXF プレビューツール実行
./gradlew :desktop:run

# 共通コア (Kotlin Multiplatform) のテスト
./gradlew :common:desktopTest

# 全モジュールビルド
./gradlew build

# Web 版 (Vite dev server、http://localhost:5173/)
cd web && npm install && npm run dev

# Web 版 (本番ビルド、web/dist/ に出力)
cd web && npm run build
```

## テスト

```bash
# Android 単体テスト (app/)
./gradlew testDevDebugUnitTest

# 共通コア (common/) — 多態化・描画・hit test・写経 golden 等
./gradlew :common:desktopTest

# DXF/SFC golden 照合
./gradlew :app:test --tests "*DxfDimensionLayoutGolden*"
```

テストは app/ 配下に 27 ファイル、common/ 配下に 67 ファイル (2026-06-18 時点)。
主な範囲:
- 三角形分割と面積計算 (`TriangleTest.kt` / `PointNumberTest.kt`)
- DXF/SFC writer の golden 照合 (`DxfFileWriterTest.kt` / `SfcWriterTest.kt`)
- Web 描画パイプライン (`WebPrimitiveRendererTest.kt` / `WebHitTestMixedTest.kt`)
- Triangle / Rectangle の多態化契約 (`GeometryUnificationTest.kt` / `WebOverridesRectangleTest.kt`)
- CSV ⇔ オブジェクト往復 (`CsvCodecBuildAllTest.kt`)

## 開発状況

### 主なブランチ
- **main**: リリース流入先、GitHub Pages デプロイの起点
- **circleci**: CI 設定検証用 (CodeQL / Dependabot 等は GitHub Actions 側)

### 最近の更新 (2026-06)
- editmodel の多態化集約: `is Triangle` / `is Rectangle` 分岐を `EditObject` 基底の
  共通契約 (`pointNumberAnchor` / `containsPoint` / `applyDimTextSize` 等) に置換
- Rectangle 直角マーカーの内向き判定を centroid 基準に再設計 (alignment 0/1/2 で内側保証)
- 直角マーカーを DXF/SFC/PDF/Web 全形式に統一描画 (web 限定だったのを基底 prim に追加)
- 描画プリミティブのデータ化 (`DrawPrim` sealed interface) と frontend/backend 分離 (ADR 0010)
- Rectangle 中央揃え時の spine オフセット (短辺 × 30%) で番号サークルを避けるレイアウト
- camera-shot.mjs を web の目視批評用検証部品として整備

## ライセンス

このプロジェクトはプライベートライセンスです。

## 開発者

測量・CAD業務の効率化を目指して開発中

---

*測量業務の精度向上と作業効率化を支援するアプリケーション*