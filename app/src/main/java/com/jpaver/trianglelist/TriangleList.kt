package com.jpaver.trianglelist

import com.jpaver.trianglelist.util.Params
import java.util.Optional

class TriangleList : EditList {


    fun rotate(basepoint: PointXY, angle: Float, startnumber: Int, separationFreeMode: Boolean = false, is_recover: Boolean = false ) {

        var startindex = startnumber - 1

        // 0番からすべて回転させる場合
        if (!(startnumber > 1 && trilist[startindex].connectionType >= 9) || !separationFreeMode) {
            this.angle += angle
            this.basepoint = basepoint.clone()
            startindex = 0
        }
        // 開始インデックス以降の要素に対してのみ処理を行う
        trilist.drop(startindex).forEach {
            it.rotate( basepoint, angle, is_recover )
            //if(!it.isPointNumberAutoAligned)
                it.pointnumber = it.pointnumber.rotate( basepoint, angle )
        }
    }

    // region parameters
    var trilist: ArrayList<Triangle>
    var trilistStored_: ArrayList<Triangle>
    var myCollisionList: ArrayList<Collision>? = null
    var outlineList_: ArrayList<ArrayList<PointXY>>? = null
    var lastTapNumber_ = 0
    var lastTapSide_ = -1
    var lastTapCollideNum_ = 0
    var selectedNumber = 0
    var scale = 1f
    var angle = 0f
    var myBounds = Bounds(0f, 0f, 0f, 0f)
    var myCenter = PointXY(0f, 0f)
    var myLength = PointXY(0f, 0f)
    var outlineStr_ = ""
    var isDoubleTap_ = false

    //endregion parameters

    fun toStrings(): String {
        return trilist.joinToString(separator = "") { it.toStrings() }
    }

