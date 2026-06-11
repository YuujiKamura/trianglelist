package com.jpaver.trianglelist.web

import com.example.trilib.PointXY
import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.Deduction
import com.jpaver.trianglelist.viewmodel.InputParameter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * placeDeduction / rotateDeductionLine の pin。
 * 期待値の根拠 = アプリの実コード:
 * - MainActivity.flagDeduction:1773-1820 (タップ位置 / 形状自動判定 / isCollide / flag(parent))
 * - MainActivity.validDeduction:1190-1202
 * - MainActivity.writeCSV:2790-2797 (13 列、Y 反転)
 * - MainActivity.fabRotate:1584-1591 (dedlist は -degrees、ビュー空間)
 */
class WebDeductionTest {

    private val csv = "1,6.0,5.0,4.0,-1,-1\n"

    /** 三角形 1 の内点 (モデル座標)。WebHitTest と同じ経路で内側であることも確認 */
    private fun interiorPoint(): Pair<Float, Float> {
        val tri = WebCsvReader.read(csv).getBy(1)
        val cx = ((tri.point[0].x + tri.pointAB.x + tri.pointBC.x) / 3.0).toFloat()
        val cy = ((tri.point[0].y + tri.pointAB.y + tri.pointBC.y) / 3.0).toFloat()
        assertEquals(1, WebHitTest.hitTriangle(csv, cx, cy))
        return cx to cy
    }

    @Test
    fun place_circle_inside_triangle_pins_parent_flag_and_signs() {
        val (cx, cy) = interiorPoint()
        val line = WebDeduction.placeDeduction(csv, cx, cy, "仕切弁", 0.23f, 0f, 1)
        val c = line.split(",")
        assertEquals("Deduction", c[0])
        assertEquals("1", c[1])
        assertEquals("仕切弁", c[2])
        assertEquals("1", c[5]) // pn = isCollide → 三角形 1
        assertEquals("Circle", c[6]) // 寸法2 = 0 → 円 (MainActivity:1778-1779)
        // 位置はクリック点そのまま (モデル座標で書き戻る = Y 反転が往復で消える)
        assertEquals(cx.toDouble(), c[8].toDouble(), 1e-4)
        assertEquals(cy.toDouble(), c[9].toDouble(), 1e-4)

        // pointFlag / shapeAngle はアプリと同式 (Deduction.flag:249-256 をビュー空間で) の値
        val tri = WebCsvReader.read(csv).getBy(1)
        val expected = Deduction(
            InputParameter("仕切弁", "Circle", 1, 0.23f, 0f, 0f, 1, 2, PointXY(cx, -cy), PointXY(0f, 0f))
        )
        expected.flag(tri)
        val pfModel = expected.pointFlag.scale(1.0, -1.0)
        assertEquals(pfModel.x, c[10].toDouble(), 1e-4)
        assertEquals(pfModel.y, c[11].toDouble(), 1e-4)
        assertEquals(expected.shapeAngle, c[12].toDouble(), 1e-4)

        // 行を CSV に足すと codec が同じ値で読み戻す (web 描画経路の入口)
        val dedlist = CsvCodec.buildDeductions(CsvCodec.parse(csv + line + "\n"))
        assertEquals(1, dedlist.size())
        assertEquals(cx.toDouble(), dedlist.get(1).point.x, 1e-4)
        assertEquals(-cy.toDouble(), dedlist.get(1).point.y, 1e-4) // ビュー空間 (y 下向き)
    }

    @Test
    fun place_box_autodetects_type_and_writes_shape_angle() {
        val (cx, cy) = interiorPoint()
        val line = WebDeduction.placeDeduction(csv, cx, cy, "集水桝", 0.8f, 0.6f, 1)
        val c = line.split(",")
        assertEquals("Box", c[6]) // 寸法2 > 0 → 長方形
        assertEquals("0.8", c[3])
        assertEquals("0.6", c[4])
        // 親あり → shapeAngle = 親の未接続辺の角度 (flag:253)。値は flag と同式で一致する
        val tri = WebCsvReader.read(csv).getBy(1)
        val expected = Deduction(
            InputParameter("集水桝", "Box", 1, 0.8f, 0.6f, 0f, 1, 1, PointXY(cx, -cy), PointXY(0f, 0f))
        )
        expected.flag(tri)
        assertEquals(expected.shapeAngle, c[12].toDouble(), 1e-4)
    }

