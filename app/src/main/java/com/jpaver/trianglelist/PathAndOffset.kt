package com.jpaver.trianglelist

class PathAndOffset(myscale: Float, p1_: PointXY, p2_: PointXY, p3_: PointXY, length: Float, align: Int, alignSide: Int, dimH: Float) {

    val myScale_ = myscale
    var pointA_: PointXY
    var pointB_: PointXY
    var offsetV_ = 0f
    var offsetH_ = 0f
    var align_ = align
    var alignSide_ = alignSide
    var dimH_ = dimH
    val lengthBC= p2_.lengthTo(p3_)
    val length_= length
    var textSpacer_ = 5f

    init {
        var p1 = p1_
        var p2 = p2_
        var p3: PointXY = PointXY(0f,0f)
        var p4: PointXY = PointXY(0f,0f)
        setOffset(0, align, length)

        val vlP2P3 = p2_.vectorTo(p3_).lengthXY()
        val vlP1P3 = p1_.vectorTo(p3_).lengthXY()


/*        if( vlP2P3 < 2.0f*myScale_ ) offsetH_ = p1_.vectorTo(p2_).lengthXY()*haba
        if( vlP1P3 < 2.0f*myScale_ ) offsetH_ = -p1_.vectorTo(p2_).lengthXY()*haba
        if( vlP2P3 < 1.0f*myScale_ ) offsetH_ = p1_.vectorTo(p2_).lengthXY()*haba*2
        if( vlP1P3 < 1.0f*myScale_ ) offsetH_ = -p1_.vectorTo(p2_).lengthXY()*haba*2
*/


        val len = p1_.vectorTo(p2_).lengthXY()*10
        if(length_ < 1.0){
            // 1mより短い場合はパスを広げる
            p1 = p1.offset(p2, -len )//p1.lengthTo(p2)*-1.5f )
            p2 = p2.offset(p1, -len )//p2.lengthTo(p1)*-1.5f )
        }
        val llen = p1_.vectorTo(p2_).lengthXY()
        // 幅寄せ
        val haba = llen*0.002f

        if( alignSide_ == 1 ) offsetH_ = -llen*haba
        if( alignSide_ == 2 ) offsetH_ = llen*haba
/*
        if(length < 1.0f){  // 短い辺の寸法を立てる
            p3 = p1.calcMidPoint(p2).offset(p3_, -0.3f*myScale_)
            p4 = p3.offset(p3_, -1.3f*myScale_)
            p1 = p3
            p2 = p4
        }
*/
        pointA_ = p1
        pointB_ = p2

                // 上下逆さまにならない様に反転
        if(p1.x >= p2.x && align != 4) flipPassAndOffset( align, length, p1, p2)

        if(align == 4){ // 線の左側、というか進行方向の左側

            p3 = p1.offset(p2, -3f*myScale_)// 一時変数
            p2 = p1.offset(p2, -0.5f*myScale_)
            p1 = p3

            setPointAB( p1, p2 )
            if( pointA_.y < pointB_.y ) pointA_.flip( pointB_ )
        }
    }

    fun setPointAB( p1: PointXY, p2: PointXY ){
        pointA_ = p1
        pointB_ = p2
    }

    fun setOffset(flipside: Int, align: Int, length: Float){
        var offsetUpper = -textSpacer_*0.2f
        var offsetLower = dimH_-textSpacer_*0.2f
        var offsetMiddle= dimH_/2
        if( flipside == 0 ) { // 夾角の、 1:内、3:外
            if (align == 3) offsetV_ = offsetUpper
            if (align == 1) offsetV_ = offsetLower
        }
        if( flipside == 1 ) { // 夾角の、 1:外、3:内
            if (align == 1) offsetV_ = offsetUpper
            if (align == 3) offsetV_ = offsetLower
        }
        //if(length < 1.0f) offsetV_ = offsetMiddle
    }



    fun flipPassAndOffset(align: Int, length: Float, p1: PointXY, p2: PointXY) {
        val p3 = p1
        val p1_ = p2
        val p2_ = p3

        setOffset(1, align, length)
        offsetH_ = -offsetH_

        pointA_ = p1_
        pointB_ = p2_
    }

}