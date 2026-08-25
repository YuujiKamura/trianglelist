package com.jpaver.trianglelist

import java.nio.charset.Charset
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/**
 * DXF / SFC の書き出し文字コードが CP932 であることを pin する。
 *
 * 2026-08-25 発見: 「Shift_JIS (CP932)」という前提でコードが書かれていたが、Java では
 * この 2 つは**別のもの**:
 *
 *   Charset.forName("Shift_JIS") / ("SJIS")     → JIS X 0208 のみ。㎡ ① ㈱ を encode できない
 *   Charset.forName("windows-31j") / ("MS932")  → CP932 (NEC/IBM 拡張込み)。encode できる
 *
 * 実害: 図面タイトル下のサブタイトル行は「面積: A=14.94㎡」を出すが、app の DXF/SFC 保存は
 * Shift_JIS だったので **㎡ が `?` に化けて**保存されていた。web (JS の encoding-japanese は
 * CP932) は ㎡ のまま出るので、同じ CSV から app と web で違うファイルが出ていた
 * (WebDrawingExportGoldenTest が「? vs ㎡」の 1 行差として検出)。
 *
 * 日本の CAD (AutoCAD/Jw_cad/BricsCAD 等) が読む SJIS は実質 CP932 なので、CP932 が正。
 */
class DxfCharsetTest {

    private val cp932 = Charset.forName("windows-31j")

    @Test
    fun `CP932 は面積単位と丸数字を encode できる`() {
        val enc = cp932.newEncoder()
        for (ch in listOf('㎡', '①', '㈱')) {
            assertTrue("CP932 が U+${ch.code.toString(16)} を encode できない", enc.canEncode(ch))
        }
    }

    @Test
    fun `Shift_JIS 別名では面積単位が化ける`() {
        // 「Shift_JIS と CP932 は同じ」という誤解を明示的に固定しておく (再発防止)。
        // ここが false に変わったら JDK 側の別名解決が変わったということなので、
        // 書き出し側の charset 指定も見直すこと。
        val sjis = Charset.forName("Shift_JIS")
        assertTrue("Shift_JIS が ㎡ を encode できてしまう", !sjis.newEncoder().canEncode('㎡'))
        assertEquals("Shift_JIS 経由で ㎡ は ? に化ける", "?", String("㎡".toByteArray(sjis), sjis))
    }

    @Test
    fun `面積単位が CP932 で往復する`() {
        val s = "面積: A=14.94㎡"
        assertEquals(s, String(s.toByteArray(cp932), cp932))
    }
}
