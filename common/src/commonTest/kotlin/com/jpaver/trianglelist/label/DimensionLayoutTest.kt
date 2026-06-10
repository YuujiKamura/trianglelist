package com.jpaver.trianglelist.label

import com.example.trilib.PointXY
import kotlin.test.Test
import kotlin.test.assertEquals

class DimensionLayoutTest {

    private val eps = 1e-4

    private fun assertPoint(expectedX: Double, expectedY: Double, actual: PointXY, label: String) {
        assertEquals(expectedX, actual.x, eps, "$label.x")
        assertEquals(expectedY, actual.y, eps, "$label.y")
    }

    // PointXY は equals(Any?) を持たないので data class equals は参照比較になる。全フィールドを数値で照合する
    private fun assertSamePlacement(expected: DimensionPlacement, actual: DimensionPlacement) {
        assertPoint(expected.dimpoint.x, expected.dimpoint.y, actual.dimpoint, "dimpoint")
        assertPoint(expected.pointA.x, expected.pointA.y, actual.pointA, "pointA")
        assertPoint(expected.pointB.x, expected.pointB.y, actual.pointB, "pointB")
        assertEquals(expected.offsetH, actual.offsetH, eps, "offsetH")
        assertEquals(expected.offsetV, actual.offsetV, eps, "offsetV")
        assertEquals(expected.verticalDxf, actual.verticalDxf, "verticalDxf")
        assertEquals(expected.clockwise, actual.clockwise, "clockwise")
    }

    @Test
    fun `vertical1は基準線の下側にdimheightの9割ぶん離す`() {
        val p = DimensionLayout.layout(PointXY(0f, 0f), PointXY(10f, 0f), vertical = 1, horizontal = 0, dimheight = 0.05)
        assertEquals(0.045, p.offsetV, eps)
    }

    @Test
    fun `vertical3は基準線の上側にdimheightの2割ぶん離す`() {
        val p = DimensionLayout.layout(PointXY(0f, 0f), PointXY(10f, 0f), vertical = 3, horizontal = 0, dimheight = 0.05)
        assertEquals(-0.01, p.offsetV, eps)
    }

    @Test
    fun `horizontalCENTERのdimpointは辺の中点`() {
        val p = DimensionLayout.layout(PointXY(0f, 0f), PointXY(10f, 0f), vertical = 1, horizontal = DimensionLayout.CENTER)
        assertPoint(5.0, 0.0, p.dimpoint, "dimpoint")
        assertEquals(0.0, p.offsetH, eps)
    }

    @Test
    fun `horizontalINRIGHTは辺長の1割ぶん右に寄せる`() {
        val p = DimensionLayout.layout(PointXY(0f, 0f), PointXY(10f, 0f), vertical = 1, horizontal = DimensionLayout.INRIGHT)
        assertEquals(-1.0, p.offsetH, eps)
        assertPoint(4.0, 0.0, p.dimpoint, "dimpoint")
    }

    @Test
    fun `horizontalINLEFTは辺長の1割ぶん左に寄せる`() {
        val p = DimensionLayout.layout(PointXY(0f, 0f), PointXY(10f, 0f), vertical = 1, horizontal = DimensionLayout.INLEFT)
        assertEquals(1.0, p.offsetH, eps)
        assertPoint(6.0, 0.0, p.dimpoint, "dimpoint")
    }

    @Test
    fun `OUTERLEFTの旗揚げは辺の手前延長線上に隙間と旗揚げ距離をとった区間に置く`() {
        // leftP=(0,0) rightP=(10,0): pointA = leftP から右向きに -3*scale = (-3,0)、
        // pointB = rightP から左向きに (0.5*scale + 辺長) = (-0.5,0)
        val p = DimensionLayout.layout(PointXY(0f, 0f), PointXY(10f, 0f), vertical = 1, horizontal = DimensionLayout.OUTERLEFT, scale = 1.0)
        assertPoint(-3.0, 0.0, p.pointA, "pointA")
        assertPoint(-0.5, 0.0, p.pointB, "pointB")
        assertEquals(-0.5, p.offsetH, eps) // 横寄せは辺長の 5%、旗側へ
        assertPoint(-2.25, 0.0, p.dimpoint, "dimpoint")
    }

