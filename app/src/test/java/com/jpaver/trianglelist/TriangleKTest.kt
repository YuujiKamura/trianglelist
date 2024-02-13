
package com.jpaver.trianglelist

import com.jpaver.trianglelist.ConnParam
import com.jpaver.trianglelist.Deduction
import com.jpaver.trianglelist.PointXY
import com.jpaver.trianglelist.TriangleK
import com.jpaver.trianglelist.util.Params
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Test

class TriangleKTest {

    @Test
    fun testCloneShallowOrDeep(){
        val t1 = TriangleK(5f, 5f, 5f)
        val t1c = t1.clone()

        Assert.assertEquals( t1.length[0], t1c.length[0] )

        t1c.length[0] = 8f

        Assert.assertEquals( 8f, t1c.length[0] )
        Assert.assertEquals( 5f, t1.length[0] )

        val t2 = TriangleK( t1, 1, 5f,5f )
        val t2c = t2.clone()

        Assert.assertEquals( t2.nodeTriangle[0], t2c.nodeTriangle[0] )
        Assert.assertEquals( t1, t2c.nodeTriangle[0] ) //クローンポインタからでも参照先は同一

        t2c.nodeTriangle[0] = null //消してしまうと
        Assert.assertEquals( null, t2c.nodeTriangle[0] ) // 当然消える、でも

        Assert.assertEquals( t1, t2.nodeTriangle[0] ) //クローン元のポインタは生きている

        t2.nodeTriangle[0] = null //実はポインタなので

        Assert.assertTrue( t1.valid_ ) // t1が消えてなくなるわけではない

    }

    @Test
    fun testRotateNode() {
        val t = TriangleK(5f, 5f, 5f)
        val t2 = TriangleK(t, 1, 5f, 5f)

        t2.rotateNode(true)
        Assert.assertEquals(t, t2.nodeTriangle[1])

        t.rotateNode(false)
        Assert.assertEquals(t2, t.nodeTriangle[0])

    }

    @Test
    fun testResetNodeChain() {
        val t = TriangleK(5f, 5f, 5f)
        val t2 = TriangleK(t, 1, 5f, 5f)
        //val t3 = TriangleK(t2, 1, 5f, 5f)
        t2[t, 1, 6f, 6f, 6f] = true
        Assert.assertEquals(6f, t.length[1], 0.001f)
        //assertEquals(6f, t3.length[0], 0.001f );
    }

    @Test
    fun testValid() {
        val t = TriangleK(5f, 5f, 5f)
        Assert.assertEquals(true, t.validTriangle())
        val t2 = TriangleK(5f, 5f, 15f)
        Assert.assertEquals(false, t2.validTriangle())
    }

    @Test
    fun testSetPointersFromCParams() {
        val one =
            TriangleK(
                Params("", "", 1, 5f, 5f, 5f, -1, -1,
                PointXY(0f, 0f),
                PointXY(0f, 0f)
            ), 0f)
        val connection = ConnParam(1, 0, 0, 5f)
        val two = TriangleK(one, connection, 6f, 6f)

        // 内容の一致
        Assert.assertEquals(one.myNumber_.toLong(), two.nodeTriangle[0]!!.myNumber_.toLong())
        Assert.assertEquals(one.nodeTriangle[1]!!.myNumber_.toLong(), two.myNumber_.toLong())

        // オブジェクトポインタの一致。
        Assert.assertEquals(one, two.nodeTriangle[0])
        Assert.assertEquals(one.nodeTriangle[1], two)
        two[one, 2, 5f] = 5f

        // オブジェクトポインタの一致。
        Assert.assertEquals(one, two.nodeTriangle[0])
        Assert.assertEquals(one.nodeTriangle[2], two)
    }

