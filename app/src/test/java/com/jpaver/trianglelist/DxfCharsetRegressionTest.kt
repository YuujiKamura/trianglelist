package com.jpaver.trianglelist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.charset.Charset

/**
 * 図面 I/O の charset 不変条件。
 *
 * "Shift_JIS" / "Shift-JIS" は JIS X 0208 のみで ㎡ (U+33A1) / ① / ㈱ を encode できず
 * `?` に落とす。図面は面積欄に ㎡ を必ず含むため、DXF/CSV の読み書きは CP932
 * (= Java の "windows-31j") でなければならない (DxfFileWriterJvm.kt:12-13 の警告)。
 *
 * 2026-08-27: MainActivity の保存/読込 6 箇所が "Shift-JIS" のままで、アプリから
 * 保存した DXF の ㎡ が `?` に化けていた。同じ退行を二度踏まないための pin。
 */
class DxfCharsetRegressionTest {

    private val cp932 = Charset.forName("windows-31j")

    /** 図面テキストに実際に出る、Shift_JIS では表現できない文字 */
    private val riskyChars = listOf("㎡" to 0x33A1, "①" to 0x2460, "㈱" to 0x3231)

    @Test
    fun `windows-31j は図面が使う特殊文字を往復できる`() {
        for ((ch, code) in riskyChars) {
            val bytes = ch.toByteArray(cp932)
            assertTrue("U+%04X が CP932 で `?` に化けた".format(code), bytes.none { it == '?'.code.toByte() })
            assertEquals("U+%04X の往復が壊れた".format(code), ch, String(bytes, cp932))
        }
    }

    @Test
    fun `Shift_JIS では化けることを示す（この差が本テストの存在理由）`() {
        val bytes = "㎡".toByteArray(Charset.forName("Shift_JIS"))
        assertTrue("Shift_JIS が ㎡ を通してしまう — 前提が変わったので本テストを見直せ",
            bytes.any { it == '?'.code.toByte() })
    }

    @Test
    fun `MainActivity の図面 I-O に Shift-JIS 指定が残っていない`() {
        val src = File("src/main/java/com/jpaver/trianglelist/MainActivity.kt")
        assertTrue("MainActivity.kt が見つからない: ${src.absolutePath}", src.exists())
        val offenders = src.readLines().withIndex().filter { (_, line) ->
            line.contains("\"Shift-JIS\"") || line.contains("\"Shift_JIS\"")
        }
        assertTrue(
            "図面 I/O に Shift-JIS 指定が残っている (windows-31j を使え):\n" +
                offenders.joinToString("\n") { (i, l) -> "  L${i + 1}: ${l.trim()}" },
            offenders.isEmpty()
        )
    }
}
