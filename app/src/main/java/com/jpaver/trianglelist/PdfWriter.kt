package com.jpaver.trianglelist

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.jpaver.trianglelist.util.Utils
import java.io.OutputStream


class PdfWriter(printScale: Float, triangleList: TriangleList) : DrawingFileWriter() {

    //region parameters

    // 定数定義
    private val MAX_LENGTH = 17
    private val FIRST_LINE_OFFSET = -100f
    private val SECOND_LINE_OFFSET = -85f
    private val SINGLE_LINE_OFFSET = -95f

    private val triangleList_ = triangleList
    lateinit var deductionList_: DeductionList

    val drawingScale_ = triangleList.scale
    var printScale_ = printScale// 2.0f
    override var textscale_ = triangleList_.getPrintTextScale( 1f, "pdf")

    // 展開図の用紙寸法。1190pt = 2.833.. * 420mm,   842pt = 2.835.. * 297mm
    // A4の縦　595pt = 2.833.. * 210mm
    // PostScript 1 point in CSS = 0.3527mm = 1 / 2.835270768
    val kaizoudo_ = 1f
    override var sizeX_ = 1190f * kaizoudo_// * scale_ //420f * scale_
    override var sizeY_ = 842f * kaizoudo_// * scale_ //297f * scale_

    var isRulerOff_ = true

    val paintRed_: Paint = Paint()

    val paintTri_: Paint = Paint()
    var paintTexS_: Paint = Paint()

    val pdfDoc_: PdfDocument = PdfDocument()

    lateinit var out_: OutputStream
    var currentPageIndex_ = 0
    val pdfBuilderList = ArrayList<PdfDocument.PageInfo.Builder>()
    val pdfPageInfList = ArrayList<PdfDocument.PageInfo>()
    val pdfPageList = ArrayList<PdfDocument.Page>()
    lateinit var currentCanvas_: Canvas// = page_.canvas
    // トランスレート位置の保存
    var viewPointer_ = PointXY(0f, 0f)

    init{
        // 最初のページを作る。環境依存がありユニットテストできないので外す
        //startNewPage(sizeX_.toInt(), sizeY_.toInt(), currentPageIndex_)

    }

    fun initPaints(){
        paintTri_.setARGB(100, 64, 64, 64)
        paintTri_.style = Paint.Style.STROKE
        paintTri_.textAlign = Paint.Align.CENTER
        paintTri_.strokeWidth = 0.05f

        paintTexS_.setARGB(255, 0, 0, 0)
        paintTexS_.style = Paint.Style.FILL
        paintTexS_.textSize = textscale_
        paintTexS_.textAlign = Paint.Align.CENTER
        paintTexS_.letterSpacing = 0.05f
        paintTexS_.strokeWidth = 0.05f

        paintRed_.setARGB(255, 255, 0, 0)
        paintRed_.style = Paint.Style.FILL
        paintRed_.textSize = textscale_
        paintRed_.textAlign = Paint.Align.LEFT
        paintRed_.letterSpacing = 0.05f
        paintRed_.strokeWidth = 0.05f

    }
//endregion parameters

//region canvas translate and text scaling
    fun translate( canvas: Canvas, x: Float, y: Float ){
        canvas.translate(x,y)
        viewPointer_.add(x,y) // rewrite viewPointer but..
    }

    fun translateCenter() {
        currentCanvas_.translate(sizeX_/2f,sizeY_/2f )
        viewPointer_.add( sizeX_/2f,sizeY_/2f )
    }

    fun setScale(drawingLength: PointXY) :Float{
        //1/200　x:80m~40m
        var scale = 2f
        paintTexS_.textSize = 2.5f

        //1/250 (x:80m以上
        if(drawingLength.x > 80 || drawingLength.y > 55 ) {
            scale = 2.5f
            paintTexS_.textSize = 2f
        }

        //1/350 (x:120m以上
        if(drawingLength.x > 40*3 || drawingLength.y > 27*3 ) {
            scale = 3.5f
            paintTexS_.textSize = 0.5f
        }

        //1/150 (x:40m以下
        if(drawingLength.x < 50 && drawingLength.y < 30 ){
            scale = 1.5f
            paintTexS_.textSize = 50f
        }
        return scale
    }
//endregion canvas trans and textscaling

//region page management
    fun startNewPage(sizeX: Int, sizeY: Int, pageIndex: Int){
        pdfBuilderList.add( PdfDocument.PageInfo.Builder( sizeX, sizeY, pageIndex ) )
        // wrong pageNum will bad.
        pdfPageInfList.add( pdfBuilderList[pageIndex].create() )
        pdfPageList.add( pdfDoc_.startPage( pdfPageInfList[pageIndex] ) )
        currentCanvas_ = pdfPageList[pageIndex].canvas
        pdfPageList[pageIndex].canvas.density = 600
    }

