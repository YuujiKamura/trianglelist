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
 */
object WebPrimitiveRenderer {

    const val DEFAULT_TEXT_SIZE = 0.25f
    const val APP_DEFAULT_TEXT_SIZE = 30f

    fun renderCsv(csv: String, scale: Float): String = renderCsv(csv, scale, "")

    fun renderCsv(csv: String, scale: Float, overridesJson: String): String {
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        if (scale != 1f && scale > 0f) trilist.setScale(PointXY(0f, 0f), scale)
        val sizeRatio = (doc.textSize?.takeIf { it > 0f } ?: APP_DEFAULT_TEXT_SIZE) / APP_DEFAULT_TEXT_SIZE
        val effScale = if (scale > 0f) scale else 1f
        val textSize = DEFAULT_TEXT_SIZE * sizeRatio * effScale
        val mixed = CsvCodec.buildMixed(doc, trilist, effScale)
        WebOverrides.applyJson(mixed, overridesJson)
        trilist.arrangePointNumbers()
        return render(mixed, textSize, CsvCodec.buildDeductions(doc), effScale, trilist.sokutenListVector)
    }

    fun render(trilist: TriangleList, textSize: Float): String {
        val mixed = EditList<EditObject>()
        for (i in 1..trilist.size()) mixed.add(trilist.getBy(i))
        return render(mixed, textSize, DeductionList(), 1f)
    }

    fun render(
        list: EditList<EditObject>,
        textSize: Float,
        dedlist: DeductionList = DeductionList(),
        scale: Float = 1f,
        sokutenListVector: Int = 0,
    ): String {
        val sb = StringBuilder()
        sb.append('[')
        var first = true
        fun item(json: String) {
            if (!first) sb.append(',')
            sb.append(json)
            first = false
        }

        // 段階0: textSize 配布
        list.forEachItem { obj ->
            obj.applyDimTextSize(textSize)
        }

        // 段階1a: 塗り
        list.forEachItemIndexed { num, obj ->
            val verts = obj.vertices()
            if (obj is Triangle) {
                if (verts.size >= 3) {
                    item(
                        """{"type":"fill","layer":"fill","x1":${verts[0].x},"y1":${verts[0].y},"x2":${verts[1].x},"y2":${verts[1].y},"x3":${verts[2].x},"y3":${verts[2].y},"color":${obj.mycolor},"tri":$num}"""
                    )
                }
            } else if (obj is Rectangle) {
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

        // 段階1b: 辺 + 寸法 + 番号サークル + 番号テキスト
        val effScale = if (scale > 0f) scale else 1f
        list.forEachItemIndexed { num, obj ->
            // 辺
            for (side in 0 until obj.sideCount) {
                val ln = obj.getLine(side)
                item(line(ln.left, ln.right, "tri", ""","tri":$num,"side":$side"""))
            }
            // 寸法
            for (spec in obj.emitDimensionSpecs(effScale)) {
                item(dimText(spec.text, spec.place, spec.angle, textSize, num, spec.side, spec.h, spec.v))
                if (spec.emitFlag) item(line(spec.place.pointA, spec.place.pointB, "dim"))
            }
            // Rectangle 専用: meta perp 識別子 + guide 線
            if (obj is Rectangle) {
                // 垂線・ガイド・直角記号は Rectangle に問い合わせるだけ (描画ロジックを露出させない)
                item("""{"type":"meta","kind":"perp","tri":$num,"perpFrom":"${obj.perpFrom}"}""")
                obj.getGuideLine()?.let { item(line(it.left, it.right, "guide")) }
                val (raA, raB) = obj.getRightAngleMark()
                item(line(raA.left, raA.right, "ra"))
                item(line(raB.left, raB.right, "ra"))
            }
            // 番号位置
            val center = obj.pointNumberAnchor()
            val circleR = textSize * 0.85f
            item("""{"type":"circle","layer":"num","cx":${center.x},"cy":${center.y},"r":$circleR,"tri":$num}""")
            item(text(num.toString(), center, 0.0, textSize, 2, "num", ""","tri":$num"""))

            // 引き出し矢印: 番号位置が図形外なら幾何中心から矢印を引く (kind 不問)。
            // Rectangle は pointNumberAnchor()=centroid() なので必ず内部、矢印は描かれない。
            if (!obj.containsPoint(center)) {
                val pc = obj.centroid()
                val pn = center
                val pnOffsetToC = pn.offset(pc, (circleR * 1.1f).toDouble())
                val arrowTail = pc.offset(pn, pc.lengthTo(pnOffsetToC) * 0.3).rotate(pc, 10.0)
                item(line(pc, pnOffsetToC, "num"))
                item(line(pc, arrowTail, "num"))
            }

            // 測点 (Station Flag)
            if (obj.name.isNotEmpty()) {
                val ln = obj.getLine(0)
                val horizontalS = if (obj is Triangle) obj.dim.horizontal.s else 0
                val ds = if (obj is Triangle) obj.scaleFactor.toDouble() else effScale.toDouble()
                val dh = if (obj is Triangle) obj.dimHeight.toDouble() else textSize.toDouble()

                val place = DimensionLayout.layout(
                    ln.right, ln.left,
                    DimensionLayout.SIDE_SOKUTEN, horizontalS,
                    ds, dh, 0.0
                )
                val angle = place.pointB.calcSokAngle(place.pointA, sokutenListVector)
                item(text(obj.name, place.dimpoint, angle, textSize, 1, "num", ""","tri":$num"""))
                item(line(place.pointA, place.pointB, "num", ""","tri":$num"""))
            }
        }

        // Deduction overlay
        if (dedlist.size() > 0) {
            val dedModel = dedlist.clone()
            dedModel.scale(PointXY(0f, 0f), effScale, -effScale)
            for (n in 1..dedModel.size()) renderDeduction(dedModel.get(n), textSize, effScale, ::item)
        }

        sb.append(']')
        return sb.toString()
    }

    private fun renderDeduction(ded: Deduction, textSize: Float, scale: Float, item: (String) -> Unit) {
        val infoStrLength = ded.infoStr.length * textSize + 0.3f
        val point = ded.point
        val pointFlag = ded.pointFlag
        val tag = ""","ded":${ded.num}"""

        item(line(point, pointFlag, "ded", tag))
        if (point.x <= pointFlag.x) {
            item(dedTextAndLine(ded.infoStr, pointFlag, pointFlag.plus((infoStrLength).toDouble(), 0.0), textSize, tag))
        } else {
            item(dedTextAndLine(ded.infoStr, pointFlag.plus((-ded.getInfo().length * textSize).toDouble(), 0.0), pointFlag, textSize, tag))
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

    private fun dedTextAndLine(st: String, p1: PointXY, p2: PointXY, textSize: Float, tag: String = ""): String =
        text(st, p1.plus(textSize.toDouble(), textSize.toDouble() * 0.2), 0.0, textSize, 1, "ded", ""","alignH":0$tag""") +
            "," + line(p1, p2, "ded", tag)

    private fun line(p1: PointXY, p2: PointXY, layer: String, extra: String = ""): String =
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
