import com.jpaver.trianglelist.dxf.*
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import kotlin.math.*

/**
 * CSV → DXF 変換 CLI
 *
 * v1 CSV形式:
 *   ヘッダー4行（koujiname, rosenname, gyousyaname, zumennum）
 *   データ行: ID, A, B, C, parentNum, connType
 *
 * v2 CSV形式 (#VERSION,2.0):
 *   #HEADER セクション
 *   #TRIANGULAR セクション（ヘッダー行 + データ行）
 *   number,lengthA,lengthB,lengthC,parent,connectionType,connectionSide,connectionLCR,name,color
 *
 * connType/connectionType: -1=独立, 1=親のB辺に接続, 2=親のC辺に接続
 */

data class CsvHeader(
    val koujiname: String = "", val rosenname: String = "",
    val gyousyaname: String = "", val zumennum: String = ""
)

data class TriData(
    val id: Int, val a: Double, val b: Double, val c: Double,
    val parentNum: Int, val connType: Int, val name: String = "", val color: Int = 7
)

data class Vertex(val x: Double, val y: Double)

data class PlacedTriangle(
    val id: Int,
    val point0: Vertex, val pointAB: Vertex, val pointBC: Vertex,
    val a: Double, val b: Double, val c: Double,
    val angle: Double,    // A辺方向（度数法）
    val angleAB: Double,  // 頂点B(pointAB)の内角（度数法）
    val angleCA: Double,  // 頂点A(point0)の内角（度数法）
    val name: String, val color: Int
) {
    /** connType=1 の子に渡す角度 */
    val angleMpAB: Double get() = angle + angleAB
    /** connType=2 の子に渡す角度 */
    val angleMmCA: Double get() = angle - angleCA
}
// point0-pointAB = A辺, pointAB-pointBC = B辺, pointBC-point0 = C辺

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: csvToDxf <input.csv> [output.dxf] [--text-height=<mm>]")
        return
    }
    // オプション解析
    val positional = args.filter { !it.startsWith("--") }
    val textHeight = args.firstOrNull { it.startsWith("--text-height=") }
        ?.substringAfter("=")?.toDoubleOrNull() ?: 250.0

    val csvFile = File(positional[0])
    val baseName = csvFile.nameWithoutExtension
    val outDir = csvFile.parentFile
    // 出力先省略時: 入力CSVと同じフォルダに {basename}_triangles.dxf/.xlsx
    val dxfFile = if (positional.size >= 2) File(positional[1])
                  else File(outDir, "${baseName}_triangles.dxf")
    val xlsxFile = File(outDir, "${baseName}_triangles.xlsx")

    if (!csvFile.exists()) {
        System.err.println("CSV not found: ${csvFile.absolutePath}")
        return
    }

    val csvLines = csvFile.readLines()
    val header = parseHeader(csvLines)
    val triangles = parseCSV(csvLines)

    if (triangles.isEmpty()) {
        System.err.println("No triangle data found")
        return
    }

    // DXF 出力
    val placed = placeTriangles(triangles)
    println("Text height: ${textHeight}mm")
    val (dxfLines, dxfTexts, dxfCircles) = buildEntities(placed, textHeight)
    val dxfContent = DxfWriter.write(dxfLines, dxfTexts, dxfCircles, insUnits = DxfConstants.Units.MILLIMETER)
    dxfFile.writeText(dxfContent)
    println("DXF written: ${dxfFile.absolutePath} (${placed.size} triangles)")

    // Excel 出力
    writeXlsx(xlsxFile, triangles, header.rosenname)
    println("XLSX written: ${xlsxFile.absolutePath} (${triangles.size} triangles)")
}

