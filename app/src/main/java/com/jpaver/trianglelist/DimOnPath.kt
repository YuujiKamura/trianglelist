package com.jpaver.trianglelist

data class DimOnPath(
    private var scale: Float = 1.0f,
    var leftP: PointXY = PointXY(10f,10f),
    var rightP: PointXY = PointXY(10f,10f),
    var vertical: Int = 1,
    var horizontal: Int = 0,
    private var dimheight: Float = 0.05f
) {

    var pointA: PointXY = PointXY(0f,0f)
    var pointB: PointXY = PointXY(0f,0f)
    var dimpoint: PointXY = PointXY(0f,0f)
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
        setVerticalOffset(0, vertical)
        if(vertical != SIDE_SOKUTEN ) initDimPoint(leftP,rightP, vertical)
        if(vertical == SIDE_SOKUTEN ) initSoktenNamePath(leftP, rightP)
        dimpoint = pointA.calcMidPoint(pointB).offset(pointB, offsetH)
    }

    fun initDimPoint( leftP: PointXY,rightP: PointXY, alignVertical: Int ){

        val lineLength = leftP.lengthTo(rightP)
        val HABAYOSE = lineLength*0.275f

        when( horizontal ){
            CENTER  -> {}
            INRIGHT -> offsetH = -HABAYOSE
            INLEFT  -> offsetH = HABAYOSE
            OUTERRIGHT -> {
                initPointsOuter(rightP, leftP, lineLength)
            }
            OUTERLEFT  -> {
                initPointsOuter(leftP, rightP, lineLength)
            }
        }

        // 上下逆さまにならない様に反転
        if( pointA.x >= pointB.x ) flipPassAndOffset(alignVertical, pointA, pointB )

        //standUpShortDim()
    }

    fun initPointsOuter(leftP: PointXY, rightP: PointXY, lineLength: Float){
        val SUKIMA = 0.5f*scale
        val movement = SUKIMA+lineLength
        val HATAAGE = 3*scale
        val HABAYOSE = -lineLength*0.05f

        pointA = leftP.offset(rightP, -HATAAGE )
        pointB = rightP.offset(leftP, movement)
        offsetH = HABAYOSE
    }

    fun initSoktenNamePath(p1: PointXY, p2: PointXY){

        // -だと線の左側、というか進行方向の左側
        val SIDE = horizontal
        var leftpoint = p1
        var rightpoint = p2

        when(SIDE){
            1 -> {
                leftpoint = p2
                rightpoint = p1
            }
        }

        val tmp = leftpoint.offset( rightpoint, -3f * this.scale)
        val outerright = leftpoint.offset(rightpoint, -0.5f * this.scale)
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
        val offsetUpper = -dimheight * 0.2f //- textSpacer_ //* 0.7f
        val offsetLower =  dimheight * 0.9f //textSpacer_ ///* 0.7f )
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