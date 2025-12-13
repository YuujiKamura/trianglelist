package com.jpaver.trianglelist.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.math.abs
import kotlin.math.pow

/**
 * 座標変換ユーティリティのテスト
 *
 * 座標系の関係:
 * - screenPos = modelPos * scale + offset
 * - modelPos = (screenPos - offset) / scale
 */
class CoordinateTransformTest {

    private val delta = 0.0001f

    private fun assertFloatEquals(expected: Float, actual: Float, message: String = "") {
        assertFloatEqualsWithDelta(expected, actual, delta, message)
    }

    private fun assertFloatEqualsWithDelta(expected: Float, actual: Float, tolerance: Float, message: String = "") {
        val diff = abs(expected - actual)
        if (diff > tolerance) {
            throw AssertionError("$message Expected: $expected, Actual: $actual, Diff: $diff, Tolerance: $tolerance")
        }
    }

    /**
     * 相対誤差を考慮した比較（大きな値や極小スケールで使用）
     * relativeError = |actual - expected| / max(|expected|, |actual|, 1)
     */
    private fun assertFloatEqualsRelative(expected: Float, actual: Float, relativeEpsilon: Float = 0.0001f, message: String = "") {
        val diff = abs(expected - actual)
        val maxAbs = maxOf(abs(expected), abs(actual), 1f)
        val relativeError = diff / maxAbs
        if (relativeError > relativeEpsilon) {
            throw AssertionError("$message Expected: $expected, Actual: $actual, RelativeError: $relativeError (max: $relativeEpsilon)")
        }
    }

    // ===== 基本変換テスト =====

    @Test
    fun modelToView_scale1_offset0() {
        val (viewX, viewY) = CoordinateTransform.modelToView(
            modelX = 100f, modelY = 200f,
            scale = 1f,
            offsetX = 0f, offsetY = 0f
        )
        assertFloatEquals(100f, viewX, "X")
        assertFloatEquals(200f, viewY, "Y")
    }

    @Test
    fun modelToView_scale2_offset0() {
        val (viewX, viewY) = CoordinateTransform.modelToView(
            modelX = 100f, modelY = 200f,
            scale = 2f,
            offsetX = 0f, offsetY = 0f
        )
        assertFloatEquals(200f, viewX, "X should be doubled")
        assertFloatEquals(400f, viewY, "Y should be doubled")
    }

    @Test
    fun modelToView_scale1_withOffset() {
        val (viewX, viewY) = CoordinateTransform.modelToView(
            modelX = 100f, modelY = 200f,
            scale = 1f,
            offsetX = 50f, offsetY = -30f
        )
        assertFloatEquals(150f, viewX, "X + offset")
        assertFloatEquals(170f, viewY, "Y + offset")
    }

    @Test
    fun modelToView_scaleAndOffset() {
        val (viewX, viewY) = CoordinateTransform.modelToView(
            modelX = 100f, modelY = 200f,
            scale = 0.5f,
            offsetX = 100f, offsetY = 50f
        )
        // viewX = 100 * 0.5 + 100 = 150
        // viewY = 200 * 0.5 + 50 = 150
        assertFloatEquals(150f, viewX)
        assertFloatEquals(150f, viewY)
    }

    @Test
    fun viewToModel_scale1_offset0() {
        val (modelX, modelY) = CoordinateTransform.viewToModel(
            viewX = 100f, viewY = 200f,
            scale = 1f,
            offsetX = 0f, offsetY = 0f
        )
        assertFloatEquals(100f, modelX)
        assertFloatEquals(200f, modelY)
    }

    @Test
    fun viewToModel_scale2_offset0() {
        val (modelX, modelY) = CoordinateTransform.viewToModel(
            viewX = 200f, viewY = 400f,
            scale = 2f,
            offsetX = 0f, offsetY = 0f
        )
        assertFloatEquals(100f, modelX, "X should be halved")
        assertFloatEquals(200f, modelY, "Y should be halved")
    }

    @Test
    fun viewToModel_scale1_withOffset() {
        val (modelX, modelY) = CoordinateTransform.viewToModel(
            viewX = 150f, viewY = 170f,
            scale = 1f,
            offsetX = 50f, offsetY = -30f
        )
        assertFloatEquals(100f, modelX)
        assertFloatEquals(200f, modelY)
    }

    // ===== 逆変換の一貫性テスト =====

    @Test
    fun roundTrip_modelToViewToModel() {
        val originalModelX = 12345f
        val originalModelY = -6789f
        val scale = 0.00123f
        val offsetX = 456f
        val offsetY = -789f

        val (viewX, viewY) = CoordinateTransform.modelToView(
            originalModelX, originalModelY, scale, offsetX, offsetY
        )
        val (modelX, modelY) = CoordinateTransform.viewToModel(
            viewX, viewY, scale, offsetX, offsetY
        )

        // 極小スケールでの往復では浮動小数点誤差が蓄積するため相対誤差で比較
        assertFloatEqualsRelative(originalModelX, modelX, 0.0001f, "Model X should survive round trip")
        assertFloatEqualsRelative(originalModelY, modelY, 0.0001f, "Model Y should survive round trip")
    }

