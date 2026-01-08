package com.jpaver.trianglelist.dxf

/**
 * DxfParseResultの拡張関数
 * エンティティを追加するためのヘルパー関数を提供
 */
fun DxfParseResult.addLines(newLines: List<DxfLine>): DxfParseResult {
    return this.copy(lines = this.lines + newLines)
}

fun DxfParseResult.addCircles(newCircles: List<DxfCircle>): DxfParseResult {
    return this.copy(circles = this.circles + newCircles)
}

fun DxfParseResult.addPolylines(newPolylines: List<DxfLwPolyline>): DxfParseResult {
    return this.copy(lwPolylines = this.lwPolylines + newPolylines)
}

fun DxfParseResult.addTexts(newTexts: List<DxfText>): DxfParseResult {
    return this.copy(texts = this.texts + newTexts)
}



















