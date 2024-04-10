@file:Suppress("DEPRECATION")

package com.jpaver.trianglelist

//import com.jpaver.trianglelist.model.*
import com.jpaver.trianglelist.util.InputParameter
import junit.framework.Assert.assertEquals
import org.junit.Assert
import org.junit.Test

fun printTriangle( t: Triangle){
    System.out.printf( t.toString() )
}

fun printTriList( tl: TriangleList){
    System.out.printf( "TriangleList size ${tl.size()}%n" )
    for( i in 0 until tl.size() ){
        printTriangle( tl[i+1] )
    }
    System.out.printf( "%n" )
}

fun printTriListList(listlist: java.util.ArrayList<TriangleList>?) {
    if (listlist != null) {
        for( i in 0 until listlist.size ){

            if( listlist.get(i).size() > 0 ) {

                val tl = listlist.get(i)//traceOrJumpForward(0, 0, ArrayList<PointXY>() )
                System.out.printf( "trilistlist[%s], size %s, outlineList_ %s, outlineStr_ %s%n", i, tl.size(), tl.outlineList_!!.size, tl.outlineStr_ )
                printTriList( tl )
            }
        }

    }
}

class TriListTest {


    @Test
    fun testDedMapping(){
        val trilist = TriangleList()
        trilist.add(Triangle(5f, 5f, 5f), true)
        trilist.add(1, 1, 5f, 5f)

        printTriangle( trilist[1] ) // getに準拠していてnumber指定。
        printTriangle( trilist[2] )

        val dedlist = DeductionList()
        dedlist.add( Deduction(1,"test",0.5f,0f,0,"Circle",0f,
            PointXY(-2.5f, -2.5f),
            PointXY(0f, 0f)
        ) )
        dedlist.add( Deduction(2,"test2",0.5f,0f,0,"Circle",0f,
            PointXY(-5.0f, -2.5f),
            PointXY(0f, 0f)
        ) )

        trilist.dedmapping(dedlist, -1)
        assertEquals( 1, dedlist[1].overlap_to )
        assertEquals( 2, dedlist[2].overlap_to )

    }


    @Test
    fun testRemoveNode() {
        val trilist = TriangleList()
        trilist.add(Triangle(5f, 8f, 5f), true)
        trilist.add(1, 1, 8f, 8f)
        trilist.add(2, 2, 5f, 5f)
        trilist.add(1, 2, 5f, 5f)

        //trilist.remove( 2 )
        //assertEquals( 5f, trilist.get(1).nodeTriangleB_.lengthB_ )
        //assertEquals( trilist.get(2), trilist.get(1).nodeTriangleB_ )

        //val trilist2 = trilist.clone()

        trilist.remove(3)
        assertEquals(null, trilist.get(2).nodeTriangleC)
        printTriList(trilist)

        trilist.remove(4)

    }

    @Test
    fun testResetNodeByObjectID() {
        val trilist = TriangleList()
        trilist.add(Triangle(5f, 8f, 5f), true)
        trilist.add(1, 1, 8f, 8f)
        trilist.add(2, 2, 5f, 5f)

        trilist.resetNodeByID(InputParameter("", "", 3, 6f, 6f, 6f, 1, 2))
        assertEquals(trilist.get(1), trilist.get(3).nodeTriangleA_)
        assertEquals(trilist.get(3), trilist.get(1).nodeTriangleC)

    }

    @Test
    fun testResetPointAndAngleByNodeChain() {
        val trilist = TriangleList()
        trilist.add(Triangle(5f, 8f, 5f), true)
        trilist.add(1, 1, 8f, 8f)
        trilist.add(2, 2, 5f, 5f)

        //trilist.resetPointAndAngleByNodeChain( Params("", "", 2, 6f, 6f, 6f, 1, 1) )
        trilist.get(2).resetElegant(InputParameter("", "", 2, 6f, 6f, 6f, 1, 1))

        assertEquals(6f, trilist.get(1).lengthB_)
        //assertEquals( trilist.get(2).pointCA_.x, trilist.get(1).pointBC_.x )

    }

    @Test
    fun testResetFromParamCase4() {
        val trilist = TriangleList()
        trilist.add(Triangle(5f, 8f, 5f), true)
        trilist.add(1, 1, 8f, 8f)
        trilist.add(2, 2, 5f, 5f)

        //　ケース４：接続三角形にとって辺長がおかしい場合は伝播を止める
        trilist.resetFromParam(InputParameter("", "", 2, 12f, 12f, 12f, 1, 1))
        assertEquals(8f, trilist.get(1).lengthB_)
        assertEquals(8f, trilist.get(3).lengthA_)

    }

