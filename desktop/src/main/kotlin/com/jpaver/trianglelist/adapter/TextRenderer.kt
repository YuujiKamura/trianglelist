package com.jpaver.trianglelist.adapter

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.jpaver.trianglelist.dxf.DxfText
import com.jpaver.trianglelist.cadview.ColorConverter
import com.jpaver.trianglelist.dxf.calculateAlignedTopLeft

/**
 * デスクトップ版テキスト描画クラス
 * DXFテキストエンティティをCompose Canvasに描画する機能を提供
 *
 * CAD 視覚シミュレーション (rev5 確定): この DXF は STYLE で MS Gothic を指定しており
 * (app/.../datamanager/TablesBuilder.kt:194-270)、DXF TEXT height (group code 40) は
 * キャップハイト (ezdxf 公式 doc の make_font(cap_height) 同義)。旧描画の
 * fontSize = height.sp (em ベース・既定フォント) は CAD 実態から乖離していたため、
 * MS Gothic + 「キャップハイト = height」へ統一する。係数はハードコードせず
 * Skia の実フォントメトリクスから取り、toSp() で density (OS 表示スケール) も打ち消す。
 */
class TextRenderer {

    companion object {
        /** この DXF の STYLE 指定フォント (TablesBuilder.kt:194-270 = MS Gothic)。 */
        val msGothicTypeface: org.jetbrains.skia.Typeface? by lazy {
            try {
                org.jetbrains.skia.FontMgr.default.matchFamilyStyle(
                    "MS Gothic", org.jetbrains.skia.FontStyle.NORMAL
                )
            } catch (e: Throwable) {
                null
            }
        }

        /** Compose 描画用の MS Gothic ファミリ。取得不能時は既定ファミリに fallback。 */
        val msGothicFamily: androidx.compose.ui.text.font.FontFamily by lazy {
            val typeface = msGothicTypeface
            if (typeface != null) {
                androidx.compose.ui.text.font.FontFamily(
                    androidx.compose.ui.text.platform.Typeface(typeface)
                )
            } else {
                androidx.compose.ui.text.font.FontFamily.Default
            }
        }

        /**
         * MS Gothic の「'A' インク実高 / em」比 (Skia 実測較正)。
         *
         * テーブル値 (OS/2 sCapHeight) ではなく実測する ── QCAD (src/core/RTextRenderer.cpp、
         * アルゴリズム参照) は TTF を固定サイズで 'A' の bbox 実測 → 1/heightA で正規化しており
         * テーブル値を信じない。MS Gothic はテーブル 0.68em に対し数字インク実高 0.77em と
         * 乖離が大きい (fontTools 実測、msgothic.ttc) ため実測必須。
         */
        val capHeightRatio: Float by lazy {
            try {
                val probeSizePx = 100f
                val font = org.jetbrains.skia.Font(msGothicTypeface, probeSizePx)
                val inkA = font.measureText("A") // Rect はベースライン原点、top は負
                val heightA = inkA.bottom - inkA.top
                if (heightA > 0f) heightA / probeSizePx else FALLBACK_CAP_RATIO
            } catch (e: Throwable) {
                FALLBACK_CAP_RATIO
            }
        }

        /** fontTools 実測 (msgothic.ttc): 数字 '5' インク実高 0.770em ('A' 同等)。実測不能時のみ使用。 */
        const val FALLBACK_CAP_RATIO: Float = 0.770f

        /** DXF height (キャップハイト, model mm) → 描画 fontSize (px = model mm)。 */
        fun fontSizePxFor(heightMm: Float): Float = heightMm / capHeightRatio

        /**
         * DXF アンカー → Compose 左上座標 (描画と判定が共用する唯一の整列式)。
         * 垂直は layout 箱でなくグリフのベースライン / キャップ帯基準 ── layout 箱基準
         * だと ascent との差ぶんグリフが下にずれる (rev3 実測 ~0.4×height)。
         * @param firstBaseline layout 上端からベースラインまでの距離 (px)
         * @param capHeightPx キャップハイト (= DXF height、px = model mm)
         */
        fun alignedTopLeft(
            baseX: Float,
            baseY: Float,
            textWidth: Float,
            textHeight: Float,
            firstBaseline: Float,
            capHeightPx: Float,
            alignH: Int,
            alignV: Int
        ): Offset {
            val x = when (alignH) {
                0 -> baseX // 左揃え：そのまま
                1 -> baseX - textWidth / 2 // 中央揃え：半分左に
                2 -> baseX - textWidth // 右揃え：全幅左に
                else -> baseX
            }
            val y = when (alignV) {
                0 -> baseY - firstBaseline // ベースライン: グリフのベースラインをアンカーに
                1 -> baseY - textHeight // 下揃え: ディセンダ線をアンカーに (layout 下端で近似)
                2 -> baseY - (firstBaseline - capHeightPx / 2) // 中央揃え: キャップ帯の中心
                3 -> baseY - (firstBaseline - capHeightPx) // 上揃え: キャップ上端
                else -> baseY - firstBaseline
            }
            return Offset(x, y)
        }
    }

