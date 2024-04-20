package com.jpaver.trianglelist

class OutlineList(var trianglelist: TriangleList) :Cloneable{

    val trilist = trianglelist.trilist
    var outlineStr_ = ArrayList<String>()
    val pointlist = ArrayList<PointXY>()
    var outlineIndex = ArrayList<Int>()

    init{
    }

    fun compare(_target1: PointXY, _target2: PointXY): PointXY {
        if (trilist.isEmpty()) {
            println("outlinelist.compare: trilist is empty, returning _target1")
            return _target1
        }

        traceForward(0, 0, trilist[0])

        val targetOne = find(_target1)
        val targetTwo = find(_target2)

        if (targetOne == null || targetTwo == null) {
            println("outlinelist.compare: one or both targets not found, returning _target1")
            return _target1
        }

        val angle1 = calcAngle(targetOne)
        val angle2 = calcAngle(targetTwo)
        println("outlinelist.compare: angles computed - angle1=$angle1, angle2=$angle2")

        // リストの各要素をループして内容を表示
        pointlist.forEachIndexed() { i, point ->
            val b = trilist[outlineIndex[i]-1].nodeB
            val bnumber = b?.mynumber ?: 0
            val c = trilist[outlineIndex[i]-1].nodeC
            val cnumber = c?.mynumber ?: 0
            println("Point $i: ${outlineStr_[i]} x = ${point.x}, y = ${point.y} NodeB:${bnumber}:${b.hashCode()} NodeC:${cnumber}:${c.hashCode()}")
        }

        return if (angle1 > angle2) {
            println("outlinelist.compare: angle1 is greater, returning targetOne\n")
            targetOne
        } else {
            println("outlinelist.compare: angle2 is greater or equal, returning targetTwo\n")
            targetTwo
        }
    }


    fun calcAngle(target: PointXY): Float {
        val result = find(target) ?: return 0f
        val index = pointlist.indexOf(result)

        if (index == -1 || pointlist.size < 3) return 0f // リストに十分な要素がない、または見つからない場合

        // リストが閉じていると仮定して、前後のポイントのインデックスを計算
        val prevIndex = if (index > 0) index - 1 else pointlist.size - 1
        val nextIndex = if (index < pointlist.size - 1) index + 1 else 0

        val prevPoint = pointlist[prevIndex]
        val nextPoint = pointlist[nextIndex]

        // ここでポイントを出力
        println("prevPoint:$prevIndex${prevPoint.format()}, target:$index${target.format()}, nextPoint:$nextIndex${nextPoint.format()}")

        // prevPoint、result（現在のポイント）、nextPointを使って角度を計算
        // 360単位の角度を得る
        return prevPoint.calcAngle360(result, nextPoint)
    }

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
        traceOrNot(triangle.nodeB, origin)
        // BC点を取る。
        // C辺接続があれば、そっち方向に自身を呼び出す
        addPoint(triangle.pointBC, "bc,", pointlist, triangle)
        traceOrNot(triangle.nodeC, origin)
        // どっちもなければ折り返し
        traceBackward(origin, pointlist, triangle)
        return pointlist
    }

    fun addPoint(pt: PointXY, strSide: String, pointlist: ArrayList<PointXY>, triangle: Triangle?) {
        if (triangle != null && notHave(pt, pointlist)) {
            pointlist.add(pt)
            outlineStr_.add(triangle.mynumber.toString() + strSide)
            outlineIndex.add(triangle.mynumber)
        }
    }

    // 同じポイントは二ついらない
    private fun notHave(target: PointXY, pointarray: ArrayList<PointXY>): Boolean {
        for (i in pointarray.indices){
            if( target.nearBy(pointarray[i], 0.005f) ) return false
        }
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
        if (triangle.nodeA != null && !triangle.isColored && !triangle.isFloating)
            traceBackward(origin, pointlist, triangle.nodeA!!)
    }

    fun branchOrNot(triangle: Triangle, origin: Int, olp: ArrayList<PointXY>) {
        if (triangle.nodeB != null && triangle.nodeC != null)
            if (notHave(triangle.nodeC!!.point[0], olp)
                //&& !triangle.nodeC!!.isFloating_
                //&& !triangle.nodeC!!.isColored_ //??? trueになってたりするのでコメントアウト。
                )
                traceForward(triangle.nodeC!!.mynumber - 1, origin, triangle.nodeC!!)
    }

}