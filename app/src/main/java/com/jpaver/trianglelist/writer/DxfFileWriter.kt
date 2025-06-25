package com.jpaver.trianglelist.writer

import com.jpaver.trianglelist.Deduction
import com.jpaver.trianglelist.DeductionList
import com.jpaver.trianglelist.Triangle
import com.jpaver.trianglelist.TriangleList
import com.jpaver.trianglelist.dataclass.ZumenInfo
import com.jpaver.trianglelist.formattedString
import com.jpaver.trianglelist.myName_
import com.jpaver.trianglelist.util.TitleParamStr
import java.io.BufferedWriter
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset


class DxfFileWriter(override var trilist_: TriangleList = TriangleList(),
                    override var dedlist_: DeductionList = DeductionList(),
                    override var zumeninfo: ZumenInfo = ZumenInfo(),
                    override var titleTri_: TitleParamStr = TitleParamStr(),
                    override var titleDed_: TitleParamStr = TitleParamStr()
): DrawingFileWriter() {
    //region parameters
    lateinit var writer: BufferedWriter
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

    override var textscale_ = trilist_.getPrintTextScale( 1f , "dxf")
    override var printscale_ = trilist_.getPrintScale(1f)//setScale(drawingLength) 
    // 注意: printscale_は縮尺分母の逆数 (0.5 = 1/50, 0.04 = 1/25など)
    override var sizeX_ = 42000f * printscale_
    override var sizeY_ = 29700f * printscale_

    override var WHITE = 256
    override var BLUE = 5
    override var RED = 1

    var activeLayer = "0"

    // 用紙サイズと縮尺の設定（外部から変更可能）
    var paper: Paper = Paper.A3_LAND
    var customPrintScale: PrintScale? = null

    //endregion parameters

    override fun save(){
        // Use configured paper and print scale
        // printscale_=0.5は1/50を意味するので、50 = 1/(printscale_*0.02) または別の変換式が必要
        val actualScale = if (printscale_ == 0.5f) 50f else 1f/printscale_  // 0.5→50への特別変換
        val printScale = customPrintScale ?: PrintScale(1f, actualScale)  // カスタム設定 or 従来値(0.5=1/50)から変換

        // ログ出力：設定値を確認（テスト時は出力しない）
        try {
            android.util.Log.d("DxfFileWriter", "=== DXF生成設定 ===")
            android.util.Log.d("DxfFileWriter", "用紙: ${paper.name} (${paper.width}×${paper.height}mm)")
            android.util.Log.d("DxfFileWriter", "縮尺: 1/${printScale.paper.toInt()} (model=${printScale.model}, paper=${printScale.paper})")
            android.util.Log.d("DxfFileWriter", "printscale_: $printscale_")
            android.util.Log.d("DxfFileWriter", "customPrintScale: $customPrintScale")
            android.util.Log.d("DxfFileWriter", "unitscale_: $unitscale_")
        } catch (e: Exception) {
            // ユニットテスト環境では標準出力に出力
            println("=== DXF生成設定 ===")
            println("用紙: ${paper.name} (${paper.width}×${paper.height}mm)")
            println("縮尺: 1/${printScale.paper.toInt()} (model=${printScale.model}, paper=${printScale.paper})")
            println("printscale_: $printscale_")
            println("customPrintScale: $customPrintScale")
            println("unitscale_: $unitscale_")
        }
        try {
            android.util.Log.d("DxfFileWriter", "textscale_: $textscale_")
            android.util.Log.d("DxfFileWriter", "sizeX_: $sizeX_")
            android.util.Log.d("DxfFileWriter", "sizeY_: $sizeY_")
        } catch (e: Exception) {
            println("textscale_: $textscale_")
            println("sizeX_: $sizeX_")
            println("sizeY_: $sizeY_")
        }

        DxfHeader(handleGen).header(writer, paper, printScale)  // ← 引数化対応
        tablesBuilder.writeMinimalTables(writer, listOf("0", "C-COL-COL1", "C-TTL-FRAM"))  // ← ビルダー版
        writeEntities()
        objectsBuilder.writeCompleteObjects(writer, paper, printScale, "32")  // ← ビルダー版

    }

    /** Helper: open [file] with Shift_JIS (CP932) encoding, write DXF, close writer automatically */
    fun saveTo(file: java.io.File, charset: Charset = Charset.forName("Shift_JIS")) {
        BufferedWriter(OutputStreamWriter(FileOutputStream(file), charset)).use { bw ->
            this.writer = bw
            save()
        }
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

        val (dimverticalA, dimverticalB, dimverticalC) = getDimTriple(tri)

        var (la, lb, lc) = stringTriple(tri)

        val textSize: Float = textscale_
        val textSize2: Float = textscale_

        // TriLines
        writeTriangleLines(tri,WHITE)

        if(isDebug){
            la += "A$dimverticalA"
            lb += "B$dimverticalB"
            lc += "C$dimverticalC"
        }

        // DimTexts
        if( tri.mynumber == 1 || tri.connectionSide > 2)
            writeTextDimension(dimverticalA, la, tri.dimpoint.a, pab.calcDimAngle(pca))
        writeTextDimension(dimverticalB, lb, tri.dimpoint.b, pbc.calcDimAngle(pab))
        writeTextDimension(dimverticalC, lc, tri.dimpoint.c, pca.calcDimAngle(pbc))

        //DimFlags
        writeDimFlags(tri, WHITE)

        // 番号
        writePointNumber(tri, textSize,BLUE,1,2, textSize*0.85f )

        // 測点
        if(tri.myName_() != "") {
            writeSokuten(tri, trilist_.sokutenListVector, textSize2, BLUE, 1, 1)
        }
    }

    private fun getDimTriple(
        tri: Triangle
    ): Triple<Int, Int, Int> {
        val dimverticalA = tri.dimOnPath[0].verticalDxf()//verticalFromBaseline(tri.dim.vertical.a, pca, pab)
        val dimverticalB = tri.dimOnPath[1].verticalDxf()//verticalFromBaseline(tri.dim.vertical.b, pab, pbc)
        val dimverticalC = tri.dimOnPath[2].verticalDxf()//verticalFromBaseline(tri.dim.vertical.c, pbc, pca)
        //val dimverticalD = verticalFromBaseline(tri.dim.vertical.a, pca, pab)
        //val dimverticalE = verticalFromBaseline(tri.dim.vertical.b, pab, pbc)
        //val dimverticalF = verticalFromBaseline(tri.dim.vertical.c, pbc, pca)
        return Triple(dimverticalA, dimverticalB, dimverticalC)
        //return Triple(dimverticalD, dimverticalE, dimverticalF)
    }

    private fun writeTextDimension(verticalAlign: Int, len: String, p1: com.example.trilib.PointXY, angle: Float){
        writeTextHV(len, p1, WHITE, textscale_, 1, verticalAlign, angle, 1f)
    }

    override fun writeTextHV(
        text: String,
        point: com.example.trilib.PointXY,
        color: Int,
        textsize: Float,
        alignH: Int,
        alignV: Int,
        angle: Float,
        scale: Float
    ){
        var x = point.x *unitscale_ //- ( alignV * 30 - 60 )// a offset when V is 3 to 1. V is 1 to -1.
        var y = point.y *unitscale_ //- ( alignV * 30 - 60 )
        val ts = textsize * unitscale_

        // its not effective..?
        if( alignV == 3) {
            if( angle < 0 ){
                x -= 50f
                y -= 50f
            }
        }

        val handle = nextHandle()

        writer.write("""
            0
            TEXT
            5
            $handle
            330
            36
            100
            AcDbEntity
            8
            $activeLayer
            62
            $color
            100
            AcDbText
            10
            $x
            20
            $y
            30
            0.0
            40
            ${ts.formattedString(0)}
            1
            $text
            41
            1.00
            7
            DimStandard
            72
            $alignH
            11
            $x
            21
            $y
            31
            0.0
            50
            $angle
            51
            0.0
            100
            AcDbText
            73
            $alignV
        """.trimIndent())
        writer.newLine()
    }

    override fun writeLine(p1: com.example.trilib.PointXY, p2: com.example.trilib.PointXY, color: Int, scale: Float ) {
        val ax = p1.x *unitscale_
        val ay = p1.y *unitscale_
        val bx = p2.x *unitscale_
        val by = p2.y *unitscale_

        val handle = nextHandle()

        writer.write(
                """
                0
                LINE
                5
                $handle
                330
                36
                100
                AcDbEntity
                8
                $activeLayer
                100
                AcDbLine
                370
                -3
                10
                $ax
                20
                $ay
                30
                0.0
                11
                $bx
                21
                $by
                31
                0.0
                62
                $color
            """.trimIndent()
        )
        writer.newLine()
    }

    override fun writeCircle(point: com.example.trilib.PointXY, size: Float, color: Int, scale: Float){
        val x = point.x *unitscale_
        val y = point.y *unitscale_
        val s = size * unitscale_

        val handle = nextHandle()

        writer.write("""
            0
            CIRCLE
            5
            $handle
            330
            36
            100
            AcDbEntity
            8
            $activeLayer
            62
            $color
            370
            13
            100
            AcDbCircle
            10
            $x
            20
            $y
            30
            0.0
            40
            $s 
        """.trimIndent())
        writer.newLine()
    }

    override fun writeTextAndLine(
        st: String,
        p1: com.example.trilib.PointXY,
        p2: com.example.trilib.PointXY,
        textsize: Float,
        scale: Float
    ){
        writeTextHV(st, p1.plus(textsize,textsize*0.2f), 1, textsize, 0, 1, 0f, 1f)
        writeLine( p1, p2, 1)
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
                pointFlag.plus(infoStrLength + textOffsetX,0f),
                textSize,
                1f
            )
        } else {                     //ptFlag is LEFT from pt
            writeLine( point, pointFlag, RED)
            writeTextAndLine(
                ded.infoStr,
                pointFlag.plus(-ded.getInfo().length*textSize - textOffsetX,0f),
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
        ded.setBox( 1f )
        writeLine( ded.pLTop, ded.pLBtm, color)
        writeLine( ded.pLTop, ded.pRTop, color)
        writeLine( ded.pRTop, ded.pRBtm, color)
        writeLine( ded.pLBtm, ded.pRBtm, color)
    }

    override fun writeEntities(){

        // 最初に書く
        writer.write("""
            0
            SECTION
            2
            ENTITIES
        """.trimIndent())
        writer.newLine()

        val myDXFTriList = trilist_.clone()
        val myDXFDedList = dedlist_.clone()

        // Ｙ軸方向反転、かつビュースケールで割り戻して大きさをtriListと揃える。
        myDXFDedList.scale(com.example.trilib.PointXY(0f, 0f),1/ viewscale_,-1/ viewscale_)

        val center = com.example.trilib.PointXY(
            21f * printscale_,
            14.85f * printscale_
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
        writeDrawingFrame(textsize = textscale_)
        writeTopTitle(textsize = textscale_)
        unitscale_ = 1000f

        if(isReverse_) {
            trilistNumbered = trilistNumbered.reverse()
        }
        // calcSheet
        writeCalcSheet(1f, textscale_, trilistNumbered, myDXFDedList )

        //一番最後に書く
        writer.write("""
            0
            ENDSEC
        """.trimIndent())
        writer.newLine()

    }


    fun writeSpritTrilist(myDXFTriList: TriangleList, trilistNumbered: TriangleList, color: Array<Int>, sixtytwo:Array<Int> ){
        val spritByColors = myDXFTriList.spritByColors()
        for( index in 0 until spritByColors.size ){

            val outlineLists = spritByColors[index].outlineList_ //myDXFTriList.outlineList() //ArrayList<PointXY>()

            if (outlineLists != null) {
                for( index2 in 0 until outlineLists.size){

                    if( outlineLists[index2].size > 0 ){
                        writeDXFTriHatch(writer, outlineLists[index2], color[index], sixtytwo[index] )
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
                        writeDXFTriOutlines( writer, outlineLists[index2] )
                    }
                }
            }
        }
    }

    private fun writeDXFTriHatch(
        wrtr: BufferedWriter,
        array: ArrayList<com.example.trilib.PointXY>,
        color: Int,
        sixtytwo: Int
    ){
        val handle = nextHandle()

        wrtr.write("""
            0
            HATCH
            5
            $handle
            100
            AcDbEntity
            8
            C-COL-COL1
            62
            $sixtytwo
            420
            $color
            370
            -3
            100
            AcDbHatch
            10
            0.0
            20
            0.0
            30
            0.0
            2
            SOLID
            70
            1
            71
            0
            91
            1
            92
            1
            93
            ${array.size}
            
        """.trimIndent())

        for( index in 0 until array.size){
            if( index + 1 < array.size ){
                wrtr.write("""
                72
                1
                10
                ${array[index].x*unitscale_}
                20
                ${( array[index].y*unitscale_ )}                
                11
                ${( array[index + 1].x*unitscale_ )}
                21
                ${( array[index + 1].y*unitscale_ )}                
            """.trimIndent())
                wrtr.newLine()
            }
        }

        wrtr.write("""
            97
            0
            75
            0
            76
            1
            98
            1
            10
            0.0
            20
            0.0
            
        """.trimIndent())

    }

    private fun writeDXFTriOutlines(wrtr: BufferedWriter, array: ArrayList<com.example.trilib.PointXY> ) {

        //writeDXFTriHatch( wrtr, array )

        val handle = nextHandle()

        wrtr.write("""
            0
            LWPOLYLINE
            5
            $handle
            100
            AcDbEntity
            8
            C-COL-COL1
            100
            AcDbPolyline
            370
            13
            90
            ${array.size}
            70
            1
            43
            0.0
        """.trimIndent())
        wrtr.newLine()

        for( index in 0 until array.size){
            wrtr.write("""
                10
                ${( array[index].x*unitscale_ )}
                20
                ${( array[index].y*unitscale_ )}                
            """.trimIndent())
            wrtr.newLine()
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

    private fun nextHandle(): String = handleGen.new()

}
