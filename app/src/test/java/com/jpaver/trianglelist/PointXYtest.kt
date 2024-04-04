package com.jpaver.trianglelist


import junit.framework.TestCase.assertEquals
import org.junit.Assert
import org.junit.Test

class PointXYtest {

    @Test
    fun testDistancesTo() {
        val centerPoint = PointXY(0f, 0f) // 中心点
        val nearby = 5f // 判定距離
        val failedTests = mutableListOf<TestResult>()

        // テスト用のポイントとラベルのリスト
        val testCases = listOf(
            listOf(PointXY(1f, 1f), PointXY(2f, 2f), PointXY(3f, 3f)) to "All Close",
            listOf(PointXY(1f, 1f), PointXY(10f, 10f), PointXY(3f, 3f)) to "Mixed Distances",
            listOf(PointXY(10f, 10f), PointXY(11f, 11f), PointXY(12f, 12f)) to "All Far"
        )

        testCases.forEach { (points, label) ->
            println("Testing case: $label")
            val results = distanceTest(centerPoint, points, nearby, label)
            processResults(results, label)
        }
    }

    fun distanceTest(centerPoint: PointXY, points: List<PointXY>, nearby: Float, label: String): List<TestResult> {
        val distances = centerPoint.distancesTo(points)
        return distances.mapIndexed { index, distance ->
            TestResult(label, index, distance, distance <= nearby)
        }
    }

    fun processResults(results: List<TestResult>, label: String) {
        results.forEach { result ->
            printResult(result)
            checkTestResult(result, label)
        }
    }

    fun printResult(result: TestResult) {
        println("  Point ${result.pointIndex}: Distance = ${result.distance}, Is Close = ${result.isClose}")
    }

    fun checkTestResult(result: TestResult, label: String) {
        val expectedClose = label != "All Far"
        if (result.isClose != expectedClose) {
            failedTests.add(result)
        }
    }

    private fun assertPointXYEquals(expected: PointXY, actual: PointXY) {
        System.out.printf( "PointXY expected x: %s, y: %s, actual x: %s, y: %s %n", expected.x, expected.y, actual.x, actual.y)
        assertEquals(expected.y.toDouble(), actual.y.toDouble(), 0.001 )
        assertEquals(expected.x.toDouble(), actual.x.toDouble(), 0.001 )

    }

    data class TestResult(val label: String, val pointIndex: Int, val distance: Float, val isClose: Boolean)

    // 失敗したテストの結果を格納するリストの宣言
    val failedTests = mutableListOf<TestResult>()



    @Test
    fun testMirrorOriginalPoint() {
        val p = PointXY(1f, 2f)
        val lineStart = PointXY(0f, 0f)
        val lineEnd = PointXY(2f, 2f)

        val actualPoint = p.mirror(lineStart, lineEnd,1f )
        val expectedPoint = PointXY(2f, 1f)

        assertPointXYEquals(expectedPoint, actualPoint)
    }

    @Test
    fun testCrossProduct(){
        val vec1 = arrayOf(
            PointXY(0f, 0f),
            PointXY(1f, 1f)
        )
        val vec2 = arrayOf(
            PointXY(0f, 1f),
            PointXY(1f, 0f)
        )
        arrayOf(
            PointXY(0f, 0.5f),
            PointXY(1f, 1.5f)
        )

        Assert.assertEquals( -2.0, vec1[1].minus(vec1[0]).outerProduct( vec2[1].minus(vec2[0]) ), 0.0001 )

    }

    @Test
    fun testTrimming(){

        val trimline = ArrayList<PointXY>()
        trimline.add(PointXY(0f, 0f))
        trimline.add(PointXY(1f, 1f))

        val tri = Triangle(5f,5f,5f,
            PointXY(-0.5f, -0.5f), 0f )

        Assert.assertEquals( false, tri.isCollide )

    }

    @Test
    fun testViewToModel(){
        val pressedInView = PointXY(1f, 0f)
        val pressedInModel = pressedInView.translateAndScale(
            PointXY(
                0f,
                0f
            ),
            PointXY(0f, 0f),
            1f,  )

        Assert.assertEquals(1f, pressedInModel.x, 0.001f)

    }

    @Test
    fun testAddMinusAdd(){
        val p1 = PointXY(1f, 0f)
        val p2 = PointXY(1f, 0f)
        p1.add( p1 ) // add rewrite it
        p1.addminus( p2 ) // add rewrite it
        Assert.assertEquals(1f, p1.x, 0.001f)

    }

    @Test
    fun testPointXYreference() {
        val p1 = PointXY(0f, 0f)
        p1.add(1f, 0f) // add rewrite it
        p1.plus(1f, 0f) // plus is NOT rewrite it
        Assert.assertEquals(1f, p1.x, 0.001f)
    }

    @Test
    fun testPointOffsetToMinus() {
        val p1 = PointXY(0f, 0f)
        val p2 = PointXY(0f, 5f)
        val p3 = p1.offset(p2, 10f)
        Assert.assertEquals(10f, p3.y, 0.001f )

        // マイナス方向に向かってのプラスのムーブメント
        assertEquals( -10.0f, p2.offset(p1, 15f).y, 0.001f )
    }

    @Test
    fun testPointVector() {
        val p1 = PointXY(-5f, 0f)
        val p2 = PointXY(0f, 0f)

        // lengthXY 符号は付かない
        Assert.assertEquals(5f, p2.vectorTo(p1).lengthXY(), 0.001f)
        val t1 = Triangle(50f, 50f, 50f)
        Assert.assertEquals(50.0f, t1.pointCA_.vectorTo(t1.pointAB).lengthXY(), 0.001f)
    }

    @Test
    fun testPointOffset() {
        val p1 = PointXY(-5f, -5f)
        val p2 = PointXY(-3f, -2f)
        //vector -2, -3
        //if offset -3, expected return -5-(-3)*(-2)=-11, -5-(-3)*(-3)=-14
        Assert.assertEquals(-7f, p1.offset(p2, -3.6065f).x, 0.001f)
        Assert.assertEquals(-8f, p1.offset(p2, -3.6065f).y, 0.001f)

        // 3,4,,?
        val p34 = PointXY(3f, 4f)
        Assert.assertEquals(5f, p34.lengthXY(), 0.001f)
    }

    @Test
    fun testMinMax() {
        val p = PointXY(0f, 0f)
        Assert.assertEquals(0f, p.min(
            PointXY(
                1f,
                0f
            )
        ).x, 0.001f)
        Assert.assertEquals(1f, p.max(
            PointXY(
                1f,
                0f
            )
        ).x, 0.001f)
        Assert.assertEquals(0f, p.max(
            PointXY(
                -1f,
                0f
            )
        ).x, 0.001f)
        Assert.assertEquals(-1f, p.min(
            PointXY(
                -1f,
                0f
            )
        ).x, 0.001f)
    }
}