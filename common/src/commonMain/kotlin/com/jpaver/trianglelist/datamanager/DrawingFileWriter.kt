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
        val place = DimensionLayout.layout(
            tri.pointAB, tri.point[0],
            DimensionLayout.SIDE_SOKUTEN, tri.dim.horizontal.s,
            tri.scaleFactor.toDouble(), tri.dimHeight.toDouble(), 0.0
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
        }
    }

    /** 改行付きテキスト (題字の工事名) を DrawPrim.Text 群に展開 (旧 writeTextWithKaigyou/splitAndWriteText の純粋版) */
    private fun kaigyouPrims(str: String, iKaigyou: Int, xr: Float, yb: Float, yo: Float, color: Int, textsize: Float): List<DrawPrim.Text> {
        if (str.length <= iKaigyou) {
            return listOf(DrawPrim.Text(str, com.example.trilib.PointXY(xr, yb), color, textsize, 0, 0, 0.0, 1f))
        }
        val parts = if (str.contains(" ")) str.split(' ', limit = 2)
                    else listOf(str.substring(0, iKaigyou), str.substring(iKaigyou))
        val out = mutableListOf(DrawPrim.Text(parts[0], com.example.trilib.PointXY(xr, yb + yo), color, textsize, 0, 0, 0.0, 1f))
        if (parts.size > 1) out.add(DrawPrim.Text(parts[1], com.example.trilib.PointXY(xr, yb - yo), color, textsize, 0, 0, 0.0, 1f))
        return out
    }

    fun writeOuterFrame(scale: Float = 1f){
        // 外枠描画 (用紙中央・用紙より一回り内側)。A3: 中心(21,14.85)・40×27cm と同値
        val cx = paperWcm / 2f; val cy = paperHcm / 2f
        drawScene(listOf(
            DrawPrim.Rect(com.example.trilib.PointXY(cx, cy, scale), (paperWcm - 2f) * scale, (paperHcm - 2.7f) * scale, WHITE, scale)
        ))
    }

    fun writeTopTitle(scale: Float = 1f, textsize: Float ){
        // 上のタイトル (用紙上中央アンカー)。A3: x=21=W/2, y は上端基準
        val cx = paperWcm / 2f
        // 文字高は writeDrawingFrame の frameTextSize と同じ textsize×scale 規約に揃える。
        // DXF は scale=1f (printscale は unitscale 側) なので不変、SFC は scale=printscale_ で
        // 枠内テキストと同じ実効サイズになる (これが無いと SFC の上中央タイトルだけ printscale 分小さい)。
        val titleTextSize = textsize * scale
        drawScene(listOf(
            DrawPrim.Text(zumeninfo.zumentitle, com.example.trilib.PointXY(cx, paperHcm - 2.6f, scale), WHITE, titleTextSize, 1, 1, 0.0, scale),
            DrawPrim.Text(rosenname_, com.example.trilib.PointXY(cx, paperHcm - 3.7f, scale), WHITE, titleTextSize, 1, 1, 0.0, scale),
            DrawPrim.Line(com.example.trilib.PointXY(cx - 2f, paperHcm - 2.7f, scale), com.example.trilib.PointXY(cx + 2f, paperHcm - 2.7f, scale), WHITE, scale),
            DrawPrim.Line(com.example.trilib.PointXY(cx - 2f, paperHcm - 2.8f, scale), com.example.trilib.PointXY(cx + 2f, paperHcm - 2.8f, scale), WHITE, scale),
        ))
    }

    open fun writeDrawingFrame(scale: Float = 1f, textsize: Float){

        val frameTextSize = textsize * scale

        //外枠と上部のタイトル
        writeOuterFrame(scale)

        // 右下のタイトル枠 (用紙右端アンカー)。rx=用紙右端、A3 では rx-1=41 等で従来と同値。
        // Y は用紙下端 (0) 基準のまま (表題欄は規格上どの用紙でも下端固定サイズ)。
        // 段A: 枠線・題字・内容を DrawPrim のリスト (= 何を何処に) に集約し drawScene に流す。
        // インライン writeLine/writeTextHV と同じ呼び出し・同じ順序なので DXF byte 不変。
        val rx = paperWcm
        val st = printscale_*100f
        val strx = (rx - 8.5f) * scale
        val yKOUJIMEI = 6.7f * scale
        val yo = 0.2f * scale
        val nengappi = currentDateStringJp()
        val w = WHITE

        val prims = mutableListOf<DrawPrim>(
            // 枠線 (yoko/tate/uchi-tate + 行罫線 + 図面番号欄の縦罫)
            DrawPrim.Line(com.example.trilib.PointXY(rx - 11f, 7.35f, scale), com.example.trilib.PointXY(rx - 1f, 7.35f, scale), w),
            DrawPrim.Line(com.example.trilib.PointXY(rx - 11f, 1.35f, scale), com.example.trilib.PointXY(rx - 11f, 7.35f, scale), w),
            DrawPrim.Line(com.example.trilib.PointXY(rx - 9f, 1.35f, scale), com.example.trilib.PointXY(rx - 9f, 7.35f, scale), w),
            DrawPrim.Line(com.example.trilib.PointXY(rx - 11f, 6.35f, scale), com.example.trilib.PointXY(rx - 1f, 6.35f, scale), w),
            DrawPrim.Line(com.example.trilib.PointXY(rx - 11f, 5.35f, scale), com.example.trilib.PointXY(rx - 1f, 5.35f, scale), w),
            DrawPrim.Line(com.example.trilib.PointXY(rx - 11f, 4.35f, scale), com.example.trilib.PointXY(rx - 1f, 4.35f, scale), w),
            DrawPrim.Line(com.example.trilib.PointXY(rx - 11f, 3.35f, scale), com.example.trilib.PointXY(rx - 1f, 3.35f, scale), w),
            DrawPrim.Line(com.example.trilib.PointXY(rx - 11f, 2.35f, scale), com.example.trilib.PointXY(rx - 1f, 2.35f, scale), w),
            DrawPrim.Line(com.example.trilib.PointXY(rx - 6f, 2.35f, scale), com.example.trilib.PointXY(rx - 6f, 3.35f, scale), w),
            DrawPrim.Line(com.example.trilib.PointXY(rx - 4f, 2.35f, scale), com.example.trilib.PointXY(rx - 4f, 3.35f, scale), w),
            // 題字 (工事名・路線名 など、左端ラベル列)
            DrawPrim.Text(zumeninfo.koujiname, com.example.trilib.PointXY(rx - 10f, 6.7f, scale), w, frameTextSize, 1, 0, 0.0, 1f),
            DrawPrim.Text(zumeninfo.tDtype_, com.example.trilib.PointXY(rx - 10f, 5.7f, scale), w, frameTextSize, 1, 0, 0.0, 1f),
            DrawPrim.Text(zumeninfo.tDname_, com.example.trilib.PointXY(rx - 10f, 4.7f, scale), w, frameTextSize, 1, 0, 0.0, 1f),
            DrawPrim.Text(zumeninfo.tDateHeader_, com.example.trilib.PointXY(rx - 10f, 3.7f, scale), w, frameTextSize, 1, 0, 0.0, 1f),
            DrawPrim.Text(zumeninfo.tScale_, com.example.trilib.PointXY(rx - 10f, 2.7f, scale), w, frameTextSize, 1, 0, 0.0, 1f),
            DrawPrim.Text(zumeninfo.tNum_, com.example.trilib.PointXY(rx - 5f, 2.7f, scale), w, frameTextSize, 1, 0, 0.0, 1f),
            DrawPrim.Text(zumeninfo.tAname_, com.example.trilib.PointXY(rx - 10f, 1.7f, scale), w, frameTextSize, 1, 0, 0.0, 1f),
            DrawPrim.Text(zumeninfo.tCredit_, com.example.trilib.PointXY(8f, 1f, scale), w, frameTextSize, 1, 0, 0.0, 1f),
        )
        // 内容: 工事名 (長ければ改行) → 図面名・路線名・作成日・縮尺・図面番号・施工者
        prims.addAll(kaigyouPrims(koujiname_, 25, strx, yKOUJIMEI, yo, w, frameTextSize))
        prims.add(DrawPrim.Text(zumeninfo.zumentitle, com.example.trilib.PointXY(strx, 5.7f * scale), w, frameTextSize, 0, 0, 0.0, 1f))
        prims.add(DrawPrim.Text(rosenname_, com.example.trilib.PointXY(strx, 4.7f * scale), w, frameTextSize, 0, 0, 0.0, 1f))
        prims.add(DrawPrim.Text(nengappi, com.example.trilib.PointXY(strx, 3.7f * scale), w, frameTextSize, 0, 0, 0.0, 1f))
        prims.add(DrawPrim.Text("1/${st.toInt()} ($paperName)", com.example.trilib.PointXY(rx - 7.5f, 2.7f, scale), w, frameTextSize, 1, 0, 0.0, 1f))
        prims.add(DrawPrim.Text(zumennum_, com.example.trilib.PointXY(rx - 2.5f, 2.7f, scale), w, frameTextSize, 1, 0, 0.0, 1f))
        prims.add(DrawPrim.Text(gyousyaname_, com.example.trilib.PointXY(strx, 1.7f * scale), w, frameTextSize, 0, 0, 0.0, 1f))

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
            com.example.trilib.PointXY(baseX, mutable_baseY), WHITE, textsize, 1, 1, 0.0, scale)
        writeTextHV(
            ( trilist_.getArea() - dedlist_.getArea() ).formattedString(2),
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

        if( editList is TriangleList) {
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


        if( editObject is Triangle) {
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