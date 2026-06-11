package com.jpaver.trianglelist.datamanager

import com.example.trilib.PointXY
import com.jpaver.trianglelist.editmodel.ConnCode
import com.jpaver.trianglelist.editmodel.ConnParam
import com.jpaver.trianglelist.editmodel.Deduction
import com.jpaver.trianglelist.editmodel.DeductionList
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import com.jpaver.trianglelist.editmodel.setColor
import com.jpaver.trianglelist.setDimAligns
import com.jpaver.trianglelist.setPointNumber
import com.jpaver.trianglelist.viewmodel.InputParameter

/**
 * CSV の文書モデルと codec (ADR 0008)。
 *
 * これまでの「ファイル ⇄ 生きた TriangleList の直結」(読みながら半構築のリストへ副作用を
 * 順番に当てる) をやめ、間に純データの文書 (CsvDoc) を置く。タイミング由来のバグ
 * (connectionSide が入る前に自動配置が走る / 手動値が後続の add に潰される、ADR 0006 付記)
 * は全部この直結から出ていた。
 *
 * - parse:    text → CsvDoc。計算しない・構築しない。未知の列・行は生のまま保持する
 *             (schema evolution の定石: 位置は再利用しない・未知フィールドは保持して書き戻す)
 * - build:    CsvDoc → TriangleList。named phases (幾何構築 → 手動配置 → リスト回転)。
 *             手動配置は「全行の add が終わった後」に当てるので、後続の add が先行行の
 *             保存値を潰す事故が構造的に起きない
 * - bake:     構築済み TriangleList → CsvDoc (完全形式 28 列)。アプリの保存
 *             (MainActivity.writeCSV:2745-2792) と同じ列順・同じ値。web の overrides を
 *             model に適用してから bake すれば、手動配置が CSV に書き戻る (ユーザー損失の解消)
 * - serialize: CsvDoc → text
 *
 * 列定義 (writeCSV / CsvLoader.TriangleColumn と同一):
 *   0-3  番号, 辺A, 辺B, 辺C
 *   4-5  親番号, 接続コード (1=B 2=C 3-8=二重断面 9/10=フロート)
 *   6-9  測点名, 番号サークル x, y, 移動フラグ
 *   10   色
 *   11-13 寸法 horizontal a/b/c   14-16 寸法 vertical a/b/c
 *   17-19 cp.side, cp.type, cp.lcr
 *   20-21 寸法手動フラグ b/c
 *   22-25 angle, pointCA.x, pointCA.y, angleInLocal (幾何キャッシュ — 読み側は再計算する)
 *   26-27 測点 horizontal, 測点手動フラグ
 */
object CsvCodec {

    /** 三角形 1 行。chunks は trim 済みの生の列 (列0 = 番号を含む)。未知の列も保持 */
    data class CsvRow(val chunks: List<String>) {
        val number: Int get() = chunks[0].toIntOrNull() ?: -1
    }

    /**
     * CSV 文書。preLines = 最初の三角形行より前の行 (ヘッダ等)、postLines = それ以降の
     * 非三角形行 (ListScale/TextSize 等)。どちらも原文のまま保持して書き戻す。
     * ListAngle 行だけは数値として取り出す (リスト回転の SoT、ADR 0007)。
     * dedRows = "Deduction" 先頭の控除行 (ADR 0008 の残課題の昇格)。chunks は列0 =
     * "Deduction" を含む生の列で、未知の追加列 (14 列目以降) も保持して書き戻す
     */
    data class CsvDoc(
        val preLines: List<String>,
        val rows: List<CsvRow>,
        val listAngle: Float?,
        val postLines: List<String>,
        val dedRows: List<CsvRow> = emptyList(),
    )

    fun parse(text: String): CsvDoc {
        val preLines = mutableListOf<String>()
        val postLines = mutableListOf<String>()
        val rows = mutableListOf<CsvRow>()
        val dedRows = mutableListOf<CsvRow>()
        var listAngle: Float? = null
        for (line in text.lineSequence()) {
            if (line.isBlank()) continue
            val chunks = line.split(",").map { it.trim() }
            if (chunks.firstOrNull() == "ListAngle") {
                listAngle = chunks.getOrNull(1)?.toFloatOrNull() ?: listAngle
                continue
            }
            if (chunks.firstOrNull() == "Deduction") {
                dedRows.add(CsvRow(chunks))
                continue
            }
            val number = if (chunks.size >= 4) chunks[0].toIntOrNull() else null
            if (number == null || number < 0) {
                (if (rows.isEmpty()) preLines else postLines).add(line)
                continue
            }
            rows.add(CsvRow(chunks))
        }
        return CsvDoc(preLines, rows, listAngle, postLines, dedRows)
    }

