package com.jpaver.trianglelist

import com.jpaver.trianglelist.util.Cloneable
import com.jpaver.trianglelist.util.InputParameter
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

@Suppress("NAME_SHADOWING")
class Triangle : EditObject, Cloneable<Triangle> {

    var dim = Dims(this)

    override fun clone(): Triangle {
        val b = Triangle()
        try {

            b.scaleFactror = scaleFactror
            b.pointNumber = pointNumber.clone()
            b.dimpoint = dimpoint.copy()//cloneArray(dimpoints) // 代入だと参照になるので要素ごとにクローン
            b.dimOnPath = dimOnPath.copyOf()
            b.pathS = pathS.copy()
            b.dim = dim.clone()

            b.dimVerticalA = dimVerticalA
            b.dimVerticalB = dimVerticalB
            b.dimVerticalC = dimVerticalC
            b.dimHorizontalA = dimHorizontalA
            b.dimHorizontalB = dimHorizontalB
            b.dimHorizontalC = dimHorizontalC

            b.length = length.copyOf(length.size)
            b.lengthNotSized = lengthNotSized.copyOf(lengthNotSized.size)
            b.angle = angle
            b.name = name
            b.mynumber = mynumber
            b.connectionSide = connectionSide
            b.parentnumber = parentnumber

            //b.point = point.clone()
            b.point[0] = point[0].clone()
            b.pointAB = pointAB.clone()
            b.pointBC = pointBC.clone()
            b.pointcenter = pointcenter.clone()
            b.pointnumber = pointnumber.clone()
            b.cParam_ = cParam_.clone()
            b.isFloating = isFloating
            b.isColored = isColored
            b.myBP_.left = myBP_.left
            b.myBP_.top = myBP_.top
            b.myBP_.right = myBP_.right
            b.myBP_.bottom = myBP_.bottom
            b.nodeA = nodeA
            b.nodeB = nodeB
            b.nodeC = nodeC
            b.childSide_ = childSide_
            b.mycolor = mycolor
            b.connectionLCR_ = connectionLCR_
            b.connectionType_ = connectionType_
            b.strLengthA = strLengthA
            b.strLengthB = strLengthB
            b.strLengthC = strLengthC
        } catch (e: Exception) {
            //e.printStackTrace();
        }
        return b
    }

    //region dimAlign

    fun setLengthStr(){
        strLengthA = lengthNotSized[0].formattedString(2)
        strLengthB = lengthNotSized[1].formattedString(2)
        strLengthC = lengthNotSized[2].formattedString(2)
    }

    // あらゆる場所でよばれる
    private fun arrangeDims(isVertical:Boolean=false, isHorizontal:Boolean=true ) {
        dim.arrangeDims(isVertical,isHorizontal)
        setDimPath(dim.height)
        setDimPoint()
    }

    val SIDE_SOKUTEN = 4

    fun setDimPath(ts: Float = dimHeight) {
        dimHeight = ts
        dimOnPath[0] = DimOnPath(scaleFactror, pointAB, point[0], dim.vertical.a, dim.horizontal.a, ts )
        dimOnPath[1] = DimOnPath(scaleFactror, pointBC, pointAB, dim.vertical.b, dim.horizontal.b, ts )
        dimOnPath[2] = DimOnPath(scaleFactror, point[0], pointBC,  dim.vertical.c, dim.horizontal.c, ts )
        pathS = DimOnPath(scaleFactror, pointAB, point[0], SIDE_SOKUTEN, dim.horizontal.s, ts )
    }

    fun controlDimHorizontal(side: Int) {
        dim.controlHorizontal(side)
        setDimPath()
        setDimPoint()
    }

    //dimVertical
    // 自動処理の中で呼ばない。
    fun controlDimVertical(side: Int) {
        dim.controlVertical(side)
        setDimPath()
        setDimPoint()
    }

    fun setDimAlignByChild() {
        dim.setAlignByChild()
    }

