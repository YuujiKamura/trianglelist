package com.jpaver.trianglelist.web

import com.example.trilib.PointXY
import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.Deduction
import com.jpaver.trianglelist.editmodel.DeductionList
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import com.jpaver.trianglelist.label.DimensionLayout
import com.jpaver.trianglelist.label.DimensionPlacement
import com.jpaver.trianglelist.setLengthStr

/**
 * Web 段階1 (insight #60/#61): TriangleList → 描画プリミティブ JSON。
 *
 * 座標計算・寸法配置 (DimensionLayout)・番号サークル位置は全部こちら (Kotlin/common) で
 * 済ませ、JS 側は素朴に描くだけにする — UX parity の核を common に保つ。
 * 構成は app/datamanager/DxfFileWriter.writeTriangle (ADR 0003 Phase 2a) を踏襲:
 *   - 辺 3 本 (point[0]→pointAB→pointBC→point[0])
 *   - 寸法値: DimensionLayout.layout の dimpoint + calcDimAngle、A辺は
 *     「先頭三角形 or connectionSide>2」のときだけ (親との共有辺の重複防止)
 *   - 旗揚げ線: horizontal>2 のとき placement の pointA→pointB
 *   - 番号: pointnumber を中心にサークル (r=textSize*0.85) + 番号テキスト
 *     (DrawingFileWriter.writePointNumber と同じ。引出線は段階1 スコープ外)
 *
 * JSON 形式 (フラット配列、座標はモデル座標系 = y 上向き。y 反転は JS 側):
 *   {"type":"line","layer":"tri|dim","x1":..,"y1":..,"x2":..,"y2":..}
 *   {"type":"text","layer":"dim|num","text":"..","x":..,"y":..,"angle":deg,"size":..,"align":1|2|3}
 *     align は DXF 垂直コード: 1=点が文字の下端 (文字は点の上), 2=中央, 3=点が文字の上端
 *   {"type":"circle","layer":"num","cx":..,"cy":..,"r":..}
 *   {"type":"fill","layer":"fill","x1":..,"y1":..,"x2":..,"y2":..,"x3":..,"y3":..,"color":idx,"tri":N}
 *     color は CSV 列 10 (= Triangle.mycolor、既定 4) の index。実色は TS 側パレットが解決
 *
 * 段階2e (task #15) の識別フィールド: TS 側の当たり判定と W/H cycle のために
 *   - dim テキスト: 末尾に "tri":番号,"side":0|1|2,"h":水平値,"v":垂直値
 *     (h/v は現在実効値。TS は実効値から次の cycle/flip 値を計算する —
 *      vertical の初期値は接続構造依存で TS から推測できないため JSON で渡す)
 *   - 番号サークル/番号テキスト: 末尾に "tri":番号
 */
object WebPrimitiveRenderer {

    /** 寸法文字高さ (モデル単位)。サンプル図面 (辺 2-6m) で読める大きさ */
    const val DEFAULT_TEXT_SIZE = 0.25f

    /** アプリの textSize 初期値 (MyView.kt:117 `var textSize = 30f`)。
     *  CSV の TextSize 行 (writeCSV:2785) はこの単位 (view px) で入るので、比率で
     *  web のモデル単位文字高さへ写す。行が無い CSV は比率 1 = 従来出力 (golden 不変) */
    const val APP_DEFAULT_TEXT_SIZE = 30f

    /** CSV 文字列から JSON プリミティブまで一気通貫 (wasmJs facade の本体) */
    fun renderCsv(csv: String, scale: Float): String = renderCsv(csv, scale, "")

    /**
     * 段階2e (task #15): overrides 付き経路。parse (+setScale) 後に WebOverrides を
     * model へ適用してから render する。render 内の setDimPathTextSize は dim の
     * horizontal/vertical 値を読み直すだけ (値は保持される)、arrangePointNumbers は
     * isMovedByUser flag (PointNumberManager.kt:36) が override 適用済みの番号を保護する。
     * overridesJson が空なら renderCsv(csv, scale) と完全同一出力。
     */
    fun renderCsv(csv: String, scale: Float, overridesJson: String): String {
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        if (scale != 1f && scale > 0f) trilist.setScale(PointXY(0f, 0f), scale)
        WebOverrides.applyJson(trilist, overridesJson)
        // CSV の TextSize 行 (アプリ texplus/minus FAB ±5f の保存値) を比率で反映。
        // 0 以下は不正値として既定にフォールバック (アプリ adjustTextSize の下限クランプ相当)
        val sizeRatio = (doc.textSize?.takeIf { it > 0f } ?: APP_DEFAULT_TEXT_SIZE) / APP_DEFAULT_TEXT_SIZE
        val textSize = DEFAULT_TEXT_SIZE * sizeRatio * (if (scale > 0f) scale else 1f)
        return render(trilist, textSize, CsvCodec.buildDeductions(doc), if (scale > 0f) scale else 1f)
    }

