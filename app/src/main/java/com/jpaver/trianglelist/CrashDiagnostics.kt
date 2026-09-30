package com.jpaver.trianglelist

import android.util.Log

/**
 * クラッシュ時の診断情報収集機構 (2026-10-01)。
 *
 * Google Play Console (Android Vitals) は例外のクラス名、メッセージ、スタックトレースのみを送信する。
 * 未捕捉例外ハンドラで本クラスをフックし、例外メッセージの先頭に
 * 「直近のユーザー操作履歴 (Breadcrumbs)」と「TriangleList の内部状態ダンプ」を注入することで、
 * Play Console 上でクラッシュ時の状況が一目で特定できるようにする。
 */
object CrashDiagnostics {
    private const val TAG = "CrashDiagnostics"
    private const val MAX_BREADCRUMBS = 20

    private val breadcrumbs = ArrayDeque<String>()
    private var stateProvider: (() -> String)? = null
    private var isInstalled = false

    fun setStateProvider(provider: () -> String) {
        stateProvider = provider
    }

    @Synchronized
    fun log(event: String) {
        if (breadcrumbs.size >= MAX_BREADCRUMBS) {
            breadcrumbs.removeFirst()
        }
        val timestamp = System.currentTimeMillis() % 100000
        breadcrumbs.addLast("[$timestamp] $event")
        Log.d(TAG, "Breadcrumb: $event")
    }

    @Synchronized
    fun buildDump(): String = buildString {
        appendLine("--- STATE DUMP ---")
        val state = try {
            stateProvider?.invoke() ?: "(No state provider)"
        } catch (t: Throwable) {
            "(Failed to get state: ${t.message})"
        }
        appendLine(state)
        appendLine("--- RECENT EVENTS (${breadcrumbs.size}) ---")
        if (breadcrumbs.isEmpty()) {
            appendLine("(None)")
        } else {
            breadcrumbs.forEach { appendLine(it) }
        }
    }

    fun install() {
        if (isInstalled) return
        isInstalled = true

        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val dump = buildDump()
                Log.e(TAG, "FATAL CRASH DETECTED:\n$dump", throwable)

                // 元の例外を包含し、メッセージの先頭にダンプ情報を付与した例外を生成
                val enrichedException = RuntimeException(
                    "CRASH_DIAGNOSTICS:\n$dump\nORIGINAL: ${throwable::class.java.name}: ${throwable.message}",
                    throwable
                )
                originalHandler?.uncaughtException(thread, enrichedException)
            } catch (t: Throwable) {
                // ダンプ生成で二次障害が起きないよう元のハンドラへ安全にフォールバック
                originalHandler?.uncaughtException(thread, throwable)
            }
        }
        Log.i(TAG, "CrashDiagnostics installed successfully.")
    }
}
