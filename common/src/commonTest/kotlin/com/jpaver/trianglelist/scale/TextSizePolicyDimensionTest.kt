package com.jpaver.trianglelist.scale

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 寸法値の「紙面基準の大きさ」を model 単位に落とす換算。
 *
 * 2026-08-27 user 指示「紙面サイズに対して視認が出来る最低限のテキストサイズというのが
 * まずあって、その時点でどうか？を考えればそれでいい」── 文字サイズは掃くパラメータでは
 * なく紙面基準で決まる 1 点。その 1 点を計算する口をここに置く。
 *
 * 現状の寸法値は TextScaleCalculator の固定表 (1/150 → 0.25 model = 紙 1.667mm) から
 * 来ていて JIS 3.5mm に対し 2.1 倍小さい。既定の切り替えは自動退避が入ってから
 * (今切り替えると重なったままの図面が出る) なので、まずは「JIS ならいくつか」を
 * 計算できる状態にする。
 */
class TextSizePolicyDimensionTest {

    @Test
    fun `JIS 寸法値 3_5mm を model 単位に直す`() {
        // 図面単位は m (DXF 書き出しで ×1000 して mm になる)。1/150 なら
        // 3.5mm × 150 = 525mm = 0.525 model
        assertEquals(0.525f, TextSizePolicy.dimensionModelSize(150f), 1e-5f)
        assertEquals(0.175f, TextSizePolicy.dimensionModelSize(50f), 1e-5f)
        assertEquals(2.1f, TextSizePolicy.dimensionModelSize(600f), 1e-4f)
    }

    @Test
    fun `縮尺分母 0 なら 0 を返す (0 除算で NaN 座標を撒かない)`() {
        assertEquals(0f, TextSizePolicy.dimensionModelSize(0f), 1e-6f)
    }

    @Test
    fun `model 単位から紙 mm への往復が保たれる`() {
        val denominator = 150f
        val model = TextSizePolicy.dimensionModelSize(denominator)
        assertEquals(
            TextSizePolicy.DIMENSION_PAPER_MM,
            TextSizePolicy.modelToPaper(model * TextSizePolicy.MM_PER_MODEL_UNIT, denominator),
            1e-4f,
        )
    }

    @Test
    fun `model 単位の文字サイズを紙 mm に直す`() {
        // 2026-08-27: アプリ画面に「紙面での寸法値の大きさ」を出すための換算。
        // 単位の取り違えがこのセッションの事故の主犯なので、往復をテストで固定する。
        // 1/150 図面で model 0.48 (アプリが DXF に書く既定値 = myview.textSize 30 × 0.016) は
        // 紙 3.2mm ── JIS の 3.5mm にほぼ乗っている
        assertEquals(3.2f, TextSizePolicy.modelSizeToPaperMm(0.48f, 150f), 1e-4f)
        // 一方、配置の判定に使っていた getPrintTextScale の 0.25 は紙 1.667mm = 基準の半分以下
        assertEquals(1.6667f, TextSizePolicy.modelSizeToPaperMm(0.25f, 150f), 1e-3f)
        assertEquals(0f, TextSizePolicy.modelSizeToPaperMm(0.48f, 0f), 1e-6f)
    }

    @Test
    fun `dimensionModelSize と modelSizeToPaperMm は逆演算`() {
        for (denominator in kotlin.collections.listOf(50f, 150f, 600f)) {
            val model = TextSizePolicy.dimensionModelSize(denominator)
            assertEquals(
                TextSizePolicy.DIMENSION_PAPER_MM,
                TextSizePolicy.modelSizeToPaperMm(model, denominator),
                1e-3f,
                "縮尺 1/$denominator で往復しない",
            )
        }
    }

    @Test
    fun `紙 mm から model 単位への逆換算`() {
        // 2026-08-27 user「スイートスポットである3.5mmとかが設定できるようにしたほうが良い」。
        // 画面から「紙で 3.5mm」を選べるようにするための逆演算。往復で戻ることを固定する
        for (denominator in kotlin.collections.listOf(50f, 150f, 300f, 600f)) {
            for (paperMm in kotlin.collections.listOf(2.5f, 3.5f, 5f, 7f, 10f)) {
                val model = TextSizePolicy.paperMmToModelSize(paperMm, denominator)
                assertEquals(
                    paperMm, TextSizePolicy.modelSizeToPaperMm(model, denominator), 1e-3f,
                    "1/$denominator の $paperMm mm で往復しない",
                )
            }
        }
        assertEquals(0f, TextSizePolicy.paperMmToModelSize(3.5f, 0f), 1e-6f)
    }

    @Test
    fun `JIS の呼び寸法階段を持っている`() {
        // 選択肢を各画面で手書きすると増減時にずれる。階段は policy が持つ
        assertEquals(
            kotlin.collections.listOf(2.5f, 3.5f, 5f, 7f, 10f, 14f, 20f),
            TextSizePolicy.PAPER_MM_LADDER,
        )
        assertEquals(true, TextSizePolicy.DIMENSION_PAPER_MM in TextSizePolicy.PAPER_MM_LADDER)
    }
}