    @Test
    fun testSetPointersFromParams() {
        val one =
            TriangleK(
                Params("", "", 1, 5f, 5f, 5f, -1, -1,
                PointXY(0f, 0f),
                PointXY(0f, 0f)
            ), 0f)
        val two =
            TriangleK(one, Params("", "", 2, 5f, 5f, 5f, 1, 1,
                PointXY(0f, 0f),
                PointXY(0f, 0f)
            )
            )

        // 内容の一致
        Assert.assertEquals(one.myNumber_.toLong(), two.nodeTriangle[0]!!.myNumber_.toLong())
        Assert.assertEquals(one.nodeTriangle[1]!!.myNumber_.toLong(), two.myNumber_.toLong())

        // オブジェクトポインタの一致。
        Assert.assertEquals(one, two.nodeTriangle[0])
        Assert.assertEquals(one.nodeTriangle[1], two)
        two[one, 2, 5f] = 5f

        // オブジェクトポインタの一致。
        Assert.assertEquals(one, two.nodeTriangle[0])
        Assert.assertEquals(one.nodeTriangle[2], two)
    }

    @Test
    fun testSetObjectPointers() {
        val one = TriangleK(5f, 5f, 5f,
            PointXY(0f, 0f), 0f)
        val two = TriangleK(one, 1, 5f, 5f)

        // 内容の一致
        Assert.assertEquals(one.myNumber_.toLong(), two.nodeTriangle[0]!!.myNumber_.toLong())
        Assert.assertEquals(one.nodeTriangle[1]!!.myNumber_.toLong(), two.myNumber_.toLong())

        // オブジェクトポインタの一致。
        Assert.assertEquals(one, two.nodeTriangle[0])
        Assert.assertEquals(one.nodeTriangle[1], two)
        two[one, 2, 5f] = 5f

        // オブジェクトポインタの一致。
        Assert.assertEquals(one, two.nodeTriangle[0])
        Assert.assertEquals(one.nodeTriangle[2], two)
    }

    @Test
    fun testGetArea() {
        val t = TriangleK(5.71f, 9.1f, 6.59f)
        Assert.assertEquals(18.74f, t.getArea(), 0.01f)
    }

    @Test
    fun testSideEffectInBasicTypes() {
        val t = TriangleK(5f, 5f, 5f)
        var me = 0
        me = t.rotateZeroToThree(me)
        // こうやって代入しない限り、meは渡された時点でクローンに代わり、副作用っぽく変化したりしない。
        // オブジェクトは違う。引数として生で渡された時、ポインタとして渡されるので、副作用が起きる。
        // t.sideEffectGo( object.clone ) とかすれば予防できる。
        Assert.assertEquals(1, me.toLong())
    }

    @Test
    fun testCalcDimAngle() {
        val tri = TriangleK(5f, 5f, 5f)
        val angle = tri.point[0].calcAngle(tri.point[1], tri.point[2])
        Assert.assertEquals(-120f, angle, 0.0001f)
        Assert.assertEquals(-120f, tri.point[2].calcAngle(tri.point[0], tri.point[1]), 0.0001f)
        Assert.assertEquals(-120f, tri.point[1].calcAngle(tri.point[2], tri.point[0]), 0.0001f)
    }

    @Test
    fun testDimSideAlign() {
        val tri1 = TriangleK(3f, 4f, 5f,
            PointXY(0f, 0f), 180f )
        tri1.rotateDimSideAlign(0)
        Assert.assertEquals(1, tri1.dimalignH[0].toLong())
        tri1.setDimPoint()
        Assert.assertEquals(-2.25f, tri1.dimpoint[0].x, 0.001f)
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
        Assert.assertEquals(-0.75f, tri1.dimpoint[0].x, 0.001f)
        tri1.flipDimAlignH(0)
        Assert.assertEquals(1, tri1.dimalignV[0].toLong())
        tri1.flipDimAlignH(0)
        Assert.assertEquals(3, tri1.dimalignV[0].toLong())
    }

    @Test
    fun testAutoAlignPointNumber2() {
        val t1 = TriangleK(6.0f, 3.5f, 3.5f,
            PointXY(0f, 0f), 180f )
        Assert.assertEquals(-3.0f, t1.pointNumberAutoAligned_.x, 0.001f)
        Assert.assertEquals(118f, t1.innerangle[2], 0.1f)
        Assert.assertEquals(0.240f, t1.pointNumberAutoAligned_.y, 0.01f)
    }

