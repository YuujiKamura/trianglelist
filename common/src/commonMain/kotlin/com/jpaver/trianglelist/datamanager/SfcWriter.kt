package com.jpaver.trianglelist.datamanager

import com.jpaver.trianglelist.editmodel.Deduction
import com.jpaver.trianglelist.editmodel.DeductionList
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import com.jpaver.trianglelist.label.DimensionLayout
import com.jpaver.trianglelist.label.DimensionPlacement

class SfcWriter(trilist: TriangleList, dedlist: DeductionList, filename: String, startnum: Int, viewscale:Float ): DrawingFileWriter() {

    override var trilist_: TriangleList = trilist.clone().numbered( setStartNumber( startnum ) )
    override var dedlist_: DeductionList = dedlist.clone()
    val filename_ = filename
    var strPool_ = "" // ここにどんどん書き込む
    var assembryNumber_ = 10

    override var viewscale_ = viewscale //初期化時に指定する
    override var textscale_ = trilist_.getPrintTextScale( 1f , "dxf") * 1.2f * unitscale_
    // 用紙寸法は基底の paper フィールドが唯一の出所。sizeX_/sizeY_/centerX_/centerY_ の
    // 旧 override は footer の用紙宣言以外で使われていなかったので削除し、footer は paper を直接読む。
    var circleSize = textscale_ * 0.8f


    override fun writeEntities(){
        // サークルサイズの更新
        circleSize = textscale_ * 0.8f

        // printscale
        printscale_ = trilist_.getPrintScale(1f)

        trilist_.scale(com.example.trilib.PointXY(0f, 0f), unitscale_ )
        dedlist_.scale(com.example.trilib.PointXY(0f, 0f), unitscale_/viewscale_, -unitscale_/viewscale_ )
        // アプリの画面に合わせて拡大されているのを戻し、Y軸も反転

        //シートの中心へ動かす
        val center = com.example.trilib.PointXY(
            21000f * printscale_,
            14850f * printscale_
        )

        val tricenter = trilist_.center

        moveCenterTri(center,tricenter)

        // 開始番号指定
        var trilistNumbered = trilist_.numbered( startTriNumber_ )
        if( isReverse_ == true ) {
            trilistNumbered = trilistNumbered.resetNumReverse()
            dedlist_ = dedlist_.reverse()
        }

        for ( trinumber in 1 .. trilistNumbered.size() ){
            writeTriangle( trilistNumbered.get(trinumber) )
        }

        // deduction
        for (dednumber in 1 .. dedlist_.size()) {
            writeDeduction( dedlist_.get(dednumber) )
        }

        //writeFrame( sheetscale_ * 0.1f * scale_, scale_, centerX_, centerY_, sizeX_, sizeY_ )
        writeDrawingFrame(unitscale_ * printscale_, 0.35f)

        // calcSheet
        if( isReverse_ == true ) {
            trilistNumbered = trilistNumbered.reverse()
        }
        trilistNumbered.scale(com.example.trilib.PointXY(0f, 0f), 1/unitscale_ )
        writeCalcSheet(1000f, textscale_/unitscale_, trilistNumbered, dedlist_ )

    }

    fun apnd(str: String ){
        strPool_ += "${str}\r\n"
    }

    fun adas(str: String ){
        strPool_ += "/*SXF\r\n#${assembryNumber_} = ${str}\r\nSXF*/\r\n"
        assembryNumber_ += 10
    }

    override fun save(){
        writeHeader()
        writeEntities()
        writeFooter()
    }

    override fun writeHeader() {
        apnd( "ISO-10303-21;" )
        apnd( "HEADER;" )
        apnd( "FILE_DESCRIPTION(('SCADEC level2 feature_mode')," )
        apnd( "'2;1');" )
        apnd( "FILE_NAME('${filename_}'," )
        apnd( "'2021-4-8T13:8:0'," )
        apnd( """('\X2\62C55F53\X0\'),""" )
        apnd( "('')," )
        apnd( "'SCADEC_API_Ver3.30${'$'}${'$'}2.0'," )
        apnd( "'TriangleList \\X2\\571F6728\\X0\\ 10'," )
        apnd( "'');" )
        apnd( "FILE_SCHEMA(('ASSOCIATIVE_DRAUGHTING'));" )
        apnd( "ENDSEC;" )
        apnd( "DATA;" )
        adas( "pre_defined_colour_feature(\'white\')" )
        adas( "pre_defined_colour_feature(\\'red\\')" )
        adas( "pre_defined_colour_feature(\\'blue\\')" )
        adas( "pre_defined_font_feature(\\'continuous\\')" )
        adas( "width_feature('0.130000')" )
        adas( "text_font_feature(\\'ＭＳ ゴシック\\')" )

        return
    }

    fun moveCenterTri(center: com.example.trilib.PointXY, tricenter: com.example.trilib.PointXY){
        dedlist_.move(
            com.example.trilib.PointXY(
                center.x - tricenter.x,
                center.y - tricenter.y
            )
        )
        trilist_.move(
            com.example.trilib.PointXY(
                center.x - tricenter.x,
                center.y - tricenter.y
            )
        )
    }

