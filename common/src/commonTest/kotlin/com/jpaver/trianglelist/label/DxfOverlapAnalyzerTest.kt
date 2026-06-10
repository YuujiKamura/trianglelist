package com.jpaver.trianglelist.label

import com.jpaver.trianglelist.dxf.DxfCircle
import com.jpaver.trianglelist.dxf.DxfLine
import com.jpaver.trianglelist.dxf.DxfParseResult
import com.jpaver.trianglelist.dxf.DxfText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DxfOverlapAnalyzerTest {

    @Test
    fun `重なりゼロの配置では overlappingTexts もペアも 0 になる`() {
        val parseResult = DxfParseResult(
            lines = listOf(DxfLine(x1 = 100.0, y1 = 100.0, x2 = 110.0, y2 = 100.0)),
            circles = listOf(DxfCircle(centerX = -100.0, centerY = -100.0, radius = 2.0)),
            texts = listOf(
                DxfText(x = 0.0, y = 0.0, text = "AB", height = 1.0),
                DxfText(x = 50.0, y = 50.0, text = "CD", height = 1.0),
            ),
        )

        val report = DxfOverlapAnalyzer.analyze(parseResult)

        assertEquals(2, report.totalTexts)
        assertEquals(0, report.overlappingTexts, "離れた配置で重なりが出るのはおかしい: ${report.pairs}")
        assertTrue(report.pairs.isEmpty(), "ペアは空のはず: ${report.pairs}")
    }

    @Test
    fun `テキストが辺を跨ぐと EDGE 種別のペアが返る`() {
        // 左寄せ "ABCD" height=2 at (0,0) → box は x∈[0,8], y∈[0,2]。x=4 の縦線が貫く
        val parseResult = DxfParseResult(
            lines = listOf(DxfLine(x1 = 4.0, y1 = -5.0, x2 = 4.0, y2 = 5.0)),
            texts = listOf(DxfText(x = 0.0, y = 0.0, text = "ABCD", height = 2.0)),
        )

        val report = DxfOverlapAnalyzer.analyze(parseResult)

        assertEquals(1, report.overlappingTexts)
        assertEquals(1, report.pairs.size, "辺跨ぎ 1 件のはず: ${report.pairs}")
        val pair = report.pairs.single()
        assertEquals("line:0", pair.otherId)
        assertEquals(ObstacleKind.EDGE, pair.otherKind)
        assertEquals("text:0:ABCD", pair.textId)
    }

    @Test
    fun `テキスト同士の重なりは A-B と B-A を 1 ペアに正規化する`() {
        // 両方左寄せ height=1: "ABCD" は x∈[0,4]、"EFGH" は x∈[2,6] → x∈[2,4] で重なる
        val parseResult = DxfParseResult(
            texts = listOf(
                DxfText(x = 0.0, y = 0.0, text = "ABCD", height = 1.0),
                DxfText(x = 2.0, y = 0.5, text = "EFGH", height = 1.0),
            ),
        )

        val report = DxfOverlapAnalyzer.analyze(parseResult)

        assertEquals(2, report.overlappingTexts, "両テキストとも重なり持ちのはず")
        assertEquals(1, report.pairs.size, "A-B と B-A は 1 ペアに正規化されるはず: ${report.pairs}")
        val pair = report.pairs.single()
        assertEquals(ObstacleKind.LABEL, pair.otherKind)
        assertTrue(
            setOf(pair.textId, pair.otherId) == setOf("text:0:ABCD", "text:1:EFGH"),
            "ペアの両端が 2 テキストの id のはず: $pair",
        )
    }

    @Test
    fun `alignH=中央 はアンカーを中心として左右に広がる`() {
        // "ABCD" height=2 → width=8。中央寄せなら box は x∈[-4,4] で x=-2 の縦線にヒット、
        // 左寄せなら x∈[0,8] でヒットしない。同じ線で補正の有無を弁別する
        val edge = DxfLine(x1 = -2.0, y1 = -5.0, x2 = -2.0, y2 = 5.0)
        val centered = DxfParseResult(
            lines = listOf(edge),
            texts = listOf(DxfText(x = 0.0, y = 0.0, text = "ABCD", height = 2.0, alignH = 1, alignV = 2)),
        )
        val leftAligned = DxfParseResult(
            lines = listOf(edge),
            texts = listOf(DxfText(x = 0.0, y = 0.0, text = "ABCD", height = 2.0, alignH = 0, alignV = 2)),
        )

        assertEquals(1, DxfOverlapAnalyzer.analyze(centered).overlappingTexts, "中央寄せは x=-2 の線を含むはず")
        assertEquals(0, DxfOverlapAnalyzer.analyze(leftAligned).overlappingTexts, "左寄せは x=-2 に届かないはず")
    }

    @Test
    fun `回転テキストは回転後の矩形で判定される`() {
        // 中央アンカー "ABCD" height=1 → width=4。無回転なら x∈[-2,2], y∈[-0.5,0.5]。
        // 90 度回転で x∈[-0.5,0.5], y∈[-2,2] になり、y=1.5 の水平線にヒットする
        val edge = DxfLine(x1 = -1.0, y1 = 1.5, x2 = 1.0, y2 = 1.5)
        val rotated = DxfParseResult(
            lines = listOf(edge),
            texts = listOf(DxfText(x = 0.0, y = 0.0, text = "ABCD", height = 1.0, rotation = 90.0, alignH = 1, alignV = 2)),
        )
        val unrotated = DxfParseResult(
            lines = listOf(edge),
            texts = listOf(DxfText(x = 0.0, y = 0.0, text = "ABCD", height = 1.0, rotation = 0.0, alignH = 1, alignV = 2)),
        )

        assertEquals(1, DxfOverlapAnalyzer.analyze(rotated).overlappingTexts, "90 度回転後は y=1.5 に届くはず")
        assertEquals(0, DxfOverlapAnalyzer.analyze(unrotated).overlappingTexts, "無回転は y=1.5 に届かないはず")
    }

    @Test
    fun `textWidthFactor で幅の係数を変えると判定が変わる`() {
        // 左寄せ "AB" height=1 at (0,0): factor=1.0 で x∈[0,2]、factor=2.0 で x∈[0,4]。x=3 の縦線で弁別
        val parseResult = DxfParseResult(
            lines = listOf(DxfLine(x1 = 3.0, y1 = -5.0, x2 = 3.0, y2 = 5.0)),
            texts = listOf(DxfText(x = 0.0, y = 0.0, text = "AB", height = 1.0)),
        )

        assertEquals(0, DxfOverlapAnalyzer.analyze(parseResult, textWidthFactor = 1.0f).overlappingTexts)
        assertEquals(1, DxfOverlapAnalyzer.analyze(parseResult, textWidthFactor = 2.0f).overlappingTexts)
    }

    @Test
    fun `サークルは外接 box の LABEL 障害物としてヒットする`() {
        // 左寄せ "ABCD" height=1 → box x∈[0,4], y∈[0,1]。中心 (5,0.5) 半径 1.5 の円の外接 box は x∈[3.5,6.5]
        val parseResult = DxfParseResult(
            circles = listOf(DxfCircle(centerX = 5.0, centerY = 0.5, radius = 1.5)),
            texts = listOf(DxfText(x = 0.0, y = 0.0, text = "ABCD", height = 1.0)),
        )

        val report = DxfOverlapAnalyzer.analyze(parseResult)

        assertEquals(1, report.pairs.size, "円の外接 box とのペア 1 件のはず: ${report.pairs}")
        assertEquals("circle:0", report.pairs.single().otherId)
        assertEquals(ObstacleKind.LABEL, report.pairs.single().otherKind)
    }
}