    @Test
    fun `OUTERRIGHTは右側への旗揚げで反転正規化も通る`() {
        // 旗が右側に出て pointA.x >= pointB.x になるため反転正規化も通る
        val p = DimensionLayout.layout(PointXY(0f, 0f), PointXY(10f, 0f), vertical = 1, horizontal = DimensionLayout.OUTERRIGHT, scale = 1.0, dimheight = 0.05)
        assertPoint(10.5, 0.0, p.pointA, "pointA")
        assertPoint(13.0, 0.0, p.pointB, "pointB")
        assertEquals("A", p.clockwise)
        // DimOnPath の実挙動 (init 順序により flip が機能せず vertical=0) を保存:
        // 反転正規化の offsetV 再設定はスキップされ、初期値 offsetLower のまま
        assertEquals(0.045, p.offsetV, eps)
        assertEquals(DimensionLayout.LOWER, p.verticalDxf)
    }

    @Test
    fun `OUTERRIGHTはverticalの入力値によらず同じverticalDxfを返す`() {
        // DimOnPath.kt:74 の flip は初期化順序バグで常に vertical=0 を代入するため、
        // 入力 1/3 の区別が verticalDxf に現れない。Phase 1 はこの実挙動を pin する
        val v1 = DimensionLayout.layout(PointXY(0f, 0f), PointXY(10f, 0f), vertical = 1, horizontal = DimensionLayout.OUTERRIGHT)
        val v3 = DimensionLayout.layout(PointXY(0f, 0f), PointXY(10f, 0f), vertical = 3, horizontal = DimensionLayout.OUTERRIGHT)
        assertEquals(v1.verticalDxf, v3.verticalDxf)
        assertEquals(DimensionLayout.LOWER, v1.verticalDxf)
    }

    @Test
    fun `右から左へ向かう辺は上下逆さまにならないよう正規化されclockwiseがAになる`() {
        val p = DimensionLayout.layout(PointXY(10f, 0f), PointXY(0f, 0f), vertical = 1, horizontal = DimensionLayout.INRIGHT, dimheight = 0.05)
        assertEquals("A", p.clockwise)
        // AB が swap され x 昇順に戻る
        assertPoint(0.0, 0.0, p.pointA, "pointA")
        assertPoint(10.0, 0.0, p.pointB, "pointB")
        // offsetH は符号反転、offsetV は上下入れ替え (vertical=1 なのに上側の値)
        assertEquals(1.0, p.offsetH, eps)
        assertEquals(-0.01, p.offsetV, eps)
    }

    @Test
    fun `測点名は辺の手前外側に旗状に置きy降順に正規化する`() {
        // 上向きの辺: outerleft=(0,-3) outerright=(0,-0.5) が y 降順に入れ替わる
        val p = DimensionLayout.layout(PointXY(0f, 0f), PointXY(0f, 10f), vertical = DimensionLayout.SIDE_SOKUTEN, horizontal = 0, scale = 1.0)
        assertPoint(0.0, -0.5, p.pointA, "pointA")
        assertPoint(0.0, -3.0, p.pointB, "pointB")
        assertEquals(0.0, p.offsetV, eps)
        assertPoint(0.0, -1.75, p.dimpoint, "dimpoint")
    }

    @Test
    fun `測点名のhorizontal1は辺の向こう側の外側に置く`() {
        val p = DimensionLayout.layout(PointXY(0f, 0f), PointXY(0f, 10f), vertical = DimensionLayout.SIDE_SOKUTEN, horizontal = 1, scale = 1.0)
        // lp/rp が入れ替わり、rightP=(0,10) の先 (上) に出る
        assertPoint(0.0, 13.0, p.pointA, "pointA")
        assertPoint(0.0, 10.5, p.pointB, "pointB")
    }

