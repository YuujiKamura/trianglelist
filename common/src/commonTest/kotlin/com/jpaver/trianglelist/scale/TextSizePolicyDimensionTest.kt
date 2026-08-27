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
}
