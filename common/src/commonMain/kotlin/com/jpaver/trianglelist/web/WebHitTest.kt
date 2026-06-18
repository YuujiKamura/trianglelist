package com.jpaver.trianglelist.web

import com.example.trilib.PointXY
import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.CycleShape
import com.jpaver.trianglelist.editmodel.Rectangle
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.isCollide

/**
 * Web 段階2c (task #11) → 2026-06-16 mixed 化: タップ点 (モデル座標) → 図形通し番号
 * (1-based、0 = 無し)。
 *
 * SoT 一本化 (2026-06-16): WebPrimitiveRenderer / WebOverrides / WebDrawingExport が
 * mixed EditList<CycleShape> 経由で動くようになった (Codex insight 126-128) のに合わせ、
 * UI のタップ→選択経路も mixed 番号に揃える。これで __tlcp/select?n=<mixed> と UI タップが
 * 同じ番号で動く ── render の tri:N と UI の current/selected が常に一致する。
 *
 * 戻り値: CsvCodec.buildMixed が組む混在 EditList の 1-based 通し番号 (figureRows 出現順)。
 * Triangle / Rectangle の両方をヒット対象にする (= UI クリックで Rectangle も選択できる)。
 *
 * 内外判定:
 *   - Triangle: 既存 Triangle.isCollide (TriangleExtensions.kt:292、符号判定)
 *   - Rectangle: vertices() の [bl, br, tr, tl] を bl-tr 対角線で 2 三角形に分割し
 *     PointXY.isCollide(ab, bc, ca) で判定。WebPrimitiveRenderer の fill 分割
 *     (WebPrimitiveRenderer.kt:130-140) と同型で、Rectangle 用に新規幾何コードを増やさない。
 *
 * 座標は renderCsvToPrimitives(scale=1) が返すプリミティブと同じモデル座標系 (y 上向き)。
 * px → モデル座標の逆変換は JS 側 ViewTransform の責務。
 */
object WebHitTest {

    fun hitTriangle(csv: String, x: Float, y: Float): Int {
        val doc = CsvCodec.parse(csv)
        val trilist = CsvCodec.build(doc)
        val mixed = CsvCodec.buildMixed(doc, trilist, 1f)
        val tap = PointXY(x, y)
        for (i in 1..mixed.size()) {
            if (hits(mixed.get(i), tap)) return i
        }
        return 0
    }

    private fun hits(obj: CycleShape, tap: PointXY): Boolean = obj.containsPoint(tap)
}
