package com.jpaver.trianglelist

class TrilistOutline( var trianglelist: TriangleList) :Cloneable{

    val trilist = trianglelist.trilist
    var outlineStr_ = trianglelist.outlineStr_

    fun traceOrJumpForward(
        startindex: Int,
        origin: Int,
        pointlist: ArrayList<PointXY>,
        triangle: Triangle
    ): ArrayList<PointXY>? {
        if (startindex < 0 || startindex >= trilist.size) return null

        // AB点を取る。すでにあったらキャンセル
        addOutlinePoint(triangle.pointAB, "ab,", pointlist, triangle)

        // 再起呼び出しで派生方向に右手伝いにのびていく
        traceOrNot(triangle.nodeTriangleB, origin, pointlist)

        // BC点を取る。すでにあったらキャンセル
        addOutlinePoint(triangle.pointBC, "bc,", pointlist, triangle)

        // 再起呼び出しで派生方向に右手伝いにのびていく
        traceOrNot(triangle.nodeTriangleC, origin, pointlist)

        // 折り返し
        traceOrJumpBackward(origin, pointlist, triangle)
        return pointlist
    }

    fun traceOrJumpBackward(origin: Int, pointlist: ArrayList<PointXY>, triangle: Triangle) {

        // 派生（ふたつとも接続）していたらそっちに伸びる、フロート接続だったり、すでに持っている点を見つけたらスルー
        branchOrNot(triangle, origin, pointlist)

        //BC点を取る。すでにあったらキャンセル
        addOutlinePoint(triangle.pointBC, "bc,", pointlist, triangle)

        //CA点を取る。すでにあったらキャンセル
        addOutlinePoint(triangle.point[0], "ca,", pointlist, triangle)

        //AB点を取る。すでにあったらキャンセル
        addOutlinePoint(triangle.pointAB, "ab,", pointlist, triangle)

        // 0まで戻る。同じ色でない時はリターン
        if (triangle.mynumber <= triangle.parentnumber) return
        if (triangle.nodeTriangleA_ != null && !triangle.isColored && !triangle.isFloating) traceOrJumpBackward(
            origin,
            pointlist,
            triangle.nodeTriangleA_!!
        )
    }

    // 同じポイントは二ついらない
    private fun notHave(it: PointXY, inthis: ArrayList<PointXY>): Boolean {
        //if( inthis.size() < 1 ) return false;
        for (i in inthis.indices) if (it.nearBy(inthis[i], 0.001f)) return false
        return true
    }

    fun traceOrNot(triangle: Triangle?, origin: Int, pointlist: ArrayList<PointXY>) {
        if (triangle != null && !triangle.isFloating && !triangle.isColored) traceOrJumpForward(
            triangle.mynumber - 1,
            origin,
            pointlist,
            triangle
        )
    }

    fun branchOrNot(t: Triangle, origin: Int, olp: ArrayList<PointXY>) {
        if (t.nodeTriangleB != null && t.nodeTriangleC != null) if (notHave(
                t.nodeTriangleC!!.point[0],
                olp
            ) && !t.nodeTriangleC!!.isFloating_ && !t.nodeTriangleC!!.isColored_
        ) traceOrJumpForward(t.nodeTriangleC!!.mynumber - 1, origin, olp, t.nodeTriangleC!!)
    }

    fun addOutlinePoint(pt: PointXY, strSide: String, pointlist: ArrayList<PointXY>, triangle: Triangle?) {
        if (triangle != null && notHave(pt, pointlist)) {
            pointlist.add(pt)
            outlineStr_ += triangle.mynumber.toString() + strSide
        }
    }

}