    fun serialize(doc: CsvDoc): String {
        val sb = StringBuilder()
        doc.preLines.forEach { sb.append(it).append('\n') }
        doc.rows.forEach { sb.append(it.chunks.joinToString(",")).append('\n') }
        // アプリ writeCSV と同じく三角形行の後に書く。値の書式 ("ListAngle, x") も同一
        doc.listAngle?.let { sb.append("ListAngle, ").append(it).append('\n') }
        doc.postLines.forEach { sb.append(it).append('\n') }
        // アプリ writeCSV:2789-2797 と同じく末尾 (ListScale/TextSize の後) に書く
        doc.dedRows.forEach { sb.append(it.chunks.joinToString(",")).append('\n') }
        return sb.toString()
    }

    /**
     * CsvDoc → TriangleList。3 phases:
     *   1. 幾何構築 — 180° 基底で add (自動配置 setDimsUnconnectedSideToOuter を含む)
     *   2. 手動配置・メタの復元 — 全行 add の後に当てる (CsvLoader の行ごと finalize と違い、
     *      後続の子の add が先行行の保存値を潰せない)
     *   3. リスト回転 — recoverState で絶対角度へ (アプリ load 経路と同一、ADR 0007)
     */
    fun build(doc: CsvDoc): TriangleList {
        val trilist = TriangleList()
        val built = mutableListOf<Pair<CsvRow, Triangle>>()

        // phase 1: 幾何構築 (CsvLoader.buildTriangle と同じ分岐)
        for (row in doc.rows) {
            val c = row.chunks
            val lengthA = c.getOrNull(1)?.toFloatOrNull() ?: continue
            val lengthB = c.getOrNull(2)?.toFloatOrNull() ?: continue
            val lengthC = c.getOrNull(3)?.toFloatOrNull() ?: continue
            val parent = c.getOrNull(4)?.toIntOrNull() ?: -1
            val conn = c.getOrNull(5)?.toIntOrNull() ?: -1

            if (conn < 1) {
                trilist.add(Triangle(lengthA, lengthB, lengthC, PointXY(0f, 0f), 180f), true)
            } else {
                if (parent < 1 || parent > trilist.size()) continue
                val ptri = trilist.getBy(parent)
                // 完全形式 (列17-19 = cp.side/type/lcr) があれば優先 (CsvLoader.readCParamSafe と同形)
                val cpSide = c.getOrNull(17)?.toIntOrNull()
                val cpType = c.getOrNull(18)?.toIntOrNull()
                val cpLcr = c.getOrNull(19)?.toIntOrNull()
                val cp = if (cpSide != null && cpType != null && cpLcr != null) {
                    ConnParam(cpSide, cpType, cpLcr, lengthA)
                } else {
                    ConnCode.toConnParam(conn, lengthA) ?: continue
                }
                trilist.add(Triangle(ptri, cp, lengthB, lengthC), true)
            }
            val tri = trilist.getBy(trilist.size())
            tri.connectionSide = conn
            built.add(row to tri)
        }

        // phase 2: 手動配置・メタの復元 (CsvLoader.finalizeBuildTriangle と同項目)
        for ((row, tri) in built) applyRowMeta(row.chunks, tri)

        // phase 3: リスト回転。行が無い CSV も angle=0 → -180° でアプリと同じ向き
        trilist.angle = doc.listAngle ?: 0f
        trilist.recoverState(PointXY(0f, 0f))
        return trilist
    }

    /**
     * dedRows → DeductionList。アプリの CsvLoader.buildDeductions (CsvLoader.kt:369-392、
     * viewscale=1) と同値: 列 8-11 (point/pointFlag) は `PointXY(x, -y)` で Y 反転して
     * ビュー空間 (y 下向き) に戻し、列 12 が空でなければ shapeAngle。type (列 6) は
     * "Box"/"Circle" 文字列、pl は MainActivity.typeToInt:924 と同写像 (Box=1, Circle=2)。
     *
     * 回転はここでは当てない — アプリのロード経路 (MainActivity.setEditLists:2904-2920) は
     * trilist.recoverState だけで dedlist は回さない (CSV の控除座標は保存時点の絶対値)。
     * 対話回転 (fabRotate:1584-1591) は別経路で、web では行の座標書き換え
     * (WebDeduction.rotateDeductionLine) が担う
     */
    fun buildDeductions(doc: CsvDoc): DeductionList {
        val dedlist = DeductionList()
        for (row in doc.dedRows) {
            val c = row.chunks
            val num = c.getOrNull(1)?.toIntOrNull() ?: continue
            val lengthX = c.getOrNull(3)?.toFloatOrNull() ?: continue
            val lengthY = c.getOrNull(4)?.toFloatOrNull() ?: continue
            val pn = c.getOrNull(5)?.toIntOrNull() ?: 0
            val type = c.getOrNull(6) ?: ""
            val px = c.getOrNull(8)?.toFloatOrNull() ?: continue
            val py = c.getOrNull(9)?.toFloatOrNull() ?: continue
            val fx = c.getOrNull(10)?.toFloatOrNull() ?: continue
            val fy = c.getOrNull(11)?.toFloatOrNull() ?: continue
            dedlist.add(
                Deduction(
                    InputParameter(
                        c.getOrNull(2) ?: "", type, num,
                        lengthX, lengthY, 0f,
                        pn, typeToInt(type),
                        PointXY(px, -py),
                        PointXY(fx, -fy),
                    )
                )
            )
            val sa = c.getOrNull(12)
            if (!sa.isNullOrEmpty()) sa.toDoubleOrNull()?.let { dedlist.get(dedlist.size()).shapeAngle = it }
        }
        return dedlist
    }

