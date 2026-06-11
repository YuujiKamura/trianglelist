package com.jpaver.trianglelist.web

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * DXF 一致テスト (brief 検証 1-4): 同じ CSV から DxfFileWriter (writeDeduction:240-282) と
 * WebPrimitiveRenderer が出す控除図形の幾何 (円の中心/半径、矩形 4 辺) が一致する —
 * 座標符号 (Y 反転の取り違い) をここで殺す。
 *
 * DXF は unitscale (×1000) + 図枠センタリング移動 (writeEntities:301-317) が掛かるので、
 * 三角形の最初の辺 (両経路で同一のモデル座標を持つ) から affine (一様スケール + 平行移動) を
 * 解いて web 座標 → DXF 座標に写してから比較する。
 */
class WebDeductionDxfParityTest {

    private val baseCsv = "1,6.0,5.0,4.0,-1,-1\n"

    private data class Seg(val x1: Double, val y1: Double, val x2: Double, val y2: Double)
    private data class Circ(val cx: Double, val cy: Double, val r: Double)

    /** DXF テキストの ENTITIES セクションから LINE / CIRCLE を抜く (BLOCKS 等の混入を避ける) */
    private fun parseDxf(dxf: String): Pair<List<Seg>, List<Circ>> {
        val start = dxf.indexOf("ENTITIES")
        val end = dxf.indexOf("ENDSEC", start)
        val section = if (start >= 0 && end > start) dxf.substring(start, end) else dxf
        val tokens = section.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val lines = mutableListOf<Seg>()
        val circles = mutableListOf<Circ>()
        // エンティティ開始 ("0" + LINE/CIRCLE) は 1 トークンずつ走査して探す —
        // スライスが group code/value ペアの途中から始まっても整列に依存しない。
        // エンティティ内部は code/value ペア (codes はエンティティ区切りの "0" まで来ない)
        var i = 0
        while (i + 1 < tokens.size) {
            if (tokens[i] == "0" && (tokens[i + 1] == "LINE" || tokens[i + 1] == "CIRCLE")) {
                val isLine = tokens[i + 1] == "LINE"
                val vals = mutableMapOf<String, Double>()
                var j = i + 2
                while (j + 1 < tokens.size && tokens[j] != "0") {
                    tokens[j + 1].toDoubleOrNull()?.let { v -> vals[tokens[j]] = v }
                    j += 2
                }
                if (isLine) {
                    val x1 = vals["10"]; val y1 = vals["20"]; val x2 = vals["11"]; val y2 = vals["21"]
                    if (x1 != null && y1 != null && x2 != null && y2 != null) lines.add(Seg(x1, y1, x2, y2))
                } else {
                    val cx = vals["10"]; val cy = vals["20"]; val r = vals["40"]
                    if (cx != null && cy != null && r != null) circles.add(Circ(cx, cy, r))
                }
                i = j
            } else i += 1
        }
        return lines to circles
    }

    /** web プリミティブ JSON → layer 指定の line/circle */
    private fun webLines(json: String, layer: String): List<Seg> =
        Regex(""""type":"line","layer":"$layer","x1":([-0-9.E]+),"y1":([-0-9.E]+),"x2":([-0-9.E]+),"y2":([-0-9.E]+)""")
            .findAll(json)
            .map { m -> Seg(m.groupValues[1].toDouble(), m.groupValues[2].toDouble(), m.groupValues[3].toDouble(), m.groupValues[4].toDouble()) }
            .toList()

    private fun webCircles(json: String): List<Circ> =
        Regex(""""type":"circle","layer":"ded","cx":([-0-9.E]+),"cy":([-0-9.E]+),"r":([-0-9.E]+)""")
            .findAll(json)
            .map { m -> Circ(m.groupValues[1].toDouble(), m.groupValues[2].toDouble(), m.groupValues[3].toDouble()) }
            .toList()

    /** 三角形 1 の最初の辺から affine (dxf = web * k + t) を解く */
    private fun solveAffine(dxfLines: List<Seg>, webTriLine: Seg): Triple<Double, Double, Double> {
        // DXF 側の三角形線 = 最初の LINE (writeEntities は三角形 → 控除 → 図枠の順)
        val d = dxfLines.first()
        val k = (d.x2 - d.x1) / (webTriLine.x2 - webTriLine.x1)
        val tx = d.x1 - k * webTriLine.x1
        val ty = d.y1 - k * webTriLine.y1
        // 同一辺であることの裏取り: y 方向のスケールも同じ k で説明できる
        assertEquals(d.y2, k * webTriLine.y2 + ty, 1e-3)
        return Triple(k, tx, ty)
    }

