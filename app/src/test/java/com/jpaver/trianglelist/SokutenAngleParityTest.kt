package com.jpaver.trianglelist

import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.dxf.DxfParser
import com.jpaver.trianglelist.label.DimensionLayout
import com.jpaver.trianglelist.web.WebDrawingExport
import org.junit.Assert.fail
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

/**
 * 測点名テキストの「向き」が画面と書き出し (DXF/SFC/PDF) で一致することの gate。
 *
 * 2026-08-27 実機報告「アプリビュー画面と DXF とで測点テキストの向きが違う」。
 * 両者は同じ DimensionPlacement (pointA/pointB) を消費しながら、角度の作り方が違う:
 *
 *   画面 (MyView.kt:667): canvas.drawTextOnPath(name, makePath(placeS), ...)
 *     makePath (MyView.kt:918-924) は (x, -y) で Path を張る。Android canvas は y 下向きなので
 *     y の反転が 2 回効いて打ち消し、見た目の向き = モデル空間の pointA -> pointB 方向。
 *
 *   書き出し (DrawingFileWriter.kt:303): pointB.calcSokAngle(pointA, sokutenListVector)
 *     calcSokAngle (PointXY.kt:285-293) は受け手が pointB・引数が pointA なので
 *     pointB -> pointA 方向 (= 画面と逆) を返し、さらに測点番号が降順 (vector<0) のとき
 *     180 度反転する。画面側にこの反転は無い。
 *
 * 結果、測点番号が昇順の通常ケースで画面と DXF が常に 180 度食い違う。
 * どちらを正とするかは別途決めるが、「一致していること」は種別を問わず不変条件。
 */
class SokutenAngleParityTest {

    /**
     * 文字高さ。画面は setDimPathTextSize が入れた実サイズ、書き出しは textscale_ を渡す
     * (DrawingFileWriter.kt:293)。ここは両者共通の代表値。0 だと旗線が潰れて向きが
     * 定義できなくなる (別 gate: SokutenTextPositionViewerTest) ので必ず正の値を使う。
     */
    private val TEXT_HEIGHT = 0.25

    private fun csv(names: List<String>): String = buildString {
        appendLine("市道○○号線 舗装打換工事")
        appendLine("市道○○号線")
        appendLine("○○建設株式会社")
        appendLine("1/1")
        appendLine("1,6.0,5.0,4.0,-1,-1,${names[0]}")
        appendLine("2,5.0,4.0,3.0,1,1,${names[1]}")
        appendLine("3,4.0,3.5,3.0,1,2,${names[2]}")
    }

    /** 測点番号の昇順 / 降順 = sokutenListVector の符号を振る 2 ケース */
    private val orders = mapOf(
        "昇順" to listOf("No.1", "No.2", "No.3"),
        "降順" to listOf("No.3", "No.2", "No.1"),
    )

    @Test
    fun `測点名の向きは画面と書き出しで一致する`() {
        val failures = mutableListOf<String>()

        for ((label, names) in orders) {
            val source = csv(names)
            val trilist = CsvCodec.build(CsvCodec.parse(source))
            val vector = trilist.sokutenListVector

            // 書き出し側は式を写さず、実際に生成した DXF から TEXT の回転角 (group 50) を読む。
            // 式を二重持ちすると writer だけ直したときテストが古い式のまま通ってしまう。
            // 図面は中心寄せで平行移動されるだけ (回転なし・等方スケール) なので角度は比較可能。
            val parsed = DxfParser().parse(WebDrawingExport.buildDxfText(source))

            for (i in 1..trilist.size()) {
                val tri = trilist.get(i)
                val place = DimensionLayout.layout(
                    tri.pointAB, tri.point[0],
                    DimensionLayout.SIDE_SOKUTEN, tri.dim.horizontal.s,
                    tri.scaleFactor.toDouble(), TEXT_HEIGHT, 0.0, tri.name
                )
                val a = place.pointA
                val b = place.pointB

                // 画面: makePath + drawTextOnPath の見た目の向き = モデル空間の A -> B
                val screenDeg = atan2(b.y - a.y, b.x - a.x) * 180.0 / PI

                val hits = parsed.texts.filter { it.text == tri.name && it.layer == "0" }
                if (hits.size != 1) {
                    failures += "$label #$i '${tri.name}': DXF 中の測点名 TEXT が ${hits.size} 個 (期待 1)"
                    continue
                }
                val writerDeg = hits.single().rotation

                val diff = angleDelta(writerDeg, screenDeg)
                if (diff > 1e-3) {
                    failures += "$label vector=$vector #$i '${tri.name}': " +
                        "画面=%.3f DXF=%.3f 差=%.3f 度".format(screenDeg, writerDeg, diff)
                }
            }
        }

        if (failures.isNotEmpty()) {
            fail("測点名の向きが画面と書き出しで食い違うケース ${failures.size} 件:\n" + failures.joinToString("\n"))
        }
    }

    /** 2 角の差を [0,180] に畳む (360 度の巻きを吸収、180 度ずれは 180 のまま残す) */
    private fun angleDelta(x: Double, y: Double): Double {
        val d = ((x - y) % 360.0 + 360.0) % 360.0
        return if (d > 180.0) 360.0 - d else d
    }
}
