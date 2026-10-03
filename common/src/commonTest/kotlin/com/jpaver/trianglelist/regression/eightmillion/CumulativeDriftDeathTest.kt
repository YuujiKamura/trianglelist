package com.jpaver.trianglelist.regression.eightmillion

import com.example.trilib.PointXY
import com.jpaver.trianglelist.datamanager.CsvCodec
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 【エイトミリオンウェイストゥダイ：反復疲労・累積浮動小数点ドリフト編】
 *
 * 1,000回の微小回転、50回の連続シリアライズ・デシリアライズ往復など、
 * 単発テストでは露見しない「徐々に歪んで崩壊する」サイレントドリフトを追跡する。
 */
class CumulativeDriftDeathTest {

    @Test
    fun `Death_31_1000回微小回転_360度一周したときの形状と面積ドリフト限界`() {
        val list = TriangleList(Triangle(6f, 5f, 4f))
        val child = Triangle(5f, 4f, 3f)
        child.parentnumber = 1
        child.connectionSide = 1
        list.add(child, true)

        val initialArea = list.getBy(1).getArea() + list.getBy(2).getArea()

        // 0.36度 × 1000回 = 360度 (ぴったり一周)
        val stepAngle = 0.36f
        val origin = PointXY(0.0, 0.0)

        for (i in 1..1000) {
            list.rotate(origin, stepAngle, 0, false)
        }

        // 面積がドリフトしていないこと
        val finalArea = list.getBy(1).getArea() + list.getBy(2).getArea()
        assertTrue(
            abs(initialArea - finalArea) < 0.1f,
            "Area drifted excessively after 1000 rotations! Initial=$initialArea, Final=$finalArea"
        )

        // 辺長が変形していないこと
        val t1 = list.getBy(1)
        assertEquals(6f, t1.lengthA_, 0.01f)
        assertEquals(5f, t1.lengthB_, 0.01f)
        assertEquals(4f, t1.lengthC_, 0.01f)
    }

    @Test
    fun `Death_32_50回連続シリアライズ往復_保存と復元を50回繰り返しても面積が失われない`() {
        var currentList = TriangleList(Triangle(6f, 5f, 4f))
        val child1 = Triangle(5f, 4f, 3f)
        child1.parentnumber = 1
        child1.connectionSide = 1
        currentList.add(child1, true)

        val child2 = Triangle(4f, 3.5f, 3f)
        child2.parentnumber = 2
        child2.connectionSide = 1
        currentList.add(child2, true)

        fun calcArea(l: TriangleList): Float = (1..l.size()).sumOf { l.getBy(it).getArea().toDouble() }.toFloat()

        val initialArea = calcArea(currentList)
        val initialSize = currentList.size()

        // 50回連続で Bake -> Serialize -> Parse -> Build
        val emptyDoc = CsvCodec.CsvDoc(emptyList(), null, emptyList())
        for (round in 1..50) {
            val bakedDoc = CsvCodec.bake(currentList, emptyDoc)
            val csvText = CsvCodec.serialize(bakedDoc)
            val parsedDoc = CsvCodec.parse(csvText)
            currentList = CsvCodec.build(parsedDoc, applyRecoverState = false)
        }

        assertEquals(initialSize, currentList.size(), "Size must not change after 50 roundtrips")
        val finalArea = calcArea(currentList)
        assertTrue(
            abs(initialArea - finalArea) < 0.05f,
            "Total area drifted across 50 serializations! Initial=$initialArea, Final=$finalArea"
        )
    }
}
