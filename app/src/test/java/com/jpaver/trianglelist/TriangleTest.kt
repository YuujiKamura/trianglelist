package com.jpaver.trianglelist

import com.jpaver.trianglelist.util.Params
import junit.framework.TestCase.assertSame
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Test
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.pow

//@RunWith(PowerMockRunner.class)
//@PrepareForTest(Log.class)
class TriangleTest {
    @Test
    fun testCalcWeitedMidPoint() {
        val t345 = Triangle(3f, 4f, 5f)
        Assert.assertEquals(-2.08f, t345.pointNumberAutoAligned_.x, 0.01f)
        val t = Triangle(5f, 5f, 5f)
        Assert.assertEquals(-2.5f, t.pointNumberAutoAligned_.x, 0.01f)
    }

    @Test
    fun testSameObjectPropaties(){
        val triangleA = Triangle() // Triangleのインスタンスを作成
        val triangleB = Triangle()
        triangleB.nodeTriangleA_ = triangleA

        // オブジェクトのハッシュコードを表示
        println("triangleA のハッシュコード: ${triangleA.hashCode()}")
        println("triangleB.nodeTriangleA のハッシュコード: ${triangleB.nodeTriangleA_.hashCode()}")

        // `nodeTriangleA_` と `parent` が同じオブジェクトを参照しているかテスト
        assertSame( "BのnodeTriangleA と A は異なるオブジェクトを参照しています。", triangleB.nodeTriangleA_, triangleA )
    }

    @Test
    fun testCompareY() {
        //PowerMockito.mockStatic(Log.class);

        //Triangle t = new Triangle(5f, 5f, 5f );
        //assertEquals( 4.33f, t.compareY( 1f, 1f ), 0.01f);

        //assertEquals( 0f, t.compareY( -1f, 1f ), 0.01f);
    }

    @Test
    fun testDistanceHataage() {
        //PowerMockito.mockStatic(Log.class);
        val t = Triangle(5f, 5f, 5f)
        val t2 = Triangle(t, 1, 5f, 5f)
        val t3 = Triangle(t2, 2, 5f, 5f)
        var p = t.hataage(PointXY(-2.5f, 2f), 0f, 1f, 1f)
        Assert.assertEquals(4.33f, p.y, 0.01f)
        p = t.hataage(PointXY(2.5f, 3f), 0f, 1f, 1f)
        Assert.assertEquals(4.33f, p.y, 0.01f)
        p = t.hataage(PointXY(2.5f, 3f), 1f, 1f, 1f)
        Assert.assertEquals(4.33f, p.y, 0.01f)
        p = t.hataage(PointXY(2.5f, 1f), 1f, 1f, 1f)
        Assert.assertEquals(0f, p.y, 0.01f)
        val p2 = t2.hataage(PointXY(2.5f, 1f), 1f, 1f, 1f)
        Assert.assertEquals(0f, p2.y, 0.01f)
        val p3 = t3.hataage(PointXY(2.5f, 5f), 1f, 1f, 1f)
        Assert.assertEquals(4.33f, p3.y, 0.01f)
    }

    @Test
    fun testResetNodeChain() {
        val t = Triangle(5f, 5f, 5f)

        //t2.set( t, 1, 6f, 6f,6f, true );
        Assert.assertEquals(5f, t.lengthB, 0.001f)
        //assertEquals(6f, t3.lengthA_, 0.001f );
    }

    @Test
    fun testValid() {
        val t = Triangle(5f, 5f, 5f)
        Assert.assertTrue(t.isValid)
        val t2 = Triangle(5f, 5f, 15f)
        Assert.assertFalse(t2.isValid)
    }

