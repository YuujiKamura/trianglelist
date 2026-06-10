package com.jpaver.trianglelist.label

import com.example.trilib.PointXY
import com.jpaver.trianglelist.dxf.DxfParseResult
import com.jpaver.trianglelist.dxf.DxfText

/**
 * 重なりペア 1 件。textId は必ず TEXT 側、otherId は相手 (TEXT/CIRCLE は LABEL、LINE は EDGE)。
 * TEXT 同士のペアは A-B / B-A を正規化して 1 件にする。
 */
data class OverlapPair(val textId: String, val otherId: String, val otherKind: ObstacleKind)

/** 図面 1 枚分の重なり集計。事実の数値のみで、良し悪しの判定や閾値は持たない。 */
data class OverlapReport(
    val totalTexts: Int,
    val overlappingTexts: Int,
    val pairs: List<OverlapPair>,
)

/**
 * DXF パース結果 → CollisionField 集計の純関数 (ADR 0002 段階 2: まず測る、直すのは後)。
 *
 * 全 LINE を辺、全 CIRCLE を外接 box、全 TEXT を係数近似の LabelBox として
 * CollisionField に登録し、各 TEXT の box を query して重なりの事実を数える。
 * 配置は一切変更しない。判定に図面知識・閾値を持ち込まない (段階 1 と同じ流儀)。
 *
 * テキスト幅は `文字数 × height × textWidthFactor` の係数近似。フォント実測は将来、
 * 係数を引数にするのは viewer 実測から後で較正するため (ADR 0002「係数近似から始めて
 * 乖離を観測してから決める」)。
 */
object DxfOverlapAnalyzer {

    fun analyze(parseResult: DxfParseResult, textWidthFactor: Float = 1.0f): OverlapReport {
        val field = CollisionField()

        parseResult.lines.forEachIndexed { index, line ->
            field.addEdge(
                "line:$index",
                PointXY(line.x1.toFloat(), line.y1.toFloat()),
                PointXY(line.x2.toFloat(), line.y2.toFloat()),
            )
        }
        parseResult.circles.forEachIndexed { index, circle ->
            val diameter = (circle.radius * 2.0).toFloat()
            field.addBox(
                "circle:$index",
                LabelBox(
                    PointXY(circle.centerX.toFloat(), circle.centerY.toFloat()),
                    widthMm = diameter,
                    heightMm = diameter,
                ),
            )
        }
        val textBoxes = parseResult.texts.mapIndexed { index, text ->
            textId(index, text.text) to toLabelBox(text, textWidthFactor)
        }
        for ((id, box) in textBoxes) {
            field.addBox(id, box)
        }

        var overlappingTexts = 0
        val seenPairKeys = mutableSetOf<String>()
        val pairs = mutableListOf<OverlapPair>()
        for ((id, box) in textBoxes) {
            val hits = field.query(box, excludeId = id)
            if (hits.isNotEmpty()) overlappingTexts++
            for (hit in hits) {
                val key = if (id < hit.id) "$id|${hit.id}" else "${hit.id}|$id"
                if (!seenPairKeys.add(key)) continue
                pairs.add(OverlapPair(textId = id, otherId = hit.id, otherKind = hit.kind))
            }
        }
        return OverlapReport(
            totalTexts = textBoxes.size,
            overlappingTexts = overlappingTexts,
            pairs = pairs,
        )
    }

    /**
     * 後から人間が読めて一意な id。一意性は index が担保し、内容先頭 8 文字は可読性のため。
     * 空白は CP の 1 行出力 (空白区切り) を壊すので _ に潰す。
     */
    private fun textId(index: Int, content: String): String {
        val head = content.take(8).replace(Regex("\\s"), "_")
        return "text:$index:$head"
    }

    /**
     * DxfText → LabelBox 変換。アンカー (x, y) から alignH/alignV で中心へ補正し、
     * 回転テキストは補正オフセットごとアンカー回りに rotation 回転する
     * (DXF の TEXT 回転はアンカー基準のため)。
     */
    private fun toLabelBox(text: DxfText, textWidthFactor: Float): LabelBox {
        val heightMm = text.height.toFloat()
        val widthMm = text.text.length * heightMm * textWidthFactor
        val offsetX = when (text.alignH) {
            1 -> 0f               // 中央: アンカーが中心
            2 -> -widthMm / 2f    // 右寄せ: アンカーは右端 → 中心は左へ
            else -> widthMm / 2f  // 左寄せ (0): アンカーは左端 → 中心は右へ
        }
        val offsetY = when (text.alignV) {
            2 -> 0f                // 中央: アンカーが中心
            3 -> -heightMm / 2f    // 上: アンカーは上端 → 中心は下へ
            else -> heightMm / 2f  // ベースライン (0)・下 (1): 中心は上へ
        }
        val anchor = PointXY(text.x.toFloat(), text.y.toFloat())
        val rotationDeg = text.rotation.toFloat()
        val center = PointXY(anchor.x + offsetX, anchor.y + offsetY).rotate(anchor, rotationDeg)
        return LabelBox(center, widthMm = widthMm, heightMm = heightMm, rotationDeg = rotationDeg)
    }
}
