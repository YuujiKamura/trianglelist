package com.jpaver.trianglelist
import com.jpaver.trianglelist.util.Cloneable



class PathAndOffset(
    private var myScale: Float = 1.0f,
    leftP: PointXY = PointXY(0f,0f),
    rightP: PointXY = PointXY(0f,0f),
    alignVertical: Int = 1,
    var alignHorizonal: Int = 1,
    private var dimH: Float = 0.05f
) : Cloneable<PathAndOffset>{

    override fun clone(): PathAndOffset {
        val b = PathAndOffset()
        b.myScale = myScale
        b.pointA = pointA.clone()
        b.pointB = pointB.clone()
        b.pointD = pointD.clone()
        b.offsetV = offsetV
        b.offsetH = offsetH
        b.textSpacer = textSpacer
        return b
    }

    var pointA: PointXY = PointXY(0f,0f)
    var pointB: PointXY = PointXY(0f,0f)
    var pointD: PointXY = PointXY(0f,0f)
    var offsetV = 0f
    var offsetH = 0f
    var textSpacer = 5f

    val CENTER = 0
    val INRIGHT = 1
    val INLEFT = 2
    val OUTERRIGHT = 3
    val OUTERLEFT =  4
    val SIDE_SOKUTEN = 4

    init {
        setPointAB( leftP, rightP )
        setVerticalOffset(0, alignVertical)
        if(alignVertical != SIDE_SOKUTEN ) initDimPoint(leftP,rightP, alignVertical)
        if(alignVertical == SIDE_SOKUTEN ) initSoktenNamePath(leftP, rightP)
        pointD = pointA.calcMidPoint(pointB).offset(pointB, offsetH)
    }

    fun initDimPoint( leftP: PointXY,rightP: PointXY, alignVertical: Int ){

        val lineLength = leftP.lengthTo(rightP)
        val HABAYOSE = lineLength*0.275f

        when( alignHorizonal ){
            CENTER  -> {}
            INRIGHT -> offsetH = -HABAYOSE
            INLEFT  -> offsetH = HABAYOSE
            OUTERRIGHT -> {
                initPointsOuter(1, rightP, leftP, lineLength )
            }
            OUTERLEFT  -> {
                initPointsOuter( 1, leftP, rightP, lineLength )
            }
        }

        // 上下逆さまにならない様に反転
        if( pointA.x >= pointB.x ) flipPassAndOffset(alignVertical, pointA, pointB )

        //standUpShortDim()
    }

    fun initPointsOuter(direction: Int, leftP:PointXY, rightP: PointXY, lineLength: Float){
        val SUKIMA = 0.5f*myScale
        val movement = SUKIMA+lineLength
        val HATAAGE = 3*myScale
        val HABAYOSE = -lineLength*0.05f

        pointA = leftP.offset(rightP, -HATAAGE )
        pointB = rightP.offset(leftP, movement)
        offsetH = HABAYOSE
    }

    fun initSoktenNamePath(p1: PointXY, p2: PointXY){

        // -だと線の左側、というか進行方向の左側
        val SIDE = alignHorizonal
        var leftpoint = p1
        var rightpoint = p2

        when(SIDE){
            1 -> {
                leftpoint = p2
                rightpoint = p1
            }
        }

        val tmp = leftpoint.offset( rightpoint, -3f * this.myScale)
        val outerright = leftpoint.offset(rightpoint, -0.5f * this.myScale)
        val outerleft  = tmp

        setPointAB( outerleft, outerright )
        if( pointA.y < pointB.y ) pointA.flip( pointB )
    }

    fun move(to: PointXY){
        pointA.add(to)
        pointB.add(to)
    }

    private fun setPointAB(p1: PointXY, p2: PointXY){
        pointA = p1
        pointB = p2
    }

    private fun setVerticalOffset(flipside: Int, alignVertical: Int){
        val offsetUpper = -dimH * 0.2f //- textSpacer_ //* 0.7f
        val offsetLower =  dimH * 0.9f //textSpacer_ ///* 0.7f )
        //var offsetMiddle= dimH_/2
        if( flipside == 0 ) { // 夾角の、 1:内、3:外
            if (alignVertical == 3) offsetV = offsetUpper
            if (alignVertical == 1) offsetV = offsetLower
        }
        if( flipside == 1 ) { // 夾角の、 1:外、3:内
            if (alignVertical == 1) offsetV = offsetUpper
            if (alignVertical == 3) offsetV = offsetLower
        }
        //if(length < 1.0f) offsetV_ = offsetMiddle
    }

    private fun flipPassAndOffset(align: Int, p1: PointXY, p2: PointXY) {

        setVerticalOffset(1, align)
        offsetH = -offsetH

        pointA = p2
        pointB = p1
    }

    /*fun standUpShortDim(TO DO)
    if(length < 1.0f){  // 短い辺の寸法を立てる
        p3 = p1.calcMidPoint(p2).offset(p3_, -0.3f*myScale_)
        p4 = p3.offset(p3_, -1.3f*myScale_)
        p1 = p3
        p2 = p4
    }*/

}