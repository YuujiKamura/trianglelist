package com.jpaver.trianglelist.datamanager

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset

/** Helper: open [file] with Shift_JIS (CP932) encoding, write DXF, close writer automatically */
fun DxfFileWriter.saveTo(file: File, charset: Charset = Charset.forName("Shift_JIS")) {
    BufferedWriter(OutputStreamWriter(FileOutputStream(file), charset)).use { bw ->
        this.writer = bw
        save()
    }
}
