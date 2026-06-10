package com.jpaver.trianglelist.dxf

internal actual fun readDxfTextOrNull(path: String): String? {
    return try {
        java.io.File(path).readText()
    } catch (e: Exception) {
        println("Error analyzing DXF: ${e.message}")
        null
    }
}