    fun setDimAligns(sa: Int, sb: Int, sc: Int, ha: Int, hb: Int, hc: Int) {
        dim.setAligns(sa,sb,sc,ha,hb,hc)
    }

    fun setDimPoint() {
        //dimpoints.forEachIndexed { i, path -> dimpoints[i] = path[i].pointD }
        dimpoint.a =
            dimOnPath[0].dimpoint
        dimpoint.b =
            dimOnPath[1].dimpoint
        dimpoint.c =
            dimOnPath[2].dimpoint
        dimpoint.s =
            pathS.dimpoint
    }

    //endregion dimalign

    // region pointNumber

    var pointnumber = PointXY(0f, 0f)

    init{
        pointnumber = PointXY(0f,0f)
    }

    var pointNumber = PointNumberManager()


    //Deductionからも呼ばれている
    fun pointUnconnectedSide(
        point: PointXY,
        clockwise: Float
    ): PointXY {
        if (nodeC == null)
            return point.mirroredAndScaledPoint(this.point[0], pointBC, clockwise)
        if (nodeB == null)
            return point.mirroredAndScaledPoint(pointAB, pointBC, clockwise)

        return point
    }

    fun angleUnconnectedSide(): Float{
        if (nodeC == null) return -angleMmCA
        return -angleMpAB
    }

    fun setPointNumber(p: PointXY, is_user:Boolean = true ) {
        pointnumber = pointNumber.setPointByUser(p, this, is_user )
    }
    //endregion pointNumber

    // 各辺に接続されている Triangle オブジェクトの識別子を返す
    fun toStrings(
        ispoints: Boolean = true,
        islength: Boolean = true,
        isnode: Boolean = true,
        isalign: Boolean = true
    ): String {
        val connectedTriangles = arrayOfNulls<Triangle>(3) // 各辺に接続された Triangle オブジェクトを保持する配列
        connectedTriangles[0] = nodeA
        connectedTriangles[1] = nodeB
        connectedTriangles[2] = nodeC
        val sb = StringBuilder()
        sb.append("Triangle ${mynumber}         hash:${System.identityHashCode(this)} valid:${isValidLengthes()}\n")

        for (i in connectedTriangles.indices) {
            if (connectedTriangles[i] != null && isnode ) {
                sb.append( "connected node to${i} hash:${System.identityHashCode(connectedTriangles[i])} \n")
            }
        }
        if(isnode)   sb.append("connection parameters to0 side${cParam_.side} type${cParam_.type} lcr${cParam_.lcr} \n")
        if(ispoints) sb.append("points:${point[0]} ${pointAB} ${pointBC} \n")
        if(ispoints) sb.append("pointDim:${dimpoint.a} ${dimpoint.b} ${dimpoint.c} \n")
        if(islength) sb.append("length:${lengthA_} ${lengthB_} ${lengthC_} \n")
        if(isalign) sb.append("isPointnumber_user: ${pointNumber.flag.isMovedByUser}\n")
        if(isalign) sb.append("isPointnumber_auto: ${pointNumber.flag.isAutoAligned}\n")
        if(isalign) sb.append("pointnumber: ${pointnumber}\n\n")
        return sb.toString()
    }

    //region Parameters
    val angleMmCA: Float
        get() = angle - angleCA
    val angleMpAB: Float
        get() = angle + angleAB

    var valid_ = false
    var length = FloatArray(3)
    var lengthNotSized = FloatArray(3)
    var scaleFactror = 1f
    var angle = 180f
    var angleInLocal_ = 0f
    var dedcount = 0f
    var strLengthA = ""
    var strLengthB = ""
    var strLengthC = ""

    var point = arrayOf(PointXY(0f,0f),PointXY(0f,0f),PointXY(0f,0f))
        private set
    var pointAB: PointXY = PointXY(0f, 0f)
        private set
    var pointBC: PointXY = PointXY(0f, 0f)
        private set
    var pointcenter = PointXY(0f, 0f)//autoAlignPointNumber();
        private set