    @Test
    fun roundTrip_viewToModelToView() {
        val originalViewX = 500f
        val originalViewY = 300f
        val scale = 2.5f
        val offsetX = -100f
        val offsetY = 200f

        val (modelX, modelY) = CoordinateTransform.viewToModel(
            originalViewX, originalViewY, scale, offsetX, offsetY
        )
        val (viewX, viewY) = CoordinateTransform.modelToView(
            modelX, modelY, scale, offsetX, offsetY
        )

        assertFloatEquals(originalViewX, viewX, "View X should survive round trip")
        assertFloatEquals(originalViewY, viewY, "View Y should survive round trip")
    }

    // ===== ビュー中心のテスト =====

    @Test
    fun getViewCenterInModel_centered() {
        // View: 800x600, offset (0,0), scale 1
        // View center = (400, 300)
        // Model center = (400, 300)
        val (modelX, modelY) = CoordinateTransform.getViewCenterInModel(
            viewWidth = 800f, viewHeight = 600f,
            scale = 1f,
            offsetX = 0f, offsetY = 0f
        )
        assertFloatEquals(400f, modelX)
        assertFloatEquals(300f, modelY)
    }

    @Test
    fun getViewCenterInModel_withOffset() {
        // View: 800x600, offset (100, 50), scale 1
        // View center = (400, 300)
        // Model center = (400 - 100, 300 - 50) = (300, 250)
        val (modelX, modelY) = CoordinateTransform.getViewCenterInModel(
            viewWidth = 800f, viewHeight = 600f,
            scale = 1f,
            offsetX = 100f, offsetY = 50f
        )
        assertFloatEquals(300f, modelX)
        assertFloatEquals(250f, modelY)
    }

    @Test
    fun getViewCenterInModel_withScale() {
        // View: 800x600, offset (0,0), scale 2
        // View center = (400, 300)
        // Model center = (400/2, 300/2) = (200, 150)
        val (modelX, modelY) = CoordinateTransform.getViewCenterInModel(
            viewWidth = 800f, viewHeight = 600f,
            scale = 2f,
            offsetX = 0f, offsetY = 0f
        )
        assertFloatEquals(200f, modelX)
        assertFloatEquals(150f, modelY)
    }

    @Test
    fun getViewCenterInModel_withScaleAndOffset() {
        // View: 800x600, offset (200, 100), scale 0.5
        // View center = (400, 300)
        // Model center = ((400 - 200) / 0.5, (300 - 100) / 0.5) = (400, 400)
        val (modelX, modelY) = CoordinateTransform.getViewCenterInModel(
            viewWidth = 800f, viewHeight = 600f,
            scale = 0.5f,
            offsetX = 200f, offsetY = 100f
        )
        assertFloatEquals(400f, modelX)
        assertFloatEquals(400f, modelY)
    }

    // ===== モデル座標をビュー中心に配置するテスト =====

    @Test
    fun calculateOffsetToCenterModel_origin() {
        // モデル原点(0,0)をビュー中心に配置
        val (offsetX, offsetY) = CoordinateTransform.calculateOffsetToCenterModel(
            modelX = 0f, modelY = 0f,
            viewWidth = 800f, viewHeight = 600f,
            scale = 1f
        )
        // offset = viewCenter - model * scale = (400, 300) - (0, 0) = (400, 300)
        assertFloatEquals(400f, offsetX)
        assertFloatEquals(300f, offsetY)
    }

    @Test
    fun calculateOffsetToCenterModel_arbitrary() {
        // モデル(1000, 500)をビュー中心に配置, scale = 0.5
        val (offsetX, offsetY) = CoordinateTransform.calculateOffsetToCenterModel(
            modelX = 1000f, modelY = 500f,
            viewWidth = 800f, viewHeight = 600f,
            scale = 0.5f
        )
        // offset = viewCenter - model * scale
        // offsetX = 400 - 1000 * 0.5 = 400 - 500 = -100
        // offsetY = 300 - 500 * 0.5 = 300 - 250 = 50
        assertFloatEquals(-100f, offsetX)
        assertFloatEquals(50f, offsetY)
    }

    @Test
    fun calculateOffsetToCenterModel_verification() {
        // 計算したオフセットでモデル座標がビュー中心に来ることを検証
        val modelX = 12345f
        val modelY = -6789f
        val viewWidth = 1920f
        val viewHeight = 1080f
        val scale = 0.01f

        val (offsetX, offsetY) = CoordinateTransform.calculateOffsetToCenterModel(
            modelX, modelY, viewWidth, viewHeight, scale
        )

        // このオフセットでモデル座標をビューに変換するとビュー中心になるはず
        val (viewX, viewY) = CoordinateTransform.modelToView(
            modelX, modelY, scale, offsetX, offsetY
        )

        assertFloatEquals(viewWidth / 2f, viewX, "Model should be at view center X")
        assertFloatEquals(viewHeight / 2f, viewY, "Model should be at view center Y")
    }

    // ===== ズーム時のオフセット計算テスト =====