    @Test
    fun place_outside_triangle_is_allowed_with_pn_zero() {
        // アプリ addDeductionBy はpn=0 (親なし) でも追加する。旗揚げは走らないので pointFlag は (0,0)
        val line = WebDeduction.placeDeduction(csv, 100f, 100f, "汚水", 0.3f, 0f, 1)
        val c = line.split(",")
        assertEquals("0", c[5])
        assertEquals(0.0, c[10].toDouble(), 1e-9)
        assertEquals(0.0, c[11].toDouble(), 1e-9) // (0,0) の Y 反転は -0.0 になり得るので数値比較
    }

    @Test
    fun place_rejects_invalid_params_like_app_validDeduction() {
        val (cx, cy) = interiorPoint()
        assertEquals("", WebDeduction.placeDeduction(csv, cx, cy, "", 0.23f, 0f, 1)) // 名前空
        assertEquals("", WebDeduction.placeDeduction(csv, cx, cy, "X", 0.05f, 0f, 1)) // a < 0.1
        assertEquals("", WebDeduction.placeDeduction(csv, cx, cy, "X", 0.5f, 0.05f, 1)) // Box で b < 0.1
        assertEquals("", WebDeduction.placeDeduction(csv, 0f, 0f, "X", 0.5f, 0f, 1)) // 位置未指定 (0,0)
    }

    @Test
    fun rotate_line_follows_triangle_rotation_in_model_space() {
        // 三角形は listAngle +90 でモデル空間 +90 (CCW) 回る (recoverState)。控除も同じ向き:
        // アプリの dedlist.rotate(origin, -degrees) はビュー空間 (y 下向き) なので、
        // モデル座標では +degrees CCW — model (1,0) → (0,1)、(2,0) → (0,2)
        val line = "Deduction,1,仕切弁,0.23,0.0,1,Circle,0.0,1.0,0.0,2.0,0.0,0.0"
        val out = WebDeduction.rotateDeductionLine(line, 90f)
        val c = out.split(",")
        assertEquals(0.0, c[8].toDouble(), 1e-4)
        assertEquals(1.0, c[9].toDouble(), 1e-4)
        assertEquals(0.0, c[10].toDouble(), 1e-4)
        assertEquals(2.0, c[11].toDouble(), 1e-4)
    }

    @Test
    fun rotate_box_updates_shape_angle_and_keeps_unknown_columns() {
        // Box は rotateShape が shapeAngle += (-degrees) (Deduction.kt:211-219、ビュー空間)
        val line = "Deduction,2,集水桝,0.8,0.6,1,Box,0.0,1.0,0.0,2.0,0.0,10.0,FUTURE"
        val out = WebDeduction.rotateDeductionLine(line, 90f)
        val c = out.split(",")
        assertEquals(-80.0, c[12].toDouble(), 1e-4) // 10 + (-90)
        assertEquals("FUTURE", c[13]) // 未知列の保持
        // 非 Deduction 行・壊れた行はそのまま返す
        assertEquals("1,3.0,3.0,3.0,-1,-1", WebDeduction.rotateDeductionLine("1,3.0,3.0,3.0,-1,-1", 90f))
    }

    @Test
    fun renderer_emits_ded_primitives_from_csv() {
        val (cx, cy) = interiorPoint()
        val line = WebDeduction.placeDeduction(csv, cx, cy, "仕切弁", 0.23f, 0f, 1)
        val json = WebPrimitiveRenderer.renderCsv(csv + line + "\n", 1.0f)
        // 円: 中心 = クリック点 (モデル座標)、半径 = lengthX/2 (DxfFileWriter:270)
        assertTrue(json.contains(""""layer":"ded""""), "ded layer prims expected: $json")
        val circle = Regex(""""type":"circle","layer":"ded","cx":([-0-9.E]+),"cy":([-0-9.E]+),"r":([-0-9.E]+)""")
            .find(json)
        assertTrue(circle != null, "ded circle expected: $json")
        assertEquals(cx.toDouble(), circle!!.groupValues[1].toDouble(), 1e-4)
        assertEquals(cy.toDouble(), circle.groupValues[2].toDouble(), 1e-4)
        assertEquals(0.115, circle.groupValues[3].toDouble(), 1e-4)
        // infoStr テキスト (左寄せマーク alignH:0 付き)
        assertTrue(json.contains(""""alignH":0"""))
        // 控除なしの CSV では従来出力と完全同一 (回 regression 防止)
        assertEquals(
            WebPrimitiveRenderer.renderCsv(csv, 1.0f),
            json.replace(Regex(""",\{[^{}]*"layer":"ded"[^{}]*\}"""), ""),
        )
    }
}
