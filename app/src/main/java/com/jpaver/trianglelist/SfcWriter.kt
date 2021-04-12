package com.jpaver.trianglelist

import android.util.Log
import java.io.BufferedOutputStream
import java.nio.charset.Charset
import java.util.*

class SfcWriter( trilist: TriangleList, dedlist: DeductionList, outputStream: BufferedOutputStream, filename: String, startnum: Int ): DrawingFileWriter() {

    var trilist_: TriangleList = trilist.clone().numbered( setStartNumber( startnum ) )
    val dedlist_: DeductionList = dedlist.clone()
    val outputStream_ = outputStream
    val filename_ = filename
    var strPool_ = "" // ここにどんどん書き込む
    var assembryNumber_ = 10
    val drawscale_ = 1000f
    val viewscale_ = 47.6f
    var sheetscale_ = trilist_.getPrintScale(1f)
    val textscale_ = trilist_.getPrintTextScale( 1f , "dxf") * drawscale_
    var sizeX_ = 420 * sheetscale_
    var sizeY_ = 297 * sheetscale_
    val centerX_ = sizeX_ * 0.5f
    val centerY_ = sizeY_ * 0.5f
    val charset = Charset.forName("SJIS")
    val tsxhalf_ = 0.5f*textscale_

    fun compare(byteArray: ByteArray ): Boolean{
        return Arrays.equals( byteArray, strPool_.toByteArray() )
    }

    fun crcn(){
        strPool_ +="\r\n"
    }

    fun apnd(str: String ){
        strPool_ += str
        crcn()
    }

    fun adas(str: String ){
        strPool_ += "/*SXF"
        crcn()
        strPool_ += "#${assembryNumber_} = ${str}"
        crcn()
        strPool_ += "SXF*/"
        crcn()

        assembryNumber_ += 10
    }


    fun writeHeader() {
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

    fun writeList(){
        trilist_.scale( PointXY(0f,0f), drawscale_ )
        dedlist_.scale( PointXY(0f,0f), drawscale_/viewscale_, -drawscale_/viewscale_ ) // アプリの画面に合わせて拡大されているのを戻し、Y軸も反転

        //シートの中心へ動かす
        val center = PointXY(21000f*sheetscale_, 14850f*sheetscale_ )
        val tricenter = trilist_.center
        dedlist_.move(PointXY(center.x-tricenter.x,center.y-tricenter.y))
        trilist_.move(PointXY(center.x-tricenter.x,center.y-tricenter.y))

        // 開始番号指定
        //trilist_ = trilist_.numbered( startTriNumber_ )

        for ( trinumber in 1 .. trilist_.size() ){
            writeTriangle( trinumber )
        }

        // deduction
        for (dednumber in 1 .. dedlist_.size()) {
            writeDeduction( dednumber )
        }

        writeFrame()
    }

    fun writeDeduction( dednumber: Int ){

        val ded = dedlist_.get( dednumber )
        val textsize: Float = textscale_
        val infoStrLength = ded.getInfo().length*textsize+100f
        val point = ded.point
        val pointFlag = ded.pointFlag
        var textOffsetX = 200f
        //if( ded.type == "Box" ) textOffsetX = -500f

        if(point.x <= pointFlag.x) {  //ptFlag is RIGHT from pt
            writeLine( 2, point, pointFlag )
            writeLine( 2, pointFlag.plus(infoStrLength,0f), pointFlag )
            writeText( 2, ded.getInfo(), pointFlag.plus( textOffsetX,0f ), textsize, textsize, 0f, 1)
        } else {                     //ptFlag is LEFT from pt
            writeLine( 2, point, pointFlag )
            writeLine( 2, pointFlag.plus(-infoStrLength,0f), pointFlag )
            writeText( 2, ded.getInfo(), pointFlag.plus(-infoStrLength + textOffsetX,0f), textsize, textsize, 0f, 1)
        }

        if(ded.type == "Circle") writeCircle( 2, point, ded.lengthX/2*1000f )
        if(ded.type == "Box")    writeDedRect( 2, ded )


    }

    fun writeDedRect( color: Int, ded: Deduction ){
        ded.shapeAngle_ = -ded.shapeAngle_ // 逆回転
        ded.setBox( 1000f )
        writeLine( color, ded.plt, ded.plb )
        writeLine( color, ded.plt, ded.prt )
        writeLine( color, ded.prt, ded.prb )
        writeLine( color, ded.plb, ded.prb )
    }

    fun alignVByVector(num: Int, p1: PointXY, p2: PointXY ): Int{
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
            if(  isVectorRight <  0 ) return 8
        }

        // 基本は内側。正の時は下2が内、負の時は上8が内。
        if(  isVectorRight >= 0 ) return 8
        if(  isVectorRight <  0 ) return 2


        return num
    }

