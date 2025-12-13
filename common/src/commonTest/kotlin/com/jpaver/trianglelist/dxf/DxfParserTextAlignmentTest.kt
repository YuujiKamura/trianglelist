package com.jpaver.trianglelist.dxf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * DXF TEXT エンティティのアライメント処理テスト
 *
 * DXF仕様:
 * - 第1挿入点 (グループコード 10, 20): alignH=0 AND alignV=0 の場合のみ使用
 * - 第2挿入点 (グループコード 11, 21): それ以外すべて (alignH≠0 OR alignV≠0)
 *
 * alignH (グループコード 72):
 *   0 = 左揃え (Left)
 *   1 = 中央揃え (Center)
 *   2 = 右揃え (Right)
 *
 * alignV (グループコード 73):
 *   0 = ベースライン (Baseline)
 *   1 = 下揃え (Bottom)
 *   2 = 中央揃え (Middle)
 *   3 = 上揃え (Top)
 */
class DxfParserTextAlignmentTest {

    private val parser = DxfParser()

    /**
     * テスト用DXFテキストエンティティを生成
     * @param firstX 第1挿入点X
     * @param firstY 第1挿入点Y
     * @param secondX 第2挿入点X
     * @param secondY 第2挿入点Y
     * @param alignH 水平アライメント
     * @param alignV 垂直アライメント
     */
    private fun createTextDxf(
        firstX: Double, firstY: Double,
        secondX: Double, secondY: Double,
        alignH: Int, alignV: Int,
        text: String = "TEST"
    ): String = """
0
SECTION
2
ENTITIES
0
TEXT
10
$firstX
20
$firstY
11
$secondX
21
$secondY
40
100.0
1
$text
72
$alignH
73
$alignV
0
ENDSEC
0
EOF
""".trimIndent()

    // ===== H0V0: 唯一の第1挿入点使用ケース =====

    @Test
    fun h0v0_uses_first_insertion_point() {
        val dxf = createTextDxf(
            firstX = 100.0, firstY = 200.0,
            secondX = 999.0, secondY = 888.0,  // 無視されるべき
            alignH = 0, alignV = 0
        )
        val result = parser.parse(dxf)
        val textEntity = result.texts.firstOrNull()

        assertNotNull(textEntity, "TEXT entity should be parsed")
        assertEquals(100.0, textEntity.x, "H0V0 should use first insertion point X")
        assertEquals(200.0, textEntity.y, "H0V0 should use first insertion point Y")
        assertEquals(0, textEntity.alignH)
        assertEquals(0, textEntity.alignV)
    }

    // ===== alignH≠0 のケース: 第2挿入点を使用 =====

    @Test
    fun h1v0_uses_second_insertion_point() {
        val dxf = createTextDxf(
            firstX = 100.0, firstY = 200.0,
            secondX = 300.0, secondY = 400.0,
            alignH = 1, alignV = 0
        )
        val result = parser.parse(dxf)
        val textEntity = result.texts.firstOrNull()

        assertNotNull(textEntity)
        assertEquals(300.0, textEntity.x, "H1V0 should use second insertion point X")
        assertEquals(400.0, textEntity.y, "H1V0 should use second insertion point Y")
    }

    @Test
    fun h2v0_uses_second_insertion_point() {
        val dxf = createTextDxf(
            firstX = 100.0, firstY = 200.0,
            secondX = 300.0, secondY = 400.0,
            alignH = 2, alignV = 0
        )
        val result = parser.parse(dxf)
        val textEntity = result.texts.firstOrNull()

        assertNotNull(textEntity)
        assertEquals(300.0, textEntity.x, "H2V0 should use second insertion point X")
        assertEquals(400.0, textEntity.y, "H2V0 should use second insertion point Y")
    }

    // ===== alignV≠0 のケース: 第2挿入点を使用 (H0でも!) =====

    @Test
    fun h0v1_uses_second_insertion_point() {
        val dxf = createTextDxf(
            firstX = 100.0, firstY = 200.0,
            secondX = 300.0, secondY = 400.0,
            alignH = 0, alignV = 1
        )
        val result = parser.parse(dxf)
        val textEntity = result.texts.firstOrNull()

        assertNotNull(textEntity)
        assertEquals(300.0, textEntity.x, "H0V1 should use second insertion point X")
        assertEquals(400.0, textEntity.y, "H0V1 should use second insertion point Y")
    }

