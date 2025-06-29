package com.jpaver.trianglelist.parser

import com.example.trilib.PointXY

/**
 * DXFファイル解析クラス
 * Android版DxfFileWriterの出力に対応した読み込み機能を提供
 */
class DxfParser {
    
    private val errors = mutableListOf<DxfParseError>()
    private val warnings = mutableListOf<String>()
    private var lineNumber = 0
    
    /**
     * DXFテキストを解析してDxfParseResultを返す
     */
    fun parse(dxfText: String): DxfParseResult {
        val startTime = System.currentTimeMillis()
        errors.clear()
        warnings.clear()
        lineNumber = 0
        
        try {
            val lines = dxfText.lines()
            val iterator = lines.iterator()
            
            val result = parseDxfContent(iterator)
            val parseTime = System.currentTimeMillis() - startTime
            
            if (errors.any { it.severity == ErrorSeverity.FATAL }) {
                throw DxfParseException("Fatal error during parsing", errors)
            }
            
            return result
            
        } catch (e: Exception) {
            addError("Parse failed: ${e.message}", "GENERAL", ErrorSeverity.FATAL)
            throw DxfParseException("DXF parsing failed", errors, e)
        }
    }
    
    /**
     * DXFコンテンツの解析メイン処理
     */
    private fun parseDxfContent(iterator: Iterator<String>): DxfParseResult {
        var currentSection: String? = null
        var header: DxfHeader? = null
        val lines = mutableListOf<DxfLine>()
        val circles = mutableListOf<DxfCircle>()
        val lwPolylines = mutableListOf<DxfLwPolyline>()
        val texts = mutableListOf<DxfText>()
        
        while (iterator.hasNext()) {
            val (code, value) = readGroupCodePair(iterator) ?: continue
            
            when (code) {
                0 -> {
                    when (value) {
                        "SECTION" -> {
                            currentSection = readSectionName(iterator)
                        }
                        "ENDSEC" -> {
                            currentSection = null
                        }
                        "TEXT" -> {
                            if (currentSection == "ENTITIES") {
                                parseTextEntity(iterator)?.let { texts.add(it) }
                            }
                        }
                        "LINE" -> {
                            if (currentSection == "ENTITIES") {
                                parseLineEntity(iterator)?.let { lines.add(it) }
                            }
                        }
                        "CIRCLE" -> {
                            if (currentSection == "ENTITIES") {
                                parseCircleEntity(iterator)?.let { circles.add(it) }
                            }
                        }
                        "LWPOLYLINE" -> {
                            if (currentSection == "ENTITIES") {
                                parseLwPolylineEntity(iterator)?.let { lwPolylines.add(it) }
                            }
                        }
                        "HATCH" -> {
                            if (currentSection == "ENTITIES") {
                                // HATCHは読み飛ばす（表示用のため）
                                skipEntity(iterator)
                            }
                        }
                        else -> {
                            // 未対応エンティティは読み飛ばす
                            if (currentSection == "ENTITIES") {
                                skipEntity(iterator)
                            }
                        }
                    }
                }
                9 -> {
                    // ヘッダー変数（$で始まる）
                    if (currentSection == "HEADER" && value.startsWith("$")) {
                        if (header == null) header = DxfHeader()
                        // 必要に応じてヘッダー情報を解析
                    }
                }
            }
        }
        
        return DxfParseResult(
            lines = lines,
            circles = circles,
            lwPolylines = lwPolylines,
            texts = texts,
            header = header
        )
    }
    
    /**
     * セクション名を読み取り
     */
    private fun readSectionName(iterator: Iterator<String>): String? {
        val (code, value) = readGroupCodePair(iterator) ?: return null
        return if (code == 2) value else null
    }
    
    /**
     * TEXTエンティティの解析
     */
    private fun parseTextEntity(iterator: Iterator<String>): DxfText? {
        var text = ""
        var insertionX = 0.0
        var insertionY = 0.0
        var height = 0.2
        var rotation = 0.0
        var horizontalAlign = 0
        var verticalAlign = 0
        var color = 0
        var layer = "0"
        var handle = ""
        var textStyle = "DIMSTANDARD"
        
        while (iterator.hasNext()) {
            val (code, value) = readGroupCodePair(iterator) ?: break
            
            when (code) {
                0 -> {
                    // 次のエンティティの開始なので、イテレータを戻す必要があるが、
                    // 簡単のため現在のエンティティを完了として扱う
                    break
                }
                1 -> text = value
                5 -> handle = value
                7 -> textStyle = value
                8 -> layer = value
                10 -> insertionX = value.toDoubleOrNull() ?: 0.0
                20 -> insertionY = value.toDoubleOrNull() ?: 0.0
                40 -> height = value.toDoubleOrNull() ?: 0.2
                50 -> rotation = Math.toRadians(value.toDoubleOrNull() ?: 0.0)
                62 -> color = value.toIntOrNull() ?: 0
                72 -> horizontalAlign = value.toIntOrNull() ?: 0
                73 -> verticalAlign = value.toIntOrNull() ?: 0
            }
        }
        
        return if (text.isNotEmpty()) {
            DxfText(
                text = text,
                insertionPoint = PointXY(insertionX.toFloat(), insertionY.toFloat()),
                height = height,
                rotationAngle = rotation,
                horizontalAlignment = horizontalAlign,
                verticalAlignment = verticalAlign,
                color = color,
                layer = layer,
                handle = handle,
                textStyle = textStyle
            )
        } else null
    }
    