    @Test
    fun calculateOffsetForZoom_preservesViewCenter() {
        val viewWidth = 800f
        val viewHeight = 600f
        val currentScale = 1f
        val newScale = 2f
        val currentOffsetX = 100f
        val currentOffsetY = 50f

        // ズーム前のビュー中心のモデル座標を取得
        val (modelCenterBefore, _) = CoordinateTransform.getViewCenterInModel(
            viewWidth, viewHeight, currentScale, currentOffsetX, currentOffsetY
        )

        // 新しいオフセットを計算
        val (newOffsetX, newOffsetY) = CoordinateTransform.calculateOffsetForZoom(
            viewWidth, viewHeight, currentScale, newScale, currentOffsetX, currentOffsetY
        )

        // ズーム後のビュー中心のモデル座標を取得
        val (modelCenterAfter, _) = CoordinateTransform.getViewCenterInModel(
            viewWidth, viewHeight, newScale, newOffsetX, newOffsetY
        )

        // ビュー中心のモデル座標が変わらないことを確認
        assertFloatEquals(modelCenterBefore, modelCenterAfter,
            "View center in model space should be preserved after zoom")
    }

    @Test
    fun calculateOffsetForZoom_zoomIn() {
        val viewWidth = 800f
        val viewHeight = 600f
        val currentScale = 1f
        val newScale = 2f  // 2倍にズームイン
        val currentOffsetX = 0f
        val currentOffsetY = 0f

        val (newOffsetX, newOffsetY) = CoordinateTransform.calculateOffsetForZoom(
            viewWidth, viewHeight, currentScale, newScale, currentOffsetX, currentOffsetY
        )

        // ズーム前: ビュー中心(400, 300) → モデル(400, 300)
        // ズーム後: モデル(400, 300)をビュー中心に維持
        // newOffset = viewCenter - model * newScale = (400, 300) - (400, 300) * 2 = (-400, -300)
        assertFloatEquals(-400f, newOffsetX)
        assertFloatEquals(-300f, newOffsetY)
    }

    @Test
    fun calculateOffsetForZoom_zoomOut() {
        val viewWidth = 800f
        val viewHeight = 600f
        val currentScale = 2f
        val newScale = 1f  // 半分にズームアウト
        val currentOffsetX = -400f
        val currentOffsetY = -300f

        val (newOffsetX, newOffsetY) = CoordinateTransform.calculateOffsetForZoom(
            viewWidth, viewHeight, currentScale, newScale, currentOffsetX, currentOffsetY
        )

        // ズーム前: ビュー中心(400, 300) → モデル((400+400)/2, (300+300)/2) = (400, 300)
        // ズーム後: モデル(400, 300)をビュー中心に維持
        // newOffset = (400, 300) - (400, 300) * 1 = (0, 0)
        assertFloatEquals(0f, newOffsetX)
        assertFloatEquals(0f, newOffsetY)
    }

    // ===== ピボット点でのズームテスト =====

    @Test
    fun calculateOffsetForZoomAtPivot_atViewCenter() {
        val viewWidth = 800f
        val viewHeight = 600f
        val pivotViewX = 400f  // ビュー中心
        val pivotViewY = 300f
        val currentScale = 1f
        val newScale = 2f
        val currentOffsetX = 0f
        val currentOffsetY = 0f

        val (newOffsetX, newOffsetY) = CoordinateTransform.calculateOffsetForZoomAtPivot(
            pivotViewX, pivotViewY, currentScale, newScale, currentOffsetX, currentOffsetY
        )

        // ビュー中心でズーム = calculateOffsetForZoomと同じ結果
        val (expectedX, expectedY) = CoordinateTransform.calculateOffsetForZoom(
            viewWidth, viewHeight, currentScale, newScale, currentOffsetX, currentOffsetY
        )

        assertFloatEquals(expectedX, newOffsetX)
        assertFloatEquals(expectedY, newOffsetY)
    }

    @Test
    fun calculateOffsetForZoomAtPivot_preservesPivot() {
        val pivotViewX = 200f  // 任意のピボット点
        val pivotViewY = 150f
        val currentScale = 1f
        val newScale = 3f
        val currentOffsetX = 50f
        val currentOffsetY = -25f

        // ズーム前のピボット点のモデル座標
        val (pivotModelX, pivotModelY) = CoordinateTransform.viewToModel(
            pivotViewX, pivotViewY, currentScale, currentOffsetX, currentOffsetY
        )

        // 新しいオフセットを計算
        val (newOffsetX, newOffsetY) = CoordinateTransform.calculateOffsetForZoomAtPivot(
            pivotViewX, pivotViewY, currentScale, newScale, currentOffsetX, currentOffsetY
        )

        // ズーム後、同じモデル座標がピボット点に来ることを確認
        val (viewXAfter, viewYAfter) = CoordinateTransform.modelToView(
            pivotModelX, pivotModelY, newScale, newOffsetX, newOffsetY
        )

        assertFloatEquals(pivotViewX, viewXAfter, "Pivot X should be preserved")
        assertFloatEquals(pivotViewY, viewYAfter, "Pivot Y should be preserved")
    }

    // ===== エッジケース =====

    @Test
    fun verySmallScale() {
        val scale = 0.0001f
        val modelX = 100000f
        val modelY = 200000f

        val (viewX, viewY) = CoordinateTransform.modelToView(
            modelX, modelY, scale, 0f, 0f
        )
        val (modelXBack, modelYBack) = CoordinateTransform.viewToModel(
            viewX, viewY, scale, 0f, 0f
        )

        assertFloatEquals(modelX, modelXBack, "Very small scale round trip X")
        assertFloatEquals(modelY, modelYBack, "Very small scale round trip Y")
    }

