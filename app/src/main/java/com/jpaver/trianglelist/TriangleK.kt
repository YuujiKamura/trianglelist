package com.jpaver.trianglelist

class TriangleK: EditObject(), Cloneable {
    protected var valid = false
    protected var lengthA = 0f
    protected var lengthB = 0f
    protected var lengthC = 0f
    protected var scale_ = 1f

    protected var pointCA = PointXY(0f, 0f) // base point by calc

    protected var pointAB = PointXY(0f, 0f)
    protected var pointBC = PointXY(0f, 0f)
    protected var pointCenter = PointXY(0f, 0f)
    protected var pointNumber = PointXY(0f, 0f)
    protected var isPointNumberMoved_ = false

    protected var myTheta = 0.0
    protected var myAlpha = 0.0
    protected var myPowA = 0.0
    protected var myPowB = 0.0
    protected var myPowC = 0.0
    protected var myAngle = 180f
    protected var myAngleCA = 0f
    protected var myAngleAB = 0f
    protected var myAngleBC = 0f
    protected var myParentNumber = 0 // 0:root

    protected var myParentBC = 0 // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC

    protected var myNumber = 1
    protected var myDimAlign = 0
    protected var myDimAlignA = 0
    protected var myDimAlignB = 0
    protected var myDimAlignC = 0
    protected var outsideA_ = 0
    protected var outsideB_ = 0
    protected var outsideC_ = 0
    protected var childSide_ = 0
    protected var myName = ""

    protected var myBP =
        Bounds(0f, 0f, 0f, 0f)

