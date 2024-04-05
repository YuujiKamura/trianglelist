package com.jpaver.trianglelist

import com.jpaver.trianglelist.util.DeductionParams
import com.jpaver.trianglelist.util.Params
import com.jpaver.trianglelist.util.Cloneable

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
                var overlap_to: Int = 0,
                var type: String = "",
                var angle: Float = 0f,
                var point: PointXY = PointXY(
                    0f,
                    0f
                ),
                var pointFlag: PointXY = PointXY(
                    0f,
                    0f
                )
) : EditObject(), Cloneable<Deduction> {

    constructor(ddp: DeductionParams) :this(
        num = ddp.num,
        name = ddp.name,
        lengthX = ddp.lengthX,
        lengthY = ddp.lengthY,
        overlap_to = ddp.parentNum,
        type = ddp.type,
        angle = ddp.angle,
        point =  ddp.point
    )

    constructor(dp: Params) :this(
        num = dp.n,
        name = dp.name,
        lengthX = dp.a,
        lengthY = dp.b,
        overlap_to = dp.pn,
        type = dp.type,
        angle = 0f,
        point =  dp.pt ,
        pointFlag = dp.ptF

    )

    var myscale = 1f
    var shapeAngle = 0f
    lateinit var plt: PointXY
    lateinit var plb: PointXY
    lateinit var prt: PointXY
    lateinit var prb: PointXY
    var infoStr: String
    var typestring: String
    var typenum: Int
    var tri: Triangle?  = null

    init{

        if(type == "Box"){
            setBox( myscale )
            typestring = "長方形"
            typenum = 0
        }
        else{
            typestring = "円"
            typenum = 1

            plt = PointXY(0f, 0f)
            plb = PointXY(0f, 0f)
            prt = PointXY(0f, 0f)
            prb = PointXY(0f, 0f)
        }

        //tri?.let { isCollide(it) } //旗上げ処理

        infoStr = getInfo()
    }

    fun setBox(scale: Float){
        myscale = scale
        plt = PointXY(
            point.x - lengthX * myscale * 0.5f,
            point.y - lengthY * myscale * 0.5f
        ).rotate(point, shapeAngle)
        plb = PointXY(
            point.x - lengthX * myscale * 0.5f,
            point.y + lengthY * myscale * 0.5f
        ).rotate(point, shapeAngle)
        prt = PointXY(
            point.x + lengthX * myscale * 0.5f,
            point.y - lengthY * myscale * 0.5f
        ).rotate(point, shapeAngle)
        prb = PointXY(
            point.x + lengthX * myscale * 0.5f,
            point.y + lengthY * myscale * 0.5f
        ).rotate(point, shapeAngle)
    }

    override fun clone(): Deduction {
        val b = Deduction()
        try {
            b.num = num
            b.name = name
            b.lengthX = lengthX
            b.lengthY = lengthY
            b.overlap_to = overlap_to
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
            b.infoStr = infoStr

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
        overlap_to = dp.pn
        type = dp.type
        angle = 0f
        if( dp.pt.x != 0f && dp.pt.y != 0f ) point =  dp.pt
        pointFlag =  dp.ptF
        infoStr = getInfo()
    }

    fun getTap( tapP: PointXY): Boolean{

        val range = 0.5f * myscale
        if( tapP.nearBy(point, lengthX*myscale) || tapP.nearBy(pointFlag, range) ) return true

        return false
    }

    fun getInfo(): String{
        var str = get_number_name_samecount()

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

    fun get_number_name_samecount(): String {
        var str = "$num.$name"
        if (sameDedcount > 0) str += "(${sameDedcount+1})"

        return str
    }

    fun setInfo(same_count: Int){
        sameDedcount = same_count
        infoStr = getInfo()
    }

    fun setNumAndInfo( num_: Int ){
        num = num_
        infoStr = getInfo()
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

    fun rotateShape(bp: PointXY, degree: Float ){
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


    fun verify( deduction: Deduction): Boolean{
        if( name == deduction.name && lengthX == deduction.lengthX && lengthY == deduction.lengthY) return true
        return false
    }

    override fun getParams() : Params {

        return Params(name, type, num, lengthX, lengthY,0f, overlap_to, typeToInt(type), point, pointFlag)
    }

    fun isCollide( tri: Triangle): Boolean{
        if(!tri.isCollide( point )) return false

        pointFlag = tri.pointUnconnectedSide(point, 1f, 1f, PointXY(0f, 0f))
        shapeAngle = tri.angleUnconnectedSide()

        return true
    }

    override fun toString(): String {
        return "Deduction $num $name $point $pointFlag $typestring $infoStr"
    }
}