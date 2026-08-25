package com.jpaver.trianglelist.web

import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.label.ModelOverlapAnalyzer

/**
 * Web エディタ用: 編集モデル (CSV) を直接読んで寸法テキストのめり込みを返す。
 * DXF 書き出しを経由しない (ModelOverlapAnalyzer、2026-08-25 user 指摘「判定ロジックは
 * dxf と関係のないモデル層に置く」)。textSize の算出式は WebPrimitiveRenderer.renderCsv と
 * 完全に揃える ── 判定が見る box と画面に実際に描かれる文字を一致させるため
 * (「描画が真実、箱はその鏡」DxfOverlapAnalyzer と同じ規律)。
 */
object WebOverlap {

    /**
     * 寸法テキスト 1 本分の当たり判定ボックス (モデル座標系、common LabelBox そのもの)。
     * JS 側は sx()/sy() の view transform をこの box にそのまま適用するだけでよい ──
     * 「判定が見ている box と目が見る box を一致させる」を JS 境界の先まで保つ
     * (2026-08-25 user 指摘: dblclick 編集用の textScreenBox 流用ではグリフ幅に box が
     * フィットしていなかった。判定 box をそのまま渡すことで解消する)。
     */
    private fun mixedAndTextSize(csv: String, scale: Float, overridesJson: String) = run {
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        val sizeRatio = (doc.textSize?.takeIf { it > 0f } ?: WebPrimitiveRenderer.APP_DEFAULT_TEXT_SIZE) /
            WebPrimitiveRenderer.APP_DEFAULT_TEXT_SIZE
        val effScale = if (scale > 0f) scale else 1f
        val textSize = WebPrimitiveRenderer.DEFAULT_TEXT_SIZE * sizeRatio * effScale
        val mixed = CsvCodec.buildMixed(doc, trilist, effScale)
        WebOverrides.applyJson(mixed, overridesJson)
        Triple(mixed, textSize, effScale to trilist.sokutenListVector)
    }

    /**
     * 寸法テキスト全件の判定ボックス + めり込み有無を JSON 配列で返す。
     * [{"id":"dim:1:0","cx":..,"cy":..,"w":..,"h":..,"rot":..,"intrusion":true}, ...]
     * cx/cy/w/h はモデル座標系 (mm 相当)、rot は度数法 (CCW 正、テキストと同じ回転規約)。
     */
    fun overlayJson(csv: String, scale: Float, overridesJson: String = "", thresholdAngle: Float = 125f): String {
        val (mixed, textSize, scaleAndVector) = mixedAndTextSize(csv, scale, overridesJson)
        val (effScale, sokutenListVector) = scaleAndVector
        val boxes = ModelOverlapAnalyzer.boxes(mixed, textSize, effScale, sokutenListVector, thresholdAngle)
        val report = ModelOverlapAnalyzer.analyze(mixed, textSize, effScale, sokutenListVector, thresholdAngle)
        val intrusionIds = report.intrusions.map { it.textId }.toSet()
        val sb = StringBuilder("[")
        boxes.forEachIndexed { i, (id, box) ->
            if (i > 0) sb.append(',')
            sb.append(
                """{"id":"$id","cx":${box.center.x},"cy":${box.center.y},""" +
                    """"w":${box.widthMm},"h":${box.heightMm},"rot":${box.rotationDeg},""" +
                    """"intrusion":${intrusionIds.contains(id)}}""",
            )
        }
        sb.append(']')
        return sb.toString()
    }
}
