package com.jpaver.trianglelist

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.fakes.RoboMenuItem
import org.robolectric.shadows.ShadowPackageManager

/**
 * エミュレータなし (Robolectric、JVM 上のみ) 版のメニュー Intent テスト。
 * androidTest/MainActivityMenuIntentTest (Espresso+実機/エミュレータ) と同じ assert を、
 * 起動経路は onOptionsItemSelected を直接呼ぶ形で JVM 上で検証する
 * (2026-08-16 user 「エミュレータもなしでテストできたりするのか」指示)。
 *
 * メールアプリの有無はホスト環境依存にしない ── ShadowPackageManager に
 * mailto: の resolver を明示登録し、「メールアプリが有る場合」を決定的に再現する。
 * (実機テストでは verify35(AVD) にメールアプリが無く、その分岐は skip 止まりだった。
 * ここでは両方の分岐 — 有る場合をこのファイルで、無い場合の Toast 分岐は
 * androidTest 側の Assume skip の裏返しとして — 別々に検証できる。)
 */
@RunWith(AndroidJUnit4::class)
class MainActivityMenuIntentRobolectricTest {

    private fun shadowPackageManager(): ShadowPackageManager {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return shadowOf(context.packageManager) as ShadowPackageManager
    }

    private fun fakeResolveInfo(pkg: String, cls: String) = ResolveInfo().apply {
        activityInfo = ActivityInfo().apply {
            packageName = pkg
            name = cls
        }
    }

    private fun registerMailtoHandler() {
        shadowPackageManager().addResolveInfoForIntent(
            Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:")),
            fakeResolveInfo("com.fake.mail", "com.fake.mail.ComposeActivity"),
        )
    }

    /** launchViewIntent() も resolveActivity ゲート付きなので、開く先の URL ごとに resolver を登録する。 */
    private fun registerViewUrlHandler(url: String) {
        shadowPackageManager().addResolveInfoForIntent(
            Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)),
            fakeResolveInfo("com.fake.browser", "com.fake.browser.ViewActivity"),
        )
    }

    private fun fireMenuItem(activity: MainActivity, itemId: Int): Intent {
        val handled = activity.onOptionsItemSelected(RoboMenuItem(itemId))
        assertTrue("onOptionsItemSelected が true (処理済み) を返すべき, itemId=$itemId", handled)
        return shadowOf(activity).nextStartedActivity
            ?: error("menu item $itemId で startActivity が呼ばれなかった")
    }

    @Test
    fun rateAppMenuItem_firesPlayStoreViewIntent() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val sent = fireMenuItem(activity, R.id.action_rate_app)
                assertEquals(Intent.ACTION_VIEW, sent.action)
                assertEquals("market://details?id=${activity.packageName}", sent.data.toString())
            }
        }
    }

    @Test
    fun contactMenuItem_firesEmailSendtoIntent_whenMailAppPresent() {
        registerMailtoHandler()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val sent = fireMenuItem(activity, R.id.action_contact)
                assertEquals(Intent.ACTION_SENDTO, sent.action)
                assertEquals("mailto:", sent.data.toString())
                assertEquals(
                    listOf("yuujikamura@gmail.com"),
                    sent.getStringArrayExtra(Intent.EXTRA_EMAIL)?.toList(),
                )
                assertEquals(
                    "【問い合わせ】ヘロンの面積展開図：TriangleList",
                    sent.getStringExtra(Intent.EXTRA_SUBJECT),
                )
            }
        }
    }

    @Test
    fun usageMenuItem_opensBlogUrl() {
        registerViewUrlHandler("https://trianglelist.home.blog")
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val sent = fireMenuItem(activity, R.id.action_usage)
                assertEquals(Intent.ACTION_VIEW, sent.action)
                assertEquals("https://trianglelist.home.blog", sent.data.toString())
            }
        }
    }

    @Test
    fun privacyMenuItem_opensPrivacyPolicyUrl() {
        registerViewUrlHandler(
            "https://trianglelist.home.blog/2023/06/28/%e3%83%97%e3%83%a9%e3%82%a4%e3%83%90%e3%82%b7%e3%83%bc%e3%83%9d%e3%83%aa%e3%82%b7%e3%83%bc%e3%81%ab%e3%81%a4%e3%81%84%e3%81%a6/",
        )
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val sent = fireMenuItem(activity, R.id.action_privacy)
                assertEquals(Intent.ACTION_VIEW, sent.action)
                assertTrue(
                    "実際は ${sent.data}",
                    sent.data.toString().startsWith("https://trianglelist.home.blog/2023/06/28/"),
                )
            }
        }
    }
}
