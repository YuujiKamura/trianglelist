package com.jpaver.trianglelist.web

import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.EditList
import com.jpaver.trianglelist.editmodel.CycleShape
import com.jpaver.trianglelist.editmodel.Rectangle
import com.jpaver.trianglelist.editmodel.Triangle
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 2026-06-17 Codex 引き継ぎ tier 2: Rectangle dim override (side 0/1/2 = A/B/C 軸) を
 * WebOverrides.apply(list) で適用する経路の cartesian テスト。
 *
 * 軸 (CLAUDE.md Rule 6):
 *   - Rectangle 配置: [R] / [T, R] / [T, R, T]
 *   - side: 0 / 1 / 2 (D=3 は描画 hardcoded で別 brief、 4=測点は Triangle 専用)
 *   - (h, v)-pattern: h-only / v-only / both
 *
 * 件数 = 3 placements × 3 sides × 3 hv-patterns、 generator で全件 pin (Rule 6
 * 「ケース数は cartesian の積で決まる、 固定数字で書くな」)。 各ケースで
 *   (a) Rectangle.dim<H|V>.<axis> が override 値で更新される
 *   (b) 同 mixed list 内の他図形 (Triangle) の dim 値が適用前と同じ (誤適用なし)
 * を pin。 さらに negative: side 3 (D 辺) override は Rectangle のどの軸にも触れない。
 */
class WebOverridesRectangleTest {

    private data class Placement(val name: String, val csv: String, val rectIdx: Int)

    private val placements = listOf(
        Placement(
            "rect-only-1",
            "Rectangle,1,2.0,3.0,3.0,-1,0,0\n",
            rectIdx = 1,
        ),
        Placement(
            "tri-rect",
            """
                1,6.0,5.0,4.0,-1,-1
                Rectangle,2,2.0,5.0,5.0,1,1,0
            """.trimIndent() + "\n",
            rectIdx = 2,
        ),
        Placement(
            "tri-rect-tri",
            """
                1,6.0,5.0,4.0,-1,-1
                Rectangle,2,2.0,5.0,5.0,1,1,0
                3,4.0,3.5,3.0,1,2
            """.trimIndent() + "\n",
            rectIdx = 2,
        ),
    )

    private data class HvPattern(val label: String, val h: Int?, val v: Int?)

    private val hvPatterns = listOf(
        HvPattern("h-only", h = 4, v = null),  // 最大旗揚げ域
        HvPattern("v-only", h = null, v = 3),  // 内
        HvPattern("both", h = 3, v = 1),       // 旗揚げ + 外
    )

    @Test
    fun `rectangle dim override applied per side (cartesian over placement x side x hv)`() {
        for (p in placements) {
            for (side in 0..2) {
                for (hv in hvPatterns) {
                    val label = "${p.name}/side=$side/${hv.label}"

                    // build twice: baseline で「override なし」の dim を確定、 target で適用後を見る
                    val baseline = buildMixed(p)
                    val target = buildMixed(p)
                    val rectBaseH = horizontalOf(baseline.get(p.rectIdx) as Rectangle, side)
                    val rectBaseV = verticalOf(baseline.get(p.rectIdx) as Rectangle, side)
                    val baseOtherDigest = otherDimDigest(baseline, p.rectIdx)

                    val json = buildDimJson(p.rectIdx, side, hv)
                    WebOverrides.applyJson(target, json)

                    val rect = target.get(p.rectIdx) as Rectangle
                    val expectedH = hv.h ?: rectBaseH
                    val expectedV = hv.v ?: rectBaseV
                    assertEquals(
                        expectedH,
                        horizontalOf(rect, side),
                        "$label: Rectangle.dimHorizontal.<side=$side> expected $expectedH",
                    )
                    assertEquals(
                        expectedV,
                        verticalOf(rect, side),
                        "$label: Rectangle.dimVertical.<side=$side> expected $expectedV",
                    )

                    val afterOtherDigest = otherDimDigest(target, p.rectIdx)
                    assertEquals(
                        baseOtherDigest,
                        afterOtherDigest,
                        "$label: 他図形の dim が変化した (誤適用)",
                    )
                }
            }
        }
    }

