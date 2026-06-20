package com.jpaver.trianglelist.dxf

/**
 * SFC (ISO-10303-21) ファイル解析クラス
 * SfcWriter の出力に対応した読み込み機能を提供し、DxfParseResult に変換する
 */
class SfcParser {
    fun parse(sfcText: String): DxfParseResult {
        val lines = mutableListOf<DxfLine>()
        val circles = mutableListOf<DxfCircle>()
        val arcs = mutableListOf<DxfArc>()
        val lwPolylines = mutableListOf<DxfLwPolyline>()
        val texts = mutableListOf<DxfText>()
        val hatches = mutableListOf<DxfHatch>()

        var drawingScaleDenominator: Float? = null

        // コマンドは #番号 = コマンド名(引数リスト) の形式
        val commandRegex = Regex("""#\d+\s*=\s*(\w+)\((.*)\)""")

        sfcText.lines().forEach { line ->
            val trimmed = line.trim()
            val match = commandRegex.find(trimmed) ?: return@forEach
            val featureName = match.groupValues[1]
            val argsStr = match.groupValues[2]

            // 引数を分割（シングルクォートで囲まれた値や、単なる数値などを抽出する）
            val args = parseArgs(argsStr)
            if (args.isEmpty()) return@forEach

            when (featureName) {
                "line_feature" -> {
                    // 'layer','color','linetype','linewidth','x1','y1','x2','y2'
                    if (args.size >= 8) {
                        val layer = args[0]
                        val sfcColor = args[1].toIntOrNull() ?: 1
                        val x1 = args[4].toDoubleOrNull() ?: 0.0
                        val y1 = args[5].toDoubleOrNull() ?: 0.0
                        val x2 = args[6].toDoubleOrNull() ?: 0.0
                        val y2 = args[7].toDoubleOrNull() ?: 0.0
                        lines.add(DxfLine(x1, y1, x2, y2, color = sfcColorToAci(sfcColor), layer = layer))
                    }
                }
                "circle_feature" -> {
                    // 'layer','color','linetype','linewidth','x','y','r'
                    if (args.size >= 7) {
                        val layer = args[0]
                        val sfcColor = args[1].toIntOrNull() ?: 1
                        val cx = args[4].toDoubleOrNull() ?: 0.0
                        val cy = args[5].toDoubleOrNull() ?: 0.0
                        val r = args[6].toDoubleOrNull() ?: 0.0
                        circles.add(DxfCircle(cx, cy, r, color = sfcColorToAci(sfcColor), layer = layer))
                    }
                }
                "text_string_feature" -> {
                    // 'layer','color','font','text','x','y','height','width','slant','angle','spacing','align','dir'
                    if (args.size >= 13) {
                        val layer = args[0]
                        val sfcColor = args[1].toIntOrNull() ?: 1
                        val text = args[3]
                        val x = args[4].toDoubleOrNull() ?: 0.0
                        val y = args[5].toDoubleOrNull() ?: 0.0
                        val height = args[6].toDoubleOrNull() ?: 1.0
                        val rotation = args[9].toDoubleOrNull() ?: 0.0
                        val alignTenkey = args[11].toIntOrNull() ?: 2
                        
                        val (alignH, alignV) = tenkeyToAlign(alignTenkey)

                        texts.add(DxfText(
                            x = x, y = y, text = text, height = height, rotation = rotation,
                            color = sfcColorToAci(sfcColor), alignH = alignH, alignV = alignV, layer = layer
                        ))

                        extractDrawingScale(text)?.let {
                            drawingScaleDenominator = it
                        }
                    }
                }
                "face_fill_feature" -> {
                    // 'layer','color','filltype','hatchtype','hatchangle','hatchspacing','points_count',x1,y1,x2,y2,...
                    if (args.size >= 7) {
                        val layer = args[0]
                        val sfcColor = args[1].toIntOrNull() ?: 6
                        val pointsCount = args[6].toIntOrNull() ?: 0
                        val vertices = mutableListOf<Pair<Double, Double>>()
                        if (args.size >= 7 + pointsCount * 2) {
                            for (i in 0 until pointsCount) {
                                val vx = args[7 + i * 2].toDoubleOrNull() ?: 0.0
                                val vy = args[7 + i * 2 + 1].toDoubleOrNull() ?: 0.0
                                vertices.add(Pair(vx, vy))
                            }
                        }

                        val trueColor = sfcColorToTrueColor(sfcColor)

                        if (vertices.isNotEmpty()) {
                            val uniqueVertices = if (vertices.size > 1 &&
                                kotlin.math.abs(vertices.first().first - vertices.last().first) < 1e-3 &&
                                kotlin.math.abs(vertices.first().second - vertices.last().second) < 1e-3) {
                                vertices.dropLast(1)
                            } else {
                                vertices
                            }

                            hatches.add(DxfHatch(
                                vertices = uniqueVertices,
                                color = sfcColorToAci(sfcColor),
                                layer = layer,
                                trueColor = trueColor
                            ))
                        }
                    }
                }
            }
        }

        return DxfParseResult(
            lines = lines,
            circles = circles,
            arcs = arcs,
            lwPolylines = lwPolylines,
            texts = texts,
            hatches = hatches,
            drawingScaleDenominator = drawingScaleDenominator
        )
    }

    private fun parseArgs(argsStr: String): List<String> {
        val list = mutableListOf<String>()
        val regex = Regex("""'(.*?)'|([^,'\s]+)""")
        regex.findAll(argsStr).forEach {
            val quoted = it.groups[1]?.value
            val unquoted = it.groups[2]?.value
            list.add(quoted ?: unquoted ?: "")
        }
        return list
    }

    private fun sfcColorToAci(sfcColor: Int): Int {
        return when (sfcColor) {
            2 -> 1  // 赤 (SFC 2 -> ACI 1)
            3 -> 3  // 緑 (SFC 3 -> ACI 3)
            4 -> 5  // 青 (SFC 4 -> ACI 5)
            5 -> 2  // 黄 (SFC 5 -> ACI 2)
            6 -> 6  // ピンク (SFC 6 -> ACI 6)
            7 -> 4  // 水色 (SFC 7 -> ACI 4)
            8 -> 7  // 白 (SFC 8 -> ACI 7)
            else -> 7
        }
    }

    private fun sfcColorToTrueColor(sfcColor: Int): Int? {
        return when (sfcColor) {
            6 -> 16769517 // HATCH_RED
            2 -> 16770756 // HATCH_ORANGE
            5 -> 16777180 // HATCH_YELLOW
            3 -> 14482130 // HATCH_GREEN
            7 -> 14939391 // HATCH_BLUE
            else -> null
        }
    }

    private fun tenkeyToAlign(tenkey: Int): Pair<Int, Int> {
        return when (tenkey) {
            1 -> Pair(0, 1) // 左下
            2 -> Pair(1, 1) // 中下
            3 -> Pair(2, 1) // 右下
            4 -> Pair(0, 2) // 左中
            5 -> Pair(1, 2) // 中中
            6 -> Pair(2, 2) // 右中
            7 -> Pair(0, 3) // 左上
            8 -> Pair(1, 3) // 中上
            9 -> Pair(2, 3) // 右上
            else -> Pair(0, 0)
        }
    }

    private fun extractDrawingScale(text: String): Float? {
        val regex = Regex("^\\s*1/(\\d+(?:\\.\\d+)?)(?:\\s|\\(|$)")
        val m = regex.find(text) ?: return null
        val denom = m.groupValues[1].toFloatOrNull()
        if (denom != null && denom > 0f) return denom
        return null
    }
}
