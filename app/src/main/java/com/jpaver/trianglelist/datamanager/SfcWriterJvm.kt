package com.jpaver.trianglelist.datamanager

import java.io.BufferedOutputStream
import java.nio.charset.Charset

/**
 * SFC 全文を CP932 で [outputStream] に書く。close は呼び出し側が行う。
 *
 * "SJIS" は Java では JIS X 0208 のみの別名で ㎡ を encode できない ── DxfFileWriterJvm と
 * 同じ理由で "windows-31j" (CP932) を使う (2026-08-25)。
 */
fun SfcWriter.saveTo(outputStream: BufferedOutputStream, charset: Charset = Charset.forName("windows-31j")) {
    outputStream.write(buildSfcString().toByteArray(charset))
}
