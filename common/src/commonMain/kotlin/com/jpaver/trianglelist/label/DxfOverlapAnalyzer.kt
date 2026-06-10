package com.jpaver.trianglelist.label

import com.example.trilib.PointXY
import com.jpaver.trianglelist.dxf.DxfParseResult
import com.jpaver.trianglelist.dxf.DxfText
import kotlin.math.hypot

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
    val depthMm: Double,
)

/**
 * 番号サークルと認識された TEXT↔CIRCLE のペア (rev6)。番号サークルは円が当たり判定の
 * 主体で、内部の番号 TEXT は円に内包されて一緒に動くだけ ── 判定の世界に入れない。
 */
data class CircledNumber(val textId: String, val circleId: String)

/** 図面 1 枚分の重なり集計。事実の数値のみで、良し悪しの判定や閾値は持たない。 */
data class OverlapReport(
    /** 判定対象の TEXT 数 (サークル番号としてペアリングされた TEXT を除いた後)。 */
    val totalTexts: Int,
    val overlappingTexts: Int,
    val pairs: List<OverlapPair>,
    /** ペアリング結果 (観測の透明性のため、どの TEXT がどの円の番号と認識されたか)。 */
    val circledNumbers: List<CircledNumber> = emptyList(),
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
                PointXY(line.x1, line.y1),
                PointXY(line.x2, line.y2),
            )
        }
        parseResult.circles.forEachIndexed { index, circle ->
            field.addCircle(
                "circle:$index",
                PointXY(circle.centerX, circle.centerY),
                circle.radius,
            )
        }
        val world = textWorld(parseResult, textWidthFactor, metrics)
        for ((id, box) in world.active) {
            field.addBox(id, box)
        }

        var overlappingTexts = 0
        val seenPairKeys = mutableSetOf<String>()
        val pairs = mutableListOf<OverlapPair>()
        for ((id, box) in world.active) {
            val hits = field.query(box, excludeId = id)
            if (hits.isNotEmpty()) overlappingTexts++
            for (hit in hits) {
                val key = if (id < hit.id) "$id|${hit.id}" else "${hit.id}|$id"
                if (!seenPairKeys.add(key)) continue
                pairs.add(OverlapPair(textId = id, otherId = hit.id, otherKind = hit.kind, depthMm = hit.depthMm))
            }
        }
        return OverlapReport(
            totalTexts = world.active.size,
            overlappingTexts = overlappingTexts,
            pairs = pairs,
            circledNumbers = world.circledNumbers,
        )
    }

    /** 判定の世界に入る TEXT (active) と、番号サークルとして外された TEXT のペアリング結果。 */
    data class TextWorld(
        val active: List<Pair<String, LabelBox>>,
        val circledNumbers: List<CircledNumber>,
    )

    /**
     * 全 TEXT を box 化し、CIRCLE と同心ペアリングして判定の世界を作る (rev6)。
     * サークル番号と認識された TEXT は障害物・被判定の両方から構造的に消える。
     * CIRCLE 自体は従来どおり一級プリミティブとして残る。
     */
    fun textWorld(
        parseResult: DxfParseResult,
        textWidthFactor: Float = 1.0f,
        metrics: LabelMetrics = LabelMetrics.Approximate,
    ): TextWorld {
        val active = mutableListOf<Pair<String, LabelBox>>()
        val circledNumbers = mutableListOf<CircledNumber>()
        parseResult.texts.forEachIndexed { index, text ->
            val id = textId(index, text.text)
            val box = toLabelBox(text, textWidthFactor, metrics)
            val circleIndex = parseResult.circles.indexOfFirst { isCircledNumber(box, it) }
            if (circleIndex >= 0) {
                circledNumbers.add(CircledNumber(textId = id, circleId = "circle:$circleIndex"))
            } else {
                active.add(id to box)
            }
        }
        return TextWorld(active, circledNumbers)
    }

    /**
     * 判定対象の TEXT の (id, LabelBox) を返す。analyze() と viewer の box overlay が
     * 共用する唯一の生成経路 ── 判定が見ている box と目が見る box を一致させる
     * (rev2: 描画用に別実装したら検証にならない)。サークル番号は判定の世界に
     * 存在しないので、ここにも出てこない (= overlay も描かない)。
     */
    fun textBoxes(
        parseResult: DxfParseResult,
        textWidthFactor: Float = 1.0f,
        metrics: LabelMetrics = LabelMetrics.Approximate,
    ): List<Pair<String, LabelBox>> = textWorld(parseResult, textWidthFactor, metrics).active

    /**
     * TEXT が円の「番号」か (= 番号サークルの複合オブジェクトの一部か)。
     * 同心条件: writer は番号サークルを番号 TEXT と同心に描く (DxfFileWriter.kt:167
     * writePointNumber の textSize*0.85f 円)。完全同心が建前で、半径×0.5 の許容は
     * アンカー補正やインク中心のずれの吸収幅 ── 円の縁にかすった無関係テキストの
     * 誤ペアリングは次の内包条件と合わせて防ぐ。
     * 内包条件: box の 4 隅がすべて円内 (番号は円の中に収まって描かれる)。
     */
    private fun isCircledNumber(box: LabelBox, circle: com.jpaver.trianglelist.dxf.DxfCircle): Boolean {
        val centerX = circle.centerX
        val centerY = circle.centerY
        val radiusMm = circle.radius
        val centerDistance = hypot(box.center.x - centerX, box.center.y - centerY)
        if (centerDistance > radiusMm * 0.5) return false
        return box.corners().all { corner ->
            hypot(corner.x - centerX, corner.y - centerY) <= radiusMm + LabelBox.EPS
        }
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
        val heightMm = text.height
        val ink = metrics.inkBoxLocal(text.text, heightMm, text.alignH, text.alignV)
        val widthMm = (ink.rightMm - ink.leftMm) * textWidthFactor
        val boxHeightMm = ink.topMm - ink.bottomMm
        val anchor = PointXY(text.x, text.y)
        val rotationDeg = text.rotation
        val center = PointXY(
            anchor.x + (ink.leftMm + ink.rightMm) / 2.0,
            anchor.y + (ink.bottomMm + ink.topMm) / 2.0,
        ).rotate(anchor, rotationDeg)
        return LabelBox(center, widthMm = widthMm, heightMm = boxHeightMm, rotationDeg = rotationDeg)
    }
}
