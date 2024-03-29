package com.jpaver.trianglelist

import com.jpaver.trianglelist.util.Params
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

@Suppress("NAME_SHADOWING")
class Triangle : EditObject, Cloneable {
    // 各辺に接続されている Triangle オブジェクトの識別子を返す
    override fun toString(): String {
        val connectedTriangles = arrayOfNulls<Triangle>(3) // 各辺に接続された Triangle オブジェクトを保持する配列
        connectedTriangles[0] = nodeTriangleA_
        connectedTriangles[1] = nodeTriangleB_
        connectedTriangles[2] = nodeTriangleC_
        val sb = StringBuilder()
        sb.append("Triangle:").append(myNumber_).append(", ").append(System.identityHashCode(this))
            .append(", connected to: ")
        for (i in connectedTriangles.indices) {
            if (connectedTriangles[i] != null) {
                sb.append(System.identityHashCode(connectedTriangles[i]))
                if (i < connectedTriangles.size - 1) {
                    sb.append(" and ")
                }
            }
        }
        sb.append("%n CA:").append(point[0]).append(" AB:").append(pointAB_).append(" BC:")
            .append(pointBC_)
        return sb.toString()
    }

    //region Parameters
    var valid_ = false
    var length = FloatArray(3)
    var lengthNotSized = FloatArray(3)
    var point = arrayOf(PointXY(0f,0f),PointXY(0f,0f),PointXY(0f,0f))
    var scale_ = 1f
    var angle = 180f
    var angleInLocal_ = 0f
    var dedcount = 0f
    var sla_ = ""
    var slb_ = ""
    var slc_ = ""

    // PointXY point[0] = new PointXY(0f, 0f); // base point by calc
    var pointAB_: PointXY = PointXY(0f, 0f)
    var pointBC_: PointXY = PointXY(0f, 0f)
    var pointCenter_ = PointXY(0f, 0f)//autoAlignPointNumber();

    //endregion
    //region getter
    var pointNumberAutoAligned_ = PointXY(0f, 0f)
    var isPointNumberMovedByUser_ = false
    var dimPointA_ = PointXY(0f, 0f)
    var dimPointB_ = PointXY(0f, 0f)
    var dimPointC_ = PointXY(0f, 0f)
    var pointName_ = PointXY(0f, 0f)
    var nameAlign_ = 0
    var nameSideAlign_ = 0
    protected var myTheta_ = 0.0
    protected var myAlpha_ = 0.0
    protected var myPowA_ = 0.0
    protected var myPowB_ = 0.0
    protected var myPowC_ = 0.0
    var angleCA = 0f
    var angleAB = 0f
    var angleBC = 0f
    var dimAngleB_ = 0f
    var dimAngleC_ = 0f
    var parentNumber = -1 // 0:root
    var parentBC = -1 // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
    var connectionType_ = 0 // 0:sameByParent, 1:differentLength, 2:floatAndDifferent
    var connectionLCR_ = 2 // 0:L 1:C 2:R
    var cParam_ = ConnParam(0, 0, 2, 0f)
    var myNumber_ = 1
    var myDimAlign_ = 0
    var myDimAlignA_ = 3
    var myDimAlignB_ = 3
    var myDimAlignC_ = 3
    var isChangeDimAlignB_ = false
    var isChangeDimAlignC_ = false
    var dimSideAlignA_ = 0
    var dimSideAlignB_ = 0
    var dimSideAlignC_ = 0
    var lastTapSide_ = -1
    var color_ = 4
    var childSide_ = 0
    var myName_ = ""
    var myBP_ = Bounds(0f, 0f, 0f, 0f)
    var pathA_: PathAndOffset? = null // = PathAndOffset();
    var pathB_: PathAndOffset? = null // = PathAndOffset();
    var pathC_: PathAndOffset? = null // = PathAndOffset();
    var pathS_: PathAndOffset? = null // = PathAndOffset();
    var dimH_ = 0f
    var nodeTriangleA_: Triangle? = null
    var nodeTriangleB_: Triangle? = null
    var nodeTriangleC_: Triangle? = null
    var isChildB_ = false
    var isChildC_ = false
    var isFloating_ = false
    var isColored_ = false
    val lengthA_: Float
        get() = length[0]
    val lengthB_: Float
        get() = length[1]
    val lengthC_: Float
        get() = length[2]
    val lengthAforce_: Float
        get() = lengthNotSized[0]
    val lengthBforce_: Float
        get() = lengthNotSized[1]
    val lengthCforce_: Float
        get() = lengthNotSized[2]
    val pointCA_: PointXY
        get() = point[0].clone()

    fun pointAB_(): PointXY {
        return PointXY(pointAB_)
    }

    fun pointBC_(): PointXY {
        return PointXY(pointBC_)
    }


    //endregion
    //region constructor
    // set argument methods
    private fun initBasicArguments(A: Float, B: Float, C: Float, pCA: PointXY?, angle: Float) {
        length[0] = A
        length[1] = B
        length[2] = C
        lengthNotSized[0] = A
        lengthNotSized[1] = B
        lengthNotSized[2] = C
        valid_ = isValid
        point[0] = PointXY(pCA!!.x, pCA.y)
        pointAB_ = PointXY(0.0f, 0.0f)
        pointBC_ = PointXY(0.0f, 0.0f)
        pointCenter_ = PointXY(0.0f, 0.0f)
        //this.pointNumber_ = new PointXY(0.0f, 0.0f);
        this.angle = angle
        angleCA = 0f
        angleAB = 0f
        angleBC = 0f
        //childSide_ = 0;
        //myDimAlignA = 0;
        //myDimAlignB = 0;
        //myDimAlignC = 0;
    }

    public override fun clone(): Triangle {
        val b = Triangle()
        try {
            b.length = length.copyOf(length.size)
            b.lengthNotSized = lengthNotSized.copyOf(lengthNotSized.size)
            b.point = point
            b.angle = angle
            b.myName_ = myName_
            b.myNumber_ = myNumber_
            b.parentBC = parentBC
            b.parentNumber = parentNumber
            b.dimPointA_ = dimPointA_
            b.myDimAlignA_ = myDimAlignA_
            b.myDimAlignB_ = myDimAlignB_
            b.myDimAlignC_ = myDimAlignC_
            b.dimSideAlignA_ = dimSideAlignA_
            b.dimSideAlignB_ = dimSideAlignB_
            b.dimSideAlignC_ = dimSideAlignC_
            b.point[0] = point[0].clone()
            b.pointAB_ = pointAB_.clone()
            b.pointBC_ = pointBC_.clone()
            b.pointCenter_ = pointCenter_.clone()
            b.pointNumberAutoAligned_ = pointNumberAutoAligned_.clone()
            b.isPointNumberMovedByUser_ = isPointNumberMovedByUser_
            b.myBP_.left = myBP_.left
            b.myBP_.top = myBP_.top
            b.myBP_.right = myBP_.right
            b.myBP_.bottom = myBP_.bottom
            b.nodeTriangleA_ = nodeTriangleA_
            b.nodeTriangleB_ = nodeTriangleB_
            b.nodeTriangleC_ = nodeTriangleC_
            b.childSide_ = childSide_
            b.color_ = color_
            b.connectionLCR_ = connectionLCR_
            b.connectionType_ = connectionType_
            b.cParam_ = cParam_.clone()
            b.isFloating_ = isFloating_
            b.isColored_ = isColored_
        } catch (e: Exception) {
            //e.printStackTrace();
        }
        return b
    }