    fun closeCurrentPage(){
        //現在のPageの編集を終了する。
        pdfDoc_.finishPage( pdfPageList[currentPageIndex_] )

    }

    fun closeDocAndStream(){
        pdfDoc_.writeTo(out_)
        pdfDoc_.close()
        out_.close() //MUST.. without write done.
    }
//endregion page management


    fun drawOverlayWhite(canvas: Canvas, yohaku: Float){
        val white = Paint()
        white.setARGB(255,255,255,255)
        white.style = Paint.Style.FILL

        val green = Paint()
        green.setARGB(64,0,255,64)
        green.style = Paint.Style.FILL

        //canvas.drawRect(0f, 0f, sizeX_, sizeY_, green)

        //canvas.drawRect(310f*printScale_, 240f*printScale_, 405f*printScale_, 282f*printScale_, white)
        canvas.drawRect(0f, 0f, yohaku,  sizeY_, white)
        canvas.drawRect(sizeX_-yohaku, 0f, sizeX_,  sizeY_, white)
        canvas.drawRect(0f, 0f, sizeX_,  yohaku, white)
        canvas.drawRect(0f, sizeY_-yohaku, sizeX_,  sizeY_, white)

    }

    fun writeTitleFrame(canvas: Canvas){
        currentCanvas_.translate(-sizeX_/2f,-sizeY_/2f)

        //1190, 842
        val yohaku = 17.5f
        val frameX = this.sizeX_-yohaku*2//*scale_ 1130
        val frameY = this.sizeY_-yohaku*2//*scale_ 782


        //w95f, h42f, 1005, 710. 1100, 752,
        val xr = sizeX_-yohaku
        val yb = sizeY_-yohaku
        val sw = 205f //タイトル枠の幅
        val uw = 155f // 仕切り縦線
        val uw2 = 105f
        val uw3 = 45f
        val tt = uw-5f // テキスト開始位置x
        val ht = 110f // タイトル枠の高さ
        val hp = 20f // 仕切り横線のピッチ
        val lt = PointXY(xr - sw, yb - ht)
        val lb = PointXY(xr - sw, yb)
        val rt = PointXY(xr, yb - ht)
        PointXY(xr, yb)
        val ut = PointXY(xr - uw, yb - ht)
        val ub = PointXY(xr - uw, yb)
        zumeninfo.tCredit_
        val centerX = sizeX_ / 2
        sizeY_ / 2
        val kt = (sw-uw)/2f+uw
        val ofs = 3f
        val st: Float = printScale_/drawingScale_*100f

        // 枠外を白抜きするための塗りつぶし
        drawOverlayWhite(canvas, yohaku)

        // Frame
        writeRect(  getCenter(), frameX, frameY, 1f, 7)

        //tate
        writeLine( lt, lb, 1f,7)
        writeLine( ut, ub, 1f,7)
        //uchi
        writeLine(
            PointXY(xr - uw2, yb - 40f),
            PointXY(xr - uw2, yb - 20f),1f,7)
        writeLine(
            PointXY(xr - uw3, yb - 40f),
            PointXY(xr - uw3, yb - 20f),1f,7)

        //yoko
        writeLine( lt, rt, 1f,7)
        writeLine(
            PointXY(xr - sw, yb - (hp * 1)),
            PointXY(xr, yb - (hp * 1)),1f,7)
        writeLine(
            PointXY(xr - sw, yb - (hp * 2)),
            PointXY(xr, yb - (hp * 2)),1f,7)
        writeLine(
            PointXY(xr - sw, yb - (hp * 3)),
            PointXY(xr, yb - (hp * 3)),1f,7)
        writeLine(
            PointXY(xr - sw, yb - (hp * 4)),
            PointXY(xr, yb - (hp * 4)),1f,7)

        //writeRuler(canvas)
/*
        val ruler = 1190f/42f/printScale_
        val rulerTen = 1190f/4.2f/printScale_
        val fortytwo = 42f * printScale_
        val ten = fortytwo/10f+1f
        val rulerstart = 0f//45f
        val rulerY = centerY + 100f
        val rulercolor = 2
        //writeText( "PrintScale "+printScale_.toString(), PointXY(100f,centerY+50f ), 1f, 7, 14f, 1 )
        //writeText( "DrawingScale "+drawingScale_.toString(), PointXY(100f,centerY+70f ), 1f, 7, 14f, 1 )

        for( i in 0 until fortytwo.toInt() ){
            var ifloat = i.toFloat()*ruler
            writeLine( PointXY( rulerstart+ifloat, rulerY ), PointXY(rulerstart+ifloat,rulerY+5f),1f,rulercolor)
        }

        for( i in 0 until ten.toInt() ){
            var ifloat = i.toFloat()*rulerTen
            var istr = (i*10).toString()
            writeLine( PointXY( rulerstart+ifloat, rulerY ), PointXY(rulerstart+ifloat,rulerY+10f),1f,rulercolor)
            writeText( istr, PointXY(rulerstart+ifloat,rulerY+20f), 1f, rulercolor, 7f, 1 )
        }
*/

        // titleline
        writeLine(
            PointXY(centerX - 50f, 52f),
            PointXY(centerX + 50f, 52f),1f,7)
        writeLine(
            PointXY(centerX - 50f, 50f),
            PointXY(centerX + 50f, 50f),1f,7)
        writeText( zumeninfo.zumentitle,
            PointXY(centerX, 45f), 1f, 7, 16f, 1 )
        writeText( rosenname_ + " A=" + Utils.formattedString( triangleList_.getArea() - deductionList_.getArea() ) + "m^2",
            PointXY(centerX, 70f), 1f, 7, 16f, 1 )

        writeText( zumeninfo.koujiname,
            PointXY(xr - kt, yb - 95f + ofs), 1f, 7, 8f, 1 )
        writeText( zumeninfo.tDtype_,
            PointXY(xr - kt, yb - 70f + ofs), 1f, 7, 8f, 1 )
        writeText( zumeninfo.tDname_,
            PointXY(xr - kt, yb - 50f + ofs), 1f, 7, 8f, 1 )
        writeText( zumeninfo.tScale_,
            PointXY(xr - kt, yb - 30f + ofs), 1f, 7, 8f, 1 )
        writeText( zumeninfo.tNum_,
            PointXY(xr - 75, yb - 30f + ofs), 1f, 7, 8f, 1 )
        writeText( zumeninfo.tAname_,
            PointXY(xr - kt, yb - 10f + ofs), 1f, 7, 8f, 1 )
        writeText( zumeninfo.tCredit_,
            PointXY(50f, yb + 10f), 1f, 7, 7f, 0 )

/*
        if( koujiname_.length > 17 ) {
            if( koujiname_.contains(" ") ){
                val array = koujiname_.split(' ')
                writeText(array[0],
                    PointXY(xr - tt, yb - 100f), 1f, 7, 8f, 0)
                writeText(array[1],
                    PointXY(xr - tt, yb - 85f), 1f, 7, 8f, 0)
            }
            else{
                val array1 = koujiname_.substring(0, 17)
                val array2 = koujiname_.substring(17, koujiname_.length)
                writeText(array1,
                    PointXY(xr - tt, yb - 100f), 1f, 7, 8f, 0)
                writeText(array2,
                    PointXY(xr - tt, yb - 85f), 1f, 7, 8f, 0)
            }
        }
        else {

        }
  */
        //}
        //else writeText(koujiname_, PointXY(xr-tt, yb-95f+ofs ), 1f, 7, 8f, 0 )
        drawTextWithLineBreak(koujiname_, xr, yb, tt)

        writeText(zumeninfo.zumentitle,
            PointXY(xr - tt, yb - 70f + ofs), 1f, 7, 8f, 0 )
        writeText(rosenname_,
            PointXY(xr - tt, yb - 50f + ofs), 1f, 7, 8f, 0 )
        val ust = (uw-uw2)/2+uw2
        val usst = uw3/2
        writeText("1/${st.toInt()} (A3)",
            PointXY(xr - ust, yb - 30f + ofs), 1f, 7, 8f, 1 )
        writeText( zumennum_,
            PointXY(xr - usst, yb - 30f + ofs), 1f, 7, 8f, 1 )
        writeText( gyousyaname_,
            PointXY(xr - tt, yb - 10f + ofs), 1f, 7, 8f, 0 )

        currentCanvas_.translate(sizeX_/2f,sizeY_/2f)
        //  translateCenter()
    }



