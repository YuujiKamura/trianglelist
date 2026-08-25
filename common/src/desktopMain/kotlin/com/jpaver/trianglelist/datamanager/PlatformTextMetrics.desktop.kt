package com.jpaver.trianglelist.datamanager

import java.awt.Font
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform

/**
 * さわらびゴシック (SIL OFL 1.1、web 版 Compose 描画で既に使用中の jp_font.ttf と同一ファイルを
 * desktop リソースにも同梱、AwtCadPanel.japaneseFont と共通の資産) で実測する。
 * REF_SIZE で 1 回測って fs へ線形スケールする (フォントの advance は point size に比例するため、
 * 毎回フォントサイズを作り直す必要が無い)。
 */
actual object PlatformTextMetrics {
    private const val REF_SIZE = 100f
    private val frc = FontRenderContext(AffineTransform(), true, true)

    private val font: Font? by lazy {
        try {
            PlatformTextMetrics::class.java.classLoader.getResourceAsStream("jp_font.ttf")?.use { stream ->
                Font.createFont(Font.TRUETYPE_FONT, stream).deriveFont(REF_SIZE)
            }
        } catch (e: Exception) {
            println("PlatformTextMetrics: さわらびゴシック読み込み失敗、heuristic にフォールバック: ${e.message}")
            null
        }
    }

    /**
     * このフォントのキャップハイト / em 比を 'A' のインク実高から実測する。
     *
     * AWT の font size は em、TextFit が渡してくるのはキャップハイト (DXF group code 40 と
     * 同じ量) ── 別の量なので、そのまま deriveFont に渡すと advance を約 25% 過小に返す。
     * 比は フォント固有の性質なので定数を置かず測る (TextRenderer が Skia で行っているのと
     * 同じ、ezdxf の make_font(name, cap_height) / QCAD RTextRenderer と同型)。
     */
    private val capHeightRatio: Float by lazy {
        val f = font ?: return@lazy TextFit.ASSUMED_CAP_HEIGHT_RATIO
        val ink = TextLayout("A", f, frc).bounds.height.toFloat()
        if (ink > 0f) ink / REF_SIZE else TextFit.ASSUMED_CAP_HEIGHT_RATIO
    }

    actual fun measureWidthOrNull(text: String, capHeight: CapHeight): Float? {
        if (text.isEmpty()) return 0f
        val f = font ?: return null
        val advance = TextLayout(text, f, frc).advance
        // advance は REF_SIZE (em) で測った値なので、em に直してから比例させる。
        // toEm を通さずに capHeight を使うと約 25% 過小になる ── 型がそれを防ぐ。
        return advance / REF_SIZE * capHeight.toEm(capHeightRatio).value
    }
}