    var dimOnPath: Array<DimOnPath> = Array(3) { DimOnPath() }
    data class Dimpoint(var a:PointXY= PointXY(0f,0f), var b:PointXY=PointXY(0f,0f), var c:PointXY=PointXY(0f,0f), var s:PointXY=PointXY(0f,0f),var name:PointXY=PointXY(0f,0f)){
        fun toArray(): Array<PointXY>{
            return arrayOf(a,b,c)
        }
    }

    // 固定長配列の初期化
    var dimpoint = Dimpoint()//: Array<PointXY> = Array(3) { PointXY(0f, 0f) } // すべての要素を PointXY(0f, 0f) で初期化

    var nameAlign_ = 0
    protected var theta = 0.0
    protected var alpha = 0.0
    var angleCA = 0f
    var angleAB = 0f
    var angleBC = 0f
    var parentnumber = -1 // 0:root
    var connectionSide = -1 // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
    var connectionType_ = 0 // 0:sameByParent, 1:differentLength, 2:floatAndDifferent
    var connectionLCR_ = 2 // 0:L 1:C 2:R
    var cParam_ = ConnParam(0, 0, 2, 0f)
    var mynumber = 1
    var dimVerticalA = 3
    var dimVerticalB = 1
    var dimVerticalC = 1
    var dimHorizontalA = 0
    var dimHorizontalB = 0
    var dimHorizontalC = 0
    var lastTapSide_ = -1
    var mycolor = 4
    var childSide_ = 0
    var name = ""
    var myBP_ = Bounds(0f, 0f, 0f, 0f)
    var pathS = DimOnPath()
    var dimHeight = 0f

    var nodeA: Triangle? = null
    var nodeB: Triangle? = null
    var nodeC: Triangle? = null

    var isFloating = false
    var isColored = false

    val lengthA_: Float
        get() = length[0]
    val lengthB_: Float
        get() = length[1]
    val lengthC_: Float
        get() = length[2]
    val lengthAforce: Float
        get() = lengthNotSized[0]
    val lengthBforce: Float
        get() = lengthNotSized[1]
    val lengthCforce: Float
        get() = lengthNotSized[2]
    val pointCA: PointXY
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

    internal constructor()

    internal constructor(A: Float, B: Float, C: Float) {
        setNumber(1)
        point[0] = PointXY(0f, 0f)
        angle = 180f
        initBasicArguments(A, B, C, point[0], angle)
        if(A <= 0.0 ) return
        calcPoints(point[0], angle)
        //myDimAlign_ = autoSetDimAlign()
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

    internal constructor(myParent: Triangle?, dP: InputParameter) {
        setOn(myParent, dP.pl, dP.a, dP.b, dP.c)
        name = dP.name
    }

    internal constructor(dP: InputParameter, angle: Float) {
        mynumber = dP.number
        name = dP.name
        initBasicArguments(dP.a, dP.b, dP.c, dP.point, angle)
        calcPoints(dP.point, angle)
    }
    //endregion constructor

    //region getter
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
        return if (nodeA == null) PointXY(0f, 0f)
        else when (pct) {
            1 -> getParentPointByLCR(pbc, lcr)
            2 -> getParentPointByLCR(pbc, lcr).crossOffset(
                nodeA!!.getPointByBackSide(pbc)!!,
                -1.0f
            )

            else -> {
                nodeA!!.getPointBySide(pbc)
                getParentPointByLCR(pbc, lcr)

                //getParentPointByLCR(pbc, lcr).crossOffset(
                //    nodeTriangleA_!!.getPointByBackSide(pbc)!!,
                //    -1.0f
                //)
            }
        }
    }

    fun getParentPointByLCR(pbc: Int, lcr: Int): PointXY {
        if (nodeA == null) return PointXY(0f, 0f)
        when (pbc) {
            1 -> when (lcr) {
                0 -> return nodeA!!.pointAB.offset(
                    nodeA!!.pointBC,
                    length[0]
                )

                1 -> return getParentOffsetPointBySide(pbc)
                2 -> return nodeA!!.pointBC.clone()
            }

            2 -> when (lcr) {
                0 -> return nodeA!!.pointBC.offset(
                    nodeA!!.point[0],
                    length[0]
                )

                1 -> return getParentOffsetPointBySide(pbc)
                2 -> return nodeA!!.point[0].clone()
            }
        }
        return PointXY(0f, 0f)
    }