    @Test
    fun `side 3 D-edge override does not touch any dim axis on Rectangle`() {
        val p = placements.last()  // tri-rect-tri
        val mixed = buildMixed(p)
        val rect = mixed.get(p.rectIdx) as Rectangle
        val beforeH = Triple(rect.dimHorizontal.a, rect.dimHorizontal.b, rect.dimHorizontal.c)
        val beforeV = Triple(rect.dimVertical.a, rect.dimVertical.b, rect.dimVertical.c)

        WebOverrides.applyJson(
            mixed,
            """{"dims":[{"tri":${p.rectIdx},"side":3,"h":4,"v":3}]}""",
        )

        val afterH = Triple(rect.dimHorizontal.a, rect.dimHorizontal.b, rect.dimHorizontal.c)
        val afterV = Triple(rect.dimVertical.a, rect.dimVertical.b, rect.dimVertical.c)
        assertEquals(beforeH, afterH, "side 3 override は dimHorizontal の a/b/c のどれも変えない")
        assertEquals(beforeV, afterV, "side 3 override は dimVertical の a/b/c のどれも変えない")
    }

    @Test
    fun `side 4 sokuten override does not touch any dim axis on Rectangle`() {
        val p = placements.last()
        val mixed = buildMixed(p)
        val rect = mixed.get(p.rectIdx) as Rectangle
        val beforeH = Triple(rect.dimHorizontal.a, rect.dimHorizontal.b, rect.dimHorizontal.c)
        val beforeV = Triple(rect.dimVertical.a, rect.dimVertical.b, rect.dimVertical.c)

        WebOverrides.applyJson(
            mixed,
            """{"dims":[{"tri":${p.rectIdx},"side":4,"h":4,"v":3}]}""",
        )

        val afterH = Triple(rect.dimHorizontal.a, rect.dimHorizontal.b, rect.dimHorizontal.c)
        val afterV = Triple(rect.dimVertical.a, rect.dimVertical.b, rect.dimVertical.c)
        assertEquals(beforeH, afterH, "side 4 (測点) override は Rectangle 軸を変えない")
        assertEquals(beforeV, afterV, "side 4 (測点) override は Rectangle 軸を変えない")
    }

    // ---- helpers ----

    private fun buildMixed(p: Placement): EditList<CycleShape> {
        val doc = CsvCodec.parse(p.csv)
        val trilist = CsvCodec.build(doc)
        return CsvCodec.buildMixed(doc, trilist, 1f)
    }

    private fun horizontalOf(rect: Rectangle, side: Int): Int = when (side) {
        0 -> rect.dimHorizontal.a
        1 -> rect.dimHorizontal.b
        2 -> rect.dimHorizontal.c
        else -> error("horizontalOf: side $side out of (0..2) test scope")
    }

    private fun verticalOf(rect: Rectangle, side: Int): Int = when (side) {
        0 -> rect.dimVertical.a
        1 -> rect.dimVertical.b
        2 -> rect.dimVertical.c
        else -> error("verticalOf: side $side out of (0..2) test scope")
    }

    private fun buildDimJson(tri: Int, side: Int, hv: HvPattern): String {
        val parts = mutableListOf("\"tri\":$tri", "\"side\":$side")
        hv.h?.let { parts.add("\"h\":$it") }
        hv.v?.let { parts.add("\"v\":$it") }
        return """{"dims":[{${parts.joinToString(",")}}]}"""
    }

    /** 指定 idx 以外の全図形について、dim* の a/b/c (vertical + horizontal) を文字列で digest */
    private fun otherDimDigest(mixed: EditList<CycleShape>, excludeIdx: Int): String {
        val sb = StringBuilder()
        for (i in 1..mixed.size()) {
            if (i == excludeIdx) continue
            when (val obj = mixed.get(i)) {
                is Triangle -> sb.append(
                    "T$i=h${obj.dim.horizontal.a}/${obj.dim.horizontal.b}/${obj.dim.horizontal.c}" +
                        "|v${obj.dim.vertical.a}/${obj.dim.vertical.b}/${obj.dim.vertical.c};",
                )
                is Rectangle -> sb.append(
                    "R$i=h${obj.dimHorizontal.a}/${obj.dimHorizontal.b}/${obj.dimHorizontal.c}" +
                        "|v${obj.dimVertical.a}/${obj.dimVertical.b}/${obj.dimVertical.c};",
                )
                else -> Unit
            }
        }
        return sb.toString()
    }
}
