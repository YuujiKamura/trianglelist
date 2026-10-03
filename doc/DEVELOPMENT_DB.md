# 開発データベース (DEVELOPMENT_DB)

このプロジェクトには、同じ目的（三角形リストやCADデータ処理）を達成するために、複数のテクノロジースタックやプラットフォーム構成（Rust, Kotlin Wasm, TypeScript/Vite）のモジュールが混在しています。それぞれの役割、起動方法、開発体験の特性を以下に整理します。

> [!NOTE]
> この情報は `doc/development.db` (SQLite) にデータベースとして保存されており、必要に応じて自動化スクリプトなどから参照・更新可能です。

## モジュール一覧カタログ

### 1. web-js (メインWebアプリ / 本命推奨)
* **パス**: `web-js/`
* **ステータス**: `Active`
* **技術スタック**: `Vite + TypeScript + Kotlin/Wasm (common)`
* **開発ポート**: `http://localhost:5173/`
* **開発コマンド**: `npm run dev`
* **ビルドコマンド**: `npm run build`
* **概要**:
  三角形リストをポチポチ追加してインタラクティブに作成するメインのWebアプリケーション。UIは軽量なHTML+Canvas 2Dで構築されています。
* **連携**:
  計算ロジックやDXF出力のコア（`DrawingFileWriter.writeDrawingFrame` や CSVパース等）は、Kotlinの `:common` モジュールがビルドしたWASM（`TriangleList-common-wasm-js.wasm`）をインポートして呼び出すハイブリッド構成です。
* **開発体験 (DX)**:
  Viteの超高速HMR（ホットリロード）により、Canvasの微調整やUI機能の試行錯誤が保存から **50〜100ms** で画面に反映されるため、最も開発効率に優れています。

---

### 2. web (コンポーズWeb版)
* **パス**: `web/`
* **ステータス**: `Experimental`
* **技術スタック**: `Kotlin/WasmJs + Compose Multiplatform for Web (Skia/Canvas)`
* **開発ポート**: `http://localhost:8080/` (または 8081 等)
* **開発コマンド**: `./gradlew :web:wasmJsBrowserDevelopmentRun`
* **ビルドコマンド**: `./gradlew :web:build -x test`
* **概要**:
  全てを Kotlin/Compose Multiplatform で構築することを目指したWebアプリケーション。画面全体が巨大なHTML5 Canvas（Skia）として描画されます。
* **開発体験 (DX)**:
  コード変更のたびに Gradle コンパイルと Webpack パックが走るため、**毎回30秒〜数分**のビルド待ちが発生します。また、Skiaのアセット（Wasmファイル等）が数MB〜10MB規模と非常に巨大で、ブラウザでのフォントレンダリングやセル幅のはみ出しバグも誘発しやすい課題があります。

---

### 3. desktop (コンポーズデスクトップ版)
* **パス**: `desktop/`
* **ステータス**: `Active (確認用ツール)`
* **技術スタック**: `Compose Multiplatform for Desktop (JVM) + Skia`
* **開発コマンド**: `./gradlew :desktop:run`
* **ビルドコマンド**: `./gradlew :desktop:build`
* **概要**:
  DXFファイルを読み込んで画面で正しく表示されるかを確認するための、CADビューアデスクトップアプリケーション。
* **開発体験 (DX)**:
  Wasmコンパイルに比べて起動とJVMコンパイルは高速で、Java HotSwap（動的コード置換）もある程度利用可能です。ただし、Web版（三角形リスト編集）のUIロジックはここには実装されていないため、Web版UIの直接の代わりにはなりません。

---

### 4. trianglelist-web (Rust版)
* **パス**: `trianglelist-web/`
* **ステータス**: `Active (特化ツール)`
* **技術スタック**: `Rust + WASM + Trunk + egui`
* **開発ポート**: `http://localhost:8080/`
* **開発コマンド**: `trunk serve`
* **ビルドコマンド**: `trunk build --release`
* **概要**:
  RustとTrunk、egui（eframe）によって実装された、横断図・切削計算WebUI用の別アプリケーション。`Trunk.toml` にてポート8080が定義されています。

---

## 開発ポータル早見表

| モジュール名 | 技術スタック | ポート | 起動コマンド | 主な役割 |
| :--- | :--- | :--- | :--- | :--- |
| **web-js** | Vite + TS + KMP Wasm | `5173` | `npm run dev` | **メインWeb編集アプリ（最速のDX）** |
| **web** | Compose Multiplatform (Wasm) | `8080` | `./gradlew :web:wasmJsBrowserDevelopmentRun` | 全面Compose Web化の実験 |
| **desktop** | Compose Desktop (JVM) | N/A | `./gradlew :desktop:run` | DXF描画確認用ビューア |
| **trianglelist-web** | Rust + WASM + Trunk | `8080` | `trunk serve` | 横断図・切削計算WebUI |

---

## プロジェクト内ドキュメントインデックス (散乱資料の一元管理)

プロジェクトルートや `doc/` ディレクトリに点在する設計図、レポート、用語集ドキュメントのインデックスです。各ドキュメントへのポータルとして機能します。

