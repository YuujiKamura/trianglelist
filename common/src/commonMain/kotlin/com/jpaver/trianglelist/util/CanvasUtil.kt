package com.jpaver.trianglelist.util

import com.jpaver.trianglelist.dxf.DxfParseResult
import com.jpaver.trianglelist.dxf.DxfLine
import com.jpaver.trianglelist.dxf.DxfCircle
import com.jpaver.trianglelist.dxf.DxfArc
import com.jpaver.trianglelist.dxf.DxfLwPolyline
import com.jpaver.trianglelist.dxf.DxfText

/**
 * Canvas 用のユーティリティクラス
 * DXF データの Y 軸反転など共通処理を提供
 */
object CanvasUtil {
    /**
     * DXF データの全エンティティの Y 座標を反転する
     */
    fun flipYAxis(parseResult: DxfParseResult): DxfParseResult = parseResult.copy(
        lines = parseResult.lines.map { it.copy(y1 = -it.y1, y2 = -it.y2) },
        circles = parseResult.circles.map { it.copy(centerY = -it.centerY) },
        arcs = parseResult.arcs.map { it.copy(centerY = -it.centerY) },
        lwPolylines = parseResult.lwPolylines.map { poly ->
            poly.copy(vertices = poly.vertices.map { (x, y) -> x to -y })
        },
        texts = parseResult.texts.map { it.copy(y = -it.y) },
        header = parseResult.header?.copy(
            acadVer = parseResult.header.acadVer,
            insUnits = parseResult.header.insUnits,
            extMin = Triple(parseResult.header.extMin.first, -parseResult.header.extMin.second, parseResult.header.extMin.third),
            extMax = Triple(parseResult.header.extMax.first, -parseResult.header.extMax.second, parseResult.header.extMax.third),
            limMin = parseResult.header.limMin.first to -parseResult.header.limMin.second,
            limMax = parseResult.header.limMax.first to -parseResult.header.limMax.second,
            insBase = Triple(parseResult.header.insBase.first, -parseResult.header.insBase.second, parseResult.header.insBase.third),
            dimScale = parseResult.header.dimScale
        )
    )
}