    /** MainActivity.typeToInt:924-929 と同写像 */
    fun typeToInt(type: String): Int = when (type) {
        "Box" -> 1
        "Circle" -> 2
        else -> 0
    }

    private fun applyRowMeta(c: List<String>, tri: Triangle) {
        // 測点名 (列6) と色 (列10) — 描画には出ないが bake / XLSX 経路のため保持
        c.getOrNull(6)?.let { if (it.isNotEmpty()) tri.name = it }
        c.getOrNull(10)?.toIntOrNull()?.let { tri.setColor(it) }
        // 番号サークル位置 (列7-9、ユーザー移動時のみ)。座標は絶対値で、flag=true が
        // recoverState の回転対象から外す
        if (c.getOrNull(9)?.toBoolean() == true) {
            val px = c.getOrNull(7)?.toFloatOrNull()
            val py = c.getOrNull(8)?.toFloatOrNull()
            if (px != null && py != null) tri.setPointNumber(PointXY(px, py), true)
        }
        // 寸法アライメント (列11-16)
        val aligns = (11..16).map { c.getOrNull(it)?.toIntOrNull() }
        if (aligns.all { it != null }) {
            tri.setDimAligns(aligns[0]!!, aligns[1]!!, aligns[2]!!, aligns[3]!!, aligns[4]!!, aligns[5]!!)
        }
        // 寸法の手動フラグ (列20-21)
        if (c.getOrNull(21) != null) {
            tri.dim.flag[1].isMovedByUser = c.getOrNull(20)?.toBoolean() ?: false
            tri.dim.flag[2].isMovedByUser = c.getOrNull(21)?.toBoolean() ?: false
        }
        // 測点アライメント (列26-27)
        c.getOrNull(26)?.toIntOrNull()?.let { tri.dim.horizontal.s = it }
        c.getOrNull(27)?.let { tri.dim.flagS.isMovedByUser = it.toBoolean() }
    }

    /**
     * 構築済み model → 完全形式 28 列の CsvDoc。列順・値はアプリの保存
     * (MainActivity.writeCSV:2760-2776) と同一 — web が書く CSV をアプリで開いても
     * 手動配置 (寸法フリップ・番号移動) が失われない。
     * preLines/postLines は元文書から引き継ぐ (ヘッダ・Deduction 等の素通し)
     */
    fun bake(trilist: TriangleList, original: CsvDoc): CsvDoc {
        val rows = (1..trilist.size()).map { i ->
            val mt = trilist.getBy(i)
            val pn = mt.pointnumber
            val cp = ConnCode.toConnParam(mt.connectionSide, mt.lengthNotSized[0], mt.cParam_.lcr)
                ?: mt.cParam_
            CsvRow(
                listOf(
                    "${mt.mynumber}", "${mt.lengthA_}", "${mt.lengthB_}", "${mt.lengthC_}",
                    "${mt.parentnumber}", "${mt.connectionSide}",
                    mt.name, "${pn.x}", "${pn.y}", "${mt.pointNumber.flag.isMovedByUser}",
                    "${mt.mycolor}",
                    "${mt.dim.horizontal.a}", "${mt.dim.horizontal.b}", "${mt.dim.horizontal.c}",
                    "${mt.dim.vertical.a}", "${mt.dim.vertical.b}", "${mt.dim.vertical.c}",
                    "${cp.side}", "${cp.type}", "${cp.lcr}",
                    "${mt.dim.flag[1].isMovedByUser}", "${mt.dim.flag[2].isMovedByUser}",
                    "${mt.angle}", "${mt.pointCA.x}", "${mt.pointCA.y}", "${mt.angleInLocal_}",
                    "${mt.dim.horizontal.s}", "${mt.dim.flagS.isMovedByUser}",
                )
            )
        }
        // dedRows は素通し (控除は TriangleList の外。web の編集は行の置換/追加で dedRows 自体を更新する)
        return CsvDoc(original.preLines, rows, trilist.angle, original.postLines, original.dedRows)
    }
}