/** CSVヘッダー情報パース */
fun parseHeader(lines: List<String>): CsvHeader {
    val isV2 = lines.any { it.trimStart().startsWith("#VERSION") }
    if (isV2) {
        var inHeader = false
        val map = mutableMapOf<String, String>()
        for (raw in lines) {
            val line = raw.trim()
            if (line.startsWith("#HEADER")) { inHeader = true; continue }
            if (line.startsWith("#") && inHeader) break
            if (!inHeader || line.isBlank()) continue
            val p = line.split(",").map { it.trim() }
            if (p.size >= 2) map[p[0]] = p[1]
        }
        return CsvHeader(
            koujiname = map["koujiname"] ?: "",
            rosenname = map["rosenname"] ?: "",
            gyousyaname = map["gyousyaname"] ?: "",
            zumennum = map["zumennum"] ?: ""
        )
    } else {
        // v1: 最初の4行がヘッダー
        return CsvHeader(
            koujiname = lines.getOrNull(0)?.trim() ?: "",
            rosenname = lines.getOrNull(1)?.trim() ?: "",
            gyousyaname = lines.getOrNull(2)?.trim() ?: "",
            zumennum = lines.getOrNull(3)?.trim() ?: ""
        )
    }
}

/** CSV パース（v1/v2 自動判定） */
fun parseCSV(lines: List<String>): List<TriData> {
    val isV2 = lines.any { it.trimStart().startsWith("#VERSION") }
    return if (isV2) parseV2(lines) else parseV1(lines)
}

fun parseV1(lines: List<String>): List<TriData> {
    return lines.drop(4).mapNotNull { line ->
        val p = line.split(",").map { it.trim() }
        if (p.size < 6) return@mapNotNull null
        try {
            TriData(p[0].toInt(), p[1].toDouble(), p[2].toDouble(), p[3].toDouble(),
                p[4].toInt(), p[5].toInt())
        } catch (_: NumberFormatException) { null }
    }
}

fun parseV2(lines: List<String>): List<TriData> {
    var inTriangular = false
    var headerSkipped = false
    val result = mutableListOf<TriData>()

    for (raw in lines) {
        val line = raw.trim()
        if (line.startsWith("#TRIANGULAR")) { inTriangular = true; headerSkipped = false; continue }
        if (line.startsWith("#") && inTriangular) break // next section
        if (!inTriangular || line.isBlank()) continue
        if (!headerSkipped) { headerSkipped = true; continue } // skip column header

        val p = line.split(",").map { it.trim() }
        if (p.size < 6) continue
        try {
            val id = p[0].toInt()
            val a = p[1].toDouble(); val b = p[2].toDouble(); val c = p[3].toDouble()
            val parent = p[4].toInt()
            val colType = p[5].toIntOrNull() ?: 0          // connectionType(p[5])
            val colSide = if (p.size > 6) p[6].toIntOrNull() ?: 0 else 0  // connectionSide(p[6])
            // B/C辺の判定: どちらかが2ならC辺接続
            val connType = if (parent == -1) -1
                           else if (colSide == 2 || colType == 2) 2
                           else 1
            val name = if (p.size > 8) p[8] else ""
            val color = if (p.size > 9) p[9].toIntOrNull() ?: 7 else 7
            result.add(TriData(id, a, b, c, parent, connType, name, color))
        } catch (_: NumberFormatException) { /* skip */ }
    }
    return result
}

/** 第3頂点(pointBC)を計算 — TriangleExtensions.kt:24-34 と同一ロジック */
fun calculatePointBC(point0: Vertex, pointAB: Vertex, a: Double, b: Double, c: Double): Vertex {
    val theta = atan2(point0.y - pointAB.y, point0.x - pointAB.x)
    val alpha = acos(((a * a + b * b - c * c) / (2 * a * b)).coerceIn(-1.0, 1.0))
    val ang = theta + alpha
    return Vertex(pointAB.x + b * cos(ang), pointAB.y + b * sin(ang))
}

/** 頂点p2における内角（度数法） — TriangleExtensions.kt:47-52 と同一ロジック */
fun calculateInternalAngle(p1: Vertex, p2: Vertex, p3: Vertex): Double {
    val v1x = p1.x - p2.x; val v1y = p1.y - p2.y
    val v2x = p3.x - p2.x; val v2y = p3.y - p2.y
    val dot = v1x * v2x + v1y * v2y
    val mag1 = sqrt(v1x * v1x + v1y * v1y); val mag2 = sqrt(v2x * v2x + v2y * v2y)
    return Math.toDegrees(acos((dot / (mag1 * mag2)).coerceIn(-1.0, 1.0)))
}