    @Test

    fun testResetFromParamCase3() {
        val trilist = TriangleList()
        trilist.add(Triangle(5f, 5f, 5f), true)
        trilist.add(1, 1, 5f, 5f)
        trilist.add(2, 2, 5f, 5f)

        //tri1△
        assertEquals(4.330f, trilist.get(1).pointBC.y, 0.001f)
        assertEquals(0.000f, trilist.get(1).pointCA_.y, 0.001f)
        //tri2▽ △
        assertEquals(4.330f, trilist.get(2).pointCA_.y, 0.001f)
        assertEquals(0.000f, trilist.get(2).pointAB.y, 0.001f)

        //　ケース３：二重断面中、7:BC、trilist.get(1)：△、(2)：▽△
        trilist.resetFromParam(InputParameter("", "", 2, 6f, 6f, 6f, 1, 7))
        assertEquals(4.763f, trilist.get(2).pointCA_.y, 0.001f)
        assertEquals(-0.433f, trilist.get(2).pointAB.y, 0.001f)

        //　ケース３：二重断面右、3:BR、trilist.get(1)：△、(2)：▽△
        trilist.resetFromParam(InputParameter("", "", 2, 6f, 6f, 6f, 1, 3))
        assertEquals(4.330f, trilist.get(2).pointCA_.y, 0.001f)
        assertEquals(-0.866f, trilist.get(2).pointAB.y, 0.001f)

        //　ケース３：二重断面左、4:BL、trilist.get(1)：△、(2)：▽△
        trilist.resetFromParam(InputParameter("", "", 2, 6f, 6f, 6f, 1, 4))
        assertEquals(5.196f, trilist.get(2).pointCA_.y, 0.001f)
        assertEquals(0.000f, trilist.get(2).pointAB.y, 0.001f)

        printTriList(trilist)
    }

    @Test
    fun testResetFromParamCase2() {
        val trilist = TriangleList()
        trilist.add(Triangle(5f, 5f, 5f), true)
        trilist.add(1, 1, 5f, 5f)
        trilist.add(2, 2, 5f, 5f)

        //　ケース２：接続辺の書き換えが子に伝播する
        trilist.resetFromParam(InputParameter("", "", 2, 5f, 6f, 6f, 1, 1))
        assertEquals(6f, trilist.get(3).lengthA_)

        //　ケース３：接続辺の書き換えが親に伝播する
        trilist.resetFromParam(InputParameter("", "", 2, 6f, 6f, 6f, 1, 1))
        assertEquals(6f, trilist.get(1).lengthB_)

    }

    @Test
    fun testResetFromParamCase1() {
        val trilist = TriangleList()
        trilist.add(Triangle(5f, 5f, 5f), true)
        trilist.add(1, 1, 5f, 5f)
        trilist.add(2, 2, 5f, 5f)

        // ケース１：親番号の書き換え
        trilist.resetFromParam(InputParameter("", "", 3, 5f, 6f, 6f, 1, 2))
        assertEquals(1, trilist.get(3).parentnumber)
        assertEquals(3, trilist.get(3).mynumber)

    }

    @Test
    fun testReplaceByConnectNodeNumber() {
        val trilist = TriangleList()
        trilist.add(Triangle(5f, 5f, 5f), true)
        trilist.add(1, 1, 5f, 5f)
        trilist.add(2, 2, 5f, 5f)

        trilist.replace(3, 1)

        assertEquals(1, trilist.get(3).parentnumber)
    }

    @Test
    fun testResetAllNodesAtClone() {
        val trilist = TriangleList()
        trilist.add(Triangle(5f, 5f, 5f), true)
        trilist.add(1, 1, 5f, 5f)
        trilist.add(1, 2, 5f, 5f)

        assertEquals(trilist.get(1), trilist.get(3).nodeTriangleA_)
        assertEquals(trilist.get(1), trilist.get(2).nodeTriangleA_)
        assertEquals(trilist.get(2), trilist.get(1).nodeTriangleB)
        assertEquals(trilist.get(3), trilist.get(1).nodeTriangleC)

        val trilist2 = trilist.clone()

        //assertEquals( trilist2.get(1), trilist2.get(3).nodeTriangleA_ )


        assertEquals(trilist2.get(1), trilist2.get(3).nodeTriangleA_)
        assertEquals(trilist2.get(1), trilist2.get(2).nodeTriangleA_)
        assertEquals(trilist2.get(2), trilist2.get(1).nodeTriangleB)
        assertEquals(trilist2.get(3), trilist2.get(1).nodeTriangleC)


    }

