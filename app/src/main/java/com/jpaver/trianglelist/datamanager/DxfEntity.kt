package com.jpaver.trianglelist.datamanager

import com.example.trilib.PointXY
import com.jpaver.trianglelist.viewmodel.formattedString
import java.io.BufferedWriter

class DxfEntity(
    private val handleGen: HandleGen,
    private var unitscale_: Float,
    private var activeLayer: String = "0"
) {

    fun writeTextHV(
        writer: BufferedWriter,
        text: String,
        point: PointXY,
        color: Int,
        textsize: Float,
        alignH: Int,
        alignV: Int,
        angle: Float
    ) {
        var x = point.x * unitscale_
        var y = point.y * unitscale_
        val ts = textsize * unitscale_

        // Offset adjustment for vertical alignment
        if (alignV == 3) {
            if (angle < 0) {
                x -= 50f
                y -= 50f
            }
        }

        val handle = handleGen.new()

        writer.write("""
            0
            TEXT
            5
            $handle
            330
            36
            100
            AcDbEntity
            8
            $activeLayer
            62
            $color
            100
            AcDbText
            10
            $x
            20
            $y
            30
            0.0
            40
            ${ts.formattedString(0)}
            1
            $text
            41
            1.00
            7
            DimStandard
            72
            $alignH
            11
            $x
            21
            $y
            31
            0.0
            50
            $angle
            51
            0.0
            100
            AcDbText
            73
            $alignV
        """.trimIndent())
        writer.newLine()
    }

    fun writeLine(
        writer: BufferedWriter,
        p1: PointXY,
        p2: PointXY,
        color: Int
    ) {
        val ax = p1.x * unitscale_
        val ay = p1.y * unitscale_
        val bx = p2.x * unitscale_
        val by = p2.y * unitscale_

        val handle = handleGen.new()

        writer.write("""
            0
            LINE
            5
            $handle
            330
            36
            100
            AcDbEntity
            8
            $activeLayer
            100
            AcDbLine
            370
            -3
            10
            $ax
            20
            $ay
            30
            0.0
            11
            $bx
            21
            $by
            31
            0.0
            62
            $color
        """.trimIndent())
        writer.newLine()
    }

    fun writeCircle(
        writer: BufferedWriter,
        point: PointXY,
        size: Float,
        color: Int
    ) {
        val x = point.x * unitscale_
        val y = point.y * unitscale_
        val s = size * unitscale_

        val handle = handleGen.new()

        writer.write("""
            0
            CIRCLE
            5
            $handle
            330
            36
            100
            AcDbEntity
            8
            $activeLayer
            62
            $color
            370
            13
            100
            AcDbCircle
            10
            $x
            20
            $y
            30
            0.0
            40
            $s 
        """.trimIndent())
        writer.newLine()
    }

    fun writeTextAndLine(
        writer: BufferedWriter,
        st: String,
        p1: PointXY,
        p2: PointXY,
        textsize: Float
    ) {
        writeTextHV(writer, st, p1.plus(textsize, textsize * 0.2f), 1, textsize, 0, 1, 0f)
        writeLine(writer, p1, p2, 1)
    }

    fun writeDXFTriHatch(
        writer: BufferedWriter,
        array: ArrayList<PointXY>,
        color: Int,
        sixtytwo: Int
    ) {
        val handle = handleGen.new()

        writer.write("""
            0
            HATCH
            5
            $handle
            100
            AcDbEntity
            8
            C-COL-COL1
            62
            $sixtytwo
            420
            $color
            370
            -3
            100
            AcDbHatch
            10
            0.0
            20
            0.0
            30
            0.0
            2
            SOLID
            70
            1
            71
            0
            91
            1
            92
            1
            93
            ${array.size}
            
        """.trimIndent())

        for (index in 0 until array.size) {
            if (index + 1 < array.size) {
                writer.write("""
                72
                1
                10
                ${array[index].x * unitscale_}
                20
                ${(array[index].y * unitscale_)}                
                11
                ${(array[index + 1].x * unitscale_)}
                21
                ${(array[index + 1].y * unitscale_)}                
            """.trimIndent())
                writer.newLine()
            }
        }

        writer.write("""
            97
            0
            75
            0
            76
            1
            98
            1
            10
            0.0
            20
            0.0
            
        """.trimIndent())
    }

    fun writeDXFTriOutlines(
        writer: BufferedWriter,
        array: ArrayList<PointXY>
    ) {
        val handle = handleGen.new()

        writer.write("""
            0
            LWPOLYLINE
            5
            $handle
            100
            AcDbEntity
            8
            C-COL-COL1
            100
            AcDbPolyline
            370
            13
            90
            ${array.size}
            70
            1
            43
            0.0
        """.trimIndent())
        writer.newLine()

        for (index in 0 until array.size) {
            writer.write("""
                10
                ${(array[index].x * unitscale_)}
                20
                ${(array[index].y * unitscale_)}                
            """.trimIndent())
            writer.newLine()
        }
    }

    // Setter methods for updating values
    fun setUnitScale(unitscale: Float) {
        this.unitscale_ = unitscale
    }

    fun setActiveLayer(layer: String) {
        this.activeLayer = layer
    }
}