    internal constructor()

    /*
    Triangle(int A, int B, int C){
        setNumber(1);
        pointCA = new PointXY(0f,0f);
        myAngle = 180f;
        initBasicArguments(A, B, C, pointCA, myAngle);
        calcPoints(pointCA, myAngle);
        myDimAlign = setDimAlign();
    }
*/
    internal constructor(A: Float, B: Float, C: Float) {
        setNumber(1)
        point[0] = PointXY(0f, 0f)
        angle = 180f
        initBasicArguments(A, B, C, point[0], angle)
        calcPoints(point[0], angle)
        //myDimAlign_ = autoSetDimAlign();
    }

    //for first triangle.
    internal constructor(A: Float, B: Float, C: Float, pCA: PointXY?, angle: Float) {
        setNumber(1)
        initBasicArguments(A, B, C, pCA, angle)
        calcPoints(pCA, angle)
    }

    internal constructor(myParent: Triangle?, pbc: Int, A: Float, B: Float, C: Float) {
        setOn(myParent, pbc, A, B, C)
        //autoSetDimAlign();
    }

    internal constructor(myParent: Triangle?, cParam: ConnParam, B: Float, C: Float) {
        setOn(myParent, cParam, B, C)
    }

    internal constructor(parent: Triangle, pbc: Int, B: Float, C: Float) {
        initBasicArguments(
            parent.getLengthByIndex(pbc),
            B,
            C,
            parent.getPointBySide(pbc),
            parent.getAngleBySide(pbc)
        )
        setOn(parent, pbc, B, C)
    }

    internal constructor(myParent: Triangle?, dP: Params) {
        setOn(myParent, dP.pl, dP.a, dP.b, dP.c)
        myName_ = dP.name
        autoSetDimSideAlign()
    }

    internal constructor(dP: Params, angle: Float) {
        setNumber(dP.n)
        setMyName_(dP.name)
        initBasicArguments(dP.a, dP.b, dP.c, dP.pt, angle)
        calcPoints(dP.pt, angle)
    }

    fun getPointByCParam(cparam: ConnParam, prnt: Triangle?): PointXY? {
        if (prnt == null) return PointXY(0f, 0f)
        val cside = cparam.side
        //pp.add( getPointBy( pp, length[0], clcr ) );
        return getPointBySide(cside)
    }

    fun getPointByBackSide(i: Int): PointXY? {
        if (getSideByIndex(i) == "B") return pointAB_()
        return if (getSideByIndex(i) == "C") pointBC_() else null
    }

    fun getParentPointByType(cParam: ConnParam): PointXY {
        return getParentPointByType(cParam.side, cParam.type, cParam.lcr)
    }

    fun getParentPointByType(pbc: Int, pct: Int, lcr: Int): PointXY {
        return if (nodeTriangleA_ == null) PointXY(0f, 0f) else when (pct) {
            1 -> getParentPointByLCR(pbc, lcr)
            2 -> getParentPointByLCR(pbc, lcr).crossOffset(
                nodeTriangleA_!!.getPointByBackSide(pbc)!!,
                -1.0f
            )

            else -> {
                nodeTriangleA_!!.getPointBySide(pbc)
                getParentPointByLCR(pbc, lcr)
                getParentPointByLCR(pbc, lcr).crossOffset(
                    nodeTriangleA_!!.getPointByBackSide(pbc)!!,
                    -1.0f
                )
            }
        }
    }

    fun getParentPointByLCR(pbc: Int, lcr: Int): PointXY {
        if (nodeTriangleA_ == null) return PointXY(0f, 0f)
        when (pbc) {
            1 -> when (lcr) {
                0 -> return nodeTriangleA_!!.pointAB_.offset(
                    nodeTriangleA_!!.pointBC_,
                    length[0]
                )

                1 -> return getParentOffsetPointBySide(pbc)
                2 -> return nodeTriangleA_!!.pointBC_.clone()
            }

            2 -> when (lcr) {
                0 -> return nodeTriangleA_!!.pointBC_.offset(
                    nodeTriangleA_!!.point[0],
                    length[0]
                )

                1 -> return getParentOffsetPointBySide(pbc)
                2 -> return nodeTriangleA_!!.point[0].clone()
            }
        }
        return PointXY(0f, 0f)
    }

    fun getNode(pbc: Int): Triangle {
        return when (pbc) {
            0 -> nodeTriangleA_!!
            1 -> nodeTriangleB_!!
            2 -> nodeTriangleC_!!
            -1 -> this
            else -> this
        }
    }

    fun getParentOffsetPointBySide(pbc: Int): PointXY {
        return if (nodeTriangleA_ == null) PointXY(0f, 0f) else when (pbc) {
            1 -> nodeTriangleA_!!.pointAB_.offset(
                nodeTriangleA_!!.pointBC_, nodeTriangleA_!!.lengthB * 0.5f + length[0] * 0.5f
            )

            2 -> nodeTriangleA_!!.pointBC_.offset(
                nodeTriangleA_!!.point[0],
                nodeTriangleA_!!.lengthC * 0.5f + length[0] * 0.5f
            )

            else -> nodeTriangleA_!!.getPointBySide(pbc)!!
        }
    }

    override fun getParams(): Params {
        return Params(
            myName_,
            "",
            myNumber_,
            length[0],
            length[1],
            length[2],
            parentNumber,
            parentBC,
            point[0],
            pointCenter_
        )
    }

    fun myName_(): String {
        return myName_
    }

    fun getTapLength(tapP: PointXY, rangeRadius: Float): Int {
        setDimPoint()
        val range = rangeRadius * scale_
        if (tapP.nearBy(pointName_, range)) return 4.also { lastTapSide_ = it }
        if (tapP.nearBy(dimPointA_, range)) return 0.also { lastTapSide_ = it }
        if (tapP.nearBy(dimPointB_, range)) return 1.also { lastTapSide_ = it }
        if (tapP.nearBy(dimPointC_, range)) return 2.also { lastTapSide_ = it }
        return if (tapP.nearBy(pointNumberAutoAligned_, range)) 3.also {
            lastTapSide_ = it
        } else -1.also {
            lastTapSide_ = it
        }
    }

    val dimAlignA: Int
        get() = calcDimAlignByInnerAngleOf(0)

    /*
  if( myAngle <= 90 || getAngle() >= 270 ) {
      if( lengthA*scale_ > 1.5f ) return myDimAlignA = 3;
      else return myDimAlignA = 1;
  }
  else {
      if( lengthA*scale_ > 1.5f ) return myDimAlignA = 1;
      else return myDimAlignA = 3;
  }*/
    val dimAlignB: Int
        get() = calcDimAlignByInnerAngleOf(1)

    /*        if( getAngleMpAB() <= 450f || getAngleMpAB() >= 270f ||
                 getAngleMpAB() <= 90f || getAngleMpAB() >= -90f ) {
             if( childSide_ == 3 || childSide_ == 4 ) return myDimAlignB = 3;
             if( lengthB*scale_ > 1.5f ) return myDimAlignB = 3;
             else return myDimAlignB = 1;
         }
 
         if( childSide_ == 3 || childSide_ == 4 ) return myDimAlignB = 1;
         return  myDimAlignB = 3;*/
    val dimAlignC: Int
        get() = calcDimAlignByInnerAngleOf(2)

