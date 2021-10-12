package com.jpaver.trianglelist

import android.graphics.Paint

open class DrawingFileWriter {
    open lateinit var trilist_: TriangleList
    open lateinit var dedlist_: DeductionList
    lateinit var titleTri_ : TitleParamStr
    lateinit var titleDed_ : TitleParamStr
    lateinit var rStr_ : ResStr

    var koujiname_: String = ""
    var rosenname_ = ""
    var gyousyaname_ = ""
    var zumennum_ = "1/1"
    var startTriNumber_ = 1
    open var unitscale_ = 1000f
    val viewscale_ = 47.6f
    open var printscale_ = 1f
    var isReverse_ = false


    open var textscale_ = 5f//trilist_.getPrintTextScale( 1f , "dxf") * drawscale_
    open var sizeX_ = 420 * printscale_
    open var sizeY_ = 297 * printscale_
    open var centerX_ = sizeX_ * 0.5f
    open var centerY_ = sizeY_ * 0.5f

    open var cWhite_ = 8
    open var cBlue_  = 4
    open var cRed_   = 2



    fun setNames(kn: String, rn: String, gn: String, zn: String){
        koujiname_ = kn
        rosenname_ = rn
        gyousyaname_ = gn
        zumennum_ =zn
    }

    fun checkInstance(): Boolean{
        if( trilist_ == null ) return false
        if( dedlist_ == null ) return false
        if( titleTri_ == null ) return false
        if( titleDed_ == null ) return false
        if( rStr_ == null ) return false
        return true
    }

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

    open fun writeLine(p1: PointXY, p2: PointXY, color: Int, scale: Float = 1f ){
    }

    open fun writeRect(point: PointXY, sizeX: Float, sizeY: Float, color: Int, scale: Float = 1f ){
        val sizex: Float = sizeX/2
        val sizey: Float = sizeY/2
        writeLine( point.plus(-sizex, -sizey), point.plus(sizex, -sizey), color)
        writeLine( point.plus(-sizex, sizey), point.plus(sizex, sizey), color)
        writeLine( point.plus(-sizex, -sizey), point.plus(-sizex, sizey), color)
        writeLine( point.plus(sizex, -sizey), point.plus(sizex, sizey), color)
    }

    open fun writeCircle( point: PointXY, size: Float, color: Int, scale: Float = 1f ){

    }

    open fun writeText(str: String, point: PointXY, scale: Float, color: Int, size: Float, align: Int){

    }

    // align tenkey ( ex 8 is top and center ) in sfc
    open fun writeText(
        text: String,
        point: PointXY,
        color: Int = 8,
        tsy: Float,
        align: Int = 2,
        angle: Float = 0.0f,
        scale: Float
    ){
    }

    // Align H and V ( 0 is center, 1 is left/top, 3 is right/bottom ) in dxf
    open fun writeText(
        text: String,
        point: PointXY,
        color: Int,
        textsize: Float,
        alignH: Int,
        alignV: Int = 0,
        angle: Float = 0.0f,
        scale: Float
    ){

    }

    open fun writeTextOnPath(){

    }

    open fun writeEntities(){

    }

    open fun writeDeduction( ded: Deduction ){}

    open fun writeTextAndLine(st: String, p1: PointXY, p2: PointXY, textsize: Float, scale: Float) {}

    fun writeOuterFrameAndTitle( scale: Float = 1f, textsize: Float ){
        // 外枠描画
        writeRect(PointXY(21f,14.85f, scale ), 40f * scale, 27f * scale,  cWhite_, scale)

        // 上のタイトル
        writeText(rStr_.tTitle_, PointXY(21f, 27.1f, scale ),  cWhite_, textsize, 1, 1, 0f, scale)
        writeText(rosenname_, PointXY(21f,26f, scale ), cWhite_, textsize, 1, 1, 0f, scale)

        writeLine( PointXY(19f,27f, scale ), PointXY(23f,27f, scale ), cWhite_, scale)
        writeLine( PointXY(19f,26.9f, scale ), PointXY(23f,26.9f, scale ), cWhite_, scale)
    }

