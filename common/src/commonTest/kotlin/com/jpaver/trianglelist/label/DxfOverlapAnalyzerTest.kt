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
    fun `テキストを辺が貫くと EDGE 種別のペアが深さ付きで返る`() {
        // 半角 "ABCD" height=2 → 幅 = 4×0.5×2×1.299 = 5.196、box は x∈[0,5.196], y∈[0,2]。
        // x=2 の縦線が貫く → 押し出し量は左端まで 2 (右端 3.196 より近い)
        val parseResult = DxfParseResult(
            lines = listOf(DxfLine(x1 = 2.0, y1 = -5.0, x2 = 2.0, y2 = 5.0)),
            texts = listOf(DxfText(x = 0.0, y = 0.0, text = "ABCD", height = 2.0)),
        )

        val report = DxfOverlapAnalyzer.analyze(parseResult)

        assertEquals(1, report.overlappingTexts)
        assertEquals(1, report.pairs.size, "辺貫通 1 件のはず: ${report.pairs}")
        val pair = report.pairs.single()
        assertEquals("line:0", pair.otherId)
        assertEquals(ObstacleKind.EDGE, pair.otherKind)
        assertEquals("text:0:ABCD", pair.textId)
        assertEquals(2f, pair.depthMm, 1e-3f, "中央貫通の押し出し量は 2 のはず: $pair")
    }

    @Test
    fun `自分の辺に寄り添う寸法値配置は深さ 0 の contact になる`() {
        // 中央寄せ・上寄せの "10.0" をアンカーごと辺上に置く = 実図面の寸法値配置。
        // box 上辺が辺と同一線上 → ヒットはするが深さ 0 (めり込みではない)
        val parseResult = DxfParseResult(
            lines = listOf(DxfLine(x1 = -5.0, y1 = 0.0, x2 = 5.0, y2 = 0.0)),
            texts = listOf(DxfText(x = 0.0, y = 0.0, text = "10.0", height = 1.0, alignH = 1, alignV = 3)),
        )

        val report = DxfOverlapAnalyzer.analyze(parseResult)

        assertEquals(1, report.pairs.size, "接触 1 件のはず: ${report.pairs}")
        assertEquals(0f, report.pairs.single().depthMm, 1e-3f, "寄り添い配置の深さは 0 のはず: ${report.pairs}")
    }

    @Test
    fun `テキスト同士の重なりは A-B と B-A を 1 ペアに正規化する`() {
        // 両方左寄せ height=1 (幅 2.598、インク高 1.0): "ABCD" は y∈[0,1]、
        // "EFGH" は y∈[0.5,1.5] → y 方向の食い込み 0.5 が最小軸
        val parseResult = DxfParseResult(
            texts = listOf(
                DxfText(x = 0.0, y = 0.0, text = "ABCD", height = 1.0),
                DxfText(x = 1.0, y = 0.5, text = "EFGH", height = 1.0),
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
        assertEquals(0.5f, pair.depthMm, 1e-3f, "y 方向の食い込み 1.0-0.5=0.5 が最小のはず: $pair")
    }

    @Test
    fun `alignH=中央 はアンカーを中心として左右に広がる`() {
        // "ABCD" height=2 → 幅 5.196。中央寄せなら box は x∈[-2.598,2.598] で x=-1 の縦線にヒット、
        // 左寄せなら x∈[0,5.196] でヒットしない。同じ線で補正の有無を弁別する
        val edge = DxfLine(x1 = -1.0, y1 = -5.0, x2 = -1.0, y2 = 5.0)
        val centered = DxfParseResult(
            lines = listOf(edge),
            texts = listOf(DxfText(x = 0.0, y = 0.0, text = "ABCD", height = 2.0, alignH = 1, alignV = 2)),
        )
        val leftAligned = DxfParseResult(
            lines = listOf(edge),
            texts = listOf(DxfText(x = 0.0, y = 0.0, text = "ABCD", height = 2.0, alignH = 0, alignV = 2)),
        )

        assertEquals(1, DxfOverlapAnalyzer.analyze(centered).overlappingTexts, "中央寄せは x=-1 の線を含むはず")
        assertEquals(0, DxfOverlapAnalyzer.analyze(leftAligned).overlappingTexts, "左寄せは x=-1 に届かないはず")
    }

    @Test
    fun `回転テキストは回転後の矩形で判定される`() {
        // 中央アンカー "ABCD" height=1 → 幅 2.598。無回転なら x∈[-1.299,1.299], y∈[-0.5,0.5]。
        // 90 度回転で x∈[-0.5,0.5], y∈[-1.299,1.299] になり、y=0.8 の水平線にヒットする
        val edge = DxfLine(x1 = -1.0, y1 = 0.8, x2 = 1.0, y2 = 0.8)
        val rotated = DxfParseResult(
            lines = listOf(edge),
            texts = listOf(DxfText(x = 0.0, y = 0.0, text = "ABCD", height = 1.0, rotation = 90.0, alignH = 1, alignV = 2)),
        )
        val unrotated = DxfParseResult(
            lines = listOf(edge),
            texts = listOf(DxfText(x = 0.0, y = 0.0, text = "ABCD", height = 1.0, rotation = 0.0, alignH = 1, alignV = 2)),
        )

        assertEquals(1, DxfOverlapAnalyzer.analyze(rotated).overlappingTexts, "90 度回転後は y=0.8 に届くはず")
        assertEquals(0, DxfOverlapAnalyzer.analyze(unrotated).overlappingTexts, "無回転は y=0.8 に届かないはず")
    }

    @Test
    fun `textWidthFactor で幅の係数を変えると判定が変わる`() {
        // 左寄せ "AB" height=1: factor=1.0 で x∈[0,1.299]、factor=2.0 で x∈[0,2.598]。x=1.5 の縦線で弁別
        val parseResult = DxfParseResult(
            lines = listOf(DxfLine(x1 = 1.5, y1 = -5.0, x2 = 1.5, y2 = 5.0)),
            texts = listOf(DxfText(x = 0.0, y = 0.0, text = "AB", height = 1.0)),
        )

        assertEquals(0, DxfOverlapAnalyzer.analyze(parseResult, textWidthFactor = 1.0f).overlappingTexts)
        assertEquals(1, DxfOverlapAnalyzer.analyze(parseResult, textWidthFactor = 2.0f).overlappingTexts)
    }

    @Test
    fun `全角文字は半角の倍の幅で数える`() {
        // 同じ 2 文字でも "あい" (全角) は幅 2.598、"AB" (半角) は幅 1.299。x=1.5 の縦線で弁別
        val edge = DxfLine(x1 = 1.5, y1 = -5.0, x2 = 1.5, y2 = 5.0)
        val zenkaku = DxfParseResult(
            lines = listOf(edge),
            texts = listOf(DxfText(x = 0.0, y = 0.0, text = "あい", height = 1.0)),
        )
        val hankaku = DxfParseResult(
            lines = listOf(edge),
            texts = listOf(DxfText(x = 0.0, y = 0.0, text = "AB", height = 1.0)),
        )

        assertEquals(1, DxfOverlapAnalyzer.analyze(zenkaku).overlappingTexts, "全角 2 文字は x=1.5 に届くはず")
        assertEquals(0, DxfOverlapAnalyzer.analyze(hankaku).overlappingTexts, "半角 2 文字は x=1.5 に届かないはず")
    }

    @Test
    fun `サークルは円プリミティブの CIRCLE 障害物としてヒットする`() {
        // 左寄せ "ABCD" height=1 → box x∈[0,2.598], y∈[0,1]。中心 (3.598,0.5) r=1.5 の円は
        // box 最近点 (2.598,0.5) まで距離 1 → 深さ 0.5 (外接 box 近似なら 0.5 より過大に出る)
        val parseResult = DxfParseResult(
            circles = listOf(DxfCircle(centerX = 3.598, centerY = 0.5, radius = 1.5)),
            texts = listOf(DxfText(x = 0.0, y = 0.0, text = "ABCD", height = 1.0)),
        )

        val report = DxfOverlapAnalyzer.analyze(parseResult)

        assertEquals(1, report.pairs.size, "円とのペア 1 件のはず: ${report.pairs}")
        val pair = report.pairs.single()
        assertEquals("circle:0", pair.otherId)
        assertEquals(ObstacleKind.CIRCLE, pair.otherKind)
        assertEquals(0.5f, pair.depthMm, 1e-3f, "深さ = r - 最近点距離 = 0.5 のはず: $pair")
    }

    @Test
    fun `LabelMetrics を差し替えると box の幅が実測値になる`() {
        // 固定幅 5mm のインク矩形を返す fake metrics ── 実測実装の差し込み口を pin する
        val fixedWidth = object : LabelMetrics {
            override fun inkBoxLocal(text: String, heightMm: Float, alignH: Int, alignV: Int) =
                LabelMetrics.InkBox(-2.5f, 2.5f, -heightMm / 2f, heightMm / 2f)
        }
        val parseResult = DxfParseResult(
            texts = listOf(DxfText(x = 0.0, y = 0.0, text = "ABCD", height = 2.0, alignH = 1, alignV = 2)),
        )

        val box = DxfOverlapAnalyzer.textBoxes(parseResult, metrics = fixedWidth).single().second

        assertEquals(5.0f, box.widthMm, 1e-3f, "幅は metrics の実測値が使われるはず")
        assertEquals(2.0f, box.heightMm, 1e-3f, "高さは DXF height のまま")
    }

    @Test
    fun `textBoxes は analyze と同じ box を返す (viewer overlay 用の単一経路)`() {
        val parseResult = DxfParseResult(
            texts = listOf(DxfText(x = 0.0, y = 0.0, text = "ABCD", height = 2.0, alignH = 1, alignV = 2)),
        )

        val boxes = DxfOverlapAnalyzer.textBoxes(parseResult)

        assertEquals(1, boxes.size)
        val (id, box) = boxes.single()
        assertEquals("text:0:ABCD", id)
        assertEquals(5.196f, box.widthMm, 1e-3f, "半角 4 文字 × 0.5 × height 2 × em/cap 1.299 = 5.196")
        assertEquals(2f, box.heightMm, 1e-3f, "インク高 = height × 1.0")
        assertEquals(0f, box.center.x, 1e-3f, "中央寄せはアンカーが中心")
        assertEquals(0f, box.center.y, 1e-3f)
    }
}