    @Test
    fun circle_deduction_geometry_matches_dxf() {
        val (cx, cy) = run {
            val tri = WebCsvReader.read(baseCsv).getBy(1)
            Pair(
                ((tri.point[0].x + tri.pointAB.x + tri.pointBC.x) / 3.0).toFloat(),
                ((tri.point[0].y + tri.pointAB.y + tri.pointBC.y) / 3.0).toFloat(),
            )
        }
        val ded = WebDeduction.placeDeduction(baseCsv, cx, cy, "仕切弁", 0.23f, 0f, 1)
        val csv = baseCsv + ded + "\n"

        val json = WebPrimitiveRenderer.renderCsv(csv, 1.0f)
        val webC = webCircles(json)
        assertEquals(1, webC.size)

        val (dxfLines, dxfCircles) = parseDxf(WebDrawingExport.buildDxfText(csv))
        val (k, tx, ty) = solveAffine(dxfLines, webLines(json, "tri").first())

        // 半径 = lengthX/2 が同倍率で一致する CIRCLE が DXF に居る (番号サークル等と区別)
        val expectedR = webC[0].r * k
        val hit = dxfCircles.filter { abs(it.r - expectedR) < 1e-3 }
        assertTrue(hit.isNotEmpty(), "ded circle (r=$expectedR) not found in DXF: $dxfCircles")
        // 中心座標 (X と、符号を取り違えやすい Y の両方) が affine 写像で一致する
        assertTrue(
            hit.any { abs(it.cx - (webC[0].cx * k + tx)) < 1e-3 && abs(it.cy - (webC[0].cy * k + ty)) < 1e-3 },
            "ded circle center mismatch: dxf=$hit web=${webC[0]} k=$k t=($tx,$ty)",
        )
    }

    @Test
    fun box_deduction_corners_match_dxf() {
        val (cx, cy) = run {
            val tri = WebCsvReader.read(baseCsv).getBy(1)
            Pair(
                ((tri.point[0].x + tri.pointAB.x + tri.pointBC.x) / 3.0).toFloat(),
                ((tri.point[0].y + tri.pointAB.y + tri.pointBC.y) / 3.0).toFloat(),
            )
        }
        val ded = WebDeduction.placeDeduction(baseCsv, cx, cy, "集水桝", 0.8f, 0.6f, 1)
        val csv = baseCsv + ded + "\n"

        val json = WebPrimitiveRenderer.renderCsv(csv, 1.0f)
        // ded の line 群 = 旗揚げ線 + infoStr 下線 + 矩形 4 辺。末尾 4 本が writeDedRect の辺
        val dedSegs = webLines(json, "ded")
        assertTrue(dedSegs.size >= 6, "expected flag+underline+4 box lines: ${dedSegs.size}")
        val boxSegs = dedSegs.takeLast(4)

        val (dxfLines, _) = parseDxf(WebDrawingExport.buildDxfText(csv))
        val (k, tx, ty) = solveAffine(dxfLines, webLines(json, "tri").first())

        // 4 辺すべてが DXF の LINE 群に (向き不問・affine 写像で) 存在する —
        // shapeAngle の逆回転 (writeDedRect:276) まで含めた一致
        for (s in boxSegs) {
            val a = Pair(s.x1 * k + tx, s.y1 * k + ty)
            val b = Pair(s.x2 * k + tx, s.y2 * k + ty)
            val found = dxfLines.any { d ->
                (near(d.x1, d.y1, a) && near(d.x2, d.y2, b)) || (near(d.x1, d.y1, b) && near(d.x2, d.y2, a))
            }
            assertTrue(found, "box edge $s (mapped $a-$b) not found in DXF lines")
        }
    }

    private fun near(x: Double, y: Double, p: Pair<Double, Double>): Boolean =
        abs(x - p.first) < 1e-3 && abs(y - p.second) < 1e-3
}
