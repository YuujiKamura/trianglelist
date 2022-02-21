package com.jpaver.trianglelist

data class ConnParam(var side: Int, var type: Int, var lcr: Int, var lenA: Float ){
     fun clone(): ConnParam {
        val b = ConnParam(side, type, lcr, lenA)
        return b
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

    var scale_ = 1f
    var shapeAngle_ = 0f
    lateinit var plt: PointXY
    lateinit var plb: PointXY
    lateinit var prt: PointXY
    lateinit var prb: PointXY
    var info_: String

    var distanceInPCA = 0f
    var angleInParent = 0f


    init{

        if(type == "Box"){
            setBox( scale_ )
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

        info_ = getInfo()
    }

    fun setInfo() {
        info_ = getInfo()
    }

    fun setBox(scale: Float){
        scale_ = scale
        plt = PointXY(point.x -lengthX*scale_*0.5f, point.y -lengthY*scale_*0.5f ).rotate(point, shapeAngle_)
        plb = PointXY(point.x -lengthX*scale_*0.5f, point.y +lengthY*scale_*0.5f ).rotate(point, shapeAngle_)
        prt = PointXY(point.x +lengthX*scale_*0.5f, point.y -lengthY*scale_*0.5f ).rotate(point, shapeAngle_)
        prb = PointXY(point.x +lengthX*scale_*0.5f, point.y +lengthY*scale_*0.5f ).rotate(point, shapeAngle_)
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
            b.scale_ = scale_
            b.plt = plt
            b.plb = plb
            b.prt = prt
            b.prb = prb
            b.shapeAngle_ = shapeAngle_
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
        info_ = getInfo()
    }

    fun getTap( tapP: PointXY ): Boolean{

        val range = 0.5f * scale_
        if( tapP.nearBy(point, lengthX*scale_) || tapP.nearBy(pointFlag, range) ) return true

        return false
    }

    fun getInfo(): String{
        var str = num.toString() + "." + name
        if( sameDedcount > 1 ) str += "(${sameDedcount})"
        if(type == "Circle") {
            val faif: Float = lengthX * 1000
            val fai: Int = faif.toInt()
            str += " φ"+fai
        }
        if(type == "Box") {
            str += " " + lengthX + " * " + lengthY
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
        scale_ = sx

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
            shapeAngle_ += degree
        }
    }

    override fun getArea() :Float {
        var area = 0.0f
        if( type == "Circle" ) {
            area = (lengthX/2) * (lengthX/2) * 3.14f
        }
        if( type == "Box" ) { area = lengthX * lengthY }
        //area = area.formattedString(2).toFloat() //roundByUnderTwo( ( Math.round(area.toDouble() * 100.0) / 100.0).toFloat() )//roundByUnderTwo(area)        return area
        return Math.round( area * 100 ).toFloat() * 0.01f
    }

    fun typeToInt(type: String) :Int{
        var pl = 0
        if(type == "Box") pl = 1
        if(type == "Circle") pl = 2
        return pl
    }

    fun typeFromInt(type: Int) :String{
        var pl = ""
        if(type == 1) pl = "Box"
        if(type == 2) pl = "Circle"
        return pl
    }


    fun verify( dp: Params ): Boolean{
        if( name == dp.name && lengthX == dp.a && lengthY == dp.b) return true
        return false
    }

    override fun getParams() :Params{
        val pr = Params(name,type,num,lengthX,lengthY,0f,parentNum,typeToInt(type),point,pointFlag)

        return pr
    }

    fun isCollide( tri: Triangle ): Boolean{
        if( tri.isCollide( point ) == false ) return false

        distanceInPCA = tri.pointCA_.lengthTo( point )

        return true
    }
}