    override fun writeDeduction( ded: Deduction){

        //val ded = dedlist_.get( dednumber )
        //val textsize: Float = textscale_
        val infoStrLength = ded.infoStr.length*textscale_*0.7f
        val point = ded.point
        val pointFlag = ded.pointFlag
        val textOffsetX = 0f
        //if( ded.type == "Box" ) textOffsetX = -500f

        if(point.x <= pointFlag.x) {  //ptFlag is RIGHT from pt
            writeLine(point, pointFlag, 2)
            writeTextAndLine(
                ded.infoStr,
                pointFlag,
                pointFlag.plus( (infoStrLength + textOffsetX).toDouble(),0.0 ),
                textscale_,
                1f
            )
//            writeLine( 2, pointFlag.plus(infoStrLength,0f), pointFlag )
//            writeText( 2, ded.info_, pointFlag.plus( textOffsetX,0f ), textscale_, 0f, 1)
        } else {                     //ptFlag is LEFT from pt
            writeLine(point, pointFlag, 2)
            writeTextAndLine(
                ded.infoStr,
                pointFlag.plus( (-infoStrLength - textOffsetX).toDouble(),0.0 ),
                pointFlag,
                textscale_,
                1f
            )

            //writeLine( 2, pointFlag.plus(-infoStrLength,0f), pointFlag )
            //writeText( 2, ded.info_, pointFlag.plus(-infoStrLength + textOffsetX,0f), textscale_, 0f, 1)
        }

        if(ded.type == "Circle") writeCircle(point, ded.lengthX/2*1000f, 2, 1f)
        if(ded.type == "Box")    writeDedRect( 2, ded )

    }

    override fun writeTextAndLine(
        st: String,
        p1: com.example.trilib.PointXY,
        p2: com.example.trilib.PointXY,
        textsize: Float,
        scale: Float
    ){
        writeLine( p1, p2, 2, scale )
        writeTextA9(st, p1.plus( 200.0, 100.0 ), 2, textscale_, 1, 0.0, scale )

        //writeDXFText(writer_, st, p1.plus(0.2f,0.1f), 1, textsize, 0)
        //writeLine(writer_, p1, p2, 1)
    }

    fun writeDedRect( color: Int, ded: Deduction){
        ded.shapeAngle = -ded.shapeAngle // 逆回転
        ded.setBox( 1000.0 )
        writeLine(ded.pLTop, ded.pLBtm, color)
        writeLine(ded.pLTop, ded.pRTop, color)
        writeLine(ded.pRTop, ded.pRBtm, color)
        writeLine(ded.pLBtm, ded.pRBtm, color)
    }

    fun alignVByVector(num: Int, p1: com.example.trilib.PointXY, p2: com.example.trilib.PointXY): Int{
        // 垂直方向の文字位置合わせタイプ(省略可能、既定 = 0): 整数コード(ビットコードではありません):
        // 0 = 基準線、2 = 下、5 = 中央、8 = 上
        // ベクトルの方向でB,Cを表現するなら
        // x軸の方向で正負を表す。正の時は下1が内、負の時は上3が内。

        // inner:3, outer:1 in Tri
        // ただし、Triangleで外(1)を指定しているときは、そちらを優先したい。
        // 正の時は上8が外、負の時は下2が外。
        val LOWER = 2
        val UPPER = 8

        if( num == 1 ){
            if( p1.isVectorToRight(p2) ) return LOWER
            return 8
        }

        // 基本は内側。正の時は下2が内、負の時は上8が内。
        if(  p1.isVectorToRight(p2) ) return UPPER
        return 2

    }

    override fun writeTriangle( tri: Triangle){
        val ts = textscale_
        //val tri = trilist_.get( trinumber )
        val (pca, pab, pbc) = xyPointXYTriple(tri)

        val (la, lb, lc) = stringTriple(tri)

        // ADR 0003 Phase 2b: 寸法座標の出所を Triangle のキャッシュ (dimpoint/dimOnPath/pathS) から
        // common の式 DimensionLayout に切替。gapPaperMm=0 なので座標はキャッシュ由来と同値。
        // SFC 固有の垂直アライメント (alignVByVector の 2/8 体系) は据え置き = 座標の出所のみ替える。
        val (placeA, placeB, placeC) = layoutTriple(tri)

        val dimA = alignVByVector(tri.dim.vertical.a, pca, pab)
        val dimB = alignVByVector(tri.dim.vertical.b, pab, pbc)//flip(tri.myDimAlignB_, tri.dimAngleB_ )
        val dimC = alignVByVector(tri.dim.vertical.c, pbc, pca)//flip(tri.myDimAlignC_, tri.dimAngleC_ )

        // TriLines
        writeTriangleLines(tri,8)

        // DimTexts
        if(tri.mynumber ==1 || tri.connectionSide > 2)
            writeTextA9(la, placeA.dimpoint, 8, ts, dimA, pab.calcDimAngle(pca), 1f)
        writeTextA9(lb, placeB.dimpoint, 8, ts, dimB, pbc.calcDimAngle(pab), 1f)
        writeTextA9(lc, placeC.dimpoint, 8, ts, dimC, pca.calcDimAngle(pbc), 1f)

        //DimFlags
        writeDimFlagsFromLayout(tri, placeA, placeB, placeC, 8)

        // 番号
        writePointNumber( tri, ts, 4, 5,-1,ts*0.85f )

        // 測点
        if(tri.name != "") {
            val slv = trilist_.sokutenListVector
            var align = 8
            if( slv < 0 ) align = 2

            writeSokutenFromLayout(tri, slv, ts, 4, align, -1 )
        }
    }