    @Test
    fun testAutoAlignPointNumber() {
        val t1 = TriangleK(5f, 5f, 5f,
            PointXY(0f, 0f), 180f )
        Assert.assertEquals(-2.5, t1.pointNumberAutoAligned_.x.toDouble(), 0.001)
        Assert.assertEquals(0.577, t1.pointNumberAutoAligned_.y.toDouble(), 0.001)
        val t2 = TriangleK(5f, 1.5f, 5f,
            PointXY(0f, 0f), 180f )
        //t2.setChildSide(1);
        Assert.assertEquals(-2.803f, t2.pointNumberAutoAligned_.x, 0.001f)
    }

    @Test
    fun testConnection() {
        val t1 = TriangleK(3f, 4f, 5f,
            PointXY(0f, 0f), 180f )
        val t2 = TriangleK(t1, 7, 3f, 5f, 4f) // connection 7 is set to B-Center
        Assert.assertEquals(3.5, t2.point[0].y.toDouble(), 0.001)
        val t3 = TriangleK(t2, 8, 3f, 4f, 5f) // connection 8 is set to C-Center
        Assert.assertEquals(-6.5, t3.point[1].x.toDouble(), 0.001)
    }

    @Test
    fun testTriBounds() {
        val myT = TriangleK(3f, 4f, 5f,
            PointXY(0f, 0f), 180f )
        Assert.assertEquals(4f, myT.myBP_.top, 0.001f)
        myT.move(PointXY(5f, 5f))
        Assert.assertEquals(9f, myT.myBP_.top, 0.001f)
        val myDParam =
            Params("集水桝", "Box", 3, 0.8f, 0.8f, 0f, 0, 0,
                PointXY(0.5f, 0.5f),
                PointXY(0f, 0f)
            )
        val myD = Deduction(myDParam)
        myD.move(PointXY(5f, 5f))
        Assert.assertEquals(5.5f, myD.point.x, 0.001f)
    }

    @Test
    fun testDimPathAndOffset() {
        val t1 = TriangleK(3f, 4f, 5f )
        //val t2 = TriangleK( t1, 8, 4f, 4f, 4f ) //8:CC

        // 1下 3上 -> // 夾角の、1:外 　3:内
        Assert.assertEquals(3, t1.dimalignV[0]) //getPath(0).getAlign_());
        Assert.assertEquals(3, t1.dimalignV[1]) // t1.getPath(1).getAlign_());
        Assert.assertEquals(3, t1.dimalignV[2]) // t1.getPath(2).getAlign_());
        Assert.assertEquals(3, t1.dimalignV[0]) //getPath(0).getAlign_());
        Assert.assertEquals(3, t1.dimalignV[1]) // t1.getPath(1).getAlign_());
        Assert.assertEquals(3, t1.dimalignV[2]) // t1.getPath(2).getAlign_());
    }

    @Test
    fun testCalcAngleOfLength() {
        val mytri = TriangleK(3.0f, 4.0f, 5.0f,
            PointXY(0f, 0f), 180.0f)
        Assert.assertEquals(
            0.0,
            mytri.point[0].calcDimAngle( mytri.point[1] ).toDouble(),
            0.001
        )
        Assert.assertEquals(
            -1.5,
            mytri.point[0].calcMidPoint( mytri.point[1] ).x.toDouble(),
            0.01
        )
    }

    @Test
    fun testwillsucseed() {
        val mytri1 = TriangleK(3.0f, 4.0f, 5.0f,
            PointXY(0f, 0f), 180.0f)
        MatcherAssert.assertThat(mytri1, CoreMatchers.`is`(mytri1))
    }

    @Test
    fun testTrianglePoint() {
        val myXY0 = PointXY(0.0f, 0.0f)
        //val myXY1 = PointXY(-3.0f, 0.0f)
        val myTriangle = TriangleK(3.0f, 4.0f, 5.0f, myXY0, 180.0f)
        Assert.assertEquals(myTriangle.point[0].x.toDouble(), myXY0.x.toDouble(), 0.001)
        Assert.assertEquals(myTriangle.point[0].y.toDouble(), myXY0.y.toDouble(), 0.001)
        Assert.assertEquals(myTriangle.point[1].x.toDouble(), -3.0, 0.001)
        Assert.assertEquals(myTriangle.point[1].y.toDouble(), -0.0, 0.001)
    }

