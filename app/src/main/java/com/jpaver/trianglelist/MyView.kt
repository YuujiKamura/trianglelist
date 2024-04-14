
package com.jpaver.trianglelist

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_UP
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.view.GestureDetectorCompat
import com.jpaver.trianglelist.util.MyScaleGestureListener
import com.jpaver.trianglelist.util.RotateGestureDetector
import com.jpaver.trianglelist.util.ScaleGestureCallback
import com.jpaver.trianglelist.util.ViewTranslateManager

import java.util.*
import kotlin.math.roundToInt

// region utilities why this?
fun Float?.formattedString(fractionDigits: Int): String{
    // nullの場合は空文字
    if(this == null) return ""

    // 小数点以下の末尾ゼロに基づいてfractionDigitsを調整
    val smart_fractionDigits = if ((this * 100).toInt() % 10 == 0) 1 else fractionDigits

    val format = "%.${smart_fractionDigits}f"
    val spaced_format = spaced_by(smart_fractionDigits) + format.format(this)
    return spaced_format
}

fun spaced_by(number: Int): String{
    return " ".repeat( 2 - number )
}

//endregion utilities exist!

class MyView(context: Context, attrs: AttributeSet?) :
    View(context, attrs),
    GestureDetector.OnGestureListener,
    ScaleGestureCallback {

// region parameters

    lateinit var rotateGestureDetector: RotateGestureDetector
    lateinit var scaleGestureDetector: ScaleGestureDetector
    lateinit var mDetector: GestureDetectorCompat

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
    val LightYellow_ = Color.argb(255, 255, 255, 100)
    val LightGreen_ = Color.argb(255, 225, 255, 155)
    val LightBlue_ = Color.argb(255, 220, 240, 255)
    val White_ = Color.argb(255, 255, 255, 255)
    val Gray_ = Color.argb(255, 50, 50, 50)

    val darkColors_ = arrayOf(DarkPink_, DarkOrange_, DarkYellow_, DarkGreen_, DarkBlue_)
    val lightColors_ = arrayOf(LightPink_, LightOrange_, LightYellow_, LightGreen_, LightBlue_)
    var colorindex_ = 4

    var myScale = 1f
    var trianglelist: TriangleList = TriangleList()
    var myDeductionList: DeductionList = DeductionList()

    var deductionMode = false

    //var drawPoint: PointXY = PointXY(0f, 0f)
    var pressedInView: PointXY =
        PointXY(0f, 0f)
    var lastCPoint: PointXY =
        PointXY(0f, 0f)
    var moveVector: PointXY =
        PointXY(0f, 0f)
    var translatePoint: PointXY =
        PointXY(0f, 0f)

    var viewSize: PointXY =
        PointXY(0f, 0f)
    var centerInView: PointXY =
        PointXY(0f, 0f)
    var centerInModel: PointXY =
        PointXY(0f, 0f)
    var baseInView: PointXY =
        PointXY(0f, 0f)
    var transOnce: Boolean = true
    var pressedInModel: PointXY =
        PointXY(0f, 0f)

    var zoomSize: Float = 1.0f
    var mFocusX = 0f
    var mFocusY = 0f
    var isViewScaling = false
    var isViewScrolling = false
    var touchCounter = 0

    var isDebug_ = false
    var isPrintPDF_ = false
    var isAreaOff_ = true

    var textSize = 30f

    var watchedA1_ = ""
    var watchedB1_ = ""
    var watchedC1_ = ""
    var watchedA2_ = ""
    var watchedB2_ = ""
    var watchedC2_ = ""

    var pointerCount = 0
        private set

    fun setPointerCountForTest(count: Int) {
        pointerCount = count
    }

    fun getPointerCountForTest(): Int {
        return pointerCount
    }

    fun setWatchedStrings(sa1:String,sb1:String,sc1:String,sa2:String,sb2:String,sc2:String ){
        watchedA1_ = sa1
        watchedB1_ = sb1
        watchedC1_ = sc1
        watchedA2_ = sa2
        watchedB2_ = sb2
        watchedC2_ = sc2
    }

    var shadowTri_ = Triangle( 0f, 0f, 0f )

    fun setFillColor(colorindex: Int, index: Int){
        trianglelist.get(index).color_ = colorindex

        paintFill.color = darkColors_.get(colorindex)
        colorindex_ = colorindex
        invalidate()
    }

    private fun initParams(){
        Log.d("MyViewLifeCycle", "initParams")
        val niigogo = 255

        paintFill.strokeWidth = 0.1f
        paintFill.color = darkColors_.get(colorindex_)//Color.argb(255, 128, 50, 75)
//        paintFill.setARGB(255, 255, 200, 230)
        paintFill.style = Paint.Style.FILL

        paintGray.strokeWidth = 0.1f
        paintGray.color = Gray_
        paintGray.style = Paint.Style.FILL
        paintGray.textSize = textSize
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
        paintTexL.textSize = textSize

        paintTexDbg.color = Color.argb(255, 100, 255, 100)
        paintTexDbg.textAlign = Paint.Align.LEFT
        paintTexDbg.style = Paint.Style.FILL_AND_STROKE
        paintTexDbg.letterSpacing = 0.1f
        paintTexDbg.textSize = textSize

        paintTexM.color = Color.argb(255, niigogo, niigogo, niigogo)
        paintTexM.textAlign = Paint.Align.CENTER
        paintTexM.style = Paint.Style.FILL_AND_STROKE
        //paintTexM.letterSpacing = 0.1f
        paintTexM.textSize = textSize + 1f

        paintTexS.color = Color.argb(255, niigogo, niigogo, niigogo)
        paintTexS.textAlign = Paint.Align.CENTER
        paintTexS.style = Paint.Style.FILL
        //paintText4.letterSpacing = 0.1f
        paintTexS.textSize = textSize
        //paintTexS.set

        paintYellow.strokeWidth = 7f
        paintYellow.color = Color.argb(100, 255, 255, 0)
        paintYellow.textAlign = Paint.Align.CENTER
        paintYellow.textSize = textSize*1.2f

        paintRed.strokeWidth = 2f
        paintRed.color = Color.argb(255, 255, 0, 0)
        paintRed.style = Paint.Style.FILL
        paintRed.textSize = textSize
        paintRed.letterSpacing = 0.1f

        paintBlue.strokeWidth = 2f
        paintBlue.color = Color.argb(255, 100, 150, 255)
        paintBlue.style = Paint.Style.STROKE
        paintBlue.letterSpacing = 0.1f
        paintBlue.textAlign = Paint.Align.CENTER
        paintBlue.textSize = textSize

    }

// endregion

// region lifecycle
    fun setupGestureDetector() {
        scaleGestureDetector = ScaleGestureDetector(context, MyScaleGestureListener(this))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        initParams()

        mDetector = GestureDetectorCompat(context, this)

        rotateGestureDetector = RotateGestureDetector(object : RotateGestureDetector.SimpleOnRotateGestureDetector() {
            override fun onRotate(degrees: Float, focusX: Float, focusY: Float): Boolean {
                (context as MainActivity).fabRotate(-degrees * 3, false, false)

                return true
            }
        })

        setScreenSize()

        setupGestureDetector()

        Log.d("MyViewLifeCycle", "OnAttachedToWindow Process Done.")

    } // end onAttachedToWindow

    fun setScreenSize() {
        screen_width = this.width
        screen_height = this.height
        //対角線の長さを求める
        screen_diagonal = Math.sqrt(
            (Math.pow(screen_width.toDouble(), 2.0).toInt() + Math.pow(
                screen_height.toDouble(),
                2.0
            ).toInt()).toDouble()
        ).toInt()
        Log.d("MyViewLifeCycle", "SetScreenSize.")
    }

// endregion

// region screen onTouchEvent

    fun adjustZoomSize(zoomStep: Float): Float {
        var newZoomSize = zoomSize + zoomStep
        newZoomSize = newZoomSize.coerceIn(0.1f, 5f) // 0.1f と 5f の間に制限する
        return newZoomSize
    }

    private var isScaleBegin = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {

        touchCounter = event.pointerCount
        when( touchCounter ){
            1 -> {
                if(!isViewScaling){
                    isViewScrolling = true
                    mDetector.onTouchEvent(event)
                    setPressEvent(event.x, event.y)
                    invalidate()
                }
            }
            2 -> {
                    this.scaleGestureDetector.onTouchEvent(event)
                    this.rotateGestureDetector.onTouchEvent(event)

                    if (!isScaleBegin) {
                        isViewScaling = true
                        isScaleBegin = true  // スケールが開始されたことを示す
                        setPressEvent(mFocusX, mFocusY)
                    }

                    invalidate()
            }

        }

        if( event.action == ACTION_UP ) {
            isScaleBegin = false
            isViewScaling = false
            isViewScrolling = false
        }

        return true

    }

    override fun onZoomChange(zoomStep: Float, focusX: Float, focusY: Float) {
        Log.d("ZoomDebug", "Zoom Step: $zoomStep")
        zoomSize = adjustZoomSize(zoomStep)  // zoomSize を更新

        if(!isScaleBegin){ //focus変更をスケール初回だけに限定する
            mFocusX = focusX
            mFocusY = focusY
        }

        pressedInViewToModel(
            PointXY(
                focusX,
                focusY
            )
        )

    }

    fun pressedInViewToModel(pressedInView: PointXY){
        translatePoint = baseInView //why this?
        viewTranslateManager.setParameters( baseInView, zoomSize, centerInModel, pressedInModel )
        pressedInModel = viewTranslateManager.pressedInViewToModel(pressedInView)
    }

    fun setPressEvent(x: Float, y: Float){
        pressedInView.set(x, y)
        lastCPoint.set(x, y)
        pressedInViewToModel(pressedInView)

    }

    override fun onScroll(
        p0: MotionEvent?,
        p1: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if( isViewScaling == true ) return false

        pressedInView.set(p1.x, p1.y)
        moveVector = pressedInView.addminus( lastCPoint )
        baseInView = translatePoint.add( moveVector )
        moveVector.set(0f, 0f)

        return true
    }

    override fun onDown(event: MotionEvent): Boolean {

        setPressEvent(event.x, event.y)
        return true
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        (context as MainActivity).setTargetEditText(zoomSize*0.5f)
        return true

    }

    override fun onFling(
        p0: MotionEvent?,
        p1: MotionEvent,
        p2: Float,
        p3: Float
    ): Boolean {
        if( ( p3 * p3 ) > 10000000f && !(context as MainActivity).deductionMode ) (context as MainActivity).fabReplace()
        return true
    }

    override fun onShowPress(e: MotionEvent) {

    }

    override fun onLongPress(event: MotionEvent) {
        if ( event.downTime > 80000 && event.pointerCount < 2 ) {
            //setPressEvent( event ) { (context as MainActivity).fabFlag() }
            //setPressEvent( event ) { (context as MainActivity).setTargetEditText() }
        }

    }
// endregion screen onTouchEvent

// region logs
    fun logModelViewPoints(){
        Log.d("ModelView", "  touchCounter: $touchCounter" )
        Log.d("ModelView", "  isViewScroll: $isViewScrolling" )
        Log.d("ModelView", "   isViewScale: $isViewScaling" )
        Log.d("ModelView", "     movePoint:" + translatePoint.x + ", " + translatePoint.y )
        Log.d("ModelView", "    moveVector:" + moveVector.x + ", " + moveVector.y )
        Log.d("ModelView", "    baseInView:" + baseInView.x + ", " + baseInView.y )
        Log.d("ModelView", "  centerInView:" + centerInView.x + ", " + centerInView.y )
        Log.d("ModelView", " centerInModel:" + centerInModel.x + ", " + centerInModel.y )
        Log.d("ModelView", " pressedInView:" + pressedInView.x + ", " + pressedInView.y )
        Log.d("ModelView", "        mFocus:" + mFocusX + ", " + mFocusY  )
        Log.d("ModelView", "pressedInModel:" + pressedInModel.x + ", " + pressedInModel.y )
        Log.d("ModelView", "      zoomSize:" + zoomSize )


    }

//endregion logs

// region setter and getter

    fun getTriangleList() : TriangleList { return trianglelist }

    fun setDeductionList(dedlist: DeductionList, scale: Float){
        dedlist.lastTapIndex_ = myDeductionList.lastTapIndex_ //逆に状態をコピー?
        myDeductionList = dedlist.clone()
        myDeductionList.setScale( scale )

    }

    fun setTriangleList(triList: EditList, setscale: Float, moveCenter: Boolean = true){
        //if( myTriangleList.size() > 0 ) trilistStored_ = myTriangleList.clone()
        myScale = setscale    // 描画倍率は外から指定する
        trianglelist = triList.clone() as TriangleList
        trianglelist.attachToTheView(
            PointXY(
                0f,
                0f
            ), setscale, paintTexS.textSize )
        setTriListLengthStr()
        if( moveCenter ) setCenterInModelToLastTappedTriNumber() //画面を動かしてしまうので注意
        //resetView()
        //invalidate()
        watchedB1_ = ""
        watchedC1_ = ""
     }

    fun setTriListLengthStr(){

        for( i in 0 until trianglelist.size() ){
            val tri = trianglelist.get(i+1)
            tri.sla_ = tri.lengthNotSized[0].formattedString(2)
            tri.slb_ = tri.lengthNotSized[1].formattedString(2)
            tri.slc_ = tri.lengthNotSized[2].formattedString(2)

        }

    }

    /**
     * `printScale` の値に基づいて適切なテキストスペーサーの値を調整します。
     *
     * - `printScale` が 5.0 を超える場合、テキスト間のスペースは最小限になります (0.2f)。
     * - `printScale` が 3.0 を超えて 5.0 以下の場合、中間のスペースを使用します (0.5f)。
     * - それ以外の場合 (3.0 以下)、最大のスペースを使用します (2f)。
     *
     * @param printScale プリントスケールの現在値。
     * @return 調整されたテキストスペーサーの値。
     */
    fun adjustTextSpacer(printScale: Float): Float = when {
        printScale > 5.0 -> 0.2f  // スケールが大きい場合はスペーサーを小さくする
        printScale > 3.0 -> 0.5f  // 中間のスケールには中間のスペーサーを使用
        else -> 2f  // 小さいスケールには大きなスペーサーを使用
    }
// endregion setter and getter

// region resetview
    fun setCenterInModelToLastTappedTriNumber() {
        centerInModel = trianglelist.getLastTriangle().pointcenter
    }

    fun resetView( pt: PointXY ){
        centerInModel = pt.clone()
        //drawPoint = pt.clone()
        resetPointToZero()
        viewResetToCenter()
        invalidate()
    }

    fun resetViewToLastTapTriangle(){
        resetView( toLastTapTriangle() )
    }


    val INDEXCENTER=3
    fun toLastTapTriangle(sideIndex: Int=INDEXCENTER): PointXY {
        val triangle = trianglelist.getLastTriangle()
        when(sideIndex){
            0 -> return triangle.dimpoint.a
            1 -> return triangle.dimpoint.b
            2 -> return triangle.dimpoint.c
            3 -> return triangle.pointcenter
        }

       return triangle.pointcenter
    }

    fun resetViewToCurrentDeduction(){
        resetView( myDeductionList.get(myDeductionList.current).point.scale( PointXY(1f, -1f) ) )
    }


    fun resetPointToZero(){
        translatePoint.set(0f, 0f)
        moveVector.set(0f, 0f)
        pressedInView.set(0f, 0f)
        lastCPoint.set(0f, 0f)
        pressedInModel.set(0f, 0f)
    }

    fun viewResetToCenter(){
        viewSize.set( this.width.toFloat(), this.height.toFloat())
        centerInView.set( viewSize.x * 0.5f, viewSize.y * 0.25f )
        baseInView.set( centerInView.x, centerInView.y )
    }



    fun onceTransViewToLastTapTriangle(){
        if(transOnce == true) {
            //setCenterInModelToLastTappedTriNumber() // 同じこと2回やってる
            resetViewToLastTapTriangle()
            transOnce = false
        }
    }

// endregion resetview

// region onDraw
    val viewTranslateManager = ViewTranslateManager()
    val zero = 0
    override fun onDraw(canvas: Canvas) {
        onceTransViewToLastTapTriangle()
        viewTranslateManager.setParameters(baseInView,zoomSize,centerInModel,pressedInModel)
        viewTranslateManager.screenTranslate(canvas)

        // 背景の塗りつぶし
        canvas.drawColor(Color.argb(255, zero, zero, zero))

        drawEntities(canvas, paintTri, paintTexS, paintRed, darkColors_, trianglelist, myDeductionList )
        drawCrossLines(canvas, pressedInModel, paintRed )
    }
// endregion onDraw

// region drawEntities
    fun drawEntities(canvas: Canvas, paintTri: Paint, paintTex: Paint, paintRed: Paint, colors: Array<Int>, myTriangleList: TriangleList, myDeductionList: DeductionList) {

        // draw the Shadow...
        drawShadowTriangle( canvas, myTriangleList )

        // 三角形の塗りつぶしの描画
        for( i in 0 until myTriangleList.size() )  {
            paintFill.color = colors.get(myTriangleList.get(i + 1).color_)
            canvas.drawPath(makeTriangleFillPath(myTriangleList.get(i + 1)), paintFill)
        }

        // 三角形の線、寸法、番号の描画
        for( i in 0 until myTriangleList.size() )  {
            val paintLine = paintTri
            //if( i + 1 == myTriangleList.lastTapNumber_ ) paintLine = paintYellow
            drawTriangle(
                canvas,
                myTriangleList.get(i + 1),
                paintLine,
                paintTex,
                paintTex,
                paintBlue,
                myTriangleList
            )
        }

        for( i in 0 until myDeductionList.size() ) drawDeduction(
                canvas,
                myDeductionList.get(i + 1),
                paintRed
        )

        //選択三角形の辺と番号サークルの強調表示
        drawBlinkLine( canvas, myTriangleList )
    }

    fun drawTriangle(
        canvas: Canvas,
        tri: Triangle,
        paintLine: Paint,
        paintDim: Paint,
        paintSok: Paint,
        paintB: Paint,
        myTriangleList: TriangleList
    ){

        // arrange
        tri.pointCA
        tri.pointAB
        tri.pointBC
        val tPathA = tri.dimOnPath[0]
        val tPathB = tri.dimOnPath[1]
        val tPathC = tri.dimOnPath[2]
        val tPathS = tri.dimOnPathSokuten

        var la = tri.sla_ //String type
        var lb = tri.slb_
        var lc = tri.slc_

        tPathA.textSpacer = textSpacer_
        tPathB.textSpacer = textSpacer_
        tPathC.textSpacer = textSpacer_
        tPathS.textSpacer = textSpacer_

        val margin = paintDim.textSize*0.52f
        val savedDimColor = paintDim.color

        if( isPrintPDF_ == false ) {
            if (isDebug_ == true) {
                logModelViewPoints()
                //val name = tri.myName_ + " :" + sokt.pointA_.x + " :" + sokt.pointA_.y + " :" + sokt.pointB_.x + " :" + sokt.pointB_.y

                la += " :" + tri.dimVerticalA + " :" + tri.dimHorizontalA
                lb += " :" + tri.dimVerticalB + " :" + tri.dimHorizontalB
                lc += " :" + tri.dimVerticalC + " :" + tri.dimHorizontalC
            }
            else if( tri.mynumber == myTriangleList.lastTapNumber ){
                la += " A"
                lb += " B"
                lc += " C"
                paintDim.color = LightYellow_
            }
            else if( tri.mynumber < myTriangleList.size() ) paintDim.color = White_
        }

        // 線
        drawTriLines( canvas, tri, paintLine )
        if(tPathA.horizontal > 2) canvas.drawPath(makePath(tPathA), paintLine)
        if(tPathB.horizontal > 2) canvas.drawPath(makePath(tPathB), paintLine)
        if(tPathC.horizontal > 2) canvas.drawPath(makePath(tPathC), paintLine)

        // 番号
        drawTriangleNumber(canvas, tri, paintDim, paintB)

        // 寸法
        if(tri.mynumber == 1 || tri.connectionType > 2 || tri.cParam_.type != 0 )
            drawDigits( canvas, la, makePath(tPathA), tPathA.offsetH, tPathA.offsetV, paintDim, margin )
        drawDigits( canvas, lb, makePath(tPathB), tPathB.offsetH, tPathB.offsetV, paintDim, margin )
        drawDigits( canvas, lc, makePath(tPathC), tPathC.offsetH, tPathC.offsetV, paintDim, margin )
        paintDim.color = savedDimColor

        // 測点
        if(tri.myName_() != ""){
            canvas.drawTextOnPath(tri.myName_(), makePath(tPathS), 0f, -5f, paintSok)
            canvas.drawPath(makePath(tPathS), paintLine)
        }
        //Log.d( "myView", "drawTriangle: " + tri.myNumber )

    }

    fun drawShadowTriangle(canvas: Canvas, trilist: TriangleList){
        if( trilist.lastTapNumber < 1 || isPrintPDF_ == true || trilist.lastTapSide < 1 || trilist.isDoubleTap == false ) return

        val parent = trilist.get(trilist.lastTapNumber )
        val lastTapSide = trilist.lastTapSide

        //番号選択されてるときは以下略。
        if( lastTapSide != 3) {
            val shadowlength = parent.getForceLength( lastTapSide ) * 0.75f
            shadowTri_ = Triangle( parent, trilist.lastTapSide, shadowlength, shadowlength )

            shadowTri_.setScale( trilist.scale)
            canvas.drawPath( makeTriangleFillPath( shadowTri_ ), paintGray )

            // 寸法値
            val pathB = shadowTri_.dimOnPath[1]
            val pathC = shadowTri_.dimOnPath[2]
            val strB = "B ${watchedB1_}"
            val strC = "C ${watchedC1_}"
            drawDigit( canvas, strB, pathB, paintYellow )
            drawDigit( canvas, strC, pathC, paintYellow )
        }
    }

    fun drawDigit(canvas: Canvas, str: String, path: DimOnPath, paint: Paint ){
        val onecharsize = paint.textSize* 0.5f
        drawDigits( canvas, str, makePath(path), path.offsetH, path.offsetV, paint, onecharsize )
    }

    fun drawDeduction(canvas: Canvas, ded: Deduction, paint: Paint){
        var str = ded.infoStr
        val point = ded.point
        val pointFlag = ded.pointFlag

        if(isAreaOff_ == false ) str += " : -" + ded.getArea().formattedString(2)+"㎡"
        var infoStrLength: Float = str.length * paint.textSize * 0.85f

        // boxの時は短くする
        if(ded.type=="Box") infoStrLength = infoStrLength*0.85f

        if( isDebug_ == true ){
            val strD = ded.point.x.toString() + " " + ded.point.y.toString()
            val strDF = ded.pointFlag.x.toString() + " " + ded.pointFlag.y.toString()
            canvas.drawText(strD, pointFlag.x, pointFlag.y-50f, paint)
            canvas.drawText(strDF, pointFlag.x, pointFlag.y-100f, paint)
            canvas.drawCircle(point.x, point.y, ded.lengthX / 2 * ded.myscale, paintYellow)

            val strA = "angle: ${ded.shapeAngle}"
            canvas.drawText(strA, point.x, point.y-50f, paint)

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
            canvas.drawCircle(point.x, point.y, ded.lengthX / 2 * ded.myscale, paint)
            paint.style = Paint.Style.FILL
        }
        if(ded.type == "Box")    drawDedRect(canvas, ded, paint)


    }

    fun drawCrossLines(canvas: Canvas, point: PointXY, paint: Paint){
        if(point.x != 0f){
            canvas.drawLine(
                point.x - 20f, point.y,
                point.x + 20f, point.y, paint
            )
            canvas.drawLine(
                point.x, point.y - 20f,
                point.x, point.y + 20f, paint
            )

        }

    }

    fun drawLine( start:PointXY, end:PointXY, paint:Paint, canvas: Canvas ){
        canvas.drawLine( start.x, -start.y,end.x, -end.y, paint )
    }

    fun drawBlinkLine( canvas: Canvas, myTriangleList: TriangleList){
        if( myTriangleList.lastTapNumber < 1 || myTriangleList.lastTapSide < 0 || isPrintPDF_ == true ) return

        val tri = myTriangleList.get( myTriangleList.lastTapNumber )

        if(myTriangleList.lastTapSide == 0)
            drawLine( tri.pointCA,tri.pointAB,paintYellow, canvas )
        if(myTriangleList.lastTapSide == 1)
            drawLine( tri.pointAB,tri.pointBC,paintYellow, canvas )
        if(myTriangleList.lastTapSide == 2)
            drawLine( tri.pointBC,tri.pointCA,paintYellow, canvas )

        val circleSize = paintTexS.textSize *0.8f
        paintYellow.style = Paint.Style.STROKE

        if( tri.mynumber == myTriangleList.lastTapNumber && isPrintPDF_ == false ) canvas.drawCircle(
            tri.pointnumber.x,
            -tri.pointnumber.y,
            circleSize,
            paintYellow
        )
        paintYellow.style = Paint.Style.FILL

    }

    fun drawLine(canvas: Canvas, p1: PointXY, p2: PointXY, sx: Float, sy: Float, paint: Paint){
        canvas.drawLine(p1.x * sx, p1.y * sy, p2.x * sx, p2.y * sy, paint)
    }

    fun drawTriLines(canvas: Canvas, tri: Triangle, paintLine: Paint){
        // arrange
        val pca = tri.pointCA
        val pab = tri.pointAB
        val pbc = tri.pointBC

        // TriLines
        drawLine(canvas, pca, pab, 1f, -1f, paintLine)
        drawLine(canvas, pab, pbc, 1f, -1f, paintLine)
        drawLine(canvas, pbc, pca, 1f, -1f, paintLine)
    }

    fun drawDigits(canvas: Canvas, str: String, path: Path, offsetH: Float, offsetV: Float, paint: Paint, onecharsize: Float ){
        for( index in 0..<str.length){
            canvas.drawTextOnPath(str.get(index).toString(), path, offsetH+((index-2)*onecharsize), offsetV, paint )
        }
    }

    fun drawTriangleNumber(
        canvas: Canvas,
        tri: Triangle,
        paint1: Paint,
        paint2: Paint
    ){
        val pnX = tri.pointnumber.x
        val pnY = -tri.pointnumber.y
        val pnpY = getPaintCenterY(pnY, paint1)
        val areaoffsetY = paint2.textSize *1.3f
        paint2.style = Paint.Style.STROKE

        val str_number: String = tri.mynumber.toString()
        val digitCount = str_number.length // 数字の桁数

        // 基本のテキストサイズ
        val baseCircleSize = paint2.textSize * 0.6f

        // 桁数に応じてサイズを調整
        val circleSize = when (digitCount) {
            1 -> baseCircleSize
            2 -> baseCircleSize * 1.2f // 2桁の場合、基本サイズの1.2倍
            3 -> baseCircleSize * 1.4f // 3桁の場合、基本サイズの1.4倍
            else -> baseCircleSize * 1.6f // 4桁以上の場合、基本サイズの1.6倍
        }

        var area =""
        if(isAreaOff_ == false ) area = tri.getArea().formattedString(1 )+"m^2"+" ${tri.pointNumber.flag.isMovedByUser}"
        //if(isDebug_ == true) area = "cp:"+tri.cParam_.type.toString() + "-"+ tri.cParam_.lcr.toString() +" pbc:"+ tri.parentBC.toString() +" Num:"+ tri.myNumber +"-"+ tri.parentNumber +" lTS"+ tri.lastTapSide_

        canvas.drawCircle(pnX, pnY, circleSize, paint2)
        canvas.drawText(str_number, pnX, pnpY, paint1)
        paint2.style = Paint.Style.FILL
        canvas.drawText(area, pnX, pnpY - areaoffsetY, paint2)

        //引き出し矢印線の描画
        if( tri.isCollide(tri.pointnumber) == false ){
            val pc = tri.pointcenter
            val pn = tri.pointnumber
            val pcOffsetToN = pc//.offset(pn, circleSize * 1.2f )
            val pnOffsetToC = pn.offset(pc, circleSize * 1.1f )
            val arrowTail = pcOffsetToN.offset(pn, pcOffsetToN.lengthTo(pnOffsetToC) * 0.3f).rotate(pcOffsetToN, 10f)
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

    fun drawDedRect(canvas: Canvas, dd: Deduction, paint: Paint){
        dd.setBox( dd.myscale )
        drawLine(canvas, dd.plt, dd.plb, 1f, 1f, paint)
        drawLine(canvas, dd.plt, dd.prt, 1f, 1f, paint)
        drawLine(canvas, dd.plb, dd.prb, 1f, 1f, paint)
        drawLine(canvas, dd.prt, dd.prb, 1f, 1f, paint)
    }


    fun makePath(PA: DimOnPath): Path {
        val path = Path()
        path.rewind()
        path.moveTo(PA.pointA.x, -PA.pointA.y)
        path.lineTo(PA.pointB.x, -PA.pointB.y)
        return path
    }

    fun makeTriangleFillPath(tri: Triangle): Path {
        val path = Path()
        path.rewind()
        path.moveTo(tri.pointCA.x, -tri.pointCA.y)
        path.lineTo(tri.pointAB.x, -tri.pointAB.y)
        path.lineTo(tri.pointBC.x, -tri.pointBC.y)
        path.lineTo(tri.pointCA.x, -tri.pointCA.y)
        return path
    }

    fun getPaintCenterY(Y: Float, paint: Paint): Float{
        val metrics: Paint.FontMetrics = paint.fontMetrics //FontMetricsを取得
        val myy: Float = Y - ( (metrics.ascent + metrics.descent) / 2 )
        return myy
    }

    fun adjustTextSize(ts: Float): Float = when {
        ts <= 5f -> 8f
        ts >= 80f -> 80f
        else -> ts
    }

    fun setAllTextSize(ts: Float){

        textSize = adjustTextSize( ts )

        paintTexS.textSize = textSize
        paintBlue.textSize = textSize
        paintBlue.strokeWidth = textSize * 0.1f
        paintRed.textSize = textSize
        paintRed.strokeWidth = textSize * 0.1f
        paintYellow.strokeWidth = textSize * 0.2f
        paintYellow.textSize = textSize

        paintTexM.textSize = textSize
        textSpacer_ = textSize * 0.2f
        trianglelist.setDimPathTextSize( textSize )


        invalidate()
        Log.d( "CadView", "TextSize changed to:" + textSize )
    }

    //endregion


    //region drawPdf
    fun patternA(
        writer: PdfWriter,
        paintTex: Paint,
        textscale: Float,
        experience: Float
    ):TriangleList { // 追跡されたcanvasの移動ベクトルを返す
        this.paintBlue.textSize = textscale
        this.paintBlue.strokeWidth = 0.05f

        isPrintPDF_ = true

        val printScale = trianglelist.getPrintScale(myScale)

        // テキストスペーサー
        textSpacer_ = adjustTextSpacer(printScale)

        // 用紙の単位にリストの大きさを合わせる
        val scaleFactor = 1.19f * writer.kaizoudo_ * (2.0f / experience / printScale)// - (myScale/100)
        myScale *= scaleFactor

        trianglelist.attachToTheView(PointXY(0f, 0f), scaleFactor, paintTex.textSize)

        return trianglelist
    }

    fun patternB(
        textscale: Float
    ):TriangleList { // 追跡されたcanvasの移動ベクトルを返す
        this.paintBlue.textSize = textscale
        this.paintBlue.strokeWidth = 0.05f

        isPrintPDF_ = true

        val pdfTrilist = trianglelist.clone()
        val printScale = pdfTrilist.getPrintScale(myScale)
        // テキストスペーサー
        textSpacer_ = adjustTextSpacer(printScale)
        // 用紙の単位にリストの大きさを合わせる

        return pdfTrilist
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

        val pdfTrilistA = patternA( writer,paintTex,textscale, experience )
        val pdfTrilistB = patternB(textscale)
        val printScale = pdfTrilistA.getPrintScale(myScale)
        val scaleFactor = 1.19f * writer.kaizoudo_ * (2.0f / experience / printScale)// - (myScale/100)
        myScale *= scaleFactor
        pdfTrilistA.attachToTheView(PointXY(0f, 0f), scaleFactor, paintTex.textSize)
        pdfTrilistB.attachToTheView(PointXY(0f, 0f), scaleFactor, paintTex.textSize)
        pdfTrilistA.toStrings()
        pdfTrilistB.toStrings()


        myDeductionList.scale(PointXY(0f, 0f), scaleFactor)
        myDeductionList.setScale( myScale )

        // 分割描画処理（使ってない）
        // canvasの動きが複雑になるためデバッグ用に追跡する
        val printPoint = PointXY(0f, 0f)
        drawSeparatedPDF(false, printPoint, canvas, paintTex )

        // リストの中心座標にキャンバスを動かす、Xはマイナス、Yはプラス
        canvas.translate(-trianglelist.center.x, trianglelist.center.y)

        drawEntities(canvas, paintTri, paintTex, paintRed, lightColors_, pdfTrilistA, myDeductionList )

        canvas.translate(trianglelist.center.x, -trianglelist.center.y)

        teardownDrawPdf(scaleFactor)

        return printPoint
    }

    fun teardownDrawPdf(scaleFactor:Float){
        //scaleを戻す
        myScale /= scaleFactor

        // pointNumberの旗揚げがリセットされる
        trianglelist.attachToTheView(PointXY(0f, 0f), 1 / scaleFactor, paintTexS.textSize )

        myDeductionList.scale(PointXY(0f, 0f), 1 / scaleFactor)
        myDeductionList.setScale(myScale)

        isPrintPDF_ = false
        this.paintBlue.textSize = textSize
        this.paintBlue.strokeWidth = 2f
        textSpacer_ = 5f
    }

    fun drawSeparatedPDF( useit:Boolean = false, printPoint:PointXY, canvas: Canvas,paintTex: Paint ): PointXY{
        if(!useit) return PointXY(0f,0f)

        //ここから横長図形の分割処理
        val printScale = trianglelist.getPrintScale(myScale)
        val printAreaW = 40f*printScale

        //スケールされてないリストの幅を測って、分割回数を計算する
        val drawingRect = trianglelist.measureLongLineNotScaled()
        val separateCount = ( drawingRect.x / printAreaW ).roundToInt() + 1
        val numberList = trianglelist.getSokutenList( 2, 4 )
        if( separateCount > 1 && numberList.size > 1 && trianglelist.get(0).angle > 45f && trianglelist.get(0).angle < 135f ){

            val printAreaH = 29.7f*printScale
            //キャンバスをどれだけ動かすか決める。幅は固定値、縦はリストのナンバーを検索し、ナンバー上のA辺の長さによって動かす。
            var wTransLen: Float
            // 縦方向の移動量
            val hYohaku = 75f
            val hkaishi = 5f
            var zukeinohaba: Float
            val halfAreaH = printAreaH*0.5f*trianglelist.scale

            //測点を持つ三角形のリスト
            val numleftList = trianglelist.getSokutenList( 0, 4 )
            numleftList.add(0, Triangle(5f, 5f, 5f) )

            if( trianglelist.angle < 0 && numberList.size > 1 ){
                Collections.reverse( numberList )
                Collections.reverse( numleftList )
            }
            var numkyori = 0f
            var numkyoriC: Float
            var numkyoriL: Float

            for( i in 0 until numberList.size ){

                //測点の座標
                val numcenPCAy = numberList[i].pointCA.y
                val numlefPCAy = numleftList[i].pointCA.y
                if( i > 0 ){
                    //前回の測点の座標と距離
                    val numcenzenPCAy = numberList[i - 1].pointCA.y
                    val numlefzenPCAy = numleftList[i - 1].pointCA.y
                    numkyoriC = numcenPCAy - numcenzenPCAy
                    numkyoriL = numlefPCAy - numlefzenPCAy
                    val katamuki = numkyoriC - numkyoriL
                    numkyori = numkyoriC - ( katamuki * 1f )
                }

                zukeinohaba = numberList.get(i).length[0] * 2
                var hTransLen = zukeinohaba + hYohaku + hkaishi// 分割数で上下の描画位置を変えたらいけない。

                // mirror -X +Y
                //最初の一回目
                if(i == 0){
                    // 初期位置に動かす。
                    printPoint.add( -numberList[0].pointCA.x,  - halfAreaH + (hTransLen) + numcenPCAy ) //* 0.75f ) )

                    canvas.translate( printPoint.x, printPoint.y )
                }
                else{
                    // 次の中心座標へ動かす
                    val lastPointX = numberList.get( i - 1 ).pointCA.x
                    wTransLen  = numberList.get(i).pointCA.x - lastPointX

                    // 縦にのびていく理由はなんだろう？それは図形の座標差が蓄積されていくからと思われる
                    hTransLen = ( zukeinohaba + hYohaku + numkyori )

                    printPoint.add( -wTransLen, hTransLen )
                    canvas.translate( -wTransLen, hTransLen )
                }

                //描画
                drawEntities(canvas, paintTri, paintTex, paintRed, lightColors_, trianglelist, myDeductionList )
            }

        }

        return printPoint
    }

//endregiondrawPDF

}