fun dist(a: Vertex, b: Vertex): Double = sqrt((a.x - b.x).pow(2) + (a.y - b.y).pow(2))

/** 三角形を座標配置 — app モジュールの Triangle.calcPoints() / setOn() 準拠 */
fun placeTriangles(triangles: List<TriData>): Map<Int, PlacedTriangle> {
    val placed = mutableMapOf<Int, PlacedTriangle>()

    for (tri in triangles) {
        val pt0: Vertex; val ptAB: Vertex; val ptBC: Vertex
        val angle: Double

        if (tri.parentNum == -1 || tri.connType == -1) {
            // 独立三角形: angle=0°（A辺が右向き＝左→右に進行）
            angle = 0.0
            pt0 = Vertex(0.0, 0.0)
            val rad = Math.toRadians(angle)
            ptAB = Vertex(pt0.x + tri.a * cos(rad), pt0.y + tri.a * sin(rad))
            ptBC = calculatePointBC(pt0, ptAB, tri.a, tri.b, tri.c)
        } else {
            val parent = placed[tri.parentNum]
            if (parent == null) {
                System.err.println("Parent ${tri.parentNum} not found for triangle ${tri.id}")
                continue
            }

            val childA: Double
            when (tri.connType) {
                1 -> {
                    // 親のB辺に接続 — 親の頂点を直接コピー（浮動小数点誤差を排除）
                    childA = parent.b
                    pt0 = parent.pointBC
                    ptAB = parent.pointAB
                }
                2 -> {
                    // 親のC辺に接続 — 親の頂点を直接コピー
                    childA = parent.c
                    pt0 = parent.point0
                    ptAB = parent.pointBC
                }
                else -> {
                    System.err.println("Unknown connType ${tri.connType} for triangle ${tri.id}")
                    continue
                }
            }
            // angleは実座標から逆算
            angle = Math.toDegrees(atan2(ptAB.y - pt0.y, ptAB.x - pt0.x))
            ptBC = calculatePointBC(pt0, ptAB, childA, tri.b, tri.c)
        }

        val angleAB = calculateInternalAngle(pt0, ptAB, ptBC)
        val angleCA = calculateInternalAngle(ptBC, pt0, ptAB)

        placed[tri.id] = PlacedTriangle(
            tri.id, pt0, ptAB, ptBC,
            tri.a, tri.b, tri.c,
            angle, angleAB, angleCA,
            tri.name, tri.color
        )
    }
    return placed
}

/** PointXY.calcDimAngle() 準拠 — 基線に沿った読みやすいテキスト角度 */
fun calcDimAngle(from: Vertex, to: Vertex): Double {
    var angle = Math.toDegrees(atan2(to.x - from.x, to.y - from.y))
    angle = -angle + 90.0
    if (angle > 90.0) angle -= 180.0
    if (angle < 0.0) angle += 360.0
    return angle
}

/** 基線方向から外側の垂直アライメントを決定 — DimOnPath.verticalDxf() 準拠
 *  辺の2頂点と対向頂点から、寸法テキストを三角形の外側に配置するためのalignVを返す */
fun dimVerticalDxf(va: Vertex, vb: Vertex, opposite: Vertex): Int {
    // 辺(va→vb)に対して対向頂点がどちら側にあるか
    val cross = (vb.x - va.x) * (opposite.y - va.y) - (vb.y - va.y) * (opposite.x - va.x)

    // calcDimAngleが方向を反転したか判定
    val naturalAngle = Math.toDegrees(atan2(vb.y - va.y, vb.x - va.x))
    val dimAngle = calcDimAngle(va, vb)
    val diff = ((dimAngle - naturalAngle) % 360 + 360) % 360
    val flipped = diff > 90.0 && diff < 270.0

    // cross > 0: 対向頂点は左側 → テキストは右側（外側）→ alignV=3(top=テキスト下方)
    // flipped の場合はDXFテキスト座標系での上下が反転する
    return if (flipped xor (cross > 0)) 3 else 1
}

