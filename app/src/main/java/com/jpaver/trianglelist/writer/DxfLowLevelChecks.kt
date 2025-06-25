package com.jpaver.trianglelist.writer

import java.io.File
import java.nio.charset.Charset

/**
 * 低レベル（構造）レベルの DXF 検査をまとめたユーティリティ。
 * ファイル内容をそのまま行リストで受け取り、問題があればエラー文字列を返す。
 */
object DxfLowLevelChecks {

    /**
     * @param lines file.readLines() などで取得した全行
     * @return 検出したエラー文字列の一覧（問題が無ければ空リスト）
     */
    fun run(lines: List<String>): List<String> {
        val errs = mutableListOf<String>()

        checkLinePairConsistency(lines, errs)
        checkFinalEof(lines, errs)
        checkNewlineConsistency(lines, errs)

        return errs
    }

    /**
     * @param file file to check
     * @return 検出したエラー文字列の一覧（問題が無ければ空リスト）
     */
    fun run(file: File): List<String> {
        val bytes = file.readBytes()
        val lines = file.readLines(Charset.forName("Shift_JIS")) // read as CP932 for pair checks
        val errs = run(lines).toMutableList()
        checkEncoding(bytes, errs)
        return errs
    }

    /** 各コード・値が 2 行ペアになっているか */
    private fun checkLinePairConsistency(lines: List<String>, errs: MutableList<String>) {
        if (lines.size % 2 != 0) {
            errs.add("DXF line count is odd – group code/value pairs are broken")
        }
        val max = lines.size - lines.size % 2
        var idx = 0
        while (idx < max) {
            val code = lines[idx].trim()
            code.toIntOrNull()?.let {
                // DXF allows group codes 0-999 (standard) and 1000-1071 (extended data/XDATA)
                if (it !in 0..999 && it !in 1000..1071) {
                    errs.add("Invalid group code '$code' at line ${idx + 1}")
                }
            } ?: errs.add("Non-numeric group code '$code' at line ${idx + 1}")
            idx += 2
        }
    }

    /** ファイル終端が 0 / EOF で終わっているか */
    private fun checkFinalEof(lines: List<String>, errs: MutableList<String>) {
        if (lines.size < 2) return
        val lastPair = lines.takeLast(2).map { it.trim() }
        if (!(lastPair[0] == "0" && lastPair[1] == "EOF")) {
            errs.add("File does not end with required 0/EOF marker")
        }
    }

    /** CRLF と LF が混在していないか。とりあえず \r を含む行があるかで判定 */
    private fun checkNewlineConsistency(lines: List<String>, errs: MutableList<String>) {
        var hasCRLF = false
        var hasLF = false
        lines.forEach { line ->
            if (line.endsWith("\r")) hasCRLF = true else hasLF = true
        }
        if (hasCRLF && hasLF) {
            errs.add("Mixed CRLF / LF newline styles detected")
        }
    }

    /** 粗い文字コードチェック。UTF-8 BOM / NUL / UTF-8 3byte シーケンスを検出 */
    private fun checkEncoding(bytes: ByteArray, errs: MutableList<String>) {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            errs.add("File contains UTF-8 BOM – expected Shift-JIS (CP932)")
        }
        if (bytes.contains(0x00)) {
            errs.add("File contains NUL (0x00) byte – possibly UTF-16/32 encoding")
        }
        // simple UTF-8 3-byte pattern detection
        loop@ for (i in 0 until bytes.size - 2) {
            val b1 = bytes[i].toInt() and 0xFF
            if (b1 in 0xE0..0xEF) {
                val b2 = bytes[i + 1].toInt() and 0xFF
                val b3 = bytes[i + 2].toInt() and 0xFF
                if (b2 in 0x80..0xBF && b3 in 0x80..0xBF) {
                    errs.add("UTF-8 multi-byte sequence detected at offset $i – file not CP932")
                    break@loop
                }
            }
        }
    }
} 