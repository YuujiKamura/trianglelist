package com.jpaver.trianglelist.datamanager

import java.io.BufferedOutputStream
import java.nio.charset.Charset

/** SFC 全文を SJIS (CP932) で [outputStream] に書く。close は呼び出し側が行う */
fun SfcWriter.saveTo(outputStream: BufferedOutputStream, charset: Charset = Charset.forName("SJIS")) {
    outputStream.write(buildSfcString().toByteArray(charset))
}
