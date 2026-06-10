package com.jpaver.trianglelist.label

import com.example.trilib.PointXY
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CollisionFieldTest {

    @Test
    fun `重なる障害物だけを種別付きの Hit で返す`() {
        val field = CollisionField()
        field.addEdge("e1", PointXY(-3f, 0f), PointXY(3f, 0f))   // 原点を貫く辺 → ヒット
        field.addEdge("e2", PointXY(-3f, 10f), PointXY(3f, 10f)) // 遠い辺 → 非ヒット
        field.addBox("b1", LabelBox(PointXY(0f, 0f), widthMm = 2.0, heightMm = 2.0))   // 重なる → ヒット
        field.addBox("b2", LabelBox(PointXY(20f, 20f), widthMm = 2.0, heightMm = 2.0)) // 遠い → 非ヒット

        val hits = field.query(LabelBox(PointXY(0f, 0f), widthMm = 2.0, heightMm = 2.0))

        assertEquals(2, hits.size, "重なる障害物 2 件だけが返るはず: $hits")
        assertTrue(hits.any { it.id == "e1" && it.kind == ObstacleKind.EDGE }, "辺 e1 が EDGE 種別で返るはず: $hits")
        assertTrue(hits.any { it.id == "b1" && it.kind == ObstacleKind.LABEL }, "ラベル b1 が LABEL 種別で返るはず: $hits")
    }

    @Test
    fun `空フィールドへの query は空リストを返す`() {
        val field = CollisionField()
        val hits = field.query(LabelBox(PointXY(0f, 0f), widthMm = 2.0, heightMm = 2.0))
        assertTrue(hits.isEmpty(), "障害物 0 件なら空のはず: $hits")
    }

    @Test
    fun `excludeId で自分自身を除外できる`() {
        val field = CollisionField()
        val self = LabelBox(PointXY(0f, 0f), widthMm = 2.0, heightMm = 2.0)
        field.addBox("self", self)

        val withoutExclude = field.query(self)
        assertEquals(1, withoutExclude.size, "除外なしなら自分自身がヒットするはず: $withoutExclude")

        val withExclude = field.query(self, excludeId = "self")
        assertTrue(withExclude.isEmpty(), "自分自身を除外したら空のはず: $withExclude")
    }

    @Test
    fun `辺もラベルも重なれば両方ヒットする`() {
        val field = CollisionField()
        field.addEdge("edge", PointXY(0f, -3f), PointXY(0f, 3f))
        field.addBox("label", LabelBox(PointXY(0f, 0f), widthMm = 4.0, heightMm = 4.0))

        val hits = field.query(LabelBox(PointXY(0f, 0f), widthMm = 1.0, heightMm = 1.0))

        assertEquals(2, hits.size, "辺とラベルの両方がヒットするはず: $hits")
        assertTrue(hits.any { it.kind == ObstacleKind.EDGE })
        assertTrue(hits.any { it.kind == ObstacleKind.LABEL })
    }

    @Test
    fun `Hit はめり込み深さを持ち境界接触は深さ 0 になる`() {
        val field = CollisionField()
        field.addEdge("cross", PointXY(-3f, 0f), PointXY(3f, 0f)) // box 中央を貫く → 押し出し量 1
        field.addBox("kiss", LabelBox(PointXY(2f, 0f), widthMm = 2.0, heightMm = 2.0)) // x=1 で接触 → 0

        val hits = field.query(LabelBox(PointXY(0f, 0f), widthMm = 2.0, heightMm = 2.0))

        val cross = hits.single { it.id == "cross" }
        assertEquals(1.0, cross.depthMm, 1e-3, "貫通辺の深さは押し出し量 1 のはず: $cross")
        val kiss = hits.single { it.id == "kiss" }
        assertEquals(0.0, kiss.depthMm, 1e-3, "境界接触の深さは 0 のはず: $kiss")
    }

    @Test
    fun `addCircle した円は CIRCLE 種別で深さ付きにヒットする`() {
        val field = CollisionField()
        field.addCircle("near", PointXY(2f, 0f), radiusMm = 1.5) // box 右辺 x=1 まで距離 1 → 深さ 0.5
        field.addCircle("far", PointXY(10f, 10f), radiusMm = 1.5)

        val hits = field.query(LabelBox(PointXY(0f, 0f), widthMm = 2.0, heightMm = 2.0))

        assertEquals(1, hits.size, "近い円だけがヒットするはず: $hits")
        val hit = hits.single()
        assertEquals("near", hit.id)
        assertEquals(ObstacleKind.CIRCLE, hit.kind)
        assertEquals(0.5, hit.depthMm, 1e-3, "深さ = r - クランプ距離 = 1.5 - 1: $hit")
    }

    @Test
    fun `queryCircle は辺・ラベル・円のすべてに閉形式で判定する`() {
        val field = CollisionField()
        field.addEdge("edge", PointXY(0f, 2f), PointXY(4f, 2f))                    // 最短距離 2 → 深さ 0.5
        field.addBox("box", LabelBox(PointXY(3f, 0f), widthMm = 2.0, heightMm = 2.0)) // 最近点 (2,0) 距離 2 → 深さ 0.5
        field.addCircle("circle", PointXY(0f, -3f), radiusMm = 1.0)                 // 中心距離 3 ≤ 2.5+1 → 深さ 0.5
        field.addEdge("farEdge", PointXY(10f, 10f), PointXY(20f, 10f))

        val hits = field.queryCircle(PointXY(0f, 0f), radiusMm = 2.5)

        assertEquals(3, hits.size, "近い 3 障害物だけがヒットするはず: $hits")
        assertEquals(0.5, hits.single { it.id == "edge" }.depthMm, 1e-3)
        assertEquals(0.5, hits.single { it.id == "box" }.depthMm, 1e-3)
        assertEquals(0.5, hits.single { it.id == "circle" }.depthMm, 1e-3)
        assertEquals(ObstacleKind.CIRCLE, hits.single { it.id == "circle" }.kind)
    }

    @Test
    fun `queryCircle でも excludeId で自分自身を除外できる`() {
        val field = CollisionField()
        field.addCircle("self", PointXY(0f, 0f), radiusMm = 2.0)

        assertEquals(1, field.queryCircle(PointXY(0f, 0f), radiusMm = 2.0).size)
        assertTrue(field.queryCircle(PointXY(0f, 0f), radiusMm = 2.0, excludeId = "self").isEmpty())
    }
}
