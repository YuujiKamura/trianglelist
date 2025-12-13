package com.jpaver.trianglelist.cadview

import androidx.compose.ui.geometry.Offset
import java.io.File
import java.util.Properties

data class ViewState(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float
)

class ViewStateManager {
    private val storeFile: File
    private val properties = Properties()

    init {
        // ユーザーホームの .cadviewer ディレクトリに保存
        val homeDir = System.getProperty("user.home")
        val configDir = File(homeDir, ".cadviewer")
        if (!configDir.exists()) {
            configDir.mkdirs()
        }
        storeFile = File(configDir, "view_states.properties")
        loadProperties()
    }

    private fun loadProperties() {
        try {
            if (storeFile.exists()) {
                storeFile.inputStream().use { properties.load(it) }
            }
        } catch (e: Exception) {
            println("Failed to load view state store: ${e.message}")
        }
    }

    private fun saveProperties() {
        try {
            storeFile.outputStream().use {
                properties.store(it, "CAD Viewer State")
            }
        } catch (e: Exception) {
            println("Failed to save view state store: ${e.message}")
        }
    }

    fun saveViewState(filePath: String, scale: Float, offset: Offset) {
        val normalizedPath = File(filePath).absolutePath
        // Base64エンコードでパスをキーに（特殊文字対策）
        val key = java.util.Base64.getEncoder().encodeToString(normalizedPath.toByteArray())
        val value = "${scale},${offset.x},${offset.y}"
        properties.setProperty(key, value)
        saveProperties()
        println("View state saved for: $normalizedPath (scale=$scale, offset=$offset)")
    }

    fun loadViewState(filePath: String): Pair<Float, Offset>? {
        val normalizedPath = File(filePath).absolutePath
        val key = java.util.Base64.getEncoder().encodeToString(normalizedPath.toByteArray())
        val value = properties.getProperty(key) ?: return null.also {
            println("No saved view state for: $normalizedPath")
        }

        return try {
            val parts = value.split(",")
            if (parts.size == 3) {
                val scale = parts[0].toFloat()
                val offsetX = parts[1].toFloat()
                val offsetY = parts[2].toFloat()
                println("View state loaded for: $normalizedPath (scale=$scale, offset=($offsetX, $offsetY))")
                Pair(scale, Offset(offsetX, offsetY))
            } else {
                null
            }
        } catch (e: Exception) {
            println("Failed to parse view state: ${e.message}")
            null
        }
    }

    fun hasViewState(filePath: String): Boolean {
        val normalizedPath = File(filePath).absolutePath
        val key = java.util.Base64.getEncoder().encodeToString(normalizedPath.toByteArray())
        return properties.containsKey(key)
    }
}
