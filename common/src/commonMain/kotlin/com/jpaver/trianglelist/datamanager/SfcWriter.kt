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
    // 単位モデル統一 (段4): SFC も DXF と同じ「モデル座標は実寸、単位変換は primitive で ×unitscale」
    // 流儀。textscale も DXF と同一 (getPrintTextScale)。旧 ×1.2 / ×unitscale は偶発的な澱なので除去。
    override var textscale_ = trilist_.getPrintTextScale( 1f , "dxf")
    // 用紙寸法は基底の paper フィールドが唯一の出所。sizeX_/sizeY_/centerX_/centerY_ の
    // 旧 override は footer の用紙宣言以外で使われていなかったので削除し、footer は paper を直接読む。
    var circleSize = textscale_ * 0.8f

    override var COLOR_PINK   = 6
    override var COLOR_ORANGE = 2
    override var COLOR_YELLOW = 5
    override var COLOR_GREEN  = 3
    override var COLOR_SKY    = 7



    override fun writeEntities(){
        // サークルサイズの更新
        circleSize = textscale_ * 0.8f

        // printscale
        printscale_ = trilist_.getPrintScale(1f)

        // 単位モデル統一 (段4): モデル座標は実寸のまま持ち、mm への変換は primitive (writeLine 等) で
        // ×unitscale する DXF 流儀に揃えた。よって trilist は scale せず、dedlist は viewscale で割り戻す
        // のみ (Y 反転込み、×unitscale は付けない)。DxfFileWriter.writeEntities と同形。
        dedlist_.scale(com.example.trilib.PointXY(0f, 0f), 1f/viewscale_, -1f/viewscale_ )

        //シートの中心へ動かす (用紙中央、実寸。paperWcm/Hcm は paper 由来)
        val center = com.example.trilib.PointXY(
            paperWcm / 2f * printscale_,
            paperHcm / 2f * printscale_
        )

        val tricenter = trilist_.center

        moveCenterTri(center,tricenter)

        // 開始番号指定
        var trilistNumbered = trilist_.numbered( startTriNumber_ )
        if( isReverse_ == true ) {
            trilistNumbered = trilistNumbered.resetNumReverse()
            dedlist_ = dedlist_.reverse()
        }

        // 1. 各図形のハッチングを描画 (Z-order のため先行描画)
        drawAllHatches(trilistNumbered, traps_, trapTris_)

        // 2. 本体の描画 (線・文字・寸法・控除)
        drawAllMainEntities(trilistNumbered, traps_, trapTris_, dedlist_)

        // 図面枠: 縮尺は枠の scale 引数 (printscale) だけに乗せる。primitive が ×unitscale するので
        // 枠座標は printscale 倍で渡す (DXF が unitscale*=printscale でやっているのと同じ最終倍率)。
        val shapes = trilist_.trilist + traps_ + trapTris_
        val deductions = dedlist_.dedlist_
        calculateAndSetZumenAreaText(shapes, deductions)

        writeDrawingFrame(printscale_, textscale_)
        // 図面上中央のタイトル (図面名・路線名 + 下線)。DXF は DxfFileWriter:225 で writeTopTitle を
        // 呼ぶが SFC は呼んでおらず、SFC だけ上中央タイトルが欠落していた (user 指摘 2026-06-12)。
        // 枠と同じ scale=printscale_ 規約で揃える (基底の同一メソッドを共有 = 一元管理)。
        writeTopTitle(printscale_, textscale_)

        // calcSheet (DXF と同形: scale=1・textsize=textscale_。単位は primitive が吸収)
        if( isReverse_ == true ) {
            trilistNumbered = trilistNumbered.reverse()
        }
        writeCalcSheet(1f, textscale_, trilistNumbered, dedlist_ )

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

    // writeDeduction / writeDedRect は基底 DrawingFileWriter に集約 (段4、DXF を正)。
    // SFC は単位を primitive で吸収する流儀に揃えたので基底版がそのまま使える。

    override fun writeTextAndLine(
        st: String,
        p1: com.example.trilib.PointXY,
        p2: com.example.trilib.PointXY,
        textsize: Float,
        scale: Float
    ){
        // DXF (dxfEntity.writeTextAndLine) と同ロジック: テキストを (textsize, textsize*0.2) ずらし
        // alignH=0/alignV=1 で書き、下線 p1→p2 を引く。色は RED。旧 SFC の (200,100) 固定オフセットは
        // 偶発的な澱なので廃止。
        writeTextHV(st, p1.plus(textsize.toDouble(), textsize.toDouble() * 0.2), RED, textsize, 0, 1, 0.0, scale)
        writeLine(p1, p2, RED, scale)
    }

    // writeTriangle / layoutTriple / writeDimFlagsFromLayout / writeSokutenFromLayout は
    // 基底 DrawingFileWriter に集約 (段2)。寸法/測点文字の縦揃えは verticalDxf に統一され、
    // SFC 固有だった alignVByVector (2/8 体系) は廃止 = DXF と同じ揃えになる。



    override fun writeCircle(point: com.example.trilib.PointXY, size: Float, color: Int, scale: Float){
        // 単位変換は primitive で 1 回 (×unitscale)。モデル座標は実寸で受ける (DXF dxfEntity と同形)
        val x = point.x * unitscale_
        val y = point.y * unitscale_
        val s = size * unitscale_
        adas( "circle_feature('1','${color}','1','1','${x}','${y}','${s}')" )
        //レイヤ、２番目がプリセット色指定(８が白、４が青、２が赤)、続いて、線種、線幅、座標XYと、半径
    }

    override fun writeLine(p1: com.example.trilib.PointXY, p2: com.example.trilib.PointXY, color: Int, scale: Float ){
        val ax = p1.x * unitscale_
        val ay = p1.y * unitscale_
        val bx = p2.x * unitscale_
        val by = p2.y * unitscale_
        adas("line_feature('1','${color}','1','1','${ax}','${ay}','${bx}','${by}')" )
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
        val x = point.x * unitscale_
        val y = point.y * unitscale_
        val ts = tsy * unitscale_
        val tsxb = sjisByteLength(text) * ts * 0.5f
        var positiveangle = angle
        if( angle < 0 ) positiveangle = 360 + angle
        adas("text_string_feature('1','${color}','1',\'${text}\','${x}','${y}','${ts}','${tsxb}','0.00','${positiveangle}','0.00','${align}','1')" )
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

    override fun writeHatch(points: List<com.example.trilib.PointXY>, color: Int, colorIdx: Int, scale: Float) {
        val colors = arrayOf(COLOR_PINK, COLOR_ORANGE, COLOR_YELLOW, COLOR_GREEN, COLOR_SKY)
        val c = colors[colorIdx.coerceIn(0, 4)]
        val coordsStr = points.joinToString(",") { 
            val px = it.x * unitscale_
            val py = it.y * unitscale_
            "'$px','$py'"
        }
        adas("face_fill_feature('1','$c','1','1','1','1','${points.size}',$coordsStr)")
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