package com.jpaver.trianglelist.util
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.nio.file.Paths

object Utils {
    fun formattedString(value: Float): String{
        return String.format("%.2f", value)
    }
}

object FileUtil {

    fun writeToUserHome(content: String, relativePath: String) {
        val filePath = constructFilePathInUserHome(relativePath)
        ensureParentDirectoryExists(filePath)
        writeContentToFile(content, filePath)
    }

    private fun constructFilePathInUserHome(relativePath: String): String {
        val userHome = System.getProperty("user.home")
        return Paths.get(userHome, relativePath).toString()
    }

    private fun ensureParentDirectoryExists(filePath: String) {
        val file = File(filePath)
        file.parentFile?.mkdirs()
    }

    private fun writeContentToFile(content: String, filePath: String) {
        BufferedWriter(FileWriter(filePath)).use { writer ->
            writer.write(content)
        }
    }
}

