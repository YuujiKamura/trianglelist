package com.jpaver.trianglelist

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.ScaleGestureDetector.OnScaleGestureListener
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

fun Float?.formattedString(fractionDigits: Int): String{
    // nullの場合は空文字
    if(this == null) return ""
    var format : String = "%.${fractionDigits}f"
    return format.format(this)
}

class MyView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    lateinit var scaleGestureDetector: ScaleGestureDetector
    private var time_elapsed: Long = 0
    private var time_start: Long = 0
    private var time_current: Long = 0
    private var distance_current = 0f
    private var distance_start = 0f
    private var flg_pinch_out: Boolean? = null
    private var flg_pinch_in: Boolean? = null
    private var wm: WindowManager? = null
    private var disp: Display? = null
    private var size: Point? = null
    private var screen_width = 0
    private var screen_height = 0
    //画面の対角線の長さ
    private var screen_diagonal = 0

    var textSpacer_ = 5f

    var paintTri: Paint = Paint()
    var paintTexL: Paint = Paint()
    var paintTexM: Paint = Paint()
    var paintTexS: Paint = Paint()
    var paintTexDbg: Paint = Paint()

    var paintYellow: Paint = Paint()
    var paintRed: Paint = Paint()
    var paintBlue: Paint = Paint()
    var paintGray: Paint = Paint()

    var paintFill: Paint = Paint()
    val DarkPink_ = Color.argb(255, 128, 40, 75)
    val DarkOrange_ = Color.argb(255, 128, 75, 40)
    val DarkYellow_ = Color.argb(255, 128, 128, 0)
    val DarkGreen_ = Color.argb(255, 75, 128, 40)
    val DarkBlue_ = Color.argb(255, 40, 75, 128)
    val LightPink_ = Color.argb(255, 255, 180, 200)
    val LightOrange_ = Color.argb(255, 255, 220, 170)
    val LightYellow_ = Color.argb(255, 255, 255, 170)
    val LightGreen_ = Color.argb(255, 225, 255, 155)
    val LightBlue_ = Color.argb(255, 220, 240, 255)
    val White_ = Color.argb(255, 255, 255, 255)
    val Gray_ = Color.argb(255, 50, 50, 50)

    val darkColors_ = arrayOf(DarkPink_, DarkOrange_, DarkYellow_, DarkGreen_, DarkBlue_)
    val lightColors_ = arrayOf(LightPink_, LightOrange_, LightYellow_, LightGreen_, LightBlue_)
    var colorindex_ = 4

    var myScale = 1f
    var myTriangleList: TriangleList = TriangleList()
    var trilistStored_: TriangleList = TriangleList()

    var myDeductionList: DeductionList = DeductionList()
    var mCollisionList: ArrayList<Collision> = ArrayList<Collision>()


    var drawPoint: PointXY = PointXY(0f, 0f)
    var clickPoint: PointXY = PointXY(0f, 0f)
    var lastCPoint: PointXY = PointXY(0f, 0f)
    var moveVector: PointXY = PointXY(0f, 0f)
    var movePoint: PointXY = PointXY(0f, 0f)

    var myViewSize: PointXY = PointXY(0f, 0f)
    var ViewCenter: PointXY = PointXY(0f, 0f)
    var myLocalTriCenter: PointXY = PointXY(0f, 0f)
    var BasePoint: PointXY = PointXY(0f, 0f)
    var transOnce: Boolean = true
    var myLongPressPoint: PointXY = PointXY(0f, 0f)
    var localPressPoint: PointXY = PointXY(0f, 0f)

    var isDoubleTap_ = 0

    var zoomSize: Float = 1.0f

    var parentNum: Int = 0
    var parentSide: Int = 0
    var alpha: Int = 255

    val isSkipDraw_ = true
    var isDebug_ = false
    var isPrintPDF_ = false
    var isAreaOff_ = true

    lateinit var myCanvas: Canvas

    var ts_ = 25f

    var tapTL_: String = "n"

    var watchedB_ = ""
    var watchedC_ = ""

    var shadowTri_ = Triangle( 0f, 0f, 0f )

    fun getViewSize() :PointXY{
        return myViewSize
    }

    fun setFillColor(colorindex: Int, index: Int){
        myTriangleList.get(index).color_ = colorindex

        paintFill.color = darkColors_.get(colorindex)
        colorindex_ = colorindex
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        screen_width = this.width
        screen_height = this.height
        //対角線の長さを求める
        screen_diagonal = Math.sqrt(
            (Math.pow(screen_width.toDouble(), 2.0).toInt() + Math.pow(
                screen_height.toDouble(),
                2.0
            ).toInt()).toDouble()
        ).toInt()

        scaleGestureDetector = ScaleGestureDetector(context, object : OnScaleGestureListener {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                time_current = detector.eventTime
                time_elapsed = time_current - time_start
                if (time_elapsed >= 0.5) {
                    distance_current = detector.currentSpan
                    if (distance_start == 0f) {
                        distance_start = distance_current
                    }
                    flg_pinch_out = distance_current - distance_start > 5
                    flg_pinch_in = distance_start - distance_current > 5
                    if (flg_pinch_out!!) {
                        zoom( ( distance_current - distance_start ) * 0.005f )

                        time_start = time_current
                        distance_start = distance_current
                    } else if (flg_pinch_in!!) {
                        zoom( -( distance_start - distance_current ) * 0.005f )

                        time_start = time_current
                        distance_start = distance_current
                    } else {
                        //pass
                    }
                }

                Log.d("MyView", "OnAttachedToWindow Process Done.")

                return true
            }

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {

                distance_start = detector.eventTime.toFloat()
                if (distance_start > screen_diagonal) {
                    distance_start = 0f
                }
                time_start = detector.eventTime
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {

            }
        })


        val niigogo = 255

        paintFill.strokeWidth = 0.1f
        paintFill.color = darkColors_.get(colorindex_)//Color.argb(255, 128, 50, 75)
//        paintFill.setARGB(255, 255, 200, 230)
        paintFill.style = Paint.Style.FILL

        paintGray.strokeWidth = 0.1f
        paintGray.color = Gray_
        paintGray.style = Paint.Style.FILL
        paintGray.textSize = ts_
        paintGray.textAlign = Paint.Align.CENTER

        paintTri.strokeWidth = 1f
        paintTri.color = Color.argb(255, 255, 255, 255)
        paintTri.style = Paint.Style.STROKE
        paintTri.isAntiAlias = true

        paintTexL.color = Color.argb(255, niigogo, niigogo, niigogo)
        //paintText1.textAlign = Paint.Align.CENTER
        paintTexL.textAlign = Paint.Align.CENTER
        paintTexL.style = Paint.Style.FILL_AND_STROKE
        //paintText1.letterSpacing = 0.2f
        paintTexL.textSize = ts_

        paintTexDbg.color = Color.argb(255, 100, 100, 100)
        paintTexDbg.textAlign = Paint.Align.LEFT
        paintTexDbg.style = Paint.Style.FILL_AND_STROKE
        paintTexDbg.letterSpacing = 0.1f
        paintTexDbg.textSize = ts_

        paintTexM.color = Color.argb(255, niigogo, niigogo, niigogo)
        paintTexM.textAlign = Paint.Align.CENTER
        paintTexM.style = Paint.Style.FILL_AND_STROKE
        //paintTexM.letterSpacing = 0.1f
        paintTexM.textSize = ts_ + 1f

        paintTexS.color = Color.argb(255, niigogo, niigogo, niigogo)
        paintTexS.textAlign = Paint.Align.CENTER
        paintTexS.style = Paint.Style.FILL
        //paintText4.letterSpacing = 0.1f
        paintTexS.textSize = ts_
        //paintTexS.set

        paintYellow.strokeWidth = 3f
        paintYellow.color = Color.argb(255, 255, 255, 0)
        paintYellow.textAlign = Paint.Align.CENTER
        paintYellow.textSize = ts_*1.5f

        paintRed.strokeWidth = 2f
        paintRed.color = Color.argb(255, 255, 0, 0)
        paintRed.style = Paint.Style.FILL
        paintRed.textSize = ts_
        paintRed.letterSpacing = 0.1f

        paintBlue.strokeWidth = 2f
        paintBlue.color = Color.argb(255, 100, 150, 255)
        paintBlue.style = Paint.Style.STROKE
        paintBlue.letterSpacing = 0.1f
        paintBlue.textAlign = Paint.Align.CENTER
        paintBlue.textSize = ts_



    }

    fun setTextSize(tsPlus: Float){
        ts_ = tsPlus
        paintTexL.textSize = ts_
        paintTexM.textSize = ts_
        paintTexS.textSize = ts_
        paintRed.textSize = ts_
        paintBlue.textSize = ts_
        paintTexL.textSize = ts_
        paintGray.textSize = ts_
        paintYellow.textSize = ts_

    }

    fun getScale(): Float{
        return myScale
    }

    fun setParentSide(pn: Int, ps: Int){
        parentNum = pn
        parentSide = ps
        invalidate()
    }

    fun getTriangleList() : TriangleList { return myTriangleList }

    fun setDeductionList(dedlist: DeductionList, scale: Float){
        dedlist.lastTapIndex_ = myDeductionList.lastTapIndex_ //逆に状態をコピー
        myDeductionList = dedlist.clone()
        myDeductionList.setScale( scale )

    }

    fun lsdp(): PointXY{ return myDeductionList.get( myDeductionList.lastTapIndex_ ).point.clone() }

    fun setTriangleList(triList: TriangleList, setscale: Float){
        trilistStored_ = myTriangleList.clone()
        myScale = setscale    // 描画倍率は外から指定する
        myTriangleList = triList.clone()
        myTriangleList.scaleAndSetPath( PointXY(0f, 0f), setscale, paintTexS.textSize )
        setTriListLengthStr()
        setLTP()
        //resetView()
        //invalidate()
        watchedB_ = ""
        watchedC_ = ""
     }

    fun undo(){
        if( trilistStored_.size() > 0 ) myTriangleList = trilistStored_.clone()
    }

    fun setTriListLengthStr(){

        for( i in 0 until myTriangleList.size() ){
            val tri = myTriangleList.get(i+1)
            tri.sla_ = tri.lengthAforce_.formattedString(2)
            tri.slb_ = tri.lengthBforce_.formattedString(2)
            tri.slc_ = tri.lengthCforce_.formattedString(2)
        }

    }

    fun getTapPoint() :PointXY{
        return localPressPoint.clone()
    }

    fun zoom(zoomstep: Float){
        zoomSize += zoomstep
        if(zoomSize<=0.1f) zoomSize = 0.1f
        if(zoomSize>=5) zoomSize = 5f
        //myTriangleList.scale(PointXY(0f,0f), myScale)

        //resetViewNotMove()
        invalidate()
    }

    fun setLTP() {
        myLocalTriCenter.set(myTriangleList.getTriangle(lstn()).pointNumber_)
    }

    fun resetView( pt: PointXY ){
        myLocalTriCenter = pt.clone()
        drawPoint = pt.clone()
        resetPointToZero()
        viewReset()
        invalidate()
    }

    fun resetViewToLSTP(){
        myLocalTriCenter = lstp()
        drawPoint = lstp()
        resetPointToZero()
        viewReset()
        invalidate()
    }

    fun resetViewToTP(){
        val gtp = getTapPoint()
        myLocalTriCenter.set( gtp.x, 0f )
        drawPoint.set( gtp.x, 0f )
        resetPointToZero()
        //BasePoint.set( ViewCenter.x, gtp.y )
        invalidate()
    }

    fun resetPointToZero(){
        movePoint.set(0f, 0f)
        moveVector.set(0f, 0f)
        clickPoint.set(0f, 0f)
        lastCPoint.set(0f, 0f)
        localPressPoint.set(0f, 0f)
    }

    fun viewReset(){
        myViewSize.set( this.width.toFloat(), this.height.toFloat())
        ViewCenter.set( myViewSize.x * 0.5f, myViewSize.y * 0.25f )
        BasePoint.set( ViewCenter.x, ViewCenter.y )
    }

    fun lstp(): PointXY {
        return myTriangleList.getTriangle(lstn()).pointNumber_
    }

    fun lstn(): Int{
        var lstn = myTriangleList.lastTapNum_
        if( lstn < 1 ) lstn = myTriangleList.size()
        return lstn
    }

    fun stp(): PointXY{
        return myTriangleList.getTriangle( myTriangleList.size() ).pointNumber_
    }

    fun transViewPoint(){
        if(transOnce == true) {
            setLTP()
            resetViewToLSTP()
            transOnce = false
        }
    }

    override fun onDraw(canvas: Canvas) {
        transViewPoint()
        canvas.translate(BasePoint.x, BasePoint.y)
        canvas.scale(zoomSize, zoomSize)
        canvas.translate(-drawPoint.x, drawPoint.y)
        // 背景
        val zero = 0
        canvas.drawColor(Color.argb(255, zero, zero, zero))
        myCanvas = canvas

        //if( isSkipDraw_ == true ) return

        drawEntities(canvas, paintTri, paintTexS, paintRed, darkColors_, myTriangleList, myDeductionList )
        //drawDebugData(canvas, PointXY(0.5f*myScale, 0.5f*myScale))
        drawLocalPressPoint( canvas, localPressPoint )
    }

    fun drawEntities(canvas: Canvas, paintTri: Paint, paintTex: Paint, paintRed: Paint, colors: Array<Int>, myTriangleList: TriangleList, myDeductionList: DeductionList ) {

        drawShadowTriangle( canvas, myTriangleList )

        for( i in 0 until myTriangleList.size() )  {
            paintFill.color = colors.get(myTriangleList.get(i + 1).color_)
            canvas.drawPath(makeTriangleFillPath(myTriangleList.get(i + 1)), paintFill)
        }

        for( i in 0 until myDeductionList.size() ) drawDeduction(
            canvas,
            myDeductionList.get(i + 1),
            paintRed, myDeductionList
        )

        for( i in 0 until myTriangleList.size() )  drawTriangle(
            canvas,
            myTriangleList.get(i + 1),
            myScale,
            paintTri,
            paintTex,
            paintTex,
            paintBlue, myTriangleList
        )

        drawBlinkLine( canvas, myTriangleList )
    }

    fun drawTriangle(
        canvas: Canvas,
        tri: Triangle,
        triScale: Float,
        paintTri: Paint,
        paintDim: Paint,
        paintSok: Paint,
        paintB: Paint, myTriangleList: TriangleList
    ){
        // 番号
        drawTriangleNumber(canvas, tri, triScale * 0.4f, paintDim, paintB, myTriangleList)

        // arrange
        val pca = tri.pointCA_
        val pab = tri.pointAB_
        val pbc = tri.pointBC_
        val abca = tri.pathA_
        val abbc = tri.pathB_
        val bcca = tri.pathC_
        val sokt = tri.pathS_

        var la = tri.sla_
        var lb = tri.slb_
        var lc = tri.slc_

        /*
// make Path and Offsets to Dims.
val abca = PathAndOffset( //　反時計回り
    myScale,
    pab,
    pca,
    pbc,
    tri.lengthAforce_,//getLengthAS(1 / triScale),
    tri.myDimAlignA_,
    tri.dimSideAlignA_,
    paintDim.textSize
)
val abbc = PathAndOffset(
    myScale,
    pbc,
    pab,
    pca,
    tri.lengthBforce_,//getLengthBS(1 / triScale),
    tri.myDimAlignB_,
    tri.dimSideAlignB_,
    paintDim.textSize
)
val bcca = PathAndOffset(
    myScale,
    pca,
    pbc,
    pab,
    tri.lengthCforce_,//getLengthCS(1 / triScale),
    tri.myDimAlignC_,
    tri.dimSideAlignC_,
    paintDim.textSize
)
val sokt = PathAndOffset(
    myScale,///myTriangleList.getPrintScale(myScale),
    pab,
    pca,
    pbc,
    tri.lengthAforce_,
    4, 0,
    paintSok.textSize
)
*/
        if( isPrintPDF_ == false ) {
            if (isDebug_ == true) {
                val name = tri.myName_ + " :" + sokt.pointA_.x + " :" + sokt.pointA_.y + " :" + sokt.pointB_.x + " :" + sokt.pointB_.y
                la += " :" + tri.myDimAlignA_ + " :" + tri.dimSideAlignA_
                lb += " :" + tri.myDimAlignB_ + " :" + tri.dimSideAlignB_
                lc += " :" + tri.myDimAlignC_ + " :" + tri.dimSideAlignC_
            }
            else if( tri.myNumber_ == myTriangleList.size() ){
                la += " A"
                lb += " B"
                lc += " C"
                paintDim.color = LightYellow_
            }
            else if( tri.myNumber_ < myTriangleList.size() ) paintDim.color = White_
            else{
                val name = tri.myName_
            }
        }


        abca.textSpacer_ = textSpacer_
        abbc.textSpacer_ = textSpacer_
        bcca.textSpacer_ = textSpacer_
        sokt.textSpacer_ = textSpacer_

        drawTriLines( canvas, tri, paintTri )

        val margin = paintDim.textSize*0.52f

        // 寸法
        if(tri.getMyNumber_() == 1 || tri.parentBC > 2 || tri.cParam_.type != 0 )
            drawDigits( canvas, la, makePath(abca), abca.offsetH_, abca.offsetV_, paintDim, margin )
        //canvas.drawTextOnPath(la, makePath(abca), abca.offsetH_, abca.offsetV_, paintDim)
        drawDigits( canvas, lb, makePath(abbc), abbc.offsetH_, abbc.offsetV_, paintDim, margin )
        drawDigits( canvas, lc, makePath(bcca), bcca.offsetH_, bcca.offsetV_, paintDim, margin )
        //canvas.drawTextOnPath(lb, makePath(abbc), abbc.offsetH_, abbc.offsetV_, paintDim)
        //canvas.drawTextOnPath(lc, makePath(bcca), bcca.offsetH_, bcca.offsetV_, paintDim)

        // 測点
        if(tri.getMyName_() != ""){
            canvas.drawTextOnPath(tri.getMyName_(), makePath(sokt), 0f, -2f, paintSok)
            canvas.drawPath(makePath(sokt), paintTri)
        }
    }

    fun drawShadowTriangle( canvas: Canvas, myTriangleList: TriangleList ){
        if( isPrintPDF_ == true || myTriangleList.lastTapSide_ < 1 || myTriangleList.isDoubleTap_ == false ) {
            shadowTri_ = Triangle( 0f, 0f, 0f )
            return
        }

        if( myTriangleList.lastTapNum_ < 1 ) myTriangleList.lastTapNum_ = myTriangleList.size()
        val shadowParent = myTriangleList.get(number = myTriangleList.lastTapNum_ )
        val shadowTapSide = myTriangleList.lastTapSide_

        //番号選択されてるときは以下略。
        if( shadowTapSide != 3) {
            val shadowTapNum = myTriangleList.lastTapNum_
            val shadowTapLength = shadowParent.getLengthByIndexForce( shadowTapSide ) * 0.75f
            shadowTri_ = Triangle( shadowParent, myTriangleList.lastTapSide_, shadowTapLength, shadowTapLength )
            //shadowTri.setDimPoint()
            val spca = shadowTri_.pointCA_
            val spab = shadowTri_.pointAB_
            val spbc = shadowTri_.pointBC_

            shadowTri_.setScale( myTriangleList.myScale )
            canvas.drawPath( makeTriangleFillPath( shadowTri_ ), paintGray )
            //        drawTriLines( canvas, shadowTri, paintGray )
            canvas.drawTextOnPath( "B " + watchedB_, makePath( spbc,  spab ), 0f, 0f, paintYellow )
            canvas.drawTextOnPath( "C " + watchedC_, makePath( spca,  spbc ), 0f, 0f, paintYellow )
        }
    }

    fun drawDeduction(canvas: Canvas, ded: Deduction, paint: Paint, myDeductionList: DeductionList ){
        var str = ded.info_
        val point = ded.point
        var pointFlag = ded.pointFlag

        if(isAreaOff_ == false ) str += " : -" + ded.getArea().formattedString(2)+"㎡"
        var infoStrLength: Float = str.length * paint.textSize * 0.85f

        // boxの時は短くする
        if(ded.type=="Box") infoStrLength = infoStrLength*0.75f

        if( isDebug_ == true ){
            val strD = ded.point.x.toString() + " " + ded.point.y.toString()
            val strDF = ded.pointFlag.x.toString() + " " + ded.pointFlag.y.toString()
            canvas.drawText(strD, pointFlag.x, pointFlag.y-50f, paint)
            canvas.drawText(strDF, pointFlag.x, pointFlag.y-100f, paint)
            canvas.drawCircle(point.x, point.y, ded.lengthX / 2 * ded.scale_, paintYellow)
        }

        if(point.x <= pointFlag.x) {  //pointFlag is RIGHT from DedCenter
            if(pointFlag.x != 0f) {
                canvas.drawLine(point.x, point.y, pointFlag.x, pointFlag.y, paint)
                drawDedText(
                    canvas,
                    str,
                    pointFlag,//.plus(3f, 0f),
                    pointFlag.plus(infoStrLength, 0f),
                    paint
                )
            }
            else                  drawDedText(
                canvas, str, point, point.plus(
                    infoStrLength,
                    0f
                ), paint
            )
        } else {                     //pointFlag is LEFT from DedCenter
            if(pointFlag.x != 0f) {
                //中継ぎ線
                canvas.drawLine(point.x, point.y, pointFlag.x, pointFlag.y, paint)
                // フラッグから文字の長さを引いた点が基準
                drawDedText(
                    canvas, str, pointFlag.plus(
                        -infoStrLength,
                        0f
                    ), pointFlag, paint
                )
            }
            else                  drawDedText(
                canvas, str, point.plus(
                    -infoStrLength,
                    0f
                ), point, paint
            )
        }

        // サークル内を色抜きするために一時書き換えて戻す
        if(ded.type == "Circle"){
            paint.style = Paint.Style.STROKE
            canvas.drawCircle(point.x, point.y, ded.lengthX / 2 * ded.scale_, paint)
            paint.style = Paint.Style.FILL
        }
        if(ded.type == "Box")    drawDedRect(canvas, point, ded, paint)


    }

    fun drawLocalPressPoint(canvas: Canvas, point: PointXY){
        if(point.x != 0f){
            canvas.drawLine(
                point.x - 20f, point.y,
                point.x + 20f, point.y, paintRed
            )
            canvas.drawLine(
                point.x, point.y - 20f,
                point.x, point.y + 20f, paintRed
            )

            // draw pdf の時だけ使う
            //if( BuildConfig.BUILD_TYPE == "debug" ) canvas.drawText( point.x.toString() + " " + point.y.toString(), point.x+10f, point.y+10f, paintRed)
        }

        if( isDebug_ == true ){
            canvas.drawText(
                "localpress: " + localPressPoint.info(),
                localPressPoint.x,
                localPressPoint.y + 50f,
                paintRed
            )
            canvas.drawText(
                "clickpoint: " + clickPoint.info(),
                localPressPoint.x,
                localPressPoint.y + 100f,
                paintRed
            )
            canvas.drawText(
                "ltcenter  : " + myLocalTriCenter.info(),
                localPressPoint.x,
                localPressPoint.y + 150f,
                paintRed
            )
            canvas.drawText(
                "activeP  : " + mActivePointerId,
                localPressPoint.x,
                localPressPoint.y + 200f,
                paintRed
            )
            canvas.drawText(
                "pCount  : " + mPointerCount,
                localPressPoint.x,
                localPressPoint.y + 250f,
                paintRed
            )
            canvas.drawText(
                "ltnD  : " + myDeductionList.lastTapIndex_,
                localPressPoint.x,
                localPressPoint.y + 300f,
                paintRed
            )
        }
    }

    fun drawBlinkLine( canvas: Canvas, myTriangleList: TriangleList ){
        if( myTriangleList.lastTapNum_ < 1 || myTriangleList.lastTapSide_ < 0 || isPrintPDF_ == true ) return

        paintYellow.color = Color.argb(alpha, 255, 255, 0)

        val tri = myTriangleList.get( myTriangleList.lastTapNum_ )

        if(myTriangleList.lastTapSide_ == 0){
            canvas.drawLine(
                tri.pointCA_.x, - tri.pointCA_.y,
                tri.pointAB_.x, - tri.pointAB_.y,
                paintYellow
            )
        }
        if(myTriangleList.lastTapSide_ == 1){
            canvas.drawLine(
                tri.pointAB_.x, - tri.pointAB_.y,
                tri.pointBC_.x, - tri.pointBC_.y,
                paintYellow
            )
        }
        if(myTriangleList.lastTapSide_ == 2) {
            canvas.drawLine(
                tri.pointCA_.x, - tri.pointCA_.y,
                tri.pointBC_.x, - tri.pointBC_.y,
                paintYellow
            )
        }

        val circleSize = paintTexS.textSize *0.8f
        paintYellow.style = Paint.Style.STROKE

        if( tri.myNumber_ == myTriangleList.lastTapNum_ && isPrintPDF_ == false ) canvas.drawCircle(
            tri.pointNumberAutoAligned_.x,
            -tri.pointNumberAutoAligned_.y,
            circleSize,
            paintYellow
        )
        paintYellow.style = Paint.Style.FILL

    }



    fun drawLine(canvas: Canvas, p1: PointXY, p2: PointXY, sx: Float, sy: Float, paint: Paint){
        canvas.drawLine(p1.x * sx, p1.y * sy, p2.x * sx, p2.y * sy, paint)
    }

    fun drawTriLines( canvas: Canvas, tri: Triangle, paintTri: Paint, ){
        // arrange
        val pca = tri.pointCA_
        val pab = tri.pointAB_
        val pbc = tri.pointBC_

        // TriLines
        drawLine(canvas, pca, pab, 1f, -1f, paintTri)
        drawLine(canvas, pab, pbc, 1f, -1f, paintTri)
        drawLine(canvas, pbc, pca, 1f, -1f, paintTri)
    }

    fun drawDigits( canvas: Canvas, str: String, path: Path, offsetH: Float, offsetV: Float, paint: Paint, margin: Float ){
        for( index in 0 .. str.length-1 ){
            canvas.drawTextOnPath(str.get(index).toString(), path, offsetH+((index-2)*margin), offsetV, paint )
        }
    }

    fun drawTriangleNumber(
        canvas: Canvas,
        tri: Triangle,
        triScale: Float,
        paint1: Paint,
        paint2: Paint, myTriangleList: TriangleList
    ){
        var mn: String = tri.getMyNumber_().toString()
        val pnX = tri.pointNumberAutoAligned_.x
        val pnY = -tri.pointNumberAutoAligned_.y
        val pnpY = getPaintCenterY(pnY, paint1)
        val circleSize = paint2.textSize *0.8f
        val areaoffsetY = paint2.textSize *1.3f
        paint2.style = Paint.Style.STROKE


        var area =""
        if(isAreaOff_ == false ) area = tri.getArea().formattedString(1 )+"m^2"
        if(isDebug_ == true) area = "cp:"+tri.cParam_.type.toString() + "-"+ tri.cParam_.lcr.toString() +" pbc:"+ tri.parentBC_.toString() +" Num:"+ tri.myNumber_ +"-"+ tri.parentNumber_ +" lTS"+ tri.lastTapSide_


        canvas.drawCircle(pnX, pnY, circleSize, paint2)
        canvas.drawText(mn, pnX, pnpY, paint1)
        paint2.style = Paint.Style.FILL
        canvas.drawText(area, pnX, pnpY - areaoffsetY, paint2)

        //引き出し矢印線の描画
        if( tri.isCollide(tri.pointNumber_) == false ){
            val pc = tri.pointCenter_
            val pn = tri.pointNumber_
            val pcOffsetToN = pc.offset(pn, circleSize * 0.5f )
            val pnOffsetToC = pn.offset(pc, circleSize * 1.1f )
            val arrowTail = pcOffsetToN.offset(pn, pcOffsetToN.lengthTo(pnOffsetToC) * 0.7f).rotate(pcOffsetToN, 5f)
            canvas.drawLine(
                pcOffsetToN.x,
                -pcOffsetToN.y,
                pnOffsetToC.x,
                -pnOffsetToC.y,
                paint2
            )

            canvas.drawLine(
                pcOffsetToN.x,
                -pcOffsetToN.y,
                arrowTail.x,
                -arrowTail.y,
                paint2
            )
        }
    }

    fun drawDedText(canvas: Canvas, st: String, p1: PointXY, p2: PointXY, paint: Paint){
        val pt = p1.plus(textSpacer_*2, -textSpacer_) //オフセット
        canvas.drawText(st, pt.x, pt.y, paint)
        canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint)
    }

    fun drawDedRect(canvas: Canvas, point: PointXY, dd: Deduction, paint: Paint){
        dd.setBox( dd.scale_ )
        drawLine(canvas, dd.plt, dd.plb, 1f, 1f, paint)
        drawLine(canvas, dd.plt, dd.prt, 1f, 1f, paint)
        drawLine(canvas, dd.plb, dd.prb, 1f, 1f, paint)
        drawLine(canvas, dd.prt, dd.prb, 1f, 1f, paint)
    }

    fun calcAlignByInnerAngleOf(tri: Triangle, ABC: Int): Int{

        if(ABC == 0 && tri.parentBC > 2 ) return 3
        if(ABC == 1 && (tri.childSide_ == 3 || tri.childSide_ == 4 || tri.childSide_ == 7)) return 1
        if(ABC == 2 && (tri.childSide_ == 5 || tri.childSide_ == 6 || tri.childSide_ == 8)) return 1

        return 3
    }

    fun makePath( p1: PointXY, p2: PointXY ): Path {
        val path = Path()
        path.rewind()
        path.moveTo(p1.x, -p1.y)
        path.lineTo(p2.x, -p2.y)
        return path
    }

    fun makePath(PA: PathAndOffset): Path {
        val path = Path()
        path.rewind()
        path.moveTo(PA.pointA_.x, -PA.pointA_.y)
        path.lineTo(PA.pointB_.x, -PA.pointB_.y)
        return path
    }

    fun makeTriangleFillPath(tri: Triangle): Path {
        val path = Path()
        path.rewind()
        path.moveTo(tri.pointCA_.x, -tri.pointCA_.y)
        path.lineTo(tri.pointAB_.x, -tri.pointAB_.y)
        path.lineTo(tri.pointBC_.x, -tri.pointBC_.y)
        path.lineTo(tri.pointCA_.x, -tri.pointCA_.y)
        return path
    }

    fun getPaintCenterY(Y: Float, paint: Paint): Float{
        val metrics: Paint.FontMetrics = paint.fontMetrics //FontMetricsを取得
        val myy: Float = Y - ( (metrics.ascent + metrics.descent) / 2 )
        return myy
    }

    fun setAllTextSize(ts: Float){
        paintTexS.textSize = ts
        paintBlue.textSize = ts
        paintRed.textSize = ts
        paintTexM.textSize = ts
        textSpacer_ = ts_ * 0.2f
        myTriangleList.setPath( ts_ )
    }

    fun drawDebugData(canvas: Canvas, dp: PointXY){
        var dpY = dp.y
        val dpPlus = 6f

        canvas.drawText(
            "localpress_x:" + localPressPoint.x.toString(),
            dp.x,
            dpY,
            paintTexDbg
        )
        dpY = dpY+dpPlus
        canvas.drawText(
            "localpress_y:" + localPressPoint.y.toString(),
            dp.x,
            dpY,
            paintTexDbg
        )
        dpY = dpY+dpPlus
        canvas.drawText("TapTL:" + tapTL_, dp.x, dpY, paintTexDbg)
        dpY = dpY+dpPlus
        canvas.drawText(
            "TriList" + myTriangleList.size() + "-B:" + myTriangleList.get(myTriangleList.size()).dimPointB_.x + " " + myTriangleList.get(
                myTriangleList.size()
            ).dimPointB_.y, dp.x, dpY, paintTexDbg
        )
        dpY = dpY+dpPlus
        canvas.drawText(
            "Tap in triangle number - " + myTriangleList.lastTapCollideNum_.toString(),
            dp.x,
            dpY,
            paintTexDbg
        )


        /*
        canvas.drawText("面積（控除なし,単位: ㎡）: "+myTriangleList.getArea().toString(), dp.getX(), dpY, paintTexDbg)
        dpY = dpY+dpPlus
        canvas.drawText("Dlength:"+myDeductionList.size().toString(), dp.getX(), dpY, paintTexDbg)
        dpY = dpY+dpPlus
        canvas.drawText("Dcurrent:"+myDeductionList.getCurrent().toString(), dp.getX(), dpY, paintTexDbg)
        dpY = dpY+dpPlus
        canvas.drawText("Tlength:"+myTriangleList.size().toString(), dp.getX(), dpY, paintTexDbg)
        dpY = dpY+dpPlus
        canvas.drawText("Tcurrent:"+myTriangleList.getCurrent().toString(), dp.getX(), dpY, paintTexDbg)
        dpY = dpY+dpPlus
        canvas.drawText("movevect_y:"+moveVector.getY().toString(), dp.getX(), dpY, paintTexDbg)
        dpY = dpY+dpPlus
        canvas.drawText("movep_x:"+movePoint.getX().toString(), dp.getX(), dpY, paintTexDbg)
        dpY = dpY+dpPlus
        canvas.drawText("movep_y:"+movePoint.getY().toString(), dp.getX(), dpY, paintTexDbg)
        dpY = dpY+dpPlus
        canvas.drawText("trans_x:"+TransPoint.getX().toString(), dp.getX(), dpY, paintTexDbg)
        dpY = dpY+dpPlus
        canvas.drawText("trans_y:"+TransPoint.getY().toString(), dp.getX(), dpY, paintTexDbg)
        dpY = dpY+dpPlus
        canvas.drawText("localtri_x:"+myLocalTriCenter.getX().toString(), dp.getX(), dpY, paintTexDbg)
        dpY = dpY+dpPlus
        canvas.drawText("localtri_y:"+myLocalTriCenter.getY().toString(), dp.getX(), dpY, paintTexDbg)
        dpY = dpY+dpPlus
        canvas.drawText("drawp_x:"+drawPoint.getX().toString(), dp.getX(), dpY, paintTexDbg)
        dpY = dpY+dpPlus
        canvas.drawText("drawp_y:"+drawPoint.getY().toString(), dp.getX(), dpY, paintTexDbg)
        dpY = dpY+dpPlus
        canvas.drawText("longpress_x:"+myLongPressPoint.getX().toString(), dp.getX(), dpY, paintTexDbg)
        dpY = dpY+dpPlus
        canvas.drawText("longpress_y:"+myLongPressPoint.getY().toString(), dp.getX(), dpY, paintTexDbg)
        dpY = dpY+dpPlus
        */
    }

    fun drawLongPressPoint(canvas: Canvas){
        if(myLongPressPoint.x != 0f){
            canvas.drawLine(
                myLongPressPoint.x - 5f, myLongPressPoint.y - 5f,
                myLongPressPoint.x + 5f, myLongPressPoint.y + 5f, paintRed
            )
        }
    }

    fun drawPDF(
        writer: PdfWriter,
        canvas: Canvas,
        paintTri: Paint,
        paintTex: Paint,
        paintRed: Paint,
        textscale: Float,
        experience: Float
    ): PointXY { // 追跡されたcanvasの移動ベクトルを返す
        this.paintBlue.textSize = textscale
        this.paintBlue.strokeWidth = 0.05f


        isPrintPDF_ = true

        val printScale = myTriangleList.getPrintScale(myScale)

        // テキストスペーサー
        if( printScale > 3.0 ) textSpacer_ = 0.5f
        if( printScale > 5.0 ) textSpacer_ = 0.2f
        else textSpacer_ = 2f

        // 用紙の単位にリストの大きさを合わせる
        //

        val scaleFactor = 1.19f * writer.kaizoudo_ *(2.0f/experience/printScale)// - (myScale/100)
        myScale *= scaleFactor
        // scale
        myTriangleList.scaleAndSetPath( PointXY(0f, 0f), scaleFactor, paintTex.textSize )
        myDeductionList.scale(PointXY(0f, 0f), scaleFactor)
        myDeductionList.setScale( myScale )


        //ここから横長図形の分割処理
//        var num = 2
//        val printableW = //myTriangleList.getUnScaledPointOfName( "No."+num ).x//40f
        val printAreaW = 40f*printScale
        val printAreaH = 29.7f*printScale

        //スケールされてないリストの幅を測って、分割回数を計算する
        val drawingRect = myTriangleList.measureLongLineNotScaled()
        val separateCount = ( drawingRect.x / printAreaW ).roundToInt() + 1

        //キャンバスをどれだけ動かすか決める。幅は固定値、縦はリストのナンバーを検索し、ナンバー上のA辺の長さによって動かす。
        var wTransLen =  printAreaW * myTriangleList.myScale
        var lastPointX = 0f


        // 縦方向の移動量
        val hYohaku = 75f
        val hkaishi = 5f
        var zukeinohaba = 5f
        val halfAreaH = printAreaH*0.5f*myTriangleList.myScale

        // canvasの動きを追跡する
        var printPoint = PointXY(0f,0f)
        val numberList = myTriangleList.getSokutenList( 2, 4 )

        if( separateCount > 1 && numberList.size > 1 && myTriangleList.get(0).angleInGlobal_ > 45f && myTriangleList.get(0).angleInGlobal_ < 135f  ){

            //測点を持つ三角形のリスト
            val numleftList = myTriangleList.getSokutenList( 0, 4 )
            numleftList.add(0, Triangle(5f, 5f, 5f) )

            if( myTriangleList.myAngle < 0 && numberList.size > 1 ){
                Collections.reverse( numberList )
                Collections.reverse( numleftList )
            }
            var numkyori = 0f
            var numkyoriC = 0f
            var numkyoriL = 0f

            for( i in 0 until numberList.size ){

                //測点の座標
                val numcenPCAy = numberList[i].pointCA_.y
                val numlefPCAy = numleftList[i].pointCA_.y
                if( i > 0 ){
                    //前回の測点の座標と距離
                    val numcenzenPCAy = numberList[i - 1].pointCA_.y
                    val numlefzenPCAy = numleftList[i - 1].pointCA_.y
                    numkyoriC = numcenPCAy - numcenzenPCAy
                    numkyoriL = numlefPCAy - numlefzenPCAy
                    val katamuki = numkyoriC - numkyoriL
                    numkyori = numkyoriC - ( katamuki * 1f )
                }

                zukeinohaba = numberList.get(i).lengthA_ * 2
                var hTransLen = zukeinohaba + hYohaku + hkaishi// 分割数で上下の描画位置を変えたらいけない。

                // mirror -X +Y
                //最初の一回目
                if(i == 0){
                    // 初期位置に動かす。
                    printPoint.add( -numberList[0].pointCA_.x,  - halfAreaH + (hTransLen) + numcenPCAy ) //* 0.75f ) )

                    canvas.translate( printPoint.x, printPoint.y )
                }
                else{
                    // 次の中心座標へ動かす
                    if( i > 0) lastPointX = numberList.get( i - 1 ).pointCA_.x
                    wTransLen  = numberList.get(i).pointCA_.x - lastPointX

                    // 縦にのびていく理由はなんだろう？それは図形の座標差が蓄積されていくからと思われる
                    hTransLen = ( zukeinohaba + hYohaku + numkyori )

                    printPoint.add( -wTransLen, hTransLen )
                    canvas.translate( -wTransLen, hTransLen )
                }

                //描画
                drawEntities(canvas, paintTri, paintTex, paintRed, lightColors_, myTriangleList, myDeductionList )
            }
        }
        else{
            // mirror -X +Y
            canvas.translate(-myTriangleList.center.x, myTriangleList.center.y)

            drawEntities(canvas, paintTri, paintTex, paintRed, lightColors_, myTriangleList, myDeductionList )

            // mirror +X -Y
            canvas.translate(myTriangleList.center.x, -myTriangleList.center.y)
        }

        // canvas を大きくしてより小さいテキストを描画するテスト
        // textsizeが小さすぎて字間が崩れているわけではないのか
        val paintmin = Paint()
        paintmin.textSize = 10f
//        paintmin.fontSpacing = 0.5f
        paintmin.color = Gray_

        paintmin.letterSpacing = 0.05f
        //canvas.drawText( "Text minimalize test", 0f, -60f, paintmin )


//        canvas.scale( 4f,4f )

        //    paintmin.letterSpacing = 0.5f
        //      canvas.drawText( "Text minimalize test", 0f, 0f, paintmin )

        //  canvas.scale( 0.25f,0.25f )
        //canvas.drawText( "Text minimalize test", 0f, 30f, paintmin )


        //scale back
        myScale /= scaleFactor
        myTriangleList.scaleAndSetPath( PointXY(0f, 0f), 1 / scaleFactor, paintTexS.textSize )
        myDeductionList.scale(PointXY(0f, 0f), 1 / scaleFactor)
        myDeductionList.setScale(myScale)
        //isAreaOff_ = true
        isPrintPDF_ = false
        this.paintBlue.textSize = ts_
        this.paintBlue.strokeWidth = 2f
        //this.paintFill.color = darkColors_.get(colorindex_)
        textSpacer_ = 5f

        return printPoint
    }

    private var mActivePointerId = -1
    private var mPointerCount = -1
    private var mLastC = -1
    private var mLastTouch = PointXY(0f,0f)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        this.scaleGestureDetector.onTouchEvent(event)

        mPointerCount = event.pointerCount
        mActivePointerId = event.getPointerId(0)

            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    if( mActivePointerId != 1 ) {
                        clickPoint.set(event.x, event.y)
                        moveVector.set(
                            clickPoint.x - lastCPoint.x,
                            clickPoint.y - lastCPoint.y
                        )
                        BasePoint.set(
                            movePoint.x + moveVector.x,
                            movePoint.y + moveVector.y
                        )
                    }

                }
                MotionEvent.ACTION_DOWN -> {

                    if( event.pointerCount == 1 ) {

                        clickPoint.set(event.x, event.y)
                        lastCPoint.set(event.x, event.y)
                        localpressReset()
                    }
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                }
                MotionEvent.ACTION_UP -> {
                        mPointerCount = event.pointerCount
                        mActivePointerId = -1

                        (context as MainActivity).setTargetEditText(tapTL_)

                    }

                MotionEvent.ACTION_POINTER_UP -> {

                    }
            }
            invalidate()
            //resetView()

        mLastC = mPointerCount
        return true
    }

    fun localpressReset(){
        movePoint.set(BasePoint.x, BasePoint.y)
        localPressPoint = clickPoint.convertToLocal(
            BasePoint,
            zoomSize,
            zoomSize,
            myLocalTriCenter
        )

    }

}

