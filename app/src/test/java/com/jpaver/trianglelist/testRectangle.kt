package com.jpaver.trianglelist

import org.junit.Assert
import org.junit.Test

class testRectangle {

    @Test
    fun testCalcPoint(){
        val rect = Rectangle(3f,4f,5f, 0f)
        Assert.assertEquals( 3f, rect.calcPoint().left.y )

        val rect2 = Rectangle(5f,5f,5f, -45f)
        Assert.assertEquals( 5f, rect2.calcPoint().left.y )

    }

}