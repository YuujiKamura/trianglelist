package com.jpaver.trianglelist

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
        private val map = values().associateBy(ConnectionSide::code)
        fun fromCode(code: Int): ConnectionSide = map[code] ?: NONE
    }
}

enum class ConnectionType(val code: Int) {
    SAME_BY_PARENT(0), DIFFERENT_LENGTH(1), FLOAT_AND_DIFF(2);

    companion object {
        private val map = values().associateBy(ConnectionType::code)
        fun fromCode(code: Int): ConnectionType = map[code] ?: SAME_BY_PARENT
    }
}

enum class LCR(val code: Int) { L(0), C(1), R(2);
    companion object {
        private val map = values().associateBy(LCR::code)
        fun fromCode(code: Int): LCR = map[code] ?: R
    }
}

// 拡張プロパティ ------------------------------------------------
val Triangle.sideEnum: ConnectionSide get() = ConnectionSide.fromCode(connectionSide)
var Triangle.sideEnumMutable: ConnectionSide
    get() = ConnectionSide.fromCode(connectionSide)
    set(value) { connectionSide = value.code }

val Triangle.typeEnum: ConnectionType get() = ConnectionType.fromCode(connectionType_)
var Triangle.typeEnumMutable: ConnectionType
    get() = ConnectionType.fromCode(connectionType_)
    set(value) { connectionType_ = value.code }

val Triangle.lcrEnum: LCR get() = LCR.fromCode(connectionLCR_)
var Triangle.lcrEnumMutable: LCR
    get() = LCR.fromCode(connectionLCR_)
    set(value) { connectionLCR_ = value.code }

// Convenience conversion: index (pbc) -> ConnectionSide
fun connectionSideByIndex(index: Int): ConnectionSide = when(index){
    1,3,4,7,9  -> ConnectionSide.B
    2,5,6,8,10 -> ConnectionSide.C
    else       -> ConnectionSide.NONE
}

// --- Triangle helper extensions ---
fun Triangle.sideByIndex(i: Int): ConnectionSide = connectionSideByIndex(i)

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