    /*
         if( getAngleMmCA() <= 450f || getAngleMmCA() >= 270f ||
                 getAngleMmCA() <= 90f || getAngleMmCA() >= -90f ) {
             if( childSide_ == 5 || childSide_ == 6 ) return myDimAlignC = 3;
             if( lengthC*scale_ > 1.5f ) return myDimAlignC = 3;
             else return myDimAlignC = 1;
         }
 
         if( childSide_ == 5 || childSide_ == 6 ) return myDimAlignC = 1;
         return  myDimAlignC = 3;*/
    fun pointCenter_(): PointXY {
        return PointXY(pointCenter_)
    }

    var parent: Triangle? = null
        get() = nodeTriangleA_?.clone()


    fun collision(): Boolean {
        return true
    }

    private fun setMyBound() {
        val lb: PointXY
        lb = pointAB_.min(pointBC_)
        myBP_.left = lb.x
        myBP_.bottom = lb.y
        val rt: PointXY
        rt = pointAB_.max(pointBC_)
        myBP_.right = rt.x
        myBP_.top = rt.y
    }

    override fun getArea(): Float {
        val sumABC = lengthNotSized[0] + lengthNotSized[1] + lengthNotSized[2]
        val myArea =
            sumABC * 0.5f * (sumABC * 0.5f - lengthNotSized[0]) * (sumABC * 0.5f - lengthNotSized[1]) * (sumABC * 0.5f - lengthNotSized[2])
        //myArea = roundByUnderTwo( myArea );
        return roundByUnderTwo(myArea.pow(0.5f))
    }

    fun roundByUnderTwo(fp: Float): Float {
        val ip = fp * 100f
        return Math.round(ip) / 100f
    }

    val lengthA: Float
        get() = length[0]
    val lengthB: Float
        get() = length[1]
    val lengthC: Float
        get() = length[2]

    fun getLengthByIndex(i: Int): Float {
        if (i == 1) return length[1]
        return if (i == 2) length[2] else 0f
    }

    fun getLengthByIndexForce(i: Int): Float {
        if (i == 1) return lengthNotSized[1]
        return if (i == 2) lengthNotSized[2] else 0f
    }

    fun getPointBySide(i: Int): PointXY? {
        if (getSideByIndex(i) == "B") return pointBC_()
        return if (getSideByIndex(i) == "C") pointCA_ else null
    }

    fun getAngleBySide(i: Int): Float {
        if (getSideByIndex(i) == "B") return angleMpAB
        return if (getSideByIndex(i) == "C") angleMmCA else 0f
    }

    fun getSideByIndex(i: Int): String {
        if (i == 1 || i == 3 || i == 4 || i == 7 || i == 9) return "B"
        if (i == 2 || i == 5 || i == 6 || i == 8 || i == 10) return "C" else if (i == 0) return "not connected"
        return "not connected"
    }

    val parentSide: Int
        get() {
            val i = parentBC
            if (i == 1 || i == 3 || i == 4 || i == 7 || i == 9) return 1
            return if (i == 2 || i == 5 || i == 6 || i == 8 || i == 10) 2 else 0
        }

    fun getPbc(pbc: Int): Int {
        if (pbc == 1 || pbc == 3 || pbc == 4 || pbc == 7 || pbc == 9) return 1
        return if (pbc == 2 || pbc == 5 || pbc == 6 || pbc == 8 || pbc == 10) 2 else 0
    }

    val angleMmCA: Float
        get() = angle - angleCA
    val angleMpAB: Float
        get() = angle + angleAB
    val lengthScaledNotConnected: Float
        get() {
            if (nodeTriangleB_ == null) return lengthB_
            return if (nodeTriangleC_ == null) lengthC_ else lengthA_
        }

    //endregion
    //endregion
    //region setter
    fun resetNode(prms: Params, parent: Triangle?, doneObjectList: ArrayList<Triangle>) {

        // 接続情報の変更、とりあえず挿入処理は考慮しない、すでに他のノードがあるときは上書きする。
        nodeTriangleA_!!.removeNode(this)
        nodeTriangleA_ = parent
        nodeTriangleA_!!.setNode(this, prms.pl)
        reset(prms)
        doneObjectList.add(this)
    }

    fun setScale(scale: Float) {
        scale_ = scale
        length[0] *= scale
        length[1] *= scale
        length[2] *= scale
        calcPoints(point[0], angle)
    }

    fun setReverseDefSide(pbc: Int, BtoC: Boolean) {
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        if (!BtoC) {
            parentBC =
                if (pbc == 3) 4 else if (pbc == 4) 3 else if (pbc == 5) 6 else if (pbc == 6) 5 else pbc
        }
        if (BtoC) {
            parentBC =
                if (pbc == 3) 6 else if (pbc == 4) 5 else if (pbc == 5) 4 else if (pbc == 6) 3 else if (pbc == 9) 10 else if (pbc == 10) 9 else -pbc + 3
        }
    }

    fun setParentBCFromCLCR() {
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        if (cParam_.type == 2) return
        when (cParam_.lcr) {
            0 -> when (cParam_.side) {
                1 -> parentBC = 4
                2 -> parentBC = 6
            }

            1 -> when (cParam_.side) {
                1 -> parentBC = 7
                2 -> parentBC = 8
            }

            2 -> when (cParam_.side) {
                1 -> parentBC = 3
                2 -> parentBC = 5
            }
        }
    }

    fun setOn(parent: Triangle?, pbc: Int, B: Float, C: Float): Triangle {
        //myNumber_ = parent.myNumber_ + 1;
        parentBC = pbc
        if (parent == null) {
            resetLength(pbc.toFloat(), B, C)
            return clone()
        } else {
            setNode(parent, 0)
            parent.setNode(this, parentSide)
        }

        //setParent(parent, A);
        when (pbc) {
            1 -> {
                parentBC = 1
                length[0] = nodeTriangleA_!!.lengthNotSized[1]
                point[0] = nodeTriangleA_!!.pointBC_()
                angle = nodeTriangleA_!!.angleMpAB
            }
            2 -> {
                parentBC = 2
                length[0] = nodeTriangleA_!!.lengthNotSized[2]
                point[0] = nodeTriangleA_!!.pointCA_
                angle = nodeTriangleA_!!.angleMmCA
            }
            else -> {
                parentBC = 0
                length[0] = 0f
                lengthNotSized[0] = 0f
                point[0] = PointXY(0f, 0f)
                angle = 180f
            }
        }
        parentNumber = nodeTriangleA_!!.myNumber_
        //nodeTriangleA_.setChild(this, parentBC_);
        initBasicArguments(length[0], B, C, point[0], angle)
        calcPoints(point[0], angle)

        //myDimAlign = setDimAlign();
        return this
    }

