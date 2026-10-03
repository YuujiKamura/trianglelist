package com.jpaver.trianglelist

import android.content.Context
import android.os.Build
import android.util.Log
import com.jpaver.trianglelist.editmodel.TriangleList
import java.io.File

/**
 * クラッシュおよびサイレント不具合の診断情報収集機構。
 *
 * 【機能】
 * 1. 直近のユーザー操作履歴 (Breadcrumbs) の記録
 * 2. クラッシュ時の例外メッセージへのダンプ情報注入 (Google Play Console / Android Vitals 対策)
 * 3. サイレントバグ（巻き戻り・幾何不変条件の破綻）の検出と記録 (recordInvariantViolation / checkInvariants)
 * 4. ユーザー問い合わせメールやサポート用の完全な診断レポート生成 (buildReport)
 */
object CrashDiagnostics {
    private const val TAG = "CrashDiagnostics"
    private const val MAX_BREADCRUMBS = 30
    private const val MAX_VIOLATIONS = 10

    private val breadcrumbs = ArrayDeque<String>()
    private val invariantViolations = ArrayDeque<String>()
    private var stateProvider: (() -> String)? = null
    private var isInstalled = false
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

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
    fun recordInvariantViolation(tag: String, message: String) {
        if (invariantViolations.size >= MAX_VIOLATIONS) {
            invariantViolations.removeFirst()
        }
        val timestamp = System.currentTimeMillis() % 100000
        val record = "[$timestamp][$tag] $message"
        invariantViolations.addLast(record)
        log("[INVARIANT_VIOLATION] $record")
        Log.w(TAG, "Invariant violation detected: $record")
    }

    /**
     * TriangleList の幾何学的・論理的不変条件をチェックし、違反があれば記録する。
     * サイレントな巻き戻りや頂点座標のNaN化、親ノード不整合を即座に捕捉する。
     */
    fun checkInvariants(list: TriangleList?): List<String> {
        if (list == null) {
            recordInvariantViolation("Model", "TriangleList is null")
            return listOf("TriangleList is null")
        }

        val errors = mutableListOf<String>()
        val size = list.size()

        if (size < 1) {
            errors.add("TriangleList is empty (size=$size)")
        }

        for (i in 1..size) {
            val t = try {
                list.getBy(i)
            } catch (e: Throwable) {
                errors.add("Failed to get triangle at $i: ${e.message}")
                continue
            }


            if (t.mynumber != i) {
                errors.add("Triangle at $i has mynumber=${t.mynumber}")
            }

            if (i == 1) {
                if (t.parentnumber != -1) {
                    errors.add("Root triangle has parentnumber=${t.parentnumber} (expected -1)")
                }
            } else {
                if (t.parentnumber !in 1 until i) {
                    errors.add("Triangle $i has invalid parentnumber=${t.parentnumber} (must be in 1..${i - 1})")
                }
            }

            val a = t.lengthA_
            val b = t.lengthB_
            val c = t.lengthC_
            if (a <= 0f || b <= 0f || c <= 0f) {
                errors.add("Triangle $i has non-positive side: a=$a, b=$b, c=$c")
            } else if (a + b <= c || b + c <= a || c + a <= b) {
                errors.add("Triangle $i violates triangle inequality: a=$a, b=$b, c=$c")
            }

            val area = t.getArea()
            if (area.isNaN() || area.isInfinite() || area <= 0.0f) {
                errors.add("Triangle $i has invalid area: $area")
            }

            val pt = t.pointnumber
            if (pt.x.isNaN() || pt.x.isInfinite() || pt.y.isNaN() || pt.y.isInfinite()) {
                errors.add("Triangle $i has invalid point coordinates: (${pt.x}, ${pt.y})")
            }
        }

        // 幾何学的完全性・当たり判定・基線一致の検証
        val integrityIssues = com.jpaver.trianglelist.editmodel.GeometryIntegrityValidator.validateList(list)
        for (issue in integrityIssues) {
            if (issue.severity == com.jpaver.trianglelist.editmodel.GeometryIntegrityValidator.Severity.ERROR) {
                errors.add(issue.toString())
            }
        }

        if (errors.isNotEmpty()) {
            recordInvariantViolation("Invariants", errors.joinToString("; "))
        }

        return errors
    }

    @Synchronized
    fun getRecordedViolations(): List<String> = invariantViolations.toList()

    @Synchronized
    fun buildDump(): String = buildString {
        appendLine("--- STATE DUMP ---")
        val state = try {
            stateProvider?.invoke() ?: "(No state provider)"
        } catch (t: Throwable) {
            "(Failed to get state: ${t.message})"
        }
        appendLine(state)

        if (invariantViolations.isNotEmpty()) {
            appendLine("--- INVARIANT VIOLATIONS (${invariantViolations.size}) ---")
            invariantViolations.forEach { appendLine(it) }
        }

        appendLine("--- RECENT EVENTS (${breadcrumbs.size}) ---")
        if (breadcrumbs.isEmpty()) {
            appendLine("(None)")
        } else {
            breadcrumbs.forEach { appendLine(it) }
        }
    }

    /**
     * 問い合わせメールや不具合調査用の詳細レポートを構築する。
     */
    fun buildReport(context: Context? = appContext): String = buildString {
        val ctx = context ?: appContext
        appendLine("--- APPLICATION & DEVICE INFO ---")
        if (ctx != null) {
            try {
                val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ctx.packageManager.getPackageInfo(ctx.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    ctx.packageManager.getPackageInfo(ctx.packageName, 0)
                }
                val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pInfo.versionCode.toLong()
                }
                appendLine("App: ${ctx.packageName} v${pInfo.versionName} ($code)")
            } catch (e: Exception) {
                appendLine("App: Unknown (${e.message})")
            }
        } else {
            appendLine("App: (Context not set)")
        }
        appendLine("OS: Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.PRODUCT})")
        appendLine()
        append(buildDump())
    }

    fun install(context: Context? = null) {
        if (context != null) {
            appContext = context.applicationContext
        }
        if (isInstalled) return
        isInstalled = true

        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val dump = buildDump()
                Log.e(TAG, "FATAL CRASH DETECTED:\n$dump", throwable)

                // 端末内ファイルにも最新クラッシュレポートを永続化
                try {
                    appContext?.let { ctx ->
                        val crashFile = File(ctx.filesDir, "crash_report_latest.txt")
                        crashFile.writeText(buildReport(ctx) + "\n\n--- STACKTRACE ---\n" + Log.getStackTraceString(throwable))
                    }
                } catch (_: Throwable) {}

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
