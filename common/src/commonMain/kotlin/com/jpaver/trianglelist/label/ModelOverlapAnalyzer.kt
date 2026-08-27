package com.jpaver.trianglelist.label

import com.example.trilib.PointXY
import com.jpaver.trianglelist.editmodel.CycleShape
import com.jpaver.trianglelist.editmodel.DimensionSpec
import com.jpaver.trianglelist.editmodel.EditList

/**
 * 編集モデル (EditList<CycleShape>) を直接読んで寸法テキストの重なりを判定する。
 * DxfOverlapAnalyzer と役割は同じ (CollisionField への登録・クエリ・OverlapReport 化) だが、
 * 入力が DXF パース結果ではなく編集モデルそのもの ── DXF 書き出しを経由しない分、
 * エディタが「今まさに保持している図形」に対してそのまま判定できる
 * (2026-08-25 user 指摘: 判定ロジックは dxf と関係のないモデル層に置く、
 * エディタ上で図形の大きさを変えたときに重なりを確認したい)。
 *
 * 判定対象は寸法値テキストのみ (番号 TEXT は DxfOverlapAnalyzer rev6 と同じ理由で対象外 ──
 * 円と同心で一緒に動くだけなので、円だけを障害物として登録する)。
 */
object ModelOverlapAnalyzer {

    fun analyze(
        list: EditList<out CycleShape>,
        textSize: Float,
        scale: Float = 1f,
        sokutenListVector: Int = 0,
        thresholdAngle: Float = 125f,
        metrics: LabelMetrics = LabelMetrics.Approximate,
    ): OverlapReport {
        val entries = boxes(list, textSize, scale, sokutenListVector, thresholdAngle, metrics)

        val field = CollisionField()
        list.forEachItemIndexed { num, obj ->
            obj.edges().forEachIndexed { side, line ->
                field.addEdge("edge:$num:$side", line.left, line.right)
            }
            val circleR = (textSize * 0.85f).toDouble()
            field.addCircle("circle:$num", obj.pointNumberAnchor(), circleR)
        }
        for ((id, box) in entries) field.addBox(id, box)

        var overlappingTexts = 0
        val seenPairKeys = mutableSetOf<String>()
        val pairs = mutableListOf<OverlapPair>()
        for ((id, box) in entries) {
            val hits = field.query(box, excludeId = id)
            if (hits.isNotEmpty()) overlappingTexts++
            for (hit in hits) {
                val key = if (id < hit.id) "$id|${hit.id}" else "${hit.id}|$id"
                if (!seenPairKeys.add(key)) continue
                pairs.add(OverlapPair(textId = id, otherId = hit.id, otherKind = hit.kind, depthMm = hit.depthMm))
            }
        }

        return OverlapReport(totalTexts = entries.size, overlappingTexts = overlappingTexts, pairs = pairs)
    }

    /**
     * 判定対象の寸法テキストの (id, LabelBox) を返す。analyze() と viewer のオーバーレイが
     * 共用する唯一の生成経路 ── 判定が見ている box と目が見る box を一致させる
     * (DxfOverlapAnalyzer.textBoxes と同じ規律: 描画用に別実装したら検証にならない、
     * 2026-08-25 user 指摘「テキストグリフの幅ぴったりに枠がフィットしてない」で判明した
     * 教訓 ── web-js 側が dblclick 編集用の textScreenBox を流用していたのが原因だった)。
     */
    fun boxes(
        list: EditList<out CycleShape>,
        textSize: Float,
        scale: Float = 1f,
        sokutenListVector: Int = 0,
        thresholdAngle: Float = 125f,
        metrics: LabelMetrics = LabelMetrics.Approximate,
    ): List<Pair<String, LabelBox>> {
        // WebPrimitiveRenderer.render の「段階0」と同じ順序 (textSize/閾値を配ってから
        // emitDimensionSpecs する) ── 実際に描画される寸法配置と同値の box を作るため。
        list.forEachItem { obj ->
            obj.applyDimTextSize(textSize)
            obj.dimThresholdAngle = thresholdAngle
        }
        val entries = mutableListOf<Pair<String, LabelBox>>()
        list.forEachItemIndexed { num, obj ->
            entries.addAll(boxesOf(obj, num, textSize, scale, sokutenListVector, metrics))
        }
        return entries
    }