    /*
    void set(Triangle parent, int pbc, float A, float B, float C, boolean byNode ) {
        set( parent, pbc, A, B, C );

        if(byNode){
            parent.resetByNode( pbc );
        }

        if( nodeTriangleB_ != null ) nodeTriangleB_.set( this, nodeTriangleB_.parentBC_, this.getLengthByIndex( nodeTriangleB_.getParentSide() ), nodeTriangleB_.length[1], nodeTriangleB_.length[2] );
        if( nodeTriangleC_ != null ) nodeTriangleC_.set( this, nodeTriangleC_.parentBC_, this.getLengthByIndex( nodeTriangleC_.getParentSide() ), nodeTriangleC_.length[1], nodeTriangleC_.length[2] );

    }
*/
    fun setOn(parent: Triangle?, pbc: Int, A: Float, B: Float, C: Float): Triangle? {
        //myNumber_ = parent.myNumber_ + 1;
        parentBC = pbc
        if (parent == null) {
            resetLength(pbc.toFloat(), B, C)
            return clone()
        } else {
            setNode(parent, 0)
            parent.setNode(this, parentSide)
        }

        //setParent(parent, pbc);
        //nodeTriangleA_.setChild(this, pbc );

        // if user rewrite A
        if (A != parent.getLengthByIndex(pbc)) {
            length[0] = A
            lengthNotSized[0] = A
        } else {
            length[0] = parent.getLengthByIndex(pbc)
            lengthNotSized[0] = parent.getLengthByIndex(pbc)
        }
        setCParamFromParentBC(pbc)
        point[0] = getParentPointByType(cParam_)
        when (pbc) {
            1 -> { // B
                parentBC = 1
                angle = parent.angleMpAB
            }
            2 -> { // C
                parentBC = 2
                angle = parent.angleMmCA
            }
            3 -> { // B-R
                parentBC = 3
                angle = parent.angleMpAB
            }
            4 -> { //B-L
                parentBC = 4
                angle = parent.angleMpAB
            }
            5 -> { //C-R
                parentBC = 5
                angle = parent.angleMmCA
            }
            6 -> { //C-L
                parentBC = 6
                angle = parent.angleMmCA
            }
            7 -> { //B-Center
                parentBC = 7
                angle = parent.angleMpAB
            }
            8 -> { //C-Center
                parentBC = 8
                angle = parent.angleMmCA
            }
            9 -> { //B-Float-R
                parentBC = 9
                angle = parent.angleMpAB
            }
            10 -> { //C-Float-R
                parentBC = 10
                angle = parent.angleMmCA
            }
            else -> {
                parentBC = 0
                length[0] = 0f
                lengthNotSized[0] = 0f
                point[0] = PointXY(0f, 0f)
                angle = 180f
            }
        }
        parentNumber = parent.myNumber_
        initBasicArguments(length[0], B, C, point[0], angle)
        if (!isValid) return null
        calcPoints(point[0], angle)
        if (parentBC == 4) {
            val vector = PointXY(
                parent.pointAB_().x - pointAB_.x,
                parent.pointAB_().y - pointAB_.y
            )
            move(vector)
        }
        if (parentBC == 6) {
            val vector = PointXY(
                parent.pointBC_().x - pointAB_.x,
                parent.pointBC_().y - pointAB_.y
            )
            move(vector)
        }
        //if( parentBC_ >= 9 ) rotate( point[0], angleInLocal_, true );

        //myDimAlign = setDimAlign();
        return clone()
    }

    fun setOn(parent: Triangle?, cParam: ConnParam, B: Float, C: Float): Triangle? {
        //myNumber_ = parent.myNumber_ + 1;
        //parentBC_ = cParam.getSide();
        if (parent == null) {
            resetLength(cParam.lenA, B, C)
            return clone()
        } else {
            setNode(parent, 0)
            parent.setNode(this, cParam.side)
        }

        //setParent( parent, cParam.getSide() );
        angle = parent.getAngleBySide(cParam.side)
        setConnectionType(cParam)
        initBasicArguments(length[0], B, C, point[0], angle)
        if (!isValid) return null
        calcPoints(point[0], angle)

        //if( parentBC_ >= 9 ) rotate( point[0], angleInLocal_, true );
        setDimAlignByChild()

        //nodeTriangleA_.setChild(this, cParam.getSide() );
        return clone()
    }

    fun setDimPath(ts: Float) {
        dimH_ = ts
        pathA_ = PathAndOffset(scale_, pointAB_, point[0], myDimAlignA_, dimSideAlignA_, dimH_)
        pathB_ = PathAndOffset(scale_, pointBC_, pointAB_, myDimAlignB_, dimSideAlignB_, dimH_)
        pathC_ = PathAndOffset(scale_, point[0], pointBC_, myDimAlignC_, dimSideAlignC_, dimH_)
        pathS_ = PathAndOffset(scale_, pointAB_, point[0], 4, 0, dimH_)

        //sla_ = formattedString( lengthNotSized[0], 3);
        //slb_ = formattedString( lengthNotSized[1], 3);
        //slc_ = formattedString( lengthNotSized[2], 3);
    }



    fun setCParamFromParentBC(pbc: Int) {
        var curLCR = cParam_.lcr
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        if (cParam_.side == 0 && (pbc == 4 || pbc == 6)) curLCR = 0
        if (cParam_.side == 0 && (pbc == 7 || pbc == 8)) curLCR = 1
        when (pbc) {
            -1, 0 -> cParam_ = ConnParam(0, 0, 2, lengthNotSized[0])
            1 -> cParam_ = ConnParam(1, 0, 2, lengthNotSized[0])
            2 -> cParam_ = ConnParam(2, 0, 2, lengthNotSized[0])
            3 -> cParam_ = ConnParam(1, 1, 2, lengthNotSized[0])
            4 -> cParam_ = ConnParam(1, 1, 0, lengthNotSized[0])
            5 -> cParam_ = ConnParam(2, 1, 2, lengthNotSized[0])
            6 -> cParam_ = ConnParam(2, 1, 0, lengthNotSized[0])
            7 -> cParam_ = ConnParam(1, 1, 1, lengthNotSized[0])
            8 -> cParam_ = ConnParam(2, 1, 1, lengthNotSized[0])
            9 -> cParam_ = ConnParam(1, 2, curLCR, lengthNotSized[0])
            10 -> cParam_ = ConnParam(2, 2, curLCR, lengthNotSized[0])
        }
    }

    fun setDimAlignByChild() {
        if (!isChangeDimAlignB_) {
            myDimAlignB_ = if (nodeTriangleB_ == null) 1 else 3
        }
        if (!isChangeDimAlignC_) {
            myDimAlignC_ = if (nodeTriangleC_ == null) 1 else 3
        }
    }

    fun setDimAligns(sa: Int, sb: Int, sc: Int, ha: Int, hb: Int, hc: Int) {
        dimSideAlignA_ = sa
        dimSideAlignB_ = sb
        dimSideAlignC_ = sc
        myDimAlignA_ = ha
        myDimAlignB_ = hb
        myDimAlignC_ = hc
    }

    fun setDimPoint() {
        dimPointA_ =
            pathA_!!.pointD //dimSideRotation( dimSideAlignA_, point[0].calcMidPoint(pointAB_), pointAB_, point[0]);
        dimPointB_ =
            pathB_!!.pointD //dimSideRotation( dimSideAlignB_, pointAB_.calcMidPoint(pointBC_), pointBC_, pointAB_);
        dimPointC_ =
            pathC_!!.pointD //dimSideRotation( dimSideAlignC_, pointBC_.calcMidPoint(point[0]), point[0], pointBC_);
    }

    fun setNode(node: Triangle?, side: Int) {
        var side = side
        if (node == null) return
        if (side > 2) side = parentSide
        when (side) {
            -1 -> {}
            0 -> {
                nodeTriangleA_ = node
                if (node === nodeTriangleB_) nodeTriangleB_ = null
                if (node === nodeTriangleC_) nodeTriangleC_ = null
            }

            1 -> {
                nodeTriangleB_ = node
                if (node === nodeTriangleA_) nodeTriangleA_ = null
                if (node === nodeTriangleC_) nodeTriangleC_ = null
            }

            2 -> {
                nodeTriangleC_ = node
                if (node === nodeTriangleB_) nodeTriangleB_ = null
                if (node === nodeTriangleA_) nodeTriangleA_ = null
            }
        }
    }