    @Test
    fun testSetPointersFromCParams() {
        val one =
            Triangle(Params("", "", 1, 5f, 5f, 5f, -1, -1, PointXY(0f, 0f), PointXY(0f, 0f)), 0f)
        val connection = ConnParam(1, 0, 0, 5f)
        val two = Triangle(one, connection, 6f, 6f)

        // 内容の一致
        Assert.assertEquals(one.myNumber.toLong(), two.nodeTriangleA_!!.myNumber.toLong())
        Assert.assertEquals(one.nodeTriangleB_!!.myNumber.toLong(), two.myNumber.toLong())

        // オブジェクトポインタの一致。
        Assert.assertEquals(one, two.nodeTriangleA_)
        Assert.assertEquals(one.nodeTriangleB_, two)
        two.setOn(one, 2, 5f, 5f)

        // オブジェクトポインタの一致。
        Assert.assertEquals(one, two.nodeTriangleA_)
        Assert.assertEquals(one.nodeTriangleC_, two)
    }

    @Test
    fun testSetPointersFromParams() {
        val one =
            Triangle(Params("", "", 1, 5f, 5f, 5f, -1, -1, PointXY(0f, 0f), PointXY(0f, 0f)), 0f)
        val two =
            Triangle(one, Params("", "", 2, 5f, 5f, 5f, 1, 1, PointXY(0f, 0f), PointXY(0f, 0f)))

        // 内容の一致
        Assert.assertEquals(one.myNumber.toLong(), two.nodeTriangleA_!!.myNumber.toLong())
        Assert.assertEquals(one.nodeTriangleB_!!.myNumber.toLong(), two.myNumber.toLong())

        // オブジェクトポインタの一致。
        Assert.assertEquals(one, two.nodeTriangleA_)
        Assert.assertEquals(one.nodeTriangleB_, two)
        two.setOn(one, 2, 5f, 5f)

        // オブジェクトポインタの一致。
        Assert.assertEquals(one, two.nodeTriangleA_)
        Assert.assertEquals(one.nodeTriangleC_, two)
    }

    @Test
    fun testSetObjectPointers() {
        val one = Triangle(5f, 5f, 5f, PointXY(0f, 0f), 0f)
        val two = Triangle(one, 1, 5f, 5f)

        // 内容の一致
        Assert.assertEquals(one.myNumber.toLong(), two.nodeTriangleA_!!.myNumber.toLong())
        Assert.assertEquals(one.nodeTriangleB_!!.myNumber.toLong(), two.myNumber.toLong())

        // オブジェクトポインタの一致。
        Assert.assertEquals(one, two.nodeTriangleA_)
        Assert.assertEquals(one.nodeTriangleB_, two)
        two.setOn(one, 2, 5f, 5f)

        // オブジェクトポインタの一致。
        Assert.assertEquals(one, two.nodeTriangleA_)
        Assert.assertEquals(one.nodeTriangleC_, two)
    }

    @Test
    fun testGetArea() {
        val t = Triangle(5.71f, 9.1f, 6.59f)
        Assert.assertEquals(18.74f, t.getArea(), 0.01f)
    }

    @Test
    fun testSideEffectInBasicTypes() {
        val t = Triangle(5f, 5f, 5f)
        var me = 0
        me = t.rotateZeroToThree(me)
        // こうやって代入しない限り、meは渡された時点でクローンに代わり、副作用っぽく変化したりしない。
        // オブジェクトは違う。引数として生で渡された時、ポインタとして渡されるので、副作用が起きる。
        // t.sideEffectGo( object.clone ) とかすれば予防できる。
        Assert.assertEquals(1, me.toLong())
    }

    @Test
    fun testCalcDimAngle() {
        val tri = Triangle(5f, 5f, 5f)
        val angle = tri.point[0].calcAngle(tri.pointAB, tri.pointBC)
        Assert.assertEquals(-120f, angle, 0.0001f)
        Assert.assertEquals(-120f, tri.pointBC.calcAngle(tri.point[0], tri.pointAB), 0.0001f)
        Assert.assertEquals(-120f, tri.pointAB.calcAngle(tri.pointBC, tri.point[0]), 0.0001f)
    }

