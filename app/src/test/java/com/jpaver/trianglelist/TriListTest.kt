package com.jpaver.trianglelist

import junit.framework.Assert.assertEquals
import org.junit.Assert
import org.junit.Test

class TriListTest {

    @Test
    fun testTriListOutline(){
        val trilist = TriangleList()
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        trilist.add( Triangle(5f, 5f, 5f))
        trilist.add( 1, 4,6f, 5f, 5f )
        trilist.add( 2, 5,6f, 5f, 5f )
        trilist.add( 3, 1,5f, 5f )
        trilist.add( 4, 9,5f,5f, 5f )

        val tlop = trilist.getOutLinePoints( 0 )
        assertEquals( 8, tlop.size )
    }

    @Test
    fun testConnectionTypeLoad(){
        val tri1 = Triangle(3f, 4f, 5f)
        val tri2 = Triangle(tri1, 3, 3f, 4f, 5f)
        val tri3 = Triangle(tri2, ConneParam(1, 0, 2, 0f), 3f, 5f)
        val tri4 = Triangle(tri3, 4, 3f, 4f, 5f)
        val tri5 = Triangle(tri4, 6, 3f, 4f, 5f)
        val tri6 = Triangle(tri5, ConneParam(1, 1, 0, 4f), 3f, 5f)
        val tri7 = Triangle(tri6, ConneParam(2, 1, 0, 4f), 3f, 5f)
        val tri8 = Triangle(tri7, 5, 3f, 4f, 5f)
        val tri9 = Triangle(tri7, 7, 3f, 4f, 5f)
        val tri10 = Triangle(tri7, 8, 3f, 4f, 5f)
        val tri11 = Triangle(tri7, 9, 3f, 4f, 5f)
        val tri12 = Triangle(tri7, 10, 3f, 4f, 5f)

        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC

        assertEquals( 2, tri2.cParam_.lcr )
        assertEquals( 2, tri3.cParam_.lcr )
        assertEquals( 0, tri4.cParam_.lcr )
        assertEquals( 0, tri5.cParam_.lcr )
        assertEquals( 0, tri6.cParam_.lcr )
        assertEquals( 0, tri7.cParam_.lcr )
        assertEquals( 2, tri8.cParam_.lcr )
        assertEquals( 1, tri9.cParam_.lcr )
        assertEquals( 1, tri10.cParam_.lcr )
        assertEquals( 2, tri11.cParam_.lcr )
        assertEquals( 2, tri12.cParam_.lcr )
    }

    @Test
    fun testResetConnectedTriangles() {
        val trilist = TriangleList()
        trilist.add(Triangle(5f, 5f, 5f))

        val params = Params("", "", 1 , 7f,7f, 7f, 0, 1 )
        trilist.resetConnectedTriangles( params )

    }



    @Test
    fun testAlignDimsInExportDXF() {
        val trilist = TriangleList()
        trilist.add(Triangle(5f, 5f, 5f))
        trilist.add(Triangle(trilist.get(1), 2, 7f, 7f))
        trilist.add(Triangle(trilist.get(1), 1, 6f, 6f))

        val dxfwriter = DxfFileWriter( trilist )
        val tri = trilist.get(1)
        val pca = tri.pointCA_
        val pab = tri.pointAB_
        val pbc = tri.pointBC_
        var alignVdimA = dxfwriter.alignVByVector(tri.myDimAlignA_, pca, pab)
        val alignVdimB = dxfwriter.alignVByVector(tri.myDimAlignB_, pab, pbc)//flip(tri.myDimAlignB_, tri.dimAngleB_ )
        val alignVdimC = dxfwriter.alignVByVector(tri.myDimAlignC_, pbc, pca)//flip(tri.myDimAlignC_, tri.dimAngleC_ )

        Assert.assertEquals( 1, alignVdimA )
        Assert.assertEquals( 3, alignVdimB )
        Assert.assertEquals( 3, alignVdimC )

        Assert.assertEquals( 4, "No.3".length )


    }


    @Test
    fun testExportPDF() {
        val trilist = TriangleList()
        trilist.add(Triangle(5f, 5f, 5f))
        trilist.add(Triangle(trilist.get(1), 2, 7f, 7f))
        trilist.add(Triangle(trilist.get(1), 1, 6f, 6f))

        val pdfwriter = PdfWriter( 1f, trilist )
    }

