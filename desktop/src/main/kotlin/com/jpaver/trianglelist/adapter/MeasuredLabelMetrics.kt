package com.jpaver.trianglelist.adapter

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.jpaver.trianglelist.dxf.DxfText
import com.jpaver.trianglelist.label.LabelMetrics

/**
 * 実描画を写す LabelMetrics (rev4)。描画は一切変えない ── 描画コードが真実、箱はその鏡。
 *
 * 位置: TextRenderer.calculateTextBounds (描画と同一ロジックの公開ミラー、
 * fontSize = height.sp の em ベース・layout 箱整列) をそのまま呼ぶ。
 * 大きさ: layout 箱はグリフより大きい (ascent〜descent + 行送り) ので、Skia の
 * Font.measureText によるインク実測でグリフ密着の矩形に絞る。
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

    private val textRenderer = TextRenderer()

    override fun inkBoxLocal(text: String, heightMm: Float, alignH: Int, alignV: Int): LabelMetrics.InkBox {
        // アンカー (0,0) に置いた仮の DxfText で、描画と同一ロジックの layout 箱を取る
        // (描画 frame = Y 反転済み座標、戻り値は [minX, maxX, minY, maxY])
        val probe = DxfText(x = 0.0, y = 0.0, text = text, height = heightMm.toDouble(), alignH = alignH, alignV = alignV)
        val layoutBox = textRenderer.calculateTextBounds(probe, 1f, textMeasurer)

        // インク実測: 描画と同じ fontSize (height.sp → px は同じ density 解釈) の Skia Font。
        // measureText の Rect はベースライン原点 (top は負)。layout 左上 + firstBaseline から
        // 実際にインクが塗られる範囲を絞り込む
        val layout = textMeasurer.measure(text, TextStyle(fontSize = heightMm.sp))
        val baseline = layout.firstBaseline
        val fontSizePx = with(density) { heightMm.sp.toPx() }
        val ink = org.jetbrains.skia.Font(null as org.jetbrains.skia.Typeface?, fontSizePx).measureText(text)

        val leftFlipped = layoutBox[0] + ink.left
        val rightFlipped = layoutBox[0] + ink.right
        val topFlipped = layoutBox[2] + baseline + ink.top
        val bottomFlipped = layoutBox[2] + baseline + ink.bottom

        // 描画 frame (y 下向き) → DXF ローカル (Y 上向き)
        return LabelMetrics.InkBox(
            leftMm = leftFlipped,
            rightMm = rightFlipped,
            bottomMm = -bottomFlipped,
            topMm = -topFlipped,
        )
    }
}