    override fun clone(): Triangle {
        var b: Triangle = com.jpaver.trianglelist.Triangle()
        try {
//            b = super.clone()
            b.pointCA_ = pointCA.clone()
            b.pointAB_ = pointAB.clone()
            b.pointBC_ = pointBC.clone()
            b.pointCenter_ = pointCenter.clone()
            b.pointNumber_ = pointNumber.clone()
            b.isPointNumberMoved_ = isPointNumberMoved_
            b.myBP_.left = myBP.left
            b.myBP_.top = myBP.top
            b.myBP_.right = myBP.right
            b.myBP_.bottom = myBP.bottom
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return b
    }

    fun Triangle() {}

    fun Triangle(A: Int, B: Int, C: Int) {
        initBasicArguments(A.toFloat(), B.toFloat(), C.toFloat(), pointCA, myAngle)
        calcPoints(pointCA, myAngle)
    }

    //for first triangle.
    fun Triangle(
        A: Float,
        B: Float,
        C: Float,
        pCA: PointXY,
        angle: Float
    ) {
        initBasicArguments(A, B, C, pCA, angle)
        calcPoints(pCA, angle)
    }

    fun Triangle(dP: Params, angle: Float) {
        myNumber = dP.n
        myName = dP.name
        initBasicArguments(dP.a, dP.b, dP.c, dP.pt, angle)
        calcPoints(dP.pt, angle)
    }

    //it use first Triangle.
    fun Triangle(
        A: Float, B: Float, C: Float, pCA: PointXY, angle: Float,
        myParNum: Int, myParBC: Int, myConne: Int
    ) {
        initBasicArguments(A, B, C, pCA, angle)
        setParentInfo(myParNum, myParBC, myConne)
        myDimAlign = 1
        calcPoints(pCA, angle)
    }

    fun Triangle(
        myParent: Triangle,
        pbc: Int,
        A: Float,
        B: Float,
        C: Float
    ) {
        set(myParent, pbc, A, B, C)
    }

    fun Triangle(myParent: Triangle, A: Int, B: Float, C: Float) {
        set(myParent, A, B, C)
    }

    fun Triangle(ta: Triangle) {
        myNumber = ta.myNumber_
        initBasicArguments(
            ta.getLengthA_(),
            ta.getLengthB_(),
            ta.getLengthC_(),
            ta.getPointCA_(),
            ta.angle
        )
        calcPoints(ta.getPointCA_(), ta.angle)
    }

    fun Triangle(myParent: Triangle, dP: Params) {
        set(myParent, dP.pl, dP.a, dP.b, dP.c)
        myName = dP.name
    }

    operator fun set(myParent: Triangle, dP: Params) {
        set(myParent, dP.pl, dP.a, dP.b, dP.c)
        myName = dP.name
    }

    fun set(
        myParent: Triangle,
        pbc: Int,
        A: Float,
        B: Float,
        C: Float
    ){//: Triangle {

        // if user rewrite A
        var pbc = pbc
        if (A != myParent.getLengthByIndex(pbc)) {
            if (pbc == 1) pbc = 3
            if (pbc == 2) pbc = 5
        }
        if (pbc == 1) { // B
            myParentBC = 1
            lengthA = myParent.getLengthB_()
            pointCA = myParent.getPointBC_()
            myAngle = myParent.angleMpAB
        } else if (pbc == 2) { // C
            myParentBC = 2
            lengthA = myParent.getLengthC_()
            pointCA = myParent.getPointCA_()
            myAngle = myParent.angleMmCA
        } else if (pbc == 3) { // B-R
            myParentBC = 3
            lengthA = A
            pointCA = myParent.getPointBC_()
            myAngle = myParent.angleMpAB
        } else if (pbc == 4) { //B-L
            myParentBC = 4
            lengthA = A
            pointCA = myParent.getPointBC_()
            myAngle = myParent.angleMpAB
        } else if (pbc == 5) { //C-R
            myParentBC = 5
            lengthA = A
            pointCA = myParent.getPointCA_()
            myAngle = myParent.angleMmCA
        } else if (pbc == 6) { //C-L
            myParentBC = 6
            lengthA = A
            pointCA = myParent.getPointCA_()
            myAngle = myParent.angleMmCA
        } else {
            myParentBC = 0
            lengthA = 0f
            pointCA = PointXY(0f, 0f)
            myAngle = 180f
        }
        myParentNumber = myParent.getMyNumber_()
        initBasicArguments(lengthA, B, C, pointCA, myAngle)
        calcPoints(pointCA, myAngle)
        if (myParentBC == 4) {
            val vector = PointXY(
                myParent.getPointAB_().x - pointAB.x,
                myParent.getPointAB_().y - pointAB.y
            )
            move(vector)
        }
        if (myParentBC == 6) {
            val vector = PointXY(
                myParent.getPointBC_().x - pointAB.x,
                myParent.getPointBC_().y - pointAB.y
            )
            move(vector)
        }
        myDimAlign = setDimAlign()
        //return this
    }

    operator fun set(myParent: Triangle, A: Int, B: Float, C: Float){//}: Triangle {
        if (A == 1) {
            myParentBC = 1
            lengthA = myParent.getLengthB_()
            pointCA = myParent.getPointBC_()
            myAngle = myParent.angleMpAB
        } else if (A == 2) {
            myParentBC = 2
            lengthA = myParent.getLengthC_()
            pointCA = myParent.getPointCA_()
            myAngle = myParent.angleMmCA
        } else {
            myParentBC = 0
            lengthA = 0f
            pointCA = PointXY(0f, 0f)
            myAngle = 180f
        }
        myParentNumber = myParent.getMyNumber_()
        initBasicArguments(lengthA, B, C, pointCA, myAngle)
        calcPoints(pointCA, myAngle)
        myDimAlign = setDimAlign()
//        return this
    }

    fun collision(x: Float, y: Float): Boolean {
        return true
    }

    private fun setMyBound() {
        var lb = pointCA.min(pointAB)
        lb = pointAB.min(pointBC)
        myBP.left = lb.x
        myBP.bottom = lb.y
        var rt = pointCA.max(pointAB)
        rt = pointAB.max(pointBC)
        myBP.right = rt.x
        myBP.top = rt.y
    }

    fun expandBoundaries(listBound: Bounds): Bounds? {
        setMyBound()
        val newB =
            Bounds(myBP.left, myBP.top, myBP.right, myBP.bottom)
        // 境界を比較し、広い方に置き換える
        if (myBP.bottom > listBound.bottom) newB.bottom = listBound.bottom
        if (myBP.top < listBound.top) newB.top = listBound.top
        if (myBP.left > listBound.left) newB.left = listBound.left
        if (myBP.right < listBound.right) newB.right = listBound.right
        return newB
    }

    override fun getArea(): Float {
        val sumABC = lengthA + lengthB + lengthC
        val myArea =
            sumABC * 0.5f * (sumABC * 0.5f - lengthA) * (sumABC * 0.5f - lengthB) * (sumABC * 0.5f - lengthC)
        return roundByUnderTwo(Math.pow(myArea.toDouble(), 0.5).toFloat())
    }

    fun roundByUnderTwo(fp: Float): Float {
        val ip = fp * 100f
        return Math.round(ip) / 100f
    }

    operator fun set(myParent: Triangle, pbc: Int) {
        this.set(myParent, pbc, lengthA, lengthB, lengthC)
    }

    // reset my parent.
    fun setWith(tri: Triangle, pbc: Int) {
        this[tri] = pbc
    }

    private fun initBasicArguments(
        A: Float,
        B: Float,
        C: Float,
        pCA: PointXY,
        angle: Float
    ) {
        lengthA = A
        lengthB = B
        lengthC = C
        valid = validTriangle()
        pointCA = PointXY(pCA.x, pCA.y)
        pointAB = PointXY(0.0f, 0.0f)
        pointBC = PointXY(0.0f, 0.0f)
        pointCenter = PointXY(0.0f, 0.0f)
        pointNumber = PointXY(0.0f, 0.0f)
        myAngle = angle
        myAngleCA = 0f
        myAngleAB = 0f
        myAngleBC = 0f
        childSide_ = 0
        myDimAlignA = 0
        myDimAlignB = 0
        myDimAlignC = 0
    }

    //maybe not use.
    private fun setParentInfo(myParNum: Int, myParBC: Int, myConne: Int) {
        myParentNumber = myParNum
        myParentBC = myParBC
    }

    fun validTriangle(): Boolean {
        if (lengthA <= 0.0f || lengthB <= 0.0f || lengthC <= 0.0f) return false
        return !(lengthA + lengthB <= lengthC || lengthB + lengthC <= lengthA || lengthC + lengthA <= lengthB)
    }

    fun calculateInternalAngle(p1: PointXY, p2: PointXY?, p3: PointXY): Double {
        val v1 = p1.subtract(p2)
        val v2 = p3.subtract(p2)
        val angleRadian =
            Math.acos(v1.innerProduct(v2) / (v1.magnitude() * v2.magnitude()))
        return angleRadian * 180 / Math.PI
    }

    private fun calcPoints(pCA: PointXY, angle: Float) {
        pointAB[(pCA.x + lengthA * Math.cos(Math.toRadians(angle.toDouble()))).toFloat()] =
            (pCA.y + lengthA * Math.sin(Math.toRadians(angle.toDouble()))).toFloat()
        myTheta = Math.atan2(
            pCA.y - pointAB.y.toDouble(),
            pCA.x - pointAB.x.toDouble()
        )
        this.myPowA = Math.pow(lengthA.toDouble(), 2.0)
        this.myPowB = Math.pow(lengthB.toDouble(), 2.0)
        this.myPowC = Math.pow(lengthC.toDouble(), 2.0)
        this.myAlpha =
            Math.acos((this.myPowA + this.myPowB - this.myPowC) / (2 * lengthA * lengthB))
        pointBC[(pointAB.x + lengthB * Math.cos(myTheta + this.myAlpha)).toFloat()] =
            (pointAB.y + lengthB * Math.sin(myTheta + this.myAlpha)).toFloat()
        myAngleAB =
            calculateInternalAngle(pointCA, pointAB, pointBC).toFloat()
        myAngleBC =
            calculateInternalAngle(pointAB, pointBC, pointCA).toFloat()
        myAngleCA =
            calculateInternalAngle(pointBC, pointCA, pointAB).toFloat()
        pointCenter[(pointAB.x + pointBC.x + pointCA.x) / 3] =
            (pointAB.y + pointBC.y + pointCA.y) / 3
        setMyBound()
        if (isPointNumberMoved_ == false) autoAlignPointNumber()
    }

    private fun autoAlignPointNumber() {
        pointNumber = pointCenter //とりあえず重心にする
        val offset2 = 2.2f
        if (myAngleBC < 30 && lengthA / scale_ < 2) {
            val midA = pointAB.calcMidPoint(pointCA)
            val lengthCenterA = pointCenter.vectorTo(midA).lengthXY()
            pointNumber = pointCenter.offset(
                pointAB.calcMidPoint(pointCA),
                lengthCenterA * ((90 - myAngleBC) * 0.01f)
            )
            if (lengthB / scale_ < 5 || myAngleBC < 5) pointNumber =
                pointNumber.offset(midA, offset2 * scale_)
            return
        }
        if (myAngleAB < 30 && lengthC / scale_ < 2) {
            val midC = pointBC.calcMidPoint(pointCA)
            val lengthCenterC = pointCenter.vectorTo(midC).lengthXY()
            pointNumber = pointCenter.offset(
                pointBC.calcMidPoint(pointCA),
                lengthCenterC * ((90 - myAngleAB) * 0.01f)
            )
            if (lengthA / scale_ < 5 || myAngleAB < 5) pointNumber =
                pointNumber.offset(midC, offset2 * scale_)
            return
        }
        if (myAngleCA < 30 && lengthB / scale_ < 2) {
            val midB = pointAB.calcMidPoint(pointBC)
            val lengthCenterB = pointCenter.vectorTo(midB).lengthXY()
            pointNumber = pointCenter.offset(
                pointAB.calcMidPoint(pointBC),
                lengthCenterB * ((90 - myAngleCA) * 0.01f)
            )
            if (lengthC / scale_ < 5 || myAngleCA < 5) pointNumber =
                pointNumber.offset(midB, offset2 * scale_)
            return
        }
    }

    fun setDimAlign(): Int {
        return if (getAngle() <= 90 || getAngle() >= 270) 3.also {
            myDimAlign = it
        } else 1.also { myDimAlign = it }
    }

    // 1:下 　3:上
    fun getDimAlignA(): Int {
        return if (myAngle <= 90 || getAngle() >= 270) 3.also {
            myDimAlignA = it
        } else 1.also { myDimAlignA = it }
    }

    fun getDimAlignB(): Int {
        if (getAngleMpAB() <= 450f || getAngleMpAB() >= 270f || getAngleMpAB() <= 90f || getAngleMpAB() >= -90f
        ) {
            return if (childSide_ == 3 || childSide_ == 4) 3.also {
                myDimAlignB = it
            } else 1.also { myDimAlignB = it }
        }
        return if (childSide_ == 3 || childSide_ == 4) 1.also {
            myDimAlignB = it
        } else 3.also { myDimAlignB = it }
    }

    fun getDimAlignC(): Int {
        if (getAngleMmCA() <= 450f || getAngleMmCA() >= 270f || getAngleMmCA() <= 90f || getAngleMmCA() >= -90f
        ) {
            return if (childSide_ == 5 || childSide_ == 6) 3.also {
                myDimAlignB = it
            } else 1.also { myDimAlignB = it }
        }
        return if (childSide_ == 5 || childSide_ == 6) 1.also {
            myDimAlignB = it
        } else 3.also { myDimAlignB = it }
    }

    fun getLengthAS(s: Float): Float {
        return lengthA * s
    }

    fun getLengthBS(s: Float): Float {
        return lengthB * s
    }

    fun getLengthCS(s: Float): Float {
        return lengthC * s
    }

    fun getLengthByIndex(i: Int): Float {
        if (i == 1) return lengthB
        return if (i == 2) lengthC else 0f
    }

    fun getAngle(): Float {
        return myAngle
    }

    fun getAngleAB(): Float {
        return myAngleAB
    }

    fun getAngleBC(): Float {
        return myAngleBC
    }

    fun getAngleCA(): Float {
        return myAngleCA
    }

    fun getAngleMmCA(): Float {
        return myAngle - myAngleCA
    }

    fun getAngleMpAB(): Float {
        return myAngle + myAngleAB
    }

    fun getParentBC(): Int {
        return myParentBC
    }

    fun getParentNumber(): Int {
        return myParentNumber
    }
/*
    fun getMyBP(): Bounds? {
        return myBP
    }

    fun getPointAB(): PointXY? {
        return PointXY(pointAB)
    }

    fun getPointBC(): PointXY? {
        return PointXY(pointBC)
    }

    fun getPointCA(): PointXY? {
        return PointXY(pointCA)
    }

    fun getPointCenter(): PointXY? {
        return PointXY(pointCenter)
    }

    fun getPointNumber(): PointXY? {
        return pointNumber
    }

    fun getLengthA(): Float {
        return lengthA
    }

    fun getLengthB(): Float {
        return lengthB
    }

    fun getLengthC(): Float {
        return lengthC
    }

    fun getMyNumber(): Int {
        return myNumber
    }

    fun setNumber(num: Int) {
        myNumber = num
    }

    fun getChildSide_(): Int {
        return childSide_
    }

    fun setChildSide_(childside: Int) {
        childSide_ = childside
    }

    fun setMyName(name: String) {
        myName = name
    }

    fun getMyName(): String? {
        return myName
    }

    fun setPointNumber(p: PointXY) {
        pointNumber = p
        isPointNumberMoved_ = true
    }
*/
    fun move(to: PointXY) {
        pointAB.add(to)
        pointBC.add(to)
        pointCA.add(to)
        pointCenter.add(to)
        pointNumber.add(to)
        myBP.left = myBP.left + to.x
        myBP.right = myBP.right + to.x
        myBP.top = myBP.top + to.y
        myBP.bottom = myBP.bottom + to.x
    }

    fun scale(basepoint: PointXY?, scale: Float) {
        scale_ = scale
        pointAB.scale(basepoint, scale)
        pointBC.scale(basepoint, scale)
        pointCA.scale(basepoint, scale)
        pointCenter.scale(basepoint, scale)
        pointNumber.scale(basepoint, scale)
        lengthA *= scale
        lengthB *= scale
        lengthC *= scale
        calcPoints(pointCA, myAngle)
    }

    override fun getParams(): Params {
        return Params(
            myName,
            "",
            myNumber,
            lengthA,
            lengthB,
            lengthC,
            myParentNumber,
            myParentBC,
            pointCA,
            pointCenter
        )
    }

    fun rotate(basepoint: PointXY?, degree: Float) {
        pointCA = pointCA.rotate(basepoint, degree)
        myAngle += degree
        //if(isPointNumberMoved_ == true) pointNumber = pointNumber.rotate(basepoint, degree);
        calcPoints(pointCA, myAngle)
    }

}