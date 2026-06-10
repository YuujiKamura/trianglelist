package com.jpaver.trianglelist.editmodel

// commonMain には android.util.Log が無いため、差し替え可能な sink を持つ最小 logger。
// app 起動時に sink を android.util.Log に繋げば従来通り出力される (未配線なら無音)。
object TriLog {
    var sink: ((tag: String, msg: String) -> Unit)? = null
    fun d(tag: String, msg: String) { sink?.invoke(tag, msg) }
    fun e(tag: String, msg: String, tr: Throwable? = null) {
        sink?.invoke(tag, if (tr != null) "$msg: $tr" else msg)
    }
}