    @Test
    fun negativeCoordinates() {
        val modelX = -5000f
        val modelY = -10000f
        val scale = 0.1f
        val offsetX = 500f
        val offsetY = 400f

        val (viewX, viewY) = CoordinateTransform.modelToView(
            modelX, modelY, scale, offsetX, offsetY
        )
        val (modelXBack, modelYBack) = CoordinateTransform.viewToModel(
            viewX, viewY, scale, offsetX, offsetY
        )

        assertFloatEquals(modelX, modelXBack)
        assertFloatEquals(modelY, modelYBack)
    }

    @Test
    fun largeCoordinates() {
        val modelX = 123456789f
        val modelY = -987654321f
        val scale = 0.000001f
        val offsetX = 1000f
        val offsetY = -2000f

        val (viewX, viewY) = CoordinateTransform.modelToView(
            modelX, modelY, scale, offsetX, offsetY
        )
        val (modelXBack, modelYBack) = CoordinateTransform.viewToModel(
            viewX, viewY, scale, offsetX, offsetY
        )

        // 大きな座標と極小スケールの組み合わせでは相対誤差で比較
        assertFloatEqualsRelative(modelX, modelXBack, 0.001f, "Large X should survive round trip")
        assertFloatEqualsRelative(modelY, modelYBack, 0.001f, "Large Y should survive round trip")
    }

    // ===== ゼロ値のテスト =====

    @Test
    fun modelToView_zeroCoordinates() {
        val (viewX, viewY) = CoordinateTransform.modelToView(
            modelX = 0f, modelY = 0f,
            scale = 2f,
            offsetX = 100f, offsetY = 200f
        )
        // 0 * 2 + 100 = 100, 0 * 2 + 200 = 200
        assertFloatEquals(100f, viewX)
        assertFloatEquals(200f, viewY)
    }

    @Test
    fun viewToModel_zeroCoordinates() {
        val (modelX, modelY) = CoordinateTransform.viewToModel(
            viewX = 0f, viewY = 0f,
            scale = 2f,
            offsetX = 100f, offsetY = 200f
        )
        // (0 - 100) / 2 = -50, (0 - 200) / 2 = -100
        assertFloatEquals(-50f, modelX)
        assertFloatEquals(-100f, modelY)
    }

    @Test
    fun zeroOffset() {
        val (viewX, viewY) = CoordinateTransform.modelToView(
            modelX = 500f, modelY = -300f,
            scale = 0.5f,
            offsetX = 0f, offsetY = 0f
        )
        assertFloatEquals(250f, viewX)
        assertFloatEquals(-150f, viewY)
    }

    // ===== 極端なスケール値 =====

    @Test
    fun veryLargeScale() {
        val scale = 10000f
        val modelX = 0.001f
        val modelY = -0.002f

        val (viewX, viewY) = CoordinateTransform.modelToView(
            modelX, modelY, scale, 0f, 0f
        )
        assertFloatEquals(10f, viewX)
        assertFloatEquals(-20f, viewY)
    }

    @Test
    fun scale_nearOne() {
        // スケール1.0付近での精度テスト
        val scales = listOf(0.999f, 1.0f, 1.001f)
        val modelX = 1000f
        val modelY = -500f

        for (scale in scales) {
            val (viewX, viewY) = CoordinateTransform.modelToView(
                modelX, modelY, scale, 0f, 0f
            )
            val (modelXBack, modelYBack) = CoordinateTransform.viewToModel(
                viewX, viewY, scale, 0f, 0f
            )
            assertFloatEquals(modelX, modelXBack, "Scale $scale X")
            assertFloatEquals(modelY, modelYBack, "Scale $scale Y")
        }
    }

    // ===== 負のオフセット =====

    @Test
    fun negativeOffset() {
        val (viewX, viewY) = CoordinateTransform.modelToView(
            modelX = 100f, modelY = 100f,
            scale = 1f,
            offsetX = -500f, offsetY = -300f
        )
        assertFloatEquals(-400f, viewX)
        assertFloatEquals(-200f, viewY)
    }

    @Test
    fun largeNegativeOffset() {
        val (viewX, viewY) = CoordinateTransform.modelToView(
            modelX = 0f, modelY = 0f,
            scale = 1f,
            offsetX = -100000f, offsetY = -200000f
        )
        assertFloatEquals(-100000f, viewX)
        assertFloatEquals(-200000f, viewY)
    }

    // ===== ビューの角点テスト =====

    @Test
    fun viewCorners_topLeft() {
        val viewWidth = 800f
        val viewHeight = 600f
        val scale = 1f
        val offsetX = 0f
        val offsetY = 0f

        // ビュー左上(0, 0)のモデル座標
        val (modelX, modelY) = CoordinateTransform.viewToModel(
            0f, 0f, scale, offsetX, offsetY
        )
        assertFloatEquals(0f, modelX)
        assertFloatEquals(0f, modelY)
    }

    @Test
    fun viewCorners_bottomRight() {
        val viewWidth = 800f
        val viewHeight = 600f
        val scale = 2f
        val offsetX = 100f
        val offsetY = 50f

        // ビュー右下(800, 600)のモデル座標
        val (modelX, modelY) = CoordinateTransform.viewToModel(
            viewWidth, viewHeight, scale, offsetX, offsetY
        )
        // (800 - 100) / 2 = 350, (600 - 50) / 2 = 275
        assertFloatEquals(350f, modelX)
        assertFloatEquals(275f, modelY)
    }

