package com.jpaver.trianglelist

class PathAndOffset(
    private val myScale: Float,
    p1_: PointXY,
    p2_: PointXY,
    align: Int,
    var alignSide: Int,
    private var dimH: Float
) {

    var pointA: PointXY
    var pointB: PointXY
    var offsetV = 0f
    var offsetH = 0f
    var textSpacer = 5f

    init {
        var p1 = p1_.clone()
        var p2 = p2_.clone()
        val p3: PointXY
        PointXY(0f,0f)
        setOffset(0, align)

        //val vlP2P3 = p2_.vectorTo(p3_).lengthXY()
        //val vlP1P3 = p1_.vectorTo(p3_).lengthXY()


/*        if( vlP2P3 < 2.0f*myScale_ ) offsetH_ = p1_.vectorTo(p2_).lengthXY()*haba
        if( vlP1P3 < 2.0f*myScale_ ) offsetH_ = -p1_.vectorTo(p2_).lengthXY()*haba
        if( vlP2P3 < 1.0f*myScale_ ) offsetH_ = p1_.vectorTo(p2_).lengthXY()*haba*2
        if( vlP1P3 < 1.0f*myScale_ ) offsetH_ = -p1_.vectorTo(p2_).lengthXY()*haba*2
*/


        p1.lengthTo(p2)*2f
        //if(length_ < 2.0){
            // 2mより短い場合はパスを広げる
            //p1.set(p1.offset(p2, -len ))// )
            //p2.set(p2.offset(p1, -len ))//p2.lengthTo(p1)*-1.5f )
        //}
        val lineLength = p1.vectorTo(p2).lengthXY()
        // 幅寄せ
        val haba = lineLength*0.275f

        val hata = 4f*myScale
        val sukima = 0.8f*myScale

        if( this.alignSide == 1 ) offsetH = -haba
        if( this.alignSide == 2 ) offsetH = haba
        if( this.alignSide == 3 ) {
            p1 = p1.offset(p2, -hata )
            p2 = p2.offset(p1, sukima+lineLength)
            offsetH = -0.5f*myScale
        }
        if( this.alignSide == 4 ){
            p2 = p2.offset(p1, -hata )
            p1 = p1.offset(p2, sukima+lineLength)
            offsetH = 0.5f*myScale
        }

/*
        if(length < 1.0f){  // 短い辺の寸法を立てる
            p3 = p1.calcMidPoint(p2).offset(p3_, -0.3f*myScale_)
            p4 = p3.offset(p3_, -1.3f*myScale_)
            p1 = p3
            p2 = p4
        }
*/

        pointA = p1
        pointB = p2

        // 上下逆さまにならない様に反転
        if(p1.x >= p2.x && align != 4) flipPassAndOffset(align, p1, p2)

        if(align == 4){ // 線の左側、というか進行方向の左側

            p3 = p1.offset(p2, -3f* this.myScale)// 一時変数
            p2 = p1.offset(p2, -0.5f* this.myScale)
            p1 = p3

            setPointAB( p1, p2 )
            if( pointA.y < pointB.y ) pointA.flip( pointB )
        }
    }

    private fun setPointAB(p1: PointXY, p2: PointXY ){
        pointA = p1
        pointB = p2
    }

    private fun setOffset(flipside: Int, align: Int){
        val offsetUpper = -dimH * 0.2f //- textSpacer_ //* 0.7f
        val offsetLower =  dimH * 0.9f //textSpacer_ ///* 0.7f )
        //var offsetMiddle= dimH_/2
        if( flipside == 0 ) { // 夾角の、 1:内、3:外
            if (align == 3) offsetV = offsetUpper
            if (align == 1) offsetV = offsetLower
        }
        if( flipside == 1 ) { // 夾角の、 1:外、3:内
            if (align == 1) offsetV = offsetUpper
            if (align == 3) offsetV = offsetLower
        }
        //if(length < 1.0f) offsetV_ = offsetMiddle
    }



    private fun flipPassAndOffset(align: Int, p1: PointXY, p2: PointXY) {

        setOffset(1, align)
        offsetH = -offsetH

        pointA = p2
        pointB = p1
    }

}