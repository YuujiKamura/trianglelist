package com.jpaver.trianglelist.dxf

/**
 * DXFファイル出力ユーティリティ
 * 完全な5セクション構造（HEADER→TABLES→BLOCKS→ENTITIES→OBJECTS→EOF）を生成
 */
object DxfWriter {

    /**
     * DxfLineリストから完全なDXFテキストを生成
     */
    fun write(
        lines: List<DxfLine>,
        texts: List<DxfText> = emptyList(),
        circles: List<DxfCircle> = emptyList(),
        insUnits: Int = DxfConstants.Units.MILLIMETER
    ): String {
        val sb = StringBuilder()
        val h = DxfHandleGen()
        val paper = DxfPaper.A3_LAND
        val scale = DxfPrintScale(1f, 50f)

        // HEADER
        DxfHeaderWriter(h).write(sb, paper, scale, insUnits)

        // TABLES + BLOCKS
        val blkPaper = DxfTablesBuilder.write(sb)

        // ENTITIES
        sb.appendLine("  0")
        sb.appendLine("SECTION")
        sb.appendLine("  2")
        sb.appendLine("ENTITIES")

        for (line in lines) {
            val handle = h.new()
            sb.appendLine("  0")
            sb.appendLine("LINE")
            sb.appendLine("  5")
            sb.appendLine(handle)
            sb.appendLine("330")
            sb.appendLine(DxfConstants.Handles.MODEL_SPACE)
            sb.appendLine("100")
            sb.appendLine("AcDbEntity")
            sb.appendLine("  8")
            sb.appendLine(line.layer)
            sb.appendLine(" 62")
            sb.appendLine(line.color.toString())
            sb.appendLine("100")
            sb.appendLine("AcDbLine")
            sb.appendLine(" 10")
            sb.appendLine(line.x1.toString())
            sb.appendLine(" 20")
            sb.appendLine(line.y1.toString())
            sb.appendLine(" 30")
            sb.appendLine("0.0")
            sb.appendLine(" 11")
            sb.appendLine(line.x2.toString())
            sb.appendLine(" 21")
            sb.appendLine(line.y2.toString())
            sb.appendLine(" 31")
            sb.appendLine("0.0")
        }

        for (text in texts) {
            val handle = h.new()
            sb.appendLine("  0")
            sb.appendLine("TEXT")
            sb.appendLine("  5")
            sb.appendLine(handle)
            sb.appendLine("330")
            sb.appendLine(DxfConstants.Handles.MODEL_SPACE)
            sb.appendLine("100")
            sb.appendLine("AcDbEntity")
            sb.appendLine("  8")
            sb.appendLine(text.layer)
            sb.appendLine(" 62")
            sb.appendLine(text.color.toString())
            sb.appendLine("100")
            sb.appendLine("AcDbText")
            sb.appendLine(" 10")
            sb.appendLine(text.x.toString())
            sb.appendLine(" 20")
            sb.appendLine(text.y.toString())
            sb.appendLine(" 30")
            sb.appendLine("0.0")
            sb.appendLine(" 40")
            sb.appendLine(text.height.toString())
            sb.appendLine("  1")
            sb.appendLine(text.text)
            sb.appendLine(" 50")
            sb.appendLine(text.rotation.toString())
            sb.appendLine(" 72")
            sb.appendLine(text.alignH.toString())
            sb.appendLine(" 11")
            sb.appendLine(text.x.toString())
            sb.appendLine(" 21")
            sb.appendLine(text.y.toString())
            sb.appendLine(" 31")
            sb.appendLine("0.0")
            sb.appendLine("100")
            sb.appendLine("AcDbText")
            sb.appendLine(" 73")
            sb.appendLine(text.alignV.toString())
        }

        for (circle in circles) {
            val handle = h.new()
            sb.appendLine("  0")
            sb.appendLine("CIRCLE")
            sb.appendLine("  5")
            sb.appendLine(handle)
            sb.appendLine("330")
            sb.appendLine(DxfConstants.Handles.MODEL_SPACE)
            sb.appendLine("100")
            sb.appendLine("AcDbEntity")
            sb.appendLine("  8")
            sb.appendLine(circle.layer)
            sb.appendLine(" 62")
            sb.appendLine(circle.color.toString())
            sb.appendLine("100")
            sb.appendLine("AcDbCircle")
            sb.appendLine(" 10")
            sb.appendLine(circle.centerX.toString())
            sb.appendLine(" 20")
            sb.appendLine(circle.centerY.toString())
            sb.appendLine(" 30")
            sb.appendLine("0.0")
            sb.appendLine(" 40")
            sb.appendLine(circle.radius.toString())
        }

        sb.appendLine("  0")
        sb.appendLine("ENDSEC")

        // OBJECTS + EOF
        DxfObjectsBuilder(h).write(sb, paper, scale, blkPaper)

        // HANDSEED を最終値に置換（全ハンドル生成後の値）
        val result = sb.toString()
        return result.replace(DxfHeaderWriter.HANDSEED_PLACEHOLDER, h.current())
    }

    /**
     * DxfParseResultからDXFテキストを生成
     */
    fun write(parseResult: DxfParseResult): String {
        val allLines = parseResult.lines.toMutableList()

        for (polyline in parseResult.lwPolylines) {
            val vertices = polyline.vertices
            for (i in 0 until vertices.size - 1) {
                allLines.add(DxfLine(
                    vertices[i].first, vertices[i].second,
                    vertices[i + 1].first, vertices[i + 1].second,
                    polyline.color, polyline.layer
                ))
            }
            if (polyline.isClosed && vertices.size > 2) {
                allLines.add(DxfLine(
                    vertices.last().first, vertices.last().second,
                    vertices.first().first, vertices.first().second,
                    polyline.color, polyline.layer
                ))
            }
        }

        return write(allLines, parseResult.texts)
    }
}
