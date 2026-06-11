package com.jpaver.trianglelist.editmodel

/**
 * 接続コード (CSV 列5 = アプリの ParentList spinner 位置) と ConnParam の写像。
 *
 * 写像表の SoT は TriangleSetters.setCParamFromParentBC:246-258 — 同じ表をここに
 * 一本化して、app の CsvLoader と web の WebCsvReader の双方がこれを使う。
 * これにより web が書く 6 列形式 CSV (コード 3-10 = 二重断面/フロート) を app も読める。
 *
 * コード: 1=B, 2=C (辺共有)、3=BR, 4=BL, 7=BC (B辺二重断面 右/左/中央)、
 *         5=CR, 6=CL, 8=CC (C辺二重断面)、9=BF, 10=CF (フロート)
 * lcr の幾何 (getParentPointByLCR, TriangleUtilitiesExtensions.kt:67):
 *   0=左起点 (親辺始点側), 1=中央揃え, 2=右起点 (親辺終点側)
 */
object ConnCode {

    /** コード → ConnParam。未知コードは null (呼び出し側で行 skip 等を判断) */
    fun toConnParam(code: Int, lenA: Float, lcrForFloat: Int = 2): ConnParam? = when (code) {
        1 -> ConnParam(1, 0, 2, lenA)
        2 -> ConnParam(2, 0, 2, lenA)
        3 -> ConnParam(1, 1, 2, lenA)
        4 -> ConnParam(1, 1, 0, lenA)
        5 -> ConnParam(2, 1, 2, lenA)
        6 -> ConnParam(2, 1, 0, lenA)
        7 -> ConnParam(1, 1, 1, lenA)
        8 -> ConnParam(2, 1, 1, lenA)
        9 -> ConnParam(1, 2, lcrForFloat, lenA)
        10 -> ConnParam(2, 2, lcrForFloat, lenA)
        else -> null
    }

    /** side(1/2) × type(0/1/2) × lcr(0/1/2) → コード。toConnParam の逆写像 */
    fun fromParts(side: Int, type: Int, lcr: Int): Int = when (type) {
        0 -> side
        1 -> when (lcr) {
            2 -> if (side == 1) 3 else 5
            0 -> if (side == 1) 4 else 6
            else -> if (side == 1) 7 else 8
        }
        else -> if (side == 1) 9 else 10
    }
}
