package com.jpaver.trianglelist.datamanager

/**
 * 工事名・路線名などのヘッダー情報。
 * アプリの CsvLoader.HeaderValues と互換。
 */
data class HeaderValues(
    var koujiname: String = "",
    var rosenname: String = "",
    var gyousyaname: String = "",
    var zumennum: String = ""
)
