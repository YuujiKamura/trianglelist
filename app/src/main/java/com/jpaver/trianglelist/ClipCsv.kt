package com.jpaver.trianglelist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import java.io.StringWriter
import java.io.Writer

class ClipCsv(private val context: Context) {
    fun copyCSVToClipboard( writeCsv: (Writer) -> Boolean ) {
        val writer = StringWriter()
        writeCsv( writer )
        val csvData = writer.toString()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("CSV Data", csvData)
        clipboard.setPrimaryClip(clip)
    }
}

