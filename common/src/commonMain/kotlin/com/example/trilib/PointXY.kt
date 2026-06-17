package com.example.trilib
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// commonMain では java.lang.String.format が使えないため、"%.Nf" (HALF_UP) と同値の
// 固定小数表記を自前で組む (viewmodel/FormattedString.kt と同じ流儀の Double 版)
private fun formatFixed(value: Double, digits: Int): String {
    val negative = value < 0.0
    var factor = 1.0
    repeat(digits) { factor *= 10.0 }
    val scaled = (if (negative) -value else value) * factor
    var units = floor(scaled).toLong()
    if (scaled - units >= 0.5) units += 1
    val s = units.toString().padStart(digits + 1, '0')
    val intPart = s.dropLast(digits)
    val body = if (digits == 0) intPart else intPart + "." + s.takeLast(digits)
    return if (negative) "-$body" else body
}

class PointXY :Cloneable<PointXY> {
    private var _x: Double = 0.0
    private var _y: Double = 0.0

    var x: Double
        get() = _x
        set(value) {
            _x = value
        }

    var y: Double
        get() = _y
        set(value) {
            _y = value
        }

    // xの値をフォーマットして返すメソッド
    val DECIMAL = 4
    fun getFormattedX(decimalPlaces: Int = DECIMAL): String {
        return formatFixed(x, decimalPlaces)
    }

    // yの値をフォーマットして返すメソッド
    fun getFormattedY(decimalPlaces: Int = DECIMAL): String {
        return formatFixed(y, decimalPlaces)
    }

    fun format(decimalPlaces: Int = DECIMAL): String{
        return "("+getFormattedX(decimalPlaces)+","+getFormattedY(decimalPlaces)+")"
    }

    // 主コンストラクタ
    constructor(x: Double, y: Double) {
        this.x = x
        this.y = y
    }

