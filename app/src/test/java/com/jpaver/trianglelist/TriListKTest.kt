package com.jpaver.trianglelist

import org.junit.Assert

import org.junit.Test

class TriListKTest {

    @Test
    fun testRotateArray() {
        val trilist = TriangleListK()
        trilist.add(TriangleK(5f, 8f, 5f))

        trilist[2].rotateArray( trilist[2].length )

    }


    @Test
    fun testResetNodes() {
        val trilist = TriangleListK()
        trilist.add(TriangleK(5f, 8f, 5f))
        trilist.add(1, 1, 8f, 8f)
        trilist.add(2, 2, 5f, 5f)
        trilist.add(3, 2, 5f, 5f)

        trilist[2].reset( TriangleK( trilist[1], 2, 6f, 6f ), arrayOf( trilist[2] ) )

    }

    fun printTriLengthes( tri : TriangleK ){
        print( tri.length[0].toString() + tri.length[1].toString() + tri.length[2].toString() )
    }

    @Test
    fun testTriArray(){
        val trilist = TriangleListK()
        trilist.triarray.plus( TriangleK(5f, 8f, 5f) )
        trilist.triarray.plus( TriangleK( trilist[0],5f, 8f, 5f ) )

        trilist.triarray.forEach { printTriLengthes(it!!) }
    }

    @Test
    fun testClone() {
        //val p1 = PointXY(0f, 0f)
        //val p2 = p1.clone()
        val mytri1 = TriangleK(3.0f, 4.0f, 5.0f, PointXY(0f, 0f), 180.0f)
        val t2 = mytri1.clone()

        mytri1.setNumber(10)
        Assert.assertEquals(10, mytri1.myNumber_ )
        Assert.assertEquals(1, t2.myNumber_ )

        val myTrilist = TriangleListK(mytri1)
        myTrilist.add(TriangleK(mytri1, 2, 5.0f, 5.0f))

        val myTrilist2 = myTrilist.clone()

//        mytri1.setNumber(55);
        myTrilist[1].setNumber(100)
        Assert.assertEquals(100, myTrilist[1].myNumber_ )
        Assert.assertEquals(2,  myTrilist2[2].myNumber_ )

        Assert.assertEquals(myTrilist2[1],  myTrilist2[2].nodeTriangle[0] )
        Assert.assertEquals(myTrilist2[2],  myTrilist2[1].nodeTriangle[2] )

        Assert.assertEquals(myTrilist[1],  myTrilist[2].nodeTriangle[0] )
        Assert.assertEquals(myTrilist[2],  myTrilist[1].nodeTriangle[2] )

    }


    @Test
    fun testReverseNode() {
        val trilist = TriangleListK()
        trilist.add(TriangleK(5f, 8f, 5f))
        trilist.add(1, 1, 8f, 8f)
        trilist.add(1, 2, 5f, 5f)

        trilist.reverse()
        Assert.assertEquals(trilist.get(2), trilist.get(1).nodeTriangle[2])

    }

    @Test
    fun testRemoveNode() {
        val trilist = TriangleListK()
        trilist.add(TriangleK(5f, 8f, 5f))
        trilist.add(1, 1, 8f, 8f)
        trilist.add(2, 2, 5f, 5f)

        //trilist.remove( 2 )
        //assertEquals( 5f, trilist.get(1).nodeTriangle[1].length[1] )
        //assertEquals( trilist.get(2), trilist.get(1).nodeTriangle[1] )

        //val list2 = trilist.clone()

        trilist.remove(3)
        Assert.assertEquals(null, trilist[2].nodeTriangle[2])
    }

    @Test
    fun testResetNodeByObjectID() {
        val trilist = TriangleListK()
        trilist.add(TriangleK(5f, 8f, 5f))
        trilist.add(1, 1, 8f, 8f)
        trilist.add(2, 2, 5f, 5f)

        trilist.resetNodeByID(Params("", "", 3, 6f, 6f, 6f, 1, 2))
        Assert.assertEquals(trilist[1], trilist[3].nodeTriangle[0])
        Assert.assertEquals(trilist[3], trilist[1].nodeTriangle[2])


    }

    @Test
    fun testResetPointAndAngleByNodeChain() {
        val trilist = TriangleListK()
        trilist.add(TriangleK(5f, 8f, 5f))
        trilist.add(1, 1, 8f, 8f)
        trilist.add(2, 2, 5f, 5f)

        //trilist.resetPointAndAngleByNodeChain( Params("", "", 2, 6f, 6f, 6f, 1, 1) )
        trilist[2].resetElegant( Params("", "", 2, 6f, 6f, 6f, 1, 1) )

        Assert.assertEquals(6f, trilist[1].length[1])
        //assertEquals( trilist.get(2).point[0].x, trilist.get(1).point[2].x )

    }

