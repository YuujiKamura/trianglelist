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
        val alignVdimA = tri.dimOnPath[0].verticalDxf()//dxfwriter.verticalFromBaseline(tri.dimVerticalA, pca, pab)
        val alignVdimB = tri.dimOnPath[1].verticalDxf()//dxfwriter.verticalFromBaseline(tri.dimVerticalB,pab,pbc)//flip(tri.myDimAlignB_, tri.dimAngleB_ )
        val alignVdimC = tri.dimOnPath[2].verticalDxf()//dxfwriter.verticalFromBaseline(tri.dimVerticalC,pbc,pca)//flip(tri.myDimAlignC_, tri.dimAngleC_ )
        val alignVdimD = dxfwriter.verticalFromBaseline(tri.dimVerticalA, pca, pab)
        val alignVdimE = dxfwriter.verticalFromBaseline(tri.dimVerticalB,pab,pbc)//flip(tri.myDimAlignB_, tri.dimAngleB_ )
        val alignVdimF = dxfwriter.verticalFromBaseline(tri.dimVerticalC,pbc,pca)//flip(tri.myDimAlignC_, tri.dimAngleC_ )
        val alignVdimG = dxfwriter.verticalFromBaseline(tri.dim.vertical.a, pca, pab)
        val alignVdimH = dxfwriter.verticalFromBaseline(tri.dim.vertical.b,pab,pbc)//flip(tri.myDimAlignB_, tri.dimAngleB_ )
        val alignVdimI = dxfwriter.verticalFromBaseline(tri.dim.vertical.c,pbc,pca)//flip(tri.myDimAlignC_, tri.dimAngleC_ )

        Assert.assertEquals(1,tri.dimOnPath[0].vertical)
        Assert.assertEquals(1,tri.dimVerticalA)
        Assert.assertEquals(1,tri.dim.vertical.a)
        Assert.assertEquals(3,tri.dimOnPath[1].vertical)
        Assert.assertEquals(3,tri.dim.vertical.b)
        Assert.assertEquals(1,tri.dimVerticalB)
        Assert.assertEquals(3,tri.dimOnPath[2].vertical)
        Assert.assertEquals(3,tri.dim.vertical.c)
        Assert.assertEquals(1,tri.dimVerticalC)

        Assert.assertEquals(1, alignVdimA)
        Assert.assertEquals(3, alignVdimB)
        Assert.assertEquals(3, alignVdimC)

        Assert.assertEquals(3, alignVdimD)
        Assert.assertEquals(1, alignVdimE)
        Assert.assertEquals(1, alignVdimF)

        Assert.assertEquals(3, alignVdimG)
        Assert.assertEquals(3, alignVdimH)
        Assert.assertEquals(3, alignVdimI)

        Assert.assertEquals(4, "No.3".length)

        printTriangle(tri)

    }
}