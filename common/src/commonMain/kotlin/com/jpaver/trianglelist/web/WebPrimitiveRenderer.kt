package com.jpaver.trianglelist.web

import com.example.trilib.PointXY
import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.Deduction
import com.jpaver.trianglelist.editmodel.DeductionList
import com.jpaver.trianglelist.editmodel.EditList
import com.jpaver.trianglelist.editmodel.EditObject
import com.jpaver.trianglelist.editmodel.Rectangle
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import com.jpaver.trianglelist.editmodel.isCollide
import com.jpaver.trianglelist.label.DimensionLayout
import com.jpaver.trianglelist.label.DimensionPlacement
import com.jpaver.trianglelist.setDimPath
import com.jpaver.trianglelist.setLengthStr

/**
 * Web 段階1 (insight #60/#61): CSV → 描画プリミティブ JSON。
 *
 * SoT 一本化 段3f (2026-06-15): renderCsv は CsvCodec.composeAll を介して混在 EditList<EditObject>
 * を作り、新 render(list) に一本化。旧 6 引数 render(trilist, traps, trapTris) と private
 * renderRectangle は撤去された ── レンダラに残るのは多態 1 ループのみ。「Pages 版の ID 手計算
 * オフセット破綻」が構造的に不可能になった (= ID は figureRows 出現順 = forEachItemIndexed 1始まり)。
 *
 * 構成は app/datamanager/DxfFileWriter.writeTriangle (ADR 0003 Phase 2a) を踏襲:
 *   - 辺 (sideCount 個、 getLine(side) 多態): Triangle=3, Rectangle=4
 *   - 寸法値 (emitDimensionSpecs 多態): A辺は親共有なし (nodeA==null && node.a==null) or 再接続のみ
 *   - 番号: pointnumber を中心にサークル (r=textSize*0.85) + 番号テキスト
 *
 * JSON 形式 (フラット配列、座標はモデル座標系 = y 上向き。y 反転は JS 側):
 *   {"type":"line","layer":"tri|dim","x1":..,"y1":..,"x2":..,"y2":..}
 *   {"type":"text","layer":"dim|num","text":"..","x":..,"y":..,"angle":deg,"size":..,"align":1|2|3}
 *   {"type":"circle","layer":"num","cx":..,"cy":..,"r":..}
 *   {"type":"fill","layer":"fill","x1":..,"y1":..,"x2":..,"y2":..,"x3":..,"y3":..,"color":idx,"tri":N}
 *   {"type":"meta","kind":"perp","tri":N,"perpFrom":"bl|tl"}  (Rectangle 垂線起点の verify 用)
 *   {"type":"line","layer":"guide",...}  (Rectangle alignment != 0 の垂線ガイド)
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
     * model へ適用してから render する。
     * SoT 一本化 段3f: composeAll 経由で混在 EditList<EditObject> を作り、 新 render(list) に流す。
     * arrangePointNumbers は new render の textSize 配布より前に呼ぶ (Triangle.pointnumber を確定)。
     */
    fun renderCsv(csv: String, scale: Float, overridesJson: String): String {
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        if (scale != 1f && scale > 0f) trilist.setScale(PointXY(0f, 0f), scale)
        WebOverrides.applyJson(trilist, overridesJson)
        val sizeRatio = (doc.textSize?.takeIf { it > 0f } ?: APP_DEFAULT_TEXT_SIZE) / APP_DEFAULT_TEXT_SIZE
        val effScale = if (scale > 0f) scale else 1f
        val textSize = DEFAULT_TEXT_SIZE * sizeRatio * effScale
        trilist.arrangePointNumbers()
        val mixed = CsvCodec.buildMixed(doc, trilist, effScale)
        return render(mixed, textSize, CsvCodec.buildDeductions(doc), effScale)
    }

    /**
     * 後方互換: 既存テスト (CsvCodecTest / WebPrimitiveRendererTest) が直接呼ぶ 2 引数 API。
     * trilist 中の三角形を混在 EditList に詰め直して新 render(list) に委譲する。
     */
    fun render(trilist: TriangleList, textSize: Float): String {
        val mixed = EditList<EditObject>()
        for (i in 1..trilist.size()) mixed.add(trilist.getBy(i))
        return render(mixed, textSize, DeductionList(), 1f)
    }

    /**
     * SoT 一本化 段3 多態ルート (2026-06-15): 混在 EditList<EditObject> を直接受けて prim を出す。
     * EditObject の多態 (sideCount / vertices / getLine / emitDimensionSpecs) で形状ごとの kind 分岐を消す:
     *   - Triangle.sideCount=3, Rectangle.sideCount=4 で stride 決定
     *   - vertices() で塗り/番号サークル中心を一律算出
     *   - getLine(side) で辺取り出し
     *   - emitDimensionSpecs(scale) で寸法 spec 取り出し
     * 引き出し矢印 (Triangle のみ) と Rectangle 垂線ガイド/meta は残る kind 分岐 — 将来
     * EditObject に open fun emitLeaderSpec() / emitGuideLines() / emitMetaPrims() を昇格させて吸収する。
     */
    fun render(
        list: EditList<EditObject>,
        textSize: Float,
        dedlist: DeductionList = DeductionList(),
        scale: Float = 1f,
    ): String {
        val sb = StringBuilder()
        sb.append('[')
        var first = true
        fun item(json: String) {
            if (!first) sb.append(',')
            sb.append(json)
            first = false
        }

        // 段階0: textSize 配布 (既存 render(trilist) の setDimPathTextSize と同等の準備)。
        // dimHeight は DimensionLayout.layout の dimheight 引数で寸法線位置を決める。
        list.forEachItem { obj ->
            when (obj) {
                is Triangle -> obj.setDimPath(textSize)
                is Rectangle -> obj.dimHeight = textSize
            }
        }

        // 段階1a: 塗り (z-order「塗り全部→線・文字」確保のため先に全部出す)。
        // Triangle = fill prim 1 個、Rectangle = 対角線 bl→tr で 2 三角形に分割して fill prim 2 個。
        // main.ts の FillPrim は 3 頂点固定なので polygon 型を増やさず Triangle と同形式で揃える。
        // Rectangle に color フィールドは無いため既定 4 (FILL_PALETTE の sky) で固定。
        list.forEachItemIndexed { num, obj ->
            if (obj is Triangle) {
                val verts = obj.vertices()
                if (verts.size >= 3) {
                    item(
                        """{"type":"fill","layer":"fill","x1":${verts[0].x},"y1":${verts[0].y},"x2":${verts[1].x},"y2":${verts[1].y},"x3":${verts[2].x},"y3":${verts[2].y},"color":${obj.mycolor},"tri":$num}"""
                    )
                }
            } else if (obj is Rectangle) {
                // vertices() = [bl, br, tr, tl] (Rectangle.kt:87-90)。凸四角形を bl-tr 対角線で
                // 2 三角形に分割: (bl,br,tr) + (bl,tr,tl)。overlap / 隙間なし。
                val verts = obj.vertices()
                if (verts.size >= 4) {
                    val bl = verts[0]; val br = verts[1]; val tr = verts[2]; val tl = verts[3]
                    val color = 4
                    item(
                        """{"type":"fill","layer":"fill","x1":${bl.x},"y1":${bl.y},"x2":${br.x},"y2":${br.y},"x3":${tr.x},"y3":${tr.y},"color":$color,"tri":$num}"""
                    )
                    item(
                        """{"type":"fill","layer":"fill","x1":${bl.x},"y1":${bl.y},"x2":${tr.x},"y2":${tr.y},"x3":${tl.x},"y3":${tl.y},"color":$color,"tri":$num}"""
                    )
                }
            }
        }

        // 段階1b: 辺 (多態 getLine) + 寸法 (多態 emitDimensionSpecs) + 番号サークル + 番号テキスト
        val effScale = if (scale > 0f) scale else 1f
        list.forEachItemIndexed { num, obj ->
            for (side in 0 until obj.sideCount) {
                val ln = obj.getLine(side)
                item(line(ln.left, ln.right, "tri", ""","tri":$num,"side":$side"""))
            }
            for (spec in obj.emitDimensionSpecs(effScale)) {
                item(dimText(spec.text, spec.place, spec.angle, textSize, num, spec.side, spec.h, spec.v))
                if (spec.emitFlag) item(line(spec.place.pointA, spec.place.pointB, "dim"))
            }
            // Rectangle 専用: meta perp 識別子 + guide 線 (alignment != 0 のとき)
            if (obj is Rectangle) {
                val lp = obj.calcPoint()
                val bl = lp.a.left; val br = lp.a.right; val tl = lp.b.left; val tr = lp.b.right
                val bottomShorter = obj.widthA <= obj.widthB
                val baseStart = if (bottomShorter) bl else tl
                val baseEnd = if (bottomShorter) br else tr
                val perpFoot = baseStart.crossOffset(baseEnd, obj.length, if (bottomShorter) -90.0 else 90.0)
                item("""{"type":"meta","kind":"perp","tri":$num,"perpFrom":"${if (bottomShorter) "bl" else "tl"}"}""")
                if (obj.alignment != 0) item(line(baseStart, perpFoot, "guide"))
            }
            // 番号位置: Triangle は事前計算済 pointnumber、Rectangle は重心 (vertices 平均)
            val center = when (obj) {
                is Triangle -> obj.pointnumber
                else -> {
                    val v = obj.vertices()
                    if (v.isEmpty()) PointXY(0f, 0f)
                    else {
                        var sx = 0f; var sy = 0f
                        for (p in v) { sx += p.x.toFloat(); sy += p.y.toFloat() }
                        PointXY(sx / v.size, sy / v.size)
                    }
                }
            }
            val circleR = textSize * 0.85f
            item("""{"type":"circle","layer":"num","cx":${center.x},"cy":${center.y},"r":$circleR,"tri":$num}""")
            item(text(num.toString(), center, 0.0, textSize, 2, "num", ""","tri":$num"""))

            // 引き出し矢印 (Triangle のみ — pointcenter/pointnumber 依存)
            if (obj is Triangle && !obj.isCollide(obj.pointnumber)) {
                val pc = obj.pointcenter
                val pn = obj.pointnumber
                val pnOffsetToC = pn.offset(pc, (circleR * 1.1f).toDouble())
                val arrowTail = pc.offset(pn, pc.lengthTo(pnOffsetToC) * 0.3).rotate(pc, 10.0)
                item(line(pc, pnOffsetToC, "num"))
                item(line(pc, arrowTail, "num"))
            }
        }

        // Deduction overlay。ded はビュー空間 (y 下向き) で持つので Y 反転でモデル空間に揃える
        if (dedlist.size() > 0) {
            val dedModel = dedlist.clone()
            dedModel.scale(PointXY(0f, 0f), effScale, -effScale)
            for (n in 1..dedModel.size()) renderDeduction(dedModel.get(n), textSize, effScale, ::item)
        }

        sb.append(']')
        return sb.toString()
    }

    /**
     * DxfFileWriter.writeDeduction:240-282 の写し (色 = 赤系は layer "ded" として TS 側で塗る):
     * - 旗揚げ線 point→pointFlag
     * - infoStr テキスト + 下線。pointFlag の左右で分岐
     * - Circle: 中心 point、半径 lengthX/2
     * - Box: 逆回転で setBox し直して 4 辺
     */
    private fun renderDeduction(ded: Deduction, textSize: Float, scale: Float, item: (String) -> Unit) {
        val infoStrLength = ded.infoStr.length * textSize + 0.3f
        val point = ded.point
        val pointFlag = ded.pointFlag
        val textOffsetX = if (ded.type == "Box") -0.5f else 0f
        val tag = ""","ded":${ded.num}"""

        item(line(point, pointFlag, "ded", tag))
        if (point.x <= pointFlag.x) {
            item(dedTextAndLine(ded.infoStr, pointFlag, pointFlag.plus((infoStrLength + textOffsetX).toDouble(), 0.0), textSize, tag))
        } else {
            val p1 = pointFlag.plus((-ded.getInfo().length * textSize - textOffsetX).toDouble(), 0.0)
            item(dedTextAndLine(ded.infoStr, p1, pointFlag, textSize, tag))
        }

        if (ded.type == "Circle") {
            item("""{"type":"circle","layer":"ded","cx":${point.x},"cy":${point.y},"r":${ded.lengthX / 2 * scale}$tag}""")
        }
        if (ded.type == "Box") {
            ded.shapeAngle = -ded.shapeAngle
            ded.setBox(scale.toDouble())
            item(line(ded.pLTop, ded.pLBtm, "ded", tag))
            item(line(ded.pLTop, ded.pRTop, "ded", tag))
            item(line(ded.pRTop, ded.pRBtm, "ded", tag))
            item(line(ded.pLBtm, ded.pRBtm, "ded", tag))
        }
    }

    /** DxfEntity.writeTextAndLine と同形: テキスト (左寄せ・点が下端) + 下線 */
    private fun dedTextAndLine(st: String, p1: PointXY, p2: PointXY, textSize: Float, tag: String = ""): String =
        text(st, p1.plus(textSize.toDouble(), textSize.toDouble() * 0.2), 0.0, textSize, 1, "ded", ""","alignH":0$tag""") +
            "," + line(p1, p2, "ded", tag)

    private fun line(p1: PointXY, p2: PointXY, layer: String): String = line(p1, p2, layer, "")

    private fun line(p1: PointXY, p2: PointXY, layer: String, extra: String): String =
        """{"type":"line","layer":"$layer","x1":${p1.x},"y1":${p1.y},"x2":${p2.x},"y2":${p2.y}$extra}"""

    private fun dimText(str: String, place: DimensionPlacement, angle: Double, textSize: Float, tri: Int, side: Int, h: Int, v: Int): String =
        text(str, place.dimpoint, angle, textSize, place.verticalDxf, "dim", ""","tri":$tri,"side":$side,"h":$h,"v":$v""")

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
