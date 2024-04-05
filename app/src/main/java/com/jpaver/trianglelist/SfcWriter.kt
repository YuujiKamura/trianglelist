package com.jpaver.trianglelist

import java.io.BufferedOutputStream
import java.nio.charset.Charset

class SfcWriter(trilist: TriangleList, dedlist: DeductionList, outputStream: BufferedOutputStream, filename: String, startnum: Int ): DrawingFileWriter() {

    override var trilist_: TriangleList = trilist.clone().numbered( setStartNumber( startnum ) )
    override var dedlist_: DeductionList = dedlist.clone()
    val outputStream_ = outputStream
    val filename_ = filename
    var strPool_ = "" // ここにどんどん書き込む
    var assembryNumber_ = 10

    override var textscale_ = trilist_.getPrintTextScale( 1f , "dxf") * 1.2f * unitscale_
    override var sizeX_ = 420 * printscale_
    override var sizeY_ = 297 * printscale_
    override var centerX_ = sizeX_ * 0.5f
    override var centerY_ = sizeY_ * 0.5f
    val charset = Charset.forName("SJIS")
    var circleSize = textscale_ * 0.8f

    fun apnd(str: String ){
        strPool_ += "${str}\r\n"
        //crcn()
    }

    fun adas(str: String ){
        strPool_ += "/*SXF\r\n#${assembryNumber_} = ${str}\r\nSXF*/\r\n"
        //crcn()
        //strPool_ += "#${assembryNumber_} = ${str}"
        //crcn()
        //strPool_ += "SXF*/"
        //crcn()

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

    override fun writeEntities(){
        // サークルサイズの更新
        circleSize = textscale_ * 0.8f

        // printscale
        printscale_ = trilist_.getPrintScale(1f)

        trilist_.scale(PointXY(0f, 0f), unitscale_ )
        dedlist_.scale(PointXY(0f, 0f), unitscale_/viewscale_, -unitscale_/viewscale_ )
        // アプリの画面に合わせて拡大されているのを戻し、Y軸も反転

        //シートの中心へ動かす
        val center = PointXY(
            21000f * printscale_,
            14850f * printscale_
        )
        val tricenter = trilist_.center
        dedlist_.move(
            PointXY(
                center.x - tricenter.x,
                center.y - tricenter.y
            )
        )
        trilist_.move(
            PointXY(
                center.x - tricenter.x,
                center.y - tricenter.y
            )
        )

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
        trilistNumbered.scale(PointXY(0f, 0f), 1/unitscale_ )
        writeCalcSheet(1000f, textscale_/unitscale_, trilistNumbered, dedlist_ )

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
                pointFlag.plus( infoStrLength + textOffsetX,0f ),
                textscale_,
                1f
            )
//            writeLine( 2, pointFlag.plus(infoStrLength,0f), pointFlag )
//            writeText( 2, ded.info_, pointFlag.plus( textOffsetX,0f ), textscale_, 0f, 1)
        } else {                     //ptFlag is LEFT from pt
            writeLine(point, pointFlag, 2)
            writeTextAndLine(
                ded.infoStr,
                pointFlag.plus( -infoStrLength - textOffsetX,0f ),
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
        p1: PointXY,
        p2: PointXY,
        textsize: Float,
        scale: Float
    ){
        writeLine( p1, p2, 2, scale )
        writeText(st, p1.plus( 200f, 100f ), 2, textscale_, 1, 0f, scale )

        //writeDXFText(writer_, st, p1.plus(0.2f,0.1f), 1, textsize, 0)
        //writeLine(writer_, p1, p2, 1)
    }

    fun writeDedRect( color: Int, ded: Deduction){
        ded.shapeAngle = -ded.shapeAngle // 逆回転
        ded.setBox( 1000f )
        writeLine(ded.plt, ded.plb, color)
        writeLine(ded.plt, ded.prt, color)
        writeLine(ded.prt, ded.prb, color)
        writeLine(ded.plb, ded.prb, color)
    }

    fun alignVByVector(num: Int, p1: PointXY, p2: PointXY): Int{
        // 垂直方向の文字位置合わせタイプ(省略可能、既定 = 0): 整数コード(ビットコードではありません):
        // 0 = 基準線、2 = 下、5 = 中央、8 = 上
        // ベクトルの方向でB,Cを表現するなら
        // x軸の方向で正負を表す。正の時は下1が内、負の時は上3が内。

        // inner:3, outer:1 in Tri
        // ただし、Triangleで外(1)を指定しているときは、そちらを優先したい。
        // 正の時は上8が外、負の時は下2が外。
        val isVectorRight = p1.vectorTo(p2).side()
        if( num == 1 ){
            if(  isVectorRight >= 0 ) return 2
            return 8
        }

        // 基本は内側。正の時は下2が内、負の時は上8が内。
        if(  isVectorRight >= 0 ) return 8
        return 2

    }

    override fun writeTriangle( tri: Triangle){
        val ts = textscale_
        //val tri = trilist_.get( trinumber )
        val pca = tri.pointCA_
        val pab = tri.pointAB
        val pbc = tri.pointBC
        val la = tri.lengthAforce_.formattedString(2)
        val lb = tri.lengthBforce_.formattedString(2)
        val lc = tri.lengthCforce_.formattedString(2)
        val dimA = alignVByVector(tri.dimVerticalA, pca, pab)
        val dimB = alignVByVector(tri.dimVerticalB, pab, pbc)//flip(tri.myDimAlignB_, tri.dimAngleB_ )
        val dimC = alignVByVector(tri.dimVerticalC, pbc, pca)//flip(tri.myDimAlignC_, tri.dimAngleC_ )

        writeLine(pca, pab, 8)
        writeLine(pab, pbc, 8)
        writeLine(pbc, pca, 8)

        // DimTexts
        if(tri.mynumber ==1 || tri.connectionType > 2)
            writeText(la, tri.dimpoint[0], 8, ts, dimA, pab.calcDimAngle(pca), 1f)
        writeText(lb, tri.dimpoint[1], 8, ts, dimB, pbc.calcDimAngle(pab), 1f)
        writeText(lc, tri.dimpoint[2], 8, ts, dimC, pca.calcDimAngle(pbc), 1f)

        // 番号
        val pn = tri.pointnumber
        val pc = tri.pointcenter
        // 本体
        writeCircle(pn, circleSize, 4, 1f)
        writeText(tri.mynumber.toString(), tri.pointnumber, 4, ts, 5, 0f, 1f)

        //引き出し矢印線の描画
        if( tri.isCollide(tri.pointnumber) == false ){
            val pcOffsetToN = pc.offset(pn, circleSize)
            val pnOffsetToC = pn.offset(pc, circleSize)
            val arrowTail = pcOffsetToN.offset(pn, pcOffsetToN.lengthTo(pnOffsetToC) * 0.7f).rotate(pcOffsetToN, 5f)

            writeLine(pcOffsetToN, pnOffsetToC, 4)
            writeLine(pcOffsetToN, arrowTail, 4)
        }

        // 測点
        if(tri.name != "") {
            val noffset = -250f
            val nlength = -tri.name.length*500f+noffset
            val nhalflength = nlength*0.5f+noffset
            val slv = trilist_.sokutenListVector
            var align = 2
            if( slv < 0 ) align = 8

            writeText(
                tri.name,
                pab.offset( pca, nhalflength ),
                4,
                ts,
                align,
                pab.calcSokAngle( pca, slv ),
                1f
            )
            writeLine(pab.offset( pca, nlength ), pab.offset (pca, noffset ), 4)
        }

    }

    override fun writeCircle(point: PointXY, size: Float, color: Int, scale: Float){
        adas( "circle_feature('1','${color}','1','1','${point.x}','${point.y}','${size}')" )
        //レイヤ、２番目がプリセット色指定(８が白、４が青、２が赤)、続いて、線種、線幅、座標XYと、半径
    }

    override fun writeLine(p1: PointXY, p2: PointXY, color: Int, scale: Float ){
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
    override fun writeText(
        text: String,
        point: PointXY,
        color: Int,
        textsize: Float,
        alignH: Int,
        alignV: Int,
        angle: Float,
        scale: Float
    ) {
        val aH = alignH - 1
        var aV = alignV
        if( alignV == 1 ) aV = - 3
        if( alignV == 2 ) aV = 0
        val aligntenkey = 5 + aH + aV

        writeText(text, point, color, textsize, aligntenkey, angle, 1f)

    }

    override fun writeText(
        text: String,
        point: PointXY,
        color: Int,
        tsy: Float,
        align: Int,
        angle: Float,
        scale: Float
    ){
        val tsxb = text.toByteArray(charset).size * tsy * 0.5f
        var positiveangle = angle
        if( angle < 0 ) positiveangle = 360 + angle
        adas("text_string_feature('1','${color}','1',\'${text}\','${point.x}','${point.y}','${tsy}','${tsxb}','0.00','${positiveangle}','0.00','${align}','1')" )
        //レイヤ、色、フォントコード、文字内容、座標ＸＹ、大きさ縦横、文字間隔、文字回転角、スラント角、配置９方向で指定２が上センター８が下センター、１が左、書き出し方向　SXF*/
    }

    override fun writeFooter(){
        adas( "sfig_org_feature(\\'ﾍﾞｰｽ\\','2')" )
        adas( "sfig_locate_feature('0',\\'ﾍﾞｰｽ\\','0.000000','0.000000','0.00000000000000','0.01000000000000','0.01000000000000')" )
        adas( "drawing_sheet_feature(\\'\\','3','1','${sizeX_}','${sizeY_}')" )
        adas( "layer_feature(\'通常\','1')" )
        apnd( "ENDSEC;" )
        apnd( "END-ISO-10303-21;" )

        // fileStr_ to outputStream
        //val charset = Charset.forName("SJIS")
        //val strUtf = "012ABC亜漢字表示"
        val strSjis = strPool_.toByteArray(charset)
        /*
        for(i in 0..strSjis.size-1){
            var c : Byte = strSjis[i]
            var s = "%X".format(c)
            Log.d("Sample","$s")
        }*/

        outputStream_.write( strSjis )
        return
    }

}