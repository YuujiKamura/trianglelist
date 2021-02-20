package com.jpaver.trianglelist

import junit.framework.Assert.assertEquals
import org.junit.Assert
import org.junit.Test

class PointXYtest {

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
        Assert.assertEquals(50.0f, t1.pointCA_.vectorTo(t1.pointAB_).lengthXY(), 0.001f)
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
        Assert.assertEquals(0f, p.min(PointXY(1f, 0f)).x, 0.001f)
        Assert.assertEquals(1f, p.max(PointXY(1f, 0f)).x, 0.001f)
        Assert.assertEquals(0f, p.max(PointXY(-1f, 0f)).x, 0.001f)
        Assert.assertEquals(-1f, p.min(PointXY(-1f, 0f)).x, 0.001f)
    }
}