    override fun clone(): TriangleList {
        val b = TriangleList()
        try {
            b.basepoint = basepoint.clone()
            b.myBounds = myBounds
            b.myCenter = myCenter.clone()
            b.myLength = myLength.clone()
            //b = new TriangleList();
            b.selectedNumber = selectedNumber
            b.lastTapNumber_ = lastTapNumber_
            b.lastTapSide_ = lastTapSide_
            b.scale = scale
            b.angle = angle
            b.basepoint = basepoint.clone()
            b.outlineList_ = outlineList_
            for (i in trilist.indices) {
                b.trilist.add(trilist[i].clone())
                //b.myTriListAtView.add(this.myTriListAtView.get(i).clone());
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        //ノードポインターのリコネクト
        if (trilist.size > 0) b.resetAllNodes()
        return b
    }

    fun undo() {
        //if( trilistStored_ != null ) trilist_ = (ArrayList<Triangle>) trilistStored_.clone();
    }

    fun getSokutenList(start: Int, pitch: Int): ArrayList<Triangle> {
        val allSokTriList = allSokutenList
        val sokTriList = ArrayList<Triangle>()
        for (i in allSokTriList.indices) {
            // No.Stringから数値を抜き出してIntに変換する
            val s = allSokTriList[i].name
            val nameInt = s.replace("[^0-9]".toRegex(), "").toInt()

            // 剰余演算子%を使ってpitch(4)の倍数の場合を判定する
            if ((nameInt - start) % pitch == 0 || nameInt == start) {
                sokTriList.add(allSokTriList[i].clone())
            }
        }

        // 最後にいっこ足す
        if (sokutenListVector > 0 && sokTriList.size > 1) {
            val lasttri = sokTriList[sokTriList.size - 1]
            val pasttri = sokTriList[sokTriList.size - 2]
            val lastx = lasttri.point[0].x
            val pastx = pasttri.point[0].x
            sokTriList.add(Triangle(5f, 5f, 5f))
            sokTriList[sokTriList.size - 1].point[0].x = lastx + (lastx - pastx)
            sokTriList[sokTriList.size - 1].lengthNotSized[0] = lasttri.lengthNotSized[0]
            sokTriList[sokTriList.size - 1].length[0] = lasttri.length[0]
        }

        // 最初にいっこ足す
        if (sokutenListVector < 0 && sokTriList.size > 1) {
            val firsttri = sokTriList[0]
            val secondtri = sokTriList[1]
            val first = firsttri.point[0]
            val second = secondtri.point[0]
            sokTriList.add(0, Triangle(5f, 5f, 5f))
            sokTriList[0].point[0] = first.minus(second).plus(first)
            sokTriList[0].lengthNotSized[0] = firsttri.lengthNotSized[0]
            sokTriList[0].length[0] = firsttri.length[0]
        }
        return sokTriList
    }

    val allSokutenList: ArrayList<Triangle>
        // 全ての測点リストを返す
        get() {
            val numTriList = ArrayList<Triangle>()
            for (i in trilist.indices) {
                if (trilist[i].name.contains("No.")) {
                    numTriList.add(trilist[i].clone())
                }
            }
            return numTriList
        }
    val sokutenListVector: Int
        // 測点のリストの順逆を返す。正の時は三角形リストと同じ方向、負の時は逆方向
        get() {
            val allSokutenList = allSokutenList
            if (allSokutenList.size > 1) {
                // No.Stringから数値を抜き出してIntに変換する
                val sf = allSokutenList[0].name
                val fnInt = sf.replace("[^0-9]".toRegex(), "").toInt()
                val ss = allSokutenList[1].name
                val snInt = ss.replace("[^0-9]".toRegex(), "").toInt()
                return snInt - fnInt
            }
            return 0
        }

    private val textScaleCalculator = TextScaleCalculator()

    fun getPrintTextScale(drawingScale: Float, exportFileType: String): Float {
        return textScaleCalculator.getTextScale(drawingScale, exportFileType)
    }

    fun getPrintScale(drawingScale: Float): Float { // ex. 1/100 is w40m h27m drawing in range.
        scale(PointXY(0f, 0f), 1 / drawingScale)
        val longsidex = measureMostLongLine().x
        val longsidey = measureMostLongLine().y
        scale(PointXY(0f, 0f), drawingScale)
        val paperWidth = 38f
        val paperHeight = 25f

//        float printScale = 1f; //drawingScale;
        //if( longsideX <= paperWidth*0.2 && longsideY <= paperHeight*0.2 ) return printScale *= 0.2f;
        if (longsidex <= paperWidth * 0.5 && longsidey <= paperHeight * 0.4) return 0.5f
        if (longsidex <= paperWidth && longsidey <= paperHeight) return 1.0f
        if (longsidex <= paperWidth * 1.5 && longsidey <= paperHeight * 1.4) return 1.5f
        if (longsidex <= paperWidth * 2.0 && longsidey <= paperHeight * 1.9) return 2.0f
        if (longsidex <= paperWidth * 2.5 && longsidey <= paperHeight * 2.5) return 2.5f
        if (longsidex <= paperWidth * 3.0 && longsidey <= paperHeight * 3.0) return 3.0f
        if (longsidex <= paperWidth * 4.0 && longsidey <= paperHeight * 4.0) return 4.0f
        if (longsidex <= paperWidth * 4.5 && longsidey <= paperHeight * 4.5) return 4.5f
        if (longsidex <= paperWidth * 5.0 && longsidey <= paperHeight * 5.0) return 5.0f
        if (longsidex <= paperWidth * 6.0 && longsidey <= paperHeight * 5.0) return 6.0f
        if (longsidex <= paperWidth * 7.0 && longsidey <= paperHeight * 5.0) return 7.0f
        if (longsidex <= paperWidth * 8.0 && longsidey <= paperHeight * 5.0) return 8.0f
        if (longsidex <= paperWidth * 9.0 && longsidey <= paperHeight * 5.0) return 9.0f
        return if (longsidex <= paperWidth * 10.0 && longsidey <= paperHeight * 10.0) 10.0f else 15f
    }

    fun calcBounds(): Bounds {
        myBounds = Bounds(0f, 0f, 0f, 0f)
        for (i in trilist.indices) {
            myBounds = trilist[i].expandBoundaries(myBounds)
        }
        return myBounds
    }

    val center: PointXY
        get() {
            calcBounds()
            myCenter[(myBounds.right + myBounds.left) / 2] = (myBounds.top + myBounds.bottom) / 2
            return myCenter
        }

    fun measureMostLongLine(): PointXY {
        calcBounds()
        myLength[myBounds.right - myBounds.left] = myBounds.top - myBounds.bottom
        return myLength
    }

    fun measureLongLineNotScaled(): PointXY {
        return measureMostLongLine().scale(1 / scale)
    }

    fun rotateByLength(align: String): Float {
        var rot = 0f
        if (align == "laydown") {
            while (true) {
                rot -= 10f
                val beforeY = measureMostLongLine().y
                rotate(basepoint, rot, 0, false)
                if (measureMostLongLine().y >= beforeY) {
                    rotate(basepoint, -rot, 0, false)
                    return rot + 10f
                }
            }
        }
        if (align == "riseup") {
            while (true) {
                rot += 10f
                val beforeY = measureMostLongLine().y
                rotate(basepoint, rot, 0, false)
                if (measureMostLongLine().y <= beforeY) {
                    rotate(basepoint, -rot, 0, false)
                    return rot - 10f
                }
            }
        }
        return rot
    }

    internal constructor() {
        trilist = ArrayList()
        trilistStored_ = ArrayList()
        myCollisionList = ArrayList()
        outlineList_ = ArrayList()
        selectedNumber = trilist.size
    }

    internal constructor(myFirstTriangle: Triangle) {
        trilist = ArrayList()
        trilistStored_ = ArrayList()
        trilist.add(myFirstTriangle)
        myFirstTriangle.setNumber(1)
        selectedNumber = trilist.size
    }

    fun setScale(bp: PointXY, sc: Float) {
        scale = sc
        basepoint = bp.clone()
        //        this.cloneByScale(basepoint, myScale);
    }

    fun scale(basepoint: PointXY, scale: Float) {
        this.scale *= scale
        forEach { triangle ->
            triangle.scale(basepoint, scale)
        }
    }

    fun attachToTheView(basepoint: PointXY, scale: Float, ts: Float) {
        this.scale *= scale
        forEach { triangle ->
            triangle.scale(basepoint, scale)

            //これがないと結果が変わる、なぜだろう？
            triangle.setDimPath(ts)
        }
    }

    fun setDimPathTextSize(ts: Float) {
        forEach { triangle ->
            triangle.setDimPath(ts)
        }
    }

    fun arrangeNumbers(){
        forEach { triangle ->
            triangle.arrangeNumber()
        }
    }

    fun forEach(action: (Triangle) -> Unit) {
        trilist.forEachIndexed { _, triangle ->
            action(triangle)
        }
    }

    fun setDimsUnconnectedSideToOuter(target: Triangle?) {
        if(target == null ) return
        if (target.nodeTriangleA_ == null) target.dimVerticalA = 1 else target.dimVerticalA = 3
        if (target.nodeTriangleB == null) target.dimVerticalB = 1 else if (target.nodeTriangleB!!.connectionType > 2 ) target.dimVerticalB = 3
        if (target.nodeTriangleC == null) target.dimVerticalC = 1 else if (target.nodeTriangleC!!.connectionType > 2 ) target.dimVerticalC = 3
    }

    fun recoverState(bp: PointXY) {
        basepoint = bp.clone()
        trilist.map {
            it.rotate(basepoint, angle - 180, false)
            if (!it.pointNumber.flag.isMovedByUser){
                it.pointnumber = it.pointnumber.rotate(basepoint, angle - 180)
            }
        }
        arrangeNumbers()
    }

    override fun size(): Int {
        return trilist.size
    }

    override fun addCurrent(num: Int): Int {
        selectedNumber = selectedNumber + num
        return selectedNumber
    }

    override fun retrieveCurrent(): Int {
        return selectedNumber
    }

    fun changeSelectedNumber(c: Int) {
        selectedNumber = c
    }

    fun validTriangle(tri: Triangle): Boolean {
        return if (tri.length[0] <= 0.0f || tri.length[1] <= 0.0f || tri.length[2] <= 0.0f) false else !(tri.length[0] + tri.length[1] <= tri.length[2]) &&
                !(tri.length[1] + tri.length[2] <= tri.length[0]) &&
                !(tri.length[2] + tri.length[0] <= tri.length[1])
    }

    fun add(pnum: Int, pbc: Int, A: Float, B: Float, C: Float): Boolean {
        return add(Triangle(get(pnum), pbc, A, B, C), true)
    }

    fun add(pnum: Int, pbc: Int, B: Float, C: Float): Boolean {
        return add(Triangle(get(pnum), pbc, B, C), true)
    }

    fun add(nextTriangle: Triangle, numbering: Boolean): Boolean {
        if (!validTriangle(nextTriangle)) return false

        // 番号を受け取る
        if (numbering) nextTriangle.mynumber = trilist.size + 1
        val pbc = nextTriangle.connectionType
        if (nextTriangle.parentnumber > 0) {
            val parent = getByNumber(nextTriangle.parentnumber)
            if (parent.alreadyHaveChild(pbc)) {
                // すでに親の接続辺上に子供がいたら、挿入処理
                //nextTriangle.myNumber_ = nextTriangle.parentNumber_ +1;
                insertAndSlide(nextTriangle)
            } else {
                trilist.add(nextTriangle) // add by arraylist
            }

            // 親に告知する
            //if( nextTriangle.myNumber_ > 1 ) trilist_.get( nextTriangle.parentNumber_ -1 ).setChild(nextTriangle, nextTriangle.getParentBC());
        } else {
            trilist.add(nextTriangle) // add by arraylist
        }

        setDimsUnconnectedSideToOuter(nextTriangle)
        setDimsUnconnectedSideToOuter(nextTriangle.nodeTriangleA_)

        selectedNumber = nextTriangle.mynumber
        return true
    }

    fun resetAllNodes() {
        for (i in trilist.indices) {
            val tri = trilist[i]
            if (tri.nodeTriangleA_ != null) {
                tri.setNode(trilist[tri.nodeTriangleA_!!.mynumber - 1], 0)
            }
            if (tri.nodeTriangleB != null) {
                tri.setNode(trilist[tri.nodeTriangleB!!.mynumber - 1], 1)
            }
            if (tri.nodeTriangleC != null) {
                tri.setNode(trilist[tri.nodeTriangleC!!.mynumber - 1], 2)
            }
        }
    }

    fun setChildsToAllParents() {
        for (i in trilist.indices) {
            try {
                val pnForMe = trilist[i].parentnumber
                val me = trilist[i]
                if (pnForMe > -1) {
                    // 改善版
                    val parent = trilist[pnForMe - 1]
                    // 親に対して、
                    parent.setChild(me, me.connectionType)
                }
            } catch (e: NullPointerException) {
                println("NullPointerException!! trilistsize:" + trilist.size + " index:" + i)
            }
        }
    }

    fun insertAndSlide(nextTriangle: Triangle) {
        trilist.add(nextTriangle.mynumber - 1, nextTriangle)
        getByNumber(nextTriangle.parentnumber).setChild(nextTriangle, nextTriangle.connectionType)

        //次以降の三角形の親番号を全部書き換える、ただし連続しない親で、かつ自分より若い親の場合はそのままにする。
        rewriteAllNodeFrom(nextTriangle, +1)
        resetTriangles(nextTriangle.mynumber, nextTriangle)
    }

    //次以降の三角形の親番号を全部書き換える、ただし連続しない親で、かつ自分より若い親の場合はそのままにする。
    // この関数自体は、どの三角形も書き換えない。
    fun rewriteAllNodeFrom(target: Triangle, numberChange: Int) {
        for (i in target.mynumber until trilist.size) {
            val parent = trilist[i]
            if (parent.hasConstantParent() || parent.parentnumber > target.mynumber) {
                parent.parentnumber += numberChange
            }
            parent.mynumber += numberChange
        }
    }

    override fun remove(num: Int) {
        //number = lastTapNum_;

        //１番目以下は消せないことにする。
        if (num <= 1 || num > trilist.size ) return
        val i = num -1
        val target = trilist[i]
        target.nodeTriangleA_!!.removeNode(target) //removeTheirNode();
        trilist.removeAt(i)

        //ひとつ前の三角形を基準にして
        val parentTriangle = trilist[target.parentnumber -1]//trilist_[number - 2]
        //次以降の三角形の親番号を全部書き換える
        rewriteAllNodeFrom(parentTriangle, -1)
        resetTriangles(parentTriangle.mynumber, parentTriangle)
        selectedNumber = num - 1
        lastTapNumber_ = num - 1
        lastTapSide_ = -1

        //this.cloneByScale(basepoint, myScale);
    }

    fun rotateCurrentTriLCR(): ConnParam? {
        if (lastTapNumber_ < 2) return null
        val curTri = trilist[lastTapNumber_ - 1]
        val cParam = curTri.rotateLCRandGet().cParam_.clone()
        //if( lastTapNum_ < myTriList.size() ) myTriList.get(lastTapNum_).resetByParent( curTri, myTriList.get(lastTapNum_).cParam_ );

        // 次以降の三角形を書き換えている
        for (i in trilist.indices) {
            val nextTri = trilist[i]
            if (i > 0) {
                val npmi = nextTri.nodeTriangleA_!!.mynumber - 1
                if (npmi == curTri.mynumber - 1) nextTri.nodeTriangleA_ = curTri.clone()
                nextTri.resetByParent(trilist[npmi], nextTri.cParam_)
            }
        }
        //resetMyChild( myTriList.get(lastTapNum_-1), myTriList.get(lastTapNum_-1).cParam_ );
        return cParam
    }

    fun resetNodeByID(prms: Params) {
        val doneObjectList = ArrayList<Triangle>()
        trilist[prms.n - 1].resetNode(prms, trilist[prms.pn - 1], doneObjectList)
    }

    fun resetFromParam(prms: Params): Boolean {
        val ci = prms.n - 1
        val tri = trilist[ci]

        // 親番号が書き換わっている時は入れ替える。ただし現在のリストの範囲外の番号は除く。
        val pn = prms.pn
        if (pn != tri.parentnumber && pn < trilist.size && pn > 0) tri.setNode(
            trilist[pn - 1],
            0
        )
        tri.reset(prms)

        // 関係する三角形を書き換える
        resetTriangles(prms.n, tri)
        return true
    }

    fun resetTriangles(number: Int, curtri: Triangle?): Boolean {
        if (curtri == null) return false
        if (!curtri.isValid) return false

        //ノードの配列として定義した方がすっきりまとまる気がする。
        //親方向に走査するなら0、自身の接続方向がCなら２、二つの引数を渡して、総当たりに全方向走査する

        //trilistStored_ = (ArrayList<Triangle>) trilist_.clone();
        curtri.mynumber = number //useless?

        // 親がいるときは、子接続を書き換える
        if (curtri.parentnumber > 0 && number - 2 >= 0) {
            val parent = trilist[curtri.parentnumber - 1]
            parent.childSide_ = curtri.connectionType
            curtri.nodeTriangleA_ = parent //再リンクしないと位置と角度が連動しない。
            val curPareNum = trilist[number - 1].parentnumber
            // 自分の親番号と、親の三角形の番号が一致したとき？
            if ( curPareNum == parent.mynumber) { //trilist_.get(curPareNum-1)
                parent.resetByChild(curtri)
            }
        }

        // 自身の書き換え
        val me = trilist[number - 1]
        //me.setNode( trilist_.get( ));


        // 浮いてる場合、さらに自己番が最後でない場合、一個前の三角形の位置と角度を自分の変化���あわせて動かしたい。
        if (curtri.parentnumber <= 0 && trilist.size != number && notHave(curtri.nodeTriangleA_)) {
            curtri.resetByChild(trilist[number])
        }
        me.reset(curtri) // ここがへん！ 親は自身の基準角度に基づいて形状変更をするので、それにあわせてもう一回呼んでいる。


        // 子をすべて書き換える
        if (trilist.size > 1 && number < trilist.size && curtri.hasChild()) resetMyChild(trilist[number - 1])

        //lastTapNum_ = 0;
        //lastTapSide_ = -1;
        return true
    }

    //　ターゲットポインターがリストの中にいたらtrue
    fun notHave(target: Triangle?): Boolean {
        if( target == null ) return false

        for (i in trilist.indices) {
            if (trilist[i] === target) return true
        }
        return false
        //return trilist_.contains( target );
    }

    fun resetMyChild(newParent: Triangle?) {
        //myTriList.get(2).reset(newParent, 1);
        var parent = newParent ?: return

        // 新しい親の次から
        for (i in parent.mynumber until trilist.size) {
            val target = trilist[i]

            // 連番と派生接続で分岐。
            if (target.parentnumber == parent.mynumber) {
                // ひとつ前の三角形の子接続を書き換える？
                if (i + 1 < trilist.size) parent.childSide_ = trilist[i + 1].connectionType
                if (target.resetByParent(parent, target.connectionType)) return
                // 自身が次の親になる
                parent = target
            } else { //連番でないとき
                //親を番号で参照する
                if( target.parentnumber < 1 ) return
                val recent_parent = trilist[target.parentnumber - 1]

                if (target.resetByParent(recent_parent, target.connectionType)) return
                trilist[target.parentnumber - 1].childSide_ = target.connectionType
            }
            //else myTriList.get(i).reload();
        }
    }

    fun replace(number: Int, pnum: Int): Boolean {
        val me = trilist[number - 1]
        val pr = trilist[pnum - 1]
        me.setOn(pr, me.connectionType, me.length[0], me.length[1], me.length[2])
        return true
    }


    // オブジェクトポインタを返す。
    fun getByNumber(number: Int): Triangle {
        return if (number < 1 || number > trilist.size) Triangle() //under 0 is empty. cant hook null. can hook lenA is not 0.
        else trilist[number - 1]
    }

    fun getLastTriangle(): Triangle{
        if(lastTapNumber_<1) return Triangle()
        return trilist[lastTapNumber_-1]
    }

    fun getLastNumber(): Int{
        if(lastTapNumber_<1) return 1
        return lastTapNumber_
    }

    // by number start 1, not index start 0.
    override operator fun get(num: Int): Triangle {
        return getByNumber(num)
    }

    fun spritByColors(): ArrayList<TriangleList> {
        val listByColors = ArrayList<TriangleList>()
        listByColors.add(TriangleList()) //0 val lightColors_ = arrayOf(LightPink_, LightOrange_, LightYellow_, LightGreen_, LightBlue_)
        listByColors.add(TriangleList())
        listByColors.add(TriangleList())
        listByColors.add(TriangleList())
        listByColors.add(TriangleList()) //4
        for (colorindex in 4 downTo -1 + 1) {
            val listByColor = listByColors[colorindex]
            for (i in trilist.indices) {
                val t = trilist[i].clone()
                if (t.color_ == colorindex) listByColor.add(
                    t,
                    false
                ) // 番号変更なしで追加する deep or shallow
                t.isColored
            }
            if (listByColor.trilist.size > 0) listByColor.outlineList()
        }
        return listByColors
    }

    fun outlineList() {
        if (trilist.size < 1) return
        val olplists = outlineList_
        val sb = StringBuilder()
        for (i in trilist.indices) {
            sb.setLength(0)
            val t = trilist[i]
            if (i == 0 || t.isFloating || t.isColored_) {
                olplists!!.add(ArrayList())
                trace(olplists[olplists.size - 1], t, true)
                olplists[olplists.size - 1].add(trilist[i].pointAB) //今のindexでtriを取り直し
                outlineStr_ = sb.append(outlineStr_).append(trilist[i].mynumber).append("ab, ")
                    .toString() //outlineStr_ + ( trilist_.get( i ).myNumber_ + "ab, " );
            }
        }
    }

    fun trace(olp: ArrayList<PointXY>, tri: Triangle?, first: Boolean?): ArrayList<PointXY> {
        if (tri == null) return olp
        if ((tri.isFloating || tri.isColored) && !first!!) return olp
        addOlp(tri.pointAB, "ab,", olp, tri)
        trace(olp, tri.nodeTriangleB, false)
        addOlp(tri.pointBC, "bc,", olp, tri)
        trace(olp, tri.nodeTriangleC, false)
        addOlp(tri.point[0], "ca,", olp, tri)
        //trace( olp, tri.nodeTriangleA_, -1 ); //ネスト抜けながら勝手にトレースしていく筈
        return olp
    }

    fun addOlp(pt: PointXY, str: String, olp: ArrayList<PointXY>, node: Triangle) {
        if (notHave(pt, olp)) {
            olp.add(pt)
            outlineStr_ += node.mynumber.toString() + str
        }
    }

    // 同じポイントは二ついらない
    private fun notHave(it: PointXY, inthis: ArrayList<PointXY>): Boolean {
        //if( inthis.size() < 1 ) return false;
        for (i in inthis.indices) if (it.nearBy(inthis[i], 0.001f)) return false
        return true
    }

    fun traceOrNot(node: Triangle?, origin: Int, olp: ArrayList<PointXY>) {
        if (node != null && !node.isFloating && !node.isColored) traceOrJumpForward(
            node.mynumber - 1,
            origin,
            olp,
            node
        )
    }

    fun branchOrNot(t: Triangle, origin: Int, olp: ArrayList<PointXY>) {
        if (t.nodeTriangleB != null && t.nodeTriangleC != null) if (notHave(
                t.nodeTriangleC!!.point[0],
                olp
            ) && !t.nodeTriangleC!!.isFloating_ && !t.nodeTriangleC!!.isColored_
        ) traceOrJumpForward(t.nodeTriangleC!!.mynumber - 1, origin, olp, t.nodeTriangleC!!)
    }

    fun isCollide(tapP: PointXY): Int {
        for (i in trilist.indices) {
            if (trilist[i].isCollide(tapP)) return i + 1.also { lastTapCollideNum_ = it }
        }
        return 0
    }

    fun dedmapping(dedlist: DeductionList, axisY: Int) {
        // トライアングルリストのdedcountをリセット
        trilist.forEach { it.dedcount = 0f }

        // Deductionオブジェクトごとに処理
        dedlist.dedlist_.forEachIndexed { index, deduction ->
            processDeduction(deduction, axisY)
        }
    }

    private fun processDeduction(deduction: Deduction, axisY: Int) {
        trilist.forEach { tri ->
            // スケールされた点での衝突判定
            val isCol = tri.isCollide(deduction.point.scale(PointXY(1f, axisY.toFloat())))
            if (isCol) {
                tri.dedcount++
                deduction.overlap_to = tri.mynumber
                deduction.shapeAngle = tri.angleUnconnectedSide()
            }
        }
    }


    fun getTapIndexArray(tapP: PointXY): IntArray {
        val tapIndexArray = IntArray(trilist.size)
        for (i in trilist.indices) {
            tapIndexArray[i] = trilist[i].getTapLength(tapP, 0.6f)
        }
        return tapIndexArray
    }

    fun getTapHitCount(tapP: PointXY): Int {
        var hitC = 0
        for (i in trilist.indices) {
            if (trilist[i].getTapLength(tapP, 0.6f) != -1) hitC++
        }
        return hitC
    }

    fun getTap(tapP: PointXY, rangeRadius: Float): Int {
        val ltn = lastTapNumber_ + lastTapSide_
        isCollide(tapP)
        for (i in trilist.indices) {
            if (trilist[i].getTapLength(tapP, rangeRadius) != -1) {
                lastTapNumber_ = i + 1
                lastTapSide_ = trilist[i].getTapLength(tapP, rangeRadius)

                //if( i > 0 && lastTapSide_ == 0 ){
                //                        lastTapNumber_ = i;
                //                      lastTapSide_ = trilist_.get(i-1).getTapLength(tapP);
                //}
                isDoubleTap_ = ltn == lastTapSide_ + lastTapNumber_
            }
        }
        if (getTapHitCount(tapP) == 0) {
            lastTapNumber_ = 0
            isDoubleTap_ = false
        }
        //Log.d("TriangleList", "Tap Triangle num: " + lastTapNumber_ + ", side:" + lastTapSide_ );
        return lastTapNumber_ * 10 + lastTapSide_
    }

    fun move(to: PointXY): ArrayList<Triangle> {
        for (i in trilist.indices) {
            trilist[i].move(to)
        }
        basepoint.add(to)
        return trilist
    }

    override fun getParams(num: Int): Params {
        if (num > trilist.size) return Params(
            "",
            "",
            0,
            0f,
            0f,
            0f,
            0,
            0,
            PointXY(0f, 0f),
            PointXY(0f, 0f)
        )
        val t = trilist[num - 1]
        return Params(
            t.myName_(),
            "",
            t.mynumber,
            t.lengthA,
            t.lengthB,
            t.lengthC,
            t.parentnumber,
            t.connectionType,
            t.pointCenter_(),
            t.pointnumber
        )
    }

    override fun getArea(): Float {
        var area = 0f
        for (i in trilist.indices) {
            area += trilist[i].getArea()
        }
        return (Math.round(area * 100.0) * 0.01).toFloat()
    }

    fun addOutlinePoint(pt: PointXY, str: String, olp: ArrayList<PointXY>, node: Triangle?) {
        if (node != null && notHave(pt, olp)) {
            olp.add(pt)
            outlineStr_ += node.mynumber.toString() + str
        }
    }

    fun traceOrJumpForward(
        startindex: Int,
        origin: Int,
        olp: ArrayList<PointXY>,
        node: Triangle
    ): ArrayList<PointXY>? {
        if (startindex < 0 || startindex >= trilist.size) return null

        // AB点を取る。すでにあったらキャンセル
        addOutlinePoint(node.pointAB, "ab,", olp, node)

        // 再起呼び出しで派生方向に右手伝いにのびていく
        traceOrNot(node.nodeTriangleB, origin, olp)

        // BC点を取る。すでにあったらキャンセル
        addOutlinePoint(node.pointBC, "bc,", olp, node)

        // 再起呼び出しで派生方向に右手伝いにのびていく
        traceOrNot(node.nodeTriangleC, origin, olp)

        // 折り返し
        traceOrJumpBackward(origin, olp, node)
        return olp
    }

    fun traceOrJumpBackward(origin: Int, olp: ArrayList<PointXY>, node: Triangle) {

        // 派生（ふたつとも接続）していたらそっちに伸びる、フロート接続だったり、すでに持っている点を見つけたらスルー
        branchOrNot(node, origin, olp)

        //BC点を取る。すでにあったらキャンセル
        addOutlinePoint(node.pointBC, "bc,", olp, node)

        //CA点を取る。すでにあったらキャンセル
        addOutlinePoint(node.point[0], "ca,", olp, node)

        //AB点を取る。すでにあったらキャンセル
        addOutlinePoint(node.pointAB, "ab,", olp, node)

        //AB点を取る。ダブりを許容
        //if( t.nodeTriangleA_ == null || t.isColored_ || t.isFloating_ ){
        //  olp.add( t.pointAB_ );
        //outlineStr_ += t.myNumber_ + "ab, ";
        //}

        // 0まで戻る。同じ色でない時はリターン
        if (node.mynumber <= node.parentnumber) return
        if (node.nodeTriangleA_ != null && !node.isColored && !node.isFloating) traceOrJumpBackward(
            origin,
            olp,
            node.nodeTriangleA_!!
        )
    }

    fun resetNumReverse(): TriangleList {
        val rev = clone()
        var iBackward = trilist.size
        //マイナンバーの書き換え
        for (i in trilist.indices) {
            rev.trilist[i].mynumber = iBackward
            iBackward--
        }
        return rev
    }

    override fun reverse(): TriangleList {
        for (i in trilist.indices) {
            //終端でなければ
            trilist.size // 連番でない時は
            //return this;//akirameru
        }

        // 番号だけを全部逆順に書き換え
        val numrev = resetNumReverse()
        for (i in trilist.indices) {
            val me = numrev.trilist[i]

            //接続情報の書き換え
            //終端でなければ
            if (i + 1 < trilist.size) {
                val next = numrev.trilist[i + 1]

                // 連番の時は
                if (i + 1 == next.parentnumber) {
                    // 自身の番号-1を親とする
                    me.parentnumber = me.mynumber - 1
                    me.rotateLengthBy(next.parentSide)
                    me.setNode(next, 0)

                    //たとえば次がB辺接続だった場合、自分のB辺がA辺となるように全体の辺長配置を時計回りに回す。
                    //それから、次の接続辺指定を自身に���植する。
                    //このとき、二重断面指定はたぶん逆になる。
                    //次がB辺接続だったとしても、そのまた次もB辺接続だったら、次のやつの辺長が配置換えされるので、接続がおかしくなる
                    //次のやつのB辺はC辺にあった。
                    //同じ接続方向が続いている時は、PBCをCtoB、BtoCに反転させる。
                    if (i + 2 < trilist.size) {
                        val nextnext = numrev.trilist[i + 2]
                        if (next.parentSide == nextnext.parentSide) me.setReverseDefSide(
                            next.connectionType,
                            true
                        ) else if (i + 2 != nextnext.parentnumber && next.connectionType != 1) me.setReverseDefSide(
                            -next.connectionType + 3,
                            false
                        ) else me.setReverseDefSide(next.connectionType, false)
                    } else me.setReverseDefSide(next.connectionType, false)
                    next.setNode(me, me.parentSide)
                } else {
                    //連番でないときは
                    me.parentnumber = -1 //next.parentNumber_;
                    me.connectionType = -1
                    me.rotateLengthBy(2)
                    me.setNode(me.nodeTriangleA_, me.parentSide)

                    //next.parentNumber_ = trilist_.get( next.parentNumber_ - 1 ).myNumber_;
                    //next.parentBC_ =  trilist_.get( next.parentNumber_ - 1 ).parentBC_;
                }
            } else if (i + 1 == trilist.size) {
                // 終端の時、つまり一番最初のやつ。
                me.parentnumber = 0
                // 終端は、自身の接続辺をもとにいっこまえの接続が決まっているので、それに合わせて辺長を回す。
                // 最初の三角形になるので、接続情報は要らない。
                me.rotateLengthBy(-me.parentSide + 3)
            }
        }

        // 逆順にソートし、新規に足していく。リビルドした方が考え方が楽。
        val rev = clone()
        rev.trilist.clear()
        for (i in trilist.size - 1 downTo -1 + 1) {
            val it = numrev.trilist[i]

            // 自身の番号よりも親番号が大きい場合を許容する
            // 子の三角形を指定して生成する。
            //rev.add( new Triangle( trilist_.get( it.parentNumber_), it.parentBC_, it.lengthAforce_, it.lengthBforce_, it.lengthCforce_ ) );
            if (it.parentnumber < 1) rev.add(
                it.clone(),
                true
            ) //new Triangle( it.lengthAforce_, it.lengthBforce_, it.lengthCforce_, it.point[0], it.angleInGlobal_)
            else rev.add(
                Triangle(
                    rev[it.parentnumber],
                    it.connectionType,
                    it.lengthAforce_,
                    it.lengthBforce_,
                    it.lengthCforce_
                ), true
            )
        }

        // 名前の書き換え、反転後のひとつ先の三角形に移設し、1番の名前は消去
        for (i in trilist.indices) {
            val it = numrev.trilist[trilist.size - i - 1]
            if (i + 2 <= trilist.size) rev[i + 2].name = it.name
        }
        rev[1].name = ""
        return rev
    }

    fun numbered(start: Int): TriangleList {
        val rev = TriangleList()
        for (i in trilist.indices) {
            val ti = trilist[i].clone()
            ti.mynumber = start + i
            rev.trilist.add(ti)
        }
        return rev
    }
} // end of class
