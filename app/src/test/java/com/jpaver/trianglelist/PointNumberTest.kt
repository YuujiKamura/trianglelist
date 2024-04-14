package com.jpaver.trianglelist

import org.junit.Assert
import org.junit.Test

class PointNumberTest {
    fun pattern1( triangle: Triangle ): TriangleList{
        val trilist = TriangleList()
        trilist.add(triangle, true)//1
        trilist.add(1, 1, 5f, 5f)//2
        return trilist
    }

    fun testCase1( pattern: () -> TriangleList ){
        val trilist = pattern()
        val outlinelist = OutlineList(trilist) // OutlineList インスタンスを作成

        trilist.get(1).calcPoints( isArrange = true, outlineList = outlinelist )

        println( "pointlist:${outlinelist.pointlist}" )
        println( "pointcenter:${trilist.get(1).pointcenter}\npointnumber:${trilist.get(1).pointnumber}\n${trilist.get(1).toStrings()}")
    }

    @Test
    fun testRotatePointNumber() {
        val t1 = Triangle(5.0f, 5.0f, 5.0f)
        t1.setPointNumber(PointXY(0f,2.5f))

        //この時点でt1は変化なし、pointNumberは内部で変化している
        //参照はこの時点で解除される
        t1.pointNumber.rotate(PointXY(0f,0f), 90f)
        Assert.assertEquals(2.5f, t1.pointnumber.y, 0.01f)
        Assert.assertEquals(0.0f, t1.pointNumber.pointnumber.y, 0.01f)

        //t1は更新される...pointNumberの方は内部で２回目の回転
        t1.pointnumber = t1.pointNumber.rotate(PointXY(0f,0f), 90f)
        Assert.assertEquals(0f, t1.pointnumber.x, 0.01f)
        Assert.assertEquals(-2.5f, t1.pointnumber.y, 0.01f)

        //更新される
        t1.control_rotate(PointXY(0f,0f), 90f)
        Assert.assertEquals(2.5f, t1.pointnumber.x, 0.01f)
        Assert.assertEquals(0.0f, t1.pointnumber.y, 0.01f)

    }


    @Test
    fun testAuto(){
        testCase1( {pattern1(Triangle(3.5f, 4f, 0.7f))} )
        testCase1( {pattern1(Triangle(3.5f, 4f, 1f))} )
        testCase1( {pattern1(Triangle(4.5f, 4f, 1f))} )
    }

    @Test
    fun testAutoAlignPointNumber2() {
        val t1 = Triangle(6.0f, 3.5f, 3.5f)
        Assert.assertEquals(-3.0f, t1.pointnumber.x, 0.001f)
        Assert.assertEquals(118f, t1.angleBC, 0.1f)
        Assert.assertEquals(0.6f, t1.pointnumber.y, 0.01f)
    }

    @Test
    fun testAutoAlignPointNumber() {
        val t1 = Triangle(5f, 5f, 5f)
        Assert.assertEquals(-2.5, t1.pointnumber.x.toDouble(), 0.001)
        Assert.assertEquals(1.4433, t1.pointnumber.y.toDouble(), 0.001)
        val t2 = Triangle(5f, 1.5f, 5f)
        //t2.setChildSide(1);
        Assert.assertEquals(-3.258f, t2.pointnumber.x, 0.001f)
    }

    //三角形の形状によって番号が移動するかテスト
    @Test
    fun testCalcWeitedMidPoint() {
        val t345 = Triangle(3f, 4f, 5f)
        Assert.assertEquals(-2.0f, t345.pointnumber.x, 0.01f)
        val t = Triangle(5f, 5f, 5f)
        Assert.assertEquals(-2.5f, t.pointnumber.x, 0.01f)
    }

}