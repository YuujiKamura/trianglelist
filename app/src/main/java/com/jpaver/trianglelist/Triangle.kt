package com.jpaver.trianglelist

import com.jpaver.trianglelist.util.Cloneable
import com.jpaver.trianglelist.util.Params
import com.jpaver.trianglelist.util.cloneArray
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

@Suppress("NAME_SHADOWING")
class Triangle : EditObject, Cloneable<Triangle> {

    fun calcPoints(basepoint: PointXY, angle: Float, isArrange: Boolean = false) {
        pointAB = basepoint.offset(length[0], angle)
        pointBC = calculatePointBC( basepoint )
        calculateInternalAngles()
        calculatePointCenter()
        arrangeDims(isArrange)
        if(isArrange) pointnumber = arrangeNumber(isArrange)
        setBoundaryBox()
    }

    //region dimAlign

    // あらゆる場所でよばれる
    private fun arrangeDims(isVertical:Boolean=false, isHorizontal:Boolean=true ) {
        if(isHorizontal) autoDimHorizontal(0)
        if(isVertical){
            dimVerticalA = autoDimVertical(0)
            dimVerticalB = autoDimVertical(1)
            dimVerticalC = autoDimVertical(2)
        }

        setDimPath(dimHeight)
        setDimPoint()
    }

    val SIDE_SOKUTEN = 4
    var flagDimArrangeA = Flags()
    var flagDimArrangeB = Flags()
    var flagDimArrangeC = Flags()

    fun setDimPath(ts: Float = dimHeight) {
        dimHeight = ts
        path[0] = PathAndOffset(scale_, pointAB, point[0], dimVerticalA, dimHorizontalA, dimHeight)
        path[1] = PathAndOffset(scale_, pointBC, pointAB, dimVerticalB, dimHorizontalB, dimHeight)
        path[2] = PathAndOffset(scale_, point[0], pointBC, dimVerticalC, dimHorizontalC, dimHeight)
        pathS_ = PathAndOffset(scale_, pointAB, point[0], SIDE_SOKUTEN, getunconnectedSide(1, -1), dimHeight)
    }

    fun getunconnectedSide(outerright: Int, outerleft: Int): Int{
        if (nodeTriangleC == null) return outerright
        return outerleft
    }

    fun autoDimHorizontal(selfside_index: Int ) {
            makeflags_dim_distances(selfside_index).forEachIndexed { targetside_index, result ->
            if ( result == true ) outerDimHorizontal(targetside_index)
        }
    }

    val BORDERDISTANCE = 0.5f
    val SELFDISTANCE = 0f
    fun makeflags_dim_distances(selfside: Int ): List<Boolean> {
        return dimpoints[selfside].distancesTo(dimpoints).map { it < BORDERDISTANCE && it > SELFDISTANCE }
    }

    //つかうの難しい？
    fun outerDimHorizontal(targetindex: Int){
        val OUTERRIGHT = 3
        val OUTERLEFT = 4
        when (targetindex) {
            SIDEA -> dimHorizontalA = getunconnectedSide(OUTERRIGHT,OUTERLEFT)
            SIDEC -> {
                if(!flagDimArrangeC.isMovedByUser )
                    dimHorizontalC = getunconnectedSide(OUTERRIGHT,OUTERLEFT)
                    flagDimArrangeC.isAutoAligned = true
                    return
            }
            SIDEB -> {
                if(!flagDimArrangeB.isMovedByUser )
                    dimHorizontalB = getunconnectedSide(OUTERRIGHT,OUTERLEFT)
                    flagDimArrangeB.isAutoAligned = true
                    return
            }
            
        }
    }


    fun controllDimHorizontal(side: Int) {
        when (side) {
            SIDEA -> dimHorizontalA = cycleIncrement(dimHorizontalA)
            SIDEB -> {
                dimHorizontalB = cycleIncrement(dimHorizontalB)
                flagDimArrangeB.isMovedByUser = true
            }
            SIDEC -> {
                dimHorizontalC = cycleIncrement(dimHorizontalC)
                flagDimArrangeC.isMovedByUser = true
            }
            SIDE_SOKUTEN -> nameHorizontal = cycleIncrement(nameHorizontal)
        }
        setDimPath(dimHeight)
    }

    fun cycleIncrement(num: Int, max: Int = 4): Int = (num + 1) % (max + 1)

    //dimVertical
    //夾角の、1:外 　3:内
    val OUTER = 1
    val INNER = 3

    val SIDEA = 0
    val SIDEB = 1
    val SIDEC = 2

