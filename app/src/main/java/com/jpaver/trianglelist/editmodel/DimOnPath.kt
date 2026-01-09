package com.jpaver.trianglelist.editmodel

data class DimOnPath(
    private var scale: Float = 1.0f,
    var leftP: com.example.trilib.PointXY = com.example.trilib.PointXY(10f, 10f),
    var rightP: com.example.trilib.PointXY = com.example.trilib.PointXY(10f, 10f),
    var vertical: Int = 1,
    var horizontal: Int = 0,
    private var dimheight: Float = 0.05f
) {

    var pointA: com.example.trilib.PointXY = com.example.trilib.PointXY(0f, 0f)
    var pointB: com.example.trilib.PointXY = com.example.trilib.PointXY(0f, 0f)
    var dimpoint: com.example.trilib.PointXY = com.example.trilib.PointXY(0f, 0f)
    var offsetV = 0f
    var offsetH = 0f
    var textSpacer = 5f

    val CENTER = 0
    val INRIGHT = 1
    val INLEFT = 2
    val OUTERRIGHT = 3
    val OUTERLEFT =  4
    val SIDE_SOKUTEN = 4

    var clockwise = "C"
    val offsetUpper = -dimheight * 0.2f //
    val offsetLower =  dimheight * 0.9f //

    init {
        setPointAB( leftP, rightP )
        if (vertical == 1) offsetV = offsetLower
        if (vertical == 3) offsetV = offsetUpper
        when(vertical){
            SIDE_SOKUTEN -> initSoktenNamePath(leftP,rightP)
            else         -> initDimPoint(leftP, rightP)
        }
        dimpoint = pointA.calcMidPoint(pointB).offset(pointB, offsetH)
    }

    fun initSoktenNamePath(p1: com.example.trilib.PointXY, p2: com.example.trilib.PointXY){

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

    fun initDimPoint(leftP: com.example.trilib.PointXY, rightP: com.example.trilib.PointXY){

        val lineLength = leftP.lengthTo(rightP)
        val HABAYOSE = lineLength*0.1f

        when( horizontal ){
            CENTER  -> {}
            INRIGHT -> offsetH = -HABAYOSE
            INLEFT  -> offsetH = HABAYOSE
            OUTERRIGHT -> {
                initPointsOuter(rightP, leftP, lineLength)
                vertical = flip(vertical)
            }
            OUTERLEFT  -> {
                initPointsOuter(leftP, rightP, lineLength)
            }
        }

        // 上下逆さまにならない様に反転
        if( pointA.x >= pointB.x ){ // 夾角の、 1:外、3:内
            clockwise = "A" // ANTI CLOCKWISE
            offsetH = -offsetH
            val tmp = pointA

            pointA = pointB
            pointB = tmp
            if (vertical == 1) offsetV = offsetUpper
            if (vertical == 3) offsetV = offsetLower
        }
    }

    // 垂直方向の文字位置合わせタイプ(省略可能、既定 = 0): 整数コード(ビットコードではありません):
    // 寸法値から見た基準線の上下
    // 0 = 基準線、1 = 下、2 = 中央、3 = 上
    // ベクトルの方向でB,Cを表現するなら
    // x軸の方向で正負を表す。正の時は下1が内、負の時は上3が内。

    // 挟角の 外:1 内:3　in view
    // 基準線が　下:1 上:3
    val UPPER = 3
    val LOWER = 1
    val INNER = 3
    val OUTER = 1

    fun flip(vertical: Int): Int {
        return when(vertical){
            OUTER -> INNER
            else  -> OUTER
        }
    }

    fun initPointsOuter(leftP: com.example.trilib.PointXY, rightP: com.example.trilib.PointXY, lineLength: Float){
        val SUKIMA = 0.5f*scale
        val movement = SUKIMA+lineLength
        val HATAAGE = -3*scale
        val HABAYOSE = -lineLength*0.05f

        pointA = leftP.offset(rightP, HATAAGE )
        pointB = rightP.offset(leftP, movement)
        offsetH = HABAYOSE
    }

    fun move(to: com.example.trilib.PointXY){
        pointA.add(to)
        pointB.add(to)
    }

    private fun setPointAB(p1: com.example.trilib.PointXY, p2: com.example.trilib.PointXY){
        pointA = p1
        pointB = p2
    }

    fun verticalDxf(): Int{

        //基準線の方向が右向きか左向きかで上下を反転する

        // 外側
        if (vertical == OUTER) {
            // 基準線が右向き の場合
            return if ( rightP.isVectorToRight(leftP) ) LOWER else UPPER
        }

        // 内側
        return if ( rightP.isVectorToRight(leftP) ) UPPER else LOWER
    }
}