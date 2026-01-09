# Kotlin・Java用語集

## Multiplatform関連

### Kotlin Multiplatform
**複数プラットフォーム**（Android、iOS、Desktop、Web）でコードを共有できるKotlinの機能。

### Expect/Actual API
Kotlin Multiplatformで**プラットフォーム固有の実装**を提供する仕組み。共通コードで`expect`宣言し、各プラットフォームで`actual`実装する。

### Common Source Set
**全プラットフォームで共有**されるコードを配置するソースセット。`commonMain`、`commonTest`など。

### Platform Source Set
**特定プラットフォーム固有**のコードを配置するソースセット。`androidMain`、`desktopMain`など。

### KMM (Kotlin Mobile Multiplatform)
Android/iOS間でのコード共有に特化したKotlin Multiplatformのサブセット。

## プラットフォーム

### Kotlin/JVM
JVMプラットフォーム向けのKotlin。**Java との相互運用性**が高い。

### Kotlin/JS
JavaScript プラットフォーム向けのKotlin。**Web アプリケーション**開発に使用。

### Kotlin/Native
ネイティブバイナリを生成するKotlin。**JVM不要**で動作する。

## 実行環境・言語機能

### JVM Target
Kotlinコードが動作する**Javaバーチャルマシンのバージョン**を指定する設定。

### Kotlin DSL
Kotlinの構文を使って**設定ファイルを記述**する仕組み。Gradleスクリプトなどで使用。

### Reflection
プログラムが実行時に**自分自身の構造を調べる**機能。クラス情報の動的取得など。

### Annotation Processing
**アノテーション**を元にコード生成や検証を行う仕組み。コンパイル時に実行される。
