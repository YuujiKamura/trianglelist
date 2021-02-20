package com.jpaver.trianglelist

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

open class DrawingFileWriter() {
    var koujiname_: String = ""
    var rosenname_ = ""
    var gyousyaname_ = ""
    var zumennum_ = "1/1"

    val paintTri_: Paint = Paint()
    var paintTexS_: Paint = Paint()
    var paintFill: Paint = Paint()

    lateinit var titleTri_ : TitleParamStr
    lateinit var titleDed_ : TitleParamStr
    lateinit var rStr_ : ResStr

    fun setNames(kn: String, rn: String, gn: String, zn: String){
        koujiname_ = kn
        rosenname_ = rn
        gyousyaname_ = gn
        zumennum_ =zn
    }

/*    open fun setScale(drawingLength: PointXY) :Float{
        var scale = 2f //1/200　x:80m~40m
        paintTexS_.textSize = 5f
        if(drawingLength.x > 80 || drawingLength.y > 55 ) {
            scale = 2.5f
            paintTexS_.setTextSize(8f)
        } //1/250 (x:80m以上

        if(drawingLength.x < 50 && drawingLength.y < 30 ){
            scale = 1.5f
            paintTexS_.textSize = 4f
        } //1/150 (x:40m以下
        return scale
    }*/

    fun Float?.formattedString(fractionDigits:Int): String{
        // nullの場合は空文字
        if(this == null) return ""
        var format : String = "%.${fractionDigits}f"
        return format.format(this)
    }

    open fun writeLine(p1: PointXY, p2: PointXY, color: Int){

    }

    open fun writeRect(point: PointXY, sizeX: Float, sizeY: Float, scale: Float, color: Int){

    }

    open fun writeCircle(){

    }

    open fun writeText(str: String, point: PointXY, scale: Float, color: Int, size: Float, align: Int){

    }

    open fun writeTextOnPath(){

    }

    open fun writeEntities(){

    }

    open fun writeTitlesAndFrames(){

    }
}