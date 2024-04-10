package com.jpaver.trianglelist

import org.junit.Assert
import org.junit.Test

class TrilistDimTest {
    @Test
    fun testDimAlign() {
        val mytri1 = Triangle(
            2.0f, 2.3f, 1.2f,
            PointXY(0f, 0f), 180.0f
        )
        val myTrilist = TriangleList(mytri1)
        myTrilist.add(Triangle(mytri1, 3, 2.5f, 1.1f, 2.0f), true) //2

        // 1下 3上 -> // 夾角の、1:外 　3:内
        Assert.assertEquals(1, myTrilist.getBy(1).dim.vertical.a)
        Assert.assertEquals(1, myTrilist.getBy(1).dim.vertical.b)
        Assert.assertEquals(1, myTrilist.getBy(1).dim.vertical.c)
        Assert.assertEquals(1, myTrilist.getBy(2).dim.vertical.a)
        Assert.assertEquals(1, myTrilist.getBy(2).dim.vertical.b)
        Assert.assertEquals(1, myTrilist.getBy(2).dim.vertical.c)
        val t1 = Triangle(1.0f, 1.5f, 1.0f)
        Assert.assertEquals(1, t1.dim.vertical.a)
        Assert.assertEquals(1, t1.dim.vertical.b)
        Assert.assertEquals(1, t1.dim.vertical.c)
    }

    @Test
    fun testAlignDimsInExportDXF() {
        val trilist = TriangleList()
        trilist.add(Triangle(5f, 5f, 5f), true)
        trilist.add(Triangle(trilist.get(1), 2, 7f, 7f), true)
        trilist.add(Triangle(trilist.get(1), 1, 6f, 6f), true)

        val dxfwriter = DxfFileWriter(trilist)
        val tri = trilist.get(1)
        val pca = tri.pointCA
        val pab = tri.pointAB
        val pbc = tri.pointBC
        val alignVdimA = dxfwriter.alignVByVector(tri.dimVerticalA, pca, pab)
        val alignVdimB = dxfwriter.alignVByVector(
            tri.dimVerticalB,
            pab,
            pbc
        )//flip(tri.myDimAlignB_, tri.dimAngleB_ )
        val alignVdimC = dxfwriter.alignVByVector(
            tri.dimVerticalC,
            pbc,
            pca
        )//flip(tri.myDimAlignC_, tri.dimAngleC_ )

        Assert.assertEquals(3, alignVdimA)
        Assert.assertEquals(1, alignVdimB)
        Assert.assertEquals(1, alignVdimC)

        Assert.assertEquals(4, "No.3".length)

    }
}