    @Test
    fun viewCorners_allFour() {
        val viewWidth = 1920f
        val viewHeight = 1080f
        val scale = 0.5f
        val offsetX = 200f
        val offsetY = 100f

        // 4つの角をモデル座標に変換して往復
        val corners = listOf(
            Pair(0f, 0f),           // 左上
            Pair(viewWidth, 0f),    // 右上
            Pair(0f, viewHeight),   // 左下
            Pair(viewWidth, viewHeight)  // 右下
        )

        for ((viewX, viewY) in corners) {
            val (modelX, modelY) = CoordinateTransform.viewToModel(
                viewX, viewY, scale, offsetX, offsetY
            )
            val (viewXBack, viewYBack) = CoordinateTransform.modelToView(
                modelX, modelY, scale, offsetX, offsetY
            )
            assertFloatEquals(viewX, viewXBack, "Corner ($viewX, $viewY) X")
            assertFloatEquals(viewY, viewYBack, "Corner ($viewX, $viewY) Y")
        }
    }

    // ===== 連続ズームテスト =====

    @Test
    fun consecutiveZoomIn() {
        val viewWidth = 800f
        val viewHeight = 600f
        var scale = 1f
        var offsetX = 0f
        var offsetY = 0f

        // 10回連続でズームイン（各回1.5倍）
        repeat(10) {
            val newScale = scale * 1.5f
            val (newOffsetX, newOffsetY) = CoordinateTransform.calculateOffsetForZoom(
                viewWidth, viewHeight, scale, newScale, offsetX, offsetY
            )
            scale = newScale
            offsetX = newOffsetX
            offsetY = newOffsetY
        }

        // 最終スケールは約57.67倍
        val expectedScale = 1f * 1.5.pow(10.0).toFloat()
        assertFloatEqualsRelative(expectedScale, scale, 0.0001f, "Final scale after 10x zoom")

        // ビュー中心のモデル座標は変わらないはず
        val (modelCenterX, modelCenterY) = CoordinateTransform.getViewCenterInModel(
            viewWidth, viewHeight, scale, offsetX, offsetY
        )
        assertFloatEquals(400f, modelCenterX, "Center X preserved after zooms")
        assertFloatEquals(300f, modelCenterY, "Center Y preserved after zooms")
    }

    @Test
    fun consecutiveZoomOut() {
        val viewWidth = 800f
        val viewHeight = 600f
        var scale = 100f
        var offsetX = -39600f  // ビュー中心(400,300)がモデル(400,300)に対応するオフセット
        var offsetY = -29700f

        // 10回連続でズームアウト（各回0.7倍）
        repeat(10) {
            val newScale = scale * 0.7f
            val (newOffsetX, newOffsetY) = CoordinateTransform.calculateOffsetForZoom(
                viewWidth, viewHeight, scale, newScale, offsetX, offsetY
            )
            scale = newScale
            offsetX = newOffsetX
            offsetY = newOffsetY
        }

        // ビュー中心のモデル座標は変わらないはず
        val (modelCenterX, modelCenterY) = CoordinateTransform.getViewCenterInModel(
            viewWidth, viewHeight, scale, offsetX, offsetY
        )
        assertFloatEqualsRelative(400f, modelCenterX, 0.001f, "Center X preserved after zoom outs")
        assertFloatEqualsRelative(300f, modelCenterY, 0.001f, "Center Y preserved after zoom outs")
    }

    @Test
    fun zoomInThenZoomOut() {
        val viewWidth = 800f
        val viewHeight = 600f
        val originalScale = 1f
        val originalOffsetX = 100f
        val originalOffsetY = -50f

        // ズームイン
        val zoomedScale = 4f
        val (zoomedOffsetX, zoomedOffsetY) = CoordinateTransform.calculateOffsetForZoom(
            viewWidth, viewHeight, originalScale, zoomedScale, originalOffsetX, originalOffsetY
        )

        // ズームアウト（元のスケールに戻す）
        val (finalOffsetX, finalOffsetY) = CoordinateTransform.calculateOffsetForZoom(
            viewWidth, viewHeight, zoomedScale, originalScale, zoomedOffsetX, zoomedOffsetY
        )

        // 元のオフセットに戻るはず
        assertFloatEquals(originalOffsetX, finalOffsetX, "Offset X should return to original")
        assertFloatEquals(originalOffsetY, finalOffsetY, "Offset Y should return to original")
    }

    // ===== パン操作テスト =====

    @Test
    fun panOperation_moveRight() {
        // パン = オフセットの変更
        val originalOffsetX = 100f
        val panDeltaX = 50f  // 右に50ピクセルパン

        val newOffsetX = originalOffsetX + panDeltaX

        // モデル原点のビュー座標が50ピクセル右に移動
        val (viewX1, _) = CoordinateTransform.modelToView(0f, 0f, 1f, originalOffsetX, 0f)
        val (viewX2, _) = CoordinateTransform.modelToView(0f, 0f, 1f, newOffsetX, 0f)

        assertFloatEquals(viewX1 + panDeltaX, viewX2)
    }

