package com.jpaver.trianglelist.dxf

import kotlin.test.Test

class DxfAnalyzerTest {

    @Test
    fun analyzeRealDxfFile() {
        val path = "H:/マイドライブ/〇市道 南千反畑町第１号線舗装補修工事/１０測量と設計照査/面積展開図_南千反畑町第１号線.dxf"
        val file = java.io.File(path)

        if (!file.exists()) {
            println("File not found: $path")
            return
        }

        println("File size: ${file.length() / 1024} KB")

        val result = DxfAnalyzer.analyzeFile(path)
        result?.print()
    }
}