    @Test
    fun testReset() {
        val trilist = TriangleList()
        trilist.add(Triangle(5f, 5f, 5f), true)
        trilist.add(1, 1, 5f, 5f)
        trilist.add(1, 2, 5f, 5f)

        assertEquals(1, trilist.get(3).parentnumber)

        trilist.get(3).resetLength(5f, 6f, 6f)

        assertEquals(1, trilist.get(3).parentnumber)


    }

    @Test
    fun testTriListUndo() {
        val trilist = TriangleList()
        trilist.add(Triangle(5f, 5f, 5f), true)
        trilist.add(1, 4, 6f, 5f, 5f)
        trilist.undo()

        assertEquals(2, trilist.size())

    }

    @Test
    fun testConnectionTypeLoad(){
        val tri1 = Triangle(3f, 4f, 5f)
        val tri2 = Triangle(tri1, 3, 3f, 4f, 5f)
        val tri3 = Triangle(tri2, ConnParam(1, 0, 2, 0f), 3f, 5f)
        val tri4 = Triangle(tri3, 4, 3f, 4f, 5f)
        val tri5 = Triangle(tri4, 6, 3f, 4f, 5f)
        val tri6 = Triangle(tri5, ConnParam(1, 1, 0, 4f), 3f, 5f)
        val tri7 = Triangle(tri6, ConnParam(2, 1, 0, 4f), 3f, 5f)
        val tri8 = Triangle(tri7, 5, 3f, 4f, 5f)
        val tri9 = Triangle(tri7, 7, 3f, 4f, 5f)
        val tri10 = Triangle(tri7, 8, 3f, 4f, 5f)
        val tri11 = Triangle(tri7, 9, 3f, 4f, 5f)
        val tri12 = Triangle(tri7, 10, 3f, 4f, 5f)

        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC

        assertEquals(2, tri2.cParam_.lcr)
        assertEquals(2, tri3.cParam_.lcr)
        assertEquals(0, tri4.cParam_.lcr)
        assertEquals(0, tri5.cParam_.lcr)
        assertEquals(0, tri6.cParam_.lcr)
        assertEquals(0, tri7.cParam_.lcr)
        assertEquals(2, tri8.cParam_.lcr)
        assertEquals(1, tri9.cParam_.lcr)
        assertEquals(1, tri10.cParam_.lcr)
        assertEquals(2, tri11.cParam_.lcr)
        assertEquals(2, tri12.cParam_.lcr)
    }

    @Test
    fun testResetConnectedTriangles() {
        val trilist = TriangleList()
        trilist.add(Triangle(5f, 5f, 5f), true)

        val params = InputParameter("", "", 1, 7f, 7f, 7f, -1, 1)
        trilist.resetFromParam(params)

    }

    @Test
    fun testExportPDF() {
        val trilist = TriangleList()
        trilist.add(Triangle(5f, 5f, 5f), true)
        trilist.add(Triangle(trilist.get(1), 2, 7f, 7f), true)
        trilist.add(Triangle(trilist.get(1), 1, 6f, 6f), true)

        //val pdfwriter = PdfWriter( 1f, trilist )
    }

    @Test
    fun testSeparateChild() {
        val trilist = TriangleList()
        trilist.add(Triangle(5f, 5f, 5f), true)
        trilist.add(Triangle(trilist.get(1), 2, 7f, 7f), true)
        trilist.add(Triangle(trilist.get(1), 1, 6f, 6f), true)

        Assert.assertEquals(true, trilist.get(1).hasChildIn(2))
        Assert.assertEquals(true, trilist.get(1).hasChildIn(1))

        trilist.add(Triangle(trilist.get(2), 1, 6f, 6f), true)
        Assert.assertEquals(true, trilist.get(2).hasChildIn(1))
        Assert.assertEquals(false, trilist.get(2).hasChildIn(2))

        trilist.add(Triangle(trilist.get(2), 2, 6f, 6f), true)
        Assert.assertEquals(true, trilist.get(2).hasChildIn(1))


    }