### 1. 開発ガイド・基本ルール
* **[CLAUDE.md](file:///C:/Users/yuuji/StudioProjects/trianglelist/CLAUDE.md)**:
  開発に必要な各種コマンド（ビルド、実行、テスト等）と、AIエージェントの作業自動化（Auto-Permission等）に関するルール設定。
* **[doc/COMMIT_WORKFLOW.md](file:///C:/Users/yuuji/StudioProjects/trianglelist/doc/COMMIT_WORKFLOW.md)**:
  Gitコミットメッセージのフォーマットおよびブランチ運用のワークフロー定義。（※ルートから doc/ へ整理移動済み）
* **[README.md](file:///C:/Users/yuuji/StudioProjects/trianglelist/README.md)**:
  アプリケーションの概要と起動手順、基本操作。

### 2. 技術調査・仕様ドキュメント
* **[doc/triangle-continuity-investigation-2025-08-13.md](file:///C:/Users/yuuji/StudioProjects/trianglelist/doc/triangle-continuity-investigation-2025-08-13.md)**:
  三角形の連続接続（Interactive Chain）に関する振る舞いの詳細仕様および境界条件の調査報告。

### 3. 用語集・技術集約
* **[doc/glossary.md](file:///C:/Users/yuuji/StudioProjects/trianglelist/doc/glossary.md)**:
  プロジェクトドメイン用語集のルートインデックス。ここからプラットフォーム個別用語（[Android](file:///C:/Users/yuuji/StudioProjects/trianglelist/doc/android-terms.md), [Gradle](file:///C:/Users/yuuji/StudioProjects/trianglelist/doc/gradle-terms.md), [Kotlin](file:///C:/Users/yuuji/StudioProjects/trianglelist/doc/kotlin-terms.md), [Git](file:///C:/Users/yuuji/StudioProjects/trianglelist/doc/git-terms.md), [Compose](file:///C:/Users/yuuji/StudioProjects/trianglelist/doc/compose-terms.md), [Architecture](file:///C:/Users/yuuji/StudioProjects/trianglelist/doc/architecture-terms.md) など）を網羅。

### 4. 過去のレポート類
* **[doc/ISSUE_STATUS_REPORT_2025-01-09.md](file:///C:/Users/yuuji/StudioProjects/trianglelist/doc/ISSUE_STATUS_REPORT_2025-01-09.md)**:
  過去（2025/01/09）時点の未解決 Issue と解決ロードマップの進捗レポート。（※ルートから doc/ へ整理移動済み）
* **[doc/LINT_ANALYSIS_2025-01-08.md](file:///C:/Users/yuuji/StudioProjects/trianglelist/doc/LINT_ANALYSIS_2025-01-08.md)**:
  過去（2025/01/08）時点の Lint静的解析結果と修正対策のまとめ。（※ルートから doc/ へ整理移動済み）

---

## 直近の作業実績 & ペンディングタスク (2026-10-03)

> [!NOTE]
> 詳細は `doc/development.db` の `work_logs` テーブル（ID: 1）に保存されています。

### 完了した作業 (Done)
1. **幾何バリデーションの偽陽性解消・正常操作保護**:
   - [`GeometryIntegrityValidator.kt`](file:///C:/Users/yuuji/StudioProjects/trianglelist/common/src/commonMain/kotlin/com/jpaver/trianglelist/editmodel/GeometryIntegrityValidator.kt): `ConnectionSide` 1..10（二重断面 BR/BL/CR/CL/BC/CC: 3..8、フロート: 9..10）の完全認識。二重断面・フロート接続を通常基線一致チェックから除外し、誤遮断・診断ログ汚染を根絶。
   - `OVERLAP` / `INWARD_FOLD` を `Severity.WARNING` に降格し、測量実務展開図で必然的に生じる重なりで入力が拒絶されないようモデルガードを是正。
   - `4.11.csv`（実測図面30図形）を用いた回帰テストを追加し、幾何破壊エラー0件（偽陽性ゼロ）を保証。
2. **初回成功体験を導く軽量UI & 視覚的アフォーダンス（合わせ技）**:
   - 過剰なモーダルや常駐Snackbarを撤去し、軽量トースト＋視覚的アフォーダンスに一本化。
   - **B/C辺のパルス発光（視覚的アフォーダンス）**: 初回起動・三角形1個の初期状態で、親三角形のB辺・C辺をシアン（ネオンブルー）色で呼吸するようにパルス明滅（`ValueAnimator`）。初見ユーザーに「この線（B/C）がタップできる」ことを言葉を介さず直感させる。
   - **一言トーストとの合わせ技**: 同時に「💡 辺（BまたはC）をタップしてみよう」を表示。タップした瞬間に点滅は停止して通常選択（黄色）に遷移し、表組みへフォーカス。
   - **プロ用CADとしての静寂性**: 最初の三角形が接続された時点で `has_added_first_triangle` が記録され、以降の通常作業では一切点滅せずプロツールとして静かに機能。
   - アプリ内メニューに「操作ガイド」ダイアログを新設。
   - 全Robolectricテスト・Desktopテスト通過。

### 保留中・次回タスク (Pending / Next Actions)
* **ステータス**: `PENDING_QUOTA_RESET`（週間利用量リセット待ち：約3.5日後）
* **次回着手予定**:
  1. **現場向けダイレクトサポート最小PoC（アプリ ⇔ ターミナル直通パイプ）**:
     - Cloudflare Worker 1ファイル（30行程度）で質問と図面CSVの一時受付エンドポイントを開設。
     - ターミナル側で現場質問を受信し、このエージェントがプロジェクトの全コードベース・ADRを参照して回答を即返信する監視スクリプトの作成。
     - アプリ内メニュー「お問い合わせ」からの送信UIの接続。
  2. **手触り確認後の発展**:
     - スマホ通知（Discord Webhook / Telegram 等）の接続による開発者のリアルタイム監視・介入機能の追加。