    // カスタムゲッターでも、呼ばれている
    fun autoDimVertical(side: Int): Int {
        when(side){
            SIDEA -> {
                if(!flagDimArrangeA.isMovedByUser ) if(connectionType < 3) return OUTER
                return dimVerticalA
            }
            SIDEB -> {
                if(!flagDimArrangeB.isMovedByUser ) return autoDimVerticalByAreaCompare(nodeTriangleB)
                return dimVerticalB
            }
            SIDEC -> {
                if(!flagDimArrangeC.isMovedByUser ) return autoDimVerticalByAreaCompare(nodeTriangleC)
                return dimVerticalC
            }
        }

        return OUTER
    }

    // 特殊接続でなければ面積の大きい側に寸法値を配置する
    fun autoDimVerticalByAreaCompare(node: Triangle?): Int{
        if(node==null) return OUTER

        if(node.getArea() > this.getArea() && node.connectionType < 3 )  return OUTER

        return INNER
    }

    fun flipOneToThree(num: Int): Int {
        if (num == OUTER) return INNER
        if (num == INNER) return OUTER
        return num
    }

    // 自動処理の中で呼ばない。
    fun controllDimVertical(side: Int): Int {
        if (side == 0) {
            dimVerticalA = flipOneToThree(dimVerticalA)
            flagDimArrangeA.isMovedByUser = true
            return dimVerticalA
        }
        if (side == 1) {
            dimVerticalB = flipOneToThree(dimVerticalB)
            isChangeDimAlignB_ = true
            flagDimArrangeB.isMovedByUser = true
            return dimVerticalB
        }
        if (side == 2) {
            dimVerticalC = flipOneToThree(dimVerticalC)
            isChangeDimAlignC_ = true
            flagDimArrangeC.isMovedByUser = true
            return dimVerticalC
        }
        if (side == 4) nameAlign_ = flipOneToThree(nameAlign_)
        setDimPath(dimHeight)
        return nameAlign_
    }

    fun setDimAlignByChild() {
        if (!isChangeDimAlignB_) {
            dimVerticalB = if (nodeTriangleB == null) OUTER else INNER
        }
        if (!isChangeDimAlignC_) {
            dimVerticalC = if (nodeTriangleC == null) OUTER else INNER
        }
    }

    fun setDimAligns(sa: Int, sb: Int, sc: Int, ha: Int, hb: Int, hc: Int) {
        dimHorizontalA = sa
        dimHorizontalB = sb
        dimHorizontalC = sc
        dimVerticalA = ha
        dimVerticalB = hb
        dimVerticalC = hc
    }

    fun setDimPoint() {
        dimpoints[0] =
            path[0].pointD //dimSideRotation( dimSideAlignA_, point[0].calcMidPoint(pointAB_), pointAB_, point[0]);
        dimpoints[1] =
            path[1].pointD //dimSideRotation( dimSideAlignB_, pointAB_.calcMidPoint(pointBC_), pointBC_, pointAB_);
        dimpoints[2] =
            path[2].pointD //dimSideRotation( dimSideAlignC_, pointBC_.calcMidPoint(point[0]), point[0], pointBC_);
    }

    //endregion dimalign


    // region pointNumber

    val WEIGHT = 35f

    fun arrangeNumber(isUse: Boolean = false ): PointXY{
        if(flags.isMovedByUser || flags.isAutoAligned ) return pointnumber
        if(!isUse) return weightedMidpoint(WEIGHT)
        return autoAlignPointNumber()
    }


    private fun autoAlignPointNumber() : PointXY{
        if (getArea() <= 4f && (lengthAforce_ < 1.5f || lengthBforce_ < 1.5f || lengthCforce_ < 1.5f)){
            pointnumber = pointcenter
            flags.isAutoAligned = true
            return pointUnconnectedSide(pointcenter, 0.8f )
                //.offset(pointcenter, pointcenter.lengthTo(pointnumber)*-10f )
        }

        return weightedMidpoint(WEIGHT)
    }

    //Deductionからも呼ばれている
    fun pointUnconnectedSide(
        point: PointXY,
        clockwise: Float
    ): PointXY {
        if (nodeTriangleC == null)
            return point.mirroredAndScaledPoint(this.point[0], pointBC, clockwise)
        if (nodeTriangleB == null)
            return point.mirroredAndScaledPoint(pointAB, pointBC, clockwise)

        return weightedMidpoint(WEIGHT)
    }

    fun angleUnconnectedSide(): Float{
        if (nodeTriangleC == null) return -angleMmCA
        return -angleMpAB
    }

    fun setPointNumberMovedByUser_(p: PointXY) {
        if( p.lengthTo(pointcenter) < 10f ) return // あまり遠い時はスルー
        pointnumber = p
        flags.isMovedByUser = true
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
        val p1 = pointAB
        val p2 = pointBC
        val p3 = point[0]

        // 重み付き座標の計算
        val weightedX = p1.x * weight1 + p2.x * weight2 + p3.x * weight3
        val weightedY = p1.y * weight1 + p2.y * weight2 + p3.y * weight3
        return PointXY(weightedX, weightedY)
    }

