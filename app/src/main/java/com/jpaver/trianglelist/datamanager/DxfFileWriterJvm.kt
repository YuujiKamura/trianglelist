package com.jpaver.trianglelist.datamanager

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset

/**
 * Helper: open [file] with CP932 encoding, write DXF, close writer automatically.
 *
 * charset は "Shift_JIS" ではなく "windows-31j" (= CP932)。Java ではこの 2 つは別物で、
 * "Shift_JIS" は JIS X 0208 のみのため ㎡ (U+33A1) / ① / ㈱ を encode できず `?` に化ける
 * (2026-08-25 発見、面積行の「A=14.94㎡」が `?` で保存されていた)。日本の CAD が読む SJIS は
 * 実質 CP932 で、web 側 (JS の encoding-japanese) も CP932 なので、こちらが正。
 */
fun DxfFileWriter.saveTo(file: File, charset: Charset = Charset.forName("windows-31j")) {
    BufferedWriter(OutputStreamWriter(FileOutputStream(file), charset)).use { bw ->
        this.writer = bw
        save()
    }
}