    fun getNode(pbc: Int): Triangle {
        return when (pbc) {
            0 -> nodeA!!
            1 -> nodeB!!
            2 -> nodeC!!
            -1 -> this
            else -> this
        }
    }

    fun getParentOffsetPointBySide(pbc: Int): PointXY {
        return if (nodeA == null) PointXY(0f, 0f) else when (pbc) {
            1 -> nodeA!!.pointAB.offset(
                nodeA!!.pointBC, nodeA!!.lengthB * 0.5f + length[0] * 0.5f
            )

            2 -> nodeA!!.pointBC.offset(
                nodeA!!.point[0],
                nodeA!!.lengthC * 0.5f + length[0] * 0.5f
            )

            else -> nodeA!!.getPointBySide(pbc)!!
        }
    }

    override fun getParams(): InputParameter {
        return InputParameter(
            name,
            "",
            mynumber,
            length[0],
            length[1],
            length[2],
            parentnumber,
            connectionSide,
            point[0],
            pointcenter
        )
    }

    fun myName_(): String {
        return name
    }

    fun getTapLength(tapP: PointXY, rangeRadius: Float): Int {
        setDimPoint()
        val range = rangeRadius * scaleFactror
        return when {
            tapP.nearBy(dimpoint.s, range) -> 4
            tapP.nearBy(dimpoint.a, range) -> 0
            tapP.nearBy(dimpoint.b, range) -> 1
            tapP.nearBy(dimpoint.c, range) -> 2
            tapP.nearBy(pointnumber, range) -> 3
            else -> -1
        }.also { lastTapSide_ = it }
    }

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

    fun getForceLength(i: Int): Float {
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
            val i = connectionSide
            if (i == 1 || i == 3 || i == 4 || i == 7 || i == 9) return 1
            return if (i == 2 || i == 5 || i == 6 || i == 8 || i == 10) 2 else 0
        }

    fun getPbc(pbc: Int): Int {
        if (pbc == 1 || pbc == 3 || pbc == 4 || pbc == 7 || pbc == 9) return 1
        return if (pbc == 2 || pbc == 5 || pbc == 6 || pbc == 8 || pbc == 10) 2 else 0
    }

    //endregion

    //region setter
    fun resetNode(prms: InputParameter, parent: Triangle?, doneObjectList: ArrayList<Triangle>) {

        // 接続情報の変更、とりあえず挿入処理は考慮しない、すでに他のノードがあるときは上書きする。
        nodeA!!.removeNode(this)
        nodeA = parent
        nodeA!!.setNode(this, prms.pl)
        reset(prms)
        doneObjectList.add(this)
    }

    fun setScale(scale_: Float) {
        scaleFactror = scale_
        length[0] *= scale_
        length[1] *= scale_
        length[2] *= scale_
        calcPoints(point[0], angle)
    }

    fun setReverseDefSide(pbc: Int, BtoC: Boolean) {
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        if (!BtoC) {
            connectionSide =
                if (pbc == 3) 4 else if (pbc == 4) 3 else if (pbc == 5) 6 else if (pbc == 6) 5 else pbc
        }
        if (BtoC) {
            connectionSide =
                if (pbc == 3) 6 else if (pbc == 4) 5 else if (pbc == 5) 4 else if (pbc == 6) 3 else if (pbc == 9) 10 else if (pbc == 10) 9 else -pbc + 3
        }
    }

