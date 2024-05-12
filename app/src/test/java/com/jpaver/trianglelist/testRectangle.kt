package com.jpaver.trianglelist

import org.junit.Assert
import org.junit.Test

class testRectangle {

    @Test
    fun testCalcPoint(){
        val rect = Rectangle(3f,4f,5f, 0f)
        Assert.assertEquals( 3f, rect.calcPoint().b.left.y )

        val rect2 = Rectangle(5f,5f,5f, -45f)
        Assert.assertEquals( 5f/1.414f, rect2.calcPoint().b.left.y,0.005f )

    }

}