package com.jpaver.trianglelist.util
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import java.nio.file.Paths

object Utils {
    fun formattedString(value: Float): String{
        return String.format("%.2f", value)
    }
}


object FileUtil {

    fun writeToUserHome(content: String, relativePath: String, charset: Charset = Charsets.UTF_8 ) {
        val filePath = constructFilePathInUserHome(relativePath)
        ensureParentDirectoryExists(filePath)
        writeContentToFile(content, filePath, charset)
    }

    private fun constructFilePathInUserHome(andRelativePath: String): String {
        val userHomeDirectory = System.getProperty("user.home")
        return Paths.get(userHomeDirectory, andRelativePath).toString()
    }

    private fun ensureParentDirectoryExists(filePath: String) {
        val file = File(filePath)
        file.parentFile?.mkdirs()
    }

    //stringの塊をfileに書ける、という、バッファのその都度感をなかったことにする簡潔な関数
    private fun writeContentToFile(content: String, filePath: String, charset: Charset = Charsets.UTF_8) {
        BufferedWriter(OutputStreamWriter(FileOutputStream(filePath), charset)).use { writer ->
            writer.write(content)
        }
    }

}

