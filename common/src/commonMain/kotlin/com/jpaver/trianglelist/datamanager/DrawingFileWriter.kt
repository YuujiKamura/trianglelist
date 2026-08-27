package com.jpaver.trianglelist.datamanager

import com.jpaver.trianglelist.*
import com.jpaver.trianglelist.editmodel.Deduction
import com.jpaver.trianglelist.editmodel.DeductionList
import com.jpaver.trianglelist.editmodel.EditList
import com.jpaver.trianglelist.editmodel.CycleShape
import com.jpaver.trianglelist.editmodel.Rectangle
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import com.jpaver.trianglelist.editmodel.ZumenInfo
import com.jpaver.trianglelist.label.DimensionLayout
import com.jpaver.trianglelist.label.DimensionPlacement
import com.jpaver.trianglelist.viewmodel.TitleParamStr
import com.jpaver.trianglelist.viewmodel.formattedString
import com.jpaver.trianglelist.editmodel.isCollide

open class DrawingFileWriter {
    //region parameter
    open lateinit var trilist_: TriangleList
    open lateinit var dedlist_: DeductionList
    open lateinit var zumeninfo : ZumenInfo
    open lateinit var titleTri_ : TitleParamStr
    open lateinit var titleDed_ : TitleParamStr
    data class AreaSegment(val text: String, val color: Int)
    val zumenAreaSegments = mutableListOf<AreaSegment>()

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

    // 用紙サイズの単一の出所 = paper フィールド (mm)。枠・タイトル欄・図形センタリング・
    // ビューポートは全てここから導出する。paperWcm/Hcm/Name は派生 (cm / 名前)。
    // 世界標準: モデルは実寸 mm、用紙寸法は 1 箇所で持つ (SXF / AutoCAD の作法、ADR 0009)。
    // 既定は A3 横。用紙を変えたい呼び出し側は paper だけ差し替える。
    // 注: 枠座標は「用紙端からのアンカー」(右端=paperWcm-1 等) で書くので、A3 では従来と同値。
    open var paper: Paper = Paper.A3_LAND
    open val paperWcm get() = paper.width / 10f   // mm → cm
    open val paperHcm get() = paper.height / 10f
    open val paperName get() = paper.name

    // sizeX_ は cm→出力単位への変換。基底/PDF/SFC は ×10 (mm)、DXF は ×1000 (DXF mm)。
    open val sizeX_ get() = paperWcm * 10f * printscale_ // カスタムゲッター
    open val sizeY_ get() = paperHcm * 10f * printscale_ // カスタムゲッター
    open val centerX_ get() = sizeX_ * 0.5f   // カスタムゲッター
    open val centerY_ get() = sizeY_ * 0.5f   // カスタムゲッター

    open var WHITE = 8
    open var BLUE  = 4
    open var RED   = 2
    open var COLOR_PINK   = 6
    open var COLOR_ORANGE = 30
    open var COLOR_YELLOW = 2
    open var COLOR_GREEN  = 3
    open var COLOR_SKY    = 4

//endregion parameter
    fun stringTriple(tri: Triangle): Triple<String, String, String> {
        tri.setLengthStr()
        val nagasaA = tri.strLengthA
        val nagasaB = tri.strLengthB
        val nagasaC = tri.strLengthC
        return Triple(nagasaA, nagasaB, nagasaC)
    }

    fun xyPointXYTriple(tri: Triangle): Triple<com.example.trilib.PointXY, com.example.trilib.PointXY, com.example.trilib.PointXY> {
        val pca = tri.pointCA
        val pab = tri.pointAB
        val pbc = tri.pointBC
        return Triple(pca, pab, pbc)
    }

    fun writeTextSwitch(str: String, point: com.example.trilib.PointXY, ts:Float, color:Int, align1: Int, align2:Int, angle: Double ){
        //引数の数でテキスト描画関数を変える
        when(align2){
            -1   -> writeTextA9( str, point, color, ts, align1, angle, 1f)
            else -> writeTextHV( str, point, color, ts, align1, align2, angle, 1f)
        }
    }

