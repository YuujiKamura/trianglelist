package com.jpaver.trianglelist

import com.jpaver.trianglelist.util.TitleParamStr
import java.time.LocalDate

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
    open val sizeX_ get() = 420 * printscale_ // カスタムゲッター
    open val sizeY_ get() = 297 * printscale_ // カスタムゲッター
    open val centerX_ get() = sizeX_ * 0.5f   // カスタムゲッター
    open val centerY_ get() = sizeY_ * 0.5f   // カスタムゲッター

    open var iWhite_ = 8
    open var iBlue_  = 4
    open var cRed_   = 2



    fun setNames(kn: String, rn: String, gn: String, zn: String){
        koujiname_ = kn
        rosenname_ = rn
        gyousyaname_ = gn
        zumennum_ =zn
    }

    fun checkInstance(): Boolean{
        return true
    }

    fun setStartNumber( num: Int ) :Int{
        if( num > 1 ) startTriNumber_ = num
        return startTriNumber_
    }

    fun Float?.formattedString(fractionDigits:Int): String{
        // nullの場合は空文字
        if(this == null) return ""
        val format = "%.${fractionDigits}f"
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

    open fun writeCircle(point: PointXY, size: Float, color: Int, scale: Float = 1f ){

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

    open fun writeEntities(){

    }

    open fun writeDeduction( ded: Deduction){}

    open fun writeTextAndLine(st: String, p1: PointXY, p2: PointXY, textsize: Float, scale: Float) {}

    fun writeOuterFrameAndTitle( scale: Float = 1f, textsize: Float ){
        // 外枠描画
        writeRect(PointXY(21f, 14.85f, scale), 40f * scale, 27f * scale,  iWhite_, scale)

        // 上のタイトル
        writeText(rStr_.tTitle_,
            PointXY(21f, 27.1f, scale),  iWhite_, textsize, 1, 1, 0f, scale)
        writeText(rosenname_,
            PointXY(21f, 26f, scale), iWhite_, textsize, 1, 1, 0f, scale)

        writeLine(
            PointXY(19f, 27f, scale),
            PointXY(23f, 27f, scale), iWhite_, scale)
        writeLine(
            PointXY(19f, 26.9f, scale),
            PointXY(23f, 26.9f, scale), iWhite_, scale)
    }

    open fun writeDrawingFrame(scale: Float = 1f, textsize: Float){

        val frameTextSize = textsize * scale
        writeOuterFrameAndTitle( scale , frameTextSize * 1.6f )

        //右下のタイトル枠
        writeLine(
            PointXY(31f, 7.35f, scale),
            PointXY(41f, 7.35f, scale), iWhite_ ) //yoko
        writeLine(
            PointXY(31f, 1.35f, scale),
            PointXY(31f, 7.35f, scale), iWhite_ ) //tate
        writeLine(
            PointXY(33f, 1.35f, scale),
            PointXY(33f, 7.35f, scale), iWhite_ ) //uchi-tate

        writeLine(
            PointXY(31f, 6.35f, scale),
            PointXY(41f, 6.35f, scale), iWhite_ )
        writeLine(
            PointXY(31f, 5.35f, scale),
            PointXY(41f, 5.35f, scale), iWhite_ )
        writeLine(
            PointXY(31f, 4.35f, scale),
            PointXY(41f, 4.35f, scale), iWhite_ )
        writeLine(
            PointXY(31f, 3.35f, scale),
            PointXY(41f, 3.35f, scale), iWhite_ )
        writeLine(
            PointXY(31f, 2.35f, scale),
            PointXY(41f, 2.35f, scale), iWhite_ )
        writeLine(
            PointXY(36f, 2.35f, scale),
            PointXY(36f, 3.35f, scale), iWhite_ )
        writeLine(
            PointXY(38f, 2.35f, scale),
            PointXY(38f, 3.35f, scale), iWhite_ )

        val st = printscale_*100f
        val strx = 33.5f * scale

        val yKOUJIMEI = 6.7f * scale
        val yo = 0.2f * scale

        // 題字(工事名　路線名　など）
        writeText(rStr_.tCname_,
            PointXY(32f, 6.7f, scale), iWhite_, frameTextSize, 1, 0, 0f, 1f)
        writeText(rStr_.tDtype_,
            PointXY(32f, 5.7f, scale), iWhite_, frameTextSize, 1, 0, 0f, 1f)
        writeText(rStr_.tDname_,
            PointXY(32f, 4.7f, scale), iWhite_, frameTextSize, 1, 0, 0f, 1f)
        writeText(rStr_.tDateHeader_,
            PointXY(32f, 3.7f, scale), iWhite_, frameTextSize, 1, 0, 0f, 1f)
        writeText(rStr_.tScale_,
            PointXY(32f, 2.7f, scale), iWhite_, frameTextSize, 1, 0, 0f, 1f)
        writeText(rStr_.tNum_,
            PointXY(37f, 2.7f, scale), iWhite_, frameTextSize, 1, 0, 0f, 1f)
        writeText(rStr_.tAname_,
            PointXY(32f, 1.7f, scale), iWhite_, frameTextSize, 1, 0, 0f, 1f)
        writeText(rStr_.tCredit_,
            PointXY(8f, 1f, scale), iWhite_, frameTextSize, 1, 0, 0f, 1f)

        // 内容
        // 工事名
        writeTextWithKaigyou(koujiname_, 25, strx, yKOUJIMEI, yo, iWhite_, frameTextSize)

        //writeText(koujiname_, PointXY(strx, yKOUJIMEI ), iWhite_, textsize, 0, 0, 0f, 1f)


        val nengappi = LocalDate.now().year.toString() + " 年 " + LocalDate.now().monthValue.toString() + " 月 " + LocalDate.now().dayOfMonth.toString() + " 日"

        writeText(rStr_.tTitle_,
            PointXY(strx, 5.7f * scale), iWhite_, frameTextSize, 0, 0, 0f, 1f)
        writeText(rosenname_,
            PointXY(strx, 4.7f * scale), iWhite_, frameTextSize, 0, 0, 0f, 1f)
        writeText(nengappi,
            PointXY(strx, 3.7f * scale), iWhite_, frameTextSize, 0, 0, 0f, 1f)
        writeText("1/${st.toInt()} (A3)",
            PointXY(34.5f, 2.7f, scale), iWhite_, frameTextSize, 1, 0, 0f, 1f)
        writeText(zumennum_,
            PointXY(39.5f, 2.7f, scale), iWhite_, frameTextSize, 1, 0, 0f, 1f)
        writeText(gyousyaname_,
            PointXY(strx, 1.7f * scale), iWhite_, frameTextSize, 0, 0, 0f, 1f)

        //myDXFscale = 1000f  // 1:1
    }

    fun writeTextWithKaigyou(str: String, iKaigyou: Int, xr: Float, yb: Float, yo: Float, iColor: Int, textsize: Float) {

        if (str.length > iKaigyou ) {
            splitAndWriteText(str, iKaigyou, xr, yb, yo, iColor, textsize)
        } else {
            writeText(str, PointXY(xr, yb), iColor, textsize, 0, 0,0f, 1f)
        }
    }

    private fun splitAndWriteText(str: String, iKaigyou: Int, xr: Float, yb: Float, yo: Float, iColor: Int, textsize: Float) {

        val parts = if (str.contains(" ")) {
            str.split(' ', limit = 2)
        } else {
            listOf(str.substring(0, iKaigyou), str.substring(iKaigyou))
        }

        writeText(parts[0], PointXY(xr, yb + yo), iColor, textsize, 0, 0,0f, 1f)
        // Check if there is a second part to avoid IndexOutOfBoundsException
        if (parts.size > 1) {
            writeText(parts[1], PointXY(xr, yb - yo), iColor, textsize, 0, 0,0f, 1f)
        }
    }

    fun writeCalcSheet(
        scale: Float = 1f,
        textsize: Float = textscale_,
        trilist: TriangleList,
        dedlist: DeductionList
    ) {
        if( checkInstance() == false ) return

        val baseX = ( 42f + 3f ) * printscale_ * scale
        var baseY = 27f * printscale_ * scale
        val ts = textsize * scale
        val xoffset = ts * 6f
        val yoffset = ts * 2f
        val yspacer = -ts * 0.01f

        writeLine(
            PointXY(
                baseX - xoffset * 0.5f,
                baseY + yoffset + yspacer
            ),
            PointXY(
                baseX + xoffset * 4.5f,
                baseY + yoffset + yspacer
            ), iWhite_, scale)

        var shokeiNum = 1
        val sprit = false
        if( sprit ){
            val tlSpC = trilist.spritByColors()
            for( index in 4 downTo 0 ){
                if( tlSpC[index].size() > 0 ) {
                    baseY = writeCalcSheetEditList(
                        tlSpC[ index ],
                        titleTri_,
                        baseX,
                        baseY,
                        ts,
                        xoffset,
                        scale,
                        shokeiNum
                    )
                    shokeiNum ++
                }
            }
        }
        else{
            if( trilist.size() > 0 ) {
                baseY = writeCalcSheetEditList(
                    trilist,
                    titleTri_,
                    baseX,
                    baseY,
                    ts,
                    xoffset,
                    scale,
                    shokeiNum
                )
                shokeiNum ++
            }
        }

        if( dedlist.size() > 0 ) baseY = writeCalcSheetEditList(
            dedlist,
            titleDed_,
            baseX,
            baseY,
            ts,
            xoffset,
            scale,
            shokeiNum
        )

        baseY -= yoffset
        writeText(rStr_.mGoukei_,
            PointXY(baseX, baseY), iWhite_, ts, 1, 1, 0f, scale)
        writeText(
            ( trilist_.getArea() - dedlist_.getArea() ).formattedString(2),
            PointXY(baseX + xoffset * 4, baseY),
            iWhite_,
            ts,
            1,
            1,
            0f,
            scale
        )
        writeLine(
            PointXY(
                baseX - xoffset * 0.5f,
                baseY + yspacer
            ),
            PointXY(
                baseX + xoffset * 4.5f,
                baseY + yspacer
            ),iWhite_, scale)
        writeLine(
            PointXY(
                baseX - xoffset * 0.5f,
                baseY + yoffset * 2 + yspacer
            ),
            PointXY(
                baseX - xoffset * 0.5f,
                baseY + yspacer
            ),iWhite_, scale)
        writeLine(
            PointXY(
                baseX + xoffset * 4.5f,
                baseY + yoffset * 2 + yspacer
            ),
            PointXY(
                baseX + xoffset * 4.5f,
                baseY + yspacer
            ),iWhite_, scale)

    }

    fun writeCalcSheetEditList(
        editList: EditList,
        titleParamStr: TitleParamStr,
        baseX: Float,
        baseY: Float,
        ts: Float,
        xoffset: Float,
        scale: Float = 1f,
        syokeiNum: Int
    ): Float {
        if( editList.size() < 1 ) return 0f

        var basey = baseY

        val yoffset = ts * 2f
        val yspacer = -ts * 0.01f

        var color = iWhite_
        if( editList is DeductionList) color = cRed_

        if( editList is TriangleList) {
            writeText(titleParamStr.n,
                PointXY(baseX, basey), color, ts, 1, 1, 0f, scale)
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
        if( editList is DeductionList) {
            writeText(titleParamStr.name,
                PointXY(baseX, basey), color, ts, 1, 1, 0f, scale)
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
        writeText(titleParamStr.a+"(m)",
            PointXY(baseX + xoffset, basey), color, ts, 1, 1, 0f, scale)
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
        writeLine(
            PointXY(
                baseX - xoffset * 0.5f,
                basey + yspacer
            ),
            PointXY(
                baseX + xoffset * 4.5f,
                basey + yspacer
            ),iWhite_, scale)
        writeLine(
            PointXY(
                baseX - xoffset * 0.5f,
                basey + yoffset + yspacer
            ),
            PointXY(
                baseX - xoffset * 0.5f,
                basey + yspacer
            ),iWhite_, scale)
        writeLine(
            PointXY(
                baseX + xoffset * 4.5f,
                basey + yoffset + yspacer
            ),
            PointXY(
                baseX + xoffset * 4.5f,
                basey + yspacer
            ),iWhite_, scale)

        basey -= yoffset

        for( number in 1 .. editList.size() ){
            writeCalcSheetLine( editList.get(number), baseX, basey, ts, color, scale )
            basey -= yoffset
        }

        writeText(rStr_.mSyoukei_+"("+syokeiNum+")",
            PointXY(baseX, basey), color, ts, 1, 1, 0f, scale)
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
        writeLine(
            PointXY(
                baseX - xoffset * 0.5f,
                basey + yspacer
            ),
            PointXY(
                baseX + xoffset * 4.5f,
                basey + yspacer
            ),iWhite_, scale)
        writeLine(
            PointXY(
                baseX - xoffset * 0.5f,
                basey + yoffset + yspacer
            ),
            PointXY(
                baseX - xoffset * 0.5f,
                basey + yspacer
            ),iWhite_, scale)
        writeLine(
            PointXY(
                baseX + xoffset * 4.5f,
                basey + yoffset + yspacer
            ),
            PointXY(
                baseX + xoffset * 4.5f,
                basey + yspacer
            ),iWhite_, scale)

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


        if( editObject is Triangle) {
            writeText(param.n.toString(),
                PointXY(baseX, baseY), color, ts, 1, 1, 0f, scale)
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
        if( editObject is Deduction){
            writeText(param.name,
                PointXY(baseX, baseY), color, ts, 1, 1, 0f, scale)
            writeText(
                    param.type,
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
        writeLine(
            PointXY(
                baseX - xoffset * 0.5f,
                baseY + yspacer
            ),
            PointXY(
                baseX + xoffset * 4.5f,
                baseY + yspacer
            ),iWhite_, scale)

        writeLine(
            PointXY(
                baseX - xoffset * 0.5f,
                baseY + yoffset + yspacer
            ),
            PointXY(
                baseX - xoffset * 0.5f,
                baseY + yspacer
            ),iWhite_, scale)
        writeLine(
            PointXY(
                baseX + xoffset * 4.5f,
                baseY + yoffset + yspacer
            ),
            PointXY(
                baseX + xoffset * 4.5f,
                baseY + yspacer
            ),iWhite_, scale)

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