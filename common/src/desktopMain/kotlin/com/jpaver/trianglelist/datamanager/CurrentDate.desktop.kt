package com.jpaver.trianglelist.datamanager

import java.time.LocalDate

actual fun currentDateStringJp(): String {
    val d = LocalDate.now()
    return "${d.year} 年 ${d.monthValue} 月 ${d.dayOfMonth} 日"
}