    private fun drawTextWithLineBreak(text: String, xr: Float, yb: Float, tt: Float = 150.0f ) {
        if (text.length > MAX_LENGTH) {
            splitAndDrawText(text, xr, yb, tt)
        } else {
            writeText(text, PointXY(xr - tt, yb + SINGLE_LINE_OFFSET), 1f, 7, 8f, 0)
        }
    }

    private fun splitAndDrawText(text: String, xr: Float, yb: Float, tt: Float) {
        val splitText = if (text.contains(" ")) text.split(' ', limit = 2) else listOf(text.take(MAX_LENGTH), text.drop(MAX_LENGTH))
        writeText( splitText[0], PointXY(xr - tt, yb + FIRST_LINE_OFFSET), 1f, 7, 8f, 0)
        if (splitText.size > 1) {
            writeText( splitText[1], PointXY(xr - tt, yb + SECOND_LINE_OFFSET), 1f, 7, 8f, 0)
        }
    }

    fun writeRect(point: PointXY, sizeX: Float, sizeY: Float, scale: Float, color: Int){
        val sizex = sizeX/2
        val sizey = sizeY/2
        writeLine(point.plus(-sizex, -sizey), point.plus(sizex, -sizey), scale, color)
        writeLine(point.plus(-sizex, sizey), point.plus(sizex, sizey), scale,  color)
        writeLine(point.plus(-sizex, -sizey), point.plus(-sizex, sizey), scale,  color)
        writeLine(point.plus(sizex, -sizey), point.plus(sizex, sizey), scale,  color)
    }

