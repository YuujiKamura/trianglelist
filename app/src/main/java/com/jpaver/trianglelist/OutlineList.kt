package com.jpaver.trianglelist

class OutlineList(var trianglelist: TriangleList) :Cloneable{

    val trilist = trianglelist.trilist
    var outlineStr_ = trianglelist.outlineStr_
    val pointlist = ArrayList<PointXY>()

    fun find(target: PointXY): PointXY? {
        return pointlist.firstOrNull { pointXY ->
            target.nearBy( pointXY, 0.05f)
        }
    }

    fun traceForward(startindex: Int, origin: Int, triangle: Triangle): ArrayList<PointXY>?
    {
        if (startindex < 0 || startindex >= trilist.size) return null
        // AB点を取る。
        // B辺接続があれば、そっち方向に自身を呼び出す
        addPoint(triangle.pointAB, "ab,", pointlist, triangle)
        traceOrNot(triangle.nodeTriangleB, origin)
        // BC点を取る。
        // C辺接続があれば、そっち方向に自身を呼び出す
        addPoint(triangle.pointBC, "bc,", pointlist, triangle)
        traceOrNot(triangle.nodeTriangleC, origin)
        // どっちもなければ折り返し
        traceBackward(origin, pointlist, triangle)
        return pointlist
    }

    fun addPoint(pt: PointXY, strSide: String, pointlist: ArrayList<PointXY>, triangle: Triangle?) {
        if (triangle != null && notHave(pt, pointlist)) {
            pointlist.add(pt)
            outlineStr_ += triangle.mynumber.toString() + strSide
        }
    }

    // 同じポイントは二ついらない
    private fun notHave(it: PointXY, inthis: ArrayList<PointXY>): Boolean {
        for (i in inthis.indices) if (it.nearBy(inthis[i], 0.001f)) return false
        return true
    }

    fun traceOrNot(triangle: Triangle?, origin: Int) {
        if (triangle != null && !triangle.isFloating && !triangle.isColored)
            traceForward(triangle.mynumber - 1, origin, triangle )
    }


    fun traceBackward(origin: Int, pointlist: ArrayList<PointXY>, triangle: Triangle)
    {
        // 派生（ふたつとも接続）していたらそっちに伸びる、フロート接続だったり、すでに持っている点を見つけたらスルー
        branchOrNot(triangle, origin, pointlist)
        //BC点を取る。すでにあったらキャンセル
        addPoint(triangle.pointBC, "bc,", pointlist, triangle)
        //CA点を取る。すでにあったらキャンセル
        addPoint(triangle.point[0], "ca,", pointlist, triangle)
        //AB点を取る。すでにあったらキャンセル
        addPoint(triangle.pointAB, "ab,", pointlist, triangle)
        // 0まで戻る。同じ色でない時はリターン
        if (triangle.mynumber <= triangle.parentnumber) return
        if (triangle.nodeTriangleA_ != null && !triangle.isColored && !triangle.isFloating)
            traceBackward(origin, pointlist, triangle.nodeTriangleA_!!)
    }

    fun branchOrNot(triangle: Triangle, origin: Int, olp: ArrayList<PointXY>) {
        if (triangle.nodeTriangleB != null && triangle.nodeTriangleC != null)
            if (notHave(triangle.nodeTriangleC!!.point[0], olp)
                && !triangle.nodeTriangleC!!.isFloating_
                && !triangle.nodeTriangleC!!.isColored_)
                traceForward(triangle.nodeTriangleC!!.mynumber - 1, origin, triangle.nodeTriangleC!!)
    }

}