package com.jpaver.trianglelist

import com.jpaver.trianglelist.util.InputParameter
import junit.framework.TestCase.assertSame
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Test
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.reflect.full.memberProperties

fun compare(target1: Any, target2:Any ){
    println("target1: ${target1.hashCode()}, $target1")
    println("target2: ${target2.hashCode()}, $target1")
}
//@RunWith(PowerMockRunner.class)
//@PrepareForTest(Log.class)
class TriangleTest {

    //PathAndOffsetはクローンしたらどうなる？
    @Test
    fun testClonePathAndOffset(){
        val triangle = Triangle(1f,3f,3f)
        val triangle2 = triangle.clone()

        compare(triangle.dimOnPath[0].dimpoint,triangle2.dimOnPath[0].dimpoint)
    }



    //dimpointの相互干渉を検出するテスト
    @Test
    fun testDetectDimCollide(){
        val triangle = Triangle(1f,3f,3f)

        // 最初の点から他の点への距離のリストを計算
        val distances = listup_dimpoint_distances(triangle)
        val booleanList = check_distances(distances, 1.0f)
        println("distances: $distances")
        println("distances: $booleanList")

    }


    fun check_distances(distances: List<Float>, nearby: Float): List<Boolean> {
        // 距離が1.0fより小さく、0ではない条件を満たすかどうかでBooleanリストを生成
        return distances.map { it > 0f && it < nearby }
    }

    fun listup_dimpoint_distances(triangle: Triangle ): List<Float>{
        val distances = listup_dimpoint_distances(triangle, 0) + listup_dimpoint_distances(triangle, 1) + listup_dimpoint_distances(triangle, 2)
        return distances
    }

    fun listup_dimpoint_distances(triangle: Triangle, index_basepoint: Int): List<Float> {
        val dimpoint = arrayOf( triangle.dimpoint.a,triangle.dimpoint.b,triangle.dimpoint.c)

        return dimpoint[index_basepoint].distancesTo(dimpoint)
    }

    //とりあえず生成したばかりの三角形にどんなふうにdimPointが入るか確認
    @Test
    fun testDimPoint(){
        val triangle = Triangle(5f,5f,5f)
        val triangle2 = triangle.clone()

        println("dimpoint: ${triangle.dimpoint}")
        println("dimpoint2: ${triangle2.dimpoint}")

        // オブジェクトのハッシュコードが一致するか？
        println("triangle のハッシュコード: ${triangle.dimpoint.a.hashCode()}")
        println("triangle2 のハッシュコード: ${triangle2.dimpoint.a.hashCode()}")
        println("triangle のハッシュコード: ${triangle.length[0].hashCode()}")
        //triangle2.length[0] = 3.0f
        println("triangle2 のハッシュコード: ${triangle2.length[0].hashCode()}")
        println("triangle のハッシュコード: ${triangle.length.hashCode()}")
        //triangle2.length[0] = 3.0f
        println("triangle2 のハッシュコード: ${triangle2.length.hashCode()}")


        // Triangleクラスのプロパティをリフレクションを使って取得
        val properties = triangle::class.memberProperties
        for (prop in properties) {
            //println("${prop.name} = ${prop.call(triangle)}")
        }
    }

    //ノードポインタやオブジェクトが参照を返すのはどんなときか？テスト
    @Test
    fun testSameObjectPropaties(){
        val triangleA = Triangle()
        val triangleB = Triangle()
        triangleB.nodeA = triangleA // 参照インスタンス代入

        // オブジェクトのハッシュコードが一致するか？
        println("triangleA のハッシュコード: ${triangleA.hashCode()}")
        println("triangleB.nodeTriangleA のハッシュコード: ${triangleB.nodeA.hashCode()}")

        assertSame( "BのnodeTriangleA と A は異なるオブジェクトを参照しています。", triangleB.nodeA, triangleA )
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
            Triangle(InputParameter("", "", 1, 5f, 5f, 5f, -1, -1, PointXY(0f, 0f), PointXY(0f, 0f)), 0f)
        val connection = ConnParam(1, 0, 0, 5f)
        val two = Triangle(one, connection, 6f, 6f)

        // 内容の一致
        Assert.assertEquals(one.mynumber.toLong(), two.nodeA!!.mynumber.toLong())
        Assert.assertEquals(one.nodeB!!.mynumber.toLong(), two.mynumber.toLong())

        // オブジェクトポインタの一致。
        Assert.assertEquals(one, two.nodeA)
        Assert.assertEquals(one.nodeB, two)
        two.setOn(one, 2, 5f, 5f)

        // オブジェクトポインタの一致。
        Assert.assertEquals(one, two.nodeA)
        Assert.assertEquals(one.nodeC, two)
    }