    fun writeTriangle( trinumber: Int){
        val ts = textscale_
        val tri = trilist_.get( trinumber )
        val pca = tri.pointCA_
        val pab = tri.pointAB_
        val pbc = tri.pointBC_
        var la = tri.lengthAforce_.formattedString(2)
        var lb = tri.lengthBforce_.formattedString(2)
        var lc = tri.lengthCforce_.formattedString(2)
        var dimA = alignVByVector(tri.myDimAlignA_, pca, pab)
        val dimB = alignVByVector(tri.myDimAlignB_, pab, pbc)//flip(tri.myDimAlignB_, tri.dimAngleB_ )
        val dimC = alignVByVector(tri.myDimAlignC_, pbc, pca)//flip(tri.myDimAlignC_, tri.dimAngleC_ )

        writeLine( 8, pca, pab )
        writeLine( 8, pab, pbc )
        writeLine( 8, pbc, pca )

        // DimTexts
        if(tri.getMyNumber_()==1 || tri.getParentBC() > 2)
            writeText( 8, la, tri.dimPointA_, ts, ts, pab.calcDimAngle(pca), dimA )
        writeText( 8, lb, tri.dimPointB_, ts, ts, pbc.calcDimAngle(pab), dimB )
        writeText( 8, lc, tri.dimPointC_, ts, ts, pca.calcDimAngle(pbc), dimC )

        // 番号
        val pn = tri.getPointNumberAutoAligned_()
        val pc = tri.pointCenter_
        val circleSize = ts * 0.7f
        // 本体
        writeCircle( 4, pn, circleSize )
        writeText( 4, tri.getMyNumber_().toString(), tri.getPointNumberAutoAligned_(), ts, ts, 0f, 5 )

        //引き出し矢印線の描画
        if( tri.isCollide(tri.pointNumber_) == false ){
            val pcOffsetToN = pc.offset(pn, circleSize)
            val pnOffsetToC = pn.offset(pc, circleSize)
            val arrowTail = pcOffsetToN.offset(pn, pcOffsetToN.lengthTo(pnOffsetToC) * 0.7f).rotate(pcOffsetToN, 5f)

            writeLine( 4, pcOffsetToN, pnOffsetToC )
            writeLine( 4, pcOffsetToN, arrowTail )
        }

    }

    fun writeFrame(){
        writeRect( PointXY( centerX_*100,centerY_*100 ),sizeX_*95,sizeY_*95, 8 )
    }

    fun writeRect( point: PointXY, sizeX: Float, sizeY: Float, color: Int ){
        val sizex: Float = sizeX/2
        val sizey: Float = sizeY/2
        writeLine( color, point.plus(-sizex, -sizey), point.plus(sizex, -sizey) )
        writeLine( color, point.plus(-sizex, sizey ), point.plus(sizex, sizey ) )
        writeLine( color, point.plus(-sizex, -sizey), point.plus(-sizex, sizey) )
        writeLine( color, point.plus( sizex, -sizey), point.plus(sizex, sizey ) )
    }

    fun writeCircle( color: Int = 8, point: PointXY, radius: Float ){
        adas( "circle_feature('1','${color}','1','1','${point.x}','${point.y}','${radius}')" )
        //レイヤ、２番目がプリセット色指定(８が白、４が青、２が赤)、続いて、線種、線幅、座標XYと、半径
    }

    fun writeLine( color: Int = 8, point1: PointXY, point2: PointXY ){
        adas("line_feature('1','${color}','1','1','${point1.x}','${point1.y}','${point2.x}','${point2.y}')" )
        //レイヤ、色、線種、線幅、始点ＸＹ、終点ＸＹ SXF*/
    }

    fun writeText( color: Int = 8, str: String, point: PointXY, tsy: Float,  tsx: Float, angle : Float = 0.0f, align: Int = 2 ){
        val tsxb = str.toByteArray(charset).size*tsxhalf_
        adas("text_string_feature('1','${color}','1',\'${str}\','${point.x}','${point.y}','${tsy}','${tsxb}','0.00','${angle}','0.00','${align}','1')" )
        //レイヤ、色、フォントコード、文字内容、座標ＸＹ、大きさ縦横、文字間隔、文字回転角、スラント角、配置９方向で指定２が上センター８が下センター、１が左、書き出し方向　SXF*/
    }

    fun writeFooter(){
        adas( "sfig_org_feature(\\'ﾍﾞｰｽ\\','2')" )
        adas( "sfig_locate_feature('0',\\'ﾍﾞｰｽ\\','0.000000','0.000000','0.00000000000000','0.01000000000000','0.01000000000000')" )
        adas( "drawing_sheet_feature(\\'\\','3','1','${sizeX_}','${sizeY_}')" )
        adas( "layer_feature(\'通常\','1')" )
        apnd( "ENDSEC;" )
        apnd( "END-ISO-10303-21;" )

        // fileStr_ to outputStream
        val charset = Charset.forName("SJIS")
        //val strUtf = "012ABC亜漢字表示"
        val strSjis = strPool_.toByteArray(charset)
        for(i in 0..strSjis.size-1){
            var c : Byte = strSjis[i]
            var s = "%X".format(c)
            Log.d("Sample","$s")
        }

        outputStream_.write( strSjis )
        return
    }

}