# Gradle用語集

## 基本概念

### Gradle
**ビルド自動化ツール**。依存関係管理、コンパイル、テスト実行などを自動化する。

### Task
Gradleの**実行単位**。コンパイル、テスト、パッケージングなどの個別処理。

### Dependency Resolution
**依存関係の解決**処理。ライブラリの競合やバージョン管理を自動化。

## 設定ファイル

### build.gradle.kts
GradleのビルドスクリプトをKotlin DSLで記述したファイル。**プロジェクトの設定**を定義する。

### settings.gradle.kts
**マルチプロジェクトの構成**を定義するファイル。サブプロジェクトやプラグイン管理を行う。

### gradle.properties
Gradleの**グローバル設定**を記述するファイル。JVMオプションや警告制御などを設定。

## 実行・最適化

### Gradle Wrapper (gradlew)
プロジェクト固有のGradleバージョンを**自動ダウンロード・実行**するスクリプト。環境差異を回避。

### Configuration Cache
Gradleの**設定情報をキャッシュ**してビルド速度を向上させる機能。

## プロジェクト構成

### Build Variants
Androidアプリの**ビルドバリエーション**。debug/release、free/full などの組み合わせ。

### Multi-module Project
**複数のモジュール**に分割されたプロジェクト構成。app、common、core など。
