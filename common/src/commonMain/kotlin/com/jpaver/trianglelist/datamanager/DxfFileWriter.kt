package com.jpaver.trianglelist.datamanager

import com.jpaver.trianglelist.dxf.DxfConstants
import com.jpaver.trianglelist.editmodel.Deduction
import com.jpaver.trianglelist.editmodel.DeductionList
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import com.jpaver.trianglelist.editmodel.ZumenInfo
import com.jpaver.trianglelist.label.DimensionLayout
import com.jpaver.trianglelist.label.DimensionPlacement
import com.jpaver.trianglelist.myName_
import com.jpaver.trianglelist.viewmodel.TitleParamStr


class DxfFileWriter(override var trilist_: TriangleList = TriangleList(),
                    override var dedlist_: DeductionList = DeductionList(),
                    override var zumeninfo: ZumenInfo = ZumenInfo(),
                    override var titleTri_: TitleParamStr = TitleParamStr(),
                    override var titleDed_: TitleParamStr = TitleParamStr()
): DrawingFileWriter() {
    //region parameters
    lateinit var writer: Appendable
    lateinit var drawingLength: com.example.trilib.PointXY // = drawingLength

    var isDebug = false

    // Shared handle generator for the whole DXF
    private val handleGen = HandleGen(100)  // Same starting point as legacy version
    
    // Entity handle management - use HandleGen for consistency
    // private var entityHandle = 100

    // Minimal TABLES / OBJECTS builders
    private val tablesBuilder = TablesBuilder()
    private val objectsBuilder = ObjectsBuilder(handleGen)
    val dxfheader = DxfHeader(handleGen)
    
    // Entity writer
    private lateinit var dxfEntity: DxfEntity

    override var textscale_ = trilist_.getPrintTextScale( 1f , "dxf")
    override var printscale_ = trilist_.getPrintScale(1f)
    // 注意: printscale_は縮尺分母の逆数 (0.5 = 1/50, 0.04 = 1/25など)
    // DXF はモデル空間 mm 単位なので cm→出力は ×1000 (基底/PDF/SFC の ×10 とは違う)。
    // paperWcm は save() で paper フィールドから設定 (A3: 42 → sizeX_=42000*ps と同値)
    override val sizeX_ get() = paperWcm * 1000f * printscale_
    override val sizeY_ get() = paperHcm * 1000f * printscale_

    override var WHITE = DxfConstants.Colors.WHITE
    override var BLUE = DxfConstants.Colors.BLUE
    override var RED = DxfConstants.Colors.RED

    var activeLayer = "0"

    // 用紙サイズと縮尺の設定（外部から変更可能）
    var paper: Paper = Paper.A3_LAND
    var customPrintScale: PrintScale? = null

    // save() で確定した縮尺。writeEntities のペーパー空間 VIEWPORT が同じ値を使う
    private var currentPrintScale: PrintScale = PrintScale(1f, 50f)

    //endregion parameters

    override fun save(){
        // 用紙サイズの単一の出所 = paper フィールド。枠・タイトル欄・図形センタリング・
        // ビューポートすべてここから導出する (基底 paperWcm/paperHcm/paperName へ伝播)
        paperWcm = paper.width / 10f
        paperHcm = paper.height / 10f
        paperName = paper.name

        // Initialize entity writer
        dxfEntity = DxfEntity(handleGen, unitscale_, activeLayer)

        // Use configured paper and print scale
        // getPrintScale は 0.5→1/50, 2.0→1/200, 5.0→1/500 (縮尺分母 = printscale_ * 100)。
        // 図面枠の実寸 sizeX_ = 42000*printscale_ = 420mm*分母 とも一致する
        val actualScale = printscale_ * 100f
        val printScale = customPrintScale ?: PrintScale(1f, actualScale)
        currentPrintScale = printScale

        // ログ出力：設定値を確認
        println("=== DXF生成設定 ===")
        println("用紙: ${paper.name} (${paper.width}×${paper.height}mm)")
        println("縮尺: 1/${printScale.paper.toInt()} (model=${printScale.model}, paper=${printScale.paper})")
        println("printscale_: $printscale_")
        println("customPrintScale: $customPrintScale")
        println("unitscale_: $unitscale_")
        println("textscale_: $textscale_")
        println("sizeX_: $sizeX_")
        println("sizeY_: $sizeY_")

        DxfHeader(handleGen).header(writer, paper, printScale)  // ← 引数化対応
        tablesBuilder.writeMinimalTables(writer, listOf("0", "C-COL-COL1", "C-TTL-FRAM"))  // ← ビルダー版
        writeEntities()
        objectsBuilder.writeCompleteObjects(writer, paper, printScale, "32")  // ← ビルダー版

    }


    fun verticalFromBaseline(vertical: Int, p1: com.example.trilib.PointXY, p2: com.example.trilib.PointXY): Int{
        // 垂直方向の文字位置合わせタイプ(省略可能、既定 = 0): 整数コード(ビットコードではありません):
        // 0 = 基準線、1 = 下、2 = 中央、3 = 上
        // ベクトルの方向でB,Cを表現するなら
        // x軸の方向で正負を表す。正の時は下1が内、負の時は上3が内。

        // 挟角の 外:3 内:1　in Triangle
        // 基準線の　下:3 上:1
        val LOWER = 3
        val UPPER = 1
        val OUTER = 1

        //基準線の方向が右向きか左向きかで上下を反転する

        // 外側
        if (vertical == OUTER) {
            // 基準線が右向き の場合
            return if ( p1.isVectorToRight(p2) ) UPPER else LOWER
        }

        // 内側
        return if ( p1.isVectorToRight(p2) ) LOWER else UPPER

    }

    override fun writeTriangle(tri: Triangle){
        // arrange
        val (pca, pab, pbc) = xyPointXYTriple(tri)

        // ADR 0003 Phase 2a: 寸法座標の出所を Triangle のキャッシュ (dimpoint/dimOnPath) から
        // common の式 DimensionLayout に切替。gapPaperMm=0 なので出力はキャッシュ由来と同値
        // (同値性は Phase 1 parity + golden diff テストで固定)
        val (placeA, placeB, placeC) = layoutTriple(tri)

        var (la, lb, lc) = stringTriple(tri)

        val textSize: Float = textscale_
        val textSize2: Float = textscale_

        // TriLines
        writeTriangleLines(tri,WHITE)

        if(isDebug){
            la += "A${placeA.verticalDxf}"
            lb += "B${placeB.verticalDxf}"
            lc += "C${placeC.verticalDxf}"
        }

        // DimTexts
        if( tri.mynumber == 1 || tri.connectionSide > 2)
            writeTextDimension(placeA.verticalDxf, la, placeA.dimpoint, pab.calcDimAngle(pca))
        writeTextDimension(placeB.verticalDxf, lb, placeB.dimpoint, pbc.calcDimAngle(pab))
        writeTextDimension(placeC.verticalDxf, lc, placeC.dimpoint, pca.calcDimAngle(pbc))

        //DimFlags
        writeDimFlagsFromLayout(tri, placeA, placeB, placeC, WHITE)

        // 番号
        writePointNumber(tri, textSize,BLUE,1,2, textSize*0.85f )

        // 測点
        if(tri.myName_() != "") {
            writeSokutenFromLayout(tri, trilist_.sokutenListVector, textSize2, BLUE, 1, 1)
        }
    }

    /**
     * 3 辺の寸法配置を DimensionLayout で計算する。
     * 入力は Triangle のキャッシュ (setDimPath) と同じ:
     * A=(pointAB,point[0]), B=(pointBC,pointAB), C=(point[0],pointBC) + dim の縦横コード。
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

    /** DrawingFileWriter.writeDimFlags の置換: 旗揚げ線の両端を DimensionLayout の計算結果から取る */
    private fun writeDimFlagsFromLayout(
        tri: Triangle,
        placeA: DimensionPlacement,
        placeB: DimensionPlacement,
        placeC: DimensionPlacement,
        color: Int
    ) {
        if (tri.dim.horizontal.a > 2) writeLine(placeA.pointA, placeA.pointB, color)
        if (tri.dim.horizontal.b > 2) writeLine(placeB.pointA, placeB.pointB, color)
        if (tri.dim.horizontal.c > 2) writeLine(placeC.pointA, placeC.pointB, color)
    }

    /**
     * DrawingFileWriter.writeSokuten の置換: 測点名の位置を DimensionLayout (SIDE_SOKUTEN) から取る。
     * 旧実装は setDimPath/setDimPoint でキャッシュを書き直してから読んでいた (= 常に再計算) ため、
     * 式の直接呼び出しと同値。キャッシュへの書き戻し副作用は以降誰も読まないので落とした。
     */
    private fun writeSokutenFromLayout(
        tri: Triangle,
        normalizedvector: Int,
        ts: Float,
        color: Int,
        align1: Int,
        align2: Int
    ) {
        val place = DimensionLayout.layout(
            tri.pointAB, tri.point[0],
            DimensionLayout.SIDE_SOKUTEN, tri.dim.horizontal.s,
            tri.scaleFactor.toDouble(), tri.dimHeight.toDouble(), 0.0
        )
        val pa = place.pointA
        val pb = place.pointB
        writeTextSwitch(tri.name, place.dimpoint, ts, color, align1, align2, pb.calcSokAngle(pa, normalizedvector))
        writeLine(pa, pb, color)
    }

    private fun writeTextDimension(verticalAlign: Int, len: String, p1: com.example.trilib.PointXY, angle: Double){
        writeTextHV(len, p1, WHITE, textscale_, 1, verticalAlign, angle, 1f)
    }

    override fun writeTextHV(
        text: String,
        point: com.example.trilib.PointXY,
        color: Int,
        textsize: Float,
        alignH: Int,
        alignV: Int,
        angle: Double,
        scale: Float
    ){
        dxfEntity.writeTextHV(writer, text, point, color, textsize, alignH, alignV, angle)
    }

    override fun writeLine(p1: com.example.trilib.PointXY, p2: com.example.trilib.PointXY, color: Int, scale: Float ) {
        dxfEntity.writeLine(writer, p1, p2, color)
    }

    override fun writeCircle(point: com.example.trilib.PointXY, size: Float, color: Int, scale: Float){
        dxfEntity.writeCircle(writer, point, size, color)
    }

    override fun writeTextAndLine(
        st: String,
        p1: com.example.trilib.PointXY,
        p2: com.example.trilib.PointXY,
        textsize: Float,
        scale: Float
    ){
        dxfEntity.writeTextAndLine(writer, st, p1, p2, textsize)
    }

    override fun writeDeduction( ded: Deduction){

        //val ded = dedlist_.get( dednumber )
        val textSize = textscale_
        val infoStrLength = ded.infoStr.length*textSize+0.3f
        val point = ded.point
        val pointFlag = ded.pointFlag
        var textOffsetX = 0f
        if( ded.type == "Box" ) textOffsetX = -0.5f

        if(point.x <= pointFlag.x) {  //ptFlag is RIGHT from pt
            writeLine( point, pointFlag, RED)
            writeTextAndLine(
                ded.infoStr,
                pointFlag,
                pointFlag.plus((infoStrLength + textOffsetX).toDouble(),0.0),
                textSize,
                1f
            )
        } else {                     //ptFlag is LEFT from pt
            writeLine( point, pointFlag, RED)
            writeTextAndLine(
                ded.infoStr,
                pointFlag.plus((-ded.getInfo().length*textSize - textOffsetX).toDouble(),0.0),
                pointFlag,
                textSize,
                1f
            )
        }

        if(ded.type == "Circle") writeCircle(point, ded.lengthX/2, RED, 1f)
        if(ded.type == "Box")    writeDedRect(ded)//writeDXFRect(writer, point, ded.lengthX, ded.lengthY, 1)
    }

    private fun writeDedRect(ded: Deduction){
        val color = RED
        ded.shapeAngle = -ded.shapeAngle // 逆回転
        ded.setBox( 1.0 )
        writeLine( ded.pLTop, ded.pLBtm, color)
        writeLine( ded.pLTop, ded.pRTop, color)
        writeLine( ded.pRTop, ded.pRBtm, color)
        writeLine( ded.pLBtm, ded.pRBtm, color)
    }

    override fun writeEntities(){

        // 最初に書く
        writer.append("""
            0
            SECTION
            2
            ENTITIES
        """.trimIndent())
        writer.append('\n')

        val myDXFTriList = trilist_.clone()
        val myDXFDedList = dedlist_.clone()

        // Ｙ軸方向反転、かつビュースケールで割り戻して大きさをtriListと揃える。
        myDXFDedList.scale(com.example.trilib.PointXY(0f, 0f),1/ viewscale_,-1/ viewscale_)

        val center = com.example.trilib.PointXY(
            paperWcm / 2f * printscale_,
            paperHcm / 2f * printscale_
        )
        val tricenter = myDXFTriList.center
        myDXFDedList.move(
            com.example.trilib.PointXY(
                center.x - tricenter.x,
                center.y - tricenter.y
            )
        )
        myDXFTriList.move(
            com.example.trilib.PointXY(
                center.x - tricenter.x,
                center.y - tricenter.y
            )
        )

        var trilistNumbered = myDXFTriList.numbered( startTriNumber_ )
        if(isReverse_) {
            trilistNumbered = trilistNumbered.resetNumReverse()
            myDXFDedList.reverse()
        }

        // アウトラインの描画
        //myDXFTriList.setChildsToAllParents()

        val RED = 16769517//16769000//16773364//16767449
        val ORANGE = 16770756//16766660//16774376//16766890
        val YELLOW = 16777180//n16646073//16775353//16776633//16776929//16777130
        val GREEN = 14482130//14811071//13828031//15400929//11796394
        val BLUE = 14939391//14941951//14286079
        val sixtytwo = arrayOf( 254, 254, 51, 254, 7)
        val color = arrayOf( RED, ORANGE, YELLOW, GREEN, BLUE )

        val sprit = false

        if( sprit == true ){
            writeSpritTrilist(myDXFTriList,trilistNumbered,color,sixtytwo)
        }
        else{
            for (index in 1 .. myDXFTriList.size()) {
                writeTriangle(trilistNumbered.get(index))
            }
        }

        // deduction
        for (number in 1 .. myDXFDedList.size()) {
            writeDeduction( myDXFDedList.get(number) )
        }

        unitscale_ *= printscale_
        activeLayer = "C-TTL-FRAM"
        dxfEntity.setUnitScale(unitscale_)
        dxfEntity.setActiveLayer(activeLayer)
        writeDrawingFrame(textsize = textscale_)
        writeTopTitle(textsize = textscale_)
        unitscale_ = 1000f
        dxfEntity.setUnitScale(unitscale_)

        if(isReverse_) {
            trilistNumbered = trilistNumbered.reverse()
        }
        // calcSheet
        writeCalcSheet(1f, textscale_, trilistNumbered, myDXFDedList )

        // ペーパー空間 VIEWPORT: レイアウト側で図面枠が縮尺どおり (例 1/50) に見えるようにする。
        // これが無いと PLOTSETTINGS の縮尺だけでは CAD はレイアウト表示倍率を決められない
        writePaperSpaceViewport()

        //一番最後に書く
        writer.append("""
            0
            ENDSEC
        """.trimIndent())
        writer.append('\n')

    }

    /**
     * Layout1 (ペーパー空間) に VIEWPORT を 2 枚書く。
     * AutoCAD R2000 (AC1015) のレイアウトは「メインビューポート (id=1, status=1)」が
     * 必須で、これが無いと CADWe'll 土木等は尺度付きビューポートシートを構築しない
     * (検証 2026-06-12: ezdxf 1.4.1 生成ファイルは id=1 と id=2 の 2 枚を持ち
     *  CADWe'll で "Layout1 1/50" シートが生成される。id=1 を欠いた当初版は
     *  「モデル/ベース」しか出ず尺度が伝わらなかった)。
     * 1 枚目: 紙面全体を映すメインビューポート (ezdxf の既定値に倣う)。
     * 2 枚目: 図面を縮尺どおり映す内容ビューポート。倍率は 41 (紙上の高さ mm)
     *  / 45 (モデル空間の表示高さ mm) = 1/分母 で表現される。モデル空間の図面枠は
     *  原点起点・紙寸×分母 (A3 1/50 なら 21000×14850mm) なので、ビュー中心は枠中央、
     *  表示高さは紙高×分母にする。
     */
    private fun writePaperSpaceViewport() {
        val den = currentPrintScale.paper / currentPrintScale.model

        // メインビューポート (id=1): 紙面全体。ezdxf の page_setup 既定 (中心=紙×25,
        // 大きさ=紙×55, status flags=557088) をそのまま借用する
        writeViewportEntity(
            centerX = paper.width * 25f, centerY = paper.height * 25f,
            paperW = paper.width * 55f, paperH = paper.height * 55f,
            viewCenterX = paper.width * 25f, viewCenterY = paper.height * 25f,
            viewHeight = paper.height * 55f,
            status = 1, id = 1, flags = 557088
        )

        // 内容ビューポート (id=2): 図面枠を縮尺どおり (1/den) に映す
        writeViewportEntity(
            centerX = paper.width / 2f, centerY = paper.height / 2f,
            paperW = paper.width, paperH = paper.height,
            viewCenterX = paper.width * den / 2f, viewCenterY = paper.height * den / 2f,
            viewHeight = paper.height * den,
            status = 2, id = 2, flags = 0
        )
    }

    private fun writeViewportEntity(
        centerX: Float, centerY: Float,
        paperW: Float, paperH: Float,
        viewCenterX: Float, viewCenterY: Float,
        viewHeight: Float,
        status: Int, id: Int, flags: Int
    ) {
        fun p(gc: Int, v: Any) { writer.append("${gc.toString().padStart(3)}\n$v\n") }

        p(0, "VIEWPORT"); p(5, handleGen.new())
        p(330, "32")                 // owner: *Paper_Space BLOCK_RECORD (TablesBuilder 固定値)
        p(100, "AcDbEntity")
        p(67, 1)                     // paper space entity
        p(8, "VIEWPORTS")
        p(100, "AcDbViewport")
        p(10, centerX); p(20, centerY); p(30, 0.0)   // 紙上の中心
        p(40, paperW); p(41, paperH)                 // 紙上の大きさ
        p(68, status); p(69, id)                     // status / viewport id
        p(12, viewCenterX); p(22, viewCenterY)       // モデル空間のビュー中心
        p(13, 0.0); p(23, 0.0)       // snap base
        p(14, 10.0); p(24, 10.0)     // snap spacing
        p(15, 10.0); p(25, 10.0)     // grid spacing
        p(16, 0.0); p(26, 0.0); p(36, 1.0)   // view direction
        p(17, 0.0); p(27, 0.0); p(37, 0.0)   // view target
        p(42, 50.0)                  // lens length
        p(43, 0.0); p(44, 0.0)       // clip planes
        p(45, viewHeight)            // モデル空間の表示高さ → 倍率 = 41/45
        p(50, 0.0); p(51, 0.0)       // snap angle / view twist
        p(72, 100)                   // circle zoom percent
        p(90, flags)                 // viewport status flags
        p(1, "")                     // plot style sheet
        p(281, 0); p(71, 0); p(74, 0)
        p(110, 0.0); p(120, 0.0); p(130, 0.0)   // UCS origin
        p(111, 1.0); p(121, 0.0); p(131, 0.0)   // UCS X axis
        p(112, 0.0); p(122, 1.0); p(132, 0.0)   // UCS Y axis
        p(79, 0); p(146, 0.0)
    }


    fun writeSpritTrilist(myDXFTriList: TriangleList, trilistNumbered: TriangleList, color: Array<Int>, sixtytwo:Array<Int> ){
        val spritByColors = myDXFTriList.spritByColors()
        for( index in 0 until spritByColors.size ){

            val outlineLists = spritByColors[index].outlineList_ //myDXFTriList.outlineList() //ArrayList<PointXY>()

            if (outlineLists != null) {
                for( index2 in 0 until outlineLists.size){

                    if( outlineLists[index2].size > 0 ){
                        dxfEntity.writeDXFTriHatch(writer, outlineLists[index2], color[index], sixtytwo[index] )
                        //writeDXFTriOutlines( writer, outlineLists[index2] )
                    }
                }
            }
        }

        for (index in 1 .. myDXFTriList.size()) {
            writeTriangle(trilistNumbered.get(index))
        }

        for( index in 0 until spritByColors.size ){

            val outlineLists = spritByColors[index].outlineList_ //myDXFTriList.outlineList() //ArrayList<PointXY>()

            if (outlineLists != null) {
                for( index2 in 0 until outlineLists.size){

                    if( outlineLists[index2].size > 0 ){
                        dxfEntity.writeDXFTriOutlines( writer, outlineLists[index2] )
                    }
                }
            }
        }
    }



    override fun writeHeader(){
        val printScale = customPrintScale ?: PrintScale(1f, printscale_)
        dxfheader.header(writer, paper, printScale)
    }

    override fun writeFooter(){
        // Always use the original DxfObject that works with CAD software
        DxfObject().dxfobject(writer)
    }

    override fun PolymorphFunctionB(): String {
        return "IAMOVERRIDED."
    }

}
