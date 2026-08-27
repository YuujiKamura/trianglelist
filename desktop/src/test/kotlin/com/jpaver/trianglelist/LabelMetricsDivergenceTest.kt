package com.jpaver.trianglelist

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.jpaver.trianglelist.adapter.MeasuredLabelMetrics
import com.jpaver.trianglelist.label.LabelMetrics
import org.junit.Test
import kotlin.test.assertTrue

/**
 * 判定に使う近似メトリクス (common、全プラットフォーム共通) と、実際に図面へ描かれる
 * MS Gothic の実測との差を固定する。
 *
 * 2026-08-27 user「すくなくとも当たり判定が食い違うのは最小限にしたい」。
 * 判定と描画がずれると、避けたはずの所が重なる/避けなくていい所を避ける。
 * 差をゼロにはできない (近似は文字送り基準、実測はインク基準) ので、
 * **差の向きと大きさをここで測って固定**し、将来の drift を落とす。
 *
 * 向きの意味:
 *   幅は近似が広め (実測/近似 < 1.0) = 安全側。広く見積もる分、衝突を見逃さない
 *   高さは近似が低め だと危険側 = 縦の当たりを見逃す。1.0 以下に保つ
 */
class LabelMetricsDivergenceTest {

    /** 実データに出てくる寸法値・測点名の代表。 */
    private val samples = listOf("5.3", "11.95", "4.70", "0.9", "8.25", "245.55", "No.1", "No.1+10", "2.04")

    private fun measured(): LabelMetrics {
        val density = Density(1f, 1f)
        return MeasuredLabelMetrics(TextMeasurer(createFontFamilyResolver(), density, LayoutDirection.Ltr), density)
    }

    @Test
    fun `幅は近似が広め (安全側) に収まる`() {
        val m = measured()
        for (s in samples) {
            val a = LabelMetrics.Approximate.inkBoxLocal(s, 250.0, 1, 2)
            val r = m.inkBoxLocal(s, 250.0, 1, 2)
            val ratio = (r.rightMm - r.leftMm) / (a.rightMm - a.leftMm)
            assertTrue(ratio <= 1.0, "\"$s\" で近似幅が実測より狭い (危険側): 比=$ratio")
            assertTrue(ratio > 0.85, "\"$s\" で近似幅が広すぎる (過剰に避ける): 比=$ratio")
        }
    }

    @Test
    fun `高さは近似が実測を下回らない`() {
        // 下回ると縦方向の当たりを見逃す。DIGIT_INK_PER_CAP をここに合わせてある
        val m = measured()
        for (s in samples) {
            val a = LabelMetrics.Approximate.inkBoxLocal(s, 250.0, 1, 2)
            val r = m.inkBoxLocal(s, 250.0, 1, 2)
            val ratio = (r.topMm - r.bottomMm) / (a.topMm - a.bottomMm)
            assertTrue(ratio <= 1.0 + 1e-3, "\"$s\" で近似高さが実測より低い (危険側): 比=$ratio")
        }
    }
}
