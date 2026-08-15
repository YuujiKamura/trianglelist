package com.jpaver.trianglelist

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.isInternal
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 実機/エミュレータ上で「オーバーフローメニューを開いて実際にタップする」動作を検証する。
 * Play ストア/メールアプリは実際には起動させず、Espresso-Intents で外向き Intent を横取りして
 * 中身 (action/data/extras) を検証する (2026-08-16、user の「実機で押した時の動作テスト、
 * できれば全種類」指示)。今日追加した2件 (評価する/お問い合わせ) に加え、同種の既存メニュー
 * (取扱説明書/個人情報保護方針) も併せて検証する。
 *
 * 前提: 実行端末/エミュレータに Play ストアとメールアプリの両方が入っていること。
 * openPlayStoreListing/openContactMail は intent.resolveActivity で事前チェックしており、
 * どちらのハンドラも無い端末では startActivity 自体が呼ばれず intended() が検出できない
 * (標準の "Google Play" システムイメージのエミュレータであれば両方揃っている)。
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityMenuIntentTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private val targetPackageName: String
        get() = InstrumentationRegistry.getInstrumentation().targetContext.packageName

    @Before
    fun setUp() {
        Intents.init()
        // Play ストア/メールアプリを実際には起動させない (テスト後に端末上へ残らないように)。
        intending(not(isInternal())).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    private fun tapMenuItem(labelResId: Int) {
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        onView(withText(labelResId)).perform(click())
    }

    @Test
    fun rateAppMenuItem_launchesPlayStoreIntentForThisPackage() {
        tapMenuItem(R.string.action_rate_app)

        val sent = Intents.getIntents().last()
        assertEquals(Intent.ACTION_VIEW, sent.action)
        assertNotNull("market:// への data URI が設定されているはず", sent.data)
        assertEquals(
            "market://details?id=$targetPackageName",
            sent.data.toString(),
        )
    }

    @Test
    fun contactMenuItem_launchesEmailIntentWithFixedSubject() {
        // openContactMail() は resolveActivity で事前チェックしており、メール処理可能な
        // アプリが端末に無ければ startActivity を呼ばず Toast のみ出す (これは正しい設計、
        // 2026-08-16 に実機テストで verify35(AVD) が mailto: を解決できないことを発見)。
        // この前提が満たされない端末ではテスト自体を skip する (fail ではなく assumption)。
        val probe = Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:"))
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        org.junit.Assume.assumeTrue(
            "この端末にメール処理可能なアプリが無いため skip (mailto: を解決できない)",
            probe.resolveActivity(targetContext.packageManager) != null,
        )

        tapMenuItem(R.string.action_contact)

        val sent = Intents.getIntents().last()
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

    @Test
    fun usageMenuItem_opensBlogUrl() {
        tapMenuItem(R.string.action_usage)

        intended(allOf(hasAction(Intent.ACTION_VIEW)))
        val sent = Intents.getIntents().last()
        assertEquals("https://trianglelist.home.blog", sent.data.toString())
    }

    @Test
    fun privacyMenuItem_opensPrivacyPolicyUrl() {
        tapMenuItem(R.string.action_privacy)

        intended(allOf(hasAction(Intent.ACTION_VIEW)))
        val sent = Intents.getIntents().last()
        assertTrue(
            "プライバシーポリシー記事のURLが開かれるべき、実際は ${sent.data}",
            sent.data.toString().startsWith("https://trianglelist.home.blog/2023/06/28/"),
        )
    }
}