    open fun writeFrame(scale: Float = 1f, textsize: Float){

        val tss = textsize * scale
        writeOuterFrameAndTitle( scale , tss * 1.6f )

        //右下のタイトル枠
        writeLine( PointXY(31f,7.35f, scale ), PointXY(41f,7.35f, scale ), cWhite_ ) //yoko
        writeLine( PointXY(31f,1.35f, scale ), PointXY(31f,7.35f, scale ), cWhite_ ) //tate
        writeLine( PointXY(33f,1.35f, scale ), PointXY(33f,7.35f, scale ), cWhite_ ) //uchi-tate

        writeLine( PointXY(31f,6.35f, scale ), PointXY(41f,6.35f, scale ), cWhite_ )
        writeLine( PointXY(31f,5.35f, scale ), PointXY(41f,5.35f, scale ), cWhite_ )
        writeLine( PointXY(31f,4.35f, scale ), PointXY(41f,4.35f, scale ), cWhite_ )
        writeLine( PointXY(31f,3.35f, scale ), PointXY(41f,3.35f, scale ), cWhite_ )
        writeLine( PointXY(31f,2.35f, scale ), PointXY(41f,2.35f, scale ), cWhite_ )
        writeLine( PointXY(36f,2.35f, scale ), PointXY(36f,3.35f, scale ), cWhite_ )
        writeLine( PointXY(38f,2.35f, scale ), PointXY(38f,3.35f, scale ), cWhite_ )

        val st = printscale_*100f
        val strx = 33.5f * scale

        val xr = strx
        val yb = 6.7f * scale
        val yo = 0.2f * scale
        val uw = 160f * scale
        val tt = 0
        val ofs = 3f * scale

        // 題字
        writeText(rStr_.tCname_, PointXY(32f, 6.7f, scale ), cWhite_, tss, 1, 0, 0f, 1f)
        writeText(rStr_.tDtype_, PointXY(32f, 5.7f, scale ), cWhite_, tss, 1, 0, 0f, 1f)
        writeText(rStr_.tDname_, PointXY(32f, 4.7f, scale ), cWhite_, tss, 1, 0, 0f, 1f)
        writeText(rStr_.tDateHeader_, PointXY(32f, 3.7f, scale ), cWhite_, tss, 1, 0, 0f, 1f)
        writeText(rStr_.tScale_, PointXY(32f, 2.7f, scale ), cWhite_, tss, 1, 0, 0f, 1f)
        writeText(rStr_.tNum_, PointXY(37f, 2.7f, scale ), cWhite_, tss, 1, 0, 0f, 1f)
        writeText(rStr_.tAname_, PointXY(32f, 1.7f, scale ), cWhite_, tss, 1, 0, 0f, 1f)
        writeText(rStr_.tCredit_, PointXY(8f, 1f, scale ), cWhite_, tss, 1, 0, 0f, 1f)

        // 内容
        // 工事名
        if( koujiname_.length > 20 ) {
            if( koujiname_.contains(" ") ){
                val array = koujiname_.split(' ')
                writeText(array[0], PointXY(xr - tt, yb + yo ), cWhite_, tss, 0, scale = 1f)
                writeText(array[1], PointXY(xr - tt, yb - yo ), cWhite_, tss, 0, scale = 1f)
            }
            else{
                val array1 = koujiname_.substring(0, 20)
                val array2 = koujiname_.substring(20, koujiname_.length)
                writeText(array1, PointXY(xr - tt, yb + yo ), cWhite_, tss, 0, scale = 1f)
                writeText(array2, PointXY(xr - tt, yb - yo ), cWhite_, tss, 0, scale = 1f)
            }
        }
        else {
            writeText(koujiname_, PointXY(xr-tt, yb ), cWhite_, tss, 0, scale = 1f)
        }
        writeText(rStr_.tTitle_, PointXY(strx, 5.7f * scale ), cWhite_, tss, 0, 0, 0f, 1f)
        writeText(rosenname_, PointXY(strx, 4.7f * scale ), cWhite_, tss, 0, 0, 0f, 1f)
        writeText("     "+rStr_.tDate_, PointXY(strx, 3.7f * scale ), cWhite_, tss, 0, 0, 0f, 1f)
        writeText("1/${st.toInt()} (A3)", PointXY(34.5f, 2.7f, scale ), cWhite_, tss, 1, 0, 0f, 1f)
        writeText(zumennum_, PointXY(39.5f, 2.7f, scale ), cWhite_, tss, 1, 0, 0f, 1f)
        writeText(gyousyaname_, PointXY(strx, 1.7f * scale ), cWhite_, tss, 0, 0, 0f, 1f)

        //myDXFscale = 1000f  // 1:1
    }

