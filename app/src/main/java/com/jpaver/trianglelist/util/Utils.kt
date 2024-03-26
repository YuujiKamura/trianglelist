package com.jpaver.trianglelist.util
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

object Utils {
    fun formattedString(value: Float): String{
        return String.format("%.2f", value)
    }
}

object FileUtil {

    fun writeToUserHome(content: String, relativePath: String) {
        val userHome = System.getProperty("user.home")
        val file = File(userHome, relativePath)
        file.parentFile?.mkdirs() // 必要に応じて親ディレクトリを作成
        BufferedWriter(FileWriter(file)).use { writer ->
            writer.write(content)
        }
    }
}

