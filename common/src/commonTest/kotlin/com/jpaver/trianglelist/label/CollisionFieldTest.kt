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
        field.addBox("b1", LabelBox(PointXY(0f, 0f), widthMm = 2f, heightMm = 2f))   // 重なる → ヒット
        field.addBox("b2", LabelBox(PointXY(20f, 20f), widthMm = 2f, heightMm = 2f)) // 遠い → 非ヒット

        val hits = field.query(LabelBox(PointXY(0f, 0f), widthMm = 2f, heightMm = 2f))

        assertEquals(2, hits.size, "重なる障害物 2 件だけが返るはず: $hits")
        assertTrue(hits.contains(Hit("e1", ObstacleKind.EDGE)), "辺 e1 が EDGE 種別で返るはず: $hits")
        assertTrue(hits.contains(Hit("b1", ObstacleKind.LABEL)), "ラベル b1 が LABEL 種別で返るはず: $hits")
    }

    @Test
    fun `空フィールドへの query は空リストを返す`() {
        val field = CollisionField()
        val hits = field.query(LabelBox(PointXY(0f, 0f), widthMm = 2f, heightMm = 2f))
        assertTrue(hits.isEmpty(), "障害物 0 件なら空のはず: $hits")
    }

    @Test
    fun `excludeId で自分自身を除外できる`() {
        val field = CollisionField()
        val self = LabelBox(PointXY(0f, 0f), widthMm = 2f, heightMm = 2f)
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
        field.addBox("label", LabelBox(PointXY(0f, 0f), widthMm = 4f, heightMm = 4f))

        val hits = field.query(LabelBox(PointXY(0f, 0f), widthMm = 1f, heightMm = 1f))

        assertEquals(2, hits.size, "辺とラベルの両方がヒットするはず: $hits")
        assertTrue(hits.any { it.kind == ObstacleKind.EDGE })
        assertTrue(hits.any { it.kind == ObstacleKind.LABEL })
    }
}
