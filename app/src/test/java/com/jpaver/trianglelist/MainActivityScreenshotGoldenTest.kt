package com.jpaver.trianglelist

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 起動画面の画像 golden (Roborazzi、エミュレータなし)。
 *
 * 起動直後の画面 (描画 canvas + 編集テーブル + FAB 群) を Pixel5 相当の
 * 画面構成で固定し、見た目のリグレッションを JVM 上で検出する。
 * desktop の AwtCadPanelImageGoldenTest (golden-img 配下の png) と同じ
 * 「画像 golden を repo に固定する」第2層の Android 版。
 *
 * 運用:
 * - 通常の testDevDebugUnitTest では capture は no-op (コスト無し)
 * - 更新: ./gradlew :app:recordRoborazziDevDebug
 * - 照合: ./gradlew :app:verifyRoborazziDevDebug
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class MainActivityScreenshotGoldenTest {

    @Test
    fun launchScreen_matchesGolden() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(isRoot()).captureRoboImage(
                filePath = "src/test/golden-img/main-activity-launch.png"
            )
        }
    }
}