    fun setNumber(num: Int) {
        myNumber_ = num
    }

    fun setColor(num: Int) {
        color_ = num
        isColored_ = true
    }

    fun setChild(newchild: Triangle, cbc: Int) {
        childSide_ = cbc
        if (newchild.getPbc(cbc) == 1) {
            nodeTriangleB_ = newchild
            isChildB_ = true
        }
        if (newchild.getPbc(cbc) == 2) {
            nodeTriangleC_ = newchild
            isChildC_ = true
        }
        setDimAlignByChild()
    }

    fun setBasePoint(cParam: ConnParam): PointXY {
        point[0] = getParentPointByType(cParam.side, cParam.type, cParam.lcr)
        connectionType_ = cParam.type
        connectionLCR_ = cParam.lcr
        calcPoints(point[0], angle)
        return point[0]
    }

    fun setBasePoint(pbc: Int, pct: Int, lcr: Int): PointXY {
        point[0] = getParentPointByType(pbc, pct, lcr)
        connectionType_ = pct
        connectionLCR_ = lcr
        calcPoints(point[0], angle)
        return point[0]
    }

    fun reset(newTri: Triangle) {
        val thisCP = cParam_.clone()
        if (nodeTriangleA_ == null || parentNumber < 1) {
            angle = newTri.angle
            angleInLocal_ = newTri.angleInLocal_
            resetLength(newTri.length[0], newTri.length[1], newTri.length[2])
        } else setOn(
            nodeTriangleA_,
            newTri.parentBC,
            newTri.length[0],
            newTri.length[1],
            newTri.length[2]
        )
        cParam_ = thisCP.clone()
        myName_ = newTri.myName_
        clone()
    }

    fun reset(newTri: Triangle, cParam: ConnParam): Triangle {
        if (nodeTriangleA_ == null) resetLength(
            newTri.length[0],
            newTri.length[1],
            newTri.length[2]
        ) else setOn(nodeTriangleA_, cParam, newTri.length[1], newTri.length[2])
        myName_ = newTri.myName_
        return clone()
    }

    fun resetLength(A: Float, B: Float, C: Float): Triangle {
        //lengthA = A; lengthB = B; lengthC = C;
        initBasicArguments(A, B, C, point[0], angle)
        calcPoints(point[0], angle)
        return clone()
    }

    fun resetByParent(prnt: Triangle, cParam: ConnParam): Boolean {
        if (!isValidLengthes(prnt.getLengthByIndex(parentSide), length[1], length[2])) return false
        val triIsValid = setOn(prnt, cParam, length[1], length[2])
        return triIsValid != null
    }

    // reset by parent.
    fun resetByParent(parent: Triangle?, pbc: Int): Boolean {
        if (parent == null) return false
        var triIsValid: Triangle? = null
        val parentLength = parent.getLengthByIndex(pbc)
        if (pbc <= 2) {
            triIsValid = if (!isValidLengthes(parentLength, length[1], length[2])) {
                return true
            } else setOn(parent, pbc, parentLength, length[1], length[2])
        }
        if (pbc > 2) triIsValid = setOn(parent, pbc, length[0], length[1], length[2])
        return triIsValid == null
    }

    // 子のA辺が書き換わったら、それを写し取ってくる。同一辺長接続のとき（１か２）以外はリターン。
    fun resetByChild(myChild: Triangle) {
        setDimAlignByChild()
        if (myChild.cParam_.type != 0) return
        val cbc = myChild.parentBC
        if (cbc == 1 && !isValidLengthes(length[0], myChild.length[0], length[2])) return
        if (cbc == 2 && !isValidLengthes(length[0], length[1], myChild.length[0])) return
        childSide_ = myChild.parentBC
        if (nodeTriangleA_ == null || parentNumber < 1) {
            if (cbc == 1) resetLength(length[0], myChild.length[0], length[2])
            if (cbc == 2) resetLength(length[0], length[1], myChild.length[0])
            return
        }
        if (cbc == 1) {
            setOn(nodeTriangleA_, parentBC, length[0], myChild.length[0], length[2])
            //nodeTriangleB_ = myChild;
        }
        if (cbc == 2) {
            setOn(nodeTriangleA_, parentBC, length[0], length[1], myChild.length[0])
            //nodeTriangleC_ = myChild;
        }
    }

    fun setConnectionType(cParam: ConnParam) {
        //myParentBC_= cParam.getSide();
        parentNumber = nodeTriangleA_!!.myNumber_
        connectionType_ = cParam.type
        connectionLCR_ = cParam.lcr
        cParam_ = cParam.clone()
        when (cParam.side) {
            1 -> angle = nodeTriangleA_!!.angleMpAB
            2 -> angle = nodeTriangleA_!!.angleMmCA
        }
        if (cParam.type == 0) {
            if (cParam.lenA != 0.0f) {
                length[0] = cParam.lenA
                lengthNotSized[0] = cParam.lenA
                point[0] = getParentPointByType(cParam.side, cParam.type, cParam.lcr)
            } else {
                length[0] = nodeTriangleA_!!.getLengthByIndex(cParam.side)
                lengthNotSized[0] = nodeTriangleA_!!.getLengthByIndex(cParam.side)
                point[0] = nodeTriangleA_!!.getPointByCParam(cParam, nodeTriangleA_)!!
            }
        } else {
            if (cParam.lenA != 0.0f) {
                length[0] = cParam.lenA
                lengthNotSized[0] = cParam.lenA
            }
            point[0] = getParentPointByType(cParam.side, cParam.type, cParam.lcr)
        }
    }

    fun reset(prm: Params) {
        //ConneParam thisCP = cParam_.clone();
        length[0] = prm.a
        lengthNotSized[0] = prm.a
        setCParamFromParentBC(prm.pl)
        parentBC = prm.pl
        parentNumber = prm.pn
        if (nodeTriangleA_ == null || parentNumber < 1) resetLength(prm.a, prm.b, prm.c) else {
            setOn(nodeTriangleA_, cParam_, prm.b, prm.c)
        }
        //set(parent_, tParams.getPl(), tParams.getA(), tParams.getB(), tParams.getC() );
        //cParam_ = thisCP.clone();
        myName_ = prm.name
    }

    fun resetElegant(prm: Params) {
        reset(prm)
        if (nodeTriangleA_ != null) nodeTriangleA_!!.resetByNode(parentSide)
    }

    fun resetByNode(pbc: Int) {
        val node = getNode(pbc)
        var lengthConnected = getLengthByIndex(pbc)
        if (node.parentBC < 3) lengthConnected = node.length[0]
        when (pbc) {
            0 -> {}
            1 -> initBasicArguments(
                length[0],
                lengthConnected,
                length[2],
                node.pointBC_,
                -node.angle
            )

            2 -> initBasicArguments(
                length[0],
                length[1],
                lengthConnected,
                node.point[0],
                node.angle + angleBC
            )
        }
        calcPoints(point[0], angle)
    }

