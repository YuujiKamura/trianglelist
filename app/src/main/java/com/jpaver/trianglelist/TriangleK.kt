package com.jpaver.trianglelist

import java.util.*

class TriangleK(var lengthA_ :Float = 0f,
                var lengthB_ :Float = 0f,
                var lengthC_ :Float = 0f,
                var nodeTriangleA_: TriangleK? = null,
                private var parentBC :Int = -1,
                var pointCA_ :PointXY = PointXY(0f, 0f),
                var angle :Float = 0f
) : EditObject(), Cloneable {

    //for first triangle.
    constructor(A: Float, B: Float, C: Float, pCA: PointXY, angle_: Float) :this(
            lengthA_ = A,
            lengthB_ = B,
            lengthC_ = C,
            nodeTriangleA_ = null,
            parentBC = -1,
            pointCA_ = pCA,
            angle = angle_
    ){
        calcPoints( null, -1, pointCA_, angle )
    }

    //for first. most simple.
    constructor(A: Float, B: Float, C: Float) :this(
        lengthA_ = A,
        lengthB_ = B,
        lengthC_ = C,
        nodeTriangleA_ = null,
        parentBC = -1,
        pointCA_ = PointXY( 0f, 0f ),
        angle = 0f
    ){
        calcPoints( null, -1, pointCA_, angle )
    }

    // use node A to my parent.
    constructor(parent: TriangleK, pbc: Int, B: Float, C: Float) :this(
        lengthA_ = parent.getLengthBySide(pbc),
        lengthB_ = B,
        lengthC_ = C,
        nodeTriangleA_ = parent,
        parentBC = pbc,
        pointCA_ = parent.getPointBySide(pbc),
        angle = parent.getAngleBySide(pbc)
            ){
        calcPoints( parent, 0 )
        //set(parent, pbc, B, C)
    }

    // use node A to my parent. A uses different length.
    constructor(myParent: TriangleK, pbc: Int, A: Float, B: Float, C: Float) :this(
        lengthA_ = A,
        lengthB_ = B,
        lengthC_ = C,
        nodeTriangleA_ = myParent,
        parentBC = pbc,
        pointCA_ = myParent.getPointBySide( pbc ),
        angle = myParent.getAngleBySide( pbc )
    ){
        //calcPoints( myParent, 0 )
        set(myParent, pbc, A, B, C)
    }

    // reference node B or C to set base point.
    constructor(child: TriangleK, A: Float, B: Float, C: Float) :this(
        lengthA_ = A,
        lengthB_ = B,
        lengthC_ = C,
        nodeTriangleA_ = null,
        parentBC = -1,
        pointCA_ = PointXY( 0f, 0f ),
        angle = 0f
    ) {
        calcPoints( child, child.parentSide)
    }

    // connection parameter
    constructor(myParent: TriangleK, cParam: ConnParam, B: Float, C: Float) :this(
        lengthA_ = myParent.getLengthBySide( cParam.side ),
        lengthB_ = B,
        lengthC_ = C,
        nodeTriangleA_ = myParent,
        parentBC = getPbcByCParam( cParam ),
        pointCA_ = myParent.getParentPointByType( cParam ),
        angle = myParent.getAngleBySide( cParam.side )
    ){
        set(myParent, cParam, B, C)
    }

    constructor(myParent: TriangleK, dP: Params) :this(
        lengthA_ = dP.a,
        lengthB_ = dP.b,
        lengthC_ = dP.c,
        nodeTriangleA_ = myParent,
        parentBC = dP.pl,
        pointCA_ = myParent.getPointBySide( dP.pl ),
        angle = myParent.getAngleBySide( dP.pl )
    ) {
        set(myParent, dP.pl, dP.a, dP.b, dP.c)
        myName_ = dP.name
        autoSetDimSideAlign()
    }

    constructor(dP: Params, angle: Float) :this(
        lengthA_ = dP.a,
        lengthB_ = dP.b,
        lengthC_ = dP.c,
        nodeTriangleA_ = null,
        parentBC = -1,
        pointCA_ = dP.pt,
        angle = angle
    ) {
        setNumber(dP.n)
        myName_ = dP.name
        initBasicArguments(dP.a, dP.b, dP.c, dP.pt, angle)
        calcPoints(dP.pt, angle)
    }


    var valid_ = false
    var scale_ = 1f
    var lengthAforce_ = 0f
    var lengthBforce_ = 0f
    var lengthCforce_ = 0f
    var sla_ = ""
    var slb_ = ""
    var slc_ = ""
    // base point by calc
    var pointAB_ = PointXY(0f, 0f)
    var pointBC_ = PointXY(0f, 0f)
    var pointCenter_ = PointXY(0f, 0f)
    var pointNumber_ = PointXY(0f, 0f)
    var isPointNumberMoved_ = false
    var dimPointA_ = PointXY(0f, 0f)
    var dimPointB_ = PointXY(0f, 0f)
    var dimPointC_ = PointXY(0f, 0f)
    var pointName_ = PointXY(0f, 0f)
    var nameAlign_ = 0
    var nameSideAlign_ = 0
    var myTheta_ = 0.0
    var myAlpha_ = 0.0
    var myPowA_ = 0.0
    var myPowB_ = 0.0
    var myPowC_ = 0.0

    var angleCA = 0f
    var angleAB = 0f
    var angleBC = 0f
    var dimAngleB_ = 0f
    var dimAngleC_ = 0f
    var parentNumber = -1 // 0:root

    // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
    var connectionType_ = 0 // 0:sameByParent, 1:differentLength, 2:floatAndDifferent
    var connectionLCR_ = 2 // 0:L 1:C 2:R
    var cParam_ = ConnParam(0, 0, 2, 0f)

    var myNumber_ = 1

    var myDimAlign_ = 0
    var myDimAlignA_ = 3
    var myDimAlignB_ = 3
    var myDimAlignC_ = 3
    var isChangeDimAlignA_ = false
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
    var pathA_ // = PathAndOffset();
            : PathAndOffset? = null
    var pathB_ // = PathAndOffset();
            : PathAndOffset? = null
    var pathC_ // = PathAndOffset();
            : PathAndOffset? = null
    var pathS_ // = PathAndOffset();
            : PathAndOffset? = null
    var dimH_ = 0f
    var nodeTriangleB_: TriangleK? = null
    var nodeTriangleC_: TriangleK? = null
    var isChildB_ = false
    var isChildC_ = false


    public override fun clone(): TriangleK {
        val b = TriangleK()
        try {
            //b = super.clone()
            b.lengthA_ = lengthA_
            b.lengthB_ = lengthB_
            b.lengthC_ = lengthC_
            b.lengthAforce_ = lengthAforce_
            b.lengthBforce_ = lengthBforce_
            b.lengthCforce_ = lengthCforce_
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
            b.pointCA_ = pointCA_.clone()
            b.pointAB_ = pointAB_.clone()
            b.pointBC_ = pointBC_.clone()
            b.pointCenter_ = pointCenter_.clone()
            b.pointNumber_ = pointNumber_.clone()
            b.isPointNumberMoved_ = isPointNumberMoved_
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
            b.cParam_ = cParam_
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return b
    }

    private fun setNode(node: TriangleK?, side_: Int) {
        var side = side_
        if (node == null) return
        if (side > 2) side = parentSide
        when (side) {
            -1 -> {
            }
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

    // set argument methods
    private fun initBasicArguments(A: Float, B: Float, C: Float, pCA: PointXY?, angle: Float) {
        lengthA_ = A
        lengthB_ = B
        lengthC_ = C
        lengthAforce_ = A
        lengthBforce_ = B
        lengthCforce_ = C
        valid_ = validTriangle()
        pointCA_ = PointXY(pCA!!.x, pCA.y)
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

    operator fun set(parent: TriangleK, pbc: Int, B: Float, C: Float): TriangleK {
        //myNumber_ = parent.myNumber_ + 1;
        parentBC = pbc
        setNode(parent, 0)
        parent.setNode(this, parentSide)

        //setParent(parent, A);
        when( pbc ) {
            1 -> {
                parentBC = 1
                lengthA_ = nodeTriangleA_!!.lengthB_
                lengthAforce_ = nodeTriangleA_!!.lengthBforce_
                pointCA_ = nodeTriangleA_!!.pointBC_
                angle = nodeTriangleA_!!.angleMpAB
            }
            2 -> {
                parentBC = 2
                lengthA_ = nodeTriangleA_!!.lengthC_
                lengthAforce_ = nodeTriangleA_!!.lengthCforce_
                pointCA_ = nodeTriangleA_!!.pointCA_
                angle = nodeTriangleA_!!.angleMmCA
            }
            else -> {
                parentBC = 0
                lengthA_ = 0f
                lengthAforce_ = 0f
                pointCA_ = PointXY(0f, 0f)
                angle = 180f
            }
        }

        parentNumber = nodeTriangleA_!!.myNumber_
        //nodeTriangleA_.setChild(this, parentBC_);
        initBasicArguments(lengthA_, B, C, pointCA_, angle)
        calcPoints(pointCA_, angle)

        //myDimAlign = setDimAlign();
        return this
    }

    operator fun set(
        parent: TriangleK,
        pbc: Int,
        A: Float,
        B: Float,
        C: Float,
        byNode: Boolean
    ): TriangleK {
        set(parent, pbc, A, B, C)
        if (byNode) {
            parent.resetByNode(pbc)
        }
        if (nodeTriangleB_ != null) nodeTriangleB_!![this, nodeTriangleB_!!.parentBC, getLengthBySide(
            nodeTriangleB_!!.parentSide
        ), nodeTriangleB_!!.lengthB_] = nodeTriangleB_!!.lengthC_
        if (nodeTriangleC_ != null) nodeTriangleC_!![this, nodeTriangleC_!!.parentBC, getLengthBySide(
            nodeTriangleC_!!.parentSide
        ), nodeTriangleC_!!.lengthB_] = nodeTriangleC_!!.lengthC_
        return this
    }

    operator fun set(parent: TriangleK, pbc: Int, A: Float, B: Float, C: Float): TriangleK? {
        //myNumber_ = parent.myNumber_ + 1;
        parentBC = pbc
        setNode(parent, 0)
        parent.setNode(this, parentSide)

        //setParent(parent, pbc);
        //nodeTriangleA_.setChild(this, pbc );

        // if user rewrite A
        if (A != parent.getLengthBySide(pbc)) {
            lengthA_ = A
            lengthAforce_ = A
        } else {
            lengthA_ = parent.getLengthBySide(pbc)
            lengthAforce_ = parent.getLengthBySide(pbc)
        }
        setCParamFromParentBC(pbc)
        pointCA_ = getParentPointByType(cParam_)
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
                lengthA_ = 0f
                lengthAforce_ = 0f
                pointCA_ = PointXY(0f, 0f)
                angle = 180f
            }
        }
        parentNumber = parent.myNumber_
        initBasicArguments(lengthA_, B, C, pointCA_, angle)
        if (!validTriangle()) return null
        calcPoints(pointCA_, angle)
        if (parentBC == 4) {
            val vector = PointXY(
                parent.pointAB_.x - pointAB_.x,
                parent.pointAB_.y - pointAB_.y
            )
            move(vector)
        }
        if (parentBC == 6) {
            val vector = PointXY(
                parent.pointBC_.x - pointAB_.x,
                parent.pointBC_.y - pointAB_.y
            )
            move(vector)
        }

        //myDimAlign = setDimAlign();
        return clone()
    }

    operator fun set(myParent: TriangleK, pbc: Int) {
        this[myParent, pbc, lengthB_] = lengthC_
    }

    operator fun set(myParent: TriangleK, dP: Params) {
        set(myParent, dP.pl, dP.a, dP.b, dP.c)
        myName_ = dP.name
    }

    operator fun set(parent: TriangleK, cParam: ConnParam, B: Float, C: Float): TriangleK? {
        //myNumber_ = parent.myNumber_ + 1;
        //parentBC_ = cParam.getSide();
        setNode(parent, 0)
        parent.setNode(this, cParam.side)

        //setParent( parent, cParam.getSide() );
        angle = nodeTriangleA_!!.getAngleBySide(cParam.side)
        setConnectionType(cParam)
        initBasicArguments(lengthA_, B, C, pointCA_, angle)
        if (!validTriangle()) return null
        calcPoints(pointCA_, angle)
        setDimAlignByChild()

        //nodeTriangleA_.setChild(this, cParam.getSide() );
        return clone()
    }


    fun reset(prm: Params): TriangleK {
        //ConneParam thisCP = cParam_.clone();
        lengthA_ = prm.a
        lengthAforce_ = prm.a
        setCParamFromParentBC(prm.pl)
        parentBC = prm.pl
        parentNumber = prm.pn
        if (nodeTriangleA_ == null || parentNumber < 1) resetLength(prm.a, prm.b, prm.c)
        else {
            set(nodeTriangleA_!!, cParam_, prm.b, prm.c)
        }
        //set(parent_, tParams.getPl(), tParams.getA(), tParams.getB(), tParams.getC() );
        //cParam_ = thisCP.clone();
        myName_ = prm.name
        return this
    }

    fun resetElegant(prm: Params) {
        reset(prm)
        if (nodeTriangleA_ != null) nodeTriangleA_!!.resetByNode(parentSide)
    }

    fun resetByNode(pbc: Int) {
        val node = getNode(pbc)
        if (node != null) {
            var length = getLengthBySide(pbc)
            if (node.parentBC < 3) length = node.lengthA_
            when (pbc) {
                0 -> {
                }
                1 -> initBasicArguments(lengthA_, length, lengthC_, node.pointBC_, -node.angle)
                2 -> initBasicArguments(
                    lengthA_,
                    lengthB_,
                    length,
                    node.pointCA_,
                    node.angle + angleBC
                )
            }
            calcPoints(pointCA_, angle)
        }
    }

    fun resetNode(prms: Params, parent: TriangleK, doneObjectList: ArrayList<TriangleK>) {

        // 接続情報の変更、とりあえず挿入処理は考慮しない、すでに他のノードがあるときは上書きする。
        nodeTriangleA_!!.removeNode(this)
        nodeTriangleA_ = parent
        nodeTriangleA_!!.setNode(this, prms.pl)
        reset(prms)
        doneObjectList.add(this)
    }


    fun reset(newTri: Triangle): TriangleK {
        val thisCP = cParam_.clone()
        if (nodeTriangleA_ == null || parentNumber < 1) resetLength(
            newTri.lengthA_,
            newTri.lengthB_,
            newTri.lengthC_
        ) else set(
            nodeTriangleA_!!,
            newTri.parentBC,
            newTri.lengthA_,
            newTri.lengthB_,
            newTri.lengthC_
        )
        cParam_ = thisCP.clone()
        myName_ = newTri.myName_
        return clone()
    }

    fun reset(newTri: TriangleK, cParam: ConnParam): TriangleK {
        if (nodeTriangleA_ == null) resetLength(
            newTri.lengthA_,
            newTri.lengthB_,
            newTri.lengthC_
        ) else set(nodeTriangleA_!!, cParam, newTri.lengthB_, newTri.lengthC_)
        myName_ = newTri.myName_
        return clone()
    }

    fun resetLength(A: Float, B: Float, C: Float): TriangleK {
        //lengthA = A; lengthB = B; lengthC = C;
        initBasicArguments(A, B, C, pointCA_, angle)
        calcPoints(pointCA_, angle)
        return clone()
    }

    fun resetByParent(prnt: TriangleK, cParam: ConnParam): Boolean {
        if (!isValidLengthes(prnt.getLengthBySide(parentSide), lengthB_, lengthC_)) return false
        val triIsValid = set(prnt, cParam, lengthB_, lengthC_)
        return triIsValid != null
    }

    fun resetByChild(myChild: TriangleK, cParam: ConnParam) {
        val cbc = myChild.cParam_.side
        childSide_ = myChild.parentBC
        if (nodeTriangleA_ == null) {
            if (cbc == 1) resetLength(lengthA_, myChild.lengthA_, lengthC_)
            if (cbc == 2) resetLength(lengthA_, lengthB_, myChild.lengthA_)
            return
        }
        if (cbc == 1) {
            set(nodeTriangleA_!!, cParam, myChild.lengthA_, lengthC_)
            nodeTriangleB_ = myChild.clone()
        }
        if (cbc == 2) {
            set(nodeTriangleA_!!, cParam, lengthB_, myChild.lengthA_)
            nodeTriangleC_ = myChild.clone()
        }
        setDimAlignByChild()
    }

    // reset by parent.
    fun resetByParent(prnt: TriangleK, pbc: Int): Boolean {
        var triIsValid: TriangleK? = null
        val parentLength = prnt.getLengthBySide(pbc)

        //if(pbc == 1 ) triIsValid = set(prnt, pbc, lengthA_, parentLength, );
        if (pbc <= 2) {
            if (!isValidLengthes(parentLength, lengthB_, lengthC_)) {
                triIsValid = set(prnt, pbc, lengthA_, lengthB_, lengthC_)
                return false
            } else triIsValid = set(prnt, pbc, parentLength, lengthB_, lengthC_)
        }
        if (pbc > 2) triIsValid = set(prnt, pbc, lengthA_, lengthB_, lengthC_)
        return triIsValid != null
    }

    // 子のA辺が書き換わったら、それを写し取ってくる。同一辺長接続のとき（１か２）以外はリターン。
    fun resetByChild(myChild: TriangleK) {
        setDimAlignByChild()
        if (myChild.cParam_.type != 0) return
        val cbc = myChild.parentBC
        if (cbc == 1 && !isValidLengthes(lengthA_, myChild.lengthA_, lengthC_)) return
        if (cbc == 2 && !isValidLengthes(lengthA_, lengthB_, myChild.lengthA_)) return
        childSide_ = myChild.parentBC
        if (nodeTriangleA_ == null || parentNumber < 1) {
            if (cbc == 1) resetLength(lengthA_, myChild.lengthA_, lengthC_)
            if (cbc == 2) resetLength(lengthA_, lengthB_, myChild.lengthA_)
            return
        }
        if (cbc == 1) {
            set(nodeTriangleA_!!, parentBC, lengthA_, myChild.lengthA_, lengthC_)
            //nodeTriangleB_ = myChild;
        }
        if (cbc == 2) {
            set(nodeTriangleA_!!, parentBC, lengthA_, lengthB_, myChild.lengthA_)
            //nodeTriangleC_ = myChild;
        }
    }

    fun removeNode(target: TriangleK) {
        if (nodeTriangleA_ === target) nodeTriangleA_ = null
        if (nodeTriangleB_ === target) nodeTriangleB_ = null
        if (nodeTriangleC_ === target) nodeTriangleC_ = null
    }

    fun removeTheirNode() {
        if (nodeTriangleA_ != null) nodeTriangleA_!!.removeNode(this)
        if (nodeTriangleB_ != null) nodeTriangleB_!!.removeNode(this)
        if (nodeTriangleC_ != null) nodeTriangleC_!!.removeNode(this)
    }

    val lengthByType: Float
        get() = 0f

    fun getNode(pbc: Int): TriangleK? {
        when (pbc) {
            0 -> return nodeTriangleA_
            1 -> return nodeTriangleB_
            2 -> return nodeTriangleC_
            -1 -> return this
        }
        return null
    }

    fun getPointByCParam(cparam: ConnParam, prnt: TriangleK): PointXY {
        val cside = cparam.side
        val ctype = cparam.type
        val clcr = cparam.lcr
        //pp.add( getPointBy( pp, lengthA_, clcr ) );
        return getPointBySide(cside)
    }

    fun getPointBy(p: PointXY, la: Float, lcr: Int): PointXY {
        return if (lcr == 2) p else p
        //        if( lcr == 1 ) return p.offset(  );
    }

    fun getParentPointByType( cParam: ConnParam = this.cParam_ ): PointXY {
        return getParentPointByType(cParam.side, cParam.type, cParam.lcr)
    }

    fun getParentPointByType(pbc: Int, conntype: Int, lcr: Int): PointXY {
        return if (nodeTriangleA_ == null) PointXY(0f, 0f)
        else when (conntype) {
            1 -> getParentPointByLCR(pbc)
            2 -> getParentPointByLCR(
                    pbc
            )!!.crossOffset(nodeTriangleA_!!.getPointByBackSide(pbc), -1.0f)
            else -> nodeTriangleA_!!.getPointBySide(pbc)
        }!!
    }

    fun getPointByBackSide(i: Int): PointXY? {
        if (getSideByIndex(i) === "B") return pointAB_
        return if (getSideByIndex(i) === "C") pointBC_ else null
    }

    fun getParentPointByLCR(pbc: Int): PointXY? {
        if (nodeTriangleA_ == null) return PointXY(0f, 0f)
        when (pbc) {
            1 -> when (cParam_.lcr) {
                0 -> return nodeTriangleA_!!.pointAB_.offset(nodeTriangleA_!!.pointBC_, lengthA_)
                1 -> return getParentOffsetPointBySide(pbc)
                2 -> return nodeTriangleA_!!.pointBC_.clone()
            }
            2 -> when (cParam_.lcr) {
                0 -> return nodeTriangleA_!!.pointBC_.offset(nodeTriangleA_!!.pointCA_, lengthA_)
                1 -> return getParentOffsetPointBySide(pbc)
                2 -> return nodeTriangleA_!!.pointCA_.clone()
            }
        }
        return PointXY(0f, 0f)
    }

    fun getParentOffsetPointBySide(pbc: Int): PointXY? {
        if (nodeTriangleA_ == null) return PointXY(0f, 0f)
        when (pbc) {
            1 -> return nodeTriangleA_!!.pointAB_.offset(
                nodeTriangleA_!!.pointBC_,
                nodeTriangleA_!!.lengthB_ * 0.5f + lengthA_ * 0.5f
            )
            2 -> return nodeTriangleA_!!.pointBC_.offset(
                nodeTriangleA_!!.pointCA_,
                nodeTriangleA_!!.lengthC_ * 0.5f + lengthA_ * 0.5f
            )
        }
        return nodeTriangleA_!!.getPointBySide(pbc)
    }

    override fun getArea(): Float {
        setForcelengthes()
        val sumABC = lengthAforce_ + lengthBforce_ + lengthCforce_
        val myArea =
            sumABC * 0.5f * (sumABC * 0.5f - lengthAforce_) * (sumABC * 0.5f - lengthBforce_) * (sumABC * 0.5f - lengthCforce_)
        //myArea = roundByUnderTwo( myArea );
        return roundByUnderTwo(Math.pow(myArea.toDouble(), 0.5).toFloat())
    }

    fun getLengthBySide(i: Int): Float {
        if (i == 1) return lengthB_
        return if (i == 2) lengthC_ else 0f
    }

    fun getPointBySide(i: Int): PointXY {
        if (getSideByIndex(i) === "B") return pointBC_
        return if (getSideByIndex(i) === "C") pointCA_ else PointXY( 0f, 0f )
    }

    fun getAngleBySide(i: Int): Float {
        if (getSideByIndex(i) === "B") return angleMpAB
        return if (getSideByIndex(i) === "C") angleMmCA else 0f
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
    val angleMpBC: Float
        get() = angle + angleBC

    val parent: TriangleK?
        get() = if (nodeTriangleA_ != null) nodeTriangleA_ else null

    val dimAlignA: Int
        get() = calcDimAlignByInnerAngleOf(0, angle)

    val dimAlignB: Int
        get() = calcDimAlignByInnerAngleOf(1, angleMpAB)

    val dimAlignC: Int
        get() = calcDimAlignByInnerAngleOf(2, angleMmCA)

    val pointNumberAutoAligned_: PointXY
        get() {
            if (isPointNumberMoved_ == true) return pointNumber_
            if (lengthAforce_ < 2.5f) return pointNumber_.offset(
                pointBC_,
                pointNumber_.vectorTo(pointBC_).lengthXY() * -0.3f
            )
            if (lengthBforce_ < 2.5f) return pointNumber_.offset(
                pointCA_,
                pointNumber_.vectorTo(pointCA_).lengthXY() * -0.3f
            )
            return if (lengthCforce_ < 2.5f) pointNumber_.offset(
                pointAB_,
                pointNumber_.vectorTo(pointAB_).lengthXY() * -0.3f
            ) else pointNumber_
        }

    override fun getParams(): Params {
        return Params(
            myName_, "", myNumber_, lengthA_, lengthB_, lengthC_,
            parentNumber,
            parentBC, pointCA_, pointCenter_
        )
    }

    fun getTapLength(tapP: PointXY): Int {
        setDimPoint()
        val range = 0.6f * scale_
        if (true == tapP.nearBy(pointName_, range)) return 4.also { lastTapSide_ = it }
        if (true == tapP.nearBy(dimPointA_, range)) return 0.also { lastTapSide_ = it }
        if (true == tapP.nearBy(dimPointB_, range)) return 1.also { lastTapSide_ = it }
        if (true == tapP.nearBy(dimPointC_, range)) return 2.also { lastTapSide_ = it }
        return if (true == tapP.nearBy(pointNumberAutoAligned_, range)) 3.also {
            lastTapSide_ = it
        } else -1.also { lastTapSide_ = it }
    }

    val isFloating: Boolean
        get() = parentBC == 9 || parentBC == 10

    companion object {
        fun getPbcByCParam(cp :ConnParam ) :Int {
            // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC

            when( cp.type ) {
                0 -> return cp.side // 1 or 2
                1 -> { // nijyuudanmen
                    when( cp.side ){
                        1 -> { //B..
                            when( cp.lcr ){
                                0 -> return 4 //BL
                                1 -> return 7 //BC
                                2 -> return 3 //BR
                            }
                        }
                        2 -> { // C..
                            when( cp.lcr ){
                                0 -> return 6 //CL
                                1 -> return 8 //CC
                                2 -> return 5 //CR
                            }
                        }
                    }
                }
                2 -> { // float connection
                    when( cp.side ){
                        1 -> return 9  //FB
                        2 -> return 10 //FC
                    }
                }
            }

            return -1
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
        when (cParam.type) {
            0 -> if (cParam.lenA != 0.0f) {
                lengthA_ = cParam.lenA
                lengthAforce_ = cParam.lenA
                pointCA_ = getParentPointByType(cParam.side, cParam.type, cParam.lcr)!!
            } else {
                lengthA_ = nodeTriangleA_!!.getLengthBySide(cParam.side)
                lengthAforce_ = nodeTriangleA_!!.getLengthBySide(cParam.side)
                pointCA_ = nodeTriangleA_!!.getPointByCParam(cParam, nodeTriangleA_!!)!!
            }
            else -> {
                if (cParam.lenA != 0.0f) {
                    lengthA_ = cParam.lenA
                    lengthAforce_ = cParam.lenA
                }
                pointCA_ = getParentPointByType(cParam.side, cParam.type, cParam.lcr)!!
            }
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

    fun setBasePoint(cParam: ConnParam): PointXY {
        pointCA_ = getParentPointByType(cParam.side, cParam.type, cParam.lcr)
        connectionType_ = cParam.type
        connectionLCR_ = cParam.lcr
        calcPoints(pointCA_, angle)
        return pointCA_
    }

    fun setBasePoint(pbc: Int, pct: Int, lcr: Int): PointXY {
        pointCA_ = getParentPointByType(pbc, pct, lcr)
        connectionType_ = pct
        connectionLCR_ = lcr
        calcPoints(pointCA_, angle)
        return pointCA_
    }

    fun setParent(parent: TriangleK, pbc: Int) {
        nodeTriangleA_ = parent.clone()
        //myParentBC_ = pbc;
    }

    fun setCParamFromParentBC(pbc: Int) {
        var curLCR = cParam_.lcr
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        if (cParam_.side == 0 && (pbc == 4 || pbc == 6)) curLCR = 0
        if (cParam_.side == 0 && (pbc == 7 || pbc == 8)) curLCR = 1
        when (pbc) {
            1 -> cParam_ = ConnParam(1, 0, 2, lengthAforce_)
            2 -> cParam_ = ConnParam(2, 0, 2, lengthAforce_)
            3 -> cParam_ = ConnParam(1, 1, 2, lengthAforce_)
            4 -> cParam_ = ConnParam(1, 1, 0, lengthAforce_)
            5 -> cParam_ = ConnParam(2, 1, 2, lengthAforce_)
            6 -> cParam_ = ConnParam(2, 1, 0, lengthAforce_)
            7 -> cParam_ = ConnParam(1, 1, 1, lengthAforce_)
            8 -> cParam_ = ConnParam(2, 1, 1, lengthAforce_)
            9 -> cParam_ = ConnParam(1, 2, curLCR, lengthAforce_)
            10 -> cParam_ = ConnParam(2, 2, curLCR, lengthAforce_)
        }
    }

    fun setDimAlignByChild() {
        if (isChangeDimAlignB_ == false) {
            myDimAlignB_ = if (nodeTriangleB_ == null) 1 else 3
        }
        if (isChangeDimAlignC_ == false) {
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

    private fun setMyBound() {
        var lb = pointCA_.min(pointAB_)
        lb = pointAB_.min(pointBC_)
        myBP_.left = lb.x
        myBP_.bottom = lb.y
        var rt = pointCA_.max(pointAB_)
        rt = pointAB_.max(pointBC_)
        myBP_.right = rt.x
        myBP_.top = rt.y
    }

    fun setForcelengthes(){
        val scaleback = 1 / scale_
        lengthAforce_ = lengthA_ * scaleback
        lengthBforce_ = lengthB_ * scaleback
        lengthCforce_ = lengthC_ * scaleback
    }

    fun setNumber(num: Int) {
        myNumber_ = num
    }

    fun setScale(scale: Float) {
        scale_ = scale
        lengthA_ *= scale
        lengthB_ *= scale
        lengthC_ *= scale
        calcPoints(pointCA_, angle)
    }

    //maybe not use.
    private fun setParentInfo(myParNum: Int, myParBC: Int, myConne: Int) {
        parentNumber = myParNum
        parentBC = myParBC
    }

    fun setDimPoint() {
        dimPointA_ =
            dimSideRotation(dimSideAlignA_, pointCA_.calcMidPoint(pointAB_), pointAB_, pointCA_)
        dimPointB_ =
            dimSideRotation(dimSideAlignB_, pointAB_.calcMidPoint(pointBC_), pointBC_, pointAB_)
        dimPointC_ =
            dimSideRotation(dimSideAlignC_, pointBC_.calcMidPoint(pointCA_), pointCA_, pointBC_)
    }

    fun setPointNumberMoved_(p: PointXY) {
        pointNumber_ = p
        isPointNumberMoved_ = true
    }

    fun setDimPath(ts: Float) {
        dimH_ = ts
        pathA_ = PathAndOffset(
            scale_,
            pointAB_, pointCA_,
            pointBC_, lengthAforce_, myDimAlignA_, dimSideAlignA_, dimH_
        )
        pathB_ = PathAndOffset(
            scale_,
            pointBC_, pointAB_,
            pointCA_, lengthBforce_, myDimAlignB_, dimSideAlignB_, dimH_
        )
        pathC_ = PathAndOffset(
            scale_,
            pointCA_, pointBC_,
            pointAB_, lengthCforce_, myDimAlignC_, dimSideAlignC_, dimH_
        )
        pathS_ = PathAndOffset(
            scale_, pointAB_,
            pointCA_, pointBC_, lengthAforce_, 4, 0, dimH_
        )

        //sla_ = formattedString( lengthAforce_, 3);
        //slb_ = formattedString( lengthBforce_, 3);
        //slc_ = formattedString( lengthCforce_, 3);
    }

    fun hasConstantParent(): Boolean {
        val iscons = myNumber_ - parentNumber
        return iscons <= 1
    }

    fun crossOffset(pbc: Int): PointXY? {
        if (pbc == 1) return pointCA_.crossOffset(pointBC_, 1.0f)
        return if (pbc == 2) pointBC_.crossOffset(pointAB_, 1.0f) else pointCA_
    }

    fun rotateLCRandGet(): TriangleK {
        rotateLCR()
        return this
    }

    fun rotateLCR(): PointXY? {
        //if(myParentBC_ < 3) return new PointXY(0f,0f);
        connectionLCR_--
        if (connectionLCR_ < 0) connectionLCR_ = 2
        cParam_.lcr = connectionLCR_
        setParentBCFromCLCR()
        return setBasePoint(cParam_)
    }

    fun reload() {
        if (nodeTriangleA_ != null) {
            nodeTriangleA_!!.reload()
            calcPoints(
                nodeTriangleA_!!.getPointBySide(parentBC).also {
                    if (it != null) {
                        pointCA_ = it
                    }
                },
                nodeTriangleA_!!.getAngleBySide(
                    parentBC
                ).also { angle = it })
        }
    }

    fun hasChild(): Boolean {
        return if (nodeTriangleB_ != null || nodeTriangleC_ != null) true else false
    }

    fun collision(x: Float, y: Float): Boolean {
        return true
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

    fun roundByUnderTwo(fp: Float): Float {
        val ip = fp * 100f
        return Math.round(ip) / 100f
    }

    fun validTriangle(): Boolean {
        return if (lengthA_ <= 0.0f || lengthB_ <= 0.0f || lengthC_ <= 0.0f) false else isValidLengthes(
            lengthA_,
            lengthB_,
            lengthC_
        )
        //!((this.lengthA_ + this.lengthB_) <= this.lengthC_) &&
        //      !((this.lengthB_ + this.lengthC_) <= this.lengthA_) &&
        //    !((this.lengthC_ + this.lengthA_) <= this.lengthB_);
    }

    fun isValidLengthes(A: Float, B: Float, C: Float): Boolean {
        return A + B > C && B + C > A && C + A > B
    }

    fun calculateInternalAngle(p1: PointXY?, p2: PointXY?, p3: PointXY?): Double {
        val v1 = p1!!.subtract(p2)
        val v2 = p3!!.subtract(p2)
        val angleRadian = Math.acos(v1.innerProduct(v2) / (v1.magnitude() * v2.magnitude()))
        return angleRadian * 180 / Math.PI
    }

    fun calcMyAngles() {
        angleAB = calculateInternalAngle(pointCA_, pointAB_, pointBC_).toFloat()
        angleBC = calculateInternalAngle(pointAB_, pointBC_, pointCA_).toFloat()
        angleCA = calculateInternalAngle(pointBC_, pointCA_, pointAB_).toFloat()
    }

    fun getBasePoint(t :TriangleK, point1 :PointXY, point2 :PointXY ) :PointXY {

        val length = t.getLengthBySide( cParam_.side )

        when( cParam_.type ){
            0 -> return point1
            1 -> { //nijyuudanmen
                when( cParam_.lcr ){
                    0 -> return point1
                    1 -> return point1.offset( point2, (lengthA_ - length ) * 0.5f )
                    2 -> return point1.offset( point2,  lengthA_ - length  )
                }
            }
            2 -> { // float connections
                when( cParam_.lcr ){
                    0 -> return point1.crossOffset( point2, -1.0f )
                    1 -> return point1.offset( point2, (lengthA_ - length ) * 0.5f ).crossOffset( point2, -1.0f )
                    2 -> return point1.offset( point2,  lengthA_ - length  ).crossOffset( point2, -1.0f )
                }
            }
        }

        return point1
    }

    fun calcPoints( ref :TriangleK?, refside :Int, pos :PointXY = PointXY( 0f, 0f ), angle_ :Float = 0f ) {
        if( ref != null || refside != -1 ) setNode( ref, refside )

        setCParamFromParentBC( this.parentBC )

        val plist: Array<PointXY?>
        val llist: FloatArray
        val powlist: DoubleArray
        var angle = 0f

        when (refside) {
            -1 -> {
                pointCA_ = pos
                angle = angle_
                plist = arrayOf(pointCA_, pointAB_, pointBC_)
                llist = floatArrayOf(lengthA_, lengthB_, lengthC_)
                powlist = doubleArrayOf(
                    Math.pow(lengthA_.toDouble(), 2.0),
                    Math.pow(lengthB_.toDouble(), 2.0),
                    Math.pow(lengthC_.toDouble(), 2.0)
                )
            }
            0 -> {
                ref!!.setNode( this, this.parentSide )
                angle = ref!!.getAngleBySide( this.parentSide )
                plist = arrayOf( getParentPointByType(), pointAB_, pointBC_)
                llist = floatArrayOf(lengthA_, lengthB_, lengthC_)
                powlist = doubleArrayOf(
                    Math.pow(lengthA_.toDouble(), 2.0),
                    Math.pow(lengthB_.toDouble(), 2.0),
                    Math.pow(lengthC_.toDouble(), 2.0)
                )
            }
            1 -> {
                ref!!.setNode( this, 0 )
                angle = ref!!.angle + 180f //- nodeTriangleB_.angleInnerCA_;
                plist = arrayOf( ref!!.getBasePoint( this, ref!!.pointAB_, ref!!.pointCA_ ), pointBC_, pointCA_)
                llist = floatArrayOf(lengthB_, lengthC_, lengthA_)
                powlist = doubleArrayOf(
                    Math.pow(lengthB_.toDouble(), 2.0),
                    Math.pow(lengthC_.toDouble(), 2.0),
                    Math.pow(lengthA_.toDouble(), 2.0)
                )
                pointAB_ = plist[0]!!
            }
            2 -> {
                ref!!.setNode( this, 0 )
                angle =ref!!.angle + 180f //- nodeTriangleB_.angleInnerCA_;
                plist = arrayOf(ref!!.getBasePoint( this, ref!!.pointAB_, ref!!.pointCA_ ), pointCA_, pointAB_)
                llist = floatArrayOf(lengthC_, lengthA_, lengthB_)
                powlist = doubleArrayOf(
                    Math.pow(lengthC_.toDouble(), 2.0),
                    Math.pow(lengthA_.toDouble(), 2.0),
                    Math.pow(lengthB_.toDouble(), 2.0)
                )
                pointBC_ = plist[0]!!
            }
            else -> throw IllegalStateException("Unexpected value: $refside")
        }

        plist[1]!![(plist[0]!!.x + llist[0] * Math.cos(Math.toRadians(angle.toDouble()))).toFloat()] =
            (plist[0]!!.y + llist[0] * Math.sin(Math.toRadians(angle.toDouble()))).toFloat()

        myTheta_ = Math.atan2(
            (plist[0]!!.y - plist[1]!!.y).toDouble(), (plist[0]!!
                .x - plist[1]!!.x).toDouble()
        )
        myAlpha_ = Math.acos((powlist[0] + powlist[1] - powlist[2]) / (2 * llist[0] * llist[1]))
        plist[2]!![(plist[1]!!.x + llist[1] * Math.cos(myTheta_ + myAlpha_)).toFloat()] =
            (plist[1]!!.y + llist[1] * Math.sin(myTheta_ + myAlpha_)).toFloat()
        calcMyAngles()
        if (refside == 1) this.angle = nodeTriangleB_!!.angle - angleCA
        if (refside == 2) this.angle = nodeTriangleC_!!.angle + angleCA

        setCenterAndBoundsAndDimPoints()
    }

    private fun calcPoints(pCA: PointXY?, angle: Float) {
        pointAB_[(pCA!!.x + lengthA_ * Math.cos(Math.toRadians(angle.toDouble()))).toFloat()] =
            (pCA.y + lengthA_ * Math.sin(Math.toRadians(angle.toDouble()))).toFloat()
        myTheta_ = Math.atan2((pCA.y - pointAB_!!.y).toDouble(), (pCA.x - pointAB_.x).toDouble())
        myPowA_ = Math.pow(lengthA_.toDouble(), 2.0)
        myPowB_ = Math.pow(lengthB_.toDouble(), 2.0)
        myPowC_ = Math.pow(lengthC_.toDouble(), 2.0)
        myAlpha_ = Math.acos((myPowA_ + myPowB_ - myPowC_) / (2 * lengthA_ * lengthB_))
        pointBC_[(pointAB_.x + lengthB_ * Math.cos(myTheta_ + myAlpha_)).toFloat()] =
            (pointAB_.y + lengthB_ * Math.sin(myTheta_ + myAlpha_)).toFloat()
        angleAB = calculateInternalAngle(pointCA_, pointAB_, pointBC_).toFloat()
        angleBC = calculateInternalAngle(pointAB_, pointBC_, pointCA_).toFloat()
        angleCA = calculateInternalAngle(pointBC_, pointCA_, pointAB_).toFloat()

        setCenterAndBoundsAndDimPoints()
    }

    fun setCenterAndBoundsAndDimPoints(){
        pointCenter_[(pointAB_.x + pointBC_.x + pointCA_.x) / 3] =
            (pointAB_.y + pointBC_.y + pointCA_.y) / 3
        setMyBound()
        if (isPointNumberMoved_ == false) autoAlignPointNumber()
        dimPointA_ = pointCA_.calcMidPoint(pointAB_) //.crossOffset(pointBC_, 0.2f*scale_);
        dimPointB_ = pointAB_.calcMidPoint(pointBC_)
        dimPointC_ = pointBC_.calcMidPoint(pointCA_)
        setDimPoint()
        dimAngleB_ = angleMpAB
        dimAngleC_ = angleMmCA
    }

    fun move(to: PointXY) {
        pointAB_.add(to)
        pointBC_.add(to)
        pointCA_.add(to)
        pointCenter_ = pointCenter_.plus(to)
        pointNumber_ = pointNumber_.plus(to)
        dimPointA_.add(to)
        dimPointB_.add(to)
        dimPointC_.add(to)
        myBP_.left = myBP_.left + to.x
        myBP_.right = myBP_.right + to.x
        myBP_.top = myBP_.top + to.y
        myBP_.bottom = myBP_.bottom + to.x
    }

    fun scale(basepoint: PointXY?, scale: Float) {
        scale_ *= scale
        //pointAB_.scale(basepoint, scale);
        //pointBC_.scale(basepoint, scale);
        pointCA_.scale(basepoint, scale)
        pointCenter_.scale(basepoint, scale)
        pointNumber_.scale(basepoint, scale)
        lengthA_ *= scale
        lengthB_ *= scale
        lengthC_ *= scale
        calcPoints(pointCA_, angle)
    }

    fun rotate(basepoint: PointXY?, degree: Float) {
        pointCA_ = pointCA_.rotate(basepoint, degree)
        angle += degree
        calcPoints(pointCA_, angle)
        //setDimAlign();
    }

    private fun autoAlignPointNumber() {
        if (isPointNumberMoved_ == false) pointNumber_ = pointCenter_ //とりあえず重心にする
    }

    fun autoSetDimAlign(): Int { // 1:下 3:上
        myDimAlignA_ = calcDimAlignByInnerAngleOf(0, angle)
        myDimAlignB_ = calcDimAlignByInnerAngleOf(1, angleMpAB)
        myDimAlignC_ = calcDimAlignByInnerAngleOf(2, angleMmCA)

        setDimPath(dimH_)
        return myDimAlign_
    }

    fun calcDimAlignByInnerAngleOf(ABC: Int, angle: Float): Int {    // 夾角の、1:外 　3:内
        if (ABC == 0) {
            if (parentBC == 9 || parentBC == 10) return 1
            if (parentBC > 2 || nodeTriangleB_ != null || nodeTriangleC_ != null) return 3
        }
        if (ABC == 1 && nodeTriangleB_ != null) return 1
        return if (ABC == 2 && nodeTriangleC_ != null) 1 else 3
        // if ABC = 0
    }

    fun rotateDimSideAlign(side: Int) {
        if (side == 0) dimSideAlignA_ = rotateZeroToThree(dimSideAlignA_)
        if (side == 1) dimSideAlignB_ = rotateZeroToThree(dimSideAlignB_)
        if (side == 2) dimSideAlignC_ = rotateZeroToThree(dimSideAlignC_)
        if (side == 4) nameSideAlign_ = rotateZeroToThree(nameSideAlign_)
        setDimPath(dimH_)
    }

    fun rotateZeroToThree(num_: Int): Int {
        var num = num_
        num++
        if (num > 2) num = 0
        return num
    }

    fun flipOneToThree(num_: Int): Int {
        var num = num_
        if (num == 1) return 3.also { num = it }
        return if (num == 3) 1.also { num = it } else num
        // sonomama kaesu.
    }

    // 呼び出す場所によって、強制になってしまう。
    fun autoSetDimSideAlign() {
        if (lengthCforce_ < 1.5f || lengthBforce_ < 1.5f) {
            myDimAlignB_ = 1
            myDimAlignC_ = 1
        }
        if (lengthCforce_ < 1.5f) dimSideAlignB_ = 1
        if (lengthBforce_ < 1.5f) dimSideAlignC_ = 2
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

    fun zeroTwoRotate(num_: Int): Int {
        var num = num_
        num = num + 1
        return if (num > 2) 0.also { num = it } else num
    }

    fun isName(name: String): Boolean {
        return myName_ == name
    }

    // 自分の次の番号がくっついている辺を調べてA辺にする。
    // 他の番号にあって自身の辺上に無い場合は、A辺を変更しない。
    fun rotateLengthBy(side: Int): TriangleK {
        //Triangle this = this.clone();
        var pf = 0f
        var pi = 0
        var pp = PointXY(0f, 0f)
        if (side == 1) { // B to A
            pf = lengthA_
            lengthA_ = lengthB_
            lengthB_ = lengthC_
            lengthC_ = pf
            pf = lengthAforce_
            lengthAforce_ = lengthBforce_
            lengthBforce_ = lengthCforce_
            lengthCforce_ = pf
            pp = pointCA_.clone()
            pointCA_ = pointAB_
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
            pf = lengthA_
            lengthA_ = lengthC_
            lengthC_ = lengthB_
            lengthB_ = pf
            pf = lengthAforce_
            lengthAforce_ = lengthCforce_
            lengthCforce_ = lengthBforce_
            lengthBforce_ = pf
            pp = pointCA_.clone()
            pointCA_ = pointBC_
            pointBC_ = pointAB_
            pointAB_ = pp.clone()
            val ga = angle
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
        return this
    }

    fun isCollide(p: PointXY): Boolean {
        return p.isCollide(pointAB_, pointBC_, pointCA_)
    }

    fun dimSideRotation(
        side: Int,
        dimPoint: PointXY,
        offsetLeft: PointXY?,
        offsetRight: PointXY?
    ): PointXY {
        if (side == 0) return dimPoint
        var offsetTo = offsetRight
        val haba = dimPoint.lengthTo(offsetRight) * 0.5f
        if (side == 1) {
            offsetTo = offsetLeft
        }
        return dimPoint.offset(offsetTo, haba)
    }

    fun formattedString(digit: Float, fractionDigits: Int): String {
        // 0の場合は空文字
        if (digit == 0f) return "0.00"
        val formatter = "%.\${fractionDigits}f"
        return String.format(java.lang.Float.toString(digit))
    }


}