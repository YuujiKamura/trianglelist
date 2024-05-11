package com.jpaver.trianglelist

import org.junit.Assert
import org.junit.Test

class testRectangle {

    @Test
    fun testCalcPoint(){
        val rect = Rectangle(3f,4f,5f)

        Assert.assertEquals( 3f, rect.calcPoint().left.y )
    }

}