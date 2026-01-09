# Android開発用語集

## Lint関連

### Lint
Androidアプリの**静的解析ツール**。コードの品質問題、パフォーマンス問題、セキュリティ問題などを自動検出する。

### Lintベースライン
既存のLint警告を「基準点」として記録し、**新しい問題のみを検出**するようにする仕組み。大規模プロジェクトの段階的改善に有効。

## Manifest・設定関連

### AndroidManifest.xml
Androidアプリの**設定ファイル**。アプリの権限、コンポーネント、intent-filterなどを定義する。

### intent-filter
Androidの**インテント（操作要求）をフィルタリング**する仕組み。どのアクションやデータ型に対応するかを定義する。

## パッケージ関連

### AAR (Android Archive)
Android用の**ライブラリパッケージ形式**。JARファイルにAndroidリソースを追加したもの。

### APK (Android Package)
Androidアプリの**インストールファイル形式**。アプリの実行に必要な全ファイルを含む。

## 開発ツール

### ADB (Android Debug Bridge)
Android端末とPC間で**通信するためのツール**。アプリのデバッグや端末操作に使用。