    @Test
    fun testResetFromParamCase4() {
        val trilist = TriangleListK()
        trilist.add(TriangleK(5f, 8f, 5f))
        trilist.add(1, 1, 8f, 8f)
        trilist.add(2, 2, 5f, 5f)

        //　ケース４：接続三角形にとって辺長がおかしい場合は伝播を止める
        trilist.resetFromParam(Params("", "", 2, 12f, 12f, 12f, 1, 1))
        Assert.assertEquals(8f, trilist[1].length[1])
        Assert.assertEquals(8f, trilist[3].length[0])

    }

    @Test
    fun testResetFromParamCase3() {
        val trilist = TriangleListK()
        trilist.add(TriangleK(5f, 5f, 5f, PointXY( 0f, 0f ), 180f ))
        trilist.add(1, 1, 5f, 5f)
        trilist.add(2, 2, 5f, 5f)

        //tri1△
        Assert.assertEquals(4.330f, trilist[1].point[2].y, 0.001f)
        Assert.assertEquals(0.000f, trilist[1].point[0].y, 0.001f)
        //tri2▽ △
        Assert.assertEquals(4.330f, trilist[2].point[0].y, 0.001f)
        Assert.assertEquals(0.000f, trilist[2].point[1].y, 0.001f)

        //　ケース３：二重断面中、7:BC、trilist.get(1)：△、(2)：▽△
        trilist.resetFromParam(Params("", "", 2, 6f, 6f, 6f, 1, 7))
        Assert.assertEquals(4.763f, trilist[2].point[0].y, 0.001f)
        Assert.assertEquals(-0.433f, trilist[2].point[1].y, 0.001f)

        //　ケース３：二重断面右、3:BR、trilist.get(1)：△、(2)：▽△
        trilist.resetFromParam(Params("", "", 2, 6f, 6f, 6f, 1, 3))
        Assert.assertEquals(4.330f, trilist[2].point[0].y, 0.001f)
        Assert.assertEquals(-0.866f, trilist[2].point[1].y, 0.001f)

        //　ケース３：二重断面左、4:BL、trilist.get(1)：△、(2)：▽△
        trilist.resetFromParam(Params("", "", 2, 6f, 6f, 6f, 1, 4))
        Assert.assertEquals(5.196f, trilist[2].point[0].y, 0.001f)
        Assert.assertEquals(0.000f, trilist[2].point[1].y, 0.001f)


    }

    @Test
    fun testResetFromParamCase2() {
        val trilist = TriangleListK()
        trilist.add(TriangleK(5f, 5f, 5f))
        trilist.add(1, 1, 5f, 5f)
        trilist.add(2, 2, 5f, 5f)

        //　ケース２：接続辺の書き換えが子に伝播する
        trilist.resetFromParam(Params("", "", 2, 5f, 6f, 6f, 1, 1))
        Assert.assertEquals(6f, trilist[3].length[0])

        //　ケース３：接続辺の書き換えが親に伝播する
        trilist.resetFromParam(Params("", "", 2, 6f, 6f, 6f, 1, 1))
        Assert.assertEquals(6f, trilist[1].length[1])

    }

    @Test
    fun testResetFromParamCase1() {
        val trilist = TriangleListK()
        trilist.add(TriangleK(5f, 5f, 5f))
        trilist.add(1, 1, 5f, 5f)
        trilist.add(2, 2, 5f, 5f)

        // ケース１：親番号の書き換え
        trilist.resetFromParam(Params("", "", 3, 5f, 6f, 6f, 1, 2))
        Assert.assertEquals(1, trilist[3].parentNumber_)
        Assert.assertEquals(3, trilist[3].myNumber_)

    }

    @Test
    fun testReplaceByConnectNodeNumber() {
        val trilist = TriangleListK()
        trilist.add(TriangleK(5f, 5f, 5f))
        trilist.add(1, 1, 5f, 5f)
        trilist.add(2, 2, 5f, 5f)

        trilist.replace(3, 1)

        Assert.assertEquals(1, trilist[3].parentNumber_)
    }

    @Test
    fun testResetAllNodesAtClone() {
        val trilist = TriangleListK()
        trilist.add(TriangleK(5f, 5f, 5f))
        trilist.add(1, 1, 5f, 5f)
        trilist.add(1, 2, 5f, 5f)

        Assert.assertEquals(trilist[1], trilist[3].nodeTriangle[0])
        Assert.assertEquals(trilist[1], trilist[2].nodeTriangle[0])
        Assert.assertEquals(trilist[2], trilist[1].nodeTriangle[1])
        Assert.assertEquals(trilist[3], trilist[1].nodeTriangle[2])

        val trilist2 = trilist.clone()

        Assert.assertEquals(trilist2.get(1), trilist2.get(3).nodeTriangle[0])
        Assert.assertEquals(trilist2.get(1), trilist2.get(2).nodeTriangle[0])
        Assert.assertEquals(trilist2.get(2), trilist2.get(1).nodeTriangle[1])
        Assert.assertEquals(trilist2.get(3), trilist2.get(1).nodeTriangle[2])


    }

    @Test
    fun testReset() {
        val trilist = TriangleListK()
        trilist.add(TriangleK(5f, 5f, 5f))
        trilist.add(1, 1, 5f, 5f)
        trilist.add(1, 2, 5f, 5f)

        Assert.assertEquals(1, trilist.get(3).parentNumber_)

        trilist.get(3).resetLength(5f, 6f, 6f)

        Assert.assertEquals(1, trilist.get(3).parentNumber_)


    }