    @Test
    fun `verticalDxfは基準線の向きで上下を反転する`() {
        // 右向きの辺 (leftP.x < rightP.x): 外 1 は UPPER、内 3 は LOWER... ではなく
        // rightP.isVectorToRight(leftP) = leftP.x > rightP.x の判定に従う (DimOnPath 同値)
        val rightward1 = DimensionLayout.layout(PointXY(0f, 0f), PointXY(10f, 0f), vertical = 1, horizontal = 0)
        val rightward3 = DimensionLayout.layout(PointXY(0f, 0f), PointXY(10f, 0f), vertical = 3, horizontal = 0)
        val leftward1 = DimensionLayout.layout(PointXY(10f, 0f), PointXY(0f, 0f), vertical = 1, horizontal = 0)
        val leftward3 = DimensionLayout.layout(PointXY(10f, 0f), PointXY(0f, 0f), vertical = 3, horizontal = 0)
        assertEquals(DimensionLayout.UPPER, rightward1.verticalDxf)
        assertEquals(DimensionLayout.LOWER, rightward3.verticalDxf)
        assertEquals(DimensionLayout.LOWER, leftward1.verticalDxf)
        assertEquals(DimensionLayout.UPPER, leftward3.verticalDxf)
    }

    @Test
    fun `gap0はgap未指定と完全に同じ配置を返す`() {
        val noGap = DimensionLayout.layout(PointXY(1f, 2f), PointXY(7f, 9f), vertical = 1, horizontal = 1, scale = 2.0, dimheight = 0.3)
        val zeroGap = DimensionLayout.layout(PointXY(1f, 2f), PointXY(7f, 9f), vertical = 1, horizontal = 1, scale = 2.0, dimheight = 0.3, gapPaperMm = 0.0)
        assertSamePlacement(noGap, zeroGap)
    }

    @Test
    fun `gap指定は下側配置でoffsetVをgapかけるscaleぶん遠ざける`() {
        val p = DimensionLayout.layout(PointXY(0f, 0f), PointXY(10f, 0f), vertical = 1, horizontal = 0, scale = 0.5, dimheight = 0.05, gapPaperMm = 2.0)
        // offsetLower=0.045 に gap 2mm * scale 0.5 = 1.0 を同方向 (正) に加算
        assertEquals(1.045, p.offsetV, eps)
    }

    @Test
    fun `gap指定は上側配置でoffsetVを負方向に遠ざける`() {
        val p = DimensionLayout.layout(PointXY(0f, 0f), PointXY(10f, 0f), vertical = 3, horizontal = 0, scale = 1.0, dimheight = 0.05, gapPaperMm = 2.0)
        // offsetUpper=-0.01 に gap 2mm * scale 1 = 2.0 を同方向 (負) に加算
        assertEquals(-2.01, p.offsetV, eps)
    }

    @Test
    fun `gap指定でもdimpointとoffsetHは変わらない`() {
        val noGap = DimensionLayout.layout(PointXY(0f, 0f), PointXY(10f, 0f), vertical = 1, horizontal = 1)
        val withGap = DimensionLayout.layout(PointXY(0f, 0f), PointXY(10f, 0f), vertical = 1, horizontal = 1, gapPaperMm = 5.0)
        assertPoint(noGap.dimpoint.x, noGap.dimpoint.y, withGap.dimpoint, "dimpoint")
        assertEquals(noGap.offsetH, withGap.offsetH, eps)
    }

    @Test
    fun `測点名はgapを指定しても影響を受けない`() {
        val noGap = DimensionLayout.layout(PointXY(0f, 0f), PointXY(0f, 10f), vertical = DimensionLayout.SIDE_SOKUTEN, horizontal = 0)
        val withGap = DimensionLayout.layout(PointXY(0f, 0f), PointXY(0f, 10f), vertical = DimensionLayout.SIDE_SOKUTEN, horizontal = 0, gapPaperMm = 5.0)
        assertSamePlacement(noGap, withGap)
    }
}