    @Test
    fun testSeparateChild() {
        val trilist = TriangleList()
        trilist.add(Triangle(5f, 5f, 5f))
        trilist.add(Triangle(trilist.get(1), 2, 7f, 7f))
        trilist.add(Triangle(trilist.get(1), 1, 6f, 6f))

        Assert.assertEquals( true, trilist.get(1).hasChildIn(2) )
        Assert.assertEquals( true, trilist.get(1).hasChildIn(1) )

        trilist.add(Triangle(trilist.get(2), 1, 6f, 6f))
        Assert.assertEquals( true, trilist.get(2).hasChildIn(1) )
        Assert.assertEquals( false, trilist.get(2).hasChildIn(2) )

        trilist.add(Triangle(trilist.get(2), 2, 6f, 6f))
        Assert.assertEquals( true, trilist.get(2).hasChildIn(1) )


    }


    @Test
    fun testParentHaveAChild() {
        val trilist = TriangleList()
        trilist.add(Triangle(5f, 5f, 5f))
        trilist.add(Triangle(trilist.get(1), 2, 7f, 7f))
        trilist.add(Triangle(trilist.get(2), 2, 6f, 6f))

        Assert.assertEquals( true, trilist.get(1).hasChildIn(2) )
    }

    @Test
    fun testGetNumberList() {
        val trilist = TriangleList()
        trilist.add(Triangle(5f, 5f, 5f))
        trilist.add(Triangle(trilist.trilist_[0], 2, 5f, 5f))
        trilist.add(Triangle(trilist.trilist_[0], 1, 6f, 6f))
        trilist.trilist_[0].myName_ = "No.10"
        trilist.trilist_[1].myName_ = "No.6"
        trilist.trilist_[2].myName_ = "No.2"

        val numlist = trilist.getSokutenList( 2, 4 )

//        Assert.assertEquals("No.2", numlist[0].myName_ )
        Assert.assertEquals("No.6", numlist[2].myName_ )

        //Assert.assertEquals(-2.5f, numlist[0].pointCA_.vectorTo( numlist[1].pointCA_ ).x )
        //Assert.assertEquals(-2.5f, numlist.vectorToNextFrom( 0 ).x )

       Assert.assertEquals(-4, trilist.sokutenListVector )

    }

    @Test
    fun testTriListSlide() {
        val trilist = TriangleList()
        trilist.add(Triangle(5f, 5f, 5f))
        trilist.add(Triangle(trilist.get(1), 2, 5f, 5f))
        trilist.add(Triangle(trilist.get(1), 2, 6f, 6f))
        trilist.setChildsToAllParents() // これやらないと認知されない。

        Assert.assertEquals(true, trilist.get(1).alreadyHaveChild(2))
        Assert.assertEquals(false, trilist.get(1).alreadyHaveChild(1))
        Assert.assertEquals(3.473, trilist.get(2).pointBC_.x.toDouble(), 0.001)
        Assert.assertEquals(4.978, trilist.get(3).pointBC_.x.toDouble(), 0.001)
    }

    @Test
    fun testTriListGetTap() {
        val tri1 = Triangle(3f, 4f, 5f)
        val tri2 = Triangle(tri1, ConneParam(1, 0, 2, 0f), 3f, 4f)
        val tri3 = Triangle(tri1, ConneParam(2, 0, 2, 0f), 3f, 5f)
        val trilist = TriangleList(tri1)
        trilist.add(tri2)
        trilist.add(tri3)
        Assert.assertEquals(3, trilist.getTapIndexArray(PointXY(-3f, 2f)).size.toLong())
        Assert.assertEquals(2, trilist.getTapHitCount(PointXY(-3f, 2f)).toLong())
    }