    @Test
    fun testInvalidTriangle() {
        val myTriT = TriangleK(3.0f, 4.0f, 5.0f,
            PointXY(0f, 0f), 180.0f)
        val myTriT2 = TriangleK(1.111f, 4.0f, 5.0f,
            PointXY(0f, 0f), 180.0f)
        val myTriF = TriangleK(0.0f, 4.0f, 5.0f,
            PointXY(0f, 0f), 180.0f)
        val myTriF2 = TriangleK(0.999f, 4.0f, 5.0f,
            PointXY(0f, 0f), 180.0f)
        val myTriF3 = TriangleK(1.0f, 4.0f, 5.0f,
            PointXY(0f, 0f), 180.0f)
        val myTriF4 = TriangleK(4.0f, 5.0f, 1.0f,
            PointXY(0f, 0f), 180.0f)
        val myTriF5 = TriangleK(4.0f, 1.0f, 5.0f,
            PointXY(0f, 0f), 180.0f)
        Assert.assertTrue(myTriT.validTriangle())
        Assert.assertTrue(myTriT2.validTriangle())
        Assert.assertFalse(myTriF.validTriangle())
        Assert.assertFalse(myTriF2.validTriangle())
        Assert.assertFalse(myTriF3.validTriangle())
        Assert.assertFalse(myTriF4.validTriangle())
        Assert.assertFalse(myTriF5.validTriangle())
        //val myList = TriangleList()
        //Assert.assertTrue(myList.add(myTriT))
        //assertTrue(myList.add(myTriT2));
        //Assert.assertFalse(myList.add(myTriF))
        //Assert.assertFalse(myList.add(myTriF2))
        //Assert.assertFalse(myList.add(myTriF3))
    }

    @Test
    fun testCalcThetaAlpha() {
        val pCA = PointXY(0.0f, 0.0f)
        val myTriangle = TriangleK(3.0f, 4.0f, 5.0f, pCA, 180.0f)

        // atan2( y, x );
        val theta = Math.atan2(
            (pCA.y - myTriangle.point[1].y).toDouble(),
            (pCA.x - myTriangle.point[1].x).toDouble()
        )
        val alpha = Math.acos(
            Math.pow(
                myTriangle.length[0].toDouble(),
                2.0
            ) + Math.pow(
                myTriangle.length[1].toDouble(),
                2.0
            ) - Math.pow(myTriangle.length[2].toDouble(), 2.0)
        )
        Assert.assertEquals((pCA.x - myTriangle.point[1].x).toDouble(), 3.0, 0.005)
        Assert.assertEquals((pCA.y - myTriangle.point[1].y).toDouble(), 0.0, 0.005)
        Assert.assertEquals(myTriangle.point[1].x.toDouble(), -3.0, 0.005)
        Assert.assertEquals(theta, 0.0, 0.005)
        Assert.assertEquals(alpha, 1.570796, 0.005)
    }

    @Test
    fun testNewCalcPointA_CParam() {
        val tri1 = TriangleK(3f, 4f, 5f,
            PointXY(0f, 0f), 0.0f)
        val tri2 = TriangleK(tri1, 10, 6f, 3f, 4f)
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        Assert.assertEquals(true, tri1.point[0].equals(0f, 0f))
        Assert.assertEquals(true, tri1.point[1].equals(3f, 0f))
        Assert.assertEquals(true, tri1.point[2].equals(3f, -4f))
        tri2.calcPoints( tri1, 0 )
        Assert.assertEquals(true, tri1.point[0].equals(0f, 0f))
        Assert.assertEquals(true, tri1.point[1].equals(3f, 0f))
        Assert.assertEquals(true, tri1.point[2].equals(3f, -4f))
    }

    @Test
    fun testNewCalcPointC_CParam4() {
        val tri1 = TriangleK(3f, 4f, 5f,
            PointXY(0f, 0f), 0.0f)
        val tri2 = TriangleK(tri1, 10, 6f, 3f, 4f)
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        Assert.assertEquals(true, tri1.point[0].equals(0f, 0f))
        Assert.assertEquals(true, tri1.point[1].equals(3f, 0f))
        Assert.assertEquals(true, tri1.point[2].equals(3f, -4f))
        tri1.calcPoints(tri2, 2)
        Assert.assertEquals(true, tri1.point[0].equals(0f, 0f))
        Assert.assertEquals(true, tri1.point[1].equals(3f, 0f))
        Assert.assertEquals(true, tri1.point[2].equals(3f, -4f))
    }