/** DXFエンティティ生成（レイヤー分離: TRI/LEN/NUM）— 座標はミリ単位 */
fun buildEntities(placed: Map<Int, PlacedTriangle>, textHeight: Double = 250.0): Triple<List<DxfLine>, List<DxfText>, List<DxfCircle>> {
    val dxfLines = mutableListOf<DxfLine>()
    val dxfTexts = mutableListOf<DxfText>()
    val dxfCircles = mutableListOf<DxfCircle>()
    val S = 1000.0  // メートル→ミリ変換係数

    // 共有辺の寸法値重複防止
    val drawnEdges = mutableSetOf<String>()
    fun edgeKey(va: Vertex, vb: Vertex): String {
        val ax = "%.3f".format(va.x); val ay = "%.3f".format(va.y)
        val bx = "%.3f".format(vb.x); val by = "%.3f".format(vb.y)
        return if ("$ax,$ay" < "$bx,$by") "$ax,$ay-$bx,$by" else "$bx,$by-$ax,$ay"
    }

    for ((_, pt) in placed) {
        // ミリ座標
        val p0  = Vertex(pt.point0.x * S, pt.point0.y * S)
        val pAB = Vertex(pt.pointAB.x * S, pt.pointAB.y * S)
        val pBC = Vertex(pt.pointBC.x * S, pt.pointBC.y * S)

        // レイヤ TRI: 3辺（白）
        dxfLines.add(DxfLine(p0.x, p0.y, pAB.x, pAB.y, 7, "TRI"))
        dxfLines.add(DxfLine(pAB.x, pAB.y, pBC.x, pBC.y, 7, "TRI"))
        dxfLines.add(DxfLine(pBC.x, pBC.y, p0.x, p0.y, 7, "TRI"))

        // レイヤ LEN: 辺長テキスト — 基線アライメント配置（appのcalcDimAngle + verticalDxf準拠）
        // 鋭角頂点(< 20°)に隣接する辺は中点から10%ずらす（Dims.autoDimHorizontalByAngle準拠）
        val SHARP = 20.0
        val angleAtPoint0  = calculateInternalAngle(pBC, p0, pAB)   // A頂点
        val angleAtPointAB = calculateInternalAngle(p0, pAB, pBC)    // B頂点
        val angleAtPointBC = calculateInternalAngle(pAB, pBC, p0)    // C頂点

        fun dimText(va: Vertex, vb: Vertex, opposite: Vertex, len: Double, shiftRatio: Double = 0.0) {
            val key = edgeKey(va, vb)
            if (!drawnEdges.add(key)) return  // 共有辺は先に描いた方だけ
            val mx = (va.x + vb.x) / 2.0 + (vb.x - va.x) * shiftRatio
            val my = (va.y + vb.y) / 2.0 + (vb.y - va.y) * shiftRatio
            val rotation = calcDimAngle(va, vb)
            val alignV = dimVerticalDxf(va, vb, opposite)
            dxfTexts.add(DxfText(mx, my, "%.2f".format(len), height = textHeight,
                rotation = rotation, color = 7, alignH = 1, alignV = alignV, layer = "LEN"))
        }

        // shiftRatio: 正=vb方向へ、負=va方向へ
        val H = 0.4
        // A辺(pAB→p0): va端=B頂点, vb端=A頂点
        val shiftA = if (angleAtPointAB < SHARP) H else if (angleAtPoint0 < SHARP) -H else 0.0
        // B辺(pBC→pAB): va端=C頂点, vb端=B頂点
        val shiftB = if (angleAtPointBC < SHARP) H else if (angleAtPointAB < SHARP) -H else 0.0
        // C辺(p0→pBC): va端=A頂点, vb端=C頂点
        val shiftC = if (angleAtPoint0 < SHARP) H else if (angleAtPointBC < SHARP) -H else 0.0

        dimText(pAB, p0, pBC, pt.a, shiftA)
        dimText(pBC, pAB, p0, pt.b, shiftB)
        dimText(p0, pBC, pAB, pt.c, shiftC)

        // レイヤ NUM: 三角形番号（青）+ 円
        val cx = (p0.x + pAB.x + pBC.x) / 3.0
        val cy = (p0.y + pAB.y + pBC.y) / 3.0
        dxfTexts.add(DxfText(cx, cy, pt.id.toString(), height = textHeight, color = 5, alignH = 1, alignV = 2, layer = "NUM"))
        dxfCircles.add(DxfCircle(cx, cy, textHeight * 0.8, color = 5, layer = "NUM"))
    }
    return Triple(dxfLines, dxfTexts, dxfCircles)
}

