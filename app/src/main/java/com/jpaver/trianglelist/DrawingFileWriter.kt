package com.jpaver.trianglelist

import com.jpaver.trianglelist.dataclass.ZumenInfo
import com.jpaver.trianglelist.util.TitleParamStr
import java.time.LocalDate

open class DrawingFileWriter {
    //region parameter
    open lateinit var trilist_: TriangleList
    open lateinit var dedlist_: DeductionList
    open lateinit var zumeninfo : ZumenInfo
    open lateinit var titleTri_ : TitleParamStr
    open lateinit var titleDed_ : TitleParamStr

    var koujiname_: String = ""
    var rosenname_ = ""
    var gyousyaname_ = ""
    var zumennum_ = "1/1"
    var startTriNumber_ = 1
    open var unitscale_ = 1000f
    open var viewscale_ = 47.6f
    open var printscale_ = 1f
    var isReverse_ = false

    open var textscale_ = 5f//trilist_.getPrintTextScale( 1f , "dxf") * drawscale_
    open val sizeX_ get() = 420 * printscale_ // カスタムゲッター
    open val sizeY_ get() = 297 * printscale_ // カスタムゲッター
    open val centerX_ get() = sizeX_ * 0.5f   // カスタムゲッター
    open val centerY_ get() = sizeY_ * 0.5f   // カスタムゲッター

    open var WHITE = 8
    open var BLUE  = 4
    open var RED   = 2

//endregion parameter
    fun stringTriple(tri: Triangle): Triple<String, String, String> {
        tri.setLengthStr()
        val nagasaA = tri.strLengthA
        val nagasaB = tri.strLengthB
        val nagasaC = tri.strLengthC
        return Triple(nagasaA, nagasaB, nagasaC)
    }

    fun xyPointXYTriple(tri: Triangle): Triple<PointXY, PointXY, PointXY> {
        val pca = tri.pointCA
        val pab = tri.pointAB
        val pbc = tri.pointBC
        return Triple(pca, pab, pbc)
    }

    fun writeTriangleLines(tri: Triangle,color: Int){

        writeLine( tri.point[0], tri.pointAB, color)
        writeLine( tri.pointAB, tri.pointBC, color)
        writeLine( tri.pointBC, tri.point[0], color)
    }

    fun writeTextSwitch( str: String, point: PointXY, ts:Float, color:Int, align1: Int, align2:Int, angle: Float ){
        //引数の数でテキスト描画関数を変える
        when(align2){
            -1   -> writeTextA9( str, point, color, ts, align1, angle, 1f)
            else -> writeTextHV( str, point, color, ts, align1, align2, angle, 1f)
        }
    }

    fun writePointNumber( tri: Triangle, ts:Float, color:Int, align1: Int, align2:Int, circleSize:Float ){
        val pn = tri.pointnumber
        val pc = tri.pointcenter
        // 本体
        writeCircle(pn, circleSize, color, 1f)

        writeTextSwitch( tri.mynumber.toString(), tri.pointnumber, ts, color, align1, align2, 0f)

        //引き出し矢印線の描画
        if( tri.isCollide(tri.pointnumber) == false ){
            val pcOffsetToN = pc.offset(pn, circleSize)
            val pnOffsetToC = pn.offset(pc, circleSize)
            val arrowTail = pcOffsetToN.offset(pn, pcOffsetToN.lengthTo(pnOffsetToC) * 0.5f).rotate(pcOffsetToN, 5f)

            writeLine(pcOffsetToN, pnOffsetToC, color)
            writeLine(pcOffsetToN, arrowTail, color)
        }
    }

