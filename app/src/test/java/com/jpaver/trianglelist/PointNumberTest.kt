package com.jpaver.trianglelist

import com.jpaver.trianglelist.editmodel.OutlineList
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import com.jpaver.trianglelist.editmodel.calcPoints
import com.jpaver.trianglelist.editmodel.control_rotate
import org.junit.Assert
import org.junit.Test

class PointNumberTest {
    fun pattern1( triangle: Triangle): TriangleList {
        val trilist = TriangleList()
        trilist.add(triangle, true)//1
        trilist.add(1, 1, 5f, 5f)//2
        return trilist
    }

    fun testCase1( pattern: () -> TriangleList){
        val trilist = pattern()
        val outlinelist = OutlineList(trilist) // OutlineList インスタンスを作成

        trilist.get(1).calcPoints(isArrangeDims = true)

        println( "pointlist:${outlinelist.pointlist}" )
        println( "pointcenter:${trilist.get(1).pointcenter}\npointnumber:${trilist.get(1).pointnumber}\n${trilist.get(1).toStrings()}")
    }

    @Test
    fun testRotatePointNumber() {
        val t1 = Triangle(5.0f, 5.0f, 5.0f)
        t1.setPointNumber(com.example.trilib.PointXY(0f, 2.5f), true)

        //参照を排除したpointnumber
        t1.pointnumber = t1.pointnumber.rotate(com.example.trilib.PointXY(0f, 0f), 90.0)
        Assert.assertEquals(-2.5, t1.pointnumber.x, 0.01)
        Assert.assertEquals(0.0, t1.pointnumber.y, 0.01)

        //一様に回転する
        t1.pointnumber = t1.pointnumber.rotate(com.example.trilib.PointXY(0f, 0f), 90.0)
        Assert.assertEquals(0.0, t1.pointnumber.x, 0.01)
        Assert.assertEquals(-2.5, t1.pointnumber.y, 0.01)

        //更新される
        t1.control_rotate(com.example.trilib.PointXY(0f, 0f), 90f)
        Assert.assertEquals(2.5, t1.pointnumber.x, 0.01)
        Assert.assertEquals(0.0, t1.pointnumber.y, 0.01)

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
        Assert.assertEquals(-3.0, t1.pointnumber.x, 0.001)
        Assert.assertEquals(118f, t1.angleBC, 0.1f)
        Assert.assertEquals(0.6, t1.pointnumber.y, 0.01)
    }

    @Test
    fun testAutoAlignPointNumber() {
        val t1 = Triangle(5f, 5f, 5f)
        Assert.assertEquals(-2.5, t1.pointnumber.x.toDouble(), 0.001)
        Assert.assertEquals(1.4433, t1.pointnumber.y.toDouble(), 0.001)
        val t2 = Triangle(5f, 1.5f, 5f)
        //t2.setChildSide(1);
        Assert.assertEquals(-3.258, t2.pointnumber.x, 0.001)
    }

    //三角形の形状によって番号が移動するかテスト
    @Test
    fun testCalcWeitedMidPoint() {
        val t345 = Triangle(3f, 4f, 5f)
        Assert.assertEquals(-2.0, t345.pointnumber.x, 0.01)
        val t = Triangle(5f, 5f, 5f)
        Assert.assertEquals(-2.5, t.pointnumber.x, 0.01)
    }

}