package com.jpaver.trianglelist.adapter

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import com.jpaver.trianglelist.label.LabelMetrics

/**
 * 描画と同一メトリクスで実測する LabelMetrics (rev5 確定)。
 * フォント = MS Gothic、fontSize = height / capHeightRatio、整列式 =
 * TextRenderer.alignedTopLeft ── すべて描画コードと同じものを共用するので、
 * 判定の箱と描かれるグリフは構造的に一致する。
 *
 * 大きさはレイアウト箱 (ascent〜descent) ではなく Skia Font.measureText の
 * インク実 bounds ── グリフが実際に塗られる範囲に密着させる。
 *
 * 座標変換: 描画は Y 反転済み frame (y 下向き)、判定は DXF frame (Y 上向き)。
 * アンカーを原点に置いて計測し、y を符号反転して DXF ローカルへ写す。
 *
 * @param textMeasurer 計測器。CP スレッドから使う場合は UI と同じ resolver から
 *   専用 instance を作って渡す (TextMeasurer の cache は thread-safe でないため共有しない)。
 */
class MeasuredLabelMetrics(
    private val textMeasurer: TextMeasurer,
    private val density: Density,
) : LabelMetrics {

    override fun inkBoxLocal(text: String, heightMm: Float, alignH: Int, alignV: Int): LabelMetrics.InkBox {
        val fontSizePx = TextRenderer.fontSizePxFor(heightMm)
        // 描画と同じ style で layout (整列計算は描画と同じ firstBaseline を使う)
        val style = TextStyle(
            fontFamily = TextRenderer.msGothicFamily,
            fontSize = with(density) { fontSizePx.toSp() },
        )
        val layout = textMeasurer.measure(text, style)
        val baseline = layout.firstBaseline
        // アンカー (0,0)・描画 frame (y 下向き) で、描画と同一の整列式から layout 左上を得る
        val topLeft = TextRenderer.alignedTopLeft(
            0f, 0f,
            layout.size.width.toFloat(), layout.size.height.toFloat(),
            baseline, heightMm, alignH, alignV,
        )

        // インク実測: 描画と同じ MS Gothic Typeface・同じ px サイズ。
        // measureText の Rect はベースライン原点 (top は負)
        val ink = org.jetbrains.skia.Font(TextRenderer.msGothicTypeface, fontSizePx).measureText(text)
        val leftFlipped = topLeft.x + ink.left
        val rightFlipped = topLeft.x + ink.right
        val topFlipped = topLeft.y + baseline + ink.top
        val bottomFlipped = topLeft.y + baseline + ink.bottom

        // 描画 frame (y 下向き) → DXF ローカル (Y 上向き)
        return LabelMetrics.InkBox(
            leftMm = leftFlipped,
            rightMm = rightFlipped,
            bottomMm = -bottomFlipped,
            topMm = -topFlipped,
        )
    }
}
