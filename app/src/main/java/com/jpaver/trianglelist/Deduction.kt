package com.jpaver.trianglelist

import kotlin.math.roundToInt

data class ConnParam(var side: Int, var type: Int, var lcr: Int, var lenA: Float ){
     fun clone(): ConnParam {
         return ConnParam(side, type, lcr, lenA)
    }
}

class Deduction(var num: Int = 0,
                 var name: String = "",
                 var lengthX: Float = 0f,
                 var lengthY: Float = 0f,
                 var parentNum: Int = 0,
                 var type: String = "",
                 var angle: Float = 0f,
                 var point: PointXY = PointXY(0f, 0f),
                 var pointFlag: PointXY = PointXY(0f, 0f)) :EditObject() {

    constructor(ddp: DeductionParams) :this(
        num = ddp.num,
        name = ddp.name,
        lengthX = ddp.lengthX,
        lengthY = ddp.lengthY,
        parentNum = ddp.parentNum,
        type = ddp.type,
        angle = ddp.angle,
        point =  ddp.point
    )

    constructor(dp: Params) :this(
        num = dp.n,
        name = dp.name,
        lengthX = dp.a,
        lengthY = dp.b,
        parentNum = dp.pn,
        type = dp.type,
        angle = 0f,
        point =  dp.pt ,
        pointFlag = dp.pts

    )

    var myscale = 1f
    var shapeAngle = 0f
    lateinit var plt: PointXY
    lateinit var plb: PointXY
    lateinit var prt: PointXY
    lateinit var prb: PointXY
    var infoStr: String

    private var distanceInPCA = 0f


    init{

        if(type == "Box"){
            setBox( myscale )
//            plt = PointXY(point.getX()-lengthX*scale_*0.5f, point.getY()-lengthY*scale_*0.5f ).rotate(point, shapeAngle_)
  //          plb = PointXY(point.getX()-lengthX*scale_*0.5f, point.getY()+lengthY*scale_*0.5f ).rotate(point, shapeAngle_)
    //        prt = PointXY(point.getX()+lengthX*scale_*0.5f, point.getY()-lengthY*scale_*0.5f ).rotate(point, shapeAngle_)
      //      prb = PointXY(point.getX()+lengthX*scale_*0.5f, point.getY()+lengthY*scale_*0.5f ).rotate(point, shapeAngle_)
        }
        else{
            plt = PointXY( 0f, 0f )
            plb = PointXY( 0f, 0f )
            prt = PointXY( 0f, 0f )
            prb = PointXY( 0f, 0f )
        }

        infoStr = getInfo()
    }

    fun setBox(scale: Float){
        myscale = scale
        plt = PointXY(point.x -lengthX*myscale*0.5f, point.y -lengthY*myscale*0.5f ).rotate(point, shapeAngle)
        plb = PointXY(point.x -lengthX*myscale*0.5f, point.y +lengthY*myscale*0.5f ).rotate(point, shapeAngle)
        prt = PointXY(point.x +lengthX*myscale*0.5f, point.y -lengthY*myscale*0.5f ).rotate(point, shapeAngle)
        prb = PointXY(point.x +lengthX*myscale*0.5f, point.y +lengthY*myscale*0.5f ).rotate(point, shapeAngle)
    }

    public override fun clone(): Deduction {
        var b = Deduction()
        try {
            b = super.clone() as Deduction
            b.num = num
            b.name = name
            b.lengthX = lengthX
            b.lengthY = lengthY
            b.parentNum = parentNum
            b.type = type
            b.angle = angle
            b.point = point
            b.pointFlag = pointFlag
            b.myscale = myscale
            b.plt = plt
            b.plb = plb
            b.prt = prt
            b.prb = prb
            b.shapeAngle = shapeAngle
            b.sameDedcount = sameDedcount

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return b
    }

    fun setParam(dp: Params){
        num = dp.n
        name = dp.name
        lengthX = dp.a
        lengthY = dp.b
        parentNum = dp.pn
        type = dp.type
        angle = 0f
        if( dp.pt.x != 0f && dp.pt.y != 0f ) point =  dp.pt
        pointFlag =  dp.pts
        infoStr = getInfo()
    }

    fun getTap( tapP: PointXY ): Boolean{

        val range = 0.5f * myscale
        if( tapP.nearBy(point, lengthX*myscale) || tapP.nearBy(pointFlag, range) ) return true

        return false
    }

    fun getInfo(): String{
        var str = "$num.$name"
        if( sameDedcount > 1 ) str += "(${sameDedcount})"
        if(type == "Circle") {
            val faif: Float = lengthX * 1000
            val fai: Int = faif.toInt()
            str += " φ$fai"
        }
        if(type == "Box") {
            str += " $lengthX * $lengthY"
        }
        return str
    }

    fun move(to: PointXY){
        point.add(to)
        pointFlag.add(to)
        //plt.add(to)
        //plb.add(to)
        //prt.add(to)
        //prb.add(to)
    }

    fun scale(basepoint: PointXY, sx: Float, sy: Float) {
        point = point.scale(basepoint, sx, sy)
        pointFlag = pointFlag.scale(basepoint, sx, sy)
        myscale = sx

        if(type == "Box"){
            plt = plt.scale( basepoint, sx, sy )
            plb = plb.scale( basepoint, sx, sy )
            prt = prt.scale( basepoint, sx, sy )
            prb = prb.scale( basepoint, sx, sy )
        }
     //   lengthX *= scale
       // lengthY *= scale
    }

    fun rotate(bp: PointXY, degree: Float){
        rotateShape( bp, degree )

        point = point.rotate(bp, degree)
        pointFlag = pointFlag.rotate(bp, degree)

    }

    fun rotateShape( bp: PointXY, degree: Float ){
        if(type == "Box"){
            plt = plt.rotate(bp, degree)
            plb = plb.rotate(bp, degree)
            prt = prt.rotate(bp, degree)
            prb = prb.rotate(bp, degree)
            shapeAngle += degree
        }
    }

    override fun getArea() :Float {
        var area = 0.0f
        if( type == "Circle" ) {
            area = (lengthX/2) * (lengthX/2) * 3.14f
        }
        if( type == "Box" ) { area = lengthX * lengthY }
        //area = area.formattedString(2).toFloat() //roundByUnderTwo( ( Math.round(area.toDouble() * 100.0) / 100.0).toFloat() )//roundByUnderTwo(area)        return area
        return (area * 100).roundToInt().toFloat() * 0.01f
    }

    private fun typeToInt(type: String) :Int{
        var pl = 0
        if(type == "Box") pl = 1
        if(type == "Circle") pl = 2
        return pl
    }


    fun verify( dp: Params ): Boolean{
        if( name == dp.name && lengthX == dp.a && lengthY == dp.b) return true
        return false
    }

    override fun getParams() :Params{

        return Params(name, type, num, lengthX, lengthY,0f, parentNum, typeToInt(type), point, pointFlag)
    }

    fun isCollide( tri: Triangle ): Boolean{
        if(!tri.isCollide( point )) return false

        distanceInPCA = tri.pointCA_.lengthTo( point )

        return true
    }
}