    @Test
    fun testTriListUndo() {
        val trilist = TriangleListK()
        trilist.add(TriangleK(5f, 5f, 5f))
        trilist.add(1, 4, 6f, 5f, 5f)
        trilist.undo()

        Assert.assertEquals(2, trilist.size())

    }

    @Test
    fun testTriTreeReverse2() {
        val tritree = TriangleListK()
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        tritree.add(TriangleK(5f, 5f, 5f, PointXY(0f, 0f), 0f))
        tritree.add(1, 1, 5f, 5f, 5f)
        tritree.add(2, 1, 5f, 5f, 5f)
        tritree.add(3, 2, 5f, 5f, 5f)
        tritree.add(2, 2, 5f, 5f, 5f)

        Assert.assertEquals(5f, tritree.get(5).point[2].x, 0.001f)

        val revtree = tritree.reverse()

        Assert.assertEquals(2.5f, revtree.get(1).point[1].x, 0.001f)

    }

    @Test
    fun testTriTreeReverse() {
        val tritree = TriangleListK()
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        tritree.add(TriangleK(5f, 5f, 5f, PointXY(0f, 0f), 0f))
        tritree.add(1, 1, 5f, 5f, 5f)
        tritree.add(2, 1, 5f, 5f, 5f)
        tritree.add(2, 2, 5f, 5f, 5f)

        Assert.assertEquals(5f, tritree.get(4).point[2].x, 0.001f)

        val revtree = tritree.reverse()

        Assert.assertEquals(2.5f, revtree.get(1).point[1].x, 0.001f)

    }

    @Test
    fun testTriListReverse(){
        val triList = TriangleListK()
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        triList.add(TriangleK(5f, 5f, 5f))
        triList.add(1, 1, 6f, 9f, 8f)
        triList.add(2, 2, 6f, 7f, 4f)

        Assert.assertEquals(1, triList[1].myNumber_)
        Assert.assertEquals(2, triList[2].myNumber_)
        Assert.assertEquals(3, triList[3].myNumber_)

        val triList2 = triList.reverse()

        Assert.assertEquals(1, triList2[1].myNumber_)
        Assert.assertEquals(2, triList2[2].myNumber_)
        Assert.assertEquals(3, triList2[3].myNumber_)

        Assert.assertEquals(-1, triList2[1].parentNumber_)
        Assert.assertEquals( 1, triList2[2].parentNumber_)
        Assert.assertEquals( 2, triList2[3].parentNumber_)

    }

    @Test
    fun testTriListOutline(){
        val trilist = TriangleListK()
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        trilist.add(TriangleK(8f, 6f, 8f))//1
        trilist.add(1, 2, 9f, 8f)//2
        trilist.add(2, 4, 7f, 4f, 9f)//3
        trilist.add(3, 2, 2f, 9f)//4
        trilist.add(4, 2, 9f, 2f)//5
        trilist.add(5, 1, 5f, 5f)//6
        trilist.add(6, 2, 5f, 2f)//7
        trilist.add(6, 1, 2f, 5f)//8
        trilist.add(8, 2, 5f, 5f)//9
        trilist.add(9, 1, 2f, 5f)//10
        trilist.add(9, 2, 5f, 5f)//11

        trilist.setChildsToAllParents()

        val op = ArrayList<PointXY>()
        val tlop = trilist.traceOrJumpForward(0, 0, op)!! //getOutLinePoints( 0 )
        Assert.assertEquals(11, trilist.size())
        Assert.assertEquals(14, tlop.size)
        Assert.assertEquals(
                "0ab,0bc,2bc,3bc,7bc,9bc,9ca,10bc,10ca,6bc,6ca,4ca,1bc,1ca,",
                trilist.outlineStr_
        )

        //       val tlop = trilist.traceOrJumpBackward( 10, 0, op ) //getOutLinePoints( 0 )


//        val aop = trilist.getOutlineLists( )
        //      assertEquals( 2, aop.size )
        //    assertEquals( "0ab,1bc,2ab,3bc,3ca,2ca,0ca,4ab,4bc,4ca,", trilist.outlineStr_ )

    }