    @Test
    fun testParentHaveAChild() {
        val trilist = TriangleList()
        trilist.add(Triangle(5f, 5f, 5f), true)
        trilist.add(Triangle(trilist.get(1), 2, 7f, 7f), true)
        trilist.add(Triangle(trilist.get(2), 2, 6f, 6f), true)

        Assert.assertEquals(true, trilist.get(1).hasChildIn(2))
    }

    @Test
    fun testGetNumberList() {
        val trilist = TriangleList()
        trilist.add(Triangle(5f, 5f, 5f), true)
        trilist.add(Triangle(trilist.trilist[0], 2, 5f, 5f), true)
        trilist.add(Triangle(trilist.trilist[0], 1, 6f, 6f), true)
        trilist.trilist[0].name = "No.10"
        trilist.trilist[1].name = "No.6"
        trilist.trilist[2].name = "No.2"

        val numlist = trilist.getSokutenList(2, 4)

//        Assert.assertEquals("No.2", numlist[0].myName_ )
        Assert.assertEquals("No.6", numlist[2].name)

        //Assert.assertEquals(-2.5f, numlist[0].pointCA_.vectorTo( numlist[1].pointCA_ ).x )
        //Assert.assertEquals(-2.5f, numlist.vectorToNextFrom( 0 ).x )

       Assert.assertEquals(-4, trilist.sokutenListVector)

    }

    @Test
    fun testTriListSlide() {
        val trilist = TriangleList()
        trilist.add(Triangle(5f, 5f, 5f), true)
        trilist.add(Triangle(trilist.get(1), 2, 5f, 5f), true)
        trilist.add(Triangle(trilist.get(1), 2, 6f, 6f), true)
        trilist.setChildsToAllParents() // これやらないと認知されない。

        Assert.assertEquals(true, trilist.get(1).alreadyHaveChild(2))
        Assert.assertEquals(false, trilist.get(1).alreadyHaveChild(1))
        Assert.assertEquals(2.5, trilist.get(2).pointBC.x.toDouble(), 0.001)
        Assert.assertEquals(3.473, trilist.get(3).pointBC.x.toDouble(), 0.001)
    }

    @Test
    fun testTriListSetChild() {
        val trilist = TriangleList()
        trilist.add(Triangle(5f, 5f, 5f), true)
        trilist.add(Triangle(trilist.get(1), 2, 5f, 5f), true)
        trilist.add(Triangle(trilist.get(2), 2, 6f, 6f), true)
        trilist.add(Triangle(trilist.get(3), 1, 6f, 6f), true)
        trilist.add(Triangle(trilist.get(1), 1, 6f, 6f), true)
        trilist.add(Triangle(trilist.get(4), 1, 6f, 6f), true)
        trilist.add(Triangle(trilist.get(2), 1, 6f, 6f), true)
        trilist.setChildsToAllParents() // これやらないと認知されない。

        Assert.assertEquals(true, trilist.get(1).alreadyHaveChild(2))
        Assert.assertEquals(true, trilist.get(1).alreadyHaveChild(1))
    }

    @Test
    fun testTriListGetTap() {
        val tri1 = Triangle(3f, 4f, 5f)
        val tri2 = Triangle(tri1, ConnParam(1, 0, 2, 0f), 3f, 4f)
        val tri3 = Triangle(tri1, ConnParam(2, 0, 2, 0f), 3f, 5f)
        val trilist = TriangleList(tri1)
        trilist.add(tri2, true)
        trilist.add(tri3, true)
        Assert.assertEquals(3, trilist.getTapIndexArray(
            PointXY(
                -3f,
                2f
            )
        ).size.toLong())
        Assert.assertEquals(2, trilist.getTapHitCount(
            PointXY(
                -3f,
                2f
            )
        ).toLong())
    }

