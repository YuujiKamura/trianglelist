package com.jpaver.trianglelist.dxf

/**
 * DXFファイル出力ユーティリティ
 * DxfLineリストからDXFテキストを生成
 */
object DxfWriter {

    /**
     * DxfLineリストからDXFテキストを生成
     */
    fun write(lines: List<DxfLine>, texts: List<DxfText> = emptyList()): String {
        val sb = StringBuilder()

        // ヘッダーセクション
        sb.appendLine("0")
        sb.appendLine("SECTION")
        sb.appendLine("2")
        sb.appendLine("HEADER")
        sb.appendLine("9")
        sb.appendLine("\$ACADVER")
        sb.appendLine("1")
        sb.appendLine("AC1015")
        sb.appendLine("9")
        sb.appendLine("\$INSUNITS")
        sb.appendLine("70")
        sb.appendLine("4")  // 4 = Millimeters
        sb.appendLine("0")
        sb.appendLine("ENDSEC")

        // エンティティセクション
        sb.appendLine("0")
        sb.appendLine("SECTION")
        sb.appendLine("2")
        sb.appendLine("ENTITIES")

        // 線を出力
        for (line in lines) {
            sb.appendLine("0")
            sb.appendLine("LINE")
            sb.appendLine("8")
            sb.appendLine(line.layer)
            sb.appendLine("62")
            sb.appendLine(line.color.toString())
            sb.appendLine("10")
            sb.appendLine(line.x1.toString())
            sb.appendLine("20")
            sb.appendLine(line.y1.toString())
            sb.appendLine("11")
            sb.appendLine(line.x2.toString())
            sb.appendLine("21")
            sb.appendLine(line.y2.toString())
        }

        // テキストを出力
        for (text in texts) {
            sb.appendLine("0")
            sb.appendLine("TEXT")
            sb.appendLine("8")
            sb.appendLine(text.layer)
            sb.appendLine("62")
            sb.appendLine(text.color.toString())
            sb.appendLine("10")
            sb.appendLine(text.x.toString())
            sb.appendLine("20")
            sb.appendLine(text.y.toString())
            sb.appendLine("40")
            sb.appendLine(text.height.toString())
            sb.appendLine("1")
            sb.appendLine(text.text)
            sb.appendLine("50")
            sb.appendLine(text.rotation.toString())
            sb.appendLine("72")
            sb.appendLine(text.alignH.toString())
            sb.appendLine("73")
            sb.appendLine(text.alignV.toString())
        }

        sb.appendLine("0")
        sb.appendLine("ENDSEC")
        sb.appendLine("0")
        sb.appendLine("EOF")

        return sb.toString()
    }

    /**
     * DxfParseResultからDXFテキストを生成
     */
    fun write(parseResult: DxfParseResult): String {
        val allLines = parseResult.lines.toMutableList()

        // ポリラインを線に変換
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