    @Test
    fun testConnectionTypeLoad(){
        val tri1 = TriangleK(3f, 4f, 5f)
        val tri2 = TriangleK(tri1, 3, 3f, 4f, 5f)
        val tri3 = TriangleK(tri2, ConnParam(1, 0, 2, 0f), 3f, 5f)
        val tri4 = TriangleK(tri3, 4, 3f, 4f, 5f)
        val tri5 = TriangleK(tri4, 6, 3f, 4f, 5f)
        val tri6 = TriangleK(tri5, ConnParam(1, 1, 0, 4f), 3f, 5f)
        val tri7 = TriangleK(tri6, ConnParam(2, 1, 0, 4f), 3f, 5f)
        val tri8 = TriangleK(tri7, 5, 3f, 4f, 5f)
        val tri9 = TriangleK(tri7, 7, 3f, 4f, 5f)
        val tri10 = TriangleK(tri7, 8, 3f, 4f, 5f)
        val tri11 = TriangleK(tri7, 9, 3f, 4f, 5f)
        val tri12 = TriangleK(tri7, 10, 3f, 4f, 5f)

        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC

        Assert.assertEquals(2, tri2.cParam_.lcr)
        Assert.assertEquals(2, tri3.cParam_.lcr)
        Assert.assertEquals(0, tri4.cParam_.lcr)
        Assert.assertEquals(0, tri5.cParam_.lcr)
        Assert.assertEquals(0, tri6.cParam_.lcr)
        Assert.assertEquals(0, tri7.cParam_.lcr)
        Assert.assertEquals(2, tri8.cParam_.lcr)
        Assert.assertEquals(1, tri9.cParam_.lcr)
        Assert.assertEquals(1, tri10.cParam_.lcr)
        Assert.assertEquals(2, tri11.cParam_.lcr)
        Assert.assertEquals(2, tri12.cParam_.lcr)
    }

    @Test
    fun testResetConnectedTriangles() {
        val trilist = TriangleListK()
        trilist.add(TriangleK(5f, 5f, 5f))

        val params = Params("", "", 1, 7f, 7f, 7f, -1, 1)
        trilist.resetFromParam(params)

    }


/*
    @Test
    fun testAlignDimsInExportDXF() {
        val trilist = TriangleListK()
        trilist.add(TriangleK(5f, 5f, 5f))
        trilist.add(TriangleK(trilist.get(1), 2, 7f, 7f))
        trilist.add(TriangleK(trilist.get(1), 1, 6f, 6f))

        val dxfwriter = DxfFileWriter(trilist)
        val tri = trilist.get(1)
        val pca = tri.point[0]
        val pab = tri.point[1]
        val pbc = tri.point[2]
        val alignVdimA = dxfwriter.alignVByVector(tri.myDimAlignA_, pca, pab)
        val alignVdimB = dxfwriter.alignVByVector(tri.myDimAlignB_, pab, pbc)//flip(tri.myDimAlignB_, tri.dimAngleB_ )
        val alignVdimC = dxfwriter.alignVByVector(tri.myDimAlignC_, pbc, pca)//flip(tri.myDimAlignC_, tri.dimAngleC_ )

        org.junit.Assert.assertEquals(1, alignVdimA)
        org.junit.Assert.assertEquals(3, alignVdimB)
        org.junit.Assert.assertEquals(3, alignVdimC)

        org.junit.Assert.assertEquals(4, "No.3".length)


    }
*/

    @Test
    fun testExportPDF() {
        val trilist = TriangleListK()
        trilist.add(TriangleK(5f, 5f, 5f))
        trilist.add(TriangleK(trilist.get(1), 2, 7f, 7f))
        trilist.add(TriangleK(trilist.get(1), 1, 6f, 6f))

        //val pdfwriter = PdfWriter( 1f, trilist )
    }

    @Test
    fun testSeparateChild() {
        val trilist = TriangleListK()
        trilist.add(TriangleK(5f, 5f, 5f))
        trilist.add(TriangleK(trilist.get(1), 2, 7f, 7f))
        trilist.add(TriangleK(trilist.get(1), 1, 6f, 6f))

        Assert.assertEquals(true, trilist.get(1).hasChildIn(2))
        Assert.assertEquals(true, trilist.get(1).hasChildIn(1))

        trilist.add(TriangleK(trilist.get(2), 1, 6f, 6f))
        Assert.assertEquals(true, trilist.get(2).hasChildIn(1))
        Assert.assertEquals(false, trilist.get(2).hasChildIn(2))

        trilist.add(TriangleK(trilist.get(2), 2, 6f, 6f))
        Assert.assertEquals(true, trilist.get(2).hasChildIn(1))


    }


    @Test
    fun testParentHaveAChild() {
        val trilist = TriangleListK()
        trilist.add(TriangleK(5f, 5f, 5f))
        trilist.add(TriangleK(trilist.get(1), 2, 7f, 7f))
        trilist.add(TriangleK(trilist.get(2), 2, 6f, 6f))

        Assert.assertEquals(true, trilist.get(1).hasChildIn(2))
    }

    @Test
    fun testGetNumberList() {
        val trilist = TriangleListK()
        trilist.add(TriangleK(5f, 5f, 5f))
        trilist.add(TriangleK(trilist.get(1), 2, 5f, 5f))
        trilist.add(TriangleK(trilist.get(1), 1, 6f, 6f))
        trilist.get(1).myName_ = "No.10"
        trilist.get(2).myName_ = "No.6"
        trilist.get(3).myName_ = "No.2"

        val numlist = trilist.getSokutenList(2, 4)

//        Assert.assertEquals("No.2", numlist[0].myName_ )
        Assert.assertEquals("No.6", numlist[2].myName_)

        //Assert.assertEquals(-2.5f, numlist[0].point[0].vectorTo( numlist[1].point[0] ).x )
        //Assert.assertEquals(-2.5f, numlist.vectorToNextFrom( 0 ).x )

        Assert.assertEquals(-4, trilist.getSokutenListVector() )

    }

