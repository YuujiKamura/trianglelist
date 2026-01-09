# プロジェクト構造・命名規則用語集

## ディレクトリ構造の設計原則

### Layer-Based Organization (層別構成)
プロジェクトを**責務別の層**で整理する方式。UI層、ビジネスロジック層、データ層など機能的な分離を重視。

### Feature-Based Organization (機能別構成)
**機能単位**でディレクトリを分割する方式。1つの機能に関連するファイルをまとめて配置。

### Clean Architecture
**依存関係の方向性**を制御するアーキテクチャ。外層（UI）から内層（ビジネスロジック）への一方向依存を徹底。

## 命名規則

### Adapter Pattern
**異なるインターフェース間**を橋渡しするクラス。外部データ形式をアプリケーション内部形式に変換する責務。

### Renderer Pattern
**データを視覚的表現に変換**するクラス。ビジネスデータを画面描画コマンドに変換する責務。

### Presenter Layer
**UI表示ロジック**を担当する層。ビジネスデータをUI表示用に加工・整形する責務。

### View Layer
**ユーザーインターフェース**そのものを担当する層。ユーザー入力受付と画面表示のみに責務を限定。

## Desktop プロジェクトの適切な構成例

### 現在の問題構造
```
data/  # ❌ 曖昧な命名
├── CADViewRenderer.kt
├── DrawingBoundsCalculator.kt
└── TextRenderer.kt
```

### 推奨構造
```
presentation/  # UI表示層
├── renderer/
│   ├── CADViewRenderer.kt
│   ├── DrawingBoundsCalculator.kt
│   └── TextRenderer.kt
└── adapter/
    └── DxfToComposeAdapter.kt

ui/  # Composeコンポーネント層
├── cadview/
│   ├── CADView.kt
│   └── ColorConverter.kt
└── common/
```

## アンチパターン

### God Folder (神フォルダ)
`data`, `utils`, `common`など**曖昧な名前**のフォルダに何でも詰め込むパターン。責務が不明確になり保守性が低下。

### Anemic Package Structure (貧血パッケージ構造)
機能的関連性を無視して**技術的分類のみ**でフォルダを分けるパターン。関連するクラス間の距離が遠くなる。

### Package by Layer Anti-pattern
レイヤー別にパッケージを分けすぎて、**1つの機能を変更**するのに複数のパッケージを跨ぐ必要が生じるパターン。