    @Test
    fun testNewCalcPointC_CParam3() {
        val tri1 = TriangleK(3f, 4f, 5f,
            PointXY(0f, 0f), 0.0f)
        val tri2 = TriangleK(tri1, 8, 6f, 3f, 4f)
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        Assert.assertEquals(true, tri1.point[0].equals(0f, 0f))
        Assert.assertEquals(true, tri1.point[1].equals(3f, 0f))
        Assert.assertEquals(true, tri1.point[2].equals(3f, -4f))
        tri1.calcPoints(tri2, 2)
        Assert.assertEquals(true, tri1.point[0].equals(0f, 0f))
        Assert.assertEquals(true, tri1.point[1].equals(3f, 0f))
        Assert.assertEquals(true, tri1.point[2].equals(3f, -4f))
    }

    @Test
    fun testNewCalcPointC_CParam2() {
        val tri1 = TriangleK(3f, 4f, 5f,
            PointXY(0f, 0f), 0.0f)
        val tri2 = TriangleK(tri1, 6, 6f, 3f, 4f)
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        Assert.assertEquals(true, tri1.point[0].equals(0f, 0f))
        Assert.assertEquals(true, tri1.point[1].equals(3f, 0f))
        Assert.assertEquals(true, tri1.point[2].equals(3f, -4f))
        tri1.calcPoints(tri2, 2)
        Assert.assertEquals(true, tri1.point[0].equals(0f, 0f))
        Assert.assertEquals(true, tri1.point[1].equals(3f, 0f))
        Assert.assertEquals(true, tri1.point[2].equals(3f, -4f))
    }

    @Test
    fun testNewCalcPointC_CParam() {
        val tri1 = TriangleK(3f, 4f, 5f,
            PointXY(0f, 0f), 0.0f)
        val tri2 = TriangleK(tri1, 5, 6f, 3f, 4f)
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        Assert.assertEquals(true, tri1.point[0].equals(0f, 0f))
        Assert.assertEquals(true, tri1.point[1].equals(3f, 0f))
        Assert.assertEquals(true, tri1.point[2].equals(3f, -4f))
        tri1.calcPoints(tri2, 2)
        Assert.assertEquals(true, tri1.point[0].equals(0f, 0f))
        Assert.assertEquals(true, tri1.point[1].equals(3f, 0f))
        Assert.assertEquals(true, tri1.point[2].equals(3f, -4f))
    }

    @Test
    fun testNewCalcPointB_CParam4() {
        val tri1 = TriangleK(3f, 5f, 4f,
            PointXY(0f, 0f), 0.0f)
        val tri2 = TriangleK(tri1, 9, 6f, 5f, 4f)
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        Assert.assertEquals(true, tri1.point[0].equals(0f, 0f))
        Assert.assertEquals(true, tri1.point[1].equals(3f, 0f))
        Assert.assertEquals(true, tri1.point[2].equals(0f, -4f))

        tri1.calcPoints(tri2, 1)
        Assert.assertEquals(true, tri1.point[0].equals(0f, 0f))
        Assert.assertEquals(true, tri1.point[1].equals(3f, 0f))
        Assert.assertEquals(true, tri1.point[2].equals(0f, -4f))
    }

    @Test
    fun testNewCalcPointB_CParam3() {
        val tri1 = TriangleK(3f, 5f, 4f,
            PointXY(0f, 0f), 0.0f)
        val tri2 = TriangleK(tri1, 7, 6f, 5f, 4f)
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        Assert.assertEquals(true, tri1.point[0].equals(0f, 0f))
        Assert.assertEquals(true, tri1.point[1].equals(3f, 0f))
        Assert.assertEquals(true, tri1.point[2].equals(0f, -4f))

        tri1.calcPoints(tri2, 1)
        Assert.assertEquals(true, tri1.point[0].equals(0f, 0f))
        Assert.assertEquals(true, tri1.point[1].equals(3f, 0f))
        Assert.assertEquals(true, tri1.point[2].equals(0f, -4f))
    }

