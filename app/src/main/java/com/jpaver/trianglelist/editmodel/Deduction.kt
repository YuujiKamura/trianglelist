package com.jpaver.trianglelist.editmodel

import com.jpaver.trianglelist.angleUnconnectedSide
import com.jpaver.trianglelist.pointUnconnectedSide
import com.jpaver.trianglelist.viewmodel.DeductionParams
import com.jpaver.trianglelist.viewmodel.InputParameter
import com.jpaver.trianglelist.viewmodel.Cloneable

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
                var point: com.example.trilib.PointXY = com.example.trilib.PointXY(
                    0f,
                    0f
                ),
                var pointFlag: com.example.trilib.PointXY = com.example.trilib.PointXY(
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

    constructor(dp: InputParameter) :this(
        num = dp.number,
        name = dp.name,
        lengthX = dp.a,
        lengthY = dp.b,
        overlap_to = dp.pn,
        type = dp.type,
        angle = 0f,
        point =  dp.point ,
        pointFlag = dp.pointflag

    )

    var myscale = 1f
    var shapeAngle = 0f
    lateinit var pLTop: com.example.trilib.PointXY
    lateinit var pLBtm: com.example.trilib.PointXY
    lateinit var pRTop: com.example.trilib.PointXY
    lateinit var pRBtm: com.example.trilib.PointXY
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

            pLTop = com.example.trilib.PointXY(0f, 0f)
            pLBtm = com.example.trilib.PointXY(0f, 0f)
            pRTop = com.example.trilib.PointXY(0f, 0f)
            pRBtm = com.example.trilib.PointXY(0f, 0f)
        }

        //tri?.let { isCollide(it) } //旗上げ処理

        infoStr = getInfo()
    }

    fun setBox(scale: Float){

        myscale = scale
        val mx = point.x - lengthX * scale * 0.5f
        val my = point.y - lengthY * scale * 0.5f
        val px = point.x + lengthX * scale * 0.5f
        val py = point.y + lengthY * scale * 0.5f
        pLTop = com.example.trilib.PointXY(mx, my).rotate(point, shapeAngle)
        pLBtm = com.example.trilib.PointXY(mx, py).rotate(point, shapeAngle)
        pRTop = com.example.trilib.PointXY(px, my).rotate(point, shapeAngle)
        pRBtm = com.example.trilib.PointXY(px, py).rotate(point, shapeAngle)
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
            b.pLTop = pLTop
            b.pLBtm = pLBtm
            b.pRTop = pRTop
            b.pRBtm = pRBtm
            b.shapeAngle = shapeAngle
            b.sameDedcount = sameDedcount
            b.infoStr = infoStr

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return b
    }

    fun set(params: InputParameter){
        num = params.number
        name = params.name
        lengthX = params.a
        lengthY = params.b
        overlap_to = params.pn
        type = params.type
        angle = 0f
        if( params.point.x != 0f && params.point.y != 0f ) point =  params.point
        pointFlag =  params.pointflag
        infoStr = getInfo()
    }

    fun getTap( tapP: com.example.trilib.PointXY): Boolean{

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

    fun move(to: com.example.trilib.PointXY){
        point.add(to)
        pointFlag.add(to)
    }

    fun scale(basepoint: com.example.trilib.PointXY, sx: Float, sy: Float) {
        point = point.scale(basepoint, sx, sy)
        pointFlag = pointFlag.scale(basepoint, sx, sy)
        myscale = sx

        if(type == "Box"){
            pLTop = pLTop.scale( basepoint, sx, sy )
            pLBtm = pLBtm.scale( basepoint, sx, sy )
            pRTop = pRTop.scale( basepoint, sx, sy )
            pRBtm = pRBtm.scale( basepoint, sx, sy )
        }
     //   lengthX *= scale
       // lengthY *= scale
    }

    fun rotate(bp: com.example.trilib.PointXY, degree: Float){
        rotateShape( bp, degree )

        point = point.rotate(bp, degree)
        pointFlag = pointFlag.rotate(bp, degree)

    }

    fun rotateShape(bp: com.example.trilib.PointXY, degree: Float ){
        if(type == "Box"){
            pLTop = pLTop.rotate(bp, degree)
            pLBtm = pLBtm.rotate(bp, degree)
            pRTop = pRTop.rotate(bp, degree)
            pRBtm = pRBtm.rotate(bp, degree)
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

    override fun getParams() : InputParameter {

        return InputParameter(name, type, num, lengthX, lengthY,0f, overlap_to, typeToInt(type), point, pointFlag)
    }

    fun flag(tri: Triangle): Boolean{
        //if(!tri.isCollide( point )) return false

        pointFlag = tri.pointUnconnectedSide( point.scale(1f,-1f), 1f ).scale(1f,-1f)
        shapeAngle = tri.angleUnconnectedSide()

        return true
    }

    override fun toString(): String {
        return "Deduction $num $name $point $pointFlag $typestring $infoStr"
    }
}