    /**
     * DXFテキストを描画する
     * @param drawScope 描画スコープ
     * @param text DXFテキストデータ
     * @param scale 現在のスケール
     * @param textMeasurer テキスト測定器
     * @param debugMode デバッグモード（true時に描画起点・テキスト範囲のボックスを表示）
     */
    fun drawText(
        drawScope: DrawScope,
        text: DxfText,
        scale: Float,
        textMeasurer: androidx.compose.ui.text.TextMeasurer,
        debugMode: Boolean = false
    ) {
        val color = ColorConverter.aciToColor(text.color)
        val capHeightPx = text.height.toFloat()
        val textStyle = TextStyle(
            color = color,
            fontFamily = msGothicFamily,
            // キャップハイト = DXF height になるよう補正。toSp で density も打ち消す
            // (DrawScope は Density を実装している)
            fontSize = with(drawScope) { fontSizePxFor(capHeightPx).toSp() }
        )

        // テキストのサイズを測定
        val textLayoutResult = textMeasurer.measure(text.text, textStyle)
        val textWidth = textLayoutResult.size.width.toFloat()
        val textHeight = textLayoutResult.size.height.toFloat() // 実際の描画高さを使用

        // 頂点データは既にY反転済みなので、そのまま使用
        val adjustedPosition = alignedTopLeft(
            text.x.toFloat(),
            text.y.toFloat(),
            textWidth,
            textHeight,
            textLayoutResult.firstBaseline,
            capHeightPx,
            text.alignH,
            text.alignV
        )
        
        // 回転を適用
        if (text.rotation != 0.0) {
            drawScope.drawContext.canvas.save()
            // データYは既に反転済みなのでそのまま回転ピボットに使用
            drawScope.drawContext.transform.rotate(
                degrees = -text.rotation.toFloat(),
                pivot = androidx.compose.ui.geometry.Offset(text.x.toFloat(), text.y.toFloat())
            )
        }
        
        // テキストを描画
        drawScope.drawText(
            textLayoutResult = textLayoutResult,
            topLeft = adjustedPosition
        )
        
        // デバッグモード時に描画起点とテキスト範囲をボックスで可視化
        if (debugMode) {
            drawDebugBoxes(drawScope, text, adjustedPosition, textWidth, textHeight, textMeasurer)
        }
        
        // 回転を復元
        if (text.rotation != 0.0) {
            drawScope.drawContext.canvas.restore()
        }
    }
    
    /**
     * テキストの境界ボックスを計算する
     * 実際の描画処理と全く同じロジックを使用して一貫性を保つ
     * @param text DXFテキストデータ
     * @param scale 現在のスケール
     * @param textMeasurer テキスト測定器
     * @return 境界ボックスの座標 (minX, maxX, minY, maxY)
     */
    fun calculateTextBounds(
        text: DxfText,
        scale: Float,
        textMeasurer: androidx.compose.ui.text.TextMeasurer,
        density: androidx.compose.ui.unit.Density = androidx.compose.ui.unit.Density(1f)
    ): List<Float> {
        // drawTextメソッドと全く同じロジックを使用
        val capHeightPx = text.height.toFloat()
        val textStyle = TextStyle(
            fontFamily = msGothicFamily,
            fontSize = with(density) { fontSizePxFor(capHeightPx).toSp() } // drawText と同じ補正
        )

        val textLayoutResult = textMeasurer.measure(text.text, textStyle)
        val textWidth = textLayoutResult.size.width.toFloat()
        val textHeight = textLayoutResult.size.height.toFloat() // 実際の描画高さを使用

        // アライメントに基づいた実際の描画位置を計算（drawTextと同じロジック）
        val adjustedPosition = alignedTopLeft(
            text.x.toFloat(),
            text.y.toFloat(),
            textWidth,
            textHeight,
            textLayoutResult.firstBaseline,
            capHeightPx,
            text.alignH,
            text.alignV
        )
        
        val minX = adjustedPosition.x
        val maxX = adjustedPosition.x + textWidth
        val minY = adjustedPosition.y
        val maxY = adjustedPosition.y + textHeight
        
        return listOf(minX, maxX, minY, maxY)
    }
    