    fun writeSokuten(tri:Triangle, normalizedvector:Int, ts:Float, color:Int, align1:Int, align2:Int ){
        tri.setDimPath()
        tri.setDimPoint()
        val pa = tri.pathS.pointA
        val pb = tri.pathS.pointB
        writeTextSwitch( tri.name, tri.dimpoint.s, ts, color, align1, align2, pb.calcSokAngle( pa, normalizedvector ) )
        writeLine( pa, pb, color)
    }
    fun writeDimFlags(tri: Triangle, color: Int){
        // DimTextの旗上げ
        val tPathA = tri.dimOnPath[0]
        val tPathB = tri.dimOnPath[1]
        val tPathC = tri.dimOnPath[2]
        if(tri.dim.horizontal.a > 2) writeLine( tPathA.pointA, tPathA.pointB, color)
        if(tri.dim.horizontal.b > 2) writeLine( tPathB.pointA, tPathB.pointB, color)
        if(tri.dim.horizontal.c > 2) writeLine( tPathC.pointA, tPathC.pointB, color)
    }

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
    open fun writeTextA9(
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
    open fun writeTextHV(
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

    fun writeOuterFrame(scale: Float = 1f){
        // 外枠描画
        writeRect(PointXY(21f, 14.85f, scale), 40f * scale, 27f * scale,  WHITE, scale)

    }

    fun writeTopTitle(scale: Float = 1f, textsize: Float ){
        // 上のタイトル
        writeTextHV(zumeninfo.zumentitle,
            PointXY(21f, 27.1f, scale),  WHITE, textsize, 1, 1, 0f, scale)
        writeTextHV(rosenname_,
            PointXY(21f, 26f, scale), WHITE, textsize, 1, 1, 0f, scale)

        writeLine(
            PointXY(19f, 27f, scale),
            PointXY(23f, 27f, scale), WHITE, scale)
        writeLine(
            PointXY(19f, 26.9f, scale),
            PointXY(23f, 26.9f, scale), WHITE, scale)

    }

    open fun writeDrawingFrame(scale: Float = 1f, textsize: Float){

        val frameTextSize = textsize * scale

        //外枠と上部のタイトル
        writeOuterFrame(scale)

        //右下のタイトル枠
        writeLine(
            PointXY(31f, 7.35f, scale),
            PointXY(41f, 7.35f, scale), WHITE ) //yoko
        writeLine(
            PointXY(31f, 1.35f, scale),
            PointXY(31f, 7.35f, scale), WHITE ) //tate
        writeLine(
            PointXY(33f, 1.35f, scale),
            PointXY(33f, 7.35f, scale), WHITE ) //uchi-tate

        writeLine(
            PointXY(31f, 6.35f, scale),
            PointXY(41f, 6.35f, scale), WHITE )
        writeLine(
            PointXY(31f, 5.35f, scale),
            PointXY(41f, 5.35f, scale), WHITE )
        writeLine(
            PointXY(31f, 4.35f, scale),
            PointXY(41f, 4.35f, scale), WHITE )
        writeLine(
            PointXY(31f, 3.35f, scale),
            PointXY(41f, 3.35f, scale), WHITE )
        writeLine(
            PointXY(31f, 2.35f, scale),
            PointXY(41f, 2.35f, scale), WHITE )
        writeLine(
            PointXY(36f, 2.35f, scale),
            PointXY(36f, 3.35f, scale), WHITE )
        writeLine(
            PointXY(38f, 2.35f, scale),
            PointXY(38f, 3.35f, scale), WHITE )

        val st = printscale_*100f
        val strx = 33.5f * scale

        val yKOUJIMEI = 6.7f * scale
        val yo = 0.2f * scale

        // 題字(工事名　路線名　など）
        writeTextHV(zumeninfo.koujiname,
            PointXY(32f, 6.7f, scale), WHITE, frameTextSize, 1, 0, 0f, 1f)
        writeTextHV(zumeninfo.tDtype_,
            PointXY(32f, 5.7f, scale), WHITE, frameTextSize, 1, 0, 0f, 1f)
        writeTextHV(zumeninfo.tDname_,
            PointXY(32f, 4.7f, scale), WHITE, frameTextSize, 1, 0, 0f, 1f)
        writeTextHV(zumeninfo.tDateHeader_,
            PointXY(32f, 3.7f, scale), WHITE, frameTextSize, 1, 0, 0f, 1f)
        writeTextHV(zumeninfo.tScale_,
            PointXY(32f, 2.7f, scale), WHITE, frameTextSize, 1, 0, 0f, 1f)
        writeTextHV(zumeninfo.tNum_,
            PointXY(37f, 2.7f, scale), WHITE, frameTextSize, 1, 0, 0f, 1f)
        writeTextHV(zumeninfo.tAname_,
            PointXY(32f, 1.7f, scale), WHITE, frameTextSize, 1, 0, 0f, 1f)
        writeTextHV(zumeninfo.tCredit_,
            PointXY(8f, 1f, scale), WHITE, frameTextSize, 1, 0, 0f, 1f)

        // 内容
        // 工事名
        writeTextWithKaigyou(koujiname_, 25, strx, yKOUJIMEI, yo, WHITE, frameTextSize)

        //writeText(koujiname_, PointXY(strx, yKOUJIMEI ), iWhite_, textsize, 0, 0, 0f, 1f)


        val nengappi = LocalDate.now().year.toString() + " 年 " + LocalDate.now().monthValue.toString() + " 月 " + LocalDate.now().dayOfMonth.toString() + " 日"

        writeTextHV(zumeninfo.zumentitle,
            PointXY(strx, 5.7f * scale), WHITE, frameTextSize, 0, 0, 0f, 1f)
        writeTextHV(rosenname_,
            PointXY(strx, 4.7f * scale), WHITE, frameTextSize, 0, 0, 0f, 1f)
        writeTextHV(nengappi,
            PointXY(strx, 3.7f * scale), WHITE, frameTextSize, 0, 0, 0f, 1f)
        writeTextHV("1/${st.toInt()} (A3)",
            PointXY(34.5f, 2.7f, scale), WHITE, frameTextSize, 1, 0, 0f, 1f)
        writeTextHV(zumennum_,
            PointXY(39.5f, 2.7f, scale), WHITE, frameTextSize, 1, 0, 0f, 1f)
        writeTextHV(gyousyaname_,
            PointXY(strx, 1.7f * scale), WHITE, frameTextSize, 0, 0, 0f, 1f)

        //myDXFscale = 1000f  // 1:1
    }

    fun writeTextWithKaigyou(str: String, iKaigyou: Int, xr: Float, yb: Float, yo: Float, iColor: Int, textsize: Float) {

        if (str.length > iKaigyou ) {
            splitAndWriteText(str, iKaigyou, xr, yb, yo, iColor, textsize)
        } else {
            writeTextHV(str, PointXY(xr, yb), iColor, textsize, 0, 0,0f, 1f)
        }
    }

    private fun splitAndWriteText(str: String, iKaigyou: Int, xr: Float, yb: Float, yo: Float, iColor: Int, textsize: Float) {

        val parts = if (str.contains(" ")) {
            str.split(' ', limit = 2)
        } else {
            listOf(str.substring(0, iKaigyou), str.substring(iKaigyou))
        }

        writeTextHV(parts[0], PointXY(xr, yb + yo), iColor, textsize, 0, 0,0f, 1f)
        // Check if there is a second part to avoid IndexOutOfBoundsException
        if (parts.size > 1) {
            writeTextHV(parts[1], PointXY(xr, yb - yo), iColor, textsize, 0, 0,0f, 1f)
        }
    }

    fun writeCalcSheet(
        scale: Float = 1f,
        textsize_: Float,
        trilist: TriangleList,
        dedlist: DeductionList
    ) {
        if( checkInstance() == false ) return

        val baseX = ( 42f + 3f ) * printscale_ * scale
        val textsize = textsize_
        val xoffset = textsize * 6f
        val yoffset = textsize * 2f
        val yspacer = -textsize * 0.01f
        var shokeiNum = 1

        //不変
        val immutable_baseY = 27f * printscale_ * scale
        //可変
        var mutable_baseY = 27f * printscale_ * scale

        val sprit = false
        if( sprit ){
            val tlSpC = trilist.spritByColors()
            for( index in 4 downTo 0 ){
                if( tlSpC[index].size() > 0 ) {
                    mutable_baseY = writeCalcSheetEditList(
                        tlSpC[ index ],
                        titleTri_,
                        baseX,
                        mutable_baseY,
                        textsize,
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
                mutable_baseY = writeCalcSheetEditList(
                    trilist,
                    titleTri_,
                    baseX,
                    mutable_baseY,
                    textsize,
                    xoffset,
                    scale,
                    shokeiNum
                )
                shokeiNum ++
            }
        }

        if( dedlist.size() > 0 ) mutable_baseY = writeCalcSheetEditList(
            dedlist,
            titleDed_,
            baseX,
            mutable_baseY,
            textsize,
            xoffset,
            scale,
            shokeiNum
        )

        mutable_baseY -= yoffset
        writeTextHV(zumeninfo.mGoukei_,
            PointXY(baseX, mutable_baseY), WHITE, textsize, 1, 1, 0f, scale)
        writeTextHV(
            ( trilist_.getArea() - dedlist_.getArea() ).formattedString(2),
            PointXY(baseX + xoffset * 4, mutable_baseY),
            WHITE,
            textsize,
            1,
            1,
            0f,
            scale
        )

        writeTopAndBottomHalfBox( baseX, xoffset, immutable_baseY, mutable_baseY, yoffset, yspacer, scale )
    }

    fun writeTopAndBottomHalfBox(baseX: Float,
                                 xoffset: Float,
                                 immutable_baseY: Float,
                                 mutable_baseY: Float,
                                 yoffset:Float,
                                 yspacer:Float,
                                 scale: Float ){

        val left = baseX - xoffset * 0.5f
        val right = baseX + xoffset * 4.5f
        val top = immutable_baseY + yoffset + yspacer
        val middle = mutable_baseY + yoffset * 2 + yspacer
        val bottom = mutable_baseY + yspacer
        // top left right
        writeLine(
            PointXY( left, top ),
            PointXY( right, top ), WHITE, scale)
        // bottom left right
        writeLine(
            PointXY( left, bottom ),
            PointXY( right, bottom ),WHITE, scale)
        // left middle bottom
        writeLine(
            PointXY( left, middle ),
            PointXY( left, bottom ),WHITE, scale)
        // right middle bottom
        writeLine(
            PointXY( right, middle ),
            PointXY( right, bottom ),WHITE, scale)
    }

    fun writeHalfBox(baseX: Float,
                     xoffset: Float,
                     basey: Float,
                     yspacer: Float,
                     yoffset: Float,
                     scale: Float ){
        writeLine(
            PointXY(
                baseX - xoffset * 0.5f,
                basey + yspacer
            ),
            PointXY(
                baseX + xoffset * 4.5f,
                basey + yspacer
            ),WHITE, scale)
        writeLine(
            PointXY(
                baseX - xoffset * 0.5f,
                basey + yoffset + yspacer
            ),
            PointXY(
                baseX - xoffset * 0.5f,
                basey + yspacer
            ),WHITE, scale)
        writeLine(
            PointXY(
                baseX + xoffset * 4.5f,
                basey + yoffset + yspacer
            ),
            PointXY(
                baseX + xoffset * 4.5f,
                basey + yspacer
            ),WHITE, scale)
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

        var color = WHITE
        if( editList is DeductionList) color = RED

        if( editList is TriangleList) {
            writeTextHV(titleParamStr.n,
                PointXY(baseX, basey), color, ts, 1, 1, 0f, scale)
            writeTextHV(
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
            writeTextHV(titleParamStr.name,
                PointXY(baseX, basey), color, ts, 1, 1, 0f, scale)
            writeTextHV(
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
        writeTextHV(titleParamStr.a+"(m)",
            PointXY(baseX + xoffset, basey), color, ts, 1, 1, 0f, scale)
        writeTextHV(
            titleParamStr.b+"(m)",
            PointXY(baseX + xoffset * 2, basey),
            color,
            ts,
            1,
            1,
            0f,
            scale
        )
        writeTextHV(
            titleParamStr.type+"(m2)",
            PointXY(baseX + xoffset * 4, basey),
            color,
            ts,
            1,
            1,
            0f,
            scale
        )

        writeHalfBox(baseX,xoffset,basey,yspacer,yoffset,scale)

        basey -= yoffset

        for( number in 1 .. editList.size() ){
            writeCalcSheetLine( editList.get(number), baseX, basey, ts, color, scale )
            basey -= yoffset
        }

        writeTextHV(zumeninfo.mSyoukei_+"("+syokeiNum+")",
            PointXY(baseX, basey), color, ts, 1, 1, 0f, scale)
        writeTextHV(
            editList.getArea().formattedString(2),
            PointXY(baseX + xoffset * 4, basey),
            color,
            ts,
            1,
            1,
            0f,
            scale
        )

        writeHalfBox(baseX,xoffset,basey,yspacer,yoffset,scale)

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
            writeTextHV(param.number.toString(),
                PointXY(baseX, baseY), color, ts, 1, 1, 0f, scale)
            writeTextHV(
                param.b.formattedString(2),
                PointXY(baseX + xoffset * 2, baseY),
                color,
                ts,
                1,
                1,
                0f,
                scale
            )
            writeTextHV(
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
            writeTextHV(param.name,
                PointXY(baseX, baseY), color, ts, 1, 1, 0f, scale)
            writeTextHV(
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
        if( param.type =="Box" )      writeTextHV(
            param.b.formattedString(2),
            PointXY(baseX + xoffset * 2, baseY),
            color,
            ts,
            1,
            1,
            0f,
            scale
        )

        writeTextHV(
            param.a.formattedString(2),
            PointXY(baseX + xoffset, baseY),
            color,
            ts,
            1,
            1,
            0f,
            scale
        )
        writeTextHV(
            editObject.getArea().formattedString(2),
            PointXY(baseX + xoffset * 4, baseY),
            color,
            ts,
            1,
            1,
            0f,
            scale
        )

        writeHalfBox(baseX,xoffset,baseY,yspacer,yoffset,scale)

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