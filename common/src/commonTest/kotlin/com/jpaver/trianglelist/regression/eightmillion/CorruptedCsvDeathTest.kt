package com.jpaver.trianglelist.regression.eightmillion

import com.jpaver.trianglelist.datamanager.CsvCodec
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 【エイトミリオンウェイストゥダイ：汚染・破損CSVデータ編】
 *
 * 0バイトファイル、壊れたヘッダ、不足カラム、非数値文字列、途中で切断された行、
 * ランダムビット反転ファジングなど、最悪のファイル入力に対する耐性を検証する。
 */
class CorruptedCsvDeathTest {

    @Test
    fun `Death_21_完全空CSV_0バイト文字列でもクラッシュしない`() {
        val doc = CsvCodec.parse("")
        assertNotNull(doc)
        val model = CsvCodec.build(doc, applyRecoverState = false)
        assertNotNull(model)
        // 空の場合はサイズ0のリストになる
        assertEquals(0, model.size())
    }

    @Test
    fun `Death_22_改行と空白のみのCSV_クラッシュせず無視できる`() {
        val whitespaceCsv = "\n\n   \r\n \t \n\n"
        val doc = CsvCodec.parse(whitespaceCsv)
        assertNotNull(doc)
        val model = CsvCodec.build(doc, applyRecoverState = false)
        assertEquals(0, model.size())
    }

    @Test
    fun `Death_23_非数値文字列の混入_文字列が数値カラムに入っていてもクラッシュしない`() {
        val dirtyCsv = """
            1, not_a_number, NaN, Infinity, -1, 1
            2, 5.0, 4.0, 3.0, 1, 1
        """.trimIndent()

        val doc = CsvCodec.parse(dirtyCsv)
        // 1行目は不正でスキップされるか、またはクラッシュしない
        val model = CsvCodec.build(doc, applyRecoverState = false)
        assertNotNull(model)
    }

    @Test
    fun `Death_24_不足カラム_カンマが足りない行でも安全に処理される`() {
        val shortCsv = """
            1, 6.0
            2
            3, 4.0, 3.0
        """.trimIndent()

        val doc = CsvCodec.parse(shortCsv)
        val model = CsvCodec.build(doc, applyRecoverState = false)
        assertNotNull(model)
    }

    @Test
    fun `Death_25_存在しない親番号_親が9999でもインデックス例外を起こさない`() {
        val orphanCsv = """
            1, 6.0, 5.0, 4.0, -1, 0
            2, 5.0, 4.0, 3.0, 9999, 1
        """.trimIndent()

        val doc = CsvCodec.parse(orphanCsv)
        val model = CsvCodec.build(doc, applyRecoverState = false)
        // 2行目は親が不正なためスキップされ、1番のみ残るはず
        assertEquals(1, model.size())
    }

    @Test
    fun `Death_26_巨大な列数_余計なカンマが100個あってもクラッシュしない`() {
        val hugeColumns = (1..100).joinToString(",") { "extra_$it" }
        val wideCsv = "1, 6.0, 5.0, 4.0, -1, 0, $hugeColumns"

        val doc = CsvCodec.parse(wideCsv)
        val model = CsvCodec.build(doc, applyRecoverState = false)
        assertEquals(1, model.size())
    }

    @Test
    fun `Death_27_バイナリゴミ混入_ヌルバイトや制御文字でも例外落ちしない`() {
        val binaryCsv = "\u0000\u0001\u00021, 6.0, 5.0, 4.0\u0000, -1, 1\u001F"
        val doc = CsvCodec.parse(binaryCsv)
        val model = CsvCodec.build(doc, applyRecoverState = false)
        assertNotNull(model)
    }

    @Test
    fun `Death_28_ランダムビット破損ファジング_有効CSVを30回破壊しても生還する`() {
        val baseCsv = """
            1, 6.000, 5.000, 4.000, -1, 1, P1, 0, 0, 0
            2, 5.000, 4.000, 3.000, 1, 1, P2, 0, 0, 0
            3, 4.000, 3.500, 3.000, 1, 2, P3, 0, 0, 0
        """.trimIndent()

        val random = Random(12345)

        for (trial in 1..30) {
            // 文字列の一部をランダムに置換・削除・挿入
            val chars = baseCsv.toCharArray()
            val mutateCount = random.nextInt(1, 10)
            for (m in 0 until mutateCount) {
                val idx = random.nextInt(chars.size)
                when (random.nextInt(3)) {
                    0 -> chars[idx] = '\u0000'
                    1 -> chars[idx] = ','
                    2 -> chars[idx] = ('a'..'z').random(random)
                }
            }
            val mutated = String(chars)

            try {
                val doc = CsvCodec.parse(mutated)
                val model = CsvCodec.build(doc, applyRecoverState = false)
                assertNotNull(model)
            } catch (t: Throwable) {
                // JVM Error や Fatal 停止ではなく、安全な例外処理であること
                assertFalse(t is OutOfMemoryError, "Fuzzing caused OutOfMemoryError!")
                assertFalse(t is StackOverflowError, "Fuzzing caused StackOverflowError!")
            }
        }
    }
}