    fun render(trilist: TriangleList, textSize: Float): String =
        render(trilist, textSize, DeductionList(), 1f)

    fun render(trilist: TriangleList, textSize: Float, dedlist: DeductionList, scale: Float): String {
        // MyView.setAllTextSize → trianglelist.setDimPathTextSize と同じ経路で dimHeight を配る
        trilist.setDimPathTextSize(textSize)
        trilist.arrangePointNumbers()

        val sb = StringBuilder()
        sb.append('[')
        var first = true
        fun item(json: String) {
            if (!first) sb.append(',')
            sb.append(json)
            first = false
        }

        // 塗り (アプリ MyView.drawEntities:572-576 の写し — 全三角形を mycolor で塗ってから
        // 線・文字を重ねる)。z-order をアプリと同じ「塗り全部 → 線・文字」にするため先に全部出す。
        // 色は index のまま出す (CSV 列 10 = Triangle.mycolor、既定 4)。実色はパレットを持つ
        // TS 側 (web/src/main.ts FILL_PALETTE) が解決する — アプリも index → 色配列の二段
        // (MainActivity.resColors / MyView.darkColors_) で同じ構図
        for (i in 1..trilist.size()) {
            val tri = trilist.getBy(i)
            item(
                """{"type":"fill","layer":"fill","x1":${tri.point[0].x},"y1":${tri.point[0].y},"x2":${tri.pointAB.x},"y2":${tri.pointAB.y},"x3":${tri.pointBC.x},"y3":${tri.pointBC.y},"color":${tri.mycolor},"tri":${tri.mynumber}}"""
            )
        }

        for (i in 1..trilist.size()) {
            val tri = trilist.getBy(i)
            val pca = tri.pointCA
            val pab = tri.pointAB
            val pbc = tri.pointBC

            // 辺 (DrawingFileWriter.writeTriangleLines と同じ順)
            item(line(tri.point[0], pab, "tri"))
            item(line(pab, pbc, "tri"))
            item(line(pbc, tri.point[0], "tri"))

            // 寸法配置 (DxfFileWriter.layoutTriple と同じ引数)
            val scale = tri.scaleFactor.toDouble()
            val dimheight = tri.dimHeight.toDouble()
            val placeA = DimensionLayout.layout(tri.pointAB, tri.point[0], tri.dim.vertical.a, tri.dim.horizontal.a, scale, dimheight, 0.0)
            val placeB = DimensionLayout.layout(tri.pointBC, tri.pointAB, tri.dim.vertical.b, tri.dim.horizontal.b, scale, dimheight, 0.0)
            val placeC = DimensionLayout.layout(tri.point[0], tri.pointBC, tri.dim.vertical.c, tri.dim.horizontal.c, scale, dimheight, 0.0)

            tri.setLengthStr()

            // A辺の寸法は先頭 or 再接続 (connectionSide>2) のときだけ (DxfFileWriter.writeTriangle と同じ)
            if (tri.mynumber == 1 || tri.connectionSide > 2) {
                item(dimText(tri.strLengthA, placeA, pab.calcDimAngle(pca), textSize, tri.mynumber, 0, tri.dim.horizontal.a, tri.dim.vertical.a))
            }
            item(dimText(tri.strLengthB, placeB, pbc.calcDimAngle(pab), textSize, tri.mynumber, 1, tri.dim.horizontal.b, tri.dim.vertical.b))
            item(dimText(tri.strLengthC, placeC, pca.calcDimAngle(pbc), textSize, tri.mynumber, 2, tri.dim.horizontal.c, tri.dim.vertical.c))

            // 旗揚げ線 (DxfFileWriter.writeDimFlagsFromLayout と同じ条件)
            if (tri.dim.horizontal.a > 2) item(line(placeA.pointA, placeA.pointB, "dim"))
            if (tri.dim.horizontal.b > 2) item(line(placeB.pointA, placeB.pointB, "dim"))
            if (tri.dim.horizontal.c > 2) item(line(placeC.pointA, placeC.pointB, "dim"))

            // 番号サークル + 番号 (DrawingFileWriter.writePointNumber と同じ r=ts*0.85、中央寄せ)
            val pn = tri.pointnumber
            item("""{"type":"circle","layer":"num","cx":${pn.x},"cy":${pn.y},"r":${textSize * 0.85f},"tri":${tri.mynumber}}""")
            item(text(tri.mynumber.toString(), pn, 0.0, textSize, 2, "num", ""","tri":${tri.mynumber}"""))
        }

        // 控除 (Deduction)。正 = DxfFileWriter.writeDeduction (DxfFileWriter.kt:240-282)。
        // ded はビュー空間 (y 下向き) で持つので、DxfFileWriter.writeEntities:299 と同じく
        // Y 反転 (web は viewscale=1) でモデル空間に揃えてから描く
        if (dedlist.size() > 0) {
            val dedModel = dedlist.clone()
            dedModel.scale(PointXY(0f, 0f), scale, -scale)
            for (n in 1..dedModel.size()) renderDeduction(dedModel.get(n), textSize, scale, ::item)
        }

        sb.append(']')
        return sb.toString()
    }

