package com.jpaver.trianglelist

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.OutputStream


class PdfWriter(printScale: Float, triangleList: TriangleList ) : DrawingFileWriter() {
    private val triangleList_ = triangleList
    lateinit var deductionList_: DeductionList


    val drawingScale_ = triangleList.myScale
    var printScale_ = printScale// 2.0f
    val textSize_ = triangleList_.getPrintTextScale( 1f, "pdf")

    // 展開図の用紙寸法。1190pt = 2.833.. * 420mm,   842pt = 2.835.. * 297mm
    // A4の縦　595pt = 2.833.. * 210mm
    // PostScript 1 point in CSS = 0.3527mm = 1 / 2.835270768
    val kai_ = 1f
    var sizeX_ = 1190f * kai_// * scale_ //420f * scale_
    var sizeY_ = 842f * kai_// * scale_ //297f * scale_

    val scale2_ = 2.5f//printScale_
    var p2sizeX_ = 595f * kai_// * scale2_ //210f * scale2_
    var p2sizeY_ = 842f * kai_// * scale2_ //297f * scale2_

    var isRulerOff_ = true

    val paintTexM_: Paint = Paint()
    val paintTexL_: Paint = Paint()
    val paintRed_: Paint = Paint()
    val paintBlue_: Paint = Paint()

    val pdfDoc_: PdfDocument = PdfDocument()

    //val builder_: PdfDocument.PageInfo.Builder = PdfDocument.PageInfo.Builder(sizeX_.toInt(), sizeY_.toInt(), currentPageIndex_)
    //val pageInfo_: PdfDocument.PageInfo = builder_.create()
    //val page_: PdfDocument.Page = pdfDoc_.startPage(pageInfo_)

    lateinit var out_: OutputStream
    var currentPageIndex_ = 0
    val pdfBuilderList = ArrayList<PdfDocument.PageInfo.Builder>()
    val pdfPageInfList = ArrayList<PdfDocument.PageInfo>()
    val pdfPageList = ArrayList<PdfDocument.Page>()
    lateinit var currentCanvas_: Canvas// = page_.canvas
    // トランスレート位置の保存
    var viewPointer_ = PointXY(0f,0f)

    fun translate( canvas: Canvas, x: Float, y: Float ){
        canvas.translate(x,y)
        viewPointer_.add(x,y) // rewrite viewPointer but..
    }

    fun startNewPage(sizeX: Int, sizeY: Int, pageIndex: Int){
        pdfBuilderList.add( PdfDocument.PageInfo.Builder( sizeX, sizeY, pageIndex ) )
        // wrong pageNum will bad.
        pdfPageInfList.add( pdfBuilderList[pageIndex].create() )
        pdfPageList.add( pdfDoc_.startPage( pdfPageInfList[pageIndex] ) )
        currentCanvas_ = pdfPageList[pageIndex].canvas
    }

    fun close(){
        //現在のPageの編集を終了する。
        pdfDoc_.finishPage( pdfPageList[currentPageIndex_] )

        writeAllCalcSheets()
        //writeAllCalcSheets()

        pdfDoc_.writeTo(out_)
        pdfDoc_.close()
        out_.close() //MUST.. without write done.
    }

    fun writeAllCalcSheets() {
        var triNumCounter = 0
        triangleList_. counter = 0
        deductionList_.counter = 0

        val maxinpage = 40
        var pageCount = ( triangleList_.size().toDouble() / maxinpage )
//        if( pageCount < 0.1 ) pageCount *= 10
  //      if( pageCount < 0.4 ) pageCount *= 2
        //if( pageCount > 1 ) pageCount = 2f

        val pci = Math.ceil(pageCount).toInt()

        for( i in 0 until pci ){
            //次のページを作る
            currentPageIndex_ += 1 // curindex = + 1 // is dead.
            startNewPage( p2sizeX_.toInt(), p2sizeY_.toInt(), currentPageIndex_ )

            triNumCounter = writeAreaCalcSheet( triNumCounter ) // 面積計算書の描画

            //現在のPageの編集を終了する。
            pdfDoc_.finishPage( pdfPageList[currentPageIndex_] )
        }
    }


    // menseki calc sheet
//    val builder2_: PdfDocument.PageInfo.Builder = PdfDocument.PageInfo.Builder(p2sizeX_.toInt(), p2sizeY_.toInt(), 1);
 //   val pageInfo2_: PdfDocument.PageInfo = builder2_.create();

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
        paintTexS_.textSize = textSize_
        paintTexS_.textAlign = Paint.Align.CENTER
        paintTexS_.letterSpacing = 0.05f
        paintTexS_.strokeWidth = 0.05f