    fun setParentBCFromCLCR() {
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        if (cParam_.type == 2) return
        when (cParam_.lcr) {
            0 -> when (cParam_.side) {
                1 -> connectionSide = 4
                2 -> connectionSide = 6
            }

            1 -> when (cParam_.side) {
                1 -> connectionSide = 7
                2 -> connectionSide = 8
            }

            2 -> when (cParam_.side) {
                1 -> connectionSide = 3
                2 -> connectionSide = 5
            }
        }
    }

    fun setOn(parent: Triangle?, pbc: Int, B: Float, C: Float): Triangle {
        //myNumber_ = parent.myNumber_ + 1;
        connectionSide = pbc
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
                connectionSide = 1
                length[0] = nodeA!!.lengthNotSized[1]
                point[0] = nodeA!!.pointBC_()
                angle = nodeA!!.angleMpAB
            }
            2 -> {
                connectionSide = 2
                length[0] = nodeA!!.lengthNotSized[2]
                point[0] = nodeA!!.pointCA
                angle = nodeA!!.angleMmCA
            }
            else -> {
                connectionSide = 0
                length[0] = 0f
                lengthNotSized[0] = 0f
                point[0] = PointXY(0f, 0f)
                angle = 180f
            }
        }
        parentnumber = nodeA!!.mynumber
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
        connectionSide = pbc
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
                connectionSide = 1
                angle = parent.angleMpAB
            }
            2 -> { // C
                point[0] = getParentPointByType(cParam_)
                connectionSide = 2
                angle = parent.angleMmCA
            }
            3 -> { // B-R
                connectionSide = 3
                angle = parent.angleMpAB
            }
            4 -> { //B-L
                connectionSide = 4
                angle = parent.angleMpAB
            }
            5 -> { //C-R
                connectionSide = 5
                angle = parent.angleMmCA
            }
            6 -> { //C-L
                connectionSide = 6
                angle = parent.angleMmCA
            }
            7 -> { //B-Center
                connectionSide = 7
                angle = parent.angleMpAB
            }
            8 -> { //C-Center
                connectionSide = 8
                angle = parent.angleMmCA
            }
            9 -> { //B-Float-R
                connectionSide = 9
                angle = parent.angleMpAB
            }
            10 -> { //C-Float-R
                connectionSide = 10
                angle = parent.angleMmCA
            }
            else -> {
                connectionSide = 0
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
        if (connectionSide == 4) {
            val vector = PointXY(
                parent.pointAB_().x - pointAB.x,
                parent.pointAB_().y - pointAB.y
            )
            move(vector)
        }
        if (connectionSide == 6) {
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
                nodeA = node
                if (node === nodeB) nodeB = null
                if (node === nodeC) nodeC = null
            }

            1 -> {
                nodeB = node
                if (node === nodeA) nodeA = null
                if (node === nodeC) nodeC = null
            }

            2 -> {
                nodeC = node
                if (node === nodeB) nodeB = null
                if (node === nodeA) nodeA = null
            }
        }
    }

    fun setNumber(num: Int) {
        mynumber = num
    }

    fun setColor(num: Int) {
        mycolor = num
        isColored = true
    }

    fun setChild(newchild: Triangle, cbc: Int) {
        childSide_ = cbc
        if (newchild.getPbc(cbc) == 1) {
            nodeB = newchild
        }
        if (newchild.getPbc(cbc) == 2) {
            nodeC = newchild
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
        if (nodeA == null || parentnumber < 1) {
            angle = newTri.angle
            angleInLocal_ = newTri.angleInLocal_
            resetLength(newTri.length[0], newTri.length[1], newTri.length[2])
        } else setOn(
            nodeA,
            newTri.connectionSide,
            newTri.length[0],
            newTri.length[1],
            newTri.length[2]
        )
        cParam_ = thisCP.clone()
        name = newTri.name
        clone()
    }

    fun reset(newTri: Triangle, cParam: ConnParam): Triangle {
        if (nodeA == null) resetLength(
            newTri.length[0],
            newTri.length[1],
            newTri.length[2]
        ) else setOn(nodeA, cParam, newTri.length[1], newTri.length[2])
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
        val cbc = myChild.connectionSide
        if (cbc == 1 && !isValidLengthes(length[0], myChild.length[0], length[2])) return
        if (cbc == 2 && !isValidLengthes(length[0], length[1], myChild.length[0])) return
        childSide_ = myChild.connectionSide
        if (nodeA == null || parentnumber < 1) {
            if (cbc == 1) resetLength(length[0], myChild.length[0], length[2])
            if (cbc == 2) resetLength(length[0], length[1], myChild.length[0])
            return
        }
        if (cbc == 1) {
            setOn(nodeA, connectionSide, length[0], myChild.length[0], length[2])
            //nodeTriangleB_ = myChild;
        }
        if (cbc == 2) {
            setOn(nodeA, connectionSide, length[0], length[1], myChild.length[0])
            //nodeTriangleC_ = myChild;
        }
    }

    fun setConnectionType(cParam: ConnParam) {
        //myParentBC_= cParam.getSide();
        parentnumber = nodeA!!.mynumber
        connectionType_ = cParam.type
        connectionLCR_ = cParam.lcr
        cParam_ = cParam.clone()
        when (cParam.side) {
            1 -> angle = nodeA!!.angleMpAB
            2 -> angle = nodeA!!.angleMmCA
        }
        if (cParam.type == 0) {
            if (cParam.lenA != 0.0f) {
                length[0] = cParam.lenA
                lengthNotSized[0] = cParam.lenA
                point[0] = getParentPointByType(cParam.side, cParam.type, cParam.lcr)
            } else {
                length[0] = nodeA!!.getLengthByIndex(cParam.side)
                lengthNotSized[0] = nodeA!!.getLengthByIndex(cParam.side)
                point[0] = nodeA!!.getPointByCParam(cParam, nodeA)!!
            }
        } else {
            if (cParam.lenA != 0.0f) {
                length[0] = cParam.lenA
                lengthNotSized[0] = cParam.lenA
            }
            point[0] = getParentPointByType(cParam.side, cParam.type, cParam.lcr)
        }
    }

    fun reset(prm: InputParameter) {
        //ConneParam thisCP = cParam_.clone();
        length[0] = prm.a
        lengthNotSized[0] = prm.a
        setCParamFromParentBC(prm.pl)
        connectionSide = prm.pl
        parentnumber = prm.pn
        if (nodeA == null || parentnumber < 1) resetLength(prm.a, prm.b, prm.c) else {
            setOn(nodeA, cParam_, prm.b, prm.c)
        }
        //set(parent_, tParams.getPl(), tParams.getA(), tParams.getB(), tParams.getC() );
        //cParam_ = thisCP.clone();
        name = prm.name
    }

    fun resetElegant(prm: InputParameter) {
        reset(prm)
        if (nodeA != null) nodeA!!.resetByNode(parentSide)
    }

    fun resetByNode(pbc: Int) {
        val node = getNode(pbc)
        var lengthConnected = getLengthByIndex(pbc)
        if (node.connectionSide < 3) lengthConnected = node.length[0]
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
        if (nodeA === target) nodeA = null
        if (nodeB === target) nodeB = null
        if (nodeC === target) nodeC = null
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
    fun calcPoints(basepoint: PointXY = point[0], _angle: Float = angle, isArrangeDims: Boolean = false) {
        pointAB = basepoint.offset(length[0], _angle)
        pointBC = calculatePointBC( basepoint )
        calculateInternalAngles()
        calculatePointCenter()
        arrangeDims(isArrangeDims)
        if(!pointNumber.flag.isMovedByUser) pointnumber = pointcenter
        setBoundaryBox()
    }

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
                angle = nodeB!!.angle + 180f //- nodeTriangleB_.angleInnerCA_;
                plist = arrayOf(nodeB!!.pointAB, pointBC, point[0])
                llist = floatArrayOf(length[1], length[2], length[0])
                powlist = floatArrayOf(length[1].pow(2.0f), length[2].pow(2.0f), length[0].pow(2.0f))
                pointAB = plist[0]!!
            }

            2 -> {
                angle = nodeC!!.angle + 180f //- nodeTriangleB_.angleInnerCA_;
                plist = arrayOf(nodeC!!.pointAB, point[0], pointAB)
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
        if (refside == 1) this.angle = nodeB!!.angle - angleCA
        if (refside == 2) this.angle = nodeC!!.angle + angleCA
    }

    //endregion calcurator

    //region scale and translate
    fun scale(basepoint: PointXY, scale_: Float, isArrangeDims: Boolean = false) {
        scaleFactror = scaleFactror * scale_
        point[0].change_scale(basepoint, scale_)
        length[0] *= scale_
        length[1] *= scale_
        length[2] *= scale_
        pointcenter.change_scale(basepoint, scale_)
        pointnumber.change_scale(basepoint, scale_)
        calcPoints(point[0], angle, isArrangeDims)
    }

    fun move(to: PointXY) {
        pointAB = pointAB.plus(to)
        pointBC = pointBC.plus(to)
        point[0]= point[0].plus(to)
        pointcenter = pointcenter.plus(to)
        pointnumber = pointnumber.plus(to)
        dimpoint.a.add(to)
        dimpoint.b.add(to)
        dimpoint.c.add(to)
        dimpoint.s.add(to)
        myBP_.left = myBP_.left + to.x
        myBP_.right = myBP_.right + to.x
        myBP_.top = myBP_.top + to.y
        myBP_.bottom = myBP_.bottom + to.x
        dimOnPath[0].move(to)
        dimOnPath[1].move(to)
        dimOnPath[2].move(to)
        pathS.move(to)
    }

    // endregion scale and translate

    //region rotater
    fun control_rotate(basepoint: PointXY, addDegree: Float) {
        angleInLocal_ += addDegree

        rotate_body(basepoint,addDegree)

        pointnumber = pointnumber.rotate(basepoint,addDegree)
    }

    fun recover_rotate(basepoint: PointXY, addDegree: Float){
        angleInLocal_ = addDegree

        rotate_body(basepoint,addDegree)

        if (!pointNumber.flag.isMovedByUser) pointnumber = pointnumber.rotate(basepoint,addDegree)
    }

    fun rotate_body(basepoint: PointXY, addDegree: Float){
        point[0] = point[0].rotate(basepoint, addDegree)
        angle += addDegree
        calcPoints(point[0], angle)

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
            pp = dimpoint.a.clone()
            dimpoint.a = dimpoint.b
            dimpoint.b = dimpoint.c
            dimpoint.c = pp.clone()
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
            pp = dimpoint.a.clone()
            dimpoint.b = dimpoint.c
            dimpoint.c = dimpoint.b
            dimpoint.b = pp.clone()
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
        when(pbc){
            1 -> if(nodeB!=null) return true
            2 -> if(nodeC!=null) return true
        }
        return false
    }

    fun hasChildIn(cbc: Int): Boolean {
        return if ((nodeB != null) && cbc == 1) true else (nodeC != null) && cbc == 2
    }

    fun hasConstantParent(): Boolean {
        val iscons = mynumber - parentnumber
        return iscons <= 1
    }

    fun hasChild(): Boolean {
        return nodeB != null || nodeC != null
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

    val getIsFloating: Boolean
        get() {
            isFloating = nodeA != null && connectionSide > 8
            return isFloating
        }

    //isColoredという名前にしていたら、プロパティ名と重複していて、意図せずtrueを返すという不思議なバグを生んでいたので修正
    //ユーザーが色を変更する以外に、こうやって隣と違う色になっているときにフラグを立てるのは、同色の三角形どうしの外枠を作るため
    val getIsColored: Boolean
        get() {
            if( nodeA == null ) return isColored
            isColored = mycolor != nodeA!!.mycolor
            return isColored
        }

    val isCollide: Boolean = false

    fun isCollide(p: PointXY): Boolean {
        return p.isCollide(pointAB, pointBC, point[0])
    }

//endregion　isIt


}