    @Test
    fun testDimSideAlign() {
        val tri1 = Triangle(3f, 4f, 5f)
        tri1.rotateDimSideAlign(0)
        Assert.assertEquals(1, tri1.dimSideAlignA_.toLong())
        tri1.setDimPoint()
        Assert.assertEquals(-2.325f, tri1.dimPointA_.x, 0.001f)
        var dim = PointXY(-1.5f, 0f)
        val offsetLeft = PointXY(-3f, 0f)
        val offsetRight = PointXY(0f, 0f)
        var haba = dim.lengthTo(offsetLeft) * 0.5f
        Assert.assertEquals(0.75f, haba, 0.001f)
        haba = dim.lengthTo(offsetRight) * 0.5f
        Assert.assertEquals(0.75f, haba, 0.001f)
        dim = dim.offset(offsetLeft, haba)
        Assert.assertEquals(-2.25f, dim.x, 0.001f)
        tri1.rotateDimSideAlign(0)
        tri1.setDimPoint()
        Assert.assertEquals(-0.67f, tri1.dimPointA_.x, 0.01f)
        tri1.flipDimAlignH(0)
        Assert.assertEquals(1, tri1.myDimAlignA_.toLong())
        tri1.flipDimAlignH(0)
        Assert.assertEquals(3, tri1.myDimAlignA_.toLong())
    }

    @Test
    fun testAutoAlignPointNumber2() {
        val t1 = Triangle(6.0f, 3.5f, 3.5f)
        Assert.assertEquals(-3.0f, t1.pointNumberAutoAligned_.x, 0.001f)
        Assert.assertEquals(118f, t1.angleBC, 0.1f)
        Assert.assertEquals(1.01f, t1.pointNumberAutoAligned_.y, 0.01f)
    }

    @Test
    fun testAutoAlignPointNumber() {
        val t1 = Triangle(5f, 5f, 5f)
        Assert.assertEquals(-2.5, t1.pointNumberAutoAligned_.x.toDouble(), 0.001)
        Assert.assertEquals(1.4433, t1.pointNumberAutoAligned_.y.toDouble(), 0.001)
        val t2 = Triangle(5f, 1.5f, 5f)
        //t2.setChildSide(1);
        Assert.assertEquals(-4.077f, t2.pointNumberAutoAligned_.x, 0.001f)
    }

    @Test
    fun testConnection() {
        val t1 = Triangle(3f, 4f, 5f)
        val t2 = Triangle(t1, 7, 3f, 5f, 4f) // connection 7 is set to B-Center
        Assert.assertEquals(3.5, t2.point[0].y.toDouble(), 0.001)
        val t3 = Triangle(t2, 8, 3f, 4f, 5f) // connection 8 is set to C-Center
        Assert.assertEquals(-6.5, t3.pointAB.x.toDouble(), 0.001)
    }

    @Test
    fun testTriBounds() {
        val myT = Triangle(3f, 4f, 5f)
        Assert.assertEquals(4f, myT.myBP_.top, 0.001f)
        myT.move(PointXY(5f, 5f))
        Assert.assertEquals(9f, myT.myBP_.top, 0.001f)
        val myDParam =
            Params("集水桝", "Box", 3, 0.8f, 0.8f, 0f, 0, 0, PointXY(0.5f, 0.5f), PointXY(0f, 0f))
        val myD = Deduction(myDParam)
        myD.move(PointXY(5f, 5f))
        Assert.assertEquals(5.5f, myD.point.x, 0.001f)
    }

    @Test
    fun testDimPathAndOffset() {
        val t1 = Triangle(3f, 4f, 5f)

        // 1下 3上 -> // 夾角の、1:外 　3:内
        Assert.assertEquals(3, t1.myDimAlignA_.toLong()) //getPath(0).getAlign_());
        Assert.assertEquals(3, t1.myDimAlignB_.toLong()) // t1.getPath(1).getAlign_());
        Assert.assertEquals(3, t1.myDimAlignC_.toLong()) // t1.getPath(2).getAlign_());
        Assert.assertEquals(3, t1.myDimAlignA_.toLong()) //getPath(0).getAlign_());
        Assert.assertEquals(3, t1.myDimAlignB_.toLong()) // t1.getPath(1).getAlign_());
        Assert.assertEquals(3, t1.myDimAlignC_.toLong()) // t1.getPath(2).getAlign_());
    }

