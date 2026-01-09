package com.jpaver.trianglelist.util

/**
 * モデル空間とビュー空間の座標変換ユーティリティ
 *
 * 座標系:
 * - モデル空間 (Model/World): DXFの座標系
 * - ビュー空間 (View/Screen): 画面上のピクセル座標
 *
 * 変換式:
 * - Model → View: screenPos = modelPos * scale + offset
 * - View → Model: modelPos = (screenPos - offset) / scale
 */
object CoordinateTransform {

    /**
     * モデル座標をビュー座標に変換
     * @param modelX モデル空間のX座標
     * @param modelY モデル空間のY座標
     * @param scale スケール係数
     * @param offsetX ビューのオフセットX
     * @param offsetY ビューのオフセットY
     * @return ビュー空間の座標 (x, y)
     */
    fun modelToView(
        modelX: Float,
        modelY: Float,
        scale: Float,
        offsetX: Float,
        offsetY: Float
    ): Pair<Float, Float> {
        val viewX = modelX * scale + offsetX
        val viewY = modelY * scale + offsetY
        return Pair(viewX, viewY)
    }

    /**
     * ビュー座標をモデル座標に変換
     * @param viewX ビュー空間のX座標
     * @param viewY ビュー空間のY座標
     * @param scale スケール係数
     * @param offsetX ビューのオフセットX
     * @param offsetY ビューのオフセットY
     * @return モデル空間の座標 (x, y)
     */
    fun viewToModel(
        viewX: Float,
        viewY: Float,
        scale: Float,
        offsetX: Float,
        offsetY: Float
    ): Pair<Float, Float> {
        val modelX = (viewX - offsetX) / scale
        val modelY = (viewY - offsetY) / scale
        return Pair(modelX, modelY)
    }

    /**
     * ビューの中心に対応するモデル座標を取得
     * @param viewWidth ビューの幅
     * @param viewHeight ビューの高さ
     * @param scale スケール係数
     * @param offsetX ビューのオフセットX
     * @param offsetY ビューのオフセットY
     * @return モデル空間でのビュー中心座標 (x, y)
     */
    fun getViewCenterInModel(
        viewWidth: Float,
        viewHeight: Float,
        scale: Float,
        offsetX: Float,
        offsetY: Float
    ): Pair<Float, Float> {
        val centerX = viewWidth / 2f
        val centerY = viewHeight / 2f
        return viewToModel(centerX, centerY, scale, offsetX, offsetY)
    }

    /**
     * モデル座標をビューの中心に配置するためのオフセットを計算
     * @param modelX 中心に配置したいモデルX座標
     * @param modelY 中心に配置したいモデルY座標
     * @param viewWidth ビューの幅
     * @param viewHeight ビューの高さ
     * @param scale スケール係数
     * @return 必要なオフセット (offsetX, offsetY)
     */
    fun calculateOffsetToCenterModel(
        modelX: Float,
        modelY: Float,
        viewWidth: Float,
        viewHeight: Float,
        scale: Float
    ): Pair<Float, Float> {
        val viewCenterX = viewWidth / 2f
        val viewCenterY = viewHeight / 2f
        val offsetX = viewCenterX - modelX * scale
        val offsetY = viewCenterY - modelY * scale
        return Pair(offsetX, offsetY)
    }

    /**
     * ズーム時にビュー中心を維持するための新しいオフセットを計算
     * @param viewWidth ビューの幅
     * @param viewHeight ビューの高さ
     * @param currentScale 現在のスケール
     * @param newScale 新しいスケール
     * @param currentOffsetX 現在のオフセットX
     * @param currentOffsetY 現在のオフセットY
     * @return 新しいオフセット (offsetX, offsetY)
     */
    fun calculateOffsetForZoom(
        viewWidth: Float,
        viewHeight: Float,
        currentScale: Float,
        newScale: Float,
        currentOffsetX: Float,
        currentOffsetY: Float
    ): Pair<Float, Float> {
        // 現在のビュー中心のモデル座標を取得
        val (modelCenterX, modelCenterY) = getViewCenterInModel(
            viewWidth, viewHeight, currentScale, currentOffsetX, currentOffsetY
        )
        // そのモデル座標を新しいスケールでビュー中心に配置
        return calculateOffsetToCenterModel(
            modelCenterX, modelCenterY, viewWidth, viewHeight, newScale
        )
    }

    /**
     * 任意のピボット点を中心にズームするための新しいオフセットを計算
     * @param pivotViewX ズームの中心となるビューX座標
     * @param pivotViewY ズームの中心となるビューY座標
     * @param currentScale 現在のスケール
     * @param newScale 新しいスケール
     * @param currentOffsetX 現在のオフセットX
     * @param currentOffsetY 現在のオフセットY
     * @return 新しいオフセット (offsetX, offsetY)
     */
    fun calculateOffsetForZoomAtPivot(
        pivotViewX: Float,
        pivotViewY: Float,
        currentScale: Float,
        newScale: Float,
        currentOffsetX: Float,
        currentOffsetY: Float
    ): Pair<Float, Float> {
        // ピボット点のモデル座標を取得
        val (pivotModelX, pivotModelY) = viewToModel(
            pivotViewX, pivotViewY, currentScale, currentOffsetX, currentOffsetY
        )
        // 新しいスケールでピボット点を同じビュー座標に維持
        val newOffsetX = pivotViewX - pivotModelX * newScale
        val newOffsetY = pivotViewY - pivotModelY * newScale
        return Pair(newOffsetX, newOffsetY)
    }
}