    // Float 入力の縁 (CSV/UI) 用の拡幅コンストラクタ。Float→Double は精度劣化なし
    constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())

    // スケールを適用する追加のコンストラクタ
    constructor(x: Double, y: Double, s: Double) : this(x * s, y * s)

    // Float 入力の縁 (紙座標リテラル) 用の拡幅コンストラクタ。Float→Double は精度劣化なし
    constructor(x: Float, y: Float, s: Float) : this(x.toDouble(), y.toDouble(), s.toDouble())

    // PointXY オブジェクトからのコピーコンストラクタ
    constructor(p: PointXY) : this(p.x, p.y)

    override fun clone(): PointXY {
        return PointXY(this.x, this.y)
    }

    private fun validateInputs(lineStart: PointXY, lineEnd: PointXY, scaleX: Double=1.0, scaleY: Double=1.0 ) {
        if (scaleX.isNaN() || scaleX.isInfinite() || scaleY.isNaN() || scaleY.isInfinite()) {
            throw IllegalArgumentException("Scaling factors must be valid numbers.")
        }
        if (lineStart.x == lineEnd.x && lineStart.y == lineEnd.y) {
            throw IllegalArgumentException("Line start and end points cannot be the same.")
        }
    }
    override fun toString(): String {
        return "(x=$x, y=$y)"
    }

    fun mirroredAndScaledPoint(lineStart: PointXY, lineEnd: PointXY, clockwise:Double=1.0 ): PointXY {

        return mirror(lineStart, lineEnd, clockwise )
    }

    fun mirror(lineStart: PointXY, lineEnd: PointXY, clockwise: Double = -1.0 ): PointXY {
        validateInputs( lineStart, lineEnd )
        val angle = this.calcAngle180(lineStart, lineEnd)
        val angleis = angle * 2.0 * clockwise
        val result = this.rotate(lineStart, angleis )
        return result
    }

    fun flip(p2: PointXY): PointXY {
        val p3 = PointXY(p2.x, p2.y)
        p2[x] = y
        this[p3.x] = p3.y
        return p3
    }

    fun min(p: PointXY): PointXY {
        val sp = PointXY(x, y)
        if (x > p.x) sp.x = p.x
        if (y > p.y) sp.y = p.y
        return sp
    }

    fun max(p: PointXY): PointXY {
        val sp = PointXY(x, y)
        if (x < p.x) sp.x = p.x
        if (y < p.y) sp.y = p.y
        return sp
    }

    fun equals(x: Double, y: Double): Boolean {
        val range = 0.001
        return this.x < x + range && this.x > x - range && this.y < y + range && this.y > y - range
    }

    operator fun set(x: Double, y: Double): PointXY {
        this.x = x
        this.y = y
        return this
    }

    fun isVectorToRight(p2: PointXY): Boolean {
        return if ( vectorTo(p2).x > 0 ) true else false
    }

    fun calcAngle180(p2: PointXY, p3: PointXY): Double {
        val v1 = p2.subtract(this)
        val v2 = p2.subtract(p3)
        val angleRadian = acos(v1.innerProduct(v2) / (v1.magnitude() * v2.magnitude()))
        val angleDegree = angleRadian * 180 / PI
        return if (v1.outerProduct(v2) > 0) {
            angleDegree - 180
        } else {
            180 - angleDegree
        }
    }

    fun calcAngle360(p2: PointXY, p3: PointXY): Double {
        val v1 = p2.subtract(this) // p2からthisへのベクトル、逆にすると結果が変わる
        val v2 = p2.subtract(p3) // p2からp3へのベクトル
        val angleRadian = acos(v1.innerProduct(v2) / (v1.magnitude() * v2.magnitude()))
        val angleDegree = angleRadian * 180 / PI
        val outerProduct = v1.outerProduct(v2)

        // 外角の反転
        if ( outerProduct > 0 && angleDegree <= 180.0 ||
             outerProduct < 0 && angleDegree >  180.0 ) {
             return  360.0 - angleDegree
        }
        return angleDegree
    }

    fun calcAngleWithXAxis(p2: PointXY): Double {
        val deltaX = p2.x - x
        val deltaY = p2.y - y
        val angleRadian = atan2(deltaY, deltaX)  // deltaY/deltaX の角度をラジアンで計算
        var angleDegree = angleRadian * 180 / PI  // ラジアンを度数に変換

        if (angleDegree < 0) {
            angleDegree += 360.0  // 負の角度の場合、360度を加えて正の範囲に修正
        }

        return angleDegree
    }

    fun moveX(length:Double,angle:Double): PointXY {
        return this.plus(length,0.0).rotate(this, angle )
    }

    fun set(sp: PointXY) {
        x = sp.x
        y = sp.y
    }

    fun translateAndScale(
        baseInView: PointXY,
        centerInModel: PointXY,
        zoom: Double
    ): PointXY {
        val inLocal = clone()
        inLocal.addminus(baseInView).change_scale(PointXY(0.0, 0.0), 1 / zoom) // // 左上起点座標を自身(pressedInView)から引く
        inLocal.add(centerInModel.scale(1.0, -1.0))
        return inLocal
    }

    fun add(a: PointXY): PointXY {
        x += a.x
        y += a.y
        return this
    }

    fun add(a: Double, b: Double): PointXY {
        x += a
        y += b
        return this
    }

    fun plus(x: Double, y: Double): PointXY {
        return PointXY(this.x + x, this.y + y)
    }

    operator fun plus(a: PointXY): PointXY {
        return PointXY(x + a.x, y + a.y)
    }

    operator fun minus(a: PointXY): PointXY {
        return PointXY(x - a.x, y - a.y)
    }

    fun addminus(a: PointXY): PointXY {
        x = x - a.x
        y = y - a.y
        return this
    }

    fun calcMidPoint(a: PointXY): PointXY {
        return PointXY((x + a.x) / 2, (y + a.y) / 2)
    }

    fun offset(p2: PointXY, movement: Double): PointXY {
        val offset = vectorTo(p2).normalize().scale(movement)
        return this + offset
    }

    fun offset(distance: Double, angle: Double): PointXY {
        return this + toVector(angle).scale(distance)
    }

    fun toVector(angle: Double): PointXY {
        val angleInRadians = angle * PI / 180.0
        return PointXY(cos(angleInRadians), sin(angleInRadians))
    }

    // 直交方向へのオフセット p2 is base line, p3 is cross vector align
    fun crossOffset(p2: PointXY, movement: Double, clockwise:Double=-90.0): PointXY {
        //PointXY normalizedVector = vectorTo(p2).normalize();

        // 一行にいろいろ書いてみる
        return PointXY(this.minus(vectorTo(p2.rotate(this, clockwise )).normalize().scale(movement)))
        //return new PointXY(this.minus(normalizedVector.scale(movement)));
        //return new PointXY( X-(normalizedVector.getX()*movement), Y-(normalizedVector.getY()*movement) );
    }

    fun vectorTo(p2: PointXY): PointXY {
        return PointXY(p2.x - x, p2.y - y)
    }

    fun lengthTo(p2: PointXY): Double {
        return PointXY(x, y).vectorTo(p2).lengthXY()
    }

    fun normalize(): PointXY {
        return PointXY(x / lengthXY(), y / lengthXY())
    }

    fun lengthXY(): Double {
        return (x.pow(2.0) + y.pow(2.0)).pow(0.5)
    }

    fun calcDimAngle(a: PointXY): Double {
        var angle =
            atan2(a.x - x, a.y - y) * 180 / PI
        //if(0 > angle )
        angle = -angle
        angle += 90.0
        if (90 < angle) angle -= 180.0
        if (angle < 0) angle += 360.0
        return angle
    }

    fun calcSokAngle(a: PointXY, vector: Int): Double {
        var angle =
            atan2(a.x - x, a.y - y) * 180 / PI
        //if(0 > angle )
        angle = -angle
        angle += 90.0
        if (vector < 0) angle -= 180.0
        return angle
    }

    fun change_scale(a: PointXY, scale: Double) {
        x = x * scale + a.x
        y = y * scale + a.y
    }

    fun scale(scale: Double): PointXY {
        return PointXY(x * scale, y * scale)
    }

    fun scale(scaleX: Double, scaleY: Double): PointXY {
        return PointXY(x * scaleX, y * scaleY)
    }

    fun scale(scale: PointXY): PointXY {
        return PointXY(x * scale.x, y * scale.y)
    }

    fun scale(a: PointXY, sx: Double, sy: Double): PointXY {
        // 基準点aからの相対座標を計算
        val relativeX = x - a.x
        val relativeY = y - a.y

        // 相対座標にスケーリング係数を適用
        val scaledX = relativeX * sx
        val scaledY = relativeY * sy

        // スケーリングされた相対座標を基準点aの座標に加えて、新しい座標を計算
        return PointXY(scaledX + a.x, scaledY + a.y)
    }

    fun nearBy(target: PointXY, range: Double): Boolean {
        return x > target.x - range && x < target.x + range &&
                y > target.y - range && y < target.y + range
    }

    fun sign(p1: PointXY, p2: PointXY, p3: PointXY): Double {
        return (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y)
    }

    fun isCollide(ab: PointXY, bc: PointXY, ca: PointXY): Boolean {
        val b1: Boolean
        val b2: Boolean
        val b3: Boolean
        b1 = sign(this, ab, bc) < 0.0
        b2 = sign(this, bc, ca) < 0.0
        b3 = sign(this, ca, ab) < 0.0
        return b1 == b2 && b2 == b3
    }

    // isCollideメソッドはlengthToを使用して距離を計算
    fun isCollide(target: PointXY, nearby: Double): Boolean {
        return this.lengthTo(target) <= nearby
    }

    fun isCollide(targets: List<PointXY>, nearby: Double): Boolean {
        return targets.any{ it.lengthTo(this) <= nearby }
    }

    fun distancesTo(targets: List<PointXY>): List<Double> {
        return targets.map { this.lengthTo(it) }
    }

    fun distancesTo(targets: Array<PointXY>): List<Double> {
        return targets.map { this.lengthTo(it) }
    }

    fun subtract(point: PointXY): PointXY {
        return PointXY(x - point.x, y - point.y)
    }

    fun magnitude(): Double {
        return sqrt(x * x + y * y)
    }

    fun innerProduct(point: PointXY): Double {
        return x * point.x + y * point.y
    }

    fun outerProduct(point: PointXY): Double {
        return x * point.y - y * point.x
    }

    // 座標回転メソッド
    fun rotate(center: PointXY, degrees: Double): PointXY {
        // 回転後の座標
        val rotatedX: Double
        val rotatedY: Double
        // 中心点からの相対座標
        val offsetX: Double = this.x - center.x
        val offsetY: Double = this.y - center.y
        // 角度をラジアンに変換
        val radians = degrees / 180 * PI
        // 回転後のX座標を計算
        rotatedX = offsetX * cos(radians) - offsetY * sin(radians) + center.x
        // 回転後のY座標を計算
        rotatedY = offsetX * sin(radians) + offsetY * cos(radians) + center.y

        return PointXY(rotatedX, rotatedY)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PointXY) return false
        val range = 0.0001
        return kotlin.math.abs(x - other.x) < range && kotlin.math.abs(y - other.y) < range
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        return result
    }

}