    @Test
    fun testTriListSlide() {
        val trilist = TriangleListK()
        trilist.add(TriangleK(5f, 5f, 5f, PointXY( 0f, 0f ), 180f ))
        trilist.add(TriangleK(trilist.get(1), 2, 5f, 5f))
        trilist.add(TriangleK(trilist.get(1), 2, 6f, 6f))
        trilist.setChildsToAllParents() // これやらないと認知されない。

        Assert.assertEquals(true, trilist.get(1).alreadyHaveChild(2))
        Assert.assertEquals(false, trilist.get(1).alreadyHaveChild(1))
        Assert.assertEquals(2.5, trilist.get(2).point[2].x.toDouble(), 0.001)
        Assert.assertEquals(3.473, trilist.get(3).point[2].x.toDouble(), 0.001)
    }

    @Test
    fun testTriListGetTap() {
        val tri1 = TriangleK(3f, 4f, 5f, PointXY( 0f, 0f ), 180f )
        val tri2 = TriangleK(tri1, ConnParam(1, 0, 2, 0f), 3f, 4f)
        val tri3 = TriangleK(tri1, ConnParam(2, 0, 2, 0f), 3f, 5f)
        val trilist = TriangleListK( tri1 )
        trilist.add(tri2)
        trilist.add(tri3)
        Assert.assertEquals(3, trilist.getTapIndexArray(PointXY(-3f, 2f)).size.toLong())
        Assert.assertEquals(2, trilist.getTapHitCount(PointXY(-3f, 2f)).toLong())
    }

    @Test
    fun testConnectionType3() {
        val tri1 = TriangleK(3f, 4f, 5f, PointXY( 0f, 0f ), 180f )
        val tri2 = TriangleK(tri1, 3, 3f, 4f, 5f)
        val tri3 = TriangleK(tri2, ConnParam(1, 0, 2, 0f), 3f, 5f)
        val trilist = TriangleListK(tri1)
        trilist.add(tri2)
        trilist.add(tri3)
        tri2.reset(tri2, ConnParam(1, 1, 1, 3f))
        trilist.lastTapNum_ = 2
        trilist.rotateCurrentTriLCR()
        Assert.assertEquals(0f, tri3.point[0].y, 0.0001f)
        //trilist.resetTriConnection(2, ConnParam(1, 1, 0, 3f))
        Assert.assertEquals(0f, tri3.point[0].y, 0.0001f)
        val tri4 = TriangleK(tri1, ConnParam(1, 1, 2, 3f), 3f, 5f)
        Assert.assertEquals(true, tri4.point[0].equals(-3f, 4f))
        val tri5 = TriangleK(tri1, ConnParam(2, 1, 2, 3f), 3f, 5f)
        Assert.assertEquals(true, tri5.point[0].equals(0f, 0f))
        val tri8 =
                TriangleK(tri1, Params("", "", 2, 3f, 3f, 5f, 2, 5, tri1.point[0], PointXY(0f, 0f)))
        Assert.assertEquals(true, tri8.point[0].equals(0f, 0f))
        val tri6 = TriangleK(tri1, ConnParam(2, 1, 0, 3f), 3f, 5f)
        Assert.assertEquals(true, tri6.point[0].equals(-1.1999f, 1.5999f))
        val tri7 =
                TriangleK(tri1, Params("", "", 2, 3f, 3f, 5f, 2, 6, tri1.point[0], PointXY(0f, 0f)))
        Assert.assertEquals(true, tri7.point[0].equals(-1.1999f, 1.5999f))
    }