    @Test
    fun testCalcAngleOfLength() {
        val mytri = Triangle(3.0f, 4.0f, 5.0f, PointXY(0f, 0f), 180.0f)
        Assert.assertEquals(0.0, mytri.point[0].calcDimAngle(mytri.pointAB_()).toDouble(), 0.001)
        Assert.assertEquals(
            -1.5,
            mytri.point[0].calcMidPoint(mytri.pointAB_()).x.toDouble(),
            0.01
        )
    }

    @Test
    fun testSuccess() {
        val mytri1 = Triangle(3.0f, 4.0f, 5.0f, PointXY(0f, 0f), 180.0f)
        MatcherAssert.assertThat(mytri1, CoreMatchers.`is`(mytri1))
    }

    @Test
    fun testTrianglePoint() {
        val myXY0 = PointXY(0.0f, 0.0f)
        val myTriangle = Triangle(3.0f, 4.0f, 5.0f, myXY0, 180.0f)
        Assert.assertEquals(myTriangle.point[0].x.toDouble(), myXY0.x.toDouble(), 0.001)
        Assert.assertEquals(myTriangle.point[0].y.toDouble(), myXY0.y.toDouble(), 0.001)
        Assert.assertEquals(myTriangle.pointAB_().x.toDouble(), -3.0, 0.001)
        Assert.assertEquals(myTriangle.pointAB_().y.toDouble(), -0.0, 0.001)
    }

    @Test
    fun testInvalidTriangle() {
        val myTriT = Triangle(3.0f, 4.0f, 5.0f, PointXY(0f, 0f), 180.0f)
        val myTriT2 = Triangle(1.111f, 4.0f, 5.0f, PointXY(0f, 0f), 180.0f)
        val myTriF = Triangle(0.0f, 4.0f, 5.0f, PointXY(0f, 0f), 180.0f)
        val myTriF2 = Triangle(0.999f, 4.0f, 5.0f, PointXY(0f, 0f), 180.0f)
        val myTriF3 = Triangle(1.0f, 4.0f, 5.0f, PointXY(0f, 0f), 180.0f)
        val myTriF4 = Triangle(4.0f, 5.0f, 1.0f, PointXY(0f, 0f), 180.0f)
        val myTriF5 = Triangle(4.0f, 1.0f, 5.0f, PointXY(0f, 0f), 180.0f)
        Assert.assertTrue(myTriT.isValid)
        Assert.assertTrue(myTriT2.isValid)
        Assert.assertFalse(myTriF.isValid)
        Assert.assertFalse(myTriF2.isValid)
        Assert.assertFalse(myTriF3.isValid)
        Assert.assertFalse(myTriF4.isValid)
        Assert.assertFalse(myTriF5.isValid)
        val myList = TriangleList()
        Assert.assertTrue(myList.add(myTriT, true))
        //assertTrue(myList.add(myTriT2));
        Assert.assertFalse(myList.add(myTriF, true))
        Assert.assertFalse(myList.add(myTriF2, true))
        Assert.assertFalse(myList.add(myTriF3, true))
    }

    @Test
    fun testCalcThetaAlpha() {
        val pCA = PointXY(0.0f, 0.0f)
        val myTriangle = Triangle(3.0f, 4.0f, 5.0f, pCA, 180.0f)

        // atan2( y, x );
        val theta = atan2(
            (pCA.y - myTriangle.pointAB_().y).toDouble(),
            (pCA.x - myTriangle.pointAB_().x).toDouble()
        )
        val alpha = acos(
            myTriangle.lengthA.pow(2.0f) + myTriangle.lengthB.pow(2.0f) - myTriangle.lengthC.pow(2.0f)
        )
        Assert.assertEquals((pCA.x - myTriangle.pointAB_().x).toDouble(), 3.0, 0.005)
        Assert.assertEquals((pCA.y - myTriangle.pointAB_().y).toDouble(), 0.0, 0.005)
        Assert.assertEquals(myTriangle.pointAB_().x.toDouble(), -3.0, 0.005)
        Assert.assertEquals(theta, 0.0, 0.005)
        Assert.assertEquals(alpha, 1.570796f, 0.005f)
    }

