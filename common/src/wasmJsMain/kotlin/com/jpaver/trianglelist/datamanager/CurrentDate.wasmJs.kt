package com.jpaver.trianglelist.datamanager

private fun jsFullYear(): Int = js("new Date().getFullYear()")
private fun jsMonth1(): Int = js("new Date().getMonth() + 1")
private fun jsDayOfMonth(): Int = js("new Date().getDate()")

actual fun currentDateStringJp(): String =
    "${jsFullYear()} 年 ${jsMonth1()} 月 ${jsDayOfMonth()} 日"