    //endregion
    fun removeNode(target: Triangle) {
        if (nodeTriangleA_ === target) nodeTriangleA_ = null
        if (nodeTriangleB_ === target) nodeTriangleB_ = null
        if (nodeTriangleC_ === target) nodeTriangleC_ = null
    }

    fun expandBoundaries(listBound: Bounds): Bounds {
        setMyBound()
        val newB = Bounds(myBP_.left, myBP_.top, myBP_.right, myBP_.bottom)
        // 境界を比較し、広い方に置き換える
        if (myBP_.bottom > listBound.bottom) newB.bottom = listBound.bottom
        if (myBP_.top < listBound.top) newB.top = listBound.top
        if (myBP_.left > listBound.left) newB.left = listBound.left
        if (myBP_.right < listBound.right) newB.right = listBound.right
        return newB
    }

    fun autoSetDimAlign(): Int { // 1:下 3:上
        myDimAlignA_ = calcDimAlignByInnerAngleOf(0)
        myDimAlignB_ = calcDimAlignByInnerAngleOf(1)
        myDimAlignC_ = calcDimAlignByInnerAngleOf(2)

        /*
        if(  getAngle() <= 90 || getAngle() >= 270 ) { // 基線の角度が90°～270°の間
            myDimAlign = 3;
            myDimAlignA = 3;
        } else { // それ以外(90°以下～270°の間)
            myDimAlign = 1;
            myDimAlignA = 1;
            myDimAlignB = 1;
            myDimAlignC = 1;
        }

        //AB
        if( getAngleMpAB() <= 90 || getAngle() >= 270 ) { // 基線+ABの角度が90°～270°の間
            myDimAlignB = 3;
        } else {
            myDimAlignB = 1;
        }

        if( getAngleMmCA() <= 90 || getAngle() >= 270 ) { // 基線-CAの角度が90°～270°の間
            myDimAlignC = 3;
        } else {
            myDimAlignC = 1;
        }
*/setDimPath(dimH_)
        return myDimAlign_
    }

    //region calculater
    fun calculateInternalAngle(p1: PointXY?, p2: PointXY?, p3: PointXY?): Double {
        val v1 = p1!!.subtract(p2!!)
        val v2 = p3!!.subtract(p2)
        val angleRadian = acos(v1.innerProduct(v2) / (v1.magnitude() * v2.magnitude()))
        return angleRadian * 180 / Math.PI
    }

    fun calcMyAngles() {
        angleAB = calculateInternalAngle(point[0], pointAB_, pointBC_).toFloat()
        angleBC = calculateInternalAngle(pointAB_, pointBC_, point[0]).toFloat()
        angleCA = calculateInternalAngle(pointBC_, point[0], pointAB_).toFloat()
    }

    fun calcPoints(ref: Triangle?, refside: Int) {
        setNode(ref, refside)
        val plist: Array<PointXY?>
        val llist: FloatArray
        val powlist: FloatArray
        val angle: Float
        when (refside) {
            0 -> {
                angle = this.angle
                plist = arrayOf(point[0], pointAB_, pointBC_)
                llist = floatArrayOf(length[0], length[1], length[2])
                powlist = floatArrayOf(length[0].pow(2.0f), length[1].pow(2.0f), length[2].pow(2.0f))
            }

            1 -> {
                angle = nodeTriangleB_!!.angle + 180f //- nodeTriangleB_.angleInnerCA_;
                plist = arrayOf(nodeTriangleB_!!.pointAB_, pointBC_, point[0])
                llist = floatArrayOf(length[1], length[2], length[0])
                powlist = floatArrayOf(length[1].pow(2.0f), length[2].pow(2.0f), length[0].pow(2.0f))
                pointAB_ = plist[0]!!
            }

            2 -> {
                angle = nodeTriangleC_!!.angle + 180f //- nodeTriangleB_.angleInnerCA_;
                plist = arrayOf(nodeTriangleC_!!.pointAB_, point[0], pointAB_)
                llist = floatArrayOf(length[2], length[0], length[1])
                powlist = floatArrayOf(length[2].pow(2.0f), length[0].pow(2.0f), length[1].pow(2.0f))
                pointBC_ = plist[0]!!
            }

            else -> throw IllegalStateException("Unexpected value: $refside")
        }
        plist[1]!![(plist[0]!!.x + llist[0] * cos(Math.toRadians(angle.toDouble()))).toFloat()] =
            (plist[0]!!.y + llist[0] * sin(
                Math.toRadians(angle.toDouble())
            )).toFloat()
        myTheta_ = atan2(
            (plist[0]!!.y - plist[1]!!.y).toDouble(),
            (plist[0]!!.x - plist[1]!!.x).toDouble()
        )
        myAlpha_ = acos((powlist[0] + powlist[1] - powlist[2]) / (2 * llist[0] * llist[1])).toDouble()
        plist[2]!![(plist[1]!!.x + llist[1] * cos(myTheta_ + myAlpha_)).toFloat()] =
            (plist[1]!!.y + llist[1] * sin(myTheta_ + myAlpha_)).toFloat()
        calcMyAngles()
        if (refside == 1) this.angle = nodeTriangleB_!!.angle - angleCA
        if (refside == 2) this.angle = nodeTriangleC_!!.angle + angleCA
    }

    fun calcPoints(pCA: PointXY?, angle: Float) {
        calculatePointAB(pCA, angle)
        calculateTheta(pCA)
        calculateSidesSquared()
        calculateAlpha()
        calculatePointBC()
        calculateInternalAngles()
        calculatePointCenter()
        finalizeCalculations()
    }

    private fun calculatePointAB(pCA: PointXY?, angle: Float) {
        pointAB_[(pCA!!.x + length[0] * cos(Math.toRadians(angle.toDouble()))).toFloat()] =
            (pCA.y + length[0] * sin(Math.toRadians(angle.toDouble()))).toFloat()
    }

    private fun calculateTheta(pCA: PointXY?) {
        myTheta_ = atan2((pCA!!.y - pointAB_.y).toDouble(), (pCA.x - pointAB_.x).toDouble())
    }

    private fun calculateSidesSquared() {
        myPowA_ = length[0].pow(2.0f).toDouble()
        myPowB_ = length[1].pow(2.0f).toDouble()
        myPowC_ = length[2].pow(2.0f).toDouble()
    }

    private fun calculateAlpha() {
        myAlpha_ = acos((myPowA_ + myPowB_ - myPowC_) / (2 * length[0] * length[1]))
    }

    private fun calculatePointBC() {
        pointBC_[(pointAB_.x + length[1] * cos(myTheta_ + myAlpha_)).toFloat()] =
            (pointAB_.y + length[1] * sin(myTheta_ + myAlpha_)).toFloat()
    }

    private fun calculateInternalAngles() {
        angleAB = calculateInternalAngle(point[0], pointAB_, pointBC_).toFloat()
        angleBC = calculateInternalAngle(pointAB_, pointBC_, point[0]).toFloat()
        angleCA = calculateInternalAngle(pointBC_, point[0], pointAB_).toFloat()
    }

    private fun calculatePointCenter() {
        pointCenter_[(pointAB_.x + pointBC_.x + point[0].x) / 3] =
            (pointAB_.y + pointBC_.y + point[0].y) / 3
    }

    private fun finalizeCalculations() {
        if (!isPointNumberMovedByUser_) {
            autoAlignPointNumber()
        }
        setMyBound()
        setDimPath(dimH_)
        setDimPoint()
        dimAngleB_ = angleMpAB
        dimAngleC_ = angleMmCA
    }

