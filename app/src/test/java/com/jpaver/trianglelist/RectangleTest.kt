package com.jpaver.trianglelist

import com.jpaver.trianglelist.editmodel.EditObject
import com.jpaver.trianglelist.editmodel.Rectangle
import com.jpaver.trianglelist.editmodel.RectriangleList
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
        val rectrilist = RectriangleList()
        rectrilist.add(rect345)
        rectrilist.add( tri1 as EditObject)
        Assert.assertEquals(2, rectrilist.list.size)
    }

    fun case1(){
        val rect1 = Rectangle(5.0,5.0,5.0, nodeA = rect555 )
        Assert.assertEquals( 10.0, rect1.calcPoint().b.left.y,0.005 )
    }

    fun case2(){
        val rect2 = Rectangle(5.0,5.0,5.0, nodeA = rect45 )
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