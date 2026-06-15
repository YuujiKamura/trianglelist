package com.jpaver.trianglelist

import com.jpaver.trianglelist.editmodel.EditList
import com.jpaver.trianglelist.editmodel.EditObject
import com.jpaver.trianglelist.editmodel.Rectangle
import com.jpaver.trianglelist.editmodel.Triangle
import org.junit.Assert
import org.junit.Test

class RectangleTest {

    val rect345 = Rectangle(3.0,4.0,5.0 )
    val rect555 = Rectangle(5.0,5.0,5.0 )
    val rect45 = Rectangle(5.0,5.0,5.0, -45.0)
    val tri1  = Triangle(5f,5f,5f)

    @Test
    fun testList(){
        // 混在の土台: 一本の EditList に台形(Rectangle)と三角形(Triangle)を
        // EditObject メンバーとして並べて持てる。種別ごとにリスト型を分けない。
        val shapes = EditList<EditObject>()
        shapes.add(rect345)
        shapes.add(tri1)
        Assert.assertEquals(2, shapes.size())
    }

    // R3: initByParent の Rectangle 分岐が side を尊重するようになった (旧: 上辺C 固定で side 無視)。
    // これらは「親の上辺(C)に子の底辺を乗せる」幾何を検証しているので side=2 を明示する。
    // getLine(2)=Line(lp.b.left,lp.b.right) は旧 parent.calcPoint().b と同一 → 期待値はビット不変。
    fun case1(){
        val rect1 = Rectangle(5.0,5.0,5.0, nodeA = rect555, side = 2 )
        Assert.assertEquals( 10.0, rect1.calcPoint().b.left.y,0.005 )
    }

    fun case2(){
        val rect2 = Rectangle(5.0,5.0,5.0, nodeA = rect45, side = 2 )
        Assert.assertEquals( 5.0/1.414, rect2.calcPoint().a.left.y,0.005 )
        Assert.assertEquals( 5.0/1.414*2, rect2.calcPoint().b.left.y,0.005 )
    }

    @Test
    fun testNode(){
        case1()
        case2()

        val rect3 = Rectangle(5.0,5.0,5.0, nodeA = tri1 )
        Assert.assertEquals( 0.0, rect3.calcPoint().a.left.y,0.005 )
    }

    @Test
    fun testCalcPoint(){
        Assert.assertEquals( 3.0, rect345.calcPoint().b.left.y, 0.0 )
        Assert.assertEquals( 5.0/1.414, rect45.calcPoint().b.left.y,0.005 )
    }
}