    @Test
    fun panOperation_moveUp() {
        val originalOffsetY = 200f
        val panDeltaY = -100f  // 上に100ピクセルパン（Y減少方向）

        val newOffsetY = originalOffsetY + panDeltaY

        val (_, viewY1) = CoordinateTransform.modelToView(0f, 0f, 1f, 0f, originalOffsetY)
        val (_, viewY2) = CoordinateTransform.modelToView(0f, 0f, 1f, 0f, newOffsetY)

        assertFloatEquals(viewY1 + panDeltaY, viewY2)
    }

    @Test
    fun panAndZoom_combined() {
        val viewWidth = 800f
        val viewHeight = 600f

        // 初期状態
        var scale = 1f
        var offsetX = 0f
        var offsetY = 0f

        // パン（右に100、下に50）
        offsetX += 100f
        offsetY += 50f

        // ズームイン（2倍）
        val newScale = 2f
        val (newOffsetX, newOffsetY) = CoordinateTransform.calculateOffsetForZoom(
            viewWidth, viewHeight, scale, newScale, offsetX, offsetY
        )

        // ビュー中心のモデル座標がズーム前後で維持されることを確認
        val (modelCenterBefore, modelCenterYBefore) = CoordinateTransform.getViewCenterInModel(
            viewWidth, viewHeight, scale, offsetX, offsetY
        )
        val (modelCenterAfter, modelCenterYAfter) = CoordinateTransform.getViewCenterInModel(
            viewWidth, viewHeight, newScale, newOffsetX, newOffsetY
        )

        assertFloatEquals(modelCenterBefore, modelCenterAfter, "Model center X preserved")
        assertFloatEquals(modelCenterYBefore, modelCenterYAfter, "Model center Y preserved")
    }

    // ===== 異形ビューテスト =====

    @Test
    fun veryWideView() {
        val viewWidth = 3840f  // 超ワイド
        val viewHeight = 600f
        val scale = 1f
        val offsetX = 0f
        val offsetY = 0f

        val (modelCenterX, modelCenterY) = CoordinateTransform.getViewCenterInModel(
            viewWidth, viewHeight, scale, offsetX, offsetY
        )

        assertFloatEquals(1920f, modelCenterX)
        assertFloatEquals(300f, modelCenterY)
    }

    @Test
    fun veryTallView() {
        val viewWidth = 400f
        val viewHeight = 2160f  // 超縦長
        val scale = 0.5f
        val offsetX = 100f
        val offsetY = 50f

        val (modelCenterX, modelCenterY) = CoordinateTransform.getViewCenterInModel(
            viewWidth, viewHeight, scale, offsetX, offsetY
        )

        // center = ((200 - 100) / 0.5, (1080 - 50) / 0.5) = (200, 2060)
        assertFloatEquals(200f, modelCenterX)
        assertFloatEquals(2060f, modelCenterY)
    }

    @Test
    fun smallView() {
        val viewWidth = 100f
        val viewHeight = 100f
        val scale = 10f
        val offsetX = 50f
        val offsetY = 50f

        val (modelCenterX, modelCenterY) = CoordinateTransform.getViewCenterInModel(
            viewWidth, viewHeight, scale, offsetX, offsetY
        )

        // center = ((50 - 50) / 10, (50 - 50) / 10) = (0, 0)
        assertFloatEquals(0f, modelCenterX)
        assertFloatEquals(0f, modelCenterY)
    }

    // ===== ピボットズームの追加テスト =====

    @Test
    fun zoomAtPivot_topLeftCorner() {
        val pivotViewX = 0f
        val pivotViewY = 0f
        val currentScale = 1f
        val newScale = 2f
        val currentOffsetX = 0f
        val currentOffsetY = 0f

        val (newOffsetX, newOffsetY) = CoordinateTransform.calculateOffsetForZoomAtPivot(
            pivotViewX, pivotViewY, currentScale, newScale, currentOffsetX, currentOffsetY
        )

        // ピボット点(0,0)のモデル座標は(0,0)
        // 新オフセット = 0 - 0 * 2 = (0, 0)
        assertFloatEquals(0f, newOffsetX)
        assertFloatEquals(0f, newOffsetY)
    }

    @Test
    fun zoomAtPivot_arbitrary() {
        val pivotViewX = 300f
        val pivotViewY = 200f
        val currentScale = 1f
        val newScale = 4f
        val currentOffsetX = 100f
        val currentOffsetY = 50f

        // ピボット点のモデル座標を計算
        val (pivotModelX, pivotModelY) = CoordinateTransform.viewToModel(
            pivotViewX, pivotViewY, currentScale, currentOffsetX, currentOffsetY
        )
        // (300 - 100) / 1 = 200, (200 - 50) / 1 = 150

        val (newOffsetX, newOffsetY) = CoordinateTransform.calculateOffsetForZoomAtPivot(
            pivotViewX, pivotViewY, currentScale, newScale, currentOffsetX, currentOffsetY
        )

        // 新オフセット = pivotView - pivotModel * newScale
        // = (300, 200) - (200, 150) * 4 = (300 - 800, 200 - 600) = (-500, -400)
        assertFloatEquals(-500f, newOffsetX)
        assertFloatEquals(-400f, newOffsetY)

        // 検証: ピボット点のモデル座標がズーム後も同じビュー座標に来る
        val (viewXAfter, viewYAfter) = CoordinateTransform.modelToView(
            pivotModelX, pivotModelY, newScale, newOffsetX, newOffsetY
        )
        assertFloatEquals(pivotViewX, viewXAfter)
        assertFloatEquals(pivotViewY, viewYAfter)
    }