    fun calcDimAlignByInnerAngleOf(side: Int): Int {    // 夾角の、1:外 　3:内
        // side == 0 の場合の特定の条件で 1 を返す
        if (side == 0 && (parentBC == 9 || parentBC == 10 || parentBC > 2 || nodeTriangleB_ != null || nodeTriangleC_ != null)) {
            return 1
        }

        // side == 1 または side == 2 の場合、それぞれ nodeTriangleB_ と nodeTriangleC_ が null でなければ 1 を返す
        return if (side == 1 && nodeTriangleB_ != null || side == 2 && nodeTriangleC_ != null
        ) {
            1
        } else 3

        // 上記のいずれの条件にも当てはまらない場合は 3 を返す
    }

    //endregion
    fun flipOneToThree(num: Int): Int {
        if (num == 1) return 3
        return if (num == 3) 1 else num
        // sonomama kaesu.
    }

    // 呼び出す場所によって、強制になってしまう。
    fun autoSetDimSideAlign() {
        // 短い時は外側？
        if (lengthNotSized[2] < 1.5f || lengthNotSized[1] < 1.5f) {
            myDimAlignB_ = 1
            myDimAlignC_ = 1
        }
        // 和が短い時は寄せる
        //if( lengthNotSized[2]+lengthNotSized[0] < 7.0f ) dimSideAlignB_ = 1;
        //if( lengthNotSized[1]+lengthNotSized[2] < 7.0f ) dimSideAlignC_ = 2;
        // 和が短い時は外に出す
        //if( lengthNotSized[2]+lengthNotSized[0] < 5.0f ) dimSideAlignB_ = 4;
        //if( lengthNotSized[1]+lengthNotSized[2] < 5.0f ) dimSideAlignC_ = 4;
    }

    fun flipDimAlignH(side: Int) {
        if (side == 0) myDimAlignA_ = flipOneToThree(myDimAlignA_)
        if (side == 1) {
            myDimAlignB_ = flipOneToThree(myDimAlignB_)
            isChangeDimAlignB_ = true
        }
        if (side == 2) {
            myDimAlignC_ = flipOneToThree(myDimAlignC_)
            isChangeDimAlignC_ = true
        }
        if (side == 4) nameAlign_ = flipOneToThree(nameAlign_)
        setDimPath(dimH_)
    }

    // region pointNumber
    fun setPointNumberMovedByUser_(p: PointXY) {
        pointNumberAutoAligned_ = p
        isPointNumberMovedByUser_ = true
    }

    fun weightedMidpoint(bias: Float): PointXY {

        // 角度が大きいほど重みを大きくするための調整
        var weight1 = angleAB + bias // 角度が大きいほど重みが大きくなる
        var weight2 = angleBC + bias
        var weight3 = angleCA + bias

        // 重みの合計で正規化
        val totalWeight = weight1 + weight2 + weight3
        weight1 /= totalWeight
        weight2 /= totalWeight
        weight3 /= totalWeight
        val p1 = pointAB_
        val p2 = pointBC_
        val p3 = point[0]

        // 重み付き座標の計算
        val weightedX = p1.x * weight1 + p2.x * weight2 + p3.x * weight3
        val weightedY = p1.y * weight1 + p2.y * weight2 + p3.y * weight3
        return PointXY(weightedX, weightedY)
    }

    // parseCSVのTriangleList.recoverStateで、自動配置された旗揚げは一緒に回転させてやる必要があるため、ここでフラグをtrueにすると凄いことになる。
    // csv保存されたpointNumberはビューの座標で保存されるため、回転に追随させる必要がなく、自動回転されたものと扱いを分けている。
    // ということは、ここで自動配置されたものをフラグを立てない状態で保存して、また読み込んだ時にまずくなりそうだったが、またここに来て再計算されるのでOkだった
    private fun autoAlignPointNumber() {
        if (!isPointNumberMovedByUser_) {
            pointNumberAutoAligned_ = if (getArea() <= 3f && (lengthAforce_ < 1.5f || lengthBforce_ < 1.5f || lengthCforce_ < 1.5f)) {
                //isPointNumberMoved_ = true; //移動済みに変える,なくても平気？手動で移動させたときのみ？
                pointUnconnectedSide(pointCenter_, 1f, 1f, PointXY(0f, 0f)).crossOffset(
                    pointCenter_,
                    lengthScaledNotConnected
                )
            } else {
                weightedMidpoint(25f)
            }
        }
    }

    val isPointNumberMoved: Boolean
        get() = if (pointNumberAutoAligned_ != pointCenter_) true.also {
            isPointNumberMovedByUser_ = it
        } else false.also { isPointNumberMovedByUser_ = it }

    fun pointUnconnectedSide(
        ref: PointXY,
        scaleX: Float,
        scaleY: Float,
        scaleOrigin: PointXY?
    ): PointXY {
        if (nodeTriangleB_ == null) return ref.mirroredAndScaledPoint(
            pointBC_,
            pointAB_,
            scaleX,
            scaleY,
            scaleOrigin!!
        )
        return if (nodeTriangleC_ == null) ref.mirroredAndScaledPoint(
            point[0],
            pointBC_,
            scaleX,
            scaleY,
            scaleOrigin!!
        ) else weightedMidpoint(-35f)
    }

    //endregion pointNumber
    fun move(to: PointXY) {
        pointAB_.add(to)
        pointBC_.add(to)
        point[0].add(to)
        pointCenter_ = pointCenter_.plus(to)
        pointNumberAutoAligned_ = pointNumberAutoAligned_.plus(to)
        dimPointA_.add(to)
        dimPointB_.add(to)
        dimPointC_.add(to)
        myBP_.left = myBP_.left + to.x
        myBP_.right = myBP_.right + to.x
        myBP_.top = myBP_.top + to.y
        myBP_.bottom = myBP_.bottom + to.x
        pathA_!!.move(to)
        pathB_!!.move(to)
        pathC_!!.move(to)
        pathS_!!.move(to)
    }

    fun scale(basepoint: PointXY?, scale: Float) {
        scale_ *= scale
        //pointAB_.scale(basepoint, scale);
        //pointBC_.scale(basepoint, scale);
        point[0].scale(basepoint!!, scale)
        pointCenter_.scale(basepoint, scale)
        pointNumberAutoAligned_.scale(basepoint, scale)
        length[0] *= scale
        length[1] *= scale
        length[2] *= scale
        calcPoints(point[0], angle)
    }

