package com.jpaver.trianglelist.datamanager

/**
 * TODO (2026-08-12): Web/Wasm 実測は canvas.measureText (2D context) で実装できるはずで、
 * jp_font.ttf 自体は Main.kt が既に fetch して Skia FontMgr に読み込んでいるので素材はある。
 * ただしこの text-size 計算 (DrawingFileWriter/TextFit) は DXF/SFC 生成の commonMain 側で
 * 動く純粋関数で、Main.kt の Compose 側が非同期 fetch した Typeface をここへ橋渡しする経路が
 * まだ無い。この session ではブラウザでの目視検証もできないため未実装のまま null
 * (= TextFit の半角/全角近似にフォールバック) にしている。
 */
actual object PlatformTextMetrics {
    actual fun measureWidthOrNull(text: String, fs: Float): Float? = null
}
