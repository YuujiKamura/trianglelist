package com.example.trilib
import kotlin.math.acos
import kotlin.math.atan2

class PointXY :Cloneable<PointXY> {
    private var _x: Float = 0.0f
    private var _y: Float = 0.0f

    var x: Float
        get() = adjustForRoundingError(_x)
        set(value) {
            _x = value
        }

    var y: Float
        get() = adjustForRoundingError(_y)
        set(value) {
            _y = value
        }

    private fun adjustForRoundingError(value: Float, threshold: Float = 1e-5f): Float {
        return if (Math.abs(value) < threshold) 0.0f else value
    }

    // xの値をフォーマットして返すメソッド
    val DECIMAL = 4
    fun getFormattedX(decimalPlaces: Int = DECIMAL): String {
        return String.format("%.${decimalPlaces}f", x)
    }

    // yの値をフォーマットして返すメソッド
    fun getFormattedY(decimalPlaces: Int = DECIMAL): String {
        return String.format("%.${decimalPlaces}f", y)
    }

    fun format(decimalPlaces: Int = DECIMAL): String{
        return "("+getFormattedX(decimalPlaces)+","+getFormattedY(decimalPlaces)+")"
    }

    // 主コンストラクタ
    constructor(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    // スケールを適用する追加のコンストラクタ
    constructor(x: Float, y: Float, s: Float) : this(x * s, y * s)

    // PointXY オブジェクトからのコピーコンストラクタ
    constructor(p: PointXY) : this(p.x, p.y)

    override fun clone(): PointXY {
        val b = PointXY(this.x,this.y)

        try {
        } catch (e: CloneNotSupportedException) {
            e.printStackTrace()
        }
        return b
    }

    private fun validateInputs(lineStart: PointXY, lineEnd: PointXY, scaleX: Float=1f, scaleY: Float=1f ) {
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

    fun mirroredAndScaledPoint(lineStart: PointXY, lineEnd: PointXY, clockwise:Float=1f ): PointXY {

        return mirror(lineStart, lineEnd, clockwise )
    }


/*
    private fun mirroredPoint(lineStart: PointXY, lineEnd: PointXY): PointXY {
        // 直線の傾き（m）とy切片（b）を計算
        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y
        val m = if (dx != 0f) dy / dx else Float.POSITIVE_INFINITY // 垂直な直線の場合、傾きは無限大
        val b = lineStart.y - m * lineStart.x

        val xh: Float
        val yh: Float

        if (m.isInfinite()) {
            // 直線が垂直の場合、垂線の足のx座標は直線のx座標と同じで、y座標は元の点のy座標
            xh = lineStart.x
            yh = y
        } else {
            // 垂直でない直線の場合、垂線の足の座標（xh, yh）を計算
            xh = (m * y + x - m * b) / (m * m + 1)
            yh = m * xh + b
        }

        // 元の点から垂線の足までの距離を2倍にして、ミラーリングされた点を計算
        val xm = 2 * xh - x
        val ym = 2 * yh - y

        // ミラーリングされた点を返す
        return PointXY(xm, ym)
    }
*/

    fun mirror(lineStart: PointXY, lineEnd: PointXY, clockwise: Float = -1f ): PointXY {
        validateInputs( lineStart, lineEnd )
        val angle = this.calcAngle180(lineStart, lineEnd)
        val angleis = angle * 2f * clockwise
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

    fun equals(x: Float, y: Float): Boolean {
        val range = 0.001f
        return this.x < x + range && this.x > x - range && this.y < y + range && this.y > y - range
    }

    operator fun set(x: Float, y: Float): PointXY {
        this.x = x
        this.y = y
        return this
    }

    fun isVectorToRight(p2: PointXY): Boolean {
        return if ( vectorTo(p2).x > 0 ) true else false
    }

    fun calcAngle180(p2: PointXY, p3: PointXY): Float {
        val v1 = p2.subtract(this)
        val v2 = p2.subtract(p3)
        val angleRadian = acos(v1.innerProduct(v2) / (v1.magnitude() * v2.magnitude()))
        val angleDegree = angleRadian * 180 / Math.PI
        return if (v1.outerProduct(v2) > 0) {
            angleDegree.toFloat() - 180
        } else {
            180 - angleDegree.toFloat()
        }
    }

    fun calcAngle360(p2: PointXY, p3: PointXY): Float {
        val v1 = p2.subtract(this) // p2からthisへのベクトル、逆にすると結果が変わる
        val v2 = p2.subtract(p3) // p2からp3へのベクトル
        val angleRadian = acos(v1.innerProduct(v2) / (v1.magnitude() * v2.magnitude()))
        val angleDegree = (angleRadian * 180 / Math.PI).toFloat()
        val outerProduct = v1.outerProduct(v2)

        // 外角の反転
        if ( outerProduct > 0 && angleDegree <= 180f ||
             outerProduct < 0 && angleDegree >  180f ) {
             return  360f - angleDegree
        }
        return angleDegree
    }

    fun calcAngleWithXAxis(p2: PointXY): Float {
        val deltaX = p2.x - x
        val deltaY = p2.y - y
        val angleRadian = atan2(deltaY, deltaX)  // deltaY/deltaX の角度をラジアンで計算
        var angleDegree = (angleRadian * 180 / Math.PI).toFloat()  // ラジアンを度数に変換

        if (angleDegree < 0) {
            angleDegree += 360f  // 負の角度の場合、360度を加えて正の範囲に修正
        }

        return angleDegree
    }

    fun moveX(length:Float,angle:Float): PointXY {
        return this.plus(length,0f).rotate(this, angle )
    }

    fun set(sp: PointXY) {
        x = sp.x
        y = sp.y
    }

    fun translateAndScale(
        baseInView: PointXY,
        centerInModel: PointXY,
        zoom: Float
    ): PointXY {
        val inLocal = clone()
        inLocal.addminus(baseInView).change_scale(PointXY(0f, 0f), 1 / zoom) // // 左上起点座標を自身(pressedInView)から引く
        inLocal.add(centerInModel.scale(1f, -1f))
        return inLocal
    }

    fun add(a: PointXY): PointXY {
        x += a.x
        y += a.y
        return this
    }

    fun add(a: Float, b: Float): PointXY {
        x += a
        y += b
        return this
    }

    fun plus(x: Float, y: Float): PointXY {
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

    fun offset(p2: PointXY, movement: Float): PointXY {
        val offset = vectorTo(p2).normalize().scale(movement)
        return this + offset
    }

    fun offset(distance: Float, angle: Float): PointXY {
        return this + toVector(angle).scale(distance)
    }

    fun toVector(angle: Float): PointXY {
        val angleInRadians = Math.toRadians(angle.toDouble())
        return PointXY(Math.cos(angleInRadians).toFloat(), Math.sin(angleInRadians).toFloat())
    }

    // 直交方向へのオフセット p2 is base line, p3 is cross vector align
    fun crossOffset(p2: PointXY, movement: Float, clockwise:Float=-90f): PointXY {
        //PointXY normalizedVector = vectorTo(p2).normalize();

        // 一行にいろいろ書いてみる
        return PointXY(this.minus(vectorTo(p2.rotate(this, clockwise )).normalize().scale(movement)))
        //return new PointXY(this.minus(normalizedVector.scale(movement)));
        //return new PointXY( X-(normalizedVector.getX()*movement), Y-(normalizedVector.getY()*movement) );
    }

    fun vectorTo(p2: PointXY): PointXY {
        return PointXY(p2.x - x, p2.y - y)
    }

    fun lengthTo(p2: PointXY): Float {
        return PointXY(x, y).vectorTo(p2).lengthXY()
    }

    fun normalize(): PointXY {
        return PointXY(x / lengthXY(), y / lengthXY())
    }

    fun lengthXY(): Float {
        return Math.pow(Math.pow(x.toDouble(), 2.0) + Math.pow(y.toDouble(), 2.0), 0.5)
            .toFloat()
    }

    fun calcDimAngle(a: PointXY): Float {
        var angle =
            (Math.atan2((a.x - x).toDouble(), (a.y - y).toDouble()) * 180 / Math.PI).toFloat()
        //if(0 > angle )
        angle = -angle
        angle += 90f
        if (90 < angle) angle -= 180f
        if (angle < 0) angle += 360f
        return angle
    }

    fun calcSokAngle(a: PointXY, vector: Int): Float {
        var angle =
            (Math.atan2((a.x - x).toDouble(), (a.y - y).toDouble()) * 180 / Math.PI).toFloat()
        //if(0 > angle )
        angle = -angle
        angle += 90f
        if (vector < 0) angle -= 180f
        return angle
    }

    fun change_scale(a: PointXY, scale: Float) {
        x = x * scale + a.x
        y = y * scale + a.y
    }

    fun scale(scale: Float): PointXY {
        return PointXY(x * scale, y * scale)
    }

    fun scale(scaleX: Float, scaleY: Float): PointXY {
        return PointXY(x * scaleX, y * scaleY)
    }

    fun scale(scale: PointXY): PointXY {
        return PointXY(x * scale.x, y * scale.y)
    }

    fun scale(a: PointXY, sx: Float, sy: Float): PointXY {
        // 基準点aからの相対座標を計算
        val relativeX = x - a.x
        val relativeY = y - a.y

        // 相対座標にスケーリング係数を適用
        val scaledX = relativeX * sx
        val scaledY = relativeY * sy

        // スケーリングされた相対座標を基準点aの座標に加えて、新しい座標を計算
        return PointXY(scaledX + a.x, scaledY + a.y)
    }

    fun nearBy(target: PointXY, range: Float): Boolean {
        return x > target.x - range && x < target.x + range &&
                y > target.y - range && y < target.y + range
    }

    fun sign(p1: PointXY, p2: PointXY, p3: PointXY): Float {
        return (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y)
    }

    fun isCollide(ab: PointXY, bc: PointXY, ca: PointXY): Boolean {
        val b1: Boolean
        val b2: Boolean
        val b3: Boolean
        b1 = sign(this, ab, bc) < 0.0f
        b2 = sign(this, bc, ca) < 0.0f
        b3 = sign(this, ca, ab) < 0.0f
        return b1 == b2 && b2 == b3
    }

    // isCollideメソッドはlengthToを使用して距離を計算
    fun isCollide(target: PointXY, nearby: Float): Boolean {
        return this.lengthTo(target) <= nearby
    }

    fun isCollide(targets: List<PointXY>, nearby: Float): Boolean {
        return targets.any{ it.lengthTo(this) <= nearby }
    }

    fun distancesTo(targets: List<PointXY>): List<Float> {
        return targets.map { this.lengthTo(it) }
    }

    fun distancesTo(targets: Array<PointXY>): List<Float> {
        return targets.map { this.lengthTo(it) }
    }

    fun subtract(point: PointXY): PointXY {
        return PointXY(x - point.x, y - point.y)
    }

    fun magnitude(): Double {
        return Math.sqrt((x * x + y * y).toDouble())
    }

    fun innerProduct(point: PointXY): Double {
        return (x * point.x + y * point.y).toDouble()
    }

    fun outerProduct(point: PointXY): Double {
        return (x * point.y - y * point.x).toDouble()
    }

    // 座標回転メソッド
    fun rotate(center: PointXY, degrees: Float): PointXY {
        // 回転後の座標
        val rotatedX: Float
        val rotatedY: Float
        // 中心点からの相対座標
        val offsetX: Double = (this.x - center.x).toDouble()
        val offsetY: Double = (this.y - center.y).toDouble()
        // 角度をラジアンに変換
        val radians = degrees / 180 * Math.PI
        // 回転後のX座標を計算
        rotatedX = (offsetX * Math.cos(radians) - offsetY * Math.sin(radians)).toFloat() + center.x
        // 回転後のY座標を計算
        rotatedY = (offsetX * Math.sin(radians) + offsetY * Math.cos(radians)).toFloat() + center.y

        return PointXY(rotatedX, rotatedY)
    }

}