    /**
     * LINEエンティティの解析
     */
    private fun parseLineEntity(iterator: Iterator<String>): DxfLine? {
        var startX = 0.0
        var startY = 0.0
        var endX = 0.0
        var endY = 0.0
        var color = 0
        var layer = "0"
        var handle = ""
        
        while (iterator.hasNext()) {
            val (code, value) = readGroupCodePair(iterator) ?: break
            
            when (code) {
                0 -> break // 次のエンティティ
                5 -> handle = value
                8 -> layer = value
                10 -> startX = value.toDoubleOrNull() ?: 0.0
                20 -> startY = value.toDoubleOrNull() ?: 0.0
                11 -> endX = value.toDoubleOrNull() ?: 0.0
                21 -> endY = value.toDoubleOrNull() ?: 0.0
                62 -> color = value.toIntOrNull() ?: 0
            }
        }
        
        return DxfLine(
            start = PointXY(startX.toFloat(), startY.toFloat()),
            end = PointXY(endX.toFloat(), endY.toFloat()),
            color = color,
            layer = layer,
            handle = handle
        )
    }
    
    /**
     * CIRCLEエンティティの解析
     */
    private fun parseCircleEntity(iterator: Iterator<String>): DxfCircle? {
        var centerX = 0.0
        var centerY = 0.0
        var radius = 0.0
        var color = 0
        var layer = "0"
        var handle = ""
        
        while (iterator.hasNext()) {
            val (code, value) = readGroupCodePair(iterator) ?: break
            
            when (code) {
                0 -> break // 次のエンティティ
                5 -> handle = value
                8 -> layer = value
                10 -> centerX = value.toDoubleOrNull() ?: 0.0
                20 -> centerY = value.toDoubleOrNull() ?: 0.0
                40 -> radius = value.toDoubleOrNull() ?: 0.0
                62 -> color = value.toIntOrNull() ?: 0
            }
        }
        
        return DxfCircle(
            center = PointXY(centerX.toFloat(), centerY.toFloat()),
            radius = radius,
            color = color,
            layer = layer,
            handle = handle
        )
    }
    
    /**
     * LWPOLYLINEエンティティの解析
     */
    private fun parseLwPolylineEntity(iterator: Iterator<String>): DxfLwPolyline? {
        val vertices = mutableListOf<PointXY>()
        var closed = false
        var color = 0
        var layer = "0"
        var handle = ""
        var vertexCount = 0
        var currentX = 0.0
        var currentY = 0.0
        var expectingY = false
        
        while (iterator.hasNext()) {
            val (code, value) = readGroupCodePair(iterator) ?: break
            
            when (code) {
                0 -> break // 次のエンティティ
                5 -> handle = value
                8 -> layer = value
                10 -> {
                    currentX = value.toDoubleOrNull() ?: 0.0
                    expectingY = true
                }
                20 -> {
                    if (expectingY) {
                        currentY = value.toDoubleOrNull() ?: 0.0
                        vertices.add(PointXY(currentX.toFloat(), currentY.toFloat()))
                        expectingY = false
                    }
                }
                62 -> color = value.toIntOrNull() ?: 0
                70 -> closed = (value.toIntOrNull() ?: 0) and 1 != 0
                90 -> vertexCount = value.toIntOrNull() ?: 0
            }
        }
        
        return if (vertices.isNotEmpty()) {
            DxfLwPolyline(
                vertices = vertices,
                closed = closed,
                color = color,
                layer = layer,
                handle = handle
            )
        } else null
    }
    
    /**
     * 未対応エンティティをスキップ
     */
    private fun skipEntity(iterator: Iterator<String>) {
        while (iterator.hasNext()) {
            val (code, _) = readGroupCodePair(iterator) ?: break
            if (code == 0) break // 次のエンティティまたはセクション終了
        }
    }
    
    /**
     * グループコードと値のペアを読み取り
     */
    private fun readGroupCodePair(iterator: Iterator<String>): Pair<Int, String>? {
        if (!iterator.hasNext()) return null
        val codeLine = iterator.next().trim()
        lineNumber++
        
        if (!iterator.hasNext()) return null
        val valueLine = iterator.next().trim()
        lineNumber++
        
        val code = codeLine.toIntOrNull()
        if (code == null) {
            addWarning("Invalid group code: $codeLine at line $lineNumber")
            return null
        }
        
        return Pair(code, valueLine)
    }
    
    /**
     * エラーを追加
     */
    private fun addError(message: String, entityType: String = "", severity: ErrorSeverity = ErrorSeverity.ERROR) {
        errors.add(DxfParseError(lineNumber, message, entityType, severity))
    }
    
    /**
     * 警告を追加
     */
    private fun addWarning(message: String) {
        warnings.add("Line $lineNumber: $message")
    }
}

/**
 * DXF解析例外
 */
class DxfParseException(
    message: String,
    val parseErrors: List<DxfParseError> = emptyList(),
    cause: Throwable? = null
) : Exception(message, cause)