    @Test
    fun testSetPointersFromParams() {
        val one =
            Triangle(InputParameter("", "", 1, 5f, 5f, 5f, -1, -1, PointXY(0f, 0f), PointXY(0f, 0f)), 0f)
        val two =
            Triangle(one, InputParameter("", "", 2, 5f, 5f, 5f, 1, 1, PointXY(0f, 0f), PointXY(0f, 0f)))

        // 内容の一致
        Assert.assertEquals(one.mynumber.toLong(), two.nodeA!!.mynumber.toLong())
        Assert.assertEquals(one.nodeB!!.mynumber.toLong(), two.mynumber.toLong())

        // オブジェクトポインタの一致。
        Assert.assertEquals(one, two.nodeA)
        Assert.assertEquals(one.nodeB, two)
        two.setOn(one, 2, 5f, 5f)

        // オブジェクトポインタの一致。
        Assert.assertEquals(one, two.nodeA)
        Assert.assertEquals(one.nodeC, two)
    }

    @Test
    fun testSetObjectPointers() {
        val one = Triangle(5f, 5f, 5f, PointXY(0f, 0f), 0f)
        val two = Triangle(one, 1, 5f, 5f)

        // 内容の一致
        Assert.assertEquals(one.mynumber.toLong(), two.nodeA!!.mynumber.toLong())
        Assert.assertEquals(one.nodeB!!.mynumber.toLong(), two.mynumber.toLong())

        // オブジェクトポインタの一致。
        Assert.assertEquals(one, two.nodeA)
        Assert.assertEquals(one.nodeB, two)
        two.setOn(one, 2, 5f, 5f)

        // オブジェクトポインタの一致。
        Assert.assertEquals(one, two.nodeA)
        Assert.assertEquals(one.nodeC, two)
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
        me = t.dim.cycleIncrement(me)
        // こうやって代入しない限り、meは渡された時点でクローンに代わり、副作用っぽく変化したりしない。
        // オブジェクトは違う。引数として生で渡された時、ポインタとして渡されるので、副作用が起きる。
        // t.sideEffectGo( object.clone ) とかすれば予防できる。
        Assert.assertEquals(1, me.toLong())
    }

    @Test
    fun testCalcDimAngle() {
        val tri = Triangle(5f, 5f, 5f)
        val angle360 = tri.point[0].calcAngle360(tri.pointAB, tri.pointBC)
        Assert.assertEquals(300f, angle360, 0.0001f)
        Assert.assertEquals(300f, tri.pointBC.calcAngle360(tri.point[0], tri.pointAB), 0.0001f)
        Assert.assertEquals(300f, tri.pointAB.calcAngle360(tri.pointBC, tri.point[0]), 0.0001f)
    }

    @Test
    fun testDimSideAlign() {
        val tri1 = Triangle(3f, 4f, 5f)
        tri1.controlDimHorizontal(0)
        Assert.assertEquals(1, tri1.dim.horizontal.a)
        tri1.setDimPoint()
        Assert.assertEquals(-2.325f, tri1.dimpoint.a.x, 0.001f)
        var dim = PointXY(-1.5f, 0f)
        val offsetLeft = PointXY(-3f, 0f)
        val offsetRight = PointXY(0f, 0f)
        var haba = dim.lengthTo(offsetLeft) * 0.5f
        Assert.assertEquals(0.75f, haba, 0.001f)
        haba = dim.lengthTo(offsetRight) * 0.5f
        Assert.assertEquals(0.75f, haba, 0.001f)
        dim = dim.offset(offsetLeft, haba)
        Assert.assertEquals(-2.25f, dim.x, 0.001f)
        tri1.controlDimHorizontal(0)
        tri1.setDimPoint()
        Assert.assertEquals(-0.67f, tri1.dimpoint.a.x, 0.01f)
        tri1.controlDimVertical(0)
        Assert.assertEquals(3, tri1.dim.vertical.a)
        tri1.controlDimVertical(0)
        Assert.assertEquals(1, tri1.dim.vertical.a)
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
            InputParameter("集水桝", "Box", 3, 0.8f, 0.8f, 0f, 0, 0, PointXY(0.5f, 0.5f), PointXY(0f, 0f))
        val myD = Deduction(myDParam)
        myD.move(PointXY(5f, 5f))
        Assert.assertEquals(5.5f, myD.point.x, 0.001f)
    }

    @Test
    fun testDimPathAndOffset() {
        val t1 = Triangle(3f, 4f, 5f)

        // 1下 3上 -> // 夾角の、1:外 　3:内
        Assert.assertEquals(1, t1.dim.vertical.a)
        Assert.assertEquals(1, t1.dim.vertical.b)
        Assert.assertEquals(1, t1.dim.vertical.c)
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
