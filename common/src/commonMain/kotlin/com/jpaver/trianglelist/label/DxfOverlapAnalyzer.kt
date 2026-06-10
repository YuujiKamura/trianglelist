package com.jpaver.trianglelist.label

import com.example.trilib.PointXY
import com.jpaver.trianglelist.dxf.DxfParseResult
import com.jpaver.trianglelist.dxf.DxfText

/**
 * 重なりペア 1 件。textId は必ず TEXT 側、otherId は相手 (TEXT は LABEL、LINE は EDGE、
 * CIRCLE は CIRCLE)。TEXT 同士のペアは A-B / B-A を正規化して 1 件にする。
 * depthMm は重なり深さ (model mm): 0 = 境界接触 (contact、寸法値が自分の辺に
 * 寄り添う正常配置もここに落ちる)、> 0 = めり込み (intrusion)。
 */
data class OverlapPair(
    val textId: String,
    val otherId: String,
    val otherKind: ObstacleKind,
    val depthMm: Float,
)

/** 図面 1 枚分の重なり集計。事実の数値のみで、良し悪しの判定や閾値は持たない。 */
data class OverlapReport(
    val totalTexts: Int,
    val overlappingTexts: Int,
    val pairs: List<OverlapPair>,
)

/**
 * DXF パース結果 → CollisionField 集計の純関数 (ADR 0002 段階 2: まず測る、直すのは後)。
 *
 * 全 LINE を辺、全 CIRCLE を円プリミティブ、全 TEXT を係数近似の LabelBox として
 * CollisionField に登録し、各 TEXT の box を query して重なりの事実 (深さ付き) を数える。
 * 配置は一切変更しない。判定に図面知識・閾値を持ち込まない (段階 1 と同じ流儀)。
 *
 * テキスト幅は LabelMetrics から取る (rev3)。desktop は描画と同じフォントの実測、
 * default は全半角係数近似の fallback (LabelMetrics.Approximate、テスト用)。
 * textWidthFactor は実測較正の追加係数として残す (通常 1.0)。
 */
object DxfOverlapAnalyzer {

    fun analyze(
        parseResult: DxfParseResult,
        textWidthFactor: Float = 1.0f,
        metrics: LabelMetrics = LabelMetrics.Approximate,
    ): OverlapReport {
        val field = CollisionField()

        parseResult.lines.forEachIndexed { index, line ->
            field.addEdge(
                "line:$index",
                PointXY(line.x1.toFloat(), line.y1.toFloat()),
                PointXY(line.x2.toFloat(), line.y2.toFloat()),
            )
        }
        parseResult.circles.forEachIndexed { index, circle ->
            field.addCircle(
                "circle:$index",
                PointXY(circle.centerX.toFloat(), circle.centerY.toFloat()),
                circle.radius.toFloat(),
            )
        }
        val boxes = textBoxes(parseResult, textWidthFactor, metrics)
        for ((id, box) in boxes) {
            field.addBox(id, box)
        }

        var overlappingTexts = 0
        val seenPairKeys = mutableSetOf<String>()
        val pairs = mutableListOf<OverlapPair>()
        for ((id, box) in boxes) {
            val hits = field.query(box, excludeId = id)
            if (hits.isNotEmpty()) overlappingTexts++
            for (hit in hits) {
                val key = if (id < hit.id) "$id|${hit.id}" else "${hit.id}|$id"
                if (!seenPairKeys.add(key)) continue
                pairs.add(OverlapPair(textId = id, otherId = hit.id, otherKind = hit.kind, depthMm = hit.depthMm))
            }
        }
        return OverlapReport(
            totalTexts = boxes.size,
            overlappingTexts = overlappingTexts,
            pairs = pairs,
        )
    }

    /**
     * 全 TEXT の (id, LabelBox) を返す。analyze() と viewer の box overlay が
     * 共用する唯一の生成経路 ── 判定が見ている box と目が見る box を一致させる
     * (rev2: 描画用に別実装したら検証にならない)。
     */
    fun textBoxes(
        parseResult: DxfParseResult,
        textWidthFactor: Float = 1.0f,
        metrics: LabelMetrics = LabelMetrics.Approximate,
    ): List<Pair<String, LabelBox>> =
        parseResult.texts.mapIndexed { index, text ->
            textId(index, text.text) to toLabelBox(text, textWidthFactor, metrics)
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
     * DxfText → LabelBox 変換。インク矩形 (アンカー基準ローカル) は LabelMetrics が
     * 供給し、ここでは回転だけをアンカー回りに適用する (DXF の TEXT 回転はアンカー基準)。
     * alignH/alignV の解釈も metrics 側 ── 描画を写す実装が描画と同じ式を使うため。
     */
    private fun toLabelBox(text: DxfText, textWidthFactor: Float, metrics: LabelMetrics): LabelBox {
        val heightMm = text.height.toFloat()
        val ink = metrics.inkBoxLocal(text.text, heightMm, text.alignH, text.alignV)
        val widthMm = (ink.rightMm - ink.leftMm) * textWidthFactor
        val boxHeightMm = ink.topMm - ink.bottomMm
        val anchor = PointXY(text.x.toFloat(), text.y.toFloat())
        val rotationDeg = text.rotation.toFloat()
        val center = PointXY(
            anchor.x + (ink.leftMm + ink.rightMm) / 2f,
            anchor.y + (ink.bottomMm + ink.topMm) / 2f,
        ).rotate(anchor, rotationDeg)
        return LabelBox(center, widthMm = widthMm, heightMm = boxHeightMm, rotationDeg = rotationDeg)
    }
}