    //endregion pointNumber

    // 各辺に接続されている Triangle オブジェクトの識別子を返す
    fun toStrings(ispoints: Boolean = true, islength: Boolean = true, isnode: Boolean = true, isalign: Boolean = true ): String {
        val connectedTriangles = arrayOfNulls<Triangle>(3) // 各辺に接続された Triangle オブジェクトを保持する配列
        connectedTriangles[0] = nodeTriangleA_
        connectedTriangles[1] = nodeTriangleB
        connectedTriangles[2] = nodeTriangleC
        val sb = StringBuilder()
        sb.append("Triangle ${mynumber}         hash:${System.identityHashCode(this)} valid:${isValidLengthes()}\n")

        for (i in connectedTriangles.indices) {
            if (connectedTriangles[i] != null && isnode ) {
                sb.append( "connected node to${i} hash:${System.identityHashCode(connectedTriangles[i])} \n")
            }
        }
        if(isnode)   sb.append("connection parameters to0 side${cParam_.side} type${cParam_.type} lcr${cParam_.lcr} \n")
        if(ispoints) sb.append("points:${point[0]} ${pointAB} ${pointBC} \n")
        if(ispoints) sb.append("pointDim:${dimpoints[0]} ${dimpoints[1]} ${dimpoints[2]} \n")
        if(islength) sb.append("length:${lengthA_} ${lengthB_} ${lengthC_} \n")
        if(isalign) sb.append("isPointnumber_user: ${flags.isMovedByUser}\n")
        if(isalign) sb.append("isPointnumber_auto: ${flags.isAutoAligned}\n\n")
        return sb.toString()
    }

    //region Parameters
    var valid_ = false
    var length = FloatArray(3)
    var lengthNotSized = FloatArray(3)
    var scale_ = 1f
    var angle = 180f
    var angleInLocal_ = 0f
    var dedcount = 0f
    var sla_ = ""
    var slb_ = ""
    var slc_ = ""

    var point = arrayOf(PointXY(0f,0f),PointXY(0f,0f),PointXY(0f,0f))
        private set
    var pointAB: PointXY = PointXY(0f, 0f)
        private set
    var pointBC: PointXY = PointXY(0f, 0f)
        private set
    var pointcenter = PointXY(0f, 0f)//autoAlignPointNumber();
        private set
    var pointName_ = PointXY(0f, 0f)
        private set

    var pointnumber = PointXY(0f, 0f)

    // 固定長配列の初期化
    var dimpoints: Array<PointXY> = Array(3) { PointXY(0f, 0f) } // すべての要素を PointXY(0f, 0f) で初期化
    var path: Array<PathAndOffset> = Array(3) { PathAndOffset() }

