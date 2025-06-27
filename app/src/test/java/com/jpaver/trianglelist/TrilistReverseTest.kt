@file:Suppress("DEPRECATION")

package com.jpaver.trianglelist

import com.example.trilib.PointXY
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import junit.framework.Assert
import org.junit.Test

class TrilistReverseTest {
    @Test
    fun testTriTreeReverse2() {
        val tritree = TriangleList()
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        tritree.add(
            Triangle(5f, 5f, 5f,
                PointXY(0f, 0f), 0f), true)
        tritree.add(1, 1, 5f, 5f, 5f)
        tritree.add(2, 1, 5f, 5f, 5f)
        tritree.add(3, 2, 5f, 5f, 5f)
        tritree.add(2, 2, 5f, 5f, 5f)

        Assert.assertEquals(5f, tritree.get(5).pointBC.x, 0.001f)

        val revtree = tritree.reverse()

        Assert.assertEquals(5f, revtree.get(1).pointAB.x, 0.001f)

    }

    @Test
    fun testTriTreeReverse() {
        val tritree = TriangleList()
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        tritree.add(
            Triangle(5f, 5f, 5f,
                PointXY(0f, 0f), 0f), true)
        tritree.add(1, 1, 5f, 5f, 5f)
        tritree.add(2, 1, 5f, 5f, 5f)
        tritree.add(2, 2, 5f, 5f, 5f)

        Assert.assertEquals(5f, tritree.get(4).pointBC.x, 0.001f)

        val revtree = tritree.reverse()

        Assert.assertEquals(5f, revtree.get(1).pointAB.x, 0.001f)

    }

    @Test
    fun testTriListReverse(){
        val triList = TriangleList()
        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        triList.add(Triangle(5f, 5f, 5f), true)
        triList.add(1, 4, 6f, 9f, 8f)
        triList.add(2, 5, 6f, 7f, 4f)
        triList.add(3, 1, 3f, 9f)
        triList.add(2, 4, 6f, 9f, 8f)

        // test trilist get(n) is returned number, not index
        // wrapped arraylist get(i). ex, trilist.trilist_.get(i) is required index, not number.
        Assert.assertEquals(1, triList.get(0).mynumber) // ほんとはおかしいけどスルー
        Assert.assertEquals(1, triList.get(1).mynumber)
        Assert.assertEquals(2, triList.get(2).mynumber)

        // reverseの動作
        // ・1番から順に、次の三角形の親番号と接続辺を元に、自身の接続を書き換える。
        // ・3辺の順序を右回りか左回りにひとつスライドさせる。
        // ・逆順にソートする
        val triList2 = triList.reverse()
        Assert.assertEquals(1, triList2.get(1).mynumber)
        Assert.assertEquals(0, triList2.get(1).parentnumber)
        Assert.assertEquals(-1, triList2.get(2).parentnumber)


        val triList3 = triList.numbered(5)
        Assert.assertEquals(5, triList3.get(1).mynumber)

    }

    @Test
    fun testReverseNode() {
        val trilist = TriangleList()
        trilist.add(Triangle(5f, 8f, 5f), true)
        trilist.add(1, 1, 8f, 8f)
        trilist.add(1, 2, 5f, 5f)

        trilist.reverse()
        Assert.assertEquals(trilist.get(3), trilist.get(1).nodeC)
        print_trilist(trilist)

    }
}