# TriangleList

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Desktop](https://img.shields.io/badge/Platform-Desktop-blue.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-Private-red.svg)]()

測量・土木業界向けの面積計算・図面作成アプリケーション

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
- **Kotlin**: 2.0.0
- **Java**: 17
- **Android Gradle Plugin**: 8.6.1
- **Compose Multiplatform**: 1.7.0

## プロジェクト構成

```
trianglelist/
├── app/                    # Androidアプリケーション
│   ├── datamanager/       # ファイル入出力
│   ├── editmodel/         # データモデル
│   ├── viewmodel/         # ビジネスロジック
│   └── fragment/          # UI フラグメント
├── common/                # 共通ライブラリ
│   ├── dxf/              # DXF処理
│   └── util/             # 描画ユーティリティ
├── desktop/              # DXFプレビュー専用ツール
│   ├── cadview/          # DXFビューア
│   └── adapter/          # 描画レンダラー
└── doc/                  # プロジェクトドキュメント
```

## ビルド設定

### 開発環境要件
- **Android Studio**: Ladybug以降
- **JDK**: 17以降
- **Android SDK**: API 35
- **Kotlin**: 2.0.0対応

### ビルドバリエーション
- **dev**: 開発版
- **free**: 無料版
- **full**: フル機能版

### ビルドコマンド

```bash
# Android版ビルド
./gradlew assembleDebug

# DXFプレビューツール実行
./gradlew :desktop:run

# テスト実行
./gradlew test

# 全モジュールビルド
./gradlew build
```

## テスト

### 単体テスト（29ファイル）
- 三角形計算: `TriangleTest.kt`
- DXF処理: `DxfFileWriterTest.kt` 
- CSV読み込み: `CsvLoaderTest.kt`
- 測量計算: `MathTest.kt`

### 統合テスト
- UI テスト: `MainActivityTest.kt`
- ライブラリテスト: `TriLibInstrumentedTest.kt`

```bash
# 単体テスト実行
./gradlew testDebugUnitTest

# 統合テスト実行  
./gradlew connectedAndroidTest
```

## 開発状況

### 現在のブランチ
- **main**: 安定版
- **circleci**: CI/CD設定
- **stable-release**: リリース版
- **triangle-kotlin**: Kotlin移行

### 最近の更新
- DXFテキスト描画・ジオメトリ精度改善
- ビルド最適化と警告修正  
- DXFモジュールのリファクタリング
- Kotlin Multiplatform対応

## ライセンス

このプロジェクトはプライベートライセンスです。

## 開発者

測量・CAD業務の効率化を目指して開発中

---

*測量業務の精度向上と作業効率化を支援するアプリケーション*