    @Test
    fun testConnectionType3() {
        val tri1 = Triangle(3f, 4f, 5f)
        val tri2 = Triangle(tri1, 3, 3f, 4f, 5f)
        val tri3 = Triangle(tri2, ConneParam(1, 0, 2, 0f), 3f, 5f)
        val trilist = TriangleList(tri1)
        trilist.add(tri2)
        trilist.add(tri3)
        tri2.reset(tri2, ConneParam(1, 1, 1, 3f))
        trilist.lastTapNum_ = 2
        trilist.rotateCurrentTriLCR()
        Assert.assertEquals(4f, tri3.pointCA_.y, 0.0001f)
        trilist.resetTriConnection(2, ConneParam(1, 1, 0, 3f))
        Assert.assertEquals(4f, tri3.pointCA_.y, 0.0001f)
        val tri4 = Triangle(tri1, ConneParam(1, 1, 2, 3f), 3f, 5f)
        Assert.assertEquals(true, tri4.pointCA_.equals(-3f, 4f))
        val tri5 = Triangle(tri1, ConneParam(2, 1, 2, 3f), 3f, 5f)
        Assert.assertEquals(true, tri5.pointCA_.equals(0f, 0f))
        val tri8 =
            Triangle(tri1, Params("", "", 2, 3f, 3f, 5f, 2, 5, tri1.pointCA_, PointXY(0f, 0f)))
        Assert.assertEquals(false, tri8.pointCA_.equals(0f, 0f))
        val tri6 = Triangle(tri1, ConneParam(2, 1, 0, 3f), 3f, 5f)
        Assert.assertEquals(true, tri6.pointCA_.equals(-1.1999f, 1.5999f))
        val tri7 =
            Triangle(tri1, Params("", "", 2, 3f, 3f, 5f, 2, 6, tri1.pointCA_, PointXY(0f, 0f)))
        Assert.assertEquals(true, tri7.pointCA_.equals(-1.1999f, 1.5999f))
    }