    @Test
    fun testNewCalcPointB_CParam() {
        val tri1 = Triangle(3f, 5f, 4f, PointXY(0f, 0f), 0.0f)
        val tri2 = Triangle(tri1, 3, 6f, 5f, 4f)
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        Assert.assertTrue(tri1.point[0].equals(0f, 0f))
        Assert.assertTrue(tri1.pointAB.equals(3f, 0f))
        Assert.assertTrue(tri1.pointBC.equals(0f, -4f))
        Assert.assertTrue(tri2.point[0].equals(0f, -4f))
        Assert.assertFalse(tri2.pointAB.equals(3f, 0f))
        Assert.assertFalse(tri2.pointBC.equals(0f, -4f))
        tri1.calcPoints(tri2, 1)
        Assert.assertFalse(tri1.point[0].equals(0f, 0f))
        Assert.assertFalse(tri1.pointAB.equals(3f, 0f))
        Assert.assertFalse(tri1.pointBC.equals(0f, -4f))
    }

    @Test
    fun testNewCalcPointC() {
        val tri1 = Triangle(3f, 4f, 5f, PointXY(0f, 0f), 0.0f)
        val tri2 = Triangle(tri1, 2, 3f, 4f)
        Assert.assertTrue(tri1.point[0].equals(0f, 0f))
        Assert.assertTrue(tri1.pointAB.equals(3f, 0f))
        Assert.assertTrue(tri1.pointBC.equals(3f, -4f))
        tri1.calcPoints(tri2, 2)
        Assert.assertTrue(tri1.point[0].equals(0f, 0f))
        Assert.assertTrue(tri1.pointAB.equals(3f, 0f))
        Assert.assertTrue(tri1.pointBC.equals(3f, -4f))
    }

    @Test
    fun testNewCalcPointB() {
        val tri1 = Triangle(3f, 5f, 4f, PointXY(0f, 0f), 0.0f)
        val tri2 = Triangle(tri1, 1, 4f, 3f)
        Assert.assertTrue(tri1.point[0].equals(0f, 0f))
        Assert.assertTrue(tri1.pointAB.equals(3f, 0f))
        Assert.assertTrue(tri1.pointBC.equals(0f, -4f))
        tri1.calcPoints(tri2, 1)
        Assert.assertTrue(tri1.point[0].equals(0f, 0f))
        Assert.assertTrue(tri1.pointAB.equals(3f, 0f))
        Assert.assertTrue(tri1.pointBC.equals(0f, -4f))
    }

    @Test
    fun testCalcPoint() {
        val myXY0 = PointXY(0.0f, 0.0f)
        val myTriangle = Triangle(3.0f, 4.0f, 5.0f, myXY0, 90.0f)
        Assert.assertEquals(myTriangle.pointAB_().x.toDouble(), 0.0, 0.001)
        Assert.assertEquals(myTriangle.pointAB_().y.toDouble(), 3.0, 0.001)
        Assert.assertEquals(myTriangle.pointBC_().x.toDouble(), 4.0, 0.001)
        Assert.assertEquals(myTriangle.pointBC_().y.toDouble(), 3.0, 0.001)
        Assert.assertEquals(myTriangle.angleCA.toDouble(), 53.130, 0.001)
        Assert.assertEquals(myTriangle.angleAB.toDouble(), 90.000, 0.001)
        Assert.assertEquals(myTriangle.angleBC.toDouble(), 36.869, 0.001)
    }
}
