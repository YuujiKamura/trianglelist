package com.jpaver.trianglelist.datamanager

import java.io.BufferedWriter

/**
 * Writes the minimum required TABLES section so that AutoCAD / DWG viewers can
 * open the DXF without handle-collision errors. All handles are generated from
 * the shared [HandleGen] passed in by the caller, guaranteeing uniqueness
 * across the whole file.
 */
class TablesBuilder {

    /**
     * Writes TABLES and returns the handle of the *Paper_Space* BLOCK_RECORD
     * so that caller can pass it to [ObjectsBuilder].
     */
    fun writeMinimalTables(
        w: BufferedWriter,
        @Suppress("UNUSED_PARAMETER") layers: List<String> = listOf("0")
    ): String {
        // Use the exact same format as DxfTable - string literal approach
        w.write("""
              0
            SECTION
              2
            TABLES
              0
            TABLE
              2
            LTYPE
              5
            5
            330
            0
            100
            AcDbSymbolTable
             70
                 1
              0
            LTYPE
              5
            28
            330
            5
            100
            AcDbSymbolTableRecord
            100
            AcDbLinetypeTableRecord
              2
            ByBlock
             70
                 0
              3
            
             72
                65
             73
                 0
             40
            0.0
              0
            LTYPE
              5
            29
            330
            5
            100
            AcDbSymbolTableRecord
            100
            AcDbLinetypeTableRecord
              2
            ByLayer
             70
                 0
              3
            
             72
                65
             73
                 0
             40
            0.0
              0
            LTYPE
              5
            2A
            330
            5
            100
            AcDbSymbolTableRecord
            100
            AcDbLinetypeTableRecord
              2
            Continuous
             70
                 0
              3
            Solid line
             72
                65
             73
                 0
             40
            0.0
              0
            ENDTAB
              0
            TABLE
              2
            LAYER
              5
            2
            330
            0
            100
            AcDbSymbolTable
             70
                 1
              0
            LAYER
              5
            25
            330
            2
            100
            AcDbSymbolTableRecord
            100
            AcDbLayerTableRecord
              2
            0
             70
                 0
             62
                 7
              6
            Continuous
            370
                -3
            390
            F
              0
            LAYER
              5
            74
            330
            2
            100
            AcDbSymbolTableRecord
            100
            AcDbLayerTableRecord
              2
            C-COL-COL1
             70
                 0
             62
                 7
              6
            Continuous
            370
                -3
            390
            F
            347
            46
              0
            LAYER
              5
            74
            330
            2
            100
            AcDbSymbolTableRecord
            100
            AcDbLayerTableRecord
              2
            C-TTL-FRAM
             70
                 0
             62
                 7
              6
            Continuous
            370
                -3
            390
            F
            347
            46
              0
            ENDTAB
              0
            TABLE
              2
            STYLE
              5
            3
            330
            0
            100
            AcDbSymbolTable
             70
                 2
              0
            STYLE
              5
            26
            330
            3
            100
            AcDbSymbolTableRecord
            100
            AcDbTextStyleTableRecord
              2
            Standard
             70
                 0
             40
            0.0
             41
            1.0
             50
            0.0
             71
                 0
             42
            0.2
              3
            
              4
            
            1001
            ACAD
            1000
            @MS Gothic
            1071
                32817
              0
            STYLE
              5
            3B
            330
            3
            100
            AcDbSymbolTableRecord
            100
            AcDbTextStyleTableRecord
              2
            DIMSTANDARD
             70
                 0
             40
            0.0
             41
            1.0
             50
            0.0
             71
                 0
             42
            0.2
              3
            
              4
            
            1001
            ACAD
            1000
            MS Gothic
            1071
                32817
              0
            ENDTAB
              0
            TABLE
              2
            VIEW
              5
            6
            330
            0
            100
            AcDbSymbolTable
             70
                 0
              0
            ENDTAB
              0
            TABLE
              2
            UCS
              5
            7
            330
            0
            100
            AcDbSymbolTable
             70
                 0
              0
            ENDTAB
              0
            TABLE
              2
            APPID
              5
            9
            330
            0
            100
            AcDbSymbolTable
             70
                 1
              0
            APPID
              5
            27
            330
            9
            100
            AcDbSymbolTableRecord
            100
            AcDbRegAppTableRecord
              2
            ACAD
             70
                 0
              0
            ENDTAB
              0
            TABLE
              2
            DIMSTYLE
              5
            A
            330
            0
            100
            AcDbSymbolTable
             70
                 1
            100
            AcDbDimStyleTable
             71
                 0
              0
            DIMSTYLE
            105
            67
            330
            A
            100
            AcDbSymbolTableRecord
            100
            AcDbDimStyleTableRecord
              2
            Standard
             70
                 0
            340
            26
              0
            ENDTAB
              0
            TABLE
              2
            BLOCK_RECORD
              5
            1
            330
            0
            100
            AcDbSymbolTable
             70
                 0
              0
            BLOCK_RECORD
              5
            36
            330
            1
            100
            AcDbSymbolTableRecord
            100
            AcDbBlockTableRecord
              2
            *Model_Space
            340
            39
              0
            BLOCK_RECORD
              5
            32
            330
            1
            100
            AcDbSymbolTableRecord
            100
            AcDbBlockTableRecord
              2
            *Paper_Space
            340
            35
              0
            ENDTAB
              0
            ENDSEC
              0
            SECTION
              2
            BLOCKS
              0
            BLOCK
              5
            37
            330
            36
            100
            AcDbEntity
              8
            0
            100
            AcDbBlockBegin
              2
            *Model_Space
             70
                 0
             10
            0.0
             20
            0.0
             30
            0.0
              3
            *Model_Space
              1
            
              0
            ENDBLK
              5
            38
            330
            36
            100
            AcDbEntity
              8
            0
            100
            AcDbBlockEnd
              0
            BLOCK
              5
            33
            330
            32
            100
            AcDbEntity
             67
                 1
              8
            0
            100
            AcDbBlockBegin
              2
            *Paper_Space
             70
                 0
             10
            0.0
             20
            0.0
             30
            0.0
              3
            *Paper_Space
              1
            
              0
            ENDBLK
              5
            34
            330
            32
            100
            AcDbEntity
             67
                 1
              8
            0
            100
            AcDbBlockEnd
              0
            ENDSEC
        """.trimIndent())
        w.newLine()

        return "32"  // Paper_Space handle
    }
} 