package com.jpaver.trianglelist.editmodel

data class ZumenInfo(
    // 文字列リソースをstringに変換して保持する。
    // 改めてみると、どれがどこで使われているのか不明、なんでtitleふたつあんの
    var zumentitle: String = "",
    var rosenname: String = "",
    var koujiname: String = "",
    var tDtype_: String = "",
    var tDname_: String = "",
    var tScale_: String = "",
    var tNum_: String = "",
    var tDateHeader_: String = "",
    var tDate_: String = "",
    var tAname_: String = "",
    var menseki_: String = "",
    var mTitle_: String = "",
    var mCname_: String = "",
    var mSyoukei_: String = "",
    var mGoukei_: String = "",
    var tCredit_: String = ""
)