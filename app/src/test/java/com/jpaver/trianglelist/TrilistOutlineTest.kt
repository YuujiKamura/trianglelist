package com.jpaver.trianglelist

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Assert
import org.junit.Test

@Suppress("DEPRECATION")
class TrilistOutlineTest {

    @Test
    fun testFind_calcAngle() { //ShouldReturnCorrectPointFromOutlineList
        // テストのセットアップ
        val trilist = setupTrilist() // テスト用の三角形リストを準備
        val outlinelist = OutlineList(trilist) // OutlineList インスタンスを作成
        val pointlist = outlinelist.traceForward(0, 0, trilist[1]) // traceForward で点のリストを生成

        // テスト対象のポイントを指定
        val target = trilist.get(10).pointAB
        val actualResult = outlinelist.find(target)!!
        // アサーション: find メソッドが正しいポイントを返すことを確認
        assertTrue( target.nearBy( actualResult, 0.01f) )

        val angles = pointlist!!.map{ outlinelist.calcAngle(it) }
        println(angles.joinToString("\n") )
        //assertTrue( angle>10f )
    }

    @Test
    fun testTrilistTrace() {
        val trilist = TriangleList()
        val olp = ArrayList<PointXY>()

        trilist.add(Triangle(3f, 4f, 5f), true)
        trilist.add(Triangle(trilist[1], 2, 3f, 4f), true)
        trilist.add(Triangle(trilist[2], 1, 3f, 4f), true)
        trilist.add(Triangle(trilist[2], 2, 3f, 4f), true)

        trilist.trace(olp, trilist[1], false)
        System.out.printf( "trilist size %s, outlineStr_ %s%n", trilist.size(), trilist.outlineStr_ )
    }

    @Test
    fun testTrilistSpritByColors() {
        val trilist = TriangleList()
        trilist.add(Triangle(3f, 4f, 5f), true)
        trilist.add(Triangle(trilist[1], 2, 3f, 4f), true)
        trilist.add(Triangle(trilist[2], 1, 4f, 5f), true)
        trilist.add(Triangle(trilist[2], 2, 3f, 4f), true)
        trilist.add(Triangle(trilist[4], 1, 4f, 5f), true)
        Assert.assertEquals(5, trilist.size().toLong())

        trilist[1].setColor(0)
        trilist[3].setColor(0)
        trilist[4].setColor(0)

        val listByColors = trilist.spritByColors()
        Assert.assertEquals(3, listByColors[0].size().toLong())
        Assert.assertEquals(0, listByColors[1].size().toLong())
        Assert.assertEquals(0, listByColors[2].size().toLong())
        Assert.assertEquals(0, listByColors[3].size().toLong())
        Assert.assertEquals(2, listByColors[4].size().toLong())

        printTriListList( listByColors )

        junit.framework.Assert.assertEquals(
            "1ab,1bc,1ca,1ab, 3ab,3bc,3ca,3ab, 4ab,4bc,4ca,4ab, ",
            listByColors[0].outlineStr_
        )

    }

    @Test
    fun testTriListOutlineFloat(){
        val trilist = TriangleList()
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        trilist.add(Triangle(8f, 6f, 8f), true)//1
        trilist.add(Triangle(trilist[1], 9, 5f,3f, 4f), true)
        trilist.add(Triangle(trilist[1], 10, 5f,3f, 4f), true)

        val listByColors = trilist.spritByColors()

        junit.framework.Assert.assertEquals(3, listByColors[4].outlineList_!!.size)
    }


    @Test
    fun testTriListOutlineSimple(){
        val trilist = TriangleList()
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        trilist.add(Triangle(8f, 6f, 8f), true)//1

        val op = ArrayList<PointXY>()
        val tlop = trilist.trace(op, trilist[1], false) //OrJumpForward(0, 0, op, trilist[1] ) //getOutLinePoints( 0 )
        junit.framework.Assert.assertEquals(1, trilist.size())
        junit.framework.Assert.assertEquals(3, tlop.size)
        junit.framework.Assert.assertEquals(
            "1ab,1bc,1ca,",
            trilist.outlineStr_
        )

        System.out.printf( "outlinestr %s%n", trilist.outlineStr_ )
        printTriList( trilist )
    }

    fun setupTrilist(): TriangleList{
        val trilist = TriangleList()
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        trilist.add(Triangle(8f, 6f, 8f), true)//1
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
        return trilist
    }

    @Test
    fun testTriListOutline(){
        val trilist = setupTrilist()
        ArrayList<PointXY>()
        val outlinelist = OutlineList(trilist)
        val pointlist = outlinelist.traceForward(0, 0, trilist[1])
        val strExpected = "1ab,1bc,3bc,4bc,8bc,10bc,10ca,11bc,11ca,7bc,7ca,5ca,2bc,2ca,"

        assertEquals(11, trilist.size())

        assertEquals(14, pointlist!!.size)


        assertEquals( strExpected, outlinelist.outlineStr_ )

        println(pointlist.toString())

        val hashes = pointlist.map { Integer.toHexString(it.hashCode()) }
        println(hashes.joinToString(" "))

    }

}