package com.jpaver.trianglelist

import com.jpaver.trianglelist.editmodel.EditObject
import com.jpaver.trianglelist.editmodel.Rectangle
import com.jpaver.trianglelist.editmodel.RectriangleList
import com.jpaver.trianglelist.editmodel.Triangle
import org.junit.Assert
import org.junit.Test

class RectangleTest {

    val rect345 = Rectangle(3f,4f,5f )
    val rect555 = Rectangle(5f,5f,5f )
    val rect45 = Rectangle(5f,5f,5f, -45f)
    val tri1  = Triangle(5f,5f,5f)

    @Test
    fun testList(){
        val rectrilist = RectriangleList()
        rectrilist.add(rect345)
        rectrilist.add( tri1 as EditObject)
        Assert.assertEquals(2, rectrilist.list.size)
    }

    fun case1(){
        val rect1 = Rectangle(5f,5f,5f, nodeA = rect555 )
        Assert.assertEquals( 10f, rect1.calcPoint().b.left.y,0.005f )
    }

    fun case2(){
        val rect2 = Rectangle(5f,5f,5f, nodeA = rect45 )
        Assert.assertEquals( 5f/1.414f, rect2.calcPoint().a.left.y,0.005f )
        Assert.assertEquals( 5f/1.414f*2, rect2.calcPoint().b.left.y,0.005f )
    }

    @Test
    fun testNode(){
        case1()
        case2()

        val rect3 = Rectangle(5f,5f,5f, nodeA = tri1 )
        Assert.assertEquals( 0f, rect3.calcPoint().a.left.y,0.005f )
    }

    @Test
    fun testCalcPoint(){
        Assert.assertEquals( 3f, rect345.calcPoint().b.left.y )
        Assert.assertEquals( 5f/1.414f, rect45.calcPoint().b.left.y,0.005f )
    }
}