    @Test
    fun testConnectionType3() {
        val tri1 = Triangle(3f, 4f, 5f)
        val tri2 = Triangle(tri1, 3, 3f, 4f, 5f)
        val tri3 = Triangle(tri2, ConnParam(1, 0, 2, 0f), 3f, 5f)
        val trilist = TriangleList(tri1)
        trilist.add(tri2, true)
        trilist.add(tri3, true)
        tri2.reset(tri2, ConnParam(1, 1, 1, 3f))
        trilist.lastTapNumber_ = 2
        trilist.rotateCurrentTriLCR()
        Assert.assertEquals(1.0f, tri3.pointCA_.y, 0.0001f)
        //trilist.resetTriConnection(2, ConnParam(1, 1, 0, 3f))
        Assert.assertEquals(1.0f, tri3.pointCA_.y, 0.0001f)
        val tri4 = Triangle(tri1, ConnParam(1, 1, 2, 3f), 3f, 5f)
        Assert.assertEquals(true, tri4.pointCA_.equals(-3f, 4f))
        val tri5 = Triangle(tri1, ConnParam(2, 1, 2, 3f), 3f, 5f)
        Assert.assertEquals(true, tri5.pointCA_.equals(0f, 0f))
        val tri8 =
            Triangle(tri1, InputParameter("", "", 2, 3f, 3f, 5f, 2, 5, tri1.pointCA_,
                PointXY(0f, 0f)
            )
            )
        Assert.assertEquals(true, tri8.pointCA_.equals(0f, 0f))
        val tri6 = Triangle(tri1, ConnParam(2, 1, 0, 3f), 3f, 5f)
        Assert.assertEquals(true, tri6.pointCA_.equals(-1.1999f, 1.5999f))
        val tri7 =
            Triangle(tri1, InputParameter("", "", 2, 3f, 3f, 5f, 2, 6, tri1.pointCA_,
                PointXY(0f, 0f)
            )
            )
        Assert.assertEquals(true, tri7.pointCA_.equals(-1.1999f, 1.5999f))
    }