        paintRed_.setARGB(255, 255, 0, 0)
        paintRed_.style = Paint.Style.FILL
        paintRed_.textSize = textSize_
        paintRed_.textAlign = Paint.Align.LEFT
        paintRed_.letterSpacing = 0.05f
        paintRed_.strokeWidth = 0.05f

    }

    fun setScale(drawingLength: PointXY) :Float{
        //1/200　x:80m~40m
        var scale = 2f
        paintTexS_.textSize = 2.5f

        //1/250 (x:80m以上
        if(drawingLength.x > 80 || drawingLength.y > 55 ) {
            scale = 2.5f
            paintTexS_.setTextSize(2f)
        }

        //1/350 (x:120m以上
        if(drawingLength.x > 40*3 || drawingLength.y > 27*3 ) {
            scale = 3.5f
            paintTexS_.setTextSize(0.5f)
        }

        //1/150 (x:40m以下
        if(drawingLength.x < 50 && drawingLength.y < 30 ){
            scale = 1.5f
            paintTexS_.textSize = 50f
        }
        return scale
    }

    fun writeAreaCalcSheet( triNumCounter: Int ) : Int{
        val scaleWeight = 2f
        val scaleHeight = 15f

        // ヘッダー描画
        currentCanvas_.drawText(rStr_.mTitle_, p2sizeX_/2, 10f+(scaleHeight*scale2_), setPaint(7,8f*scale2_,1))
        currentCanvas_.drawText(rStr_.mCname_ + koujiname_, p2sizeX_/2, 10f+(8f*scale2_)+(scaleHeight*scale2_), setPaint(7,4.5f*scale2_,1))
        currentCanvas_.drawText(rosenname_, p2sizeX_/2, 10f+(16f*scale2_)+(scaleHeight*scale2_), setPaint(7,5f*scale2_,1))

        // 外枠
        writeRect( PointXY(p2sizeX_/2, p2sizeY_/2), p2sizeX_-(20*scale2_), p2sizeY_-(20*scale2_), 1f,7)

        // 面積合計
        var allArea = triangleList_.getArea()

        var kei = rStr_.mSyoukei_ +" (1)"
        if(deductionList_.size() == 0) kei = rStr_.mGoukei_



        // 左側
        // Triangles

        val TNC = writeEntities( triangleList_,  30f+(scaleWeight*scale2_), titleTri_, rStr_.mSyoukei_+" (1)", 7, triNumCounter )

        // 右側
        // Deductions
        if(deductionList_.size() > 0) {
            writeEntities( deductionList_, 130f+(scaleWeight*scale2_),  titleDed_, rStr_.mSyoukei_+" (2)", 1, triNumCounter )
            allArea -= deductionList_.getArea()
            writeText( rStr_.mGoukei_ + " (1) - (2) ${allArea.formattedString(2)} m^2", PointXY(198f+(scale2_*scaleWeight),65f+(deductionList_.size()*7f)), scale2_, 7, 5f*scale2_, 2 )

        }

        return TNC
    }

    fun writeEntities(list: EditList, xStart_: Float, sTitle : TitleParamStr, syoukei: String, color: Int, triNumCounter: Int ): Int {
        if( list.counter >= list.size() ) return list.size()

        //s1: String, s2: String, s3: String, s4: String, s5: String, s6: String,
        val s1 = sTitle.n
        val s2 = sTitle.a
        val s3 = sTitle.b
        val s4 = sTitle.c
        val s5 = sTitle.type
        val s6 = syoukei
        val ts = 5f
        val ySpacer = 2f
        val xStart = xStart_
        val yStart = 45f
        val yHeadSpacer = 7.5f
        val xPitch = 17f
        val yPitch = ts + ySpacer
        val xa = xStart + xPitch
        val xb = xa + xPitch
        val xc = xb + xPitch
        val xar = xc + xPitch

        val maxinpage = 40
        var writeBegin = list.counter
        var writeEnd   = list.counter + maxinpage

        if( list.size()  <  writeEnd ) writeEnd = list.size()

        val listArea = list.getArea()

        // ヘッダーテーブル
        writeText( s1, PointXY( xStart, yStart ), scale2_, color, ts*scale2_, 2 )
        writeText( s2, PointXY( xa,     yStart ), scale2_, color, ts*scale2_, 2 )
        writeText( s3, PointXY( xb,     yStart ), scale2_, color, ts*scale2_, 2 )
        writeText( s4, PointXY( xc,     yStart ), scale2_, color, ts*scale2_, 2 )
        writeText( s5, PointXY( xar,    yStart ), scale2_, color, ts*scale2_, 2 )

        var ti = 0
        // エンティティ
        for( i in writeBegin until writeEnd ) {
            writeEntity ( yStart + yHeadSpacer, list.counter, ti, xStart, list.get(i+1), color)
            list.counter += 1
            ti += 1
        }

        // 小計
        if( list.counter == list.size() ) writeText( s6+"   ${listArea} m^2", PointXY( xar,yStart + yHeadSpacer + ( ti*yPitch ) ), scale2_, color, ts*scale2_, 2 )

        return list.counter
    }

    fun writeEntity(yStart: Float, index: Int, indexInPage: Int, xStart_: Float, entity: EditObject, color: Int){
        val ts = 5f
        val xPitch = 17f
        val yPitch = 7f
        val xStart = xStart_
        val xa = xStart + xPitch
        val xb = xa + xPitch
        val xc = xb + xPitch
        val xar = xc + xPitch

        val yOffset = ( yStart + yPitch * indexInPage )

        val tp = entity.getParams()
        var n = tp.n.toString()
        if(entity is Deduction) {
            n = tp.name
            if( entity.sameDedcount > 1 ) n += "("+entity.sameDedcount+")"
        }
        val a = tp.a.formattedString(2)
        val b = tp.b.formattedString(2)
        var c = tp.c.formattedString(2)
        if(entity is Deduction) {
            if( tp.type == "Box" ) c = "長方形"
            if( tp.type == "Circle" ) c = "円"
        }

        val area = entity.getArea().formattedString(2)

        writeText( n, PointXY(xStart,yOffset), scale2_, color, ts*scale2_, 2 )
        writeText( a, PointXY(xa,yOffset), scale2_, color, ts*scale2_, 2 )
        if(entity is Deduction && tp.type == "Box") writeText( b, PointXY(xb,yOffset), scale2_, color, ts*scale2_, 2 )
        if(entity !is Deduction ) writeText( b, PointXY(xb,yOffset), scale2_, color, ts*scale2_, 2 )
        writeText( c, PointXY(xc,yOffset), scale2_, color, ts*scale2_, 2 )
        writeText( area, PointXY(xar,yOffset), scale2_, color, ts*scale2_, 2 )

    }

    fun translateCenter(){
        currentCanvas_.translate(sizeX_/2f,sizeY_/2f )
        viewPointer_.add( sizeX_/2f,sizeY_/2f )
    }


    fun drawOverlayWhite(canvas: Canvas, frameX: Float, frameY: Float, lt: PointXY, rb: PointXY, yohaku: Float){
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

    fun writeRuler(canvas: Canvas){
        val centerX = sizeX_ / 2
        val centerY = sizeY_ / 2
        val ruler = 1190f/42f/printScale_
        val rulerTen = 1190f/4.2f/printScale_
        val fortytwo = 42f * printScale_
        val ten = fortytwo/10f+1f
        val rulerstart = 0f//45f
        val rulerY = centerY + 200f
        val rulercolor = 3
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
        val lt = PointXY(xr-sw, yb-ht)
        val lb = PointXY(xr-sw, yb)
        val rt = PointXY(xr, yb-ht)
        val rb = PointXY(xr, yb)
        val ut = PointXY(xr-uw, yb-ht)
        val ub = PointXY(xr-uw, yb)
        val ft = 320f
        val credit = rStr_.tCredit_
        val centerX = sizeX_ / 2
        val centerY = sizeY_ / 2
        val kt = (sw-uw)/2f+uw
        val ofs = 3f
        val st: Float = printScale_/drawingScale_*100f

        // 枠外を白抜きするための塗りつぶし
        drawOverlayWhite(canvas, frameX, frameY, lt, rb, yohaku)

        // Frame
        writeRect(  getCenter(), frameX, frameY, 1f, 7)

        //tate
        writeLine( lt, lb, 1f,7)
        writeLine( ut, ub, 1f,7)
        //uchi
        writeLine( PointXY(xr-uw2, yb-40f), PointXY(xr-uw2, yb-20f),1f,7)
        writeLine( PointXY(xr-uw3, yb-40f), PointXY(xr-uw3, yb-20f),1f,7)

        //yoko
        writeLine( lt, rt, 1f,7)
        writeLine( PointXY(xr-sw, yb-(hp*1)), PointXY(xr, yb-(hp*1)) ,1f,7)
        writeLine( PointXY(xr-sw, yb-(hp*2)), PointXY(xr, yb-(hp*2)) ,1f,7)
        writeLine( PointXY(xr-sw, yb-(hp*3)), PointXY(xr, yb-(hp*3)) ,1f,7)
        writeLine( PointXY(xr-sw, yb-(hp*4)), PointXY(xr, yb-(hp*4)) ,1f,7)

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
        writeLine( PointXY(centerX-50f,52f), PointXY(centerX+50f,52f),1f,7)
        writeLine( PointXY(centerX-50f,50f), PointXY(centerX+50f,50f),1f,7)
        writeText( rStr_.tTitle_, PointXY(centerX,45f), 1f, 7, 16f, 1 )
        writeText( rosenname_, PointXY(centerX,65f), 1f, 7, 14f, 1 )

        writeText( rStr_.tCname_, PointXY(xr-kt, yb-95f+ofs ), 1f, 7, 8f, 1 )
        writeText( rStr_.tDtype_, PointXY(xr-kt, yb-70f+ofs ), 1f, 7, 8f, 1 )
        writeText( rStr_.tDname_, PointXY(xr-kt, yb-50f+ofs), 1f, 7, 8f, 1 )
        writeText( rStr_.tScale_, PointXY(xr-kt, yb-30f+ofs), 1f, 7, 8f, 1 )
        writeText( rStr_.tNum_, PointXY(xr-75, yb-30f+ofs), 1f, 7, 8f, 1 )
        writeText( rStr_.tAname_, PointXY(xr-kt, yb-10f+ofs), 1f, 7, 8f, 1 )
        writeText( credit, PointXY(50f, yb+10f), 1f, 7, 7f, 0 )


        if( koujiname_.length > 14 ) {
            if( koujiname_.contains(" ") ){
                val array = koujiname_.split(' ')
                writeText(array[0], PointXY(xr - tt, yb - 100f), 1f, 7, 7f, 0)
                writeText(array[1], PointXY(xr - tt, yb - 85f), 1f, 7, 7f, 0)
            }
            else{
                val array1 = koujiname_.substring(0, 12)
                val array2 = koujiname_.substring(12, koujiname_.length)
                writeText(array1, PointXY(xr - tt, yb - 100f), 1f, 7, 7f, 0)
                writeText(array2, PointXY(xr - tt, yb - 85f), 1f, 7, 7f, 0)
            }
        }
        else {
            writeText(koujiname_, PointXY(xr-tt, yb-95f+ofs ), 1f, 7, 6f, 0 )
        }
        //}
        //else writeText(koujiname_, PointXY(xr-tt, yb-95f+ofs ), 1f, 7, 8f, 0 )

        writeText(rStr_.tTitle_, PointXY(xr-tt, yb-70f+ofs ), 1f, 7, 8f, 0 )
        writeText(rosenname_, PointXY(xr-tt, yb-50f+ofs ), 1f, 7, 8f, 0 )
        val ust = (uw-uw2)/2+uw2
        val usst = uw3/2
        writeText("1/${st.toInt()} (A3)", PointXY(xr-ust, yb-30f+ofs ), 1f, 7, 8f, 1 )
        writeText( zumennum_, PointXY(xr-usst, yb-30f+ofs ), 1f, 7, 8f, 1 )
        writeText( gyousyaname_, PointXY(xr-tt, yb-10f+ofs), 1f, 7, 8f, 0 )

        currentCanvas_.translate(sizeX_/2f,sizeY_/2f)
        //  translateCenter()
    }

    override fun writeRect(point: PointXY, sizeX: Float, sizeY: Float, scale: Float, color: Int){
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

    override fun writeLine(p1: PointXY, p2: PointXY, color: Int){
        currentCanvas_.drawLine(p1.x, p1.y, p2.x, p2.y, setPaint(color, 5f, 1))
    }

    override fun writeText(str: String, point: PointXY, scale: Float, color: Int, size: Float, align: Int){

        val paint = this.setPaint(color, size, align)
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

    fun getCenter(): PointXY{
        return PointXY(sizeX_/2f, sizeY_/2f)
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

    fun getPaintFromColor(color: Int): Paint {
        val paint = Paint()
        paint.setARGB(255,255,255,255)
        return paint
    }

}//EOC

