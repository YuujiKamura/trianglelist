package com.jpaver.trianglelist.dxf

import androidx.compose.ui.geometry.Offset

/**
 * 9点アライメントのtopLeft座標を計算するユーティリティ
 * @param boxLeft ボックスの左上X
 * @param boxTop ボックスの左上Y
 * @param boxWidth ボックスの幅
 * @param boxHeight ボックスの高さ
 * @param textWidth テキストの幅
 * @param textHeight テキストの高さ
 * @param alignH 水平アライメント（0=左, 1=中央, 2=右）
 * @param alignV 垂直アライメント（3=上, 2=中央, 1=下）
 * @return topLeft座標
 */
fun calculateAlignedTopLeft(
    boxLeft: Float,
    boxTop: Float,
    boxWidth: Float,
    boxHeight: Float,
    textWidth: Float,
    textHeight: Float,
    alignH: Int,
    alignV: Int
): Offset {
    val x = when (alignH) {
        0 -> boxLeft // 左
        1 -> boxLeft + boxWidth / 2 - textWidth / 2 // 中央
        2 -> boxLeft + boxWidth - textWidth // 右
        else -> boxLeft
    }
    val y = when (alignV) {
        3 -> boxTop // 上
        2 -> boxTop + boxHeight / 2 - textHeight / 2 // 中央
        1 -> boxTop + boxHeight - textHeight // 下
        else -> boxTop
    }
    return Offset(x, y)
}

/**
 * DXFテキストエンティティの水平アライメント
 */
val DxfText.alignH: Int
    get() = 0 // デフォルトは左寄せ

/**
 * DXFテキストエンティティの垂直アライメント
 */
val DxfText.alignV: Int
    get() = 1 // デフォルトは下寄せ