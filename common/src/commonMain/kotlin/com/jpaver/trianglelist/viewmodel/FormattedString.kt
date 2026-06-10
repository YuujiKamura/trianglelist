package com.jpaver.trianglelist.viewmodel

import kotlin.math.floor

fun Float?.formattedString(fractionDigits: Int): String {
    if (this == null) return ""
    val smartFractionDigits = if ((this * 100).toInt() % 10 == 0) 1 else fractionDigits
    return spaced_by(smartFractionDigits) + formatFixed(this, smartFractionDigits)
}

fun spaced_by(number: Int): String = " ".repeat(2 - number)

// commonMain では java.lang.String.format が使えないため、"%.Nf" (HALF_UP) と同値の固定小数表記を自前で組む
private fun formatFixed(value: Float, digits: Int): String {
    val d = value.toDouble()
    val negative = d < 0.0
    var factor = 1.0
    repeat(digits) { factor *= 10.0 }
    val scaled = (if (negative) -d else d) * factor
    var units = floor(scaled).toLong()
    if (scaled - units >= 0.5) units += 1
    val s = units.toString().padStart(digits + 1, '0')
    val intPart = s.dropLast(digits)
    val body = if (digits == 0) intPart else intPart + "." + s.takeLast(digits)
    return if (negative) "-$body" else body
}
