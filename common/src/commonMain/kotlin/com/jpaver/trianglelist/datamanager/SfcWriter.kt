package com.jpaver.trianglelist.datamanager

import com.jpaver.trianglelist.editmodel.Deduction
import com.jpaver.trianglelist.editmodel.DeductionList
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList

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

    // writeTriangle / layoutTriple / writeDimFlagsFromLayout / writeSokutenFromLayout は
    // 基底 DrawingFileWriter に集約 (段2)。寸法/測点文字の縦揃えは verticalDxf に統一され、
    // SFC 固有だった alignVByVector (2/8 体系) は廃止 = DXF と同じ揃えになる。



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