    /**
     * 3 辺の寸法配置を DimensionLayout で計算する (Phase 2a DxfFileWriter.layoutTriple と同型)。
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



    override fun writeCircle(point: com.example.trilib.PointXY, size: Float, color: Int, scale: Float){
        adas( "circle_feature('1','${color}','1','1','${point.x}','${point.y}','${size}')" )
        //レイヤ、２番目がプリセット色指定(８が白、４が青、２が赤)、続いて、線種、線幅、座標XYと、半径
    }

    override fun writeLine(p1: com.example.trilib.PointXY, p2: com.example.trilib.PointXY, color: Int, scale: Float ){
        adas("line_feature('1','${color}','1','1','${p1.x}','${p1.y}','${p2.x}','${p2.y}')" )
        //レイヤ、色、線種、線幅、始点ＸＹ、終点ＸＹ SXF*/
    }

    //水平方向の位置合わせタイプ(省略可能、既定 = 0)整数コード(ビットコードではありません):
    //0 = 左寄せ、1= 中揃え、2 = 右寄せ
    //3 = 両端揃え(垂直位置合わせ = 0 の場合)
    //4 = 中心揃え(垂直位置合わせ = 0 の場合)
    //5 = フィット(垂直位置合わせ = 0 の場合)
    //垂直方向の文字位置合わせタイプ(省略可能、既定 = 0): 整数コード(ビットコードではありません):
    //0 = 基準線、1 = 下、2 = 中央、3 = 上
    override fun writeTextHV(
        text: String,
        point: com.example.trilib.PointXY,
        color: Int,
        textsize: Float,
        alignH: Int,
        alignV: Int,
        angle: Double,
        scale: Float
    ) {
        val aH = alignH - 1
        var aV = alignV
        if( alignV == 1 ) aV = - 3
        if( alignV == 2 ) aV = 0
        val aligntenkey = 5 + aH + aV

        writeTextA9(text, point, color, textsize, aligntenkey, angle, 1f)

    }

    override fun writeTextA9(
        text: String,
        point: com.example.trilib.PointXY,
        color: Int,
        tsy: Float,
        align: Int,
        angle: Double,
        scale: Float
    ){
        val tsxb = sjisByteLength(text) * tsy * 0.5f
        var positiveangle = angle
        if( angle < 0 ) positiveangle = 360 + angle
        adas("text_string_feature('1','${color}','1',\'${text}\','${point.x}','${point.y}','${tsy}','${tsxb}','0.00','${positiveangle}','0.00','${align}','1')" )
        //レイヤ、色、フォントコード、文字内容、座標ＸＹ、大きさ縦横、文字間隔、文字回転角、スラント角、配置９方向で指定２が上センター８が下センター、１が左、書き出し方向　SXF*/
    }

    override fun writeFooter(){
        adas( "sfig_org_feature(\\'ﾍﾞｰｽ\\','2')" )
        adas( "sfig_locate_feature('0',\\'ﾍﾞｰｽ\\','0.000000','0.000000','0.00000000000000','0.01000000000000','0.01000000000000')" )
        adas( "drawing_sheet_feature(\\'\\','3','1','${paper.width}','${paper.height}')" )
        adas( "layer_feature(\'通常\','1')" )
        apnd( "ENDSEC;" )
        apnd( "END-ISO-10303-21;" )
        return
    }

    /**
     * SFC 全文 (SJIS エンコード前の String) を組み立てて返す。
     * 書き出し先 (stream / file) は platform 側の拡張 (app の SfcWriter.saveTo) が担う。
     */
    fun buildSfcString(): String {
        save()
        return strPool_
    }

}

/**
 * SJIS (CP932) エンコード時のバイト幅。ASCII と半角カナ = 1 byte、他 = 2 byte。
 * 旧実装 text.toByteArray(SJIS).size の pure 置換 (golden が同値性を証明)。
 * sumOf を使わないのは整数リテラルの overload 解決曖昧さ (KT-46360) 回避。
 */
fun sjisByteLength(text: String): Int {
    var bytes = 0
    for (c in text) bytes += if (c.code <= 0x7F || c.code in 0xFF61..0xFF9F) 1 else 2
    return bytes
}