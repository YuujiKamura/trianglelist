package com.jpaver.trianglelist.dxf

/**
 * DXFファイル解析クラス
 * Android版DxfFileWriterの出力に対応した読み込み機能を提供
 */
class DxfParser {
    
    /**
     * DXFテキストを解析してDxfParseResultを返す
     */
    fun parse(dxfText: String): DxfParseResult {
        try {
            val lines = dxfText.lines()
            val iterator = lines.iterator()
            
            return parseDxfContent(iterator)
            
        } catch (e: Exception) {
            // エラーが発生した場合は空の結果を返す
            return DxfParseResult()
        }
    }
    
    /**
     * 先読み可能なイテレータ
     */
    class PeekableIterator<T>(private val iterator: Iterator<T>) : Iterator<T> {
        private var peeked: T? = null
        private var hasPeeked = false

        fun peek(): T? {
            if (!hasPeeked && iterator.hasNext()) {
                peeked = iterator.next()
                hasPeeked = true
            }
            return peeked
        }

        override fun hasNext(): Boolean = hasPeeked || iterator.hasNext()

        override fun next(): T {
            return if (hasPeeked) {
                hasPeeked = false
                peeked!!
            } else {
                iterator.next()
            }
        }
    }
    
    /**
     * DXFコンテンツの解析メイン処理
     */
    private fun parseDxfContent(iterator: Iterator<String>): DxfParseResult {
        val peekableIterator = PeekableIterator(iterator)
        var currentSection: String? = null
        var header: DxfHeader? = null
        val lines = mutableListOf<DxfLine>()
        val circles = mutableListOf<DxfCircle>()
        val lwPolylines = mutableListOf<DxfLwPolyline>()
        val texts = mutableListOf<DxfText>()
        
        while (peekableIterator.hasNext()) {
            val (code, value) = readGroupCodePair(peekableIterator) ?: continue
            
            when (code) {
                0 -> {
                    when (value) {
                        "SECTION" -> {
                            currentSection = readSectionName(peekableIterator)
                        }
                        "ENDSEC" -> {
                            currentSection = null
                        }
                        "TEXT" -> {
                            if (currentSection == "ENTITIES") {
                                parseTextEntity(peekableIterator)?.let { texts.add(it) }
                            }
                        }
                        "LINE" -> {
                            if (currentSection == "ENTITIES") {
                                parseLineEntity(peekableIterator)?.let { lines.add(it) }
                            }
                        }
                        "CIRCLE" -> {
                            if (currentSection == "ENTITIES") {
                                parseCircleEntity(peekableIterator)?.let { circles.add(it) }
                            }
                        }
                        "LWPOLYLINE" -> {
                            if (currentSection == "ENTITIES") {
                                parseLwPolylineEntity(peekableIterator)?.let { lwPolylines.add(it) }
                            }
                        }
                        "HATCH" -> {
                            if (currentSection == "ENTITIES") {
                                // HATCHは読み飛ばす（表示用のため）
                                skipEntity(peekableIterator)
                            }
                        }
                        else -> {
                            // 未対応エンティティは読み飛ばす
                            if (currentSection == "ENTITIES") {
                                skipEntity(peekableIterator)
                            }
                        }
                    }
                }
                9 -> {
                    // ヘッダー変数（$で始まる）
                    if (currentSection == "HEADER" && value.startsWith("$")) {
                        if (header == null) header = DxfHeader()
                        header = parseHeaderVariable(peekableIterator, header, value)
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
     * セクション名の読み取り
     */
    private fun readSectionName(iterator: PeekableIterator<String>): String? {
        val (code, value) = readGroupCodePair(iterator) ?: return null
        return if (code == 2) value else null
    }
    
    /**
     * TEXTエンティティの解析
     */
    private fun parseTextEntity(iterator: PeekableIterator<String>): DxfText? {
        var x: Double? = null
        var y: Double? = null
        var x2: Double? = null
        var y2: Double? = null
        var text: String? = null
        var height: Double = 1.0
        var rotation: Double = 0.0
        var color = 7 // デフォルト色（白/黒）
        var alignH = 0 // 水平アライメント
        var alignV = 0 // 垂直アライメント
        
        while (iterator.hasNext()) {
            // 次のエンティティの開始をチェック
            val nextCode = iterator.peek()?.trim()
            if (nextCode == "0") break
            
            val (code, value) = readGroupCodePair(iterator) ?: continue
            
            when (code) {
                10 -> x = value.toDoubleOrNull()
                20 -> y = value.toDoubleOrNull()
                11 -> x2 = value.toDoubleOrNull()
                21 -> y2 = value.toDoubleOrNull()
                1 -> text = value
                40 -> height = value.toDoubleOrNull() ?: 1.0
                50 -> rotation = value.toDoubleOrNull() ?: 0.0
                62 -> color = value.toIntOrNull() ?: 7
                72 -> alignH = value.toIntOrNull() ?: 0
                73 -> alignV = value.toIntOrNull() ?: 0
            }
        }
        
        // alignHまたはalignVが0でない場合、第2挿入点を使用
        val useSecondPoint = (alignH != 0 || alignV != 0) && x2 != null && y2 != null
        val finalX = if (useSecondPoint) x2 else x
        val finalY = if (useSecondPoint) y2 else y
        
        return if (finalX != null && finalY != null && text != null) {
            DxfText(finalX, finalY, text, height, rotation, color, alignH, alignV)
        } else null
    }
    
    /**
     * LINEエンティティの解析
     */
    private fun parseLineEntity(iterator: PeekableIterator<String>): DxfLine? {
        var x1: Double? = null
        var y1: Double? = null
        var x2: Double? = null
        var y2: Double? = null
        var color = 7 // デフォルト色（白/黒）
        
        while (iterator.hasNext()) {
            // 次のエンティティの開始をチェック
            val nextCode = iterator.peek()?.trim()
            if (nextCode == "0") break
            
            val (code, value) = readGroupCodePair(iterator) ?: continue
            
            when (code) {
                10 -> x1 = value.toDoubleOrNull()
                20 -> y1 = value.toDoubleOrNull()
                11 -> x2 = value.toDoubleOrNull()
                21 -> y2 = value.toDoubleOrNull()
                62 -> color = value.toIntOrNull() ?: 7
            }
        }
        
        return if (x1 != null && y1 != null && x2 != null && y2 != null) {
            DxfLine(x1, y1, x2, y2, color)
        } else null
    }
    
    /**
     * CIRCLEエンティティの解析
     */
    private fun parseCircleEntity(iterator: PeekableIterator<String>): DxfCircle? {
        var centerX: Double? = null
        var centerY: Double? = null
        var radius: Double? = null
        var color = 7 // デフォルト色（白/黒）
        
        while (iterator.hasNext()) {
            // 次のエンティティの開始をチェック
            val nextCode = iterator.peek()?.trim()
            if (nextCode == "0") break
            
            val (code, value) = readGroupCodePair(iterator) ?: continue
            
            when (code) {
                10 -> centerX = value.toDoubleOrNull()
                20 -> centerY = value.toDoubleOrNull()
                40 -> radius = value.toDoubleOrNull()
                62 -> color = value.toIntOrNull() ?: 7
            }
        }
        
        return if (centerX != null && centerY != null && radius != null) {
            DxfCircle(centerX, centerY, radius, color)
        } else null
    }
    
    /**
     * LWPOLYLINEエンティティの解析
     */
    private fun parseLwPolylineEntity(iterator: PeekableIterator<String>): DxfLwPolyline? {
        val vertices = mutableListOf<Pair<Double, Double>>()
        var isClosed = false
        var color = 7 // デフォルト色（白/黒）
        var currentX: Double? = null
        
        while (iterator.hasNext()) {
            // 次のエンティティの開始をチェック
            val nextCode = iterator.peek()?.trim()
            if (nextCode == "0") break
            
            val (code, value) = readGroupCodePair(iterator) ?: continue
            
            when (code) {
                70 -> isClosed = (value.toIntOrNull() ?: 0) and 1 != 0
                10 -> currentX = value.toDoubleOrNull()
                20 -> {
                    val y = value.toDoubleOrNull()
                    if (currentX != null && y != null) {
                        vertices.add(Pair(currentX!!, y))
                        currentX = null
                    }
                }
                62 -> color = value.toIntOrNull() ?: 7
            }
        }
        
        return if (vertices.isNotEmpty()) {
            DxfLwPolyline(vertices, isClosed, color)
        } else null
    }
    
    /**
     * 未対応エンティティの読み飛ばし
     */
    private fun skipEntity(iterator: PeekableIterator<String>) {
        while (iterator.hasNext()) {
            // 次のエンティティの開始をチェック
            val nextCode = iterator.peek()?.trim()
            if (nextCode == "0") break
            
            // グループコードペアを読み飛ばし
            readGroupCodePair(iterator)
        }
    }
    
    /**
     * グループコードと値のペアを読み取り
     */
    private fun readGroupCodePair(iterator: PeekableIterator<String>): Pair<Int, String>? {
        if (!iterator.hasNext()) return null
        val codeStr = iterator.next().trim()
        if (!iterator.hasNext()) return null
        val value = iterator.next().trim()
        
        val code = codeStr.toIntOrNull() ?: return null
        return Pair(code, value)
    }
    
    /**
     * ヘッダー変数の解析
     */
    private fun parseHeaderVariable(iterator: PeekableIterator<String>, currentHeader: DxfHeader, varName: String): DxfHeader {
        return when (varName) {
            "\$ACADVER" -> {
                val (_, value) = readGroupCodePair(iterator) ?: return currentHeader
                currentHeader.copy(acadVer = value)
            }
            "\$INSUNITS" -> {
                val (_, value) = readGroupCodePair(iterator) ?: return currentHeader
                currentHeader.copy(insUnits = value.toIntOrNull() ?: 6)
            }
            "\$EXTMIN" -> {
                val x = readGroupCodePair(iterator)?.second?.toDoubleOrNull() ?: 0.0
                val y = readGroupCodePair(iterator)?.second?.toDoubleOrNull() ?: 0.0 
                val z = readGroupCodePair(iterator)?.second?.toDoubleOrNull() ?: 0.0
                currentHeader.copy(extMin = Triple(x, y, z))
            }
            "\$EXTMAX" -> {
                val x = readGroupCodePair(iterator)?.second?.toDoubleOrNull() ?: 0.0
                val y = readGroupCodePair(iterator)?.second?.toDoubleOrNull() ?: 0.0
                val z = readGroupCodePair(iterator)?.second?.toDoubleOrNull() ?: 0.0
                currentHeader.copy(extMax = Triple(x, y, z))
            }
            "\$LIMMIN" -> {
                val x = readGroupCodePair(iterator)?.second?.toDoubleOrNull() ?: 0.0
                val y = readGroupCodePair(iterator)?.second?.toDoubleOrNull() ?: 0.0
                currentHeader.copy(limMin = Pair(x, y))
            }
            "\$LIMMAX" -> {
                val x = readGroupCodePair(iterator)?.second?.toDoubleOrNull() ?: 0.0
                val y = readGroupCodePair(iterator)?.second?.toDoubleOrNull() ?: 0.0
                currentHeader.copy(limMax = Pair(x, y))
            }
            "\$INSBASE" -> {
                val x = readGroupCodePair(iterator)?.second?.toDoubleOrNull() ?: 0.0
                val y = readGroupCodePair(iterator)?.second?.toDoubleOrNull() ?: 0.0
                val z = readGroupCodePair(iterator)?.second?.toDoubleOrNull() ?: 0.0
                currentHeader.copy(insBase = Triple(x, y, z))
            }
            "\$DIMSCALE" -> {
                val (_, value) = readGroupCodePair(iterator) ?: return currentHeader
                currentHeader.copy(dimScale = value.toDoubleOrNull() ?: 1.0)
            }
            else -> {
                // 未対応のヘッダー変数は読み飛ばす
                readGroupCodePair(iterator)
                currentHeader
            }
        }
    }
}