/** Excel 面積計算書出力 — app モジュールの XlsxWriter 準拠 */
fun writeXlsx(file: File, triangles: List<TriData>, rosenmei: String) {
    val wb = XSSFWorkbook()
    val sheet = wb.createSheet("面積計算書")
    val fmt = wb.createDataFormat()

    // スタイル定義
    val styleC = wb.createCellStyle().apply {
        alignment = HorizontalAlignment.CENTER
        borderBottom = BorderStyle.THIN
    }
    val styleTitle = wb.createCellStyle().apply {
        alignment = HorizontalAlignment.CENTER
    }
    val styleDigit = wb.createCellStyle().apply {
        alignment = HorizontalAlignment.CENTER
        borderBottom = BorderStyle.THIN
        dataFormat = fmt.getFormat("0.00")
    }

    // 列幅: 番号(B), 辺長A(C), 辺長B(D), 辺長C(E), 面積(F)
    val widths = intArrayOf(2, 12, 8, 8, 8, 12)
    widths.forEachIndexed { i, w -> sheet.setColumnWidth(i, 256 * w) }

    val rowSizer = 1.25f

    fun createStyledRow(rowNum: Int): org.apache.poi.ss.usermodel.Row {
        val row = sheet.createRow(rowNum)
        row.heightInPoints = sheet.defaultRowHeightInPoints * rowSizer
        return row
    }

    // タイトル行
    createStyledRow(1).createCell(3).apply {
        setCellValue("面 積 計 算 書"); setCellStyle(styleTitle)
    }
    // 路線名
    if (rosenmei.isNotBlank()) {
        createStyledRow(2).createCell(3).apply {
            setCellValue(rosenmei); setCellStyle(styleTitle)
        }
    }
    // ヘッダー行
    val headerRow = createStyledRow(3)
    listOf("番号", "辺長A", "辺長B", "辺長C", "面積").forEachIndexed { i, h ->
        headerRow.createCell(i + 1).apply { setCellValue(h); setCellStyle(styleC) }
    }

    // データ行（row 4〜）
    val dataStart = 4
    for ((i, tri) in triangles.withIndex()) {
        val row = createStyledRow(dataStart + i)
        val rn = dataStart + 1 + i  // Excel行番号（1-based）
        val lenA = Math.round(tri.a * 100) * 0.01
        val lenB = Math.round(tri.b * 100) * 0.01
        val lenC = Math.round(tri.c * 100) * 0.01

        row.createCell(1).apply { setCellValue(tri.id.toDouble()); setCellStyle(styleC) }
        row.createCell(2).apply { setCellValue(lenA); setCellStyle(styleDigit) }
        row.createCell(3).apply { setCellValue(lenB); setCellStyle(styleDigit) }
        row.createCell(4).apply { setCellValue(lenC); setCellStyle(styleDigit) }

        // ヘロンの公式: s = (a+b+c)/2, area = sqrt(s*(s-a)*(s-b)*(s-c))
        val sFormula = "(0.5*(C$rn+D$rn+E$rn))"
        val areaFormula = "ROUND(($sFormula*($sFormula-C$rn)*($sFormula-D$rn)*($sFormula-E$rn))^0.5,2)"
        row.createCell(5).apply { cellFormula = areaFormula; setCellStyle(styleDigit) }
    }

    // 小計行
    val sumRowNum = dataStart + triangles.size
    val sumStart = dataStart + 1
    val sumEnd = dataStart + triangles.size
    val sumRow = createStyledRow(sumRowNum)
    sumRow.createCell(1).apply { setCellValue("小計"); setCellStyle(styleC) }
    listOf(2, 3, 4).forEach { sumRow.createCell(it).apply { setCellValue(""); setCellStyle(styleC) } }
    sumRow.createCell(5).apply {
        cellFormula = "SUM(F$sumStart:F$sumEnd)"
        setCellStyle(styleDigit)
    }

    // ファイル書き込み
    FileOutputStream(file).use { out ->
        wb.write(out)
    }
    wb.close()
}