    /**
     * 図形 1 つ分の (id, LabelBox)。配置の探索 (寸法値の退避) が「1 本だけ動かして
     * 試す」ときに、全件を作り直さないための入口。
     *
     * 1 本の寸法値の配置は**その図形の形と自分の horizontal コードだけ**で決まる
     * (emitDimensionSpecs は他図形を見ない) ので、動かした図形の分だけ作り直せば足りる。
     * 全件再計算だと 1 候補あたり 57 本 × メトリクス計算が走り、実データで探索が
     * 134ms かかっていた (2026-08-27 実測) ── モデルの更新ごとに走らせられる速さではない。
     *
     * 注意: applyDimTextSize / dimThresholdAngle の配布は呼び出し側の責任 (boxes() は
     * 先頭でやっている)。
     */
    fun boxesOf(
        shape: CycleShape,
        num: Int,
        textSize: Float,
        scale: Float = 1f,
        sokutenListVector: Int = 0,
        metrics: LabelMetrics = LabelMetrics.Approximate,
    ): List<Pair<String, LabelBox>> =
        shape.emitDimensionSpecs(if (scale > 0f) scale else 1f, sokutenListVector)
            .map { spec -> "dim:$num:${spec.side}" to toLabelBox(spec, textSize.toDouble(), metrics) }

    /**
     * 既に作ってある box 群に対して衝突を集計する (box の再計算をしない版)。
     * 配置の探索が候補ごとに呼ぶので、ここは純粋な幾何クエリだけに保つ。
     */
    fun analyzeBoxes(
        list: EditList<out CycleShape>,
        entries: List<Pair<String, LabelBox>>,
        textSize: Float,
    ): OverlapReport {
        val field = CollisionField()
        list.forEachItemIndexed { num, obj ->
            obj.edges().forEachIndexed { side, line ->
                field.addEdge("edge:$num:$side", line.left, line.right)
            }
            field.addCircle("circle:$num", obj.pointNumberAnchor(), (textSize * 0.85f).toDouble())
        }
        for ((id, box) in entries) field.addBox(id, box)

        var overlappingTexts = 0
        val seenPairKeys = mutableSetOf<String>()
        val pairs = mutableListOf<OverlapPair>()
        for ((id, box) in entries) {
            val hits = field.query(box, excludeId = id)
            if (hits.isNotEmpty()) overlappingTexts++
            for (hit in hits) {
                val key = if (id < hit.id) "$id|${hit.id}" else "${hit.id}|$id"
                if (!seenPairKeys.add(key)) continue
                pairs.add(OverlapPair(textId = id, otherId = hit.id, otherKind = hit.kind, depthMm = hit.depthMm))
            }
        }
        return OverlapReport(totalTexts = entries.size, overlappingTexts = overlappingTexts, pairs = pairs)
    }

    /**
     * DimensionSpec → LabelBox 変換。DxfOverlapAnalyzer.toLabelBox の模写だが、
     * DXF の TEXT 回転/整列コードではなく DimensionSpec.angle / place.verticalDxf を直接使う
     * (書き出しを経由しないので DXF 表現に翻訳し直す必要がない)。
     * alignH は寸法値テキストが常に中央寄せ (DrawingFileWriter.kt:175 の DrawPrim.Text 第5引数=1 と同じ)。
     */
    private fun toLabelBox(spec: DimensionSpec, heightMm: Double, metrics: LabelMetrics): LabelBox {
        val ink = metrics.inkBoxLocal(spec.text, heightMm, alignH = 1, alignV = spec.place.verticalDxf)
        val widthMm = ink.rightMm - ink.leftMm
        val boxHeightMm = ink.topMm - ink.bottomMm
        val anchor = spec.place.dimpoint
        val center = PointXY(
            anchor.x + (ink.leftMm + ink.rightMm) / 2.0,
            anchor.y + (ink.bottomMm + ink.topMm) / 2.0,
        ).rotate(anchor, spec.angle)
        return LabelBox(center, widthMm = widthMm, heightMm = boxHeightMm, rotationDeg = spec.angle)
    }
}
