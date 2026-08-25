package com.jpaver.trianglelist.label

import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.CycleShape
import com.jpaver.trianglelist.editmodel.EditList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * ModelOverlapAnalyzer: 編集モデル (CsvCodec.buildMixed が組む EditList<CycleShape>) を
 * 直接読んで寸法テキストの重なりを判定する。DxfOverlapAnalyzer と違い DXF の
 * パース結果を経由しない ── エディタが今まさに保持している図形から直接判定できることが
 * 目的 (2026-08-25 user 指摘「そもそも dxf と関係のないモデル層に位置して欲しい」)。
 */
class ModelOverlapAnalyzerTest {

    private fun buildList(csv: String, scale: Float = 1f): EditList<CycleShape> {
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        return CsvCodec.buildMixed(doc, trilist, scale)
    }

    // 寸法値は自分の辺に接して置かれるのが正常配置 (depthMm ≈ 0 の contact) ── これは
    // OverlapReport.contacts 側に落ちる。ここで見るべきは intrusions (depthMm > EPS の
    // 本当のめり込み) だけ (LabelBox.EPS のドキュメント通り)。

    @Test
    fun `十分大きな三角形なら寸法テキストは辺にめり込まない (接触のみ)`() {
        val list = buildList("1,100.0,100.0,100.0,-1,-1\n")

        val report = ModelOverlapAnalyzer.analyze(list, textSize = 0.25f)

        assertEquals(0, report.intrusions.size, "十分大きい三角形でめり込みが出るのはおかしい: ${report.pairs}")
        assertEquals(3, report.contacts.size, "各辺の寸法値はその辺と接触するのが正常配置のはず: ${report.pairs}")
    }

    @Test
    fun `極小三角形では寸法テキストが自分の辺以外ともめり込む`() {
        val list = buildList("1,0.05,0.05,0.05,-1,-1\n")

        val report = ModelOverlapAnalyzer.analyze(list, textSize = 0.25f)

        assertTrue(report.intrusions.isNotEmpty(), "極小三角形でめり込みが検出されないのはおかしい: $report")
    }

    @Test
    fun `同じ図形でも textSize を大きくすればめり込みが増える方向に動く`() {
        val listBig = buildList("1,0.3,0.3,0.3,-1,-1\n")
        val listSmall = buildList("1,0.3,0.3,0.3,-1,-1\n")

        val reportBigText = ModelOverlapAnalyzer.analyze(listBig, textSize = 0.25f)
        val reportSmallText = ModelOverlapAnalyzer.analyze(listSmall, textSize = 0.01f)

        assertTrue(
            reportBigText.intrusions.size >= reportSmallText.intrusions.size,
            "textSize を大きくしてめり込みが減るのはおかしい: big=$reportBigText small=$reportSmallText",
        )
    }

    @Test
    fun `辺を1本だけ縮めると縮めた側のめり込みが増える (図形の可変に追従する)`() {
        // 正三角形の状態からB辺だけを極端に短くする ── 崩れた側の寸法テキストが
        // 隣接辺に寄る/めり込む方を機械的に検出できるかを見る (辺長スイープの最小形)。
        val regular = buildList("1,3.0,3.0,3.0,-1,-1\n")
        val squashed = buildList("1,3.0,0.05,3.0,-1,-1\n")

        val reportRegular = ModelOverlapAnalyzer.analyze(regular, textSize = 0.25f)
        val reportSquashed = ModelOverlapAnalyzer.analyze(squashed, textSize = 0.25f)

        assertTrue(
            reportSquashed.intrusions.size >= reportRegular.intrusions.size,
            "B辺を潰したらめり込みが増えるはず: regular=$reportRegular squashed=$reportSquashed",
        )
    }
}