    @Test
    fun h0v2_uses_second_insertion_point() {
        val dxf = createTextDxf(
            firstX = 100.0, firstY = 200.0,
            secondX = 300.0, secondY = 400.0,
            alignH = 0, alignV = 2
        )
        val result = parser.parse(dxf)
        val textEntity = result.texts.firstOrNull()

        assertNotNull(textEntity)
        assertEquals(300.0, textEntity.x, "H0V2 should use second insertion point X")
        assertEquals(400.0, textEntity.y, "H0V2 should use second insertion point Y")
    }

    @Test
    fun h0v3_uses_second_insertion_point() {
        val dxf = createTextDxf(
            firstX = 100.0, firstY = 200.0,
            secondX = 300.0, secondY = 400.0,
            alignH = 0, alignV = 3
        )
        val result = parser.parse(dxf)
        val textEntity = result.texts.firstOrNull()

        assertNotNull(textEntity)
        assertEquals(300.0, textEntity.x, "H0V3 should use second insertion point X")
        assertEquals(400.0, textEntity.y, "H0V3 should use second insertion point Y")
    }

    // ===== alignH≠0 AND alignV≠0 のケース =====

    @Test
    fun h1v1_uses_second_insertion_point() {
        val dxf = createTextDxf(
            firstX = 100.0, firstY = 200.0,
            secondX = 300.0, secondY = 400.0,
            alignH = 1, alignV = 1
        )
        val result = parser.parse(dxf)
        val textEntity = result.texts.firstOrNull()

        assertNotNull(textEntity)
        assertEquals(300.0, textEntity.x)
        assertEquals(400.0, textEntity.y)
    }

    @Test
    fun h1v2_uses_second_insertion_point() {
        val dxf = createTextDxf(
            firstX = 100.0, firstY = 200.0,
            secondX = 300.0, secondY = 400.0,
            alignH = 1, alignV = 2
        )
        val result = parser.parse(dxf)
        val textEntity = result.texts.firstOrNull()

        assertNotNull(textEntity)
        assertEquals(300.0, textEntity.x)
        assertEquals(400.0, textEntity.y)
    }

    @Test
    fun h1v3_uses_second_insertion_point() {
        val dxf = createTextDxf(
            firstX = 100.0, firstY = 200.0,
            secondX = 300.0, secondY = 400.0,
            alignH = 1, alignV = 3
        )
        val result = parser.parse(dxf)
        val textEntity = result.texts.firstOrNull()

        assertNotNull(textEntity)
        assertEquals(300.0, textEntity.x)
        assertEquals(400.0, textEntity.y)
    }

    @Test
    fun h2v1_uses_second_insertion_point() {
        val dxf = createTextDxf(
            firstX = 100.0, firstY = 200.0,
            secondX = 300.0, secondY = 400.0,
            alignH = 2, alignV = 1
        )
        val result = parser.parse(dxf)
        val textEntity = result.texts.firstOrNull()

        assertNotNull(textEntity)
        assertEquals(300.0, textEntity.x)
        assertEquals(400.0, textEntity.y)
    }

    @Test
    fun h2v2_uses_second_insertion_point() {
        val dxf = createTextDxf(
            firstX = 100.0, firstY = 200.0,
            secondX = 300.0, secondY = 400.0,
            alignH = 2, alignV = 2
        )
        val result = parser.parse(dxf)
        val textEntity = result.texts.firstOrNull()

        assertNotNull(textEntity)
        assertEquals(300.0, textEntity.x)
        assertEquals(400.0, textEntity.y)
    }

    @Test
    fun h2v3_uses_second_insertion_point() {
        val dxf = createTextDxf(
            firstX = 100.0, firstY = 200.0,
            secondX = 300.0, secondY = 400.0,
            alignH = 2, alignV = 3
        )
        val result = parser.parse(dxf)
        val textEntity = result.texts.firstOrNull()

        assertNotNull(textEntity)
        assertEquals(300.0, textEntity.x)
        assertEquals(400.0, textEntity.y)
    }

    // ===== アライメント値が正しく保存されることを確認 =====

    @Test
    fun alignment_values_are_preserved() {
        for (h in 0..2) {
            for (v in 0..3) {
                val dxf = createTextDxf(
                    firstX = 100.0, firstY = 200.0,
                    secondX = 300.0, secondY = 400.0,
                    alignH = h, alignV = v,
                    text = "H${h}V${v}"
                )
                val result = parser.parse(dxf)
                val textEntity = result.texts.firstOrNull()

                assertNotNull(textEntity, "H${h}V${v} should be parsed")
                assertEquals(h, textEntity.alignH, "alignH should be $h")
                assertEquals(v, textEntity.alignV, "alignV should be $v")
                assertEquals("H${h}V${v}", textEntity.text)
            }
        }
    }
}
