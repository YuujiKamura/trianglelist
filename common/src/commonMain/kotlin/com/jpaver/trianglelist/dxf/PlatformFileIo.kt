package com.jpaver.trianglelist.dxf

/**
 * パス指定のテキスト読み込み (MarkingCommandExecutor の analyze コマンド用)。
 * wasmJs 追加に伴い、java.io.File 直叩きを expect/actual に吸収した。
 * JVM (android/desktop) は従来の DxfAnalyzer.analyzeFile と同じ読み方・エラー出力、
 * wasmJs はファイルシステムが無いため null (呼び出し側がメッセージを返す)。
 */
internal expect fun readDxfTextOrNull(path: String): String?
