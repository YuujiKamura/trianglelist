package com.jpaver.trianglelist
import org.junit.Assert.assertEquals
import org.junit.Test

class DeductionTest {

    @Test
    fun testDedRotate() {
        val ded = Deduction(1, "masu", 1f, 1f, 0, "Box", 0f, PointXY(0f, 0f), PointXY(0f, 0f))
        ded.rotateShape(ded.point, 45f)
        assertEquals(-0.707, ded.plb.x.toDouble(), 0.001)
    }

    @Test
    fun testDedListTap() {
        val dedlist = DeductionList()
        dedlist.add(
            Deduction(
                1,
                "仕切弁",
                0.23f,
                0f,
                0,
                "Circle",
                0f,
                PointXY(1f, 1f),
                PointXY(2f, 2f)
            )
        )
        dedlist.add(
            Deduction(
                2,
                "仕切弁",
                0.23f,
                0f,
                0,
                "Circle",
                0f,
                PointXY(3f, 3f),
                PointXY(4f, 4f)
            )
        )
        assertEquals(0, dedlist.getTapIndex(PointXY(1f, 1f)).toLong())
        assertEquals(1, dedlist.getTapIndex(PointXY(3f, 3f)).toLong())
    }

    @Test
    fun SameDedCountTest(){
        val ded1: Deduction = Deduction(
            1, "仕切弁", 0.23f, 0f, 0, "Circle", 0f, PointXY(0f, 0f), PointXY(
                0f,
                0f
            )
        )
        val ded2: Deduction = Deduction(
            Params(
                "仕切弁", "Circle", 2, 0.23f, 0f, 0f, 0, 0, PointXY(0f, 0f), PointXY(
                    0f,
                    0f
                )
            )
        )
        val dedlist = DeductionList()

        dedlist.add(ded1)
        dedlist.add(ded2)
        dedlist.add(ded2)
        assertEquals(3, ded2.sameDedcount)
        assertEquals("2.仕切弁(3) φ230", ded2.getInfo())
    }

    @Test
    fun testStringLength() {
        val str = "仕切弁 φ230"
        assertEquals(8, str.length.toLong())
    }

    @Test
    fun testDedClone() {
        val myDParam =
            Params("集水桝", "RECT", 3, 0.8f, 0.8f, 0f, 0, 0, PointXY(0.5f, 0.5f), PointXY(0f, 0f))
        val myFirstD = Deduction(myDParam)
        val sD = myFirstD.clone()
        assertEquals("集水桝", sD.name)
    }

    @Test
    fun testDeduction() {
        val myDParam =
            Params("集水桝", "Box", 3, 0.8f, 0.8f, 0f, 0, 0, PointXY(0.5f, 0.5f), PointXY(0f, 0f))
        val myFirstD = Deduction(myDParam)
        val myRectD = Deduction(myDParam)
        val myDList = DeductionList()
        myDList.add(myFirstD) //1
        myDList.add(myRectD) //2
        myDList.add(myDParam) //3
        myDList.add(myDParam) //4
        myDList.add(myDParam) //5
        assertEquals(3f, myDList.getDeduction(3).num.toFloat(), 0.001f)
        assertEquals(0.8, myDList.getDeduction(3).lengthX.toDouble(), 0.001)
        assertEquals("Box", myDList.getDeduction(3).type)
        assertEquals(0.5, myDList.getDeduction(3).point.x.toDouble(), 0.001)
        myDList.remove(2) //RIP. myRectD
        assertEquals(3, myDList.getDeduction(1).num.toLong())
        assertEquals(2, myDList.getDeduction(2).num.toLong())
        assertEquals(3, myDList.getDeduction(3).num.toLong())
        assertEquals(4, myDList.getDeduction(4).num.toLong())
        myDList.replace(1, myDParam) //all of the world.
        assertEquals("集水桝", myDList.getDeduction(1).name)
        myDList.move(PointXY(5f, 5f))
    }

}