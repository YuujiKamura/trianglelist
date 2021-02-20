package com.jpaver.trianglelist

import org.junit.Assert
import org.junit.Test

class MathTest {

    @Test
    fun testRoundInt() {
        var count3 = 0.05f
        var count2 = 0.5f
        val count1 = 1f
        Assert.assertEquals(0, count3.toInt())
        Assert.assertEquals(0, count2.toInt())
        Assert.assertEquals(1, count1.toInt())
        Assert.assertEquals(0, Math.round(count3).toLong())
        Assert.assertEquals(1, Math.round(count2).toLong())
        if (count3 < 0.1) count3 *= 10f
        Assert.assertEquals(1, Math.round(count3).toLong())
        count2 = 0.49f
        Assert.assertEquals(0, Math.round(count2).toLong())
        if (count2 < 0.5) count2 += count2
        Assert.assertEquals(1, Math.round(count2).toLong())
    }

    @Test
    fun testRoundByTwo() {
        //      float rfp = roundByUnderTwo(3.141592f);
//        assertEquals(3.14f, rfp, 0.01f);
    }

    @Test
    fun testSineCosine() {
        val S = Math.sin(Math.toRadians(90.0))
        val C = Math.cos(Math.toRadians(90.0))
        Assert.assertEquals(S, 1.0, 0.001)
        Assert.assertEquals(C, 0.0, 0.001)
    }
}