package com.jpaver.trianglelist

import java.util.*

class TriangleK(@kotlin.jvm.JvmField
                    var nodeTriangle: Array<TriangleK?> = arrayOfNulls(3),
                @kotlin.jvm.JvmField
                    var length: Array<Float> = Array(3) { 0f },
                @kotlin.jvm.JvmField
                    var point: Array<PointXY> = Array(3) { PointXY(0f, 0f) },
                    var parentBC_: Int = -1,
                @kotlin.jvm.JvmField
                    var baseangle: Float = 0f
) : EditObject(), Cloneable {

    //for first triangle.
    constructor(A: Float, B: Float, C: Float, pCA: PointXY, angle_: Float) :this(
            nodeTriangle = arrayOf( null, null, null ),
            length       = arrayOf( A, B, C ),
            point        = arrayOf( pCA, PointXY(0f,0f), PointXY(0f,0f) ),
            parentBC_    = -1,
            baseangle    = angle_
    ){
        calcPoints(null, -1, point[0], baseangle)
    }

    //for first. most simple.
    constructor(A: Float, B: Float, C: Float) :this(
            nodeTriangle = arrayOf( null, null, null ),
            length       = arrayOf( A, B, C ),
            point        = arrayOf( PointXY(0f,0f), PointXY(0f,0f), PointXY(0f,0f) ),
            parentBC_    = -1,
            baseangle    = 0f
    ){
        calcPoints(null, -1, point[0], baseangle)
    }

    // use node A to my parent.
    constructor(parent: TriangleK, pbc: Int, B: Float, C: Float) :this(
            nodeTriangle = arrayOf( parent, null, null ),
            length       = arrayOf( parent.getLengthBySide(pbc), B, C ),
            point        = arrayOf( parent.getPointBySide(pbc), PointXY(0f,0f), PointXY(0f,0f) ),
            parentBC_    = pbc,
            baseangle    = parent.getAngleBySide(pbc)
    ){
        calcPoints(parent, 0)
        //set(parent, pbc, B, C)
    }

    // use node A to my parent. A uses different length.
    constructor(parent: TriangleK, pbc: Int, A: Float, B: Float, C: Float) :this(
            nodeTriangle = arrayOf( parent, null, null ),
            length       = arrayOf( parent.getLengthBySide(pbc), B, C ),
            point        = arrayOf( parent.getPointBySide(pbc), PointXY(0f,0f), PointXY(0f,0f) ),
            parentBC_    = pbc,
            baseangle    = parent.getAngleBySide(pbc)
    ){
        //calcPoints( myParent, 0 )
        set(parent, pbc, A, B, C)
    }

    // reference node B or C to set base point.
    constructor(child: TriangleK, A: Float, B: Float, C: Float) :this(
            nodeTriangle = arrayOf( null, null, null ),
            length       = arrayOf( A, B, C ),
            point        = arrayOf( PointXY(0f,0f), PointXY(0f,0f), PointXY(0f,0f) ),
            parentBC_    = -1,
            baseangle    = 0f
    ) {
        calcPoints(child, child.parentSide)
    }

    // connection parameter
    constructor(parent: TriangleK, cParam: ConnParam, B: Float, C: Float) :this(
            nodeTriangle = arrayOf( parent, null, null ),
            length       = arrayOf( parent.getLengthBySide(cParam.side), B, C ),
            point        = arrayOf( parent.getPointBySide(cParam.side), PointXY(0f,0f), PointXY(0f,0f) ),
            parentBC_    = getPbcByCParam(cParam),
            baseangle    = parent.getAngleBySide(cParam.side)
    ){
        set(parent, cParam, B, C)
    }

    constructor(parent: TriangleK, dP: Params) :this(
            nodeTriangle = arrayOf( parent, null, null ),
            length       = arrayOf( parent.getLengthBySide(dP.pl), dP.b, dP.c ),
            point        = arrayOf( parent.getPointBySide(dP.pl), PointXY(0f,0f), PointXY(0f,0f) ),
            parentBC_    = dP.pl,
            baseangle    = parent.getAngleBySide(dP.pl)
    ) {
        set(parent, dP.pl, dP.a, dP.b, dP.c)
        myName_ = dP.name
        autoSetDimSideAlign()
    }

    constructor(dP: Params, angle: Float) :this(
            nodeTriangle = arrayOf( null, null, null ),
            length       = arrayOf( dP.a, dP.b, dP.c ),
            point        = arrayOf( dP.pt, PointXY(0f,0f), PointXY(0f,0f) ),
            parentBC_    = -1,
            baseangle    = angle
    ) {
        setNumber(dP.n)
        myName_ = dP.name
        initBasicArguments(dP.a, dP.b, dP.c, dP.pt, angle)
        calcPoints(dP.pt, angle)
    }


    @kotlin.jvm.JvmField
        var innerangle = arrayOf( 0f, 0f, 0f )
    @kotlin.jvm.JvmField
        var lengthforce = arrayOf( 0f, 0f, 0f )
    @kotlin.jvm.JvmField
        var dimpoint = arrayOf( PointXY(0f, 0f), PointXY(0f, 0f), PointXY(0f, 0f) )
    @kotlin.jvm.JvmField
        var dimalignV = arrayOf( 3, 3, 3 )
    @kotlin.jvm.JvmField
        var dimalignH = arrayOf( 0, 0, 0 )
    @kotlin.jvm.JvmField
        var dimpath :Array<PathAndOffset?> = arrayOfNulls(3)
    
    var valid_ = false
    var scale_ = 1f
    var lengthAforce_ = 0f
    var lengthBforce_ = 0f
    var lengthCforce_ = 0f
    var sla_ = ""
    var slb_ = ""
    var slc_ = ""
    // base point by calc
    //@kotlin.jvm.JvmField
    //var point[1] = PointXY(0f, 0f)
    //@kotlin.jvm.JvmField
    //var point[2] = PointXY(0f, 0f)
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

    //var innerangle[0] = 0f
    //var innerangle[1] = 0f
    //var innerangle[2] = 0f
    var dimAngleB_ = 0f
    var dimAngleC_ = 0f
    var parentNumber_ = -1 // 0:root

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


    
    var isChildB_ = false
    var isChildC_ = false


    public override fun clone(): TriangleK {
        val b = TriangleK()
        try {
            //b = super.clone()
            b.nodeTriangle = nodeTriangle.clone()
            b.length = length.clone()
            b.lengthAforce_ = lengthAforce_
            b.lengthBforce_ = lengthBforce_
            b.lengthCforce_ = lengthCforce_
            b.point = point.clone()
            b.baseangle = baseangle
            b.myName_ = myName_
            b.myNumber_ = myNumber_
            b.parentBC_ = parentBC_
            b.parentNumber_ = parentNumber_
            b.dimPointA_ = dimPointA_
            b.myDimAlignA_ = myDimAlignA_
            b.myDimAlignB_ = myDimAlignB_
            b.myDimAlignC_ = myDimAlignC_
            b.dimSideAlignA_ = dimSideAlignA_
            b.dimSideAlignB_ = dimSideAlignB_
            b.dimSideAlignC_ = dimSideAlignC_
            //b.point[1] = point[1].clone()
            //b.point[2] = point[2].clone()
            b.pointCenter_ = pointCenter_.clone()
            b.pointNumber_ = pointNumber_.clone()
            b.isPointNumberMoved_ = isPointNumberMoved_
            b.myBP_.left = myBP_.left
            b.myBP_.top = myBP_.top
            b.myBP_.right = myBP_.right
            b.myBP_.bottom = myBP_.bottom
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

    // set argument methods
    private fun initBasicArguments(A: Float, B: Float, C: Float, pCA: PointXY?, angle: Float) {
        length[0] = A
        length[1] = B
        length[2] = C
        lengthAforce_ = A
        lengthBforce_ = B
        lengthCforce_ = C
        valid_ = validTriangle()
        point[0] = PointXY(pCA!!.x, pCA.y)
        point[1] = PointXY(0.0f, 0.0f)
        point[2] = PointXY(0.0f, 0.0f)
        pointCenter_ = PointXY(0.0f, 0.0f)
        //this.pointNumber_ = new PointXY(0.0f, 0.0f);
        this.baseangle = angle
        innerangle[0] = 0f
        innerangle[1] = 0f
        innerangle[2] = 0f
        //childSide_ = 0;
        //myDimAlignA = 0;
        //myDimAlignB = 0;
        //myDimAlignC = 0;
    }

    operator fun set(parent: TriangleK, pbc: Int, B: Float, C: Float): TriangleK {
        //myNumber_ = parent.myNumber_ + 1;
        parentBC_ = pbc
        setNode(parent, 0, false)
        parent.setNode(this, parentSide, false)

        //setParent(parent, A);
        when( pbc ) {
            1 -> {
                parentBC_ = 1
                length[0] = nodeTriangle[0]!!.length[1]
                lengthAforce_ = nodeTriangle[0]!!.lengthBforce_
                point[0] = nodeTriangle[0]!!.point[2]
                baseangle = nodeTriangle[0]!!.angleMpAB
            }
            2 -> {
                parentBC_ = 2
                length[0] = nodeTriangle[0]!!.length[2]
                lengthAforce_ = nodeTriangle[0]!!.lengthCforce_
                point[0] = nodeTriangle[0]!!.point[0]
                baseangle = nodeTriangle[0]!!.angleMmCA
            }
            else -> {
                parentBC_ = 0
                length[0] = 0f
                lengthAforce_ = 0f
                point[0] = PointXY(0f, 0f)
                baseangle = 180f
            }
        }

        parentNumber_ = nodeTriangle[0]!!.myNumber_
        //nodeTriangle[0].setChild(this, parentBC_);
        initBasicArguments(length[0], B, C, point[0], baseangle)
        calcPoints(point[0], baseangle)

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
        if ( nodeTriangle[1] != null )
            nodeTriangle[1]!![ this, nodeTriangle[1]!!.parentBC_, getLengthBySide( nodeTriangle[1]!!.parentSide),
                                    nodeTriangle[1]!!.length[1]] = nodeTriangle[1]!!.length[2]
        if ( nodeTriangle[2] != null )
            nodeTriangle[2]!![ this, nodeTriangle[2]!!.parentBC_, getLengthBySide( nodeTriangle[2]!!.parentSide),
                                    nodeTriangle[2]!!.length[1] ] = nodeTriangle[2]!!.length[2]


        return this
    }

    operator fun set(parent: TriangleK, pbc: Int, A: Float, B: Float, C: Float): TriangleK? {
        //myNumber_ = parent.myNumber_ + 1;
        parentBC_ = pbc
        setNode(parent, 0, false)
        parent.setNode(this, parentSide, false)

        //setParent(parent, pbc);
        //nodeTriangle[0].setChild(this, pbc );

        // if user rewrite A
        if (A != parent.getLengthBySide(pbc)) {
            length[0] = A
            lengthAforce_ = A
        } else {
            length[0] = parent.getLengthBySide(pbc)
            lengthAforce_ = parent.getLengthBySide(pbc)
        }
        setCParamFromParentBC(pbc)
        point[0] = getParentPointByType(cParam_)
        when (pbc) {
            1 -> { // B
                parentBC_ = 1
                baseangle = parent.angleMpAB
            }
            2 -> { // C
                parentBC_ = 2
                baseangle = parent.angleMmCA
            }
            3 -> { // B-R
                parentBC_ = 3
                baseangle = parent.angleMpAB
            }
            4 -> { //B-L
                parentBC_ = 4
                baseangle = parent.angleMpAB
            }
            5 -> { //C-R
                parentBC_ = 5
                baseangle = parent.angleMmCA
            }
            6 -> { //C-L
                parentBC_ = 6
                baseangle = parent.angleMmCA
            }
            7 -> { //B-Center
                parentBC_ = 7
                baseangle = parent.angleMpAB
            }
            8 -> { //C-Center
                parentBC_ = 8
                baseangle = parent.angleMmCA
            }
            9 -> { //B-Float-R
                parentBC_ = 9
                baseangle = parent.angleMpAB
            }
            10 -> { //C-Float-R
                parentBC_ = 10
                baseangle = parent.angleMmCA
            }
            else -> {
                parentBC_ = 0
                length[0] = 0f
                lengthAforce_ = 0f
                point[0] = PointXY(0f, 0f)
                baseangle = 180f
            }
        }
        parentNumber_ = parent.myNumber_
        initBasicArguments(length[0], B, C, point[0], baseangle)
        if (!validTriangle()) return null
        calcPoints(point[0], baseangle)
        if (parentBC_ == 4) {
            val vector = PointXY(
                    parent.point[1].x - point[1].x,
                    parent.point[1].y - point[1].y
            )
            move(vector)
        }
        if (parentBC_ == 6) {
            val vector = PointXY(
                    parent.point[2].x - point[1].x,
                    parent.point[2].y - point[1].y
            )
            move(vector)
        }

        //myDimAlign = setDimAlign();
        return clone()
    }

    operator fun set(myParent: TriangleK, pbc: Int) {
        this[myParent, pbc, length[1]] = length[2]
    }

    operator fun set(myParent: TriangleK, dP: Params) {
        set(myParent, dP.pl, dP.a, dP.b, dP.c)
        myName_ = dP.name
    }

    operator fun set(parent: TriangleK, cParam: ConnParam, B: Float, C: Float): TriangleK? {
        //myNumber_ = parent.myNumber_ + 1;
        //parentBC_ = cParam.getSide();
        setNode(parent, 0, false)
        parent.setNode(this, cParam.side, false)

        //setParent( parent, cParam.getSide() );
        baseangle = nodeTriangle[0]!!.getAngleBySide(cParam.side)
        setConnectionType(cParam)
        initBasicArguments(length[0], B, C, point[0], baseangle)
        if (!validTriangle()) return null
        calcPoints(point[0], baseangle)
        setDimAlignByChild()

        //nodeTriangle[0].setChild(this, cParam.getSide() );
        return clone()
    }


    fun reset(prm: Params): TriangleK {
        //ConneParam thisCP = cParam_.clone();
        length[0] = prm.a
        lengthAforce_ = prm.a
        setCParamFromParentBC(prm.pl)
        parentBC_ = prm.pl
        parentNumber_ = prm.pn
        if (nodeTriangle[0] == null || parentNumber_ < 1) resetLength(prm.a, prm.b, prm.c)
        else {
            set(nodeTriangle[0]!!, cParam_, prm.b, prm.c)
        }
        //set(parent_, tParams.getPl(), tParams.getA(), tParams.getB(), tParams.getC() );
        //cParam_ = thisCP.clone();
        myName_ = prm.name
        return this
    }

    fun resetElegant(prm: Params) {
        reset(prm)
        if (nodeTriangle[0] != null) nodeTriangle[0]!!.resetByNode(parentSide)
    }

    fun reset( new :TriangleK?, triarray : Array<TriangleK?> ) {
        if( new == null ) return

        if( new.nodeTriangle[0] != null )
            setNode(new.nodeTriangle[0]!!, 0, true )

        reset( new, new.cParam_ )

    }

    fun reset(newTri: TriangleK): TriangleK {
        val thisCP = cParam_.clone()
        if (nodeTriangle[0] == null || parentNumber_ < 1) resetLength(
                newTri.length[0],
                newTri.length[1],
                newTri.length[2]
        ) else set(
                nodeTriangle[0]!!,
                newTri.parentBC_,
                newTri.length[0],
                newTri.length[1],
                newTri.length[2]
        )
        cParam_ = thisCP.clone()
        myName_ = newTri.myName_
        return clone()
    }

    fun reset(newTri: TriangleK, cParam: ConnParam): TriangleK {
        if (nodeTriangle[0] == null) resetLength(
                newTri.length[0],
                newTri.length[1],
                newTri.length[2]
        ) else set(nodeTriangle[0]!!, cParam, newTri.length[1], newTri.length[2])
        myName_ = newTri.myName_
        return clone()
    }

    fun resetLength(A: Float, B: Float, C: Float): TriangleK {
        //lengthA = A; lengthB = B; lengthC = C;
        initBasicArguments(A, B, C, point[0], baseangle)
        calcPoints(point[0], baseangle)
        return clone()
    }

    fun resetByParent(prnt: TriangleK, cParam: ConnParam): Boolean {
        if (!isValidLengthes(prnt.getLengthBySide(parentSide), length[1], length[2])) return false
        val triIsValid = set(prnt, cParam, length[1], length[2])
        return triIsValid != null
    }

    fun resetByChild(myChild: TriangleK, cParam: ConnParam) {
        val cbc = myChild.cParam_.side
        childSide_ = myChild.parentBC_
        if (nodeTriangle[0] == null) {
            if (cbc == 1) resetLength(length[0], myChild.length[0], length[2])
            if (cbc == 2) resetLength(length[0], length[1], myChild.length[0])
            return
        }
        if (cbc == 1) {
            set(nodeTriangle[0]!!, cParam, myChild.length[0], length[2])
            nodeTriangle[1] = myChild.clone()
        }
        if (cbc == 2) {
            set(nodeTriangle[0]!!, cParam, length[1], myChild.length[0])
            nodeTriangle[2] = myChild.clone()
        }
        setDimAlignByChild()
    }

    // reset by parent.
    fun resetByParent(prnt: TriangleK, pbc: Int): Boolean {
        var triIsValid: TriangleK? = null
        val parentLength = prnt.getLengthBySide(pbc)

        //if(pbc == 1 ) triIsValid = set(prnt, pbc, length[0], parentLength, );
        if (pbc <= 2) {
            if (!isValidLengthes(parentLength, length[1], length[2])) {
                triIsValid = set(prnt, pbc, length[0], length[1], length[2])
                return false
            } else triIsValid = set(prnt, pbc, parentLength, length[1], length[2])
        }
        if (pbc > 2) triIsValid = set(prnt, pbc, length[0], length[1], length[2])
        return triIsValid != null
    }

    // 子のA辺が書き換わったら、それを写し取ってくる。同一辺長接続のとき（１か２）以外はリターン。
    fun resetByChild(myChild: TriangleK?) {
        if( myChild == null ) return
        setDimAlignByChild()
        if (myChild.cParam_.type != 0) return
        val cbc = myChild.parentBC_
        if (cbc == 1 && !isValidLengthes(length[0], myChild.length[0], length[2])) return
        if (cbc == 2 && !isValidLengthes(length[0], length[1], myChild.length[0])) return
        childSide_ = myChild.parentBC_
        if (nodeTriangle[0] == null || parentNumber_ < 1) {
            if (cbc == 1) resetLength(length[0], myChild.length[0], length[2])
            if (cbc == 2) resetLength(length[0], length[1], myChild.length[0])
            return
        }
        if (cbc == 1) {
            set(nodeTriangle[0]!!, parentBC_, length[0], myChild.length[0], length[2])
            //nodeTriangle[1] = myChild;
        }
        if (cbc == 2) {
            set(nodeTriangle[0]!!, parentBC_, length[0], length[1], myChild.length[0])
            //nodeTriangle[2] = myChild;
        }
    }

    fun setNode(node: TriangleK?, pbc: Int, shake: Boolean = false ) {
        if (node == null) return

        var side = pbc

        if (side > 2) side = parentSide
        when (side) {
            -1 -> {
            }
            0 -> {
                nodeTriangle[0] = node
                parentNumber_ = nodeTriangle[0]!!.myNumber_
                if (node === nodeTriangle[1]) nodeTriangle[1] = null
                if (node === nodeTriangle[2]) nodeTriangle[2] = null
            }
            1 -> {
                nodeTriangle[1] = node
                if (node === nodeTriangle[0]) nodeTriangle[0] = null
                if (node === nodeTriangle[2]) nodeTriangle[2] = null
            }
            2 -> {
                nodeTriangle[2] = node
                if (node === nodeTriangle[1]) nodeTriangle[1] = null
                if (node === nodeTriangle[0]) nodeTriangle[0] = null
            }
        }

        if( shake == true ) node.setNode( this, cParam_.side )
    }

    fun resetByNode(pbc: Int) {
        val node = getNode(pbc)
        if (node != null) {
            var length = getLengthBySide(pbc)
            if (node.parentBC_ < 3) length = node.length[0]
            when (pbc) {
                0 -> {
                }
                1 -> initBasicArguments( this.length[0], length, this.length[2], node.point[2], -node.baseangle)
                2 -> initBasicArguments(
                        this.length[0],
                        this.length[1],
                        length,
                        node.point[0],
                        node.baseangle + innerangle[2]
                )
            }
            calcPoints(point[0], baseangle)
        }
    }

    fun resetNode(prms: Params, parent: TriangleK, doneObjectList: ArrayList<TriangleK>) {

        // 接続情報の変更、とりあえず挿入処理は考慮しない、すでに他のノードがあるときは上書きする。
        nodeTriangle[0]!!.removeNode(this)
        nodeTriangle[0] = parent
        nodeTriangle[0]!!.setNode(this, prms.pl, false)
        reset(prms)
        doneObjectList.add(this)
    }

    fun getNode(pbc: Int): TriangleK? {
        when (pbc) {
            0 -> return nodeTriangle[0]
            1 -> return nodeTriangle[1]
            2 -> return nodeTriangle[2]
            -1 -> return this
        }
        return null
    }

    fun removeNode(target: TriangleK) {
        if (nodeTriangle[0] === target) {
            nodeTriangle[0] = null
            parentNumber_  = -1
        }
        if (nodeTriangle[1] === target) nodeTriangle[1] = null
        if (nodeTriangle[2] === target) nodeTriangle[2] = null
    }

    fun removeTheirNode() {
        if (nodeTriangle[0] != null) nodeTriangle[0]!!.removeNode(this)
        if (nodeTriangle[1] != null) nodeTriangle[1]!!.removeNode(this)
        if (nodeTriangle[2] != null) nodeTriangle[2]!!.removeNode(this)
    }

    fun rotateNode( clockwise : Boolean = true ){

        when( clockwise ){
            true -> {
                val node = nodeTriangle[2]
                nodeTriangle[2] = nodeTriangle[1]
                nodeTriangle[1] = nodeTriangle[0]
                nodeTriangle[0] = node
            }
            false -> {
                val node = nodeTriangle[0]
                nodeTriangle[0] = nodeTriangle[1]
                nodeTriangle[1] = nodeTriangle[2]
                nodeTriangle[2] = node
            }
        }

    }

    fun <T>rotateArray( array :Array<T>, clockwise : Boolean = true ){

        when( clockwise ){
            true -> {
                val node = array[2]
                array[2] = array[1]
                array[1] = array[0]
                array[0] = node
            }
            false -> {
                val node = array[0]
                array[0] = array[1]
                array[1] = array[2]
                array[2] = node
            }
        }

    }

    fun <A>rotArray( array: Array<A>, someFunction: (A) -> Unit ) {
            someFunction(array[0])
    }

    fun autoRotateNode( clockwise : Boolean = true ){
        rotateArray( nodeTriangle, clockwise )
        rotateArray( length, clockwise )
        rotateArray( point, clockwise )

        if( nodeTriangle[1] != null ) setNodeToZero( nodeTriangle[1]!! )
        if( nodeTriangle[2] != null ) setNodeToZero( nodeTriangle[2]!! )
    }

    fun setNodeToZero( target :TriangleK, me :TriangleK = this ){
        if( target.nodeTriangle[1] == me ) target.autoRotateNode( false )
        if( target.nodeTriangle[2] == me ) target.autoRotateNode(  )
    }

    fun isConstant(): Boolean{
        if( nodeTriangle[0] == null ) return false
        return ( myNumber_ - nodeTriangle[0]!!.myNumber_ == 1 )
    }

    val lengthByType: Float
        get() = 0f


    fun calcPoints(ref: TriangleK?, refside: Int, pos: PointXY = PointXY(0f, 0f), angle_: Float = 0f) {
        setCParamFromParentBC( parentSide )

        if( ref != null || refside != -1 ) setNode(ref, refside, true )

        val plist: Array<PointXY?>
        val llist: FloatArray
        val powlist: DoubleArray
        var angle = 0f

        when (refside) {
            -1 -> {
                point[0] = pos
                angle = angle_
                plist = arrayOf(point[0], point[1], point[2])
                llist = floatArrayOf(length[0], length[1], length[2])
                powlist = doubleArrayOf(
                        Math.pow(length[0].toDouble(), 2.0),
                        Math.pow(length[1].toDouble(), 2.0),
                        Math.pow(length[2].toDouble(), 2.0)
                )
            }
            0 -> {
                if( ref == null ) return
                angle = ref.getAngleBySide(this.parentSide)
                plist = arrayOf(getParentPointByType(), point[1], point[2])
                llist = floatArrayOf(length[0], length[1], length[2])
                powlist = doubleArrayOf(
                        Math.pow(length[0].toDouble(), 2.0),
                        Math.pow(length[1].toDouble(), 2.0),
                        Math.pow(length[2].toDouble(), 2.0)
                )
            }
            1 -> {
                if( ref == null ) return
                angle = ref.baseangle + 180f //- nodeTriangle[1].angleInnerCA_;
                plist = arrayOf(ref.getBasePoint(this, ref.point[1], ref.point[0]), point[2], point[0])
                llist = floatArrayOf(length[1], length[2], length[0])
                powlist = doubleArrayOf(
                        Math.pow(length[1].toDouble(), 2.0),
                        Math.pow(length[2].toDouble(), 2.0),
                        Math.pow(length[0].toDouble(), 2.0)
                )
                point[1] = plist[0]!!
            }
            2 -> {
                if( ref == null ) return
                angle = ref.baseangle + 180f //- nodeTriangle[1].angleInnerCA_;
                plist = arrayOf(ref.getBasePoint(this, ref.point[1], ref.point[0]), point[0], point[1])
                llist = floatArrayOf(length[2], length[0], length[1])
                powlist = doubleArrayOf(
                        Math.pow(length[2].toDouble(), 2.0),
                        Math.pow(length[0].toDouble(), 2.0),
                        Math.pow(length[1].toDouble(), 2.0)
                )
                point[2] = plist[0]!!
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
        if (refside == 1) this.baseangle = nodeTriangle[1]!!.baseangle - innerangle[0]
        if (refside == 2) this.baseangle = nodeTriangle[2]!!.baseangle + innerangle[0]

        setCenterAndBoundsAndDimPoints()
    }

    private fun calcPoints(pCA: PointXY?, angle: Float) {
        point[1][(pCA!!.x + length[0] * Math.cos(Math.toRadians(angle.toDouble()))).toFloat()] =
                (pCA.y + length[0] * Math.sin(Math.toRadians(angle.toDouble()))).toFloat()
        myTheta_ = Math.atan2((pCA.y - point[1].y).toDouble(), (pCA.x - point[1].x).toDouble())
        myPowA_ = Math.pow(length[0].toDouble(), 2.0)
        myPowB_ = Math.pow(length[1].toDouble(), 2.0)
        myPowC_ = Math.pow(length[2].toDouble(), 2.0)
        myAlpha_ = Math.acos((myPowA_ + myPowB_ - myPowC_) / (2 * length[0] * length[1]))
        point[2][(point[1].x + length[1] * Math.cos(myTheta_ + myAlpha_)).toFloat()] =
                (point[1].y + length[1] * Math.sin(myTheta_ + myAlpha_)).toFloat()
        innerangle[1] = calculateInternalAngle(point[0], point[1], point[2]).toFloat()
        innerangle[2] = calculateInternalAngle(point[1], point[2], point[0]).toFloat()
        innerangle[0] = calculateInternalAngle(point[2], point[0], point[1]).toFloat()

        setCenterAndBoundsAndDimPoints()
    }

    fun calculateInternalAngle(p1: PointXY?, p2: PointXY?, p3: PointXY?): Double {
        val v1 = p1!!.subtract(p2)
        val v2 = p3!!.subtract(p2)
        val angleRadian = Math.acos(v1.innerProduct(v2) / (v1.magnitude() * v2.magnitude()))
        return angleRadian * 180 / Math.PI
    }

    fun calcMyAngles() {
        innerangle[1] = calculateInternalAngle(point[0], point[1], point[2]).toFloat()
        innerangle[2] = calculateInternalAngle(point[1], point[2], point[0]).toFloat()
        innerangle[0] = calculateInternalAngle(point[2], point[0], point[1]).toFloat()
    }

    fun setCenterAndBoundsAndDimPoints(){
        pointCenter_[(point[1].x + point[2].x + point[0].x) / 3] =
                (point[1].y + point[2].y + point[0].y) / 3
        setMyBound()
        if (isPointNumberMoved_ == false) autoAlignPointNumber()
        dimPointA_ = point[0].calcMidPoint(point[1]) //.crossOffset(point[2], 0.2f*scale_);
        dimPointB_ = point[1].calcMidPoint(point[2])
        dimPointC_ = point[2].calcMidPoint(point[0])
        setDimPoint()
        dimAngleB_ = angleMpAB
        dimAngleC_ = angleMmCA
    }

    fun getBasePoint(t: TriangleK, point1: PointXY, point2: PointXY) :PointXY {

        val length = t.getLengthBySide(cParam_.side)

        when( cParam_.type ){
            0 -> return point1
            1 -> { //nijyuudanmen
                when (cParam_.lcr) {
                    0 -> return point1
                    1 -> return point1.offset(point2, (this.length[0] - length) * 0.5f)
                    2 -> return point1.offset(point2, this.length[0] - length)
                }
            }
            2 -> { // float connections
                when (cParam_.lcr) {
                    0 -> return point1.crossOffset(point2, -1.0f)
                    1 -> return point1.offset(point2, (this.length[0] - length) * 0.5f).crossOffset(point2, -1.0f)
                    2 -> return point1.offset(point2, this.length[0] - length).crossOffset(point2, -1.0f)
                }
            }
        }

        return point1
    }

    fun getPointByCParam(cparam: ConnParam, prnt: TriangleK): PointXY {
        val cside = cparam.side
        val ctype = cparam.type
        val clcr = cparam.lcr
        //pp.add( getPointBy( pp, length[0], clcr ) );
        return getPointBySide(cside)
    }

    fun getPointBy(p: PointXY, la: Float, lcr: Int): PointXY {
        return if (lcr == 2) p else p
        //        if( lcr == 1 ) return p.offset(  );
    }

    fun getPointByBackSide(i: Int): PointXY? {
        if (getSideByIndex(i) === "B") return point[1]
        return if (getSideByIndex(i) === "C") point[2] else null
    }

    fun getLengthBySide(i: Int): Float {
        if (i == 1) return length[1]
        return if (i == 2) length[2] else 0f
    }

    fun getPointBySide(i: Int): PointXY {
        if (getSideByIndex(i) === "B") return point[2]
        return if (getSideByIndex(i) === "C") point[0] else PointXY(0f, 0f)
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

    fun getParentPointByType(cParam: ConnParam = this.cParam_): PointXY {
        return getParentPointByType(cParam.side, cParam.type, cParam.lcr)
    }

    fun getParentPointByType(pbc: Int, conntype: Int, lcr :Int = -1 ): PointXY {
        return if (nodeTriangle[0] == null) PointXY(0f, 0f)
        else when (conntype) {
            1 -> getParentPointByLCR(pbc)
            2 -> getParentPointByLCR(
                    pbc
            ).crossOffset(nodeTriangle[0]!!.getPointByBackSide(pbc), -1.0f)
            else -> nodeTriangle[0]!!.getPointBySide(pbc)
        }!!
    }

    fun getParentPointByLCR(pbc: Int): PointXY {
        if (nodeTriangle[0] == null) return PointXY(0f, 0f)
        when (pbc) {
            1 -> when (cParam_.lcr) {
                0 -> return nodeTriangle[0]!!.point[1].offset(nodeTriangle[0]!!.point[2], length[0])
                1 -> return getParentOffsetPointBySide(pbc)
                2 -> return nodeTriangle[0]!!.point[2].clone()
            }
            2 -> when (cParam_.lcr) {
                0 -> return nodeTriangle[0]!!.point[2].offset(nodeTriangle[0]!!.point[0], length[0])
                1 -> return getParentOffsetPointBySide(pbc)
                2 -> return nodeTriangle[0]!!.point[0].clone()
            }
        }
        return PointXY(0f, 0f)
    }

    fun getParentOffsetPointBySide(pbc: Int): PointXY {
        if (nodeTriangle[0] == null) return PointXY(0f, 0f)
        when (pbc) {
            1 -> return nodeTriangle[0]!!.point[1].offset(
                    nodeTriangle[0]!!.point[2],
                    nodeTriangle[0]!!.length[1] * 0.5f + length[0] * 0.5f
            )
            2 -> return nodeTriangle[0]!!.point[2].offset(
                    nodeTriangle[0]!!.point[0],
                    nodeTriangle[0]!!.length[2] * 0.5f + length[0] * 0.5f
            )
        }
        return nodeTriangle[0]!!.getPointBySide(pbc)
    }

    override fun getArea(): Float {
        setForcelengthes()
        val sumABC = lengthAforce_ + lengthBforce_ + lengthCforce_
        val myArea =
            sumABC * 0.5f * (sumABC * 0.5f - lengthAforce_) * (sumABC * 0.5f - lengthBforce_) * (sumABC * 0.5f - lengthCforce_)
        //myArea = roundByUnderTwo( myArea );
        return roundByUnderTwo(Math.pow(myArea.toDouble(), 0.5).toFloat())
    }

    override fun getParams(): Params {
        return Params(
                myName_, "", myNumber_, length[0], length[1], length[2],
                parentNumber_,
                parentBC_, point[0], pointCenter_
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

    fun setConnectionType(cParam: ConnParam) {
        //myParentBC_= cParam.getSide();
        parentNumber_ = nodeTriangle[0]!!.myNumber_
        connectionType_ = cParam.type
        connectionLCR_ = cParam.lcr
        cParam_ = cParam.clone()
        when (cParam.side) {
            1 -> baseangle = nodeTriangle[0]!!.angleMpAB
            2 -> baseangle = nodeTriangle[0]!!.angleMmCA
        }
        when (cParam.type) {
            0 -> if (cParam.lenA != 0.0f) {
                length[0] = cParam.lenA
                lengthAforce_ = cParam.lenA
                point[0] = getParentPointByType(cParam.side, cParam.type, cParam.lcr)
            } else {
                length[0] = nodeTriangle[0]!!.getLengthBySide(cParam.side)
                lengthAforce_ = nodeTriangle[0]!!.getLengthBySide(cParam.side)
                point[0] = nodeTriangle[0]!!.getPointByCParam(cParam, nodeTriangle[0]!!)
            }
            else -> {
                if (cParam.lenA != 0.0f) {
                    length[0] = cParam.lenA
                    lengthAforce_ = cParam.lenA
                }
                point[0] = getParentPointByType(cParam.side, cParam.type, cParam.lcr)
            }
        }
    }

    fun setParentBCFromCLCR() {
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        if (cParam_.type == 2) return
        when (cParam_.lcr) {
            0 -> when (cParam_.side) {
                1 -> parentBC_ = 4
                2 -> parentBC_ = 6
            }
            1 -> when (cParam_.side) {
                1 -> parentBC_ = 7
                2 -> parentBC_ = 8
            }
            2 -> when (cParam_.side) {
                1 -> parentBC_ = 3
                2 -> parentBC_ = 5
            }
        }
    }

    fun setBasePoint(cParam: ConnParam): PointXY {
        point[0] = getParentPointByType(cParam.side, cParam.type, cParam.lcr)
        connectionType_ = cParam.type
        connectionLCR_ = cParam.lcr
        calcPoints(point[0], baseangle)
        return point[0]
    }

    fun setBasePoint(pbc: Int, pct: Int, lcr: Int): PointXY {
        point[0] = getParentPointByType(pbc, pct, lcr)
        connectionType_ = pct
        connectionLCR_ = lcr
        calcPoints(point[0], baseangle)
        return point[0]
    }

    fun setParent(parent: TriangleK, pbc: Int) {
        nodeTriangle[0] = parent.clone()
        //myParentBC_ = pbc;
    }

    fun setCParamFromParentBC(pbc: Int) {
        var curLCR = cParam_.lcr
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        //if (cParam_.side == 0 && (pbc == 4 || pbc == 6)) curLCR = 0
        //if (cParam_.side == 0 && (pbc == 7 || pbc == 8)) curLCR = 1
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
            myDimAlignB_ = if (nodeTriangle[1] == null) 1 else 3
        }
        if (isChangeDimAlignC_ == false) {
            myDimAlignC_ = if (nodeTriangle[2] == null) 1 else 3
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
        var lb = point[0].min(point[1])
        lb = point[1].min(point[2])
        myBP_.left = lb.x
        myBP_.bottom = lb.y
        var rt = point[0].max(point[1])
        rt = point[1].max(point[2])
        myBP_.right = rt.x
        myBP_.top = rt.y
    }

    fun setForcelengthes(){
        val scaleback = 1 / scale_
        lengthAforce_ = length[0] * scaleback
        lengthBforce_ = length[1] * scaleback
        lengthCforce_ = length[2] * scaleback
    }

    fun setNumber(num: Int) {
        myNumber_ = num
    }

    fun setScale(scale: Float) {
        scale_ = scale
        length[0] *= scale
        length[1] *= scale
        length[2] *= scale
        calcPoints(point[0], baseangle)
    }

    //maybe not use.
    private fun setParentInfo(myParNum: Int, myParBC: Int, myConne: Int) {
        parentNumber_ = myParNum
        parentBC_ = myParBC
    }

    fun setDimPoint() {
        dimPointA_ =
                dimSideRotation(dimSideAlignA_, point[0].calcMidPoint(point[1]), point[1], point[0])
        dimPointB_ =
                dimSideRotation(dimSideAlignB_, point[1].calcMidPoint(point[2]), point[2], point[1])
        dimPointC_ =
                dimSideRotation(dimSideAlignC_, point[2].calcMidPoint(point[0]), point[0], point[2])
    }

    fun setPointNumberMoved_(p: PointXY) {
        pointNumber_ = p
        isPointNumberMoved_ = true
    }

    fun setDimPath(ts: Float) {
        dimH_ = ts
        pathA_ = PathAndOffset(
                scale_,
                point[1], point[0],
                point[2], lengthAforce_, myDimAlignA_, dimSideAlignA_, dimH_
        )
        pathB_ = PathAndOffset(
                scale_,
                point[2], point[1],
                point[0], lengthBforce_, myDimAlignB_, dimSideAlignB_, dimH_
        )
        pathC_ = PathAndOffset(
                scale_,
                point[0], point[2],
                point[1], lengthCforce_, myDimAlignC_, dimSideAlignC_, dimH_
        )
        pathS_ = PathAndOffset(
                scale_, point[1],
                point[0], point[2], lengthAforce_, 4, 0, dimH_
        )

        //sla_ = formattedString( lengthAforce_, 3);
        //slb_ = formattedString( lengthBforce_, 3);
        //slc_ = formattedString( lengthCforce_, 3);
    }

    val parentSide: Int
        get() {
            val i = parentBC_
            if (i == 1 || i == 3 || i == 4 || i == 7 || i == 9) return 1
            return if (i == 2 || i == 5 || i == 6 || i == 8 || i == 10) 2 else 0
        }

    fun getPbc(pbc: Int): Int {
        if (pbc == 1 || pbc == 3 || pbc == 4 || pbc == 7 || pbc == 9) return 1
        return if (pbc == 2 || pbc == 5 || pbc == 6 || pbc == 8 || pbc == 10) 2 else 0
    }

    val angleMmCA: Float
        get() = baseangle - innerangle[0]
    val angleMpAB: Float
        get() = baseangle + innerangle[1]
    val angleMpBC: Float
        get() = baseangle + innerangle[2]

    val parent: TriangleK?
        get() = if (nodeTriangle[0] != null) nodeTriangle[0] else null

    val dimAlignA: Int
        get() = calcDimAlignByInnerAngleOf(0, baseangle)

    val dimAlignB: Int
        get() = calcDimAlignByInnerAngleOf(1, angleMpAB)

    val dimAlignC: Int
        get() = calcDimAlignByInnerAngleOf(2, angleMmCA)

    val pointNumberAutoAligned_: PointXY
        get() {
            if (isPointNumberMoved_ == true) return pointNumber_
            if (lengthAforce_ < 2.5f) return pointNumber_.offset(
                    point[2],
                    pointNumber_.vectorTo(point[2]).lengthXY() * -0.3f
            )
            if (lengthBforce_ < 2.5f) return pointNumber_.offset(
                    point[0],
                    pointNumber_.vectorTo(point[0]).lengthXY() * -0.3f
            )
            return if (lengthCforce_ < 2.5f) pointNumber_.offset(
                    point[1],
                    pointNumber_.vectorTo(point[1]).lengthXY() * -0.3f
            ) else pointNumber_
        }


    val isFloating: Boolean
        get() = parentBC_ == 9 || parentBC_ == 10

    companion object {
        fun getPbcByCParam(cp: ConnParam) :Int {
            // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC

            when( cp.type ) {
                0 -> return cp.side // 1 or 2
                1 -> { // nijyuudanmen
                    when (cp.side) {
                        1 -> { //B..
                            when (cp.lcr) {
                                0 -> return 4 //BL
                                1 -> return 7 //BC
                                2 -> return 3 //BR
                            }
                        }
                        2 -> { // C..
                            when (cp.lcr) {
                                0 -> return 6 //CL
                                1 -> return 8 //CC
                                2 -> return 5 //CR
                            }
                        }
                    }
                }
                2 -> { // float connection
                    when (cp.side) {
                        1 -> return 9  //FB
                        2 -> return 10 //FC
                    }
                }
            }

            return -1
        }
    }


    fun hasConstantParent(): Boolean {
        val iscons = myNumber_ - parentNumber_
        return iscons <= 1
    }

    fun crossOffset(pbc: Int): PointXY? {
        if (pbc == 1) return point[0].crossOffset(point[2], 1.0f)
        return if (pbc == 2) point[2].crossOffset(point[1], 1.0f) else point[0]
    }

    fun rotateLCRandGet(): TriangleK {
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

    fun reload() {
        if (nodeTriangle[0] != null) {
            nodeTriangle[0]!!.reload()
            calcPoints(
                    nodeTriangle[0]!!.getPointBySide(parentBC_).also {
                        if (it != null) {
                            point[0] = it
                        }
                    },
                    nodeTriangle[0]!!.getAngleBySide(
                            parentBC_
                    ).also { baseangle = it })
        }
    }

    fun hasChild(): Boolean {
        return nodeTriangle[1] != null || nodeTriangle[2] != null
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
        return if (length[0] <= 0.0f || length[1] <= 0.0f || length[2] <= 0.0f) false else isValidLengthes(
                length[0],
                length[1],
                length[2]
        )
        //!((this.length[0] + this.length[1]) <= this.length[2]) &&
        //      !((this.length[1] + this.length[2]) <= this.length[0]) &&
        //    !((this.length[2] + this.length[0]) <= this.length[1]);
    }

    fun isValidLengthes(A: Float, B: Float, C: Float): Boolean {
        return A + B > C && B + C > A && C + A > B
    }


    fun move(to: PointXY) {
        point[1].add(to)
        point[2].add(to)
        point[0].add(to)
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
        //point[1].scale(basepoint, scale);
        //point[2].scale(basepoint, scale);
        point[0].scale(basepoint, scale)
        pointCenter_.scale(basepoint, scale)
        pointNumber_.scale(basepoint, scale)
        length[0] *= scale
        length[1] *= scale
        length[2] *= scale
        calcPoints(point[0], baseangle)
    }

    fun rotate(basepoint: PointXY?, degree: Float) {
        point[0] = point[0].rotate(basepoint, degree)
        baseangle += degree
        calcPoints(point[0], baseangle)
        //setDimAlign();
    }

    private fun autoAlignPointNumber() {
        if (isPointNumberMoved_ == false) pointNumber_ = pointCenter_ //とりあえず重心にする
    }

    fun autoSetDimAlign(): Int { // 1:下 3:上
        myDimAlignA_ = calcDimAlignByInnerAngleOf(0, baseangle)
        myDimAlignB_ = calcDimAlignByInnerAngleOf(1, angleMpAB)
        myDimAlignC_ = calcDimAlignByInnerAngleOf(2, angleMmCA)

        setDimPath(dimH_)
        return myDimAlign_
    }

    fun calcDimAlignByInnerAngleOf(ABC: Int, angle: Float): Int {    // 夾角の、1:外 　3:内
        if (ABC == 0) {
            if (parentBC_ == 9 || parentBC_ == 10) return 1
            if (parentBC_ > 2 || nodeTriangle[1] != null || nodeTriangle[2] != null) return 3
        }
        if (ABC == 1 && nodeTriangle[1] != null) return 1
        return if (ABC == 2 && nodeTriangle[2] != null) 1 else 3
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
            pf = length[0]
            length[0] = length[1]
            length[1] = length[2]
            length[2] = pf
            pf = lengthAforce_
            lengthAforce_ = lengthBforce_
            lengthBforce_ = lengthCforce_
            lengthCforce_ = pf
            pp = point[0].clone()
            point[0] = point[1]
            point[1] = point[2]
            point[2] = pp.clone()
            pf = innerangle[0]
            innerangle[0] = innerangle[1]
            innerangle[1] = innerangle[2]
            innerangle[2] = pf
            baseangle = angleMmCA - innerangle[1]
            if (baseangle < 0) baseangle += 360f
            if (baseangle > 360) baseangle -= 360f
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
            pf = lengthAforce_
            lengthAforce_ = lengthCforce_
            lengthCforce_ = lengthBforce_
            lengthBforce_ = pf
            pp = point[0].clone()
            point[0] = point[2]
            point[2] = point[1]
            point[1] = pp.clone()
            val ga = baseangle
            pf = innerangle[0]
            innerangle[0] = innerangle[2]
            innerangle[2] = innerangle[1]
            innerangle[1] = pf
            baseangle += innerangle[0] + innerangle[2]
            if (baseangle < 0) baseangle += 360f
            if (baseangle > 360) baseangle -= 360f
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
        return p.isCollide(point[1], point[2], point[0])
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

    fun alreadyHaveChild(pbc: Int): Boolean {
        if (pbc < 1) return false
        return if (getSideByIndex(pbc) === "B" && isChildB_ == true) true else getSideByIndex(pbc) === "C" && isChildC_ == true
    }

    fun hasChildIn(cbc: Int): Boolean? {
        return if ((nodeTriangle[1] != null || isChildB_ == true) && cbc == 1) true else (nodeTriangle[2] != null || isChildC_ == true) && cbc == 2
    }

    fun setChild(newchild: TriangleK, cbc: Int) {
        childSide_ = cbc
        if (newchild.getPbc(cbc) == 1) {
            nodeTriangle[1] = newchild
            isChildB_ = true
        }
        if (newchild.getPbc(cbc) == 2) {
            nodeTriangle[2] = newchild
            isChildC_ = true
        }
        setDimAlignByChild()
    }

    @JvmName("setChildSide_1")
    fun setChildSide_(childside: Int) {
        childSide_ = childside
    }


    fun setReverseDefSide(pbc: Int, BtoC: Boolean) {
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        if (BtoC == false) {
            parentBC_ = if (pbc == 3) 4 else if (pbc == 4) 3 else if (pbc == 5) 6 else if (pbc == 6) 5 else if (pbc == 9) 9 else if (pbc == 10) 10 else pbc
        }
        if (BtoC == true) {
            parentBC_ = if (pbc == 3) 6 else if (pbc == 4) 5 else if (pbc == 5) 4 else if (pbc == 6) 3 else if (pbc == 9) 10 else if (pbc == 10) 9 else -pbc + 3
        }
    }

}