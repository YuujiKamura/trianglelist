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

    actual fun measureWidthOrNull(text: String, fs: Float): Float? {
        if (text.isEmpty()) return 0f
        val f = font ?: return null
        val advance = TextLayout(text, f, frc).advance
        return advance / REF_SIZE * fs
    }
}