    //region rotater
    // 自分の次の番号がくっついている辺を調べてA辺にする。
    // 他の番号にあって自身の辺上に無い場合は、A辺を変更しない。
    fun rotateLengthBy(side: Int) {
        //Triangle this = this.clone();
        var pf: Float
        var pi: Int
        var pp: PointXY
        if (side == 1) { // B to A
            pf = length[0]
            length[0] = length[1]
            length[1] = length[2]
            length[2] = pf
            pf = lengthNotSized[0]
            lengthNotSized[0] = lengthNotSized[1]
            lengthNotSized[1] = lengthNotSized[2]
            lengthNotSized[2] = pf
            pp = point[0].clone()
            point[0] = pointAB_
            pointAB_ = pointBC_
            pointBC_ = pp.clone()
            pf = angleCA
            angleCA = angleAB
            angleAB = angleBC
            angleBC = pf
            angle = angleMmCA - angleAB
            if (angle < 0) angle += 360f
            if (angle > 360) angle -= 360f
            pp = dimPointA_.clone()
            dimPointA_ = dimPointB_
            dimPointB_ = dimPointC_
            dimPointC_ = pp.clone()
            pi = myDimAlignA_
            myDimAlignA_ = myDimAlignB_
            myDimAlignB_ = myDimAlignC_
            myDimAlignC_ = pi
            pi = dimSideAlignA_
            dimSideAlignA_ = dimSideAlignB_
            dimSideAlignB_ = dimSideAlignC_
            dimSideAlignC_ = pi
        }
        if (side == 2) { // C to A
            pf = length[0]
            length[0] = length[2]
            length[2] = length[1]
            length[1] = pf
            pf = lengthNotSized[0]
            lengthNotSized[0] = lengthNotSized[2]
            lengthNotSized[2] = lengthNotSized[1]
            lengthNotSized[1] = pf
            pp = point[0].clone()
            point[0] = pointBC_
            pointBC_ = pointAB_
            pointAB_ = pp.clone()
            pf = angleCA
            angleCA = angleBC
            angleBC = angleAB
            angleAB = pf
            angle += angleCA + angleBC
            if (angle < 0) angle += 360f
            if (angle > 360) angle -= 360f
            pp = dimPointA_.clone()
            dimPointA_ = dimPointC_
            dimPointC_ = dimPointB_
            dimPointB_ = pp.clone()
            pi = myDimAlignA_
            myDimAlignA_ = myDimAlignC_
            myDimAlignC_ = myDimAlignB_
            myDimAlignB_ = pi
            pi = dimSideAlignA_
            dimSideAlignA_ = dimSideAlignC_
            dimSideAlignC_ = dimSideAlignB_
            dimSideAlignB_ = pi
        }
    }

    fun rotateLCRandGet(): Triangle {
        rotateLCR()
        return this
    }

    fun rotateLCR(): PointXY {
        //if(myParentBC_ < 3) return new PointXY(0f,0f);
        connectionLCR_--
        if (connectionLCR_ < 0) connectionLCR_ = 2
        cParam_.lcr = connectionLCR_
        setParentBCFromCLCR()
        return setBasePoint(cParam_)
    }

    fun rotateDimSideAlign(side: Int) {
        if (side == 0) dimSideAlignA_ = rotateZeroToThree(dimSideAlignA_)
        if (side == 1) dimSideAlignB_ = rotateZeroToThree(dimSideAlignB_)
        if (side == 2) dimSideAlignC_ = rotateZeroToThree(dimSideAlignC_)
        if (side == 4) nameSideAlign_ = rotateZeroToThree(nameSideAlign_)
        setDimPath(dimH_)
    }

    fun rotateZeroToThree(num: Int): Int {
        var num = num
        num++
        if (num > 4) num = 0
        return num
    }

    fun rotate(basepoint: PointXY?, addDegree: Float, recover: Boolean) {
        if (parentBC < 9 && recover) return
        if (!recover) angleInLocal_ += addDegree else angleInLocal_ = addDegree
        point[0] = point[0].rotate(basepoint!!, addDegree)
        angle += addDegree
        calcPoints(point[0], angle)
        setDimPath(dimH_)
        //setDimAlign();
        //Log.d("Triangle", "num:" + myNumber_ + "pCA: " + point[0].getX() + " , " + point[0].getY() );
        //Log.d("Triangle", "num:" + myNumber_ + "pAB: " + point[0].getX() + " , " + point[0].getY() );
        //Log.d("Triangle", "num:" + myNumber_ + "pBC: " + point[0].getX() + " , " + point[0].getY() );
        //Log.d("Triangle", "num:" + myNumber_ + "angleInGlobal/Local: " + angleInGlobal_ + " , " + angleInLocal_ );
    }

    //endregion
    fun hataage(p: PointXY, offset: Float, axisY: Float, number: Float): PointXY {
        val distanceFromCenter = p.y - pointCenter_.y * axisY
        var direction = 1f
        if (distanceFromCenter < 0) direction = -1f
        val hataage = p.clone()
        val compareY = compareY(direction, axisY)
        hataage[p.x] = p.y + (compareY - p.y) + offset * direction * (dedcount * number)

        //Log.d("Triangle Deduction", "p.getY: " + p.getY() + " , pointCenterY: " + pointCenter_.getY()+ " , axisY: " + axisY );
        //Log.d("Triangle Deduction", "DistanceFromCenter: " + distanceFromCenter + " , direction: " + direction );
        //Log.d("Triangle Deduction", "compareY: " + compareY + " , hataage.y: " + hataage.getY() );
        return hataage
    }

    fun compareY(direction: Float, axisY: Float): Float {
        val pCAy = point[0].y * axisY
        val pABy = pointAB_.y * axisY
        val pBCy = pointBC_.y * axisY
        //Log.d("Triangle Deduction", "pCAy: " + pCAy + " , pABy: " + pABy + " , pBCy: " + pBCy );
        var Y = pCAy
        if (direction == -1f) {
            if (Y > pABy) Y = pABy
            if (Y > pBCy) Y = pBCy
        } else if (direction == 1f) {
            if (Y <= pABy) Y = pABy
            if (Y <= pBCy) Y = pBCy
        }
        return Y
    }

    fun trimming(): Boolean {
        return isCollide
    }

    //region isBoolean
    fun alreadyHaveChild(pbc: Int): Boolean {
        if (pbc < 1) return false
        return if (getSideByIndex(pbc) == "B" && isChildB_) true else getSideByIndex(pbc) == "C" && isChildC_
    }

    fun hasChildIn(cbc: Int): Boolean {
        return if ((nodeTriangleB_ != null || isChildB_) && cbc == 1) true else (nodeTriangleC_ != null || isChildC_) && cbc == 2
    }

    fun hasConstantParent(): Boolean {
        val iscons = myNumber_ - parentNumber
        return iscons <= 1
    }

    fun hasChild(): Boolean {
        return nodeTriangleB_ != null || nodeTriangleC_ != null
    }

    val isValid: Boolean
        get() = if (length[0] <= 0.0f || length[1] <= 0.0f || length[2] <= 0.0f) false else isValidLengthes(
            length[0],
            length[1],
            length[2]
        )

    //!((this.length[0] + this.length[1]) <= this.length[2]) &&
    //      !((this.length[1] + this.length[2]) <= this.length[0]) &&
    //    !((this.length[2] + this.length[0]) <= this.length[1]);
    fun isValidLengthes(A: Float, B: Float, C: Float): Boolean {
        return !(A + B <= C) && !(B + C <= A) && !(C + A <= B)
    }

    val isFloating: Boolean
        get() {
            isFloating_ = nodeTriangleA_ != null && parentBC > 8
            return isFloating_
        }
    val isColored: Boolean
        get() {
            isColored_ = nodeTriangleA_ != null && color_ != nodeTriangleA_!!.color_
            return isColored_
        }
    val isCollide: Boolean
        get() = true

    fun isCollide(p: PointXY): Boolean {
        return p.isCollide(pointAB_, pointBC_, point[0])
    } //endregion



}

fun Triangle.setMyName_(name: String) {
    myName_ = name
}
fun Triangle.getLengthA(): Float {
    return lengthA_
}

fun Triangle.getLengthB(): Float {
    return lengthB_
}
fun Triangle.getLengthC(): Float {
    return lengthC_
}