    /**
     * DxfFileWriter.writeDeduction:240-282 の写し (色 = 赤系は layer "ded" として TS 側で塗る):
     * - 旗揚げ線 point→pointFlag
     * - infoStr テキスト + 下線 (writeTextAndLine = DxfEntity.kt:172-181: テキストは
     *   p1 + (ts, ts*0.2)、左寄せ・下端基準、下線は p1→p2)。pointFlag の左右で分岐
     * - Circle: 中心 point、半径 lengthX/2 (writeCircle:270)
     * - Box: writeDedRect:274-281 = shapeAngle 逆回転で setBox し直して 4 辺
     */
    private fun renderDeduction(ded: Deduction, textSize: Float, scale: Float, item: (String) -> Unit) {
        val infoStrLength = ded.infoStr.length * textSize + 0.3f
        val point = ded.point
        val pointFlag = ded.pointFlag
        val textOffsetX = if (ded.type == "Box") -0.5f else 0f

        item(line(point, pointFlag, "ded"))
        if (point.x <= pointFlag.x) { // ptFlag is RIGHT from pt
            item(dedTextAndLine(ded.infoStr, pointFlag, pointFlag.plus((infoStrLength + textOffsetX).toDouble(), 0.0), textSize))
        } else {                      // ptFlag is LEFT from pt
            val p1 = pointFlag.plus((-ded.getInfo().length * textSize - textOffsetX).toDouble(), 0.0)
            item(dedTextAndLine(ded.infoStr, p1, pointFlag, textSize))
        }

        if (ded.type == "Circle") {
            item("""{"type":"circle","layer":"ded","cx":${point.x},"cy":${point.y},"r":${ded.lengthX / 2 * scale}}""")
        }
        if (ded.type == "Box") {
            // writeDedRect: 逆回転で box を組み直す (Y 反転後の空間では shapeAngle の符号が逆)
            ded.shapeAngle = -ded.shapeAngle
            ded.setBox(scale.toDouble())
            item(line(ded.pLTop, ded.pLBtm, "ded"))
            item(line(ded.pLTop, ded.pRTop, "ded"))
            item(line(ded.pRTop, ded.pRBtm, "ded"))
            item(line(ded.pLBtm, ded.pRBtm, "ded"))
        }
    }

    /** DxfEntity.writeTextAndLine と同形: テキスト (左寄せ・点が下端) + 下線。2 つの prim を結合して返す */
    private fun dedTextAndLine(st: String, p1: PointXY, p2: PointXY, textSize: Float): String =
        text(st, p1.plus(textSize.toDouble(), textSize.toDouble() * 0.2), 0.0, textSize, 1, "ded", ""","alignH":0""") +
            "," + line(p1, p2, "ded")

    private fun line(p1: PointXY, p2: PointXY, layer: String): String =
        """{"type":"line","layer":"$layer","x1":${p1.x},"y1":${p1.y},"x2":${p2.x},"y2":${p2.y}}"""

    private fun dimText(str: String, place: DimensionPlacement, angle: Double, textSize: Float, tri: Int, side: Int, h: Int, v: Int): String =
        text(str, place.dimpoint, angle, textSize, place.verticalDxf, "dim", ""","tri":$tri,"side":$side,"h":$h,"v":$v""")

    // extra は識別フィールド (",key":value 形式) をオブジェクト末尾に足すための口。
    // commonTest の count ベース assert (prefix 部分一致) を壊さないため末尾固定
    private fun text(str: String, p: PointXY, angle: Double, size: Float, align: Int, layer: String, extra: String = ""): String =
        """{"type":"text","layer":"$layer","text":"${escapeJson(str)}","x":${p.x},"y":${p.y},"angle":$angle,"size":$size,"align":$align$extra}"""

    private fun escapeJson(s: String): String {
        val sb = StringBuilder()
        for (c in s) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c.code < 0x20) sb.append("\\u" + c.code.toString(16).padStart(4, '0')) else sb.append(c)
            }
        }
        return sb.toString()
    }
}
