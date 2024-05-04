package com.jpaver.trianglelist.util
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import java.nio.file.Paths

object Utils {
    fun formattedString(value: Float): String{
        return String.format("%.2f", value)
    }
}


object FileUtil {

    fun initBufferedWriter(path:String, filename:String ): BufferedWriter?{
        if (System.getenv("CI") != null) return null
        // CI環境ではこのテストをスキップ

        val fullPath = "$path${File.separator}$filename"

        // ディレクトリが存在するか確認し、存在しない場合は作成する
        val directory = File(path)
        if (!directory.exists()) {
            directory.mkdirs()  // ディレクトリが存在しない場合は、ディレクトリを作成する
        }

        // ファイル出力ストリームを安全に作成
        val fileWriter = FileWriter(fullPath)
        return BufferedWriter(fileWriter)
    }

    fun initBufferedOutputStream(path:String, filename:String ): BufferedOutputStream?{
        if (System.getenv("CI") != null) return null
        // CI環境ではこのテストをスキップ

        val fullPath = "$path${File.separator}$filename"

        // ディレクトリが存在するか確認し、存在しない場合は作成する
        val directory = File(path)
        if (!directory.exists()) {
            directory.mkdirs()  // ディレクトリが存在しない場合は、ディレクトリを作成する
        }

        // ファイル出力ストリームを安全に作成
        val fileOutputStream = FileOutputStream(fullPath)
        return BufferedOutputStream(fileOutputStream)
    }

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

