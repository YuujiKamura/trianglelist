package com.jpaver.trianglelist.editmodel

/**
 * 列挙型で接続種別などを型安全に扱うための共通定義。
 * 既存コードは Int を使っているため、段階的移行を行う。
 */

enum class ConnectionSide(val code: Int) {
    NONE(0),
    B(1), C(2),
    BR(3), BL(4),
    CR(5), CL(6),
    BC(7), CC(8),
    FB(9), FC(10);

    companion object {
        private val map = entries.associateBy(ConnectionSide::code)
        fun fromCode(code: Int): ConnectionSide = map[code] ?: NONE
    }
}

// 拡張プロパティ ------------------------------------------------
val Triangle.sideEnum: ConnectionSide get() = ConnectionSide.fromCode(connectionSide)

// Convenience conversion: index (pbc) -> ConnectionSide
fun connectionSideByIndex(index: Int): ConnectionSide = when(index){
    1,3,4,7,9  -> ConnectionSide.B
    2,5,6,8,10 -> ConnectionSide.C
    else       -> ConnectionSide.NONE
}

// --- Triangle helper extensions ---
fun sideByIndex(i: Int): ConnectionSide = connectionSideByIndex(i)

fun Triangle.pointBySide(i: Int): com.example.trilib.PointXY? = when(sideByIndex(i)){
    ConnectionSide.B -> pointBC_()
    ConnectionSide.C -> point[0]
    else -> null
}

fun Triangle.angleBySide(i: Int): Float = when(sideByIndex(i)){
    ConnectionSide.B -> angleMpAB
    ConnectionSide.C -> angleMmCA
    else -> 0f
} 