    @Test
    fun testConnectionType2() {
        val tri1 = Triangle(3f, 4f, 5f)
        val tri2 = Triangle(tri1, 9, 3f, 4f, 5f)// 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC

        // test float connection
        Assert.assertEquals(-3f, tri1.pointAB_.x, 0.0001f)

        Assert.assertEquals(-4f, tri2.pointCA_.x, 0.0001f)


        Assert.assertEquals(3.5f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(3.0f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(4.0f, tri2.rotateLCR().y, 0.0001f)

        val tri3 = Triangle(tri2, ConneParam(1, 0, 2, 3f), 3f, 5f)
        Assert.assertEquals(1f, tri3.pointCA_.y, 0.0001f)

        tri3.resetByParent(tri2.rotateLCRandGet(), tri3.cParam_)
        Assert.assertEquals(3.0f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(0.5f, tri3.pointCA_.y, 0.0001f)
        tri3.resetByParent(tri2.rotateLCRandGet(), tri3.cParam_)
        Assert.assertEquals(3.5f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(1f, tri3.pointCA_.y, 0.0001f)
        tri3.resetByParent(tri2.rotateLCRandGet(), tri3.cParam_)
        Assert.assertEquals(4.0f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(0f, tri3.pointCA_.y, 0.0001f)
        val trilist = TriangleList(tri1)
        trilist.add(tri2)
        trilist.add(tri3)
        trilist.lastTapNum_ = 2
        trilist.rotateCurrentTriLCR() //3
        trilist.rotateCurrentTriLCR() //3.5
        Assert.assertEquals(4.0f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(3.5f, tri2.rotateLCR().y, 0.0001f)
        trilist.rotateCurrentTriLCR() //3.0
        Assert.assertEquals(4f, tri3.pointCA_.y, 0.0001f)
    }


    @Test
    fun testConnectionType() {
        val tri1 = Triangle(5f, 5f, 5f)
        val tri2 = Triangle(tri1, 3, 4f, 5f, 5f)

        //tri2.setConnectionType( 1, 1, 1, 4f);
        Assert.assertEquals(-5f, tri2.parent_.pointAB_.x, 0.0001f)
        Assert.assertEquals(-2.75f, tri2.getParentPointByLCR(1, 1).x, 0.0001f)
        Assert.assertEquals(-2.5f, tri2.getParentPointByType(1, 0, 1).x, 0.0001f)
        Assert.assertEquals(-2.75f, tri2.getParentPointByType(1, 1, 1).x, 0.0001f)
        Assert.assertEquals(-3.6160f, tri2.getParentPointByType(1, 2, 1).x, 0.0001f)
        Assert.assertEquals(4.3971f, tri2.getParentPointByType(1, 2, 1).y, 0.0001f)
        Assert.assertEquals(4.3971f, tri2.setBasePoint(1, 2, 1).y, 0.0001f)
        Assert.assertEquals(3.4641f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(4.3301f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(3.8971f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(3.4641f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(4.3301f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(3.8971f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(3.4641f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(4.3301f, tri2.rotateLCR().y, 0.0001f)
        val trilist = TriangleList(tri1)
        trilist.add(tri2)
        Assert.assertEquals(3.8971f, trilist[2].rotateLCR().y, 0.001f)
        trilist.scale(PointXY(0f, 0f), 2f)
        Assert.assertEquals(6.928f, trilist[2].rotateLCR().y, 0.001f)
        Assert.assertEquals(8.046f, trilist[2].pointBC_.y, 0.001f)
    }

    @Test
    fun testTrilistSpritByColors() {
        val trilist = TriangleList()
        trilist.add(Triangle(3f, 4f, 5f))
        Assert.assertEquals(true, trilist.validTriangle( Triangle( trilist[1], 2, 3f, 4f ) ) )

        trilist.add(Triangle( trilist[1], 2, 3f, 4f ) )
        trilist.add(Triangle( trilist[2], 1, 4f, 5f ) )
        trilist.add(Triangle( trilist[3], 2, 3f, 4f ) )
        trilist.add(Triangle( trilist[4], 1, 4f, 5f ) )
        Assert.assertEquals(5, trilist.size().toLong() )
        trilist[2].setColor( 0 )
        val listByColors = trilist.spritByColors()
        Assert.assertEquals(1, listByColors[0].size().toLong())
        Assert.assertEquals(0, listByColors[1].size().toLong())
        Assert.assertEquals(0, listByColors[2].size().toLong())
        Assert.assertEquals(0, listByColors[3].size().toLong())
        Assert.assertEquals(4, listByColors[4].size().toLong())
    }


    @Test
    fun testSetTriAngleBy() {
        val trilist = TriangleList(Triangle(3f, 4f, 5f))
        trilist.add(Triangle(trilist[1], 2, 3f, 4f))
        trilist.add(Triangle(trilist[2], 1, 4f, 5f))
        Assert.assertEquals(126.86f, trilist[1].angleMmCA, 0.01f)
    }

    @Test
    fun testGetTapLength() {
        val trilist = TriangleList(Triangle(5f, 5f, 5f))
        Assert.assertEquals(0, trilist[1].getTapLength(PointXY(-2.5f, 0f)).toLong())
        Assert.assertEquals(1, trilist[1].getTapLength(PointXY(-3.75f, 2.5f)).toLong())
        Assert.assertEquals(2, trilist[1].getTapLength(PointXY(-1.25f, 2.5f)).toLong())
        Assert.assertEquals(-2.5f, trilist[1].getDimPointA_().x, 0.001f)
        trilist.add(Triangle(trilist[1], 2, 5f, 5f))
        trilist.add(Triangle(trilist[2], 1, 5f, 5f))
        Assert.assertEquals(6.495f, trilist[3].getDimPointB_().y, 0.001f)
        Assert.assertEquals(-1.25f, trilist[3].getDimPointB_().x, 0.001f)
    }

    @Test
    fun testTriListPrintScale() {
        val trilist = TriangleList(Triangle(80f, 40f, 50f))
        Assert.assertEquals(2.5, trilist.getPrintScale(1.0f).toDouble(), 0.1)
        val scalefactor = 12.5f
        trilist.scale(PointXY(0f, 0f), scalefactor)
        Assert.assertEquals(2.5, trilist.getPrintScale(12.5f).toDouble(), 0.1)
    }

    @Test
    fun testFloatConnection() { // connection type - 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        val mytlist = TriangleList(Triangle(3f, 4f, 5f))
        mytlist.add(Triangle(mytlist[1], 2, 3f, 4f))
        mytlist.add(Triangle(mytlist[2], 9, 3f, 4f, 5f)) // pbc:9 is B-Float, 10 is C-Float
        Assert.assertEquals(3f, mytlist.size().toFloat(), 0.001f)
        Assert.assertEquals(5.0f, mytlist[3].getPointCA_().y, 0.001f)
    }

    @Test
    fun testRotateBySize() {
        val mytlist = TriangleList(Triangle(3f, 4f, 5f))
        mytlist.add(Triangle(mytlist[1], 2, 5f, 8f))
        mytlist.add(Triangle(mytlist[2], 2, 4f, 3f))
        //mytlist.add(new Triangle(mytlist.get(3),2,4,5));
        mytlist.rotateByLength("riseup")
        mytlist.rotateByLength("laydown")
        Assert.assertEquals(4.5980763f, mytlist.measureMostLongLine().x, 0.001f)
    }
    @Test
    fun testTrilistBounds() {
        val mytlist = TriangleList(Triangle(3f, 4f, 5f))
        mytlist.add(Triangle(mytlist[1], 2, 6f, 5f))
        mytlist.add(Triangle(mytlist[2], 2, 5f, 6f))
        val (left, top, right, bottom) = mytlist.calcBounds()
        Assert.assertEquals(6f, right, 0.001f)
        Assert.assertEquals(0f, bottom, 0.001f)
        Assert.assertEquals(-3f, left, 0.001f)
        Assert.assertEquals(4f, top, 0.001f)
        Assert.assertEquals(2f, mytlist.center.y, 0.001f)
        Assert.assertEquals(9f, mytlist.measureMostLongLine().x, 0.001f)
        Assert.assertEquals(4f, mytlist.measureMostLongLine().y, 0.001f)
        var myList2 = TriangleList(Triangle(3f, 4f, 5f))
        myList2.add(Triangle(mytlist[1], 2, 6f, 5f))
        myList2.add(Triangle(mytlist[2], 2, 5f, 6f))
        myList2.scale(PointXY(0f, 0f), 5f)
        myList2 = mytlist.clone()
        myList2.scale(PointXY(0f, 0f), 5f)
        Assert.assertEquals(4f, mytlist.measureMostLongLine().y, 0.001f)
    }

    @Test
    fun testCollision() {
        val tri = Triangle(3f, 4f, 5f)
        Assert.assertEquals(true, tri.collision(-2f, 2f))
        Assert.assertEquals(false, PointXY(-2f, -2f).isCollide(tri))
        Assert.assertEquals(true, PointXY(-2f, 2f).isCollide(tri))
        Assert.assertEquals(true, tri.isCollide(PointXY(-2f, 2f)))
        val trilist = TriangleList(tri)
        val tri2 = Triangle(tri, 2, 3f, 4f)
        trilist.add(tri2)
        Assert.assertEquals(4f, tri2.pointBC_.y, 0.001f)
        Assert.assertEquals(2, trilist.isCollide(PointXY(-1f, 3f)).toLong())
        Assert.assertEquals(1, trilist.isCollide(PointXY(-2f, 2f)).toLong())
    }

    @Test
    fun testClone() {
        val p1 = PointXY(0f, 0f)
        val p2 = p1.clone()
        val mytri1 = Triangle(3.0f, 4.0f, 5.0f, PointXY(0f, 0f), 180.0f)
        val t2 = mytri1.clone()
        mytri1.setNumber(10)
        Assert.assertEquals(10, mytri1.getMyNumber_().toLong())
        Assert.assertEquals(1, t2.getMyNumber_().toLong())
        val myTrilist = TriangleList(mytri1)
        myTrilist.add(Triangle(mytri1, 2, 5.0f, 5.0f))
        val myTrilist2 = myTrilist.clone()

//        mytri1.setNumber(55);
        myTrilist.getTriangle(2).setNumber(100)
        Assert.assertEquals(100, myTrilist.getTriangle(2).getMyNumber_().toLong())
        Assert.assertEquals(2, myTrilist2.getTriangle(2).getMyNumber_().toLong())
    }

    @Test
    fun testRotate() {
        val mytri1 = Triangle(3.0f, 4.0f, 5.0f, PointXY(5f, 5f), 180.0f)
        val myTrilist = TriangleList(mytri1)
        myTrilist.add(Triangle(mytri1, 2, 3.0f, 4.0f))
        mytri1.rotate(PointXY(0f, 0f), -90f)
        mytri1.rotate(PointXY(0f, 0f), -90f)
        mytri1.rotate(PointXY(0f, 0f), -90f)
        mytri1.rotate(PointXY(0f, 0f), -90f)
        Assert.assertEquals(0f, mytri1.getPointCA_().x, 0.00001f)
        Assert.assertEquals(0f, mytri1.getPointCA_().y, 0.00001f)
    }

    @Test
    fun testScale() {
        val mytri1 = Triangle(3.0f, 4.0f, 5.0f, PointXY(0f, 0f), 180.0f)
        val myTrilist = TriangleList(mytri1)
        myTrilist.add(Triangle(mytri1, 2, 3.0f, 4.0f))
        myTrilist.setScale(PointXY(0f, 0f), 5f)
        Assert.assertEquals(4f, myTrilist.getTriangle(1).getPointBC_().y, 0.001f)
    }

    @Test
    fun testGetTriangle() {
        val mytri1 = Triangle(3.0f, 4.0f, 5.0f, PointXY(0f, 0f), 180.0f)
        val myTrilist = TriangleList()
        myTrilist.add(mytri1)
        Assert.assertEquals(3.0f, myTrilist.getTriangle(1).getLengthA_(), 0.001f)
        Assert.assertEquals(1, myTrilist.size().toLong())
    }


    @Test
    fun testReplace() {
        val mytri1 = Triangle(3.0f, 4.0f, 5.0f, PointXY(0f, 0f), 180.0f)
        val myTrilist = TriangleList(mytri1)
        myTrilist.add(Triangle(mytri1, 2, 3f, 4f))
        myTrilist.add(Triangle(mytri1, 1, 3f, 5f))
        myTrilist.resetConnectedTriangles(1, mytri1.resetLength(3f, 2f, 4.5f))
        //myTrilist.getTriangle(3).reset(mytri1, 1);
        Assert.assertEquals(2.0f, myTrilist.getTriangle(1).getLengthB_(), 0.001f)
        Assert.assertEquals(3.0f, myTrilist.getTriangle(1).getLengthA_(), 0.001f)
        Assert.assertEquals(4.0f, myTrilist.getTriangle(3).getLengthA_(), 0.001f)
        Assert.assertEquals(1, myTrilist.getTriangle(1).myNumber_.toLong())
        Assert.assertEquals(3, myTrilist.size().toLong())


//        myTrilist.ResetTriangle(1, new Triangle(6.0f, 4.0f, 5.0f, new PointXY(0,0), 180.0f));
        //      assertEquals(6.0f, myTrilist.getTriangle(1).getLengthA(), 0.001f);
    }

    @Test
    fun testTriangleArea() {
        val mytrilist = TriangleList(Triangle(3.0f, 4.0f, 5.0f, PointXY(0f, 0f), 180.0f))
        mytrilist.add(Triangle(mytrilist.getTriangle(1), 2, 3f, 4f))
        Assert.assertEquals(12f, mytrilist.getArea(), 0.01f)
    }

    @Test
    fun testDimAlign() {
        val mytri1 = Triangle(2.0f, 2.3f, 1.2f, PointXY(0f, 0f), 180.0f)
        val myTrilist = TriangleList(mytri1)
        myTrilist.add(Triangle(mytri1, 3, 2.5f, 1.1f, 2.0f)) //2

        // 1下 3上 -> // 夾角の、1:外 　3:内
        Assert.assertEquals(3, myTrilist.getTriangle(1).dimAlignA.toLong())
        Assert.assertEquals(3, myTrilist.getTriangle(1).dimAlignB.toLong())
        Assert.assertEquals(3, myTrilist.getTriangle(1).dimAlignC.toLong())
        Assert.assertEquals(3, myTrilist.getTriangle(2).dimAlignA.toLong())
        Assert.assertEquals(3, myTrilist.getTriangle(2).dimAlignB.toLong())
        Assert.assertEquals(3, myTrilist.getTriangle(2).dimAlignC.toLong())
        val t1 = Triangle(1.0f, 1.5f, 1.0f)
        Assert.assertEquals(3, t1.dimAlignA.toLong())
        Assert.assertEquals(3, t1.dimAlignB.toLong())
        Assert.assertEquals(3, t1.dimAlignC.toLong())
    }

    @Test
    fun testTriangleList() {
        val mytri1 = Triangle(3.0f, 4.0f, 5.0f, PointXY(0f, 0f), 180.0f)
        val myTrilist = TriangleList(mytri1)
        myTrilist.add(Triangle(mytri1, 2, 3.0f, 4.0f))
        myTrilist.add(Triangle(myTrilist.getTriangle(myTrilist.size()), 1, 4.0f, 5.0f))
        myTrilist.add(Triangle(myTrilist.getTriangle(myTrilist.size()), 2, 3.0f, 4.0f))
        Assert.assertEquals(5.0, myTrilist.getTriangle(4).getLengthA_().toDouble(), 0.001)
        myTrilist.resetConnectedTriangles(1, Triangle(myTrilist.getTriangle(1), 2, 5.0f, 5.0f))
        Assert.assertEquals(2.0, myTrilist.getTriangle(2).getMyNumber_().toDouble(), 0.001)
        Assert.assertEquals(3.0, myTrilist.getTriangle(3).getLengthA_().toDouble(), 0.001)
    }
}