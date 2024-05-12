package com.jpaver.trianglelist

import org.junit.Assert
import org.junit.Test

class testRectangle {

    val rect345 = Rectangle(3f,4f,5f )
    val rect45 = Rectangle(5f,5f,5f, -45f)
    val tri1  = Triangle(5f,5f,5f)

    @Test
    fun testNode(){
        val rect1 = rect45
        val rect2 = Rectangle(3f,4f,5f, nodeA = rect1 )
        Assert.assertEquals( 5f/1.414f, rect2.calcPoint().a.left.y,0.005f )
    }

    @Test
    fun testList(){
        val rectrilist = RectriangleList()
        rectrilist.add(rect345)
        rectrilist.add(tri1)
        Assert.assertEquals(2, rectrilist.list.size)
    }

    @Test
    fun testCalcPoint(){
        Assert.assertEquals( 3f, rect345.calcPoint().b.left.y )
        Assert.assertEquals( 5f/1.414f, rect45.calcPoint().b.left.y,0.005f )

    }

}