    fun writeCalcSheet(
        scale: Float = 1f,
        textsize: Float = textscale_,
        trilist: TriangleList,
        dedlist: DeductionList
    ) {
        if( checkInstance() == false ) return

        var area = 0.0f
        val baseX = ( 42f + 3f ) * printscale_ * scale
        var baseY = 27f * printscale_ * scale
        val ts = textsize * scale
        val xoffset = ts * 6f
        val yoffset = ts * 2f
        val yspacer = -ts * 0.01f

        writeLine( PointXY( baseX - xoffset *0.5f, baseY + yoffset + yspacer ), PointXY(baseX + xoffset * 4.5f, baseY + yoffset + yspacer ), cWhite_, scale)

        baseY = writeCalcSheetEditList(trilist, titleTri_, baseX, baseY, ts, xoffset, scale)
        if( dedlist.size() > 0 ) baseY = writeCalcSheetEditList(dedlist, titleDed_, baseX, baseY, ts, xoffset, scale)

        baseY -= yoffset
        writeText(rStr_.mGoukei_, PointXY(baseX, baseY), cWhite_, ts, 1, 1, 0f, scale)
        writeText(
            ( trilist_.getArea() - dedlist_.getArea() ).formattedString(2),
            PointXY(baseX + xoffset * 4, baseY),
            cWhite_,
            ts,
            1,
            1,
            0f,
            scale
        )
        writeLine( PointXY( baseX - xoffset *0.5f, baseY + yspacer ), PointXY(baseX + xoffset * 4.5f, baseY + yspacer ),cWhite_, scale)
        writeLine( PointXY( baseX - xoffset * 0.5f, baseY + yoffset * 2 + yspacer ), PointXY(baseX - xoffset * 0.5f, baseY + yspacer ),cWhite_, scale)
        writeLine( PointXY( baseX + xoffset * 4.5f, baseY + yoffset * 2 + yspacer ), PointXY(baseX + xoffset * 4.5f, baseY + yspacer ),cWhite_, scale)

    }