    @Test
    fun zoomAtPivot_zoomOut() {
        val pivotViewX = 500f
        val pivotViewY = 400f
        val currentScale = 4f
        val newScale = 1f
        val currentOffsetX = -500f
        val currentOffsetY = -400f

        val (pivotModelX, pivotModelY) = CoordinateTransform.viewToModel(
            pivotViewX, pivotViewY, currentScale, currentOffsetX, currentOffsetY
        )

        val (newOffsetX, newOffsetY) = CoordinateTransform.calculateOffsetForZoomAtPivot(
            pivotViewX, pivotViewY, currentScale, newScale, currentOffsetX, currentOffsetY
        )

        // ズーム後もピボット点が維持される
        val (viewXAfter, viewYAfter) = CoordinateTransform.modelToView(
            pivotModelX, pivotModelY, newScale, newOffsetX, newOffsetY
        )
        assertFloatEquals(pivotViewX, viewXAfter, "Pivot X after zoom out")
        assertFloatEquals(pivotViewY, viewYAfter, "Pivot Y after zoom out")
    }

    // ===== 複数回の往復変換テスト =====

    @Test
    fun multipleRoundTrips() {
        val modelX = 1234.5678f
        val modelY = -8765.4321f
        val scale = 1.5f
        val offsetX = 200f
        val offsetY = -150f

        var currentX = modelX
        var currentY = modelY

        // 10回往復
        repeat(10) {
            val (viewX, viewY) = CoordinateTransform.modelToView(
                currentX, currentY, scale, offsetX, offsetY
            )
            val (backX, backY) = CoordinateTransform.viewToModel(
                viewX, viewY, scale, offsetX, offsetY
            )
            currentX = backX
            currentY = backY
        }

        assertFloatEqualsRelative(modelX, currentX, 0.0001f, "X after 10 round trips")
        assertFloatEqualsRelative(modelY, currentY, 0.0001f, "Y after 10 round trips")
    }

    // ===== 境界値テスト =====

    @Test
    fun floatMaxValue() {
        val maxVal = Float.MAX_VALUE / 2f  // オーバーフロー防止
        val (viewX, viewY) = CoordinateTransform.modelToView(
            maxVal, -maxVal, 1f, 0f, 0f
        )
        assertFloatEquals(maxVal, viewX)
        assertFloatEquals(-maxVal, viewY)
    }

    @Test
    fun floatMinPositive() {
        val minVal = Float.MIN_VALUE
        val (viewX, viewY) = CoordinateTransform.modelToView(
            minVal, minVal, 1f, 0f, 0f
        )
        assertFloatEquals(minVal, viewX)
        assertFloatEquals(minVal, viewY)
    }

    // ===== DXF実用シナリオテスト =====

    @Test
    fun dxfTypicalScenario_fitToView() {
        // DXF図面: モデル範囲 (0,0) - (100000, 50000) mm
        val modelMinX = 0f
        val modelMinY = 0f
        val modelMaxX = 100000f
        val modelMaxY = 50000f
        val modelCenterX = (modelMinX + modelMaxX) / 2f
        val modelCenterY = (modelMinY + modelMaxY) / 2f

        // ビューサイズ: 1920x1080
        val viewWidth = 1920f
        val viewHeight = 1080f

        // フィットスケールを計算
        val scaleX = viewWidth / (modelMaxX - modelMinX)
        val scaleY = viewHeight / (modelMaxY - modelMinY)
        val scale = minOf(scaleX, scaleY) * 0.9f  // 10%マージン

        // モデル中心をビュー中心に配置
        val (offsetX, offsetY) = CoordinateTransform.calculateOffsetToCenterModel(
            modelCenterX, modelCenterY, viewWidth, viewHeight, scale
        )

        // 検証: モデル中心がビュー中心に来る
        val (viewX, viewY) = CoordinateTransform.modelToView(
            modelCenterX, modelCenterY, scale, offsetX, offsetY
        )
        assertFloatEquals(viewWidth / 2f, viewX, "Model center at view center X")
        assertFloatEquals(viewHeight / 2f, viewY, "Model center at view center Y")
    }

    @Test
    fun dxfTypicalScenario_zoomToSelection() {
        // 選択範囲にズーム
        val selectionMinX = 30000f
        val selectionMinY = 20000f
        val selectionMaxX = 40000f
        val selectionMaxY = 30000f
        val selectionCenterX = (selectionMinX + selectionMaxX) / 2f
        val selectionCenterY = (selectionMinY + selectionMaxY) / 2f

        val viewWidth = 800f
        val viewHeight = 600f

        // 選択範囲をビューにフィット
        val scaleX = viewWidth / (selectionMaxX - selectionMinX)
        val scaleY = viewHeight / (selectionMaxY - selectionMinY)
        val scale = minOf(scaleX, scaleY) * 0.8f

        val (offsetX, offsetY) = CoordinateTransform.calculateOffsetToCenterModel(
            selectionCenterX, selectionCenterY, viewWidth, viewHeight, scale
        )

        // 選択範囲の中心がビュー中心に来ることを確認
        val (viewCenterModelX, viewCenterModelY) = CoordinateTransform.getViewCenterInModel(
            viewWidth, viewHeight, scale, offsetX, offsetY
        )
        assertFloatEquals(selectionCenterX, viewCenterModelX, "Selection center X")
        assertFloatEquals(selectionCenterY, viewCenterModelY, "Selection center Y")
    }