    fun writeSokuten(tri: Triangle, normalizedvector:Int, ts:Float, color:Int, align1:Int, align2:Int ){
        tri.setDimPath(ts)
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

    open fun save(){}

    // 寸法ラベルにアライメントコードを焼き込むデバッグ表示 (DXF で使用、既定 off)
    open var isDebug = false

    /**
     * 三角形 1 つの描画 (DXF/SFC/Web 共通)。座標の単位は subclass のプリミティブが
     * 吸収するので、ここはモデル座標をそのまま writeLine/writeTextHV へ渡すだけ
     * (DXF=cm を entity で ×1000、SFC=mm をそのまま、と各 writer が自分の流儀で処理)。
     * 寸法/測点の文字揃えは DimensionPlacement.verticalDxf + writeTextHV に統一
     * (SFC の writeTextHV override がテンキー式へ翻訳する)。色は WHITE/BLUE の override 任せ。
     */
    open fun writeTriangle(tri: Triangle){
        drawScene(buildTrianglePrims(tri))
    }

    // Rectangle (台形) のリスト (web 出力経路が CsvCodec.buildMixed で組んで渡す)。
    // app 経路は三角形のみで既定 empty = DXF/SFC golden 不変。
    open var traps_: List<Rectangle> = emptyList()

    // Rectangle を親に持つ Triangle のリスト (web 出力経路が CsvCodec.buildMixed で組んで渡す)。
    // 空なら DXF/SFC golden 不変。
    open var trapTris_: List<Triangle> = emptyList()

    open fun writeRectangle(rect: Rectangle, number: Int) {
        drawScene(buildRectanglePrims(rect, number))
    }

    /**
     * 台形 1 つを DrawPrim 列に組む (buildTrianglePrims の台形版、ADR 0010 段A コメントが予定した分岐)。
     * web の WebPrimitiveRenderer.renderRectangle と同じ幾何: 4 辺 + 底辺A/上辺C/延長B の寸法 +
     * 番号サークル。延長 B は「底辺からの垂線 (rect.height)」で、左脚の斜辺長ではない。中央/右寄せ
     * (alignment≠0) は左脚が斜辺になるので垂線 bl→perpFoot を補助線で別に引く。
     * 座標は実寸モデル (DrawPrim の約束) なので寸法は実長そのまま。これで backend (各 writer) を
     * 触らず台形が DXF/SFC/PDF に出る。
     */
    protected open fun buildRectanglePrims(rect: Rectangle, number: Int): List<DrawPrim> {
        val lp = rect.calcPoint()
        val bl = lp.a.left;  val br = lp.a.right
        val tl = lp.b.left;  val tr = lp.b.right
        val ts = textscale_
        val ds = rect.dimScale.toDouble()
        val dh = rect.dimHeight.toDouble()
        val prims = ArrayList<DrawPrim>()
        // 4 辺 (底辺A bl→br / 右脚D br→tr / 上辺C tr→tl / 左脚B tl→bl)
        prims += DrawPrim.Line(bl, br, WHITE)
        prims += DrawPrim.Line(br, tr, WHITE)
        prims += DrawPrim.Line(tr, tl, WHITE)
        prims += DrawPrim.Line(tl, bl, WHITE)
        // 寸法 (DimensionLayout = 三角形と同じ式層)。底辺A・上辺C は実辺長、延長B は垂線 rect.height
        fun dim(start: com.example.trilib.PointXY, end: com.example.trilib.PointXY, v: Int, h: Int, len: Float) {
            val place = DimensionLayout.layout(end, start, v, h, ds, dh, 0.0)
            prims += DrawPrim.Text(len.formattedString(2), place.dimpoint, WHITE, ts, 1, place.verticalDxf, start.calcDimAngle(end), 1f)
            if (h > 2) prims += DrawPrim.Line(place.pointA, place.pointB, WHITE)
        }
        dim(bl, br, rect.dimVertical.a, rect.dimHorizontal.a, bl.lengthTo(br).toFloat())
        dim(tr, tl, rect.dimVertical.c, rect.dimHorizontal.c, tr.lengthTo(tl).toFloat())
        val perpFoot = bl.crossOffset(br, rect.height)
        dim(bl, perpFoot, rect.dimVertical.b, rect.dimHorizontal.b, rect.height.toFloat())
        if (rect.alignment != 0) prims += DrawPrim.Line(bl, perpFoot, WHITE)
        // 直角マーカー (web の getRightAngleMark と同じ 2 本、yuuji 2026-06-18「邪魔かは見ないと分からない」)。
        val ram = rect.getRightAngleMark()
        prims += DrawPrim.Line(ram.a.left, ram.a.right, WHITE)
        prims += DrawPrim.Line(ram.b.left, ram.b.right, WHITE)
        // 番号サークル + 番号 (重心に中央寄せ)
        val center = com.example.trilib.PointXY((bl.x + br.x + tr.x + tl.x) / 4f, (bl.y + br.y + tr.y + tl.y) / 4f)
        prims += DrawPrim.Circle(center, ts * 0.85f, BLUE, 1f)
        prims += DrawPrim.Text(number.toString(), center, BLUE, ts, 1, 2, 0.0, 1f)
        return prims
    }

    /**
     * 三角形 1 つを DrawPrim 列に組む (frontend、ADR 0010 段A の三角形版)。
     * 旧 writeTriangle のインライン描画呼び出しと同じプリミティブを同じ順序で並べるだけなので、
     * drawScene 経由でも出力はバイト不変 (DXF/SFC golden で担保)。混在リスト化の際は、ここを
     * 図形種別ごとの buildPrims に分岐させれば backend (各 writer) は触らずに済む。
     */
    protected open fun buildTrianglePrims(tri: Triangle): List<DrawPrim> {
        val (pca, pab, pbc) = xyPointXYTriple(tri)
        val (placeA, placeB, placeC) = layoutTriple(tri)
        var (la, lb, lc) = stringTriple(tri)

        val textSize: Float = textscale_
        val prims = ArrayList<DrawPrim>()

        // 三角形の 3 辺
        prims += triangleLinePrims(tri, WHITE)

        if (isDebug) {
            la += "A${placeA.verticalDxf}"
            lb += "B${placeB.verticalDxf}"
            lc += "C${placeC.verticalDxf}"
        }

        // 寸法値
        if (tri.mynumber == 1 || tri.connectionSide > 2)
            prims += dimTextPrim(placeA.verticalDxf, la, placeA.dimpoint, pab.calcDimAngle(pca))
        prims += dimTextPrim(placeB.verticalDxf, lb, placeB.dimpoint, pbc.calcDimAngle(pab))
        prims += dimTextPrim(placeC.verticalDxf, lc, placeC.dimpoint, pca.calcDimAngle(pbc))

        // 旗揚げ線
        prims += dimFlagPrims(tri, placeA, placeB, placeC, WHITE)

        // 番号
        prims += pointNumberPrims(tri, textSize, BLUE, 1, 2, textSize * 0.85f)

        // 測点
        if (tri.name != "") {
            prims += sokutenPrims(tri, trilist_.sokutenListVector, textSize, BLUE, 1, 1)
        }
        return prims
    }

    /** 三角形の 3 辺 (旧 writeTriangleLines)。順序 = A辺(point0→AB)・B辺(AB→BC)・C辺(BC→point0) */
    private fun triangleLinePrims(tri: Triangle, color: Int): List<DrawPrim> = listOf(
        DrawPrim.Line(tri.point[0], tri.pointAB, color),
        DrawPrim.Line(tri.pointAB, tri.pointBC, color),
        DrawPrim.Line(tri.pointBC, tri.point[0], color),
    )

    /** 寸法値テキスト 1 つ (旧 writeTextDimension)。縦揃え=verticalDxf、横=中央(1)、色=WHITE */
    private fun dimTextPrim(verticalAlign: Int, len: String, p1: com.example.trilib.PointXY, angle: Double): DrawPrim =
        DrawPrim.Text(len, p1, WHITE, textscale_, 1, verticalAlign, angle, 1f)

    /** 旗揚げ線 (旧 writeDimFlagsFromLayout)。horizontal>2 の辺だけ線を引く */
    private fun dimFlagPrims(
        tri: Triangle,
        placeA: DimensionPlacement,
        placeB: DimensionPlacement,
        placeC: DimensionPlacement,
        color: Int
    ): List<DrawPrim> = buildList {
        if (tri.dim.horizontal.a > 2) add(DrawPrim.Line(placeA.pointA, placeA.pointB, color))
        if (tri.dim.horizontal.b > 2) add(DrawPrim.Line(placeB.pointA, placeB.pointB, color))
        if (tri.dim.horizontal.c > 2) add(DrawPrim.Line(placeC.pointA, placeC.pointB, color))
    }

    /** 番号サークル + 番号 + (重なり時のみ) 引き出し矢印線 (旧 writePointNumber) */
    private fun pointNumberPrims(
        tri: Triangle,
        ts: Float,
        color: Int,
        align1: Int,
        align2: Int,
        circleSize: Float
    ): List<DrawPrim> = buildList {
        val pn = tri.pointnumber
        val pc = tri.pointcenter
        add(DrawPrim.Circle(pn, circleSize, color, 1f))
        add(DrawPrim.Text(tri.mynumber.toString(), tri.pointnumber, color, ts, align1, align2, 0.0, 1f))
        if (tri.isCollide(tri.pointnumber) == false) {
            val pcOffsetToN = pc.offset(pn, circleSize.toDouble())
            val pnOffsetToC = pn.offset(pc, circleSize.toDouble())
            val arrowTail = pcOffsetToN.offset(pn, pcOffsetToN.lengthTo(pnOffsetToC) * 0.5).rotate(pcOffsetToN, 5.0)
            add(DrawPrim.Line(pcOffsetToN, pnOffsetToC, color))
            add(DrawPrim.Line(pcOffsetToN, arrowTail, color))
        }
    }

    /** 測点名テキスト + 測点線 (旧 writeSokutenFromLayout)。位置は DimensionLayout(SIDE_SOKUTEN) */
    private fun sokutenPrims(
        tri: Triangle,
        normalizedvector: Int,
        ts: Float,
        color: Int,
        align1: Int,
        align2: Int
    ): List<DrawPrim> {
        // 高さは「実際に文字を描く ts」を渡す。tri.dimHeight は画面描画が setDimPathTextSize で
        // 埋めるキャッシュで、書き出し経路では 0f のまま (CycleShape.kt:30) — それを渡すと
        // 旗線長 = textWidth + dimheight*0.4 が 0 になり dimpoint が NaN 化する。
        // 直下の DrawPrim.Text も ts で描くので、レイアウトと実寸がこれで一致する。
        val place = DimensionLayout.layout(
            tri.pointAB, tri.point[0],
            DimensionLayout.SIDE_SOKUTEN, tri.dim.horizontal.s,
            tri.scaleFactor.toDouble(), ts.toDouble(), 0.0, tri.name
        )
        val pa = place.pointA
        val pb = place.pointB
        return listOf(
            DrawPrim.Text(tri.name, place.dimpoint, color, ts, align1, align2, pb.calcSokAngle(pa, normalizedvector), 1f),
            DrawPrim.Line(pa, pb, color),
        )
    }

    /**
     * 3 辺の寸法配置を DimensionLayout で計算する。入力は Triangle のキャッシュ (setDimPath) と同じ:
     * A=(pointAB,point[0]), B=(pointBC,pointAB), C=(point[0],pointBC) + dim の縦横コード。
     * gapPaperMm=0 なのでキャッシュ由来と同値 (ADR 0003 Phase 2)。
     */
    private fun layoutTriple(tri: Triangle): Triple<DimensionPlacement, DimensionPlacement, DimensionPlacement> {
        val scale = tri.scaleFactor.toDouble()
        val dimheight = tri.dimHeight.toDouble()
        return Triple(
            DimensionLayout.layout(tri.pointAB, tri.point[0], tri.dim.vertical.a, tri.dim.horizontal.a, scale, dimheight, 0.0),
            DimensionLayout.layout(tri.pointBC, tri.pointAB, tri.dim.vertical.b, tri.dim.horizontal.b, scale, dimheight, 0.0),
            DimensionLayout.layout(tri.point[0], tri.pointBC, tri.dim.vertical.c, tri.dim.horizontal.c, scale, dimheight, 0.0)
        )
    }

    open fun writeLine(p1: com.example.trilib.PointXY, p2: com.example.trilib.PointXY, color: Int, scale: Float = 1f ){
    }

    open fun writeRect(point: com.example.trilib.PointXY, sizeX: Float, sizeY: Float, color: Int, scale: Float = 1f ){
        val sizex: Double = sizeX/2.0
        val sizey: Double = sizeY/2.0
        writeLine( point.plus(-sizex, -sizey), point.plus(sizex, -sizey), color)
        writeLine( point.plus(-sizex, sizey), point.plus(sizex, sizey), color)
        writeLine( point.plus(-sizex, -sizey), point.plus(-sizex, sizey), color)
        writeLine( point.plus(sizex, -sizey), point.plus(sizex, sizey), color)
    }

    open fun writeCircle(point: com.example.trilib.PointXY, size: Float, color: Int, scale: Float = 1f ){

    }

    open fun writeText(str: String, point: com.example.trilib.PointXY, scale: Float, color: Int, size: Float, align: Int){

    }

    // align tenkey ( ex 8 is top and center ) in sfc
    open fun writeTextA9(
        text: String,
        point: com.example.trilib.PointXY,
        color: Int = 8,
        tsy: Float,
        align: Int = 2,
        angle: Double = 0.0,
        scale: Float
    ){
    }

    // Align H and V ( 0 is center, 1 is left/top, 3 is right/bottom ) in dxf
    open fun writeTextHV(
        text: String,
        point: com.example.trilib.PointXY,
        color: Int,
        textsize: Float,
        alignH: Int,
        alignV: Int = 0,
        angle: Double = 0.0,
        scale: Float
    ){

    }

    open fun writeEntities(){

    }

    /**
     * 控除 (欠損) 1 つの描画 (DXF/SFC 共通、DXF を正として集約)。
     * 単位は subclass のプリミティブが吸収するのでモデル座標 (実寸) のまま扱う。
     * 旗線・情報テキスト・円 (Circle) / 矩形 (Box) を RED で描く。
     */
    open fun writeDeduction( ded: Deduction){
        val textSize = textscale_
        val infoStrLength = ded.infoStr.length * textSize + 0.3f
        val point = ded.point
        val pointFlag = ded.pointFlag
        var textOffsetX = 0f
        if (ded.type == "Box") textOffsetX = -0.5f

        if (point.x <= pointFlag.x) {  // pointFlag が pt より右
            writeLine(point, pointFlag, RED)
            writeTextAndLine(
                ded.infoStr,
                pointFlag,
                pointFlag.plus((infoStrLength + textOffsetX).toDouble(), 0.0),
                textSize,
                1f
            )
        } else {                       // pointFlag が pt より左
            writeLine(point, pointFlag, RED)
            writeTextAndLine(
                ded.infoStr,
                pointFlag.plus((-ded.getInfo().length * textSize - textOffsetX).toDouble(), 0.0),
                pointFlag,
                textSize,
                1f
            )
        }

        if (ded.type == "Circle") writeCircle(point, ded.lengthX / 2, RED, 1f)
        if (ded.type == "Box") writeDedRect(ded)
    }

    /** Box 控除の矩形 4 辺。setBox はモデル単位 1.0 で箱を作る (DXF を正) */
    private fun writeDedRect(ded: Deduction){
        val color = RED
        ded.shapeAngle = -ded.shapeAngle // 逆回転
        ded.setBox(1.0)
        writeLine(ded.pLTop, ded.pLBtm, color)
        writeLine(ded.pLTop, ded.pRTop, color)
        writeLine(ded.pRTop, ded.pRBtm, color)
        writeLine(ded.pLBtm, ded.pRBtm, color)
    }

    open fun writeTextAndLine(st: String, p1: com.example.trilib.PointXY, p2: com.example.trilib.PointXY, textsize: Float, scale: Float) {}

    open fun writeHatch(points: List<com.example.trilib.PointXY>, color: Int, colorIdx: Int, scale: Float = 1f) {}

    /**
     * DrawPrim のリストを各 backend のプリミティブ実装へ 1:1 でディスパッチ (ADR 0010 段A)。
     * sealed なので when は全 prim を網羅し、新 prim 追加時の漏れはコンパイルエラーで止まる。
     * インライン呼び出しと同じプリミティブを同じ順序で呼ぶため、出力はバイト単位で不変。
     */
    protected fun drawScene(prims: List<DrawPrim>) {
        for (p in prims) when (p) {
            is DrawPrim.Line   -> writeLine(p.p1, p.p2, p.color, p.scale)
            is DrawPrim.Rect   -> writeRect(p.center, p.sizeX, p.sizeY, p.color, p.scale)
            is DrawPrim.Circle -> writeCircle(p.center, p.size, p.color, p.scale)
            is DrawPrim.Text   -> writeTextHV(p.text, p.pos, p.color, p.size, p.alignH, p.alignV, p.angle, p.scale)
            is DrawPrim.Hatch  -> writeHatch(p.points, p.color, p.colorIdx, p.scale)
        }
    }

    /** 三角形 1 つのハッチングプリミティブをビルド */
    protected open fun buildTriangleHatchPrims(tri: Triangle): List<DrawPrim> {
        val cIdx = tri.mycolor.coerceIn(0, 4)
        val color = getHatchColor(cIdx)
        val (pca, pab, pbc) = xyPointXYTriple(tri)
        val points = listOf(pca, pab, pbc, pca)
        return listOf(DrawPrim.Hatch(points, color, cIdx))
    }

    /** 台形 1 つのハッチングプリミティブをビルド */
    protected open fun buildRectangleHatchPrims(rect: Rectangle): List<DrawPrim> {
        val cIdx = rect.mycolor.coerceIn(0, 4)
        val color = getHatchColor(cIdx)
        val lp = rect.calcPoint()
        val bl = lp.a.left;  val br = lp.a.right
        val tl = lp.b.left;  val tr = lp.b.right
        val points = listOf(bl, br, tr, tl, bl)
        return listOf(DrawPrim.Hatch(points, color, cIdx))
    }

    /** ハッチング用のRGB色値を取得 (各ファイルライターでオーバーライド) */
    open fun getHatchColor(colorIdx: Int): Int {
        return WHITE
    }

    /**
     * ハッチング（背景の塗りつぶし）だけをまとめて描画する共通処理。
     * Z-order（重ね順）を保ち、線や文字の下に隠すために、本体描画の前に呼び出す。
     */
    fun drawAllHatches(
        trilistNumbered: TriangleList,
        traps: List<Rectangle>,
        trapTris: List<Triangle>
    ) {
        val prims = ArrayList<DrawPrim>()
        for (index in 1..trilistNumbered.size()) {
            val tri = trilistNumbered.get(index)
            prims.addAll(buildTriangleHatchPrims(tri))
        }
        for (rect in traps) {
            prims.addAll(buildRectangleHatchPrims(rect))
        }
        for (t in trapTris) {
            prims.addAll(buildTriangleHatchPrims(t))
        }
        drawScene(prims)
    }

    /**
     * 本体（線・文字・寸法・控除）を描画する共通処理。
     */
    fun drawAllMainEntities(
        trilistNumbered: TriangleList,
        traps: List<Rectangle>,
        trapTris: List<Triangle>,
        dedlist: DeductionList
    ) {
        for (index in 1..trilistNumbered.size()) {
            writeTriangle(trilistNumbered.get(index))
        }
        for (number in 1..dedlist.size()) {
            writeDeduction(dedlist.get(number))
        }
        for ((i, rect) in traps.withIndex()) {
            writeRectangle(rect, trilistNumbered.size() + i + 1)
        }
        for (t in trapTris) {
            writeTriangle(t)
        }
    }

    /** 改行付きテキスト (題字の工事名) を DrawPrim.Text 群に展開 (旧 writeTextWithKaigyou/splitAndWriteText の純粋版)
     *  alignV=2 (中央) で他の表題欄 cell (図面名/路線名/作成日/縮尺/施工者、writeDrawingFrame 参照) と揃える
     *  ── 旧 alignV=0 (ベースライン) は同じ cell 内で他行と縦位置がずれる原因だった (2026-08-12)。 */
    private fun kaigyouPrims(str: String, iKaigyou: Int, xr: Float, yb: Float, yo: Float, color: Int, textsize: Float): List<DrawPrim.Text> {
        if (str.length <= iKaigyou) {
            return listOf(DrawPrim.Text(str, com.example.trilib.PointXY(xr, yb), color, textsize, 0, 2, 0.0, 1f))
        }
        val parts = if (str.contains(" ")) str.split(' ', limit = 2)
                    else listOf(str.substring(0, iKaigyou), str.substring(iKaigyou))
        val out = mutableListOf(DrawPrim.Text(parts[0], com.example.trilib.PointXY(xr, yb + yo), color, textsize, 0, 2, 0.0, 1f))
        if (parts.size > 1) out.add(DrawPrim.Text(parts[1], com.example.trilib.PointXY(xr, yb - yo), color, textsize, 0, 2, 0.0, 1f))
        return out
    }

    fun writeOuterFrame(scale: Float = 1f){
        // 外枠描画 (用紙中央・用紙より一回り内側)。
        // 2026-06-18 user 確認 + 国交省「CAD製図基準」 + 東京都建設局「CAD製図基準・同解説 (令和 6 年 4 月)」:
        // A3 (= 297×420mm) は用紙端から 7.5mm 余白原則 (= outerMarginCm=0.75)。 A0/A1 は 20mm、
        // A2/A3/A4 は 10mm 以上 〜 7.5mm の派生規定、 ここでは A3 専用の 7.5mm を採用 (= 旧 40×27cm は
        // 上下 1.35cm + 左右 1cm の独自値で電子納品基準と乖離)。
        // 外枠寸法 = (paperWcm - 2×MARGIN) × (paperHcm - 2×MARGIN) = A3 で 40.5×28.2cm。
        val cx = paperWcm / 2f; val cy = paperHcm / 2f
        val w = (paperWcm - outerMarginCm * 2f) * scale
        val h = (paperHcm - outerMarginCm * 2f) * scale
        drawScene(listOf(
            DrawPrim.Rect(com.example.trilib.PointXY(cx, cy, scale), w, h, WHITE, scale)
        ))
    }

    fun writeTopTitle(scale: Float = 1f, textsize: Float ){
        // 上のタイトル (用紙上中央アンカー)。 cx = 用紙横中心、 y は外枠上辺基準で配置。
        // 2026-06-18 user 方針「画面と図面で同じレイアウト (冪等)」 + 「すべての部品が外枠基準」 ──
        // outerMarginCm を変えれば title 群も外枠上辺に追従する。 旧コードは y=paperHcm-2.6 直値
        // で旧外枠上辺 (= paperHcm-1.35) 基準の暗黙 offset、 = 「なぜ -2.6 か」 が式に出てなかった。
        // ty = title 上端 (= 外枠上辺の 1.5cm 下、 ×3 倍 title の頭に外枠との余白を確保)。
        val cx = paperWcm / 2f
        val ty = paperHcm - outerMarginCm - 1.5f
        // textsize (引数) はもう使わない ── writeTopTitle/writeDrawingFrame は drawingScale を
        // 打ち消した paper 固定 cm 空間で動くため、サイズは role だけで決まる (TextSizePolicy 参照)。
        // 2026-08-13 発見のバグ修正: ここでは常に scale=1 (default) で resolve する。DXF は常に
        // scale=1 で呼ぶので影響なしだが、SFC は scale=printscale_ を直渡しする経路があり、
        // ここで scale 依存の値を作ってしまうと後段で ty 等の scale 非依存アンカーと算術演算
        // (subtitleLineGap 等) した上でさらに PointXY(...,scale) で scale を掛けるため、scale が
        // 実質 2 重に掛かってしまう (DrawingFileWriterScaleConsistencyTest で再現)。scale は
        // 最終的な DrawPrim 生成時 (位置は PointXY 経由、サイズは *scale) に一度だけ適用する。
        var titleTextSize = TextSizePolicy.resolve(TextRole.TopTitle)
        // サブタイトル行 (路線名+面積合計) も TopTitle と同じ role/サイズ (2026-08-12 user 指示
        // 「路線＋面積のサイズも二倍にしていい」= 表題欄 (BottomTitleFrame) の 2 倍、TITLE_PAPER_MM が
        // 元々 FRAME_LABEL_PAPER_MM*2 で定義されているのでその値をそのまま使う)。shrink 管理は
        // titleTextSize とは独立した変数 (areaFs) のまま維持し、後続の shrink 判定を分離できるようにする。
        var areaFs = TextSizePolicy.resolve(TextRole.TopTitle)
        val title = zumeninfo.zumentitle

        // タイトルブロック (タイトル文字 / サブタイトル行) が外枠の外にはみ出ないよう、外枠幅を
        // 上限とした「箱」を先に決め、それに収まるサイズへ逆算する (2026-08-12 user 指示「枠のサイズを
        // 記憶しておいて、収まるように上手くコントロールする」)。実測 (dumptexts) で下線幅の計算ミスが
        // 2 回見つかった経緯があるので、role 由来の base サイズを無条件に使わず必ずここで検算する。
        //
        // shrink 係数は「タイトル」と「サブタイトル行 (路線名+面積合計)」で別々に算出する
        // (2026-08-12 user 指示「上部タイトルはもっとでかくしたい。管理を分けたほうが良い」)。
        // 旧実装は 3 者の最大幅から単一の shrink を出して全部に掛けていたため、色分け内訳が
        // 増えて長くなりがちな面積合計行にタイトル自体が引きずられて縮んでいた。
        //
        // サブタイトル行は 路線名 + 面積合計 を同一行にまとめる (2026-08-12 user 指示「路線名と面積は
        // 同一行で表現していいぞ。路線名　面積　の形で」、旧実装は縦に 2 行だった)。各要素を個別 prim
        // で左詰めに並べる (= 手動 X 座標計算) と位置がずれるバグを踏んだため、1 本の文字列に連結して
        // 1 個の text prim として中央寄せで描画する (2026-08-12 user 指示「ストリングは連結して一個の
        // 文字列にして表現しろ」)。色分け内訳の色は失うが、位置計算のバグ源を断つ方を優先する。
        val maxWidth = paperWcm - outerMarginCm * 2f
        val titleWidthAtBase = TextFit.estimateWidth(title, titleTextSize)
        if (titleWidthAtBase > maxWidth && titleWidthAtBase > 0f) {
            titleTextSize *= maxWidth / titleWidthAtBase
        }

        val subtitleText = buildString {
            append(rosenname_)
            if (rosenname_.isNotEmpty() && zumenAreaSegments.isNotEmpty()) append("　")
            zumenAreaSegments.forEach { append(it.text) }
        }
        val subtitleWidthAtBase = TextFit.estimateWidth(subtitleText, areaFs)
        if (subtitleWidthAtBase > maxWidth && subtitleWidthAtBase > 0f) {
            areaFs *= maxWidth / subtitleWidthAtBase
        }

        val titleWidth = TextFit.estimateWidth(title, titleTextSize)
        val subtitleWidth = TextFit.estimateWidth(subtitleText, areaFs)
        // 下線幅は title 文字列長 (paper-cm 単位) とサブタイトル行の実幅、大きい方に合わせる ──
        // サブタイトル行は路線名や面積の桁数次第でタイトルより長くなることがあり、title.length だけで
        // 下線を決めるとサブタイトルテキストが下線からはみ出る (2026-08-12、dumptexts で実座標を
        // 数値検証して確認)。
        val halfW = maxOf(titleWidth, subtitleWidth) / 2f

        // 下線の間隔は titleTextSize に比例させる (2026-08-12、タイトルを 7→10mm に拡大した際に
        // 固定 cm offset のままだとタイトル本体・下線が衝突することを実描画の目視で発見)。
        val lineUnit = titleTextSize.value / 7f
        val underlineGap1 = lineUnit * 1f  // 旧 0.1cm
        val underlineGap2 = lineUnit * 2f  // 旧 0.2cm
        // サブタイトル行の縦オフセットは「下線の位置 + サブタイトル自身の実サイズ」から逆算する
        // (2026-08-12 user 指示「位置がおかしいぞ、テキストサイズを基にしてオフセット計算しろ」)。
        // サブタイトル行を表題欄の 2 倍 (= titleTextSize と同サイズ) に拡大した結果、旧 lineUnit*9
        // 固定比率 (小さいサブタイトル前提) では下線と衝突した。alignV=1 (Bottom) 描画は指定 y から
        // 上方向にフォントサイズぶん伸びるため、下線の下に「サブタイトル自身の高さ + 余白」を確保する。
        // 実描画で目視して「もうすこし下」と判定、user 指示「テキスト一個分オフセット追加」で
        // areaFs をもう 1 つぶん (計 2.2 倍) 加算する。
        val subtitleLineGap = underlineGap2 + areaFs.value * 2.2f

        val prims = mutableListOf<DrawPrim>(
            DrawPrim.Text(title, com.example.trilib.PointXY(cx, ty, scale), WHITE, titleTextSize.value * scale, 1, 1, 0.0, scale),
            DrawPrim.Line(com.example.trilib.PointXY(cx - halfW, ty - underlineGap1, scale), com.example.trilib.PointXY(cx + halfW, ty - underlineGap1, scale), WHITE, scale),
            DrawPrim.Line(com.example.trilib.PointXY(cx - halfW, ty - underlineGap2, scale), com.example.trilib.PointXY(cx + halfW, ty - underlineGap2, scale), WHITE, scale)
        )

        if (subtitleText.isNotEmpty()) {
            prims.add(DrawPrim.Text(subtitleText, com.example.trilib.PointXY(cx, ty - subtitleLineGap, scale), WHITE, areaFs.value * scale, 1, 1, 0.0, scale))
        }

        drawScene(prims)
    }

    fun calculateAndSetZumenAreaText(shapes: List<CycleShape>, deductions: List<Deduction>) {
        val shapeMap = shapes.associateBy { it.mynumber }
        val colorNetAreas = FloatArray(5) { 0f }
        for (shape in shapes) {
            val color = shape.mycolor.coerceIn(0, 4)
            colorNetAreas[color] += shape.getArea()
        }
        for (ded in deductions) {
            if (ded.overlap_to != 0) {
                val parentShape = shapeMap[ded.overlap_to]
                val color = parentShape?.mycolor?.coerceIn(0, 4) ?: 4
                colorNetAreas[color] -= ded.getArea()
            }
        }
        
        val totalArea = shapes.sumOf { it.getArea().toDouble() } - deductions.filter { it.overlap_to != 0 }.sumOf { it.getArea().toDouble() }
        val totalAreaFloat = maxOf(0f, totalArea.toFloat())
        
        val colorOrder = listOf(0, 3, 2, 4, 1)
        val colorAbstractCodes = mapOf(
            0 to 100, // Web COLORS.pink (webColorMap 準拠)
            3 to 103, // Web COLORS.green
            2 to 102, // Web COLORS.yellow
            4 to 104, // Web COLORS.sky
            1 to 101  // Web COLORS.orange
        )
        
        fun formatArea(value: Float): String {
            val d = value.toDouble()
            val negative = d < 0.0
            val digits = 2
            var factor = 1.0
            repeat(digits) { factor *= 10.0 }
            val scaled = (if (negative) -d else d) * factor
            var units = kotlin.math.floor(scaled).toLong()
            if (scaled - units >= 0.5) units += 1
            val s = units.toString().padStart(digits + 1, '0')
            val intPart = s.dropLast(digits)
            val body = if (digits == 0) intPart else intPart + "." + s.takeLast(digits)
            return if (negative) "-$body" else body
        }
        
        zumenAreaSegments.clear()
        
        val activeColors = colorOrder.filter { colorNetAreas[it] > 0f }
        if (activeColors.size <= 1) {
            zumenAreaSegments.add(AreaSegment("面積: A=${formatArea(totalAreaFloat)}㎡", WHITE))
        } else {
            zumenAreaSegments.add(AreaSegment("面積: A=${formatArea(totalAreaFloat)}㎡ (", WHITE))
            for ((idx, c) in activeColors.withIndex()) {
                val area = maxOf(0f, colorNetAreas[c])
                val colorCode = colorAbstractCodes[c] ?: WHITE
                zumenAreaSegments.add(AreaSegment("■:", colorCode))
                val suffix = if (idx == activeColors.lastIndex) "${formatArea(area)}㎡)" else "${formatArea(area)}㎡, "
                zumenAreaSegments.add(AreaSegment(suffix, WHITE))
            }
        }
    }
    companion object {
        // 枠内テキスト 3 region: TopTitle (上部) / BottomTitleFrame (右下表題欄) / BottomCredit (左下 url)。
        // size は TextSizePolicy.resolve(TextRole, scale) に一本化済み (2026-08-12)。旧 TOP_TITLE_MM /
        // BOTTOM_TITLE_MM / BOTTOM_CREDIT_MM の public const はここから削除し、TextSizePolicy 内
        // private const へ移設した ── DrawingFileWriter から数値を直接触れないようにする意図。

        // 外枠 (= 図面輪郭) の用紙端からの余白 cm の default 値。 電子納品基準 (国交省 CAD製図基準)
        // で A0/A1 = 20mm、 A2/A3/A4 = 10mm 以上 〜 7.5mm。
        // 2026-06-18 user 指示「デフォルト 15mm くらいが見やすいな」 ── A2 規定 (10mm) と A1 規定
        // (20mm) の中間 15mm を default に (旧 2.0cm から変更)。 UI で 7.5/10/15/20mm のいずれにも
        // 切替可能、 const はあくまで default 値の正。 runtime 値は var outerMarginCm。
        const val DEFAULT_OUTER_MARGIN_CM = 1.5f
    }

    // 外枠余白の runtime 値。 UI で user が選択した値を WebFrame.renderFrame 経由でここに書き込み、
    // writeOuterFrame / writeDrawingFrame / writeTopTitle が参照する (= 1 つの軸を変えれば連動)。
    var outerMarginCm: Float = DEFAULT_OUTER_MARGIN_CM

    open fun writeDrawingFrame(scale: Float = 1f, textsize: Float){

        // 2026-08-13 発見のバグ修正 (writeTopTitle と同根): ここも常に scale=1 で resolve する。
        // frameTextSize/creditTextSize を scale 依存にすると、scale 非依存の boxWidth 群
        // (contentBoxWidth 等) との TextFit 比較基準がズレて縮小判定が scale ごとに変わってしまう
        // (DXF は常に scale=1 なので影響なし、SFC の scale=printscale_ 直渡し経路でだけ発生)。
        // scale は最終的な DrawPrim 生成時に *scale で一度だけ適用する。
        val frameTextSize = TextSizePolicy.resolve(TextRole.BottomTitleFrame)
        val creditTextSize = TextSizePolicy.resolve(TextRole.BottomCredit)

        //外枠と上部のタイトル
        writeOuterFrame(scale)

        // rx = 表題欄右辺 = 外枠右辺と共用 (outerMarginCm に追従)。by = 表題欄下辺 = 外枠下辺と共用。
        val rx = paperWcm - outerMarginCm
        val by = outerMarginCm
        val st = printscale_*100f

        // 表題欄の左辺 = 紙を「中央で谷折り→右側フラップが紙幅の 1/4 になるよう蛇腹折り」した時の
        // 折り線 (2026-08-12 user 確定)。foldX は outerMarginCm に依存しない紙面固定値、rx は外枠右端
        // (= outerMarginCm 依存) なので、表題欄の幅 boxWidth = rx - foldX は outerMarginCm を変えると
        // 連動して変わる (= 「フラップの中で右辺/下辺は内枠の分だけ狭くなる」を式で表現)。
        // 旧仕様は幅固定 10cm 決め打ちで、折り線ともマージン変更とも無関係だった。
        val foldX = paperWcm * 0.75f
        val boxWidth = rx - foldX
        // 内訳は旧 10cm 前提レイアウトの各要素比率 (rx からのオフセット ÷ 10) をそのまま保つ ──
        // 個々のセル比率を再設計するのではなく、幅が変わっても崩れないよう相似形にスケールするだけ。
        fun px(oldOffsetFromRx: Float): Float = rx - (oldOffsetFromRx / 10f) * boxWidth

        val boxLeft = px(10f)          // == foldX
        val labelDivider = px(8f)
        val labelCenter = px(9f)
        val strx = px(7.5f) * scale
        val midDivider = px(5f)        // 縮尺 / 図面番号 の左右分割
        val subDivider = px(3f)        // 図面番号 label/content 分割
        val scaleContentX = px(6.5f)
        val numLabelCenter = px(4f)
        val numContentX = px(1.5f)

        // 内容列の実幅 (= 文字が収まるべき箱)。TextFit.fitSize に渡して、決め打ちサイズで
        // はみ出す前に縮める。右端に 0.2cm 余白を残す。
        val contentBoxWidth = (rx - strx / scale) - 0.2f
        // 縮尺/図面番号 の 4 要素 (ラベル2+内容2) 個別の箱幅。外部 CAD (CADWe'll) で実際に開いて
        // 初めて発覚: この行だけラベルも内容も TextFit を適用しておらず、均等 4 分割の狭い列に
        // 実フォント (MS Gothic 系、さわらびゴシックより字幅が広い) で描くと隣とぶつかっていた
        // (2026-08-12、dumptexts で座標を数値確認: 4 要素が 2.25cm 間隔の中心配置なのに幅チェック皆無)。
        val labelBoxWidth = (labelDivider - boxLeft) - 0.1f
        val scaleContentBoxWidth = (midDivider - labelDivider) - 0.1f
        val numLabelBoxWidth = (subDivider - midDivider) - 0.1f
        val numContentBoxWidth = (rx - subDivider) - 0.1f
        // 決め打ちサイズで箱に収まらない文字列は TextFit で縮める。全 cell 共通の入口にする
        // (長い工事名だけ改行、他は無条件はみ出し、という非対称が今回の不具合の元だった)。
        // DrawPrim.Text.size は Float (寸法系 model / 枠系 paper-cm の両方を運ぶ ADR 0001 の
        // 2 path 構造のため型を付けていない)。キャップハイト型はここで境界を越えて落ちる。
        fun fitted(text: String, boxWidth: Float): Float = TextFit.fitSize(text, boxWidth, frameTextSize).size.value

        val yKOUJIMEI = (by + 5.5f) * scale // cell 中央 (alignV=2 と整合)
        val yo = 0.2f * scale
        val nengappi = currentDateStringJp()
        val w = WHITE

        val prims = mutableListOf<DrawPrim>(
            // 枠線 (yoko/tate/uchi-tate + 行罫線 + 図面番号欄の縦罫)。下辺=by、右辺=rx、左辺=boxLeft (折り線)。
            DrawPrim.Line(com.example.trilib.PointXY(boxLeft, by + 6f, scale), com.example.trilib.PointXY(rx, by + 6f, scale), w),       // 上辺
            DrawPrim.Line(com.example.trilib.PointXY(boxLeft, by, scale),       com.example.trilib.PointXY(boxLeft, by + 6f, scale), w), // 左辺 (= 折り線)
            DrawPrim.Line(com.example.trilib.PointXY(labelDivider, by, scale),  com.example.trilib.PointXY(labelDivider, by + 6f, scale), w),  // ラベル列 縦罫
            DrawPrim.Line(com.example.trilib.PointXY(boxLeft, by + 5f, scale), com.example.trilib.PointXY(rx, by + 5f, scale), w),       // 行罫
            DrawPrim.Line(com.example.trilib.PointXY(boxLeft, by + 4f, scale), com.example.trilib.PointXY(rx, by + 4f, scale), w),
            DrawPrim.Line(com.example.trilib.PointXY(boxLeft, by + 3f, scale), com.example.trilib.PointXY(rx, by + 3f, scale), w),
            DrawPrim.Line(com.example.trilib.PointXY(boxLeft, by + 2f, scale), com.example.trilib.PointXY(rx, by + 2f, scale), w),
            DrawPrim.Line(com.example.trilib.PointXY(boxLeft, by + 1f, scale), com.example.trilib.PointXY(rx, by + 1f, scale), w),
            DrawPrim.Line(com.example.trilib.PointXY(midDivider, by + 1f, scale), com.example.trilib.PointXY(midDivider, by + 2f, scale), w),  // 図番欄 縦罫
            DrawPrim.Line(com.example.trilib.PointXY(subDivider, by + 1f, scale), com.example.trilib.PointXY(subDivider, by + 2f, scale), w),
            // 題字 (左端ラベル列)。 y = cell 中央 (= cellBottomY + 0.5cm)、 alignV=2 (middle) で
            // CAD 標準センタリング (= AutoCAD group code 73=2、 SXF 中心点指定、 backend で glyph bbox 中央化)。
            DrawPrim.Text(zumeninfo.koujiname,    com.example.trilib.PointXY(labelCenter, by + 5.5f, scale), w, fitted(zumeninfo.koujiname, labelBoxWidth) * scale, 1, 2, 0.0, 1f),
            DrawPrim.Text(zumeninfo.tDtype_,      com.example.trilib.PointXY(labelCenter, by + 4.5f, scale), w, fitted(zumeninfo.tDtype_, labelBoxWidth) * scale, 1, 2, 0.0, 1f),
            DrawPrim.Text(zumeninfo.tDname_,      com.example.trilib.PointXY(labelCenter, by + 3.5f, scale), w, fitted(zumeninfo.tDname_, labelBoxWidth) * scale, 1, 2, 0.0, 1f),
            DrawPrim.Text(zumeninfo.tDateHeader_, com.example.trilib.PointXY(labelCenter, by + 2.5f, scale), w, fitted(zumeninfo.tDateHeader_, labelBoxWidth) * scale, 1, 2, 0.0, 1f),
            DrawPrim.Text(zumeninfo.tScale_,      com.example.trilib.PointXY(labelCenter, by + 1.5f, scale), w, fitted(zumeninfo.tScale_, labelBoxWidth) * scale, 1, 2, 0.0, 1f),
            DrawPrim.Text(zumeninfo.tNum_,        com.example.trilib.PointXY(numLabelCenter, by + 1.5f, scale), w, fitted(zumeninfo.tNum_, numLabelBoxWidth) * scale, 1, 2, 0.0, 1f),
            DrawPrim.Text(zumeninfo.tAname_,      com.example.trilib.PointXY(labelCenter, by + 0.5f, scale), w, fitted(zumeninfo.tAname_, labelBoxWidth) * scale, 1, 2, 0.0, 1f),
            // tCredit (= url、 = BottomCredit region): 外枠左下角 anchor + alignV=3 (top、 anchor が
            // text 上端)。 anchor y = outerMarginCm (= 外枠下辺ぴったり) = text 上端を外枠下辺と
            // 一致させる、 = 文字は外枠下辺の真下に物理的に降りる。 web canvas で glyph 物理上端
            // (= measureText.actualBoundingBoxAscent) 補正、 CAD 標準センタリングと同じ「グリフを観る」 path。
            // alignH=0 (left) で文字左端 = 外枠左辺ぴったり。 outerMarginCm を変えれば url も追従。
            DrawPrim.Text(zumeninfo.tCredit_,     com.example.trilib.PointXY(outerMarginCm, outerMarginCm, scale), w, creditTextSize.value * scale, 0, 3, 0.0, 1f),
        )
        // 工事名は縮小を先に試し、縮小の下限 (TextFit の minSize) でも収まらない特に長い文字列だけ
        // 改行にフォールバックする (2026-08-12: 旧仕様は無条件改行で、縮小後なら 1 行に収まる
        // ケースでも改行し、2 行目が下のセルの罫線を越えて衝突するバグがあった。実際に長い工事名で
        // 描画して確認済み)。
        val koujinameFit = TextFit.fitSize(koujiname_, contentBoxWidth, frameTextSize)
        if (koujinameFit.wraps) {
            prims.addAll(kaigyouPrims(koujiname_, 25, strx, yKOUJIMEI, yo, w, koujinameFit.size.value * scale))
        } else {
            prims.add(DrawPrim.Text(koujiname_, com.example.trilib.PointXY(strx, yKOUJIMEI, scale), w, koujinameFit.size.value * scale, 0, 2, 0.0, 1f))
        }
        // 内容 prim も cell 中央 + alignV=2 (middle) で 統一 (= CAD 標準)。
        prims.add(DrawPrim.Text(zumeninfo.zumentitle, com.example.trilib.PointXY(strx, (by + 4.5f) * scale), w, fitted(zumeninfo.zumentitle, contentBoxWidth) * scale, 0, 2, 0.0, 1f))
        prims.add(DrawPrim.Text(rosenname_,           com.example.trilib.PointXY(strx, (by + 3.5f) * scale), w, fitted(rosenname_, contentBoxWidth) * scale, 0, 2, 0.0, 1f))
        prims.add(DrawPrim.Text(nengappi,             com.example.trilib.PointXY(strx, (by + 2.5f) * scale), w, fitted(nengappi, contentBoxWidth) * scale, 0, 2, 0.0, 1f))
        prims.add(DrawPrim.Text("1/${st.toInt()} ($paperName)", com.example.trilib.PointXY(scaleContentX, by + 1.5f, scale), w, fitted("1/${st.toInt()} ($paperName)", scaleContentBoxWidth) * scale, 1, 2, 0.0, 1f))
        prims.add(DrawPrim.Text(zumennum_,            com.example.trilib.PointXY(numContentX, by + 1.5f, scale), w, fitted(zumennum_, numContentBoxWidth) * scale, 1, 2, 0.0, 1f))
        prims.add(DrawPrim.Text(gyousyaname_,         com.example.trilib.PointXY(strx, (by + 0.5f) * scale), w, fitted(gyousyaname_, contentBoxWidth) * scale, 0, 2, 0.0, 1f))

        drawScene(prims)
    }

    // 旧 writeTextWithKaigyou / splitAndWriteText は段A で kaigyouPrims (DrawPrim.Text を返す純粋版)
    // に置き換え済み。同じ分割ロジックの 2 重持ちを避けるため削除した (一元化の意図そのもの)。

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

        // 1. 三角形リスト
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

        // 2. 台形リスト (Rectangle)
        if (traps_.isNotEmpty()) {
            val titleTrap = TitleParamStr(
                type = "面積",
                n = "番号",
                a = "下底",
                b = "上底",
                c = "高さ"
            )
            val trapEditList = object : EditList<Rectangle>() {
                override fun getArea(): Float = traps_.sumOf { it.getArea().toDouble() }.toFloat()
                override fun size(): Int = traps_.size
                override fun get(num: Int): CycleShape = traps_[num - 1]
            }
            mutable_baseY = writeCalcSheetEditList(
                trapEditList,
                titleTrap,
                baseX,
                mutable_baseY,
                textsize,
                xoffset,
                scale,
                shokeiNum
            )
            shokeiNum ++
        }

        // 3. 台形の子三角形
        if (trapTris_.isNotEmpty()) {
            val trapTriEditList = object : EditList<Triangle>() {
                override fun getArea(): Float = trapTris_.sumOf { it.getArea().toDouble() }.toFloat()
                override fun size(): Int = trapTris_.size
                override fun get(num: Int): CycleShape = trapTris_[num - 1]
            }
            mutable_baseY = writeCalcSheetEditList(
                trapTriEditList,
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

        // 4. 控除リスト
        if( dedlist.size() > 0 ) {
            mutable_baseY = writeCalcSheetEditList(
                dedlist,
                titleDed_,
                baseX,
                mutable_baseY,
                textsize,
                xoffset,
                scale,
                shokeiNum
            )
        }

        mutable_baseY -= yoffset
        writeTextHV(zumeninfo.mGoukei_,
            com.example.trilib.PointXY(baseX, mutable_baseY), WHITE, textsize, 1, 1, 0.0, scale)

        val totalArea = trilist_.getArea() + traps_.sumOf { it.getArea().toDouble() } + trapTris_.sumOf { it.getArea().toDouble() } - dedlist_.getArea()
        val totalAreaFloat = maxOf(0.0, totalArea).toFloat()

        writeTextHV(
            totalAreaFloat.formattedString(2),
            com.example.trilib.PointXY(baseX + xoffset * 4, mutable_baseY),
            WHITE,
            textsize,
            1,
            1,
            0.0,
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
            com.example.trilib.PointXY(left, top),
            com.example.trilib.PointXY(right, top), WHITE, scale)
        // bottom left right
        writeLine(
            com.example.trilib.PointXY(left, bottom),
            com.example.trilib.PointXY(right, bottom),WHITE, scale)
        // left middle bottom
        writeLine(
            com.example.trilib.PointXY(left, middle),
            com.example.trilib.PointXY(left, bottom),WHITE, scale)
        // right middle bottom
        writeLine(
            com.example.trilib.PointXY(right, middle),
            com.example.trilib.PointXY(right, bottom),WHITE, scale)
    }

    fun writeHalfBox(baseX: Float,
                     xoffset: Float,
                     basey: Float,
                     yspacer: Float,
                     yoffset: Float,
                     scale: Float ){
        writeLine(
            com.example.trilib.PointXY(
                baseX - xoffset * 0.5f,
                basey + yspacer
            ),
            com.example.trilib.PointXY(
                baseX + xoffset * 4.5f,
                basey + yspacer
            ),WHITE, scale)
        writeLine(
            com.example.trilib.PointXY(
                baseX - xoffset * 0.5f,
                basey + yoffset + yspacer
            ),
            com.example.trilib.PointXY(
                baseX - xoffset * 0.5f,
                basey + yspacer
            ),WHITE, scale)
        writeLine(
            com.example.trilib.PointXY(
                baseX + xoffset * 4.5f,
                basey + yoffset + yspacer
            ),
            com.example.trilib.PointXY(
                baseX + xoffset * 4.5f,
                basey + yspacer
            ),WHITE, scale)
    }

    fun writeCalcSheetEditList(
        editList: EditList<*>,
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

        if( editList !is DeductionList) {
            writeTextHV(titleParamStr.n,
                com.example.trilib.PointXY(baseX, basey), color, ts, 1, 1, 0.0, scale)
            writeTextHV(
                titleParamStr.c+"(m)",
                com.example.trilib.PointXY(baseX + xoffset * 3, basey),
                color,
                ts,
                1,
                1,
                0.0,
                scale
            )
        }
        if( editList is DeductionList) {
            writeTextHV(titleParamStr.name,
                com.example.trilib.PointXY(baseX, basey), color, ts, 1, 1, 0.0, scale)
            writeTextHV(
                titleParamStr.pl,
                com.example.trilib.PointXY(baseX + xoffset * 3, basey),
                color,
                ts,
                1,
                1,
                0.0,
                scale
            )
        }
        writeTextHV(titleParamStr.a+"(m)",
            com.example.trilib.PointXY(baseX + xoffset, basey), color, ts, 1, 1, 0.0, scale)
        writeTextHV(
            titleParamStr.b+"(m)",
            com.example.trilib.PointXY(baseX + xoffset * 2, basey),
            color,
            ts,
            1,
            1,
            0.0,
            scale
        )
        writeTextHV(
            titleParamStr.type+"(m2)",
            com.example.trilib.PointXY(baseX + xoffset * 4, basey),
            color,
            ts,
            1,
            1,
            0.0,
            scale
        )

        writeHalfBox(baseX,xoffset,basey,yspacer,yoffset,scale)

        basey -= yoffset

        for( number in 1 .. editList.size() ){
            writeCalcSheetLine( editList.get(number), baseX, basey, ts, color, scale )
            basey -= yoffset
        }

        writeTextHV(zumeninfo.mSyoukei_+"("+syokeiNum+")",
            com.example.trilib.PointXY(baseX, basey), color, ts, 1, 1, 0.0, scale)
        writeTextHV(
            editList.getArea().formattedString(2),
            com.example.trilib.PointXY(baseX + xoffset * 4, basey),
            color,
            ts,
            1,
            1,
            0.0,
            scale
        )

        writeHalfBox(baseX,xoffset,basey,yspacer,yoffset,scale)

        basey -= yoffset
        return basey
    }

    fun writeCalcSheetLine(
        editObject: CycleShape,
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


        if( editObject is Triangle || editObject is Rectangle) {
            writeTextHV(param.number.toString(),
                com.example.trilib.PointXY(baseX, baseY), color, ts, 1, 1, 0.0, scale)
            writeTextHV(
                param.b.formattedString(2),
                com.example.trilib.PointXY(baseX + xoffset * 2, baseY),
                color,
                ts,
                1,
                1,
                0.0,
                scale
            )
            writeTextHV(
                param.c.formattedString(2),
                com.example.trilib.PointXY(baseX + xoffset * 3, baseY),
                color,
                ts,
                1,
                1,
                0.0,
                scale
            )
        }
        if( editObject is Deduction){
            writeTextHV(param.name,
                com.example.trilib.PointXY(baseX, baseY), color, ts, 1, 1, 0.0, scale)
            writeTextHV(
                    param.type,
                com.example.trilib.PointXY(baseX + xoffset * 3, baseY),
                color,
                ts,
                1,
                1,
                0.0,
                scale
            )
        }
        if( param.type =="Box" )      writeTextHV(
            param.b.formattedString(2),
            com.example.trilib.PointXY(baseX + xoffset * 2, baseY),
            color,
            ts,
            1,
            1,
            0.0,
            scale
        )

        writeTextHV(
            param.a.formattedString(2),
            com.example.trilib.PointXY(baseX + xoffset, baseY),
            color,
            ts,
            1,
            1,
            0.0,
            scale
        )
        writeTextHV(
            editObject.getArea().formattedString(2),
            com.example.trilib.PointXY(baseX + xoffset * 4, baseY),
            color,
            ts,
            1,
            1,
            0.0,
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