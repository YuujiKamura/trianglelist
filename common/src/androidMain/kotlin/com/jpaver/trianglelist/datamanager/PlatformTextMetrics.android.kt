package com.jpaver.trianglelist.datamanager

/**
 * TODO (2026-08-12): Android 実測は android.graphics.Paint.measureText + Typeface.createFromAsset
 * (さわらびゴシックを asset に同梱) で実装できるはずだが、この session では実機/エミュレータでの
 * 目視検証ができないため未実装のまま null (= TextFit の半角/全角近似にフォールバック) にしている。
 * 実装する際は desktop 版 (PlatformTextMetrics.desktop.kt) と同じ「REF_SIZE で 1 回測って
 * fs へ線形スケール」の形を踏襲する。
 */
actual object PlatformTextMetrics {
    actual fun measureWidthOrNull(text: String, capHeight: CapHeight): Float? = null
}
