package com.jpaver.trianglelist

import android.graphics.Paint

open class DrawingFileWriter {
    open lateinit var trilist_: TriangleList
    open lateinit var dedlist_: DeductionList

    var koujiname_: String = ""
    var rosenname_ = ""
    var gyousyaname_ = ""
    var zumennum_ = "1/1"
    var startTriNumber_ = 1
    val drawscale_ = 1000f
    val viewscale_ = 47.6f
    open var sheetscale_ = 1f

    open var textscale_ = 5f//trilist_.getPrintTextScale( 1f , "dxf") * drawscale_
    open var sizeX_ = 420 * sheetscale_
    open var sizeY_ = 297 * sheetscale_
    open var centerX_ = sizeX_ * 0.5f
    open var centerY_ = sizeY_ * 0.5f

    var cWhite_ = 8
    var cBlue_  = 4
    var cRed_   = 2

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

    fun setStartNumber( num: Int ) :Int{
        if( num > 1 ) startTriNumber_ = num
        return startTriNumber_
    }

    fun Float?.formattedString(fractionDigits:Int): String{
        // nullの場合は空文字
        if(this == null) return ""
        var format : String = "%.${fractionDigits}f"
        return format.format(this)
    }

    open fun save(){}

    open fun writeTriangle(tri: Triangle){}

    open fun writeLine(p1: PointXY, p2: PointXY, color: Int){
    }

    open fun writeRect(point: PointXY, sizeX: Float, sizeY: Float, color: Int){
        val sizex: Float = sizeX/2
        val sizey: Float = sizeY/2
        writeLine( point.plus(-sizex, -sizey), point.plus(sizex, -sizey), color)
        writeLine( point.plus(-sizex, sizey), point.plus(sizex, sizey), color)
        writeLine( point.plus(-sizex, -sizey), point.plus(-sizex, sizey), color)
        writeLine( point.plus(sizex, -sizey), point.plus(sizex, sizey), color)
    }

    open fun writeCircle(point: PointXY, size: Float, color: Int){

    }

    open fun writeText(str: String, point: PointXY, scale: Float, color: Int, size: Float, align: Int){

    }

    // align tenkey ( ex 8 is top and center ) in sfc
    open fun writeText( str: String, point: PointXY, color: Int = 8, tsy: Float, align: Int = 2, angle: Float = 0.0f ){
    }

    // Align H and V ( 0 is center, 1 is left/top, 3 is right/bottom ) in dxf
    open fun writeText( text: String, point: PointXY, color: Int, textsize: Float, alignH: Int, alignV:Int = 0, angle: Float = 0.0f ){

    }

    open fun writeTextOnPath(){

    }

    open fun writeEntities(){

    }

    open fun writeDeduction( ded: Deduction ){}

    open fun writeTextAndLine( st: String, p1: PointXY, p2: PointXY, textsize: Float) {}

    open fun writeFrame( drawscale: Float, sheetscale: Float, centerx: Float, centery: Float, sizeX: Float, sizeY: Float ){
        val yohaku = 50f * drawscale
        val yTitle = 27.1f
        val yRosen = 26f

        writeRect( PointXY( centerx * drawscale, centery * drawscale ),sizeX * drawscale - yohaku,sizeY * drawscale - yohaku,  cWhite_ )

        //writeText( cWhite_, rStr_.tTitle_, PointXY( centerX_, yTitle ), textscale_, 0f, 2 )
        //writeText( cWhite_, rosenname_, PointXY( centerX_, yRosen ), textscale_, 0f, 2 )
/*
        writeLine( PointXY(19f,27f  ), PointXY(23f,27f  ),7 )
        writeLine( PointXY(19f,26.9f), PointXY(23f,26.9f),7 )

        writeLine( PointXY(31f,7.35f), PointXY(41f,7.35f),7 ) //yoko
        writeLine( PointXY(31f,1.35f), PointXY(31f,7.35f),7 ) //tate
        writeLine( PointXY(33f,1.35f), PointXY(33f,7.35f),7 ) //uchi-tate

        writeLine( PointXY(31f,6.35f), PointXY(41f,6.35f),7 )
        writeLine( PointXY(31f,5.35f), PointXY(41f,5.35f), 7 )
        writeLine( PointXY(31f,4.35f), PointXY(41f,4.35f), 7 )
        writeLine( PointXY(31f,3.35f), PointXY(41f,3.35f), 7 )
        writeLine( PointXY(31f,2.35f), PointXY(41f,2.35f), 7 )
        writeLine( PointXY(36f,2.35f), PointXY(36f,3.35f), 7 )
        writeLine( PointXY(38f,2.35f), PointXY(38f,3.35f), 7 )


        val tss = 0.2f

        val st = drawscale_*100f
        val strx = 33.5f

        val xr = strx
        val yb = 6.7f
        val yo = 0.2f
        val uw = 160f
        val tt = 0
        val ofs = 3f

        writeText( rStr_.tCname_, PointXY(32f, 6.7f), 1f, 7, tss, 1)
        writeText( rStr_.tDtype_, PointXY(32f, 5.7f), 1f, 7, tss, 1)
        writeText( rStr_.tDname_, PointXY(32f, 4.7f), 1f, 7, tss, 1)
        writeText( rStr_.tDateHeader_, PointXY(32f, 3.7f), 1f, 7, tss, 1)
        writeText( rStr_.tScale_, PointXY(32f, 2.7f), 1f,7, tss, 1)
        writeText( rStr_.tNum_, PointXY(37f, 2.7f), 1f, 7, tss, 1)
        writeText( rStr_.tAname_, PointXY(32f, 1.7f), 1f, 7, tss, 1)
        writeText( rStr_.tCredit_, PointXY(7f, 1f), 1f, 7, tss, 1 )

        if( koujiname_.length > 20 ) {
            if( koujiname_.contains(" ") ){
                val array = koujiname_.split(' ')
                writeText( array[0], PointXY(xr - tt, yb + yo ), 1f, 7, tss, 0)
                writeText( array[1], PointXY(xr - tt, yb - yo ), 1f, 7, tss, 0)
            }
            else{
                val array1 = koujiname_.substring(0, 20)
                val array2 = koujiname_.substring(20, koujiname_.length)
                writeText( array1, PointXY(xr - tt, yb + yo ), 1f, 7, tss, 0)
                writeText( array2, PointXY(xr - tt, yb - yo ), 1f, 7, tss, 0)
            }
        }
        else {
            writeText( koujiname_, PointXY(xr-tt, yb ), 1f, 7, tss, 0)
        }

        writeText( rStr_.tTitle_, PointXY(strx, 5.7f), 1f, 7, tss, 0)
        writeText( rosenname_, PointXY(strx, 4.7f), 1f, 7, tss, 0)
        writeText( rStr_.tDate_, PointXY(strx, 3.7f), 1f, 7, tss, 0)
        writeText( "1/${st.toInt()} (A3)", PointXY(34.5f, 2.7f), 1f, 7, tss, 1)
        writeText( zumennum_, PointXY(39.5f, 2.7f), 1f, 7, tss, 1)
        writeText( gyousyaname_, PointXY(strx, 1.7f), 1f, 7, tss, 0)
*/
    }

    open fun writeHeader(){}

    open fun writeFooter(){}

    open fun getPolymorphString(): String{
        return PolymorphFunction()+PolymorphFunctionB()
    }

    open fun PolymorphFunction(): String{
        return "IAMBASE."
    }

    open fun PolymorphFunctionB(): String{
        return "IAMBASE."
    }

}