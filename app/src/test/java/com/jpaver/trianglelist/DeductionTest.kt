package com.jpaver.trianglelist
import com.jpaver.trianglelist.util.Params
import org.junit.Assert.assertEquals
import org.junit.Test

class DeductionTest {

    @Test
    fun testDedListClone(){
        val dedlist = DeductionList()
        dedlist.add( setup_deduction(1) )
        dedlist.add( setup_deduction(2) )

        val dedlist2 = dedlist.clone()

        compare( dedlist, dedlist2)

    }

    fun setup_deduction(num:Int): Deduction{
        return Deduction(
            num,
            "仕切弁",
            0.23f,
            0f,
            0,
            "Circle",
            0f,
            PointXY(3f, 3f),
            PointXY(4f, 4f)
        )
    }

    @Test
    fun testDedLocalPoint() {
        val ded = Deduction(1, "circle", 0.23f, 0f, 0, "Circle", 0f,
            PointXY(0f, 0f),
            PointXY(0f, 0f)
        )
        val tri = Triangle(5f,5f,5f )
        ded.flag( tri )
    }

        @Test
    fun testDedGetArea() {
        val ded = Deduction(1, "circle", 0.23f, 0f, 0, "Circle", 0f,
            PointXY(0f, 0f),
            PointXY(0f, 0f)
        )
        assertEquals(0.04f, ded.getArea(), 0.000001f)
        val dedded = Deduction(1, "circle", 0.67f, 0f, 0, "Circle", 0f,
            PointXY(0f, 0f),
            PointXY(0f, 0f)
        )
        assertEquals(0.35f, dedded.getArea(), 0.001f)
    }


    @Test
    fun testDedRotate() {
        val ded = Deduction(1, "masu", 1f, 1f, 0, "Box", 0f,
            PointXY(0f, 0f),
            PointXY(0f, 0f)
        )
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
        assertEquals(0, dedlist.getTapIndex(
            PointXY(
                1f,
                1f
            )
        ).toLong())
        assertEquals(1, dedlist.getTapIndex(
            PointXY(
                3f,
                3f
            )
        ).toLong())
    }

    @Test
    fun testDedReverse(){
        val dedlist = DeductionList()
        dedlist.add( Deduction(1, "仕切弁", 0.23f, 0f, 0, "Circle" ) )
        dedlist.add( Deduction(2, "仕切弁", 0.23f, 0f, 0, "Circle" ) )
        dedlist.add( Deduction(3, "汚水",   0.23f, 0f, 0, "Circle" ) )

        assertEquals("1.仕切弁 φ230", dedlist.get(1).getInfo() )
        assertEquals("2.仕切弁(2) φ230", dedlist.get(2).getInfo() )
        assertEquals("3.汚水 φ230", dedlist.get(3).getInfo() )

        dedlist.reverse()

        assertEquals("1.汚水 φ230", dedlist.get(1).getInfo() )
        assertEquals("2.仕切弁 φ230", dedlist.get(2).getInfo() )
        assertEquals("3.仕切弁(2) φ230", dedlist.get(3).getInfo() )

    }

    @Test
    fun testSameDedCount(){
        val ded1 = Deduction(
            1, "仕切弁", 0.23f, 0f, 0, "Circle", 0f,
            PointXY(0f, 0f),
            PointXY(
                0f,
                0f
            )
        )
        val ded2 = Deduction(
            Params(
                "仕切弁", "Circle", 2, 0.23f, 0f, 0f, 0, 0,
                PointXY(0f, 0f),
                PointXY(
                    0f,
                    0f
                )
            )
        )
        val dedlist = DeductionList()

        dedlist.add(ded1)
        dedlist.add(ded2)
        assertEquals(1, dedlist.get(2).sameDedcount )
        assertEquals("2.仕切弁(2) φ230", dedlist.get(2).getInfo() )

    }

    @Test
    fun testStringLength() {
        val str = "仕切弁 φ230"
        assertEquals(8, str.length.toLong())
    }

    @Test
    fun testDedClone() {
        val myDParam =
            Params("集水桝", "RECT", 3, 0.8f, 0.8f, 0f, 0, 0,
                PointXY(0.5f, 0.5f),
                PointXY(0f, 0f)
            )
        val myFirstD = Deduction(myDParam)
        val sD = myFirstD.clone()
        assertEquals("集水桝", sD.name)
    }

    @Test
    fun testDeduction() {
        val myDParam =
            Params("集水桝", "Box", 3, 0.8f, 0.8f, 0f, 0, 0,
                PointXY(0.5f, 0.5f),
                PointXY(0f, 0f)
            )
        val myFirstD = Deduction(myDParam)
        val myRectD = Deduction(myDParam)
        val myDList = DeductionList()
        myDList.add(myFirstD) //1
        myDList.add(myRectD) //2
        myDList.add(myDParam) //3
        myDList.add(myDParam) //4
        myDList.add(myDParam) //5
        myDList.getDeduction(3)?.num?.let { assertEquals(3f, it.toFloat(), 0.001f) }
        myDList.getDeduction(3)?.lengthX?.let { assertEquals(0.8, it.toDouble(), 0.001) }
        assertEquals("Box", myDList.getDeduction(3)?.type)
        myDList.getDeduction(3)?.point?.x?.let { assertEquals(0.5, it.toDouble(), 0.001) }
        myDList.remove(2) //RIP. myRectD
        myDList.getDeduction(1)?.num?.let { assertEquals(3, it.toLong()) }
        myDList.getDeduction(2)?.num?.let { assertEquals(2, it.toLong()) }
        myDList.getDeduction(3)?.num?.let { assertEquals(3, it.toLong()) }
        myDList.getDeduction(4)?.num?.let { assertEquals(4, it.toLong()) }
        myDList.replace(1, myDParam) //all of the world.
        //assertEquals(/* expected = */ "集水桝", /* actual = */ myDList.getDeduction(1)?.name ?: )
        myDList.move(PointXY(5f, 5f))
    }

}