    fun writeCalcSheetEditList(
        editList: EditList,
        titleParamStr: TitleParamStr,
        baseX: Float,
        baseY: Float,
        ts: Float,
        xoffset: Float,
        scale: Float = 1f
    ): Float {
        var basey = baseY
        var sn = 1

        val yoffset = ts * 2f
        val yspacer = -ts * 0.01f

        var color = cWhite_
        if( editList is DeductionList ) color = cRed_

        if( editList is TriangleList ) {
            writeText(titleParamStr.n, PointXY(baseX, basey), color, ts, 1, 1, 0f, scale)
            writeText(
                titleParamStr.c+"(m)",
                PointXY(baseX + xoffset * 3, basey),
                color,
                ts,
                1,
                1,
                0f,
                scale
            )
        }
        if( editList is DeductionList ) {
            sn = 2
            writeText(titleParamStr.name, PointXY(baseX, basey), color, ts, 1, 1, 0f, scale)
            writeText(
                titleParamStr.pl,
                PointXY(baseX + xoffset * 3, basey),
                color,
                ts,
                1,
                1,
                0f,
                scale
            )
        }
        writeText(titleParamStr.a+"(m)", PointXY(baseX + xoffset, basey), color, ts, 1, 1, 0f, scale)
        writeText(
            titleParamStr.b+"(m)",
            PointXY(baseX + xoffset * 2, basey),
            color,
            ts,
            1,
            1,
            0f,
            scale
        )
        writeText(
            titleParamStr.type+"(m2)",
            PointXY(baseX + xoffset * 4, basey),
            color,
            ts,
            1,
            1,
            0f,
            scale
        )
        writeLine( PointXY( baseX - xoffset *0.5f, basey + yspacer ), PointXY(baseX + xoffset * 4.5f, basey + yspacer ),cWhite_, scale)
        writeLine( PointXY( baseX - xoffset * 0.5f, basey + yoffset + yspacer ), PointXY(baseX - xoffset * 0.5f, basey + yspacer ),cWhite_, scale)
        writeLine( PointXY( baseX + xoffset * 4.5f, basey + yoffset + yspacer ), PointXY(baseX + xoffset * 4.5f, basey + yspacer ),cWhite_, scale)

        basey -= yoffset

        for( index in 1 .. editList.size() ){
            writeCalcSheetLine(editList.get(index), baseX, basey, ts, color, scale)
            basey -= yoffset
        }

        writeText(rStr_.mSyoukei_+"("+sn+")", PointXY(baseX, basey), color, ts, 1, 1, 0f, scale)
        writeText(
            editList.getArea().formattedString(2),
            PointXY(baseX + xoffset * 4, basey),
            color,
            ts,
            1,
            1,
            0f,
            scale
        )
        writeLine( PointXY( baseX - xoffset *0.5f, basey + yspacer ), PointXY(baseX + xoffset * 4.5f, basey + yspacer ),cWhite_, scale)
        writeLine( PointXY( baseX - xoffset * 0.5f, basey + yoffset + yspacer ), PointXY(baseX - xoffset * 0.5f, basey + yspacer ),cWhite_, scale)
        writeLine( PointXY( baseX + xoffset * 4.5f, basey + yoffset + yspacer ), PointXY(baseX + xoffset * 4.5f, basey + yspacer ),cWhite_, scale)

        basey -= yoffset
        return basey
    }

    fun writeCalcSheetLine(
        editObject: EditObject,
        baseX: Float,
        baseY: Float,
        ts: Float,
        color: Int,
        scale: Float = 1f
    ) :Float {
        val param = editObject.getParams()
        val xoffset = ts * 6f
        val yoffset = ts * 2f
        val yspacer = -ts * 0.01f


        if( editObject is Triangle ) {
            writeText(param.n.toString(), PointXY(baseX, baseY), color, ts, 1, 1, 0f, scale)
            writeText(
                param.b.formattedString(2),
                PointXY(baseX + xoffset * 2, baseY),
                color,
                ts,
                1,
                1,
                0f,
                scale
            )
            writeText(
                param.c.formattedString(2),
                PointXY(baseX + xoffset * 3, baseY),
                color,
                ts,
                1,
                1,
                0f,
                scale
            )
        }
        if( editObject is Deduction ){
            writeText(param.name.toString(), PointXY(baseX, baseY), color, ts, 1, 1, 0f, scale)
            writeText(
                param.type.toString(),
                PointXY(baseX + xoffset * 3, baseY),
                color,
                ts,
                1,
                1,
                0f,
                scale
            )
        }
        if( param.type =="Box" )      writeText(
            param.b.formattedString(2),
            PointXY(baseX + xoffset * 2, baseY),
            color,
            ts,
            1,
            1,
            0f,
            scale
        )

        writeText(
            param.a.formattedString(2),
            PointXY(baseX + xoffset, baseY),
            color,
            ts,
            1,
            1,
            0f,
            scale
        )
        writeText(
            editObject.getArea().formattedString(2),
            PointXY(baseX + xoffset * 4, baseY),
            color,
            ts,
            1,
            1,
            0f,
            scale
        )
        writeLine( PointXY( baseX - xoffset * 0.5f, baseY + yspacer ), PointXY(baseX + xoffset * 4.5f, baseY + yspacer ),cWhite_, scale)

        writeLine( PointXY( baseX - xoffset * 0.5f, baseY + yoffset + yspacer ), PointXY(baseX - xoffset * 0.5f, baseY + yspacer ),cWhite_, scale)
        writeLine( PointXY( baseX + xoffset * 4.5f, baseY + yoffset + yspacer ), PointXY(baseX + xoffset * 4.5f, baseY + yspacer ),cWhite_, scale)

        return editObject.getArea()
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