    @Test
    fun testNewCalcPointB_CParam2() {
        val tri1 = TriangleK(3f, 5f, 4f,
            PointXY(0f, 0f), 0.0f)
        val tri2 = TriangleK(tri1, 4, 6f, 5f, 4f)
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        Assert.assertEquals(true, tri1.point[0].equals(0f, 0f))
        Assert.assertEquals(true, tri1.point[1].equals(3f, 0f))
        Assert.assertEquals(true, tri1.point[2].equals(0f, -4f))

        tri1.calcPoints(tri2, 1)
        Assert.assertEquals(true, tri1.point[0].equals(0f, 0f))
        Assert.assertEquals(true, tri1.point[1].equals(3f, 0f))
        Assert.assertEquals(true, tri1.point[2].equals(0f, -4f))
    }

    @Test
    fun testNewCalcPointB_CParam() {
        val tri1 = TriangleK(3f, 5f, 4f,
            PointXY(0f, 0f), 0.0f)
        val tri2 = TriangleK(tri1, 3, 6f, 5f, 4f)
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        Assert.assertEquals(true, tri1.point[0].equals(0f, 0f))
        Assert.assertEquals(true, tri1.point[1].equals(3f, 0f))
        Assert.assertEquals(true, tri1.point[2].equals(0f, -4f))

        tri1.calcPoints(tri2, 1)
        Assert.assertEquals(true, tri1.point[0].equals(0f, 0f))
        Assert.assertEquals(true, tri1.point[1].equals(3f, 0f))
        Assert.assertEquals(true, tri1.point[2].equals(0f, -4f))
    }

    @Test
    fun testNewCalcPointC() {
        val tri1 = TriangleK(3f, 4f, 5f,
            PointXY(0f, 0f), 0.0f)
        val tri2 = TriangleK(tri1, 2, 3f, 4f)
        Assert.assertEquals(true, tri1.point[0].equals(0f, 0f))
        Assert.assertEquals(true, tri1.point[1].equals(3f, 0f))
        Assert.assertEquals(true, tri1.point[2].equals(3f, -4f))
        tri1.calcPoints(tri2, 2)
        Assert.assertEquals(true, tri1.point[0].equals(0f, 0f))
        Assert.assertEquals(true, tri1.point[1].equals(3f, 0f))
        Assert.assertEquals(true, tri1.point[2].equals(3f, -4f))
    }

    @Test
    fun testNewCalcPointB() {
        val tri1 = TriangleK(3f, 5f, 4f,
            PointXY(0f, 0f), 0.0f)
        val tri2 = TriangleK(tri1, 1, 4f, 3f)
        Assert.assertEquals(true, tri1.point[0].equals(0f, 0f))
        Assert.assertEquals(true, tri1.point[1].equals(3f, 0f))
        Assert.assertEquals(true, tri1.point[2].equals(0f, -4f))
        Assert.assertEquals(true, tri2.point[0].equals(0f, -4f))
        Assert.assertEquals(true, tri2.point[1].equals(3f, 0f))
        Assert.assertEquals(true, tri2.point[2].equals(3f, -4f))
        tri1.calcPoints(tri2, 1)
        Assert.assertEquals(true, tri1.point[0].equals(0f, 0f))
        Assert.assertEquals(true, tri1.point[1].equals(3f, 0f))
        Assert.assertEquals(true, tri1.point[2].equals(0f, -4f))
    }

    @Test
    fun testCalcPoint() {
        val myXY0 = PointXY(0.0f, 0.0f)
        val myTriangle = TriangleK(3.0f, 4.0f, 5.0f, myXY0, 90.0f)
        Assert.assertEquals(myTriangle.point[1].x.toDouble(), 0.0, 0.001)
        Assert.assertEquals(myTriangle.point[1].y.toDouble(), 3.0, 0.001)
        Assert.assertEquals(myTriangle.point[2].x.toDouble(), 4.0, 0.001)
        Assert.assertEquals(myTriangle.point[2].y.toDouble(), 3.0, 0.001)
        Assert.assertEquals(myTriangle.innerangle[0].toDouble(), 53.130, 0.001)
        Assert.assertEquals(myTriangle.innerangle[1].toDouble(), 90.000, 0.001)
        Assert.assertEquals(myTriangle.innerangle[2].toDouble(), 36.869, 0.001)
    }
}