    var nameAlign_ = 0
    var nameHorizontal = 0
    protected var theta = 0.0
    protected var alpha = 0.0
    var angleCA = 0f
    var angleAB = 0f
    var angleBC = 0f
    var parentnumber = -1 // 0:root
    var connectionType = -1 // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
    var connectionType_ = 0 // 0:sameByParent, 1:differentLength, 2:floatAndDifferent
    var connectionLCR_ = 2 // 0:L 1:C 2:R
    var cParam_ = ConnParam(0, 0, 2, 0f)
    var mynumber = 1
    var dimVerticalA = 3
    var dimVerticalB = 1
    var dimVerticalC = 1
    var isChangeDimAlignB_ = false
    var isChangeDimAlignC_ = false
    var dimHorizontalA = 0
    var dimHorizontalB = 0
    var dimHorizontalC = 0
    var lastTapSide_ = -1
    var color_ = 4
    var childSide_ = 0
    var name = ""
    var myBP_ = Bounds(0f, 0f, 0f, 0f)
    var pathS_: PathAndOffset? = null // = PathAndOffset();
    var dimHeight = 0f
    var nodeTriangleB: Triangle? = null
    var nodeTriangleC: Triangle? = null
    var nodeTriangleA_: Triangle? = null

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
        return PointXY(pointAB)
    }

    fun pointBC_(): PointXY {
        return PointXY(pointBC)
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
        pointAB = PointXY(0.0f, 0.0f)
        pointBC = PointXY(0.0f, 0.0f)
        pointcenter = PointXY(0.0f, 0.0f)
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

    //var isPointnumber = BooleanArray(2){false}

    data class Flags( var isMovedByUser: Boolean = false, var isAutoAligned: Boolean = false )
    var flags = Flags()

    override fun clone(): Triangle {
        val b = Triangle()
        try {
            b.flags = flags.copy()
            b.flagDimArrangeA = flagDimArrangeA.copy()
            b.flagDimArrangeB = flagDimArrangeB.copy()
            b.flagDimArrangeC = flagDimArrangeC.copy()

            b.length = length.copyOf(length.size)
            b.lengthNotSized = lengthNotSized.copyOf(lengthNotSized.size)
            b.dimpoints = cloneArray(dimpoints) // 代入だと参照になるので要素ごとにクローン
            b.path = cloneArray( path )

            //b.isPointnumber = isPointnumber.copyOf(isPointnumber.size)
            b.angle = angle
            b.name = name
            b.mynumber = mynumber
            b.connectionType = connectionType
            b.parentnumber = parentnumber

            b.dimVerticalA = dimVerticalA
            b.dimVerticalB = dimVerticalB
            b.dimVerticalC = dimVerticalC
            b.dimHorizontalA = dimHorizontalA
            b.dimHorizontalB = dimHorizontalB
            b.dimHorizontalC = dimHorizontalC
            //b.point = point.clone()
            b.point[0] = point[0].clone()
            b.pointAB = pointAB.clone()
            b.pointBC = pointBC.clone()
            b.pointcenter = pointcenter.clone()
            b.pointnumber = pointnumber.clone()
            b.cParam_ = cParam_.clone()
            b.isFloating_ = isFloating_
            b.isColored_ = isColored_
            b.myBP_.left = myBP_.left
            b.myBP_.top = myBP_.top
            b.myBP_.right = myBP_.right
            b.myBP_.bottom = myBP_.bottom
            b.nodeTriangleA_ = nodeTriangleA_
            b.nodeTriangleB = nodeTriangleB
            b.nodeTriangleC = nodeTriangleC
            b.childSide_ = childSide_
            b.color_ = color_
            b.connectionLCR_ = connectionLCR_
            b.connectionType_ = connectionType_
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
        if(A <= 0.0 ) return
        calcPoints(point[0], angle)
        //myDimAlign_ = autoSetDimAlign();
    }

    //for first triangle.
    internal constructor(A: Float, B: Float, C: Float, pCA: PointXY, angle: Float) {
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
        name = dP.name
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
        return if (nodeTriangleA_ == null) PointXY(0f, 0f)
        else when (pct) {
            1 -> getParentPointByLCR(pbc, lcr)
            2 -> getParentPointByLCR(pbc, lcr).crossOffset(
                nodeTriangleA_!!.getPointByBackSide(pbc)!!,
                -1.0f
            )

            else -> {
                nodeTriangleA_!!.getPointBySide(pbc)
                getParentPointByLCR(pbc, lcr)

                //getParentPointByLCR(pbc, lcr).crossOffset(
                //    nodeTriangleA_!!.getPointByBackSide(pbc)!!,
                //    -1.0f
                //)
            }
        }
    }

    fun getParentPointByLCR(pbc: Int, lcr: Int): PointXY {
        if (nodeTriangleA_ == null) return PointXY(0f, 0f)
        when (pbc) {
            1 -> when (lcr) {
                0 -> return nodeTriangleA_!!.pointAB.offset(
                    nodeTriangleA_!!.pointBC,
                    length[0]
                )

                1 -> return getParentOffsetPointBySide(pbc)
                2 -> return nodeTriangleA_!!.pointBC.clone()
            }

            2 -> when (lcr) {
                0 -> return nodeTriangleA_!!.pointBC.offset(
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
            1 -> nodeTriangleB!!
            2 -> nodeTriangleC!!
            -1 -> this
            else -> this
        }
    }

    fun getParentOffsetPointBySide(pbc: Int): PointXY {
        return if (nodeTriangleA_ == null) PointXY(0f, 0f) else when (pbc) {
            1 -> nodeTriangleA_!!.pointAB.offset(
                nodeTriangleA_!!.pointBC, nodeTriangleA_!!.lengthB * 0.5f + length[0] * 0.5f
            )

            2 -> nodeTriangleA_!!.pointBC.offset(
                nodeTriangleA_!!.point[0],
                nodeTriangleA_!!.lengthC * 0.5f + length[0] * 0.5f
            )

            else -> nodeTriangleA_!!.getPointBySide(pbc)!!
        }
    }

    override fun getParams(): Params {
        return Params(
            name,
            "",
            mynumber,
            length[0],
            length[1],
            length[2],
            parentnumber,
            connectionType,
            point[0],
            pointcenter
        )
    }

    fun myName_(): String {
        return name
    }

    fun getTapLength(tapP: PointXY, rangeRadius: Float): Int {
        setDimPoint()
        val range = rangeRadius * scale_
        if (tapP.nearBy(pointName_, range)) return 4.also { lastTapSide_ = it }
        if (tapP.nearBy(dimpoints[0], range)) return 0.also { lastTapSide_ = it }
        if (tapP.nearBy(dimpoints[1], range)) return 1.also { lastTapSide_ = it }
        if (tapP.nearBy(dimpoints[2], range)) return 2.also { lastTapSide_ = it }
        return if (tapP.nearBy(pointnumber, range)) 3.also {
            lastTapSide_ = it
        } else -1.also {
            lastTapSide_ = it
        }
    }

    val dimAlignA: Int
        get() = autoDimVertical(0)

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
        get() = autoDimVertical(1)

    /*        if( getAngleMpAB() <= 450f || getAngleMpAB() >= 270f ||
                 getAngleMpAB() <= 90f || getAngleMpAB() >= -90f ) {
             if( childSide_ == 3 || childSide_ == 4 ) return myDimAlignB = 3;
             if( lengthB*scale_ > 1.5f ) return myDimAlignB = 3;
             else return myDimAlignB = 1;
         }

         if( childSide_ == 3 || childSide_ == 4 ) return myDimAlignB = 1;
         return  myDimAlignB = 3;*/
    val dimAlignC: Int
        get() = autoDimVertical(2)

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
        return PointXY(pointcenter)
    }



    fun collision(): Boolean {
        return true
    }

    private fun setBoundaryBox() {
        val lb: PointXY
        lb = pointAB.min(pointBC)
        myBP_.left = lb.x
        myBP_.bottom = lb.y
        val rt: PointXY
        rt = pointAB.max(pointBC)
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
        return if (getSideByIndex(i) == "C") point[0] else null
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
            val i = connectionType
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
            connectionType =
                if (pbc == 3) 4 else if (pbc == 4) 3 else if (pbc == 5) 6 else if (pbc == 6) 5 else pbc
        }
        if (BtoC) {
            connectionType =
                if (pbc == 3) 6 else if (pbc == 4) 5 else if (pbc == 5) 4 else if (pbc == 6) 3 else if (pbc == 9) 10 else if (pbc == 10) 9 else -pbc + 3
        }
    }

    fun setParentBCFromCLCR() {
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        if (cParam_.type == 2) return
        when (cParam_.lcr) {
            0 -> when (cParam_.side) {
                1 -> connectionType = 4
                2 -> connectionType = 6
            }

            1 -> when (cParam_.side) {
                1 -> connectionType = 7
                2 -> connectionType = 8
            }

            2 -> when (cParam_.side) {
                1 -> connectionType = 3
                2 -> connectionType = 5
            }
        }
    }

    fun setOn(parent: Triangle?, pbc: Int, B: Float, C: Float): Triangle {
        //myNumber_ = parent.myNumber_ + 1;
        connectionType = pbc
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
                connectionType = 1
                length[0] = nodeTriangleA_!!.lengthNotSized[1]
                point[0] = nodeTriangleA_!!.pointBC_()
                angle = nodeTriangleA_!!.angleMpAB
            }
            2 -> {
                connectionType = 2
                length[0] = nodeTriangleA_!!.lengthNotSized[2]
                point[0] = nodeTriangleA_!!.pointCA_
                angle = nodeTriangleA_!!.angleMmCA
            }
            else -> {
                connectionType = 0
                length[0] = 0f
                lengthNotSized[0] = 0f
                point[0] = PointXY(0f, 0f)
                angle = 180f
            }
        }
        parentnumber = nodeTriangleA_!!.mynumber
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
        connectionType = pbc
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
                connectionType = 1
                angle = parent.angleMpAB
            }
            2 -> { // C
                point[0] = getParentPointByType(cParam_)
                connectionType = 2
                angle = parent.angleMmCA
            }
            3 -> { // B-R
                connectionType = 3
                angle = parent.angleMpAB
            }
            4 -> { //B-L
                connectionType = 4
                angle = parent.angleMpAB
            }
            5 -> { //C-R
                connectionType = 5
                angle = parent.angleMmCA
            }
            6 -> { //C-L
                connectionType = 6
                angle = parent.angleMmCA
            }
            7 -> { //B-Center
                connectionType = 7
                angle = parent.angleMpAB
            }
            8 -> { //C-Center
                connectionType = 8
                angle = parent.angleMmCA
            }
            9 -> { //B-Float-R
                connectionType = 9
                angle = parent.angleMpAB
            }
            10 -> { //C-Float-R
                connectionType = 10
                angle = parent.angleMmCA
            }
            else -> {
                connectionType = 0
                length[0] = 0f
                lengthNotSized[0] = 0f
                point[0] = PointXY(0f, 0f)
                angle = 180f
            }
        }
        parentnumber = parent.mynumber
        initBasicArguments(length[0], B, C, point[0], angle)
        if (!isValid) return null
        calcPoints(point[0], angle)
        if (connectionType == 4) {
            val vector = PointXY(
                parent.pointAB_().x - pointAB.x,
                parent.pointAB_().y - pointAB.y
            )
            move(vector)
        }
        if (connectionType == 6) {
            val vector = PointXY(
                parent.pointBC_().x - pointAB.x,
                parent.pointBC_().y - pointAB.y
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


    fun setNode(node: Triangle?, side: Int) {
        var side = side
        if (node == null) return
        if (side > 2) side = parentSide
        when (side) {
            -1 -> {}
            0 -> {
                nodeTriangleA_ = node
                if (node === nodeTriangleB) nodeTriangleB = null
                if (node === nodeTriangleC) nodeTriangleC = null
            }

            1 -> {
                nodeTriangleB = node
                if (node === nodeTriangleA_) nodeTriangleA_ = null
                if (node === nodeTriangleC) nodeTriangleC = null
            }

            2 -> {
                nodeTriangleC = node
                if (node === nodeTriangleB) nodeTriangleB = null
                if (node === nodeTriangleA_) nodeTriangleA_ = null
            }
        }
    }

    fun setNumber(num: Int) {
        mynumber = num
    }

    fun setColor(num: Int) {
        color_ = num
        isColored_ = true
    }

    fun setChild(newchild: Triangle, cbc: Int) {
        childSide_ = cbc
        if (newchild.getPbc(cbc) == 1) {
            nodeTriangleB = newchild
            isChildB_ = true
        }
        if (newchild.getPbc(cbc) == 2) {
            nodeTriangleC = newchild
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
        val cParam = ConnParam(pbc, pct, lcr, lengthA_)
        return setBasePoint(cParam)
    }

    fun reset(newTri: Triangle) {
        val thisCP = cParam_.clone()
        if (nodeTriangleA_ == null || parentnumber < 1) {
            angle = newTri.angle
            angleInLocal_ = newTri.angleInLocal_
            resetLength(newTri.length[0], newTri.length[1], newTri.length[2])
        } else setOn(
            nodeTriangleA_,
            newTri.connectionType,
            newTri.length[0],
            newTri.length[1],
            newTri.length[2]
        )
        cParam_ = thisCP.clone()
        name = newTri.name
        clone()
    }

    fun reset(newTri: Triangle, cParam: ConnParam): Triangle {
        if (nodeTriangleA_ == null) resetLength(
            newTri.length[0],
            newTri.length[1],
            newTri.length[2]
        ) else setOn(nodeTriangleA_, cParam, newTri.length[1], newTri.length[2])
        name = newTri.name
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
        val cbc = myChild.connectionType
        if (cbc == 1 && !isValidLengthes(length[0], myChild.length[0], length[2])) return
        if (cbc == 2 && !isValidLengthes(length[0], length[1], myChild.length[0])) return
        childSide_ = myChild.connectionType
        if (nodeTriangleA_ == null || parentnumber < 1) {
            if (cbc == 1) resetLength(length[0], myChild.length[0], length[2])
            if (cbc == 2) resetLength(length[0], length[1], myChild.length[0])
            return
        }
        if (cbc == 1) {
            setOn(nodeTriangleA_, connectionType, length[0], myChild.length[0], length[2])
            //nodeTriangleB_ = myChild;
        }
        if (cbc == 2) {
            setOn(nodeTriangleA_, connectionType, length[0], length[1], myChild.length[0])
            //nodeTriangleC_ = myChild;
        }
    }

    fun setConnectionType(cParam: ConnParam) {
        //myParentBC_= cParam.getSide();
        parentnumber = nodeTriangleA_!!.mynumber
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
        connectionType = prm.pl
        parentnumber = prm.pn
        if (nodeTriangleA_ == null || parentnumber < 1) resetLength(prm.a, prm.b, prm.c) else {
            setOn(nodeTriangleA_, cParam_, prm.b, prm.c)
        }
        //set(parent_, tParams.getPl(), tParams.getA(), tParams.getB(), tParams.getC() );
        //cParam_ = thisCP.clone();
        name = prm.name
    }

    fun resetElegant(prm: Params) {
        reset(prm)
        if (nodeTriangleA_ != null) nodeTriangleA_!!.resetByNode(parentSide)
    }

    fun resetByNode(pbc: Int) {
        val node = getNode(pbc)
        var lengthConnected = getLengthByIndex(pbc)
        if (node.connectionType < 3) lengthConnected = node.length[0]
        when (pbc) {
            0 -> {}
            1 -> initBasicArguments(
                length[0],
                lengthConnected,
                length[2],
                node.pointBC,
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

    //region node and boundaries
    fun removeNode(target: Triangle) {
        if (nodeTriangleA_ === target) nodeTriangleA_ = null
        if (nodeTriangleB === target) nodeTriangleB = null
        if (nodeTriangleC === target) nodeTriangleC = null
    }

    fun expandBoundaries(listBound: Bounds): Bounds {
        setBoundaryBox()
        val newB = Bounds(myBP_.left, myBP_.top, myBP_.right, myBP_.bottom)
        // 境界を比較し、広い方に置き換える
        if (myBP_.bottom > listBound.bottom) newB.bottom = listBound.bottom
        if (myBP_.top < listBound.top) newB.top = listBound.top
        if (myBP_.left > listBound.left) newB.left = listBound.left
        if (myBP_.right < listBound.right) newB.right = listBound.right
        return newB
    }

    //endregion node and boundaries

    //region calculater

    private fun calculatePointBC(basepoint: PointXY): PointXY {
        val theta = atan2( (basepoint.y - pointAB.y).toDouble(), (basepoint.x - pointAB.x).toDouble() )
        val powA = length[0].pow(2.0f).toDouble()
        val powB = length[1].pow(2.0f).toDouble()
        val powC = length[2].pow(2.0f).toDouble()
        val alpha = acos((powA + powB - powC) / (2 * length[0] * length[1]))
        val angle = theta + alpha
        val offset_x = ( length[1] * cos( angle ) ).toFloat()
        val offset_y = ( length[1] * sin( angle ) ).toFloat()
        return pointAB.plus( offset_x, offset_y )
    }

    private fun calculateInternalAngles() {
        angleAB = calculateInternalAngle(point[0], pointAB, pointBC).toFloat()
        angleBC = calculateInternalAngle(pointAB, pointBC, point[0]).toFloat()
        angleCA = calculateInternalAngle(pointBC, point[0], pointAB).toFloat()
    }

    fun calculateInternalAngle(p1: PointXY, p2: PointXY, p3: PointXY): Double {
        val v1 = p1.subtract(p2)
        val v2 = p3.subtract(p2)
        val angleRadian = acos(v1.innerProduct(v2) / (v1.magnitude() * v2.magnitude()))
        return angleRadian * 180 / Math.PI
    }

    private fun calculatePointCenter() :PointXY{
        val averageX = ( pointAB.x + pointBC.x + point[0].x ) / 3
        val averageY = ( pointAB.y + pointBC.y + point[0].y ) / 3
        pointcenter = PointXY( averageX, averageY )
        return pointcenter
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
                plist = arrayOf(point[0], pointAB, pointBC)
                llist = floatArrayOf(length[0], length[1], length[2])
                powlist = floatArrayOf(length[0].pow(2.0f), length[1].pow(2.0f), length[2].pow(2.0f))
            }

            1 -> {
                angle = nodeTriangleB!!.angle + 180f //- nodeTriangleB_.angleInnerCA_;
                plist = arrayOf(nodeTriangleB!!.pointAB, pointBC, point[0])
                llist = floatArrayOf(length[1], length[2], length[0])
                powlist = floatArrayOf(length[1].pow(2.0f), length[2].pow(2.0f), length[0].pow(2.0f))
                pointAB = plist[0]!!
            }

            2 -> {
                angle = nodeTriangleC!!.angle + 180f //- nodeTriangleB_.angleInnerCA_;
                plist = arrayOf(nodeTriangleC!!.pointAB, point[0], pointAB)
                llist = floatArrayOf(length[2], length[0], length[1])
                powlist = floatArrayOf(length[2].pow(2.0f), length[0].pow(2.0f), length[1].pow(2.0f))
                pointBC = plist[0]!!
            }

            else -> throw IllegalStateException("Unexpected value: $refside")
        }
        plist[1]!![(plist[0]!!.x + llist[0] * cos(Math.toRadians(angle.toDouble()))).toFloat()] =
            (plist[0]!!.y + llist[0] * sin(
                Math.toRadians(angle.toDouble())
            )).toFloat()
        theta = atan2(
            (plist[0]!!.y - plist[1]!!.y).toDouble(),
            (plist[0]!!.x - plist[1]!!.x).toDouble()
        )
        alpha = acos((powlist[0] + powlist[1] - powlist[2]) / (2 * llist[0] * llist[1])).toDouble()
        plist[2]!![(plist[1]!!.x + llist[1] * cos(theta + alpha)).toFloat()] =
            (plist[1]!!.y + llist[1] * sin(theta + alpha)).toFloat()
        calculateInternalAngles()
        if (refside == 1) this.angle = nodeTriangleB!!.angle - angleCA
        if (refside == 2) this.angle = nodeTriangleC!!.angle + angleCA
    }

    //endregion calcurator



    //region old hataage method
    fun hataage(p: PointXY, offset: Float, axisY: Float, number: Float): PointXY {
        val distanceFromCenter = p.y - pointcenter.y * axisY
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
        val pABy = pointAB.y * axisY
        val pBCy = pointBC.y * axisY
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
    //endregion old hataage method

    //region scale and translate
    fun scale(basepoint: PointXY, scale: Float) {
        scale_ *= scale
        //pointAB.scale(basepoint, scale);
        //pointBC.scale(basepoint, scale);
        point[0].scale(basepoint, scale)
        pointcenter.scale(basepoint, scale)
        pointnumber.scale(basepoint, scale)
        length[0] *= scale
        length[1] *= scale
        length[2] *= scale
        calcPoints( point[0], angle, true )
    }

    fun move(to: PointXY) {
        pointAB.add(to)
        pointBC.add(to)
        point[0].add(to)
        pointcenter = pointcenter.plus(to)
        pointnumber = pointnumber.plus(to)
        dimpoints[0].add(to)
        dimpoints[1].add(to)
        dimpoints[2].add(to)
        myBP_.left = myBP_.left + to.x
        myBP_.right = myBP_.right + to.x
        myBP_.top = myBP_.top + to.y
        myBP_.bottom = myBP_.bottom + to.x
        path[0].move(to)
        path[1].move(to)
        path[2].move(to)
        pathS_!!.move(to)
    }

    // endregion scale and translate

    //region rotater
    fun rotate(basepoint: PointXY, addDegree: Float, recover: Boolean = false ) {
        if (connectionType < 9 && recover ) return
        if (!recover) angleInLocal_ += addDegree else angleInLocal_ = addDegree

        //val allpoints = arrayOf(*point, *dimpoint, pointcenter, pointCA_, pointAB, pointBC)
        //allpoints.map { it.rotate(basepoint, addDegree) }

        point[0] = point[0].rotate(basepoint, addDegree)
        angle += addDegree
        calcPoints(point[0], angle)
        //setDimPath(dimHeight)

    }


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
            point[0] = pointAB
            pointAB = pointBC
            pointBC = pp.clone()
            pf = angleCA
            angleCA = angleAB
            angleAB = angleBC
            angleBC = pf
            angle = angleMmCA - angleAB
            if (angle < 0) angle += 360f
            if (angle > 360) angle -= 360f
            pp = dimpoints[0].clone()
            dimpoints[0] = dimpoints[1]
            dimpoints[1] = dimpoints[2]
            dimpoints[2] = pp.clone()
            pi = dimVerticalA
            dimVerticalA = dimVerticalB
            dimVerticalB = dimVerticalC
            dimVerticalC = pi
            pi = dimHorizontalA
            dimHorizontalA = dimHorizontalB
            dimHorizontalB = dimHorizontalC
            dimHorizontalC = pi
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
            point[0] = pointBC
            pointBC = pointAB
            pointAB = pp.clone()
            pf = angleCA
            angleCA = angleBC
            angleBC = angleAB
            angleAB = pf
            angle += angleCA + angleBC
            if (angle < 0) angle += 360f
            if (angle > 360) angle -= 360f
            pp = dimpoints[0].clone()
            dimpoints[0] = dimpoints[2]
            dimpoints[2] = dimpoints[1]
            dimpoints[1] = pp.clone()
            pi = dimVerticalA
            dimVerticalA = dimVerticalC
            dimVerticalC = dimVerticalB
            dimVerticalB = pi
            pi = dimHorizontalA
            dimHorizontalA = dimHorizontalC
            dimHorizontalC = dimHorizontalB
            dimHorizontalB = pi
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


    //endregion rotater

    //region isBoolean
    fun alreadyHaveChild(pbc: Int): Boolean {
        if (pbc < 1) return false
        return if (getSideByIndex(pbc) == "B" && isChildB_) true else getSideByIndex(pbc) == "C" && isChildC_
    }

    fun hasChildIn(cbc: Int): Boolean {
        return if ((nodeTriangleB != null || isChildB_) && cbc == 1) true else (nodeTriangleC != null || isChildC_) && cbc == 2
    }

    fun hasConstantParent(): Boolean {
        val iscons = mynumber - parentnumber
        return iscons <= 1
    }

    fun hasChild(): Boolean {
        return nodeTriangleB != null || nodeTriangleC != null
    }

    val isValid: Boolean
        get() = if (length[0] <= 0.0f || length[1] <= 0.0f || length[2] <= 0.0f) false else isValidLengthes(
            length[0],
            length[1],
            length[2]
        )

    fun isValidLengthes(A: Float=lengthA_, B: Float=lengthB_, C: Float=lengthC_): Boolean {
        return !(A + B <= C) && !(B + C <= A) && !(C + A <= B)
    }

    val isFloating: Boolean
        get() {
            isFloating_ = nodeTriangleA_ != null && connectionType > 8
            return isFloating_
        }
    val isColored: Boolean
        get() {
            isColored_ = nodeTriangleA_ != null && color_ != nodeTriangleA_!!.color_
            return isColored_
        }
    val isCollide: Boolean = false

    fun isCollide(p: PointXY): Boolean {
        return p.isCollide(pointAB, pointBC, point[0])
    }

//endregion　isIt


}

fun Triangle.setMyName_(name: String) {
    this.name = name
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