    @Test
    fun testConnectionType2() {
        val tri1 = Triangle(3f, 4f, 5f)
        val tri2 = Triangle(tri1, 9, 3f, 4f, 5f)// 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC

        // test float connection
        Assert.assertEquals(-3f, tri1.pointAB.x, 0.0001f)

        Assert.assertEquals(-4f, tri2.pointCA_.x, 0.0001f)


        Assert.assertEquals(3.5f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(3.0f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(4.0f, tri2.rotateLCR().y, 0.0001f)

        val tri3 = Triangle(tri2, ConnParam(1, 0, 2, 3f), 3f, 5f)
        Assert.assertEquals(1f, tri3.pointCA_.y, 0.0001f)

        tri3.resetByParent(tri2.rotateLCRandGet(), tri3.cParam_)
        Assert.assertEquals(3.0f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(1f, tri3.pointCA_.y, 0.0001f)
        tri3.resetByParent(tri2.rotateLCRandGet(), tri3.cParam_)
        Assert.assertEquals(3.5f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(1f, tri3.pointCA_.y, 0.0001f)
        tri3.resetByParent(tri2.rotateLCRandGet(), tri3.cParam_)
        Assert.assertEquals(4.0f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(1f, tri3.pointCA_.y, 0.0001f)
        val trilist = TriangleList(tri1)
        trilist.add(tri2, true)
        trilist.add(tri3, true)
        trilist.lastTapNumber_ = 2
        trilist.rotateCurrentTriLCR() //3
        trilist.rotateCurrentTriLCR() //3.5
        Assert.assertEquals(4.0f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(3.5f, tri2.rotateLCR().y, 0.0001f)
        trilist.rotateCurrentTriLCR() //3.0
        Assert.assertEquals(1f, tri3.pointCA_.y, 0.0001f)
    }

    @Test
    fun testConnectionType() {
        val tri1 = Triangle(5f, 5f, 5f)
        val tri2 = Triangle(tri1, 3, 4f, 5f, 5f)

        //tri2.setConnectionType( 1, 1, 1, 4f);
        Assert.assertEquals(-5f, tri2.nodeTriangleA_!!.pointAB.x, 0.0001f)
        Assert.assertEquals(-2.75f, tri2.getParentPointByLCR(1, 1).x, 0.0001f)
        Assert.assertEquals(-2.75f, tri2.getParentPointByType(1, 0, 1).x, 0.0001f)
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
        trilist.add(tri2, true)
        Assert.assertEquals(3.8971f, trilist[2].rotateLCR().y, 0.001f)
        trilist.scale(PointXY(0f, 0f), 2f)
        Assert.assertEquals(6.928f, trilist[2].rotateLCR().y, 0.001f)
        Assert.assertEquals(8.046f, trilist[2].pointBC.y, 0.001f)
    }

    @Test
    fun testSetTriAngleBy() {
        val trilist = TriangleList(Triangle(3f, 4f, 5f))
        trilist.add(Triangle(trilist[1], 2, 3f, 4f), true)
        trilist.add(Triangle(trilist[2], 1, 4f, 5f), true)
        Assert.assertEquals(126.86f, trilist[1].angleMmCA, 0.01f)
    }

    @Test
    fun testGetTapLength() {
        //PowerMockito.mockStatic(Log::class.java)

        val trilist = TriangleList(Triangle(5f, 5f, 5f))

        //Assert.assertEquals( 10, trilist.getTap( PointXY( -2.5f, 0f ) ) )
        //Assert.assertEquals( 11, trilist.getTap( PointXY( -3.75f, 2.165f ) ) )
        //Assert.assertEquals( 12, trilist.getTap( PointXY( -1.25f, 2.165f ) ) )

        trilist.add(Triangle(trilist[1], 1, 5f, 5f), true)
        trilist.add(Triangle(trilist[2], 2, 5f, 5f), true)
        //Assert.assertEquals( 20, trilist.getTap( PointXY( -3.75f, 2.165f ) ) )


        //Assert.assertEquals( 21, trilist.getTap( PointXY( -6.25f, 2.165f ) ) )
        Assert.assertEquals(30, trilist.getTap(
            PointXY(
                -5f,
                4.33f
            ), 0.6f))

        //Assert.assertEquals( 31, trilist.getTap( PointXY( -6.25f, 6.495f ) ) )
        //Assert.assertEquals( 32, trilist.getTap( PointXY( -3.75f, 6.495f ) ) )

        /*
        Assert.assertEquals(0, trilist[1].getTapLength(PointXY(-2.5f, 0f)).toLong())
        Assert.assertEquals(1, trilist[1].getTapLength(PointXY(-3.75f, 2.5f)).toLong())
        Assert.assertEquals(2, trilist[1].getTapLength(PointXY(-1.25f, 2.5f)).toLong())
        Assert.assertEquals(-2.5f, trilist[1].getDimPointA_().x, 0.001f)
        Assert.assertEquals(6.495f, trilist[3].getDimPointB_().y, 0.001f)
        Assert.assertEquals(-1.25f, trilist[3].getDimPointB_().x, 0.001f)
        */
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
        mytlist.add(Triangle(mytlist[1], 2, 3f, 4f), true)
        mytlist.add(Triangle(mytlist[2], 9, 3f, 4f, 5f), true) // pbc:9 is B-Float, 10 is C-Float
        Assert.assertEquals(3f, mytlist.size().toFloat(), 0.001f)
        Assert.assertEquals(5.0f, mytlist[3].pointCA_.y, 0.001f)
    }

    @Test
    fun testRotateBySize() {
        //PowerMockito.mockStatic(Log::class.java)

        val mytlist = TriangleList(Triangle(3f, 4f, 5f))
        mytlist.add(Triangle(mytlist[1], 2, 5f, 8f), true)
        mytlist.add(Triangle(mytlist[2], 2, 4f, 3f), true)
        //mytlist.add(new Triangle(mytlist.get(3),2,4,5));
        mytlist.rotateByLength("riseup")
        mytlist.rotateByLength("laydown")
        Assert.assertEquals(4.5980763f, mytlist.measureMostLongLine().x, 0.001f)
    }
    @Test
    fun testTrilistBounds() {
        val mytlist = TriangleList(Triangle(3f, 4f, 5f))
        mytlist.add(Triangle(mytlist[1], 2, 6f, 5f), true)
        mytlist.add(Triangle(mytlist[2], 2, 5f, 6f), true)
        val (left, top, right, bottom) = mytlist.calcBounds()
        Assert.assertEquals(6f, right, 0.001f)
        Assert.assertEquals(0f, bottom, 0.001f)
        Assert.assertEquals(-3f, left, 0.001f)
        Assert.assertEquals(4f, top, 0.001f)
        Assert.assertEquals(2f, mytlist.center.y, 0.001f)
        Assert.assertEquals(9f, mytlist.measureMostLongLine().x, 0.001f)
        Assert.assertEquals(4f, mytlist.measureMostLongLine().y, 0.001f)
        var myList2 = TriangleList(Triangle(3f, 4f, 5f))
        myList2.add(Triangle(mytlist[1], 2, 6f, 5f), true)
        myList2.add(Triangle(mytlist[2], 2, 5f, 6f), true)
        myList2.scale(PointXY(0f, 0f), 5f)
        myList2 = mytlist.clone()
        myList2.scale(PointXY(0f, 0f), 5f)
        Assert.assertEquals(4f, mytlist.measureMostLongLine().y, 0.001f)
    }

    @Test
    fun testCollision() {
        val tri = Triangle(3f, 4f, 5f)
        Assert.assertEquals(true, tri.collision())
        Assert.assertEquals(false, PointXY(-2f, -2f)
            .isCollide(tri))
        Assert.assertEquals(true, PointXY(-2f, 2f).isCollide(tri))
        Assert.assertEquals(true, tri.isCollide(
            PointXY(
                -2f,
                2f
            )
        ))
        val trilist = TriangleList(tri)
        val tri2 = Triangle(tri, 2, 3f, 4f)
        trilist.add(tri2, true)
        Assert.assertEquals(4f, tri2.pointBC.y, 0.001f)
        Assert.assertEquals(2, trilist.isCollide(
            PointXY(
                -1f,
                3f
            )
        ).toLong())
        Assert.assertEquals(1, trilist.isCollide(
            PointXY(
                -2f,
                2f
            )
        ).toLong())
    }

    @Test
    fun testClone() {
        //val p1 = PointXY(0f, 0f)
        //val p2 = p1.clone()
        val mytri1 = Triangle(3.0f, 4.0f, 5.0f,
            PointXY(0f, 0f), 180.0f)
        val t2 = mytri1.clone()
        mytri1.setNumber(10)
        Assert.assertEquals(10, mytri1.mynumber.toLong())
        Assert.assertEquals(1, t2.mynumber.toLong())
        val myTrilist = TriangleList(mytri1)
        myTrilist.add(Triangle(mytri1, 2, 5.0f, 5.0f), true)
        val myTrilist2 = myTrilist.clone()

//        mytri1.setNumber(55);
        myTrilist.getBy(2).setNumber(100)
        Assert.assertEquals(100, myTrilist.getBy(2).mynumber.toLong())
        Assert.assertEquals(2, myTrilist2.getBy(2).mynumber.toLong())
    }

    @Test
    fun testRotateFloat() {
        //PowerMockito.mockStatic(Log::class.java)
        //setDimAlign();

        val t1 = Triangle(3.0f, 4.0f, 5.0f,
            PointXY(0f, 0f), 180.0f )
        val t2 = Triangle( t1, 9, 4f, 3.0f, 5.0f )
        val trilist = TriangleList( t1 )
        trilist.add(t2, true)
        System.out.println( "Initialized.")

        printTriangle( t2 )

        trilist.rotate(PointXY(0f, 0f), -90f, 2, false)
        System.out.println( "Trilist rotate t2.")

        printTriangle( t2 )

        Assert.assertEquals(3f, t1.pointBC.y, 0.001f)
        Assert.assertEquals(7f, t2.pointBC.y, 0.001f)

        trilist.resetTriangles(1, Triangle(3.0f, 4.0f, 5.0f,
            PointXY(0f, 0f), 180.0f) )
        System.out.println( "Trilist reset t1.")

        printTriangle( t2 )

        Assert.assertEquals(4f, trilist.get(1).pointBC.y, 0.001f)
        Assert.assertEquals(7f, t2.pointBC.y, 0.001f)


    }

    @Test
    fun testRotate() {
        //PowerMockito.mockStatic(Log::class.java)

        val mytri1 = Triangle(3.0f, 4.0f, 5.0f,
            PointXY(5f, 5f), 180.0f)
        val myTrilist = TriangleList(mytri1)
        myTrilist.add(Triangle(mytri1, 2, 3.0f, 4.0f), true)
        mytri1.rotate(PointXY(0f, 0f), -90f, false)
        mytri1.rotate(PointXY(0f, 0f), -90f, false)
        mytri1.rotate(PointXY(0f, 0f), -90f, false)
        mytri1.rotate(PointXY(0f, 0f), -90f, false)
        Assert.assertEquals(5f, mytri1.pointCA_.x, 0.00001f)
        Assert.assertEquals(5f, mytri1.pointCA_.y, 0.00001f)
    }

    @Test
    fun testScale() {
        val mytri1 = Triangle(3.0f, 4.0f, 5.0f,
            PointXY(0f, 0f), 180.0f)
        val myTrilist = TriangleList(mytri1)
        myTrilist.add(Triangle(mytri1, 2, 3.0f, 4.0f), true)
        myTrilist.setScale(PointXY(0f, 0f), 5f)
        Assert.assertEquals(4f, myTrilist.getBy(1).pointBC_().y, 0.001f)
    }

    @Test
    fun testGetTriangle() {
        val mytri1 = Triangle(3.0f, 4.0f, 5.0f,
            PointXY(0f, 0f), 180.0f)
        val myTrilist = TriangleList()
        myTrilist.add(mytri1, true)
        Assert.assertEquals(3.0f, myTrilist.getBy(1).lengthA, 0.001f)
        Assert.assertEquals(1, myTrilist.size().toLong())
    }

    @Test
    fun testReplaceA() {
        val trilist = TriangleList()
        trilist.add(
            Triangle(5.0f, 5.0f, 5.0f,
            PointXY(0f, 0f), 180.0f), true)
        trilist.add(Triangle(trilist.get(1), 2, 5f, 5f), true)

        // 新しい三角形を作って渡すと連動しないので、ポインタを取得してリセットする。
        trilist.resetTriangles(2, trilist.get(2).setOn(trilist.get(1), 2, 5f, 5f))
        assertEquals(trilist.get(2).pointAB.x, trilist.get(1).pointBC.x, 0.001f)
        assertEquals(trilist.get(2).pointAB.y, trilist.get(1).pointBC.y, 0.001f)

        trilist.resetFromParam(InputParameter("", "", 2, 2.5f, 5f, 5f, 1, 2))
        assertEquals(trilist.get(2).pointAB.x, trilist.get(1).pointBC.x, 0.001f)
        assertEquals(trilist.get(2).pointAB.y, trilist.get(1).pointBC.y, 0.001f)

    }
    @Test
    fun testReplace() {
        val mytri1 = Triangle(3.0f, 4.0f, 5.0f,
            PointXY(0f, 0f), 180.0f)
        val myTrilist = TriangleList(mytri1)
        myTrilist.add(Triangle(mytri1, 2, 3f, 4f), true)
        myTrilist.add(Triangle(mytri1, 1, 3f, 5f), true)
        myTrilist.resetTriangles(1, mytri1.resetLength(3f, 2f, 4.5f))
        //myTrilist.getTriangle(3).reset(mytri1, 1);
        Assert.assertEquals(2.0f, myTrilist.getBy(1).lengthB, 0.001f)
        Assert.assertEquals(3.0f, myTrilist.getBy(1).lengthA, 0.001f)
        Assert.assertEquals(4.0f, myTrilist.getBy(3).lengthA, 0.001f)
        Assert.assertEquals(1, myTrilist.getBy(1).mynumber.toLong())
        Assert.assertEquals(3, myTrilist.size().toLong())


//        myTrilist.ResetTriangle(1, new Triangle(6.0f, 4.0f, 5.0f, new PointXY(0,0), 180.0f));
        //      assertEquals(6.0f, myTrilist.getTriangle(1).getLengthA(), 0.001f);
    }

    @Test
    fun testTriangleArea() {
        val mytrilist = TriangleList(Triangle(3.0f, 4.0f, 5.0f,
            PointXY(0f, 0f), 180.0f))
        mytrilist.add(Triangle(mytrilist.getBy(1), 2, 3f, 4f), true)
        Assert.assertEquals(12f, mytrilist.getArea(), 0.01f)
    }

    @Test
    fun testTriangleList() {
        val mytri1 = Triangle(3.0f, 4.0f, 5.0f,
            PointXY(0f, 0f), 180.0f)
        val myTrilist = TriangleList(mytri1)
        myTrilist.add(Triangle(mytri1, 2, 3.0f, 4.0f), true)
        myTrilist.add(Triangle(myTrilist.getBy(myTrilist.size()), 1, 4.0f, 5.0f), true)
        myTrilist.add(Triangle(myTrilist.getBy(myTrilist.size()), 2, 3.0f, 4.0f), true)
        Assert.assertEquals(5.0, myTrilist.getBy(4).lengthA.toDouble(), 0.001)

        myTrilist.resetTriangles(1, Triangle(myTrilist.getBy(1), 2, 5.0f, 5.0f))
        Assert.assertEquals(2.0, myTrilist.getBy(2).mynumber.toDouble(), 0.001)
        Assert.assertEquals(3.0, myTrilist.getBy(3).lengthA.toDouble(), 0.001)
    }
}