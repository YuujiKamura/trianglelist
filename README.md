# TriangleList

[![Live Demo](https://img.shields.io/badge/Live_Demo-GitHub_Pages-orange.svg)](https://yuujikamura.github.io/trianglelist/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-Private-red.svg)]()

測量・土木向けに、三角形と台形を接続して土地形状を作図し、 **DXF / SFC / Excel** に出力するアプリ。辺長を直接いじると図形と寸法がその場で連動する対話編集。

Android版: <https://play.google.com/store/apps/details?id=com.jpaver.myapplication/>

ブラウザ版: <https://yuujikamura.github.io/trianglelist/> (インストール不要、AIに作らせている台形編集混成の試作版、バグってる可能性あり)

## 構成

```
app/         Android アプリ (PDF 出力もここ)
common/      Kotlin Multiplatform 共通コア (editmodel / writer / 描画)
desktop/     Desktop DXF プレビュー
web/         Web 版 (Vite + Kotlin/Wasm、GitHub Pages 配信)
docs/adr/    アーキテクチャ決定記録
```

Triangle と Rectangle (台形) は `EditObject` 基底の共通契約を実装する。 writer 側は `DrawingFileWriter` 基底に `DrawPrim` のリストを組んで、各形式 (DXF/SFC/PDF/Web) がそれを翻訳する frontend/backend 分離 (詳細は [`docs/adr/`](docs/adr/))。

## ビルド

```bash
./gradlew assembleDevDebug       # Android
./gradlew :desktop:run           # Desktop プレビュー
./gradlew :common:desktopTest    # 共通コアテスト
cd web && npm install && npm run dev    # Web dev server (localhost:5173)
cd web && npm run build                 # Web 本番ビルド (web/dist/)
```

Web 版 Pages デプロイは `gh workflow run deploy.yml --ref main` で手動起動 (push では公開されない)。

## ライセンス

プライベート。
