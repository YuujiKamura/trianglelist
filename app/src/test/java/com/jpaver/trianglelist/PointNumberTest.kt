package com.jpaver.trianglelist

import org.junit.Test

class PointNumberTest {
    fun pattern1( triangle: Triangle ): TriangleList{
        val trilist = TriangleList()
        trilist.add(triangle, true)//1
        trilist.add(1, 1, 5f, 5f)//2
        return trilist
    }

    fun testCase1( pattern: () -> TriangleList ){
        val trilist = pattern()
        val outlinelist = OutlineList(trilist) // OutlineList インスタンスを作成

        trilist.get(1).calcPoints( isArrange = true, outlineList = outlinelist )

        println( "pointlist:${outlinelist.pointlist}" )
        println( "pointcenter:${trilist.get(1).pointcenter}\npointnumber:${trilist.get(1).pointnumber}\n${trilist.get(1).toStrings()}")
    }

    @Test
    fun testAuto(){
        testCase1( {pattern1(Triangle(3.5f, 4f, 0.7f))} )
        testCase1( {pattern1(Triangle(3.5f, 4f, 1f))} )
        testCase1( {pattern1(Triangle(4.5f, 4f, 1f))} )
    }
}