    @Test
    fun dxfTypicalScenario_mouseWheelZoom() {
        // マウスホイールズーム（マウス位置でズーム）
        val viewWidth = 800f
        val viewHeight = 600f
        var scale = 0.01f  // 初期スケール
        var offsetX = 400f  // モデル原点がビュー中心
        var offsetY = 300f

        // マウス位置
        val mouseX = 600f
        val mouseY = 400f

        // マウス位置のモデル座標を記録
        val (modelAtMouseBefore, modelAtMouseYBefore) = CoordinateTransform.viewToModel(
            mouseX, mouseY, scale, offsetX, offsetY
        )

        // ホイールで1.1倍ズームイン
        val newScale = scale * 1.1f
        val (newOffsetX, newOffsetY) = CoordinateTransform.calculateOffsetForZoomAtPivot(
            mouseX, mouseY, scale, newScale, offsetX, offsetY
        )

        // マウス位置のモデル座標が変わらないことを確認
        val (modelAtMouseAfter, modelAtMouseYAfter) = CoordinateTransform.viewToModel(
            mouseX, mouseY, newScale, newOffsetX, newOffsetY
        )

        assertFloatEqualsRelative(modelAtMouseBefore, modelAtMouseAfter, 0.0001f, "Model X at mouse")
        assertFloatEqualsRelative(modelAtMouseYBefore, modelAtMouseYAfter, 0.0001f, "Model Y at mouse")
    }

    // ===== 対称性テスト =====

    @Test
    fun symmetry_xAxis() {
        val scale = 2f
        val offsetX = 100f
        val offsetY = 0f

        val (viewX1, viewY1) = CoordinateTransform.modelToView(50f, 100f, scale, offsetX, offsetY)
        val (viewX2, viewY2) = CoordinateTransform.modelToView(50f, -100f, scale, offsetX, offsetY)

        // X座標は同じ
        assertFloatEquals(viewX1, viewX2, "X should be same for Y-symmetric points")
        // Y座標は中心から等距離（符号反転）
        assertFloatEquals(viewY1, -viewY2, "Y should be symmetric")
    }

    @Test
    fun symmetry_yAxis() {
        val scale = 2f
        val offsetX = 0f
        val offsetY = 100f

        val (viewX1, viewY1) = CoordinateTransform.modelToView(100f, 50f, scale, offsetX, offsetY)
        val (viewX2, viewY2) = CoordinateTransform.modelToView(-100f, 50f, scale, offsetX, offsetY)

        // Y座標は同じ
        assertFloatEquals(viewY1, viewY2, "Y should be same for X-symmetric points")
        // X座標は中心から等距離（符号反転）
        assertFloatEquals(viewX1, -viewX2, "X should be symmetric")
    }

    // ===== 線形変換の検証テスト =====

    @Test
    fun linearTransform_scaleProperty() {
        // スケールの線形性: (A + B) * scale = A * scale + B * scale
        val scale = 2f
        val offsetX = 0f  // オフセット0で純粋なスケールをテスト
        val offsetY = 0f

        val modelA = Pair(100f, 200f)
        val modelB = Pair(50f, -30f)

        // A + Bのビュー座標（オフセット0）
        val (viewSumX, viewSumY) = CoordinateTransform.modelToView(
            modelA.first + modelB.first,
            modelA.second + modelB.second,
            scale, offsetX, offsetY
        )

        // 個別のスケール後の合計
        val (viewAX, viewAY) = CoordinateTransform.modelToView(
            modelA.first, modelA.second, scale, offsetX, offsetY
        )
        val (viewBX, viewBY) = CoordinateTransform.modelToView(
            modelB.first, modelB.second, scale, offsetX, offsetY
        )

        // (A+B)*s = A*s + B*s
        assertFloatEquals(viewSumX, viewAX + viewBX, "Scale linearity X")
        assertFloatEquals(viewSumY, viewAY + viewBY, "Scale linearity Y")
    }

    // ===== スケール変更の可逆性 =====

    @Test
    fun scaleChange_reversible() {
        val viewWidth = 800f
        val viewHeight = 600f
        val originalScale = 1f
        val originalOffsetX = 0f
        val originalOffsetY = 0f

        // 様々なスケール変更をテスト
        val scaleFactors = listOf(0.1f, 0.5f, 2f, 5f, 10f, 100f)

        for (factor in scaleFactors) {
            val newScale = originalScale * factor
            val (newOffsetX, newOffsetY) = CoordinateTransform.calculateOffsetForZoom(
                viewWidth, viewHeight, originalScale, newScale, originalOffsetX, originalOffsetY
            )

            // 元に戻す
            val (revertedOffsetX, revertedOffsetY) = CoordinateTransform.calculateOffsetForZoom(
                viewWidth, viewHeight, newScale, originalScale, newOffsetX, newOffsetY
            )

            assertFloatEqualsRelative(originalOffsetX, revertedOffsetX, 0.001f, "Factor $factor offset X")
            assertFloatEqualsRelative(originalOffsetY, revertedOffsetY, 0.001f, "Factor $factor offset Y")
        }
    }
}
