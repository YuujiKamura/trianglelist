package com.jpaver.trianglelist.dxf

internal actual fun readDxfTextOrNull(path: String): String? {
    // ブラウザ (wasmJs) にはパス指定のファイルシステムが無い。
    // Web ではファイル内容を文字列で渡す経路 (DxfAnalyzer.analyze) を使うこと
    println("readDxfTextOrNull is not supported on wasmJs: $path")
    return null
}
