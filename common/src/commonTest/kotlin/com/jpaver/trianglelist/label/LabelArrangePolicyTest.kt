package com.jpaver.trianglelist.label

import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * 自動配置の ON/OFF (2026-08-27 user「寸法自動配置は、設定でトグルしてオフに出来たほうが良い」)。
 *
 * 自動が完全でない以上、気に入らない時に切れる口が要る。切ったら**既定配置のまま**に
 * なるだけで、既に確定した配置が巻き戻るわけではない (user が手で直した配置も当然そのまま)。
 */
class LabelArrangePolicyTest {

    private val NL = 10.toChar().toString()

    @AfterTest
    fun restore() {
        LabelArrangePolicy.enabled = true
    }

    private fun chain(): TriangleList = CsvCodec.build(
        CsvCodec.parse(
            buildString {
                append("1,4.0,3.0,3.5,-1,-1").append(NL)
                for (i in 2..8) append("$i,3.0,2.8,3.2,${i - 1},${if (i % 2 == 0) 1 else 2}").append(NL)
            },
        ),
    )

    private fun placement(list: TriangleList): String = buildString {
        list.forEachItem { s ->
            s as Triangle
            append(s.dimHorizontal.a).append(s.dimHorizontal.b).append(s.dimHorizontal.c)
            append("%.4f,%.4f".format(s.pointnumber.x, s.pointnumber.y)).append('|')
        }
    }

    @Test
    fun `既定は ON で配置が動く`() {
        val list = chain()
        val before = placement(list)

        LabelArrangePolicy.enabled = true
        list.arrangeLabelsForDrawing()

        assertNotEquals(before, placement(list), "既定 ON なのに何も動いていない (前提が変わった)")
    }

    @Test
    fun `OFF なら 1 つも動かさない`() {
        val list = chain()
        val before = placement(list)

        LabelArrangePolicy.enabled = false
        list.arrangeLabelsForDrawing()

        assertEquals(before, placement(list), "OFF なのに配置が動いた")
    }

    @Test
    fun `OFF は既に確定した配置を巻き戻さない`() {
        val list = chain()
        LabelArrangePolicy.enabled = true
        list.arrangeLabelsForDrawing()
        val arranged = placement(list)

        LabelArrangePolicy.enabled = false
        list.arrangeLabelsForDrawing()

        assertEquals(arranged, placement(list), "OFF にしたら配置が巻き戻った")
    }
}