    @Test
    fun testConnectionType2() {
        val tri1 = TriangleK(3f, 4f, 5f)
        val tri2 = TriangleK(tri1, 9, 3f, 4f, 5f)// 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC

        // test float connection
        Assert.assertEquals(3f, tri1.point[1].x, 0.0001f)

        Assert.assertEquals(4f, tri2.point[0].x, 0.0001f)


        Assert.assertEquals(-3.5f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(-3.0f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(-4.0f, tri2.rotateLCR().y, 0.0001f)

        val tri3 = TriangleK(tri2, ConnParam(1, 0, 2, 3f), 3f, 5f)
        Assert.assertEquals(-1f, tri3.point[0].y, 0.0001f)

        tri3.resetByParent(tri2.rotateLCRandGet(), tri3.cParam_)
        Assert.assertEquals(-3.0f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(-0.5f, tri3.point[0].y, 0.0001f)
        tri3.resetByParent(tri2.rotateLCRandGet(), tri3.cParam_)
        Assert.assertEquals(-3.5f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(-1f, tri3.point[0].y, 0.0001f)
        tri3.resetByParent(tri2.rotateLCRandGet(), tri3.cParam_)
        Assert.assertEquals(-4.0f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(0f, tri3.point[0].y, 0.0001f)
        val trilist = TriangleListK(tri1)
        trilist.add(tri2)
        trilist.add(tri3)
        trilist.lastTapNum_ = 2
        trilist.rotateCurrentTriLCR() //3
        trilist.rotateCurrentTriLCR() //3.5
        Assert.assertEquals(-4.0f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(-3.5f, tri2.rotateLCR().y, 0.0001f)
        trilist.rotateCurrentTriLCR() //3.0
        Assert.assertEquals(0f, tri3.point[0].y, 0.0001f)
    }


    @Test
    fun testConnectionType() {
        val tri1 = TriangleK(5f, 5f, 5f)
        val tri2 = TriangleK(tri1, 3, 4f, 5f, 5f)

        //tri2.setConnectionType( 1, 1, 1, 4f);
        Assert.assertEquals(5f, tri2.nodeTriangle[0]!!.point[1].x, 0.0001f)
        Assert.assertEquals(2.5f, tri2.getParentPointByLCR(1 ).x, 0.0001f)
        Assert.assertEquals(2.5f, tri2.getParentPointByType(1, 0, 1).x, 0.0001f)
        Assert.assertEquals(2.5f, tri2.getParentPointByType(1, 1, 1).x, 0.0001f)
        Assert.assertEquals(3.3660f, tri2.getParentPointByType(1, 2, 1).x, 0.0001f)
        Assert.assertEquals(-4.8301f, tri2.getParentPointByType(1, 2, 1).y, 0.0001f)
        Assert.assertEquals(-4.8301f, tri2.setBasePoint(1, 2, 1).y, 0.0001f)
        Assert.assertEquals(-3.4641f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(-4.3301f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(-3.8971f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(-3.4641f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(-4.3301f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(-3.8971f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(-3.4641f, tri2.rotateLCR().y, 0.0001f)
        Assert.assertEquals(-4.3301f, tri2.rotateLCR().y, 0.0001f)
        val trilist = TriangleListK(tri1)
        trilist.add(tri2)
        Assert.assertEquals(-3.8971f, trilist[2].rotateLCR().y, 0.001f)
        trilist.scale(PointXY(0f, 0f), 2f)
        Assert.assertEquals(-6.928f, trilist[2].rotateLCR().y, 0.001f)
        Assert.assertEquals(-8.046f, trilist[2].point[2].y, 0.001f)
    }

    @Test
    fun testTrilistSpritByColors() {
        val trilist = TriangleListK()
        trilist.add(TriangleK(3f, 4f, 5f))
        Assert.assertEquals(true, trilist.validTriangle(TriangleK(trilist[1], 2, 3f, 4f)))

        trilist.add(TriangleK(trilist[1], 2, 3f, 4f))
        trilist.add(TriangleK(trilist[2], 1, 4f, 5f))
        trilist.add(TriangleK(trilist[3], 2, 3f, 4f))
        trilist.add(TriangleK(trilist[4], 1, 4f, 5f))
        Assert.assertEquals(5, trilist.size().toLong())
        trilist[2].color_ = 0
        val listByColors = trilist.spritByColors()
        Assert.assertEquals(1, listByColors[0].size().toLong())
        Assert.assertEquals(0, listByColors[1].size().toLong())
        Assert.assertEquals(0, listByColors[2].size().toLong())
        Assert.assertEquals(0, listByColors[3].size().toLong())
        Assert.assertEquals(4, listByColors[4].size().toLong())
    }


    @Test
    fun testSetTriAngleBy() {
        val trilist = TriangleListK(TriangleK(3f, 4f, 5f))
        trilist.add(TriangleK(trilist[1], 2, 3f, 4f))
        trilist.add(TriangleK(trilist[2], 1, 4f, 5f))
        Assert.assertEquals(-53.13f, trilist[1].angleMmCA, 0.01f)
    }

    @Test
    fun testGetTapLength() {
        val trilist = TriangleListK(TriangleK(5f, 5f, 5f))
        Assert.assertEquals(0, trilist[1].getTapLength(PointXY(2.5f, 0f)).toLong())
        Assert.assertEquals(1, trilist[1].getTapLength(PointXY(3.75f, -2.5f)).toLong())
        Assert.assertEquals(2, trilist[1].getTapLength(PointXY(1.25f, -2.5f)).toLong())
        Assert.assertEquals(2.5f, trilist[1].dimpoint[0].x, 0.001f)
        trilist.add(TriangleK(trilist[1], 2, 5f, 5f))
        trilist.add(TriangleK(trilist[2], 1, 5f, 5f))
        Assert.assertEquals(-6.495f, trilist[3].dimpoint[1].y, 0.001f)
        Assert.assertEquals(1.25f, trilist[3].dimpoint[1].x, 0.001f)
    }

    @Test
    fun testTriListPrintScale() {
        val trilist = TriangleListK(TriangleK(80f, 40f, 50f))
        Assert.assertEquals(2.5, trilist.getPrintScale(1.0f).toDouble(), 0.1)
        val scalefactor = 12.5f
        trilist.scale(PointXY(0f, 0f), scalefactor)
        Assert.assertEquals(2.5, trilist.getPrintScale(12.5f).toDouble(), 0.1)
    }

    @Test
    fun testFloatConnection() { // connection type - 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        val mytlist = TriangleListK(TriangleK(3f, 4f, 5f))
        mytlist.add(TriangleK(mytlist[1], 2, 3f, 4f))
        mytlist.add(TriangleK(mytlist[2], 9, 3f, 4f, 5f)) // pbc:9 is B-Float, 10 is C-Float
        Assert.assertEquals(3f, mytlist.size().toFloat(), 0.001f)
        Assert.assertEquals(-5.0f, mytlist[3].point[0].y, 0.001f)
    }

    @Test
    fun testRotateBySize() {
        val mytlist = TriangleListK(TriangleK(3f, 4f, 5f))
        mytlist.add(TriangleK(mytlist[1], 2, 5f, 8f))
        mytlist.add(TriangleK(mytlist[2], 2, 4f, 3f))
        //mytlist.add(new TriangleK(mytlist.get(3),2,4,5));
        mytlist.rotateByLength("riseup")
        mytlist.rotateByLength("laydown")
        Assert.assertEquals(4.5980763f, mytlist.measureMostLongLine().x, 0.001f)
    }
    @Test
    fun testTrilistBounds() {
        val mytlist = TriangleListK(TriangleK(3f, 4f, 5f))
        mytlist.add(TriangleK(mytlist[1], 2, 6f, 5f))
        mytlist.add(TriangleK(mytlist[2], 2, 5f, 6f))
        val (left, top, right, bottom) = mytlist.calcBounds()
        Assert.assertEquals(3f, right, 0.001f)
        Assert.assertEquals(-4f, bottom, 0.001f)
        Assert.assertEquals(-6f, left, 0.001f)
        Assert.assertEquals(0f, top, 0.001f)
        Assert.assertEquals(0f, mytlist.myCenter.y, 0.001f)
        Assert.assertEquals(9f, mytlist.measureMostLongLine().x, 0.001f)
        Assert.assertEquals(4f, mytlist.measureMostLongLine().y, 0.001f)
        var myList2 = TriangleListK(TriangleK(3f, 4f, 5f))
        myList2.add(TriangleK(mytlist[1], 2, 6f, 5f))
        myList2.add(TriangleK(mytlist[2], 2, 5f, 6f))
        myList2.scale(PointXY(0f, 0f), 5f)

        myList2 = mytlist.clone()
        myList2.scale(PointXY(0f, 0f), 5f)
        Assert.assertEquals(20f, mytlist.measureMostLongLine().y, 0.001f)
    }

    @Test
    fun testCollision() {
        val tri = TriangleK(3f, 4f, 5f)
        Assert.assertEquals(true, tri.collision(-2f, 2f))
        Assert.assertEquals(false, PointXY(-2f, -2f).isCollide(tri))
        Assert.assertEquals(true, PointXY(2f, -2f).isCollide(tri))
        Assert.assertEquals(true, tri.isCollide(PointXY(2f, -2f)))
        val trilist = TriangleListK(tri)
        val tri2 = TriangleK(tri, 2, 3f, 4f)
        trilist.add(tri2)
        Assert.assertEquals(-4f, tri2.point[2].y, 0.001f)
        Assert.assertEquals(2, trilist.isCollide(PointXY(1f, -3f)).toLong())
        Assert.assertEquals(1, trilist.isCollide(PointXY(2f, -2f)).toLong())
    }

    @Test
    fun testRotate() {
        val mytri1 = TriangleK(3.0f, 4.0f, 5.0f, PointXY(5f, 5f), 180.0f)
        val myTrilist = TriangleListK(mytri1)
        myTrilist.add(TriangleK(mytri1, 2, 3.0f, 4.0f))
        mytri1.rotate(PointXY(0f, 0f), -90f)
        mytri1.rotate(PointXY(0f, 0f), -90f)
        mytri1.rotate(PointXY(0f, 0f), -90f)
        mytri1.rotate(PointXY(0f, 0f), -90f)
        Assert.assertEquals(5f, mytri1.point[0].x, 0.00001f)
        Assert.assertEquals(5f, mytri1.point[0].y, 0.00001f)
    }

    @Test
    fun testScale() {
        val mytri1 = TriangleK(3.0f, 4.0f, 5.0f, PointXY(0f, 0f), 180.0f)
        val myTrilist = TriangleListK(mytri1)
        myTrilist.add(TriangleK(mytri1, 2, 3.0f, 4.0f))
        myTrilist.setScale(PointXY(0f, 0f), 5f)
        Assert.assertEquals(4f, myTrilist.getTriangle(1).point[2].y, 0.001f)
    }

    @Test
    fun testGetTriangle() {
        val mytri1 = TriangleK(3.0f, 4.0f, 5.0f, PointXY(0f, 0f), 180.0f)
        val myTrilist = TriangleListK()
        myTrilist.add(mytri1)
        Assert.assertEquals(3.0f, myTrilist.getTriangle(1).length[0], 0.001f)
        Assert.assertEquals(1, myTrilist.size().toLong())
    }



    @Test
    fun testReplaceA() {
        val trilist = TriangleListK()
        trilist.add(TriangleK(5.0f, 5.0f, 5.0f, PointXY(0f, 0f), 180.0f))
        trilist.add(TriangleK(trilist.get(1), 2, 5f, 5f))

        // 新しい三角形を作って渡すと連動しないので、ポインタを取得してリセットする。
        trilist.resetConnectedTriangles(1, trilist.get(2).set(trilist.get(1), 2, 5f, 5f))
        Assert.assertEquals(trilist.get(2).point[1].x, trilist.get(1).point[2].x, 0.001f)
        Assert.assertEquals(trilist.get(2).point[1].y, trilist.get(1).point[2].y, 0.001f)

        trilist.resetFromParam(Params("", "", 2, 2.5f, 5f, 5f, 1, 2))
        Assert.assertEquals(trilist.get(2).point[1].x, trilist.get(1).point[2].x, 0.001f)
        Assert.assertEquals(trilist.get(2).point[1].y, trilist.get(1).point[2].y, 0.001f)

    }
    @Test
    fun testReplace() {
        val mytri1 = TriangleK(3.0f, 4.0f, 5.0f, PointXY(0f, 0f), 180.0f)
        val myTrilist = TriangleListK(mytri1)
        myTrilist.add(TriangleK(mytri1, 2, 3f, 4f))
        myTrilist.add(TriangleK(mytri1, 1, 3f, 5f))
        myTrilist.resetConnectedTriangles(1, mytri1.resetLength(3f, 2f, 4.5f))
        //myTrilist.getTriangle(3).reset(mytri1, 1);
        Assert.assertEquals(2.0f, myTrilist.getTriangle(1).length[1], 0.001f)
        Assert.assertEquals(3.0f, myTrilist.getTriangle(1).length[0], 0.001f)
        Assert.assertEquals(4.0f, myTrilist.getTriangle(3).length[0], 0.001f)
        Assert.assertEquals(1, myTrilist.getTriangle(1).myNumber_.toLong())
        Assert.assertEquals(3, myTrilist.size().toLong())


//        myTrilist.ResetTriangle(1, new TriangleK(6.0f, 4.0f, 5.0f, new PointXY(0,0), 180.0f));
        //      assertEquals(6.0f, myTrilist.getTriangle(1).getLengthA(), 0.001f);
    }

    @Test
    fun testTriangleArea() {
        val mytrilist = TriangleListK( TriangleK(3.0f, 4.0f, 5.0f, PointXY(0f, 0f), 180.0f) )
        mytrilist.add(TriangleK(mytrilist.getTriangle(1), 2, 3f, 4f))
        Assert.assertEquals(12f, mytrilist.getArea(), 0.01f)
    }

    @Test
    fun testDimAlign() {
        val mytri1 = TriangleK(2.0f, 2.3f, 1.2f, PointXY(0f, 0f), 180.0f)
        val myTrilist = TriangleListK(mytri1)
        myTrilist.add(TriangleK(mytri1, 3, 2.5f, 1.1f, 2.0f)) //2

        // 1下 3上 -> // 夾角の、1:外 　3:内
        Assert.assertEquals(3, myTrilist.getTriangle(1).dimAlignA.toLong())
        Assert.assertEquals(1, myTrilist.getTriangle(1).dimAlignB.toLong())
        Assert.assertEquals(3, myTrilist.getTriangle(1).dimAlignC.toLong())
        Assert.assertEquals(3, myTrilist.getTriangle(2).dimAlignA.toLong())
        Assert.assertEquals(3, myTrilist.getTriangle(2).dimAlignB.toLong())
        Assert.assertEquals(3, myTrilist.getTriangle(2).dimAlignC.toLong())
        val t1 = TriangleK(1.0f, 1.5f, 1.0f)
        Assert.assertEquals(3, t1.dimAlignA.toLong())
        Assert.assertEquals(3, t1.dimAlignB.toLong())
        Assert.assertEquals(3, t1.dimAlignC.toLong())
    }

    @Test
    fun testTriangleList() {
        val mytri1 = TriangleK(3.0f, 4.0f, 5.0f, PointXY(0f, 0f), 180.0f)
        val myTrilist = TriangleListK(mytri1)
        myTrilist.add(TriangleK(mytri1, 2, 3.0f, 4.0f))
        myTrilist.add(TriangleK(myTrilist.getTriangle(myTrilist.size()), 1, 4.0f, 5.0f))
        myTrilist.add(TriangleK(myTrilist.getTriangle(myTrilist.size()), 2, 3.0f, 4.0f))
        Assert.assertEquals(5.0, myTrilist.getTriangle(4).length[0].toDouble(), 0.001)

        myTrilist.resetConnectedTriangles(1, TriangleK(myTrilist.getTriangle(1), 2, 5.0f, 5.0f))
        Assert.assertEquals(2.0, myTrilist.getTriangle(2).myNumber_.toDouble(), 0.001)
        Assert.assertEquals(3.0, myTrilist.getTriangle(3).length[0].toDouble(), 0.001)
    }
}