package com.jpaver.trianglelist.web

import com.example.trilib.PointXY
import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.Deduction
import com.jpaver.trianglelist.viewmodel.InputParameter

/**
 * Web の控除 (Deduction) 配置・回転の幾何 — TS には幾何を書かない (brief 設計、
 * hitTriangle と同型の stateless API)。
 *
 * 正はアプリの実コード:
 * - 配置: MainActivity.addDeductionBy:1752 → flagDeduction:1773-1820
 *   (タップ位置がカーソル / 形状自動判定 b>0→Box / 親 = trilist.isCollide / ded.flag(parent))
 * - 検証: MainActivity.validDeduction:1190-1202 (名前非空、a>=0.1、Box は b>=0.1)
 * - CSV 書式: MainActivity.writeCSV:2790-2797 の 13 列。point/pointFlag は
 *   `scale(1/viewscale, -1/viewscale)` で Y 反転して書く (web は viewscale=1)
 *
 * 座標系: 引数 (x, y) は renderCsvToPrimitives / hitTriangle と同じモデル座標 (y 上向き)。
 * アプリの Deduction はビュー空間 (y 下向き) で持つので、内部では PointXY(x, -y) に
 * 変換してから app と同じ式を踏む。
 */
object WebDeduction {

    /**
     * クリック位置 (モデル座標) に控除を配置し、完成した 13 列 Deduction CSV 行を返す。
     * 不正パラメータ (validDeduction 違反 / 原点 = 位置未指定) は空文字列。
     * 親無し (pn=0) でも追加は許す (アプリ addDeductionBy 同様)。
     */
    fun placeDeduction(csv: String, x: Float, y: Float, name: String, lenX: Float, lenY: Float, num: Int): String {
        // validDeduction:1190 (名前非空 + a>=0.1 + Box は b>=0.1)
        if (name.isEmpty() || lenX < 0.1f) return ""
        if (lenY > 0f && lenY < 0.1f) return ""
        // flagDeduction:1775 — (0,0) は「先に位置をタップしてから」の拒否
        if (x == 0f && y == 0f) return ""

        val trilist = WebCsvReader.read(csv)
        // 形状の自動判定 (1778-1779)
        val type = if (lenY > 0f) "Box" else "Circle"
        // アプリの ded はビュー空間 (y 下向き)。モデル座標 → ビュー座標 (viewscale=1)
        val pointView = PointXY(x, -y)
        // 親三角形の判定 (1782-1789): isCollide はモデル座標 = point.scale(1,-1)
        val pn = trilist.isCollide(PointXY(x, y))

        val ded = Deduction(
            InputParameter(name, type, num, lenX, lenY, 0f, pn, CsvCodec.typeToInt(type), pointView, PointXY(0f, 0f))
        )
        // 旗揚げ位置と角度 (1793-1805): 親の未接続辺上の点 + その辺の角度
        if (pn != 0) ded.flag(trilist.getBy(pn))

        return serializeDeduction(ded)
    }

    /**
     * 全体回転 (web fabRotate) への控除の連動。アプリ MainActivity.fabRotate:1584-1591 は
     * trilist.rotate(+degrees) と同時に dedlist.rotate(origin, -degrees) を当てる
     * (ded はビュー空間 y 下向きなので符号が反転して見た目は同方向)。
     * web は CSV 行 (モデル座標) が SoT なので、行を読み戻して同じ回転を適用して書き直す。
     * 14 列目以降の未知列は保持。読めない行はそのまま返す (壊さない)
     */
    fun rotateDeductionLine(line: String, degrees: Float): String {
        val chunks = line.split(",").map { it.trim() }
        if (chunks.firstOrNull() != "Deduction" || chunks.size < 13) return line
        val doc = CsvCodec.CsvDoc(emptyList(), null, emptyList(), listOf(CsvCodec.CsvRow(chunks)))
        val dedlist = CsvCodec.buildDeductions(doc)
        if (dedlist.size() < 1) return line
        val ded = dedlist.get(1)
        // ビュー空間で -degrees (アプリ 1587 と同符号)。Box は rotateShape が shapeAngle も回す
        ded.rotate(PointXY(0f, 0f), (-degrees).toDouble())
        val extras = if (chunks.size > 13) "," + chunks.drop(13).joinToString(",") else ""
        return serializeDeduction(ded) + extras
    }

    /**
     * 選択控除の個別回転 (控除モードの rot FAB)。アプリ MainActivity.fabRotate の ded 分岐
     * :1593-1600 — 控除自身の中心 (point) 回りに rotateShape だけ当てる。位置・旗は不動、
     * 回るのは Box の shapeAngle のみ (Circle は Deduction.rotateShape の type ガードで無変化)。
     * 符号はアプリと同じ -degrees (ビュー空間)。14 列目以降の未知列は保持
     */
    fun rotateDeductionShape(line: String, degrees: Float): String {
        val chunks = line.split(",").map { it.trim() }
        if (chunks.firstOrNull() != "Deduction" || chunks.size < 13) return line
        val doc = CsvCodec.CsvDoc(emptyList(), null, emptyList(), listOf(CsvCodec.CsvRow(chunks)))
        val dedlist = CsvCodec.buildDeductions(doc)
        if (dedlist.size() < 1) return line
        val ded = dedlist.get(1)
        ded.rotateShape(ded.point, (-degrees).toDouble())
        val extras = if (chunks.size > 13) "," + chunks.drop(13).joinToString(",") else ""
        return serializeDeduction(ded) + extras
    }

    /** MainActivity.writeCSV:2795 と同列順 (viewscale=1、point/pointFlag は Y 反転で書く) */
    private fun serializeDeduction(ded: Deduction): String {
        val p = ded.point.scale(1.0, -1.0)
        val pf = ded.pointFlag.scale(1.0, -1.0)
        return "Deduction,${ded.num},${ded.name},${ded.lengthX},${ded.lengthY},${ded.overlap_to}," +
            "${ded.type},${ded.angle},${p.x},${p.y},${pf.x},${pf.y},${ded.shapeAngle}"
    }
}
