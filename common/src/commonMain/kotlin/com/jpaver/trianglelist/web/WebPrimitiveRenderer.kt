package com.jpaver.trianglelist.web

import com.example.trilib.PointXY
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
 */
object WebPrimitiveRenderer {

    /** 寸法文字高さ (モデル単位)。サンプル図面 (辺 2-6m) で読める大きさ */
    const val DEFAULT_TEXT_SIZE = 0.25f

    /** CSV 文字列から JSON プリミティブまで一気通貫 (wasmJs facade の本体) */
    fun renderCsv(csv: String, scale: Float): String {
        val trilist = WebCsvReader.read(csv)
        if (scale != 1f && scale > 0f) trilist.setScale(PointXY(0f, 0f), scale)
        val textSize = DEFAULT_TEXT_SIZE * (if (scale > 0f) scale else 1f)
        return render(trilist, textSize)
    }

    fun render(trilist: TriangleList, textSize: Float): String {
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
                item(dimText(tri.strLengthA, placeA, pab.calcDimAngle(pca), textSize))
            }
            item(dimText(tri.strLengthB, placeB, pbc.calcDimAngle(pab), textSize))
            item(dimText(tri.strLengthC, placeC, pca.calcDimAngle(pbc), textSize))

            // 旗揚げ線 (DxfFileWriter.writeDimFlagsFromLayout と同じ条件)
            if (tri.dim.horizontal.a > 2) item(line(placeA.pointA, placeA.pointB, "dim"))
            if (tri.dim.horizontal.b > 2) item(line(placeB.pointA, placeB.pointB, "dim"))
            if (tri.dim.horizontal.c > 2) item(line(placeC.pointA, placeC.pointB, "dim"))

            // 番号サークル + 番号 (DrawingFileWriter.writePointNumber と同じ r=ts*0.85、中央寄せ)
            val pn = tri.pointnumber
            item("""{"type":"circle","layer":"num","cx":${pn.x},"cy":${pn.y},"r":${textSize * 0.85f}}""")
            item(text(tri.mynumber.toString(), pn, 0.0, textSize, 2, "num"))
        }

        sb.append(']')
        return sb.toString()
    }

    private fun line(p1: PointXY, p2: PointXY, layer: String): String =
        """{"type":"line","layer":"$layer","x1":${p1.x},"y1":${p1.y},"x2":${p2.x},"y2":${p2.y}}"""

    private fun dimText(str: String, place: DimensionPlacement, angle: Double, textSize: Float): String =
        text(str, place.dimpoint, angle, textSize, place.verticalDxf, "dim")

    private fun text(str: String, p: PointXY, angle: Double, size: Float, align: Int, layer: String): String =
        """{"type":"text","layer":"$layer","text":"${escapeJson(str)}","x":${p.x},"y":${p.y},"angle":$angle,"size":$size,"align":$align}"""

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
