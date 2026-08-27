package com.jpaver.trianglelist

import com.jpaver.trianglelist.label.LabelMetrics
import org.jetbrains.skia.Font
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

/**
 * アプリ画面 (Android Canvas) が実際に使うフォントの寸法を、実機もエミュレータも使わずに測る。
 *
 * 2026-08-27 user「実機がなくてもAndroidCanvasの見た目を再現する方法はエミュレータ以外には
 * ないのか」。見た目そのものの再現 (layoutlib / Robolectric native graphics) は Windows で
 * 動く保証が無いが、**当たり判定に効くのは字送りの寸法**なので、Android SDK が同梱している
 * プラットフォームフォントの実体を直接測れば足りる。
 *   SDK: platforms/android-29/data/fonts/Roboto-Regular.ttf (端末の既定 sans-serif)
 *
 * ここで測るのは「判定の前提 (MS Gothic 等幅 = 半角 0.5em)」と「アプリが実際に描く字送り」の差。
 * 配置の決定は図面 (MS Gothic) 基準に一本化済みなので、この差は**画面の見え方**に出る。
 */
class AndroidFontMetricsTest {

    private val samples = listOf("5.3", "11.95", "4.70", "0.9", "8.25", "245.55", "2.04")

    private fun sdkFont(name: String): File? {
        val root = File(System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"), "Android/Sdk/platforms")
        if (!root.isDirectory) return null
        return root.listFiles()
            ?.mapNotNull { File(it, "data/fonts/$name").takeIf { f -> f.isFile } }
            ?.firstOrNull()
    }

    @Test
    fun `Android 既定フォントの字送りと判定の前提の差を測る`() {
        val robotoFile = sdkFont("Roboto-Regular.ttf")
        if (robotoFile == null) {
            println("[font] Android SDK のフォントが見つからないので skip")
            return
        }
        val mgr = org.jetbrains.skia.FontMgr.default
        fun load(file: File): Font? =
            mgr.makeFromData(org.jetbrains.skia.Data.makeFromBytes(file.readBytes()))?.let { Font(it, 100f) }
        val roboto = load(robotoFile) ?: run { println("[font] Roboto を読めないので skip"); return }
        val mono = sdkFont("DroidSansMono.ttf")?.let { load(it) }

        // 判定の前提: 半角 1 文字 = 0.5em、em = cap × EM_PER_CAP
        val emPerCap = LabelMetrics.Approximate.EM_PER_CAP
        var worst = 0.0
        for (s in samples) {
            val assumed = s.length * 0.5 * 100.0 * emPerCap   // cap=100 相当での想定字送り
            val actual = roboto.measureTextWidth(s).toDouble() * emPerCap
            val monoW = mono?.measureTextWidth(s)?.toDouble()?.times(emPerCap)
            val ratio = actual / assumed
            worst = maxOf(worst, kotlin.math.abs(ratio - 1.0))
            println(
                "[font] \"%s\" 想定=%.1f Roboto=%.1f 比=%.3f%s".format(
                    s, assumed, actual, ratio,
                    monoW?.let { " | DroidSansMono=%.1f 比=%.3f".format(it, it / assumed) } ?: "",
                ),
            )
        }
        println("[font] 想定からの最大乖離 = %.1f%%".format(worst * 100))

        // 実測 (2026-08-27): Roboto は想定比 0.925〜1.025、最大乖離 7.5%。
        // 判定は余白 (NumberCircleEscape.DEFAULT_CLEARANCE = 10%) を持たせてあるので、
        // この乖離は余白の内側 = **当たり判定としては吸収できている**。
        // 余白を超えたら、避けたはずの所が画面で重なる/その逆が起きる ── そこが行動の境界。
        //
        // 参考: 等幅にすれば揃う、は誤り。DroidSansMono は一律 1.200 (20% 広い) で
        // 乖離がむしろ増える。「等幅であること」と「半角 = 0.5em であること」は別。
        val clearance = com.jpaver.trianglelist.label.NumberCircleEscape.DEFAULT_CLEARANCE.toDouble()
        assertTrue(
            worst < clearance,
            "アプリの字送りが判定の余白 (${clearance * 100}%) を超えて乖離: ${worst * 100}%",
        )
    }
}
