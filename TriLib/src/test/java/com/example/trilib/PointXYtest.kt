package com.example.trilib


import junit.framework.TestCase.assertEquals
import org.junit.Assert
import org.junit.Test

class PointXYtest {

    data class AnglePoint(var num:Int, var prev: PointXY, var targ: PointXY, var next: PointXY){
        init{
            calculate()
        }
        fun calculate(): Float{
            val angle1 = prev.calcAngle360(targ,next)
            println("$num: $angle1")
            return angle1
        }
    }

    @Test
    fun testCalcAngle(){
        val a = AnglePoint( 1,
            PointXY(-7.9f, 6.2f),
            PointXY(-6.4f, 4.6f),
            PointXY(-5.7f, 5.2f)
        )
        val b = AnglePoint( 2, a.targ, a.next, PointXY(-6.1f, 7.1f))
        val c = AnglePoint( 3, b.targ, b.next, PointXY(-4.1f, 7.2f))
        val d = AnglePoint( 4, c.targ, c.next, PointXY(-3.7f, 8.1f))
                AnglePoint( 5, d.targ, d.next, PointXY(-0.9f, 24.2f))

    }

    @Test
    fun testDistancesTo() {
        val centerPoint = PointXY(0f, 0f) // 中心点
        val nearby = 5f // 判定距離

        // テスト用のポイントとラベルのリスト
        val testCases = listOf(
            listOf(
                PointXY(1f, 1f),
                PointXY(2f, 2f),
                PointXY(3f, 3f)
            ) to "All Close",
            listOf(
                PointXY(1f, 1f),
                PointXY(10f, 10f),
                PointXY(3f, 3f)
            ) to "Mixed Distances",
            listOf(
                PointXY(10f, 10f),
                PointXY(11f, 11f),
                PointXY(12f, 12f)
            ) to "All Far"
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
/*
    @Test
    fun testTrimming(){

        val trimline = ArrayList<PointXY>()
        trimline.add(PointXY(0f, 0f))
        trimline.add(PointXY(1f, 1f))

        val tri = com.jpaver.trianglelist.Triangle(5f,5f,5f,
            PointXY(-0.5f, -0.5f), 0f )

        Assert.assertEquals( false, tri.isCollide )

    }

    @Test
    fun testPointVector() {
        val p1 = PointXY(-5f, 0f)
        val p2 = PointXY(0f, 0f)

        // lengthXY 符号は付かない
        Assert.assertEquals(5f, p2.vectorTo(p1).lengthXY(), 0.001f)
        val t1 = Triangle(50f, 50f, 50f)
        Assert.assertEquals(50.0f, t1.pointCA.vectorTo(t1.pointAB).lengthXY(), 0.001f)
    }
*/
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