    fun writeLine(p1: PointXY, p2: PointXY, scale: Float, color: Int){
        currentCanvas_.drawLine(p1.x*scale, p1.y*scale, p2.x*scale, p2.y*scale, setPaint(color, 5f, 1))
    }

    override fun writeLine(p1: PointXY, p2: PointXY, color: Int, scale: Float ){
        currentCanvas_.drawLine(p1.x, p1.y, p2.x, p2.y, setPaint(color, 5f, 1))
    }

    override fun writeText(str: String, point: PointXY, scale: Float, color: Int, size: Float, align: Int){

        this.setPaint(color, size, align)
        currentCanvas_.drawText(str, point.x*scale, point.y*scale ,setPaint(color, size, align))
/*
        if(align <= 1) {
            if (align == 1) point.x -= (size * str.length  / 2)
            for (i: Int in 0 until str.length) {
                var char = str[i].toString()
                var offsetX = size * scale * i.toFloat()
                currentCanvas_.drawText(char, point.x * scale + offsetX, point.y * scale, paint)
            }
        }
        if (align == 2){
            for (i: Int in 0 until str.length) {
                var char = str[str.length-1-i].toString()
                var offsetX = -size * scale * i.toFloat()
                currentCanvas_.drawText(char, point.x * scale + offsetX, point.y * scale, paint)
            }
        }
*/
    }

    fun getCenter(): PointXY {
        return PointXY(sizeX_ / 2f, sizeY_ / 2f)
    }

    fun setPaint(color: Int, size: Float, align: Int): Paint {
        val paint = Paint()
        paint.style = Paint.Style.FILL
        paint.textSize = size
        paint.letterSpacing = 0.05f
        if( align == 0 ) paint.textAlign = Paint.Align.LEFT
        if( align == 1 ) paint.textAlign = Paint.Align.CENTER
        if( align == 2 ) paint.textAlign = Paint.Align.RIGHT

        if( color == 1 ) paint.setARGB(255,255,0,0)
        if( color == 2 ) paint.setARGB(255,100,100,255)
        if( color == 3 ) paint.setARGB(255,100,255,100)
        if( color == 7 ) paint.setARGB(255,0,0,0)
        return paint
    }

}//EOC