    /**
     * デバッグ用のボックスを描画する
     * 1. 描画起点（DXFの基準点）- 小さい赤いボックス
     * 2. テキスト範囲（実際のテキスト領域）- 青い枠線
     * 3. DXF高さボックス（参考用）- 緑の点線
     * @param drawScope 描画スコープ
     * @param text DXFテキストデータ
     * @param adjustedPosition 調整済み描画位置（左上）
     * @param textWidth テキスト幅
     * @param textHeight テキスト高さ
     */
    private fun drawDebugBoxes(
        drawScope: DrawScope,
        text: DxfText,
        adjustedPosition: Offset,
        textWidth: Float,
        textHeight: Float,
        textMeasurer: androidx.compose.ui.text.TextMeasurer
    ) {
        // 1. 描画起点（DXFの基準点）を小さい赤いボックスで表示
        val originSize = 10f
        drawScope.drawRect(
            color = Color.Red,
            topLeft = Offset(
                text.x.toFloat() - originSize / 2,
                text.y.toFloat() - originSize / 2
            ),
            size = Size(originSize, originSize),
            style = Stroke(width = 3f)
        )
        
        // 2. テキスト範囲を青い枠線で表示（実際の描画と同じサイズ）
        drawScope.drawRect(
            color = Color.Blue,
            topLeft = adjustedPosition,
            size = Size(textWidth, textHeight),
            style = Stroke(width = 2f)
        )
        
        // 3. DXF高さボックス（参考用）を緑の点線で表示
        val dxfHeight = text.height.toFloat()
        if (dxfHeight != textHeight) {
            val dxfY = when (text.alignV) {
                0 -> text.y.toFloat() - dxfHeight // ベースライン
                1 -> text.y.toFloat() - dxfHeight // 下揃え
                2 -> text.y.toFloat() - dxfHeight / 2 // 中央揃え
                3 -> text.y.toFloat() // 上揃え
                else -> text.y.toFloat() - dxfHeight
            }
            
            val dxfX = when (text.alignH) {
                0 -> text.x.toFloat() // 左揃え
                1 -> text.x.toFloat() - textWidth / 2 // 中央揃え
                2 -> text.x.toFloat() - textWidth // 右揃え
                else -> text.x.toFloat()
            }
            
            drawScope.drawRect(
                color = Color.Green,
                topLeft = Offset(dxfX, dxfY),
                size = Size(textWidth, dxfHeight),
                style = Stroke(width = 1f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f)))
            )
        }
        
        // 4. alignH/alignVラベルをテキストの上に表示
        val labelStyle = TextStyle(
            color = Color.Magenta,
            fontSize = (text.height * 0.5).sp
        )
        val labelText = "H${text.alignH}V${text.alignV}"
        val labelLayout = textMeasurer.measure(labelText, labelStyle)
        val labelHeight = labelLayout.size.height.toFloat()
        drawScope.drawText(
            textLayoutResult = labelLayout,
            topLeft = Offset(adjustedPosition.x, adjustedPosition.y - labelHeight - 20f)
        )
        
        // 5. デバッグ情報をコンソールに出力
        println("=== Text Debug Info ===")
        println("Text: '${text.text}'")
        println("DXF Position: (${text.x}, ${text.y})")
        println("DXF Height: ${text.height}")
        println("Measured Height: $textHeight")
        println("DXF Align: H=${text.alignH}, V=${text.alignV}")
        println("Measured Width: $textWidth")
        println("Adjusted Position: $adjustedPosition")
        println("========================")
    }
}