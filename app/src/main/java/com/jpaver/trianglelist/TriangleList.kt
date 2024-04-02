package com.jpaver.trianglelist

import com.jpaver.trianglelist.util.Params
import java.util.Optional

class TriangleList : EditList {


　　　　　fun rotate(basepoint: PointXY, angle: Float, startnumber: Int, separationFreeMode: Boolean = false, is_recover: Boolean = false ) {

        var startindex = startnumber - 1

        // 0番からすべて回転させる場合
        if (!(startnumber > 1 && trilist_[startindex].parentBC >= 9) || !separationFreeMode) {
            this.angle += angle
            this.basepoint = basepoint.clone()
            startindex = 0
        }
        // 開始インデックス以降の要素に対してのみ処理を行う
        trilist_.drop(startindex).forEach {
            it.rotate( basepoint, angle, is_recover )
            it.pointnumber = it.pointnumber.rotate( basepoint, -angle )
        }
    }

    var trilist_: ArrayList<Triangle>
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

    override fun toString(): String {
        return trilist_.joinToString(separator = "") { it.toString() }
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
            for (i in trilist_.indices) {
                b.trilist_.add(trilist_[i].clone())
                //b.myTriListAtView.add(this.myTriListAtView.get(i).clone());
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        //ノードポインターのリコネクト
        if (trilist_.size > 0) b.resetAllNodes()
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
            val s = allSokTriList[i].myName_
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
            for (i in trilist_.indices) {
                if (trilist_[i].myName_.contains("No.")) {
                    numTriList.add(trilist_[i].clone())
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
                val sf = allSokutenList[0].myName_
                val fnInt = sf.replace("[^0-9]".toRegex(), "").toInt()
                val ss = allSokutenList[1].myName_
                val snInt = ss.replace("[^0-9]".toRegex(), "").toInt()
                return snInt - fnInt
            }
            return 0
        }

    fun getPrintTextScale(drawingScale: Float, exportFileType: String): Float {
        val textScaleMapPDF: MutableMap<Float, Float> = HashMap()
        textScaleMapPDF[45f] = 3f
        textScaleMapPDF[40f] = 5f
        textScaleMapPDF[25f] = 6f
        textScaleMapPDF[20f] = 8f
        textScaleMapPDF[15f] = 8f
        val textScaleMapDXF: MutableMap<Float, Float> = HashMap()
        textScaleMapDXF[15f] = 0.35f
        textScaleMapDXF[5f] = 0.25f
        val exportFileTypeMap: MutableMap<String, Map<Float, Float>> = HashMap()
        exportFileTypeMap["dxf"] = textScaleMapDXF
        exportFileTypeMap["sfc"] = textScaleMapDXF
        exportFileTypeMap["pdf"] = textScaleMapPDF
        val defaultTextScaleMap: MutableMap<String, Float> = HashMap()
        defaultTextScaleMap["dxf"] = 0.45f
        defaultTextScaleMap["sfc"] = 0.45f
        defaultTextScaleMap["pdf"] = 10f
        val defaultValue = Optional.ofNullable(defaultTextScaleMap[exportFileType]).orElse(10f)
        val selectedMap = Optional.ofNullable(
            exportFileTypeMap[exportFileType]
        ).orElse(textScaleMapDXF)
        return Optional.ofNullable(selectedMap[getPrintScale(drawingScale) * 10])
            .orElse(defaultValue)
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
        for (i in trilist_.indices) {
            myBounds = trilist_[i].expandBoundaries(myBounds)
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
        trilist_ = ArrayList()
        trilistStored_ = ArrayList()
        myCollisionList = ArrayList()
        outlineList_ = ArrayList()
        selectedNumber = trilist_.size
    }

    internal constructor(myFirstTriangle: Triangle) {
        trilist_ = ArrayList()
        trilistStored_ = ArrayList()
        trilist_.add(myFirstTriangle)
        myFirstTriangle.setNumber(1)
        selectedNumber = trilist_.size
    }

    fun setScale(bp: PointXY, sc: Float) {
        scale = sc
        basepoint = bp.clone()
        //        this.cloneByScale(basepoint, myScale);
    }

    /*public void cloneByScale(PointXY basepoint, float scale){
        myTriListAtView.clear();
        for (int i = 0; i < trilist_.size(); i++ ) {
            myTriListAtView.add(trilist_.get(i).clone());
            myTriListAtView.get(i).scale(basepoint, scale);
        }
    }*/
    fun scale(basepoint: PointXY?, scale: Float) {
        this.scale *= scale
        for (i in trilist_.indices) {
            trilist_[i].scale(basepoint, scale)
        }
    }

    fun scaleAndSetPath(basepoint: PointXY?, scale: Float, ts: Float) {
        this.scale *= scale
        for (i in trilist_.indices) {
            trilist_[i].scale(basepoint, scale)
            trilist_[i].setDimPath(ts)
        }
    }

    fun setDimPathTextSize(ts: Float) {

        for (i in trilist_.indices) {
            trilist_[i].setDimPath(ts)
        }
    }

    

    fun setDimsUnconnectedSideToOuter(target: Triangle?) {
        if(target == null ) return
        if (target.nodeTriangleA_ == null) target.myDimAlignA_ = 1 else target.myDimAlignA_ = 3
        if (target.nodeTriangleB_ == null) target.myDimAlignB_ = 1 else if (target.nodeTriangleB_!!.parentBC > 2 ) target.myDimAlignB_ = 3
        if (target.nodeTriangleC_ == null) target.myDimAlignC_ = 1 else if (target.nodeTriangleC_!!.parentBC > 2 ) target.myDimAlignC_ = 3
    }

    fun recoverState(bp: PointXY) {
        basepoint = bp.clone()
        for (i in trilist_.indices) {
            trilist_[i].rotate(basepoint, angle - 180, false)
            if (trilist_[i].isPointNumberMoved == false) trilist_[i].pointnumber =
                trilist_[i].pointnumber.rotate(basepoint, angle - 180)
        }
    }

    override fun size(): Int {
        return trilist_.size
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
        if (numbering) nextTriangle.myNumber = trilist_.size + 1
        val pbc = nextTriangle.parentBC
        if (nextTriangle.parentNumber > 0) {
            val parent = getMemberByIndex(nextTriangle.parentNumber)
            if (parent.alreadyHaveChild(pbc)) {
                // すでに親の接続辺上に子供がいたら、挿入処理
                //nextTriangle.myNumber_ = nextTriangle.parentNumber_ +1;
                insertAndSlide(nextTriangle)
            } else {
                trilist_.add(nextTriangle) // add by arraylist
            }

            // 親に告知する
            //if( nextTriangle.myNumber_ > 1 ) trilist_.get( nextTriangle.parentNumber_ -1 ).setChild(nextTriangle, nextTriangle.getParentBC());
        } else {
            trilist_.add(nextTriangle) // add by arraylist
        }

        setDimsUnconnectedSideToOuter(nextTriangle)
        setDimsUnconnectedSideToOuter(nextTriangle.nodeTriangleA_)

        selectedNumber = nextTriangle.myNumber

        return true
    }

    fun resetAllNodes() {
        for (i in trilist_.indices) {
            val tri = trilist_[i]
            if (tri.nodeTriangleA_ != null) {
                tri.setNode(trilist_[tri.nodeTriangleA_!!.myNumber - 1], 0)
            }
            if (tri.nodeTriangleB_ != null) {
                tri.setNode(trilist_[tri.nodeTriangleB_!!.myNumber - 1], 1)
            }
            if (tri.nodeTriangleC_ != null) {
                tri.setNode(trilist_[tri.nodeTriangleC_!!.myNumber - 1], 2)
            }
        }
    }

    fun setChildsToAllParents() {
        for (i in trilist_.indices) {
            try {
                val pnForMe = trilist_[i].parentNumber
                val me = trilist_[i]
                if (pnForMe > -1) {
                    // 改善版
                    val parent = trilist_[pnForMe - 1]
                    // 親に対して、
                    parent.setChild(me, me.parentBC)
                }
            } catch (e: NullPointerException) {
                println("NullPointerException!! trilistsize:" + trilist_.size + " index:" + i)
            }
        }
    }

    fun insertAndSlide(nextTriangle: Triangle) {
        trilist_.add(nextTriangle.myNumber - 1, nextTriangle)
        getMemberByIndex(nextTriangle.parentNumber).setChild(nextTriangle, nextTriangle.parentBC)

        //次以降の三角形の親番号を全部書き換える、ただし連続しない親で、かつ自分より若い親の場合はそのままにする。
        rewriteAllNodeFrom(nextTriangle, +1)
        resetTriangles(nextTriangle.myNumber, nextTriangle)
    }

    //次以降の三角形の親番号を全部書き換える、ただし連続しない親で、かつ自分より若い親の場合はそのままにする。
    // この関数自体は、どの三角形も書き換えない。
    fun rewriteAllNodeFrom(target: Triangle, numberChange: Int) {
        for (i in target.myNumber until trilist_.size) {
            val parent = trilist_[i]
            if (parent.hasConstantParent() || parent.parentNumber > target.myNumber) {
                parent.parentNumber += numberChange
            }
            parent.myNumber += numberChange
        }
    }

    override fun remove(num: Int) {
        //number = lastTapNum_;

        //１番目以下は消せないことにする。
        if (num <= 1 || num > trilist_.size ) return
        val i = num -1
        val target = trilist_[i]
        target.nodeTriangleA_!!.removeNode(target) //removeTheirNode();
        trilist_.removeAt(i)

        //ひとつ前の三角形を基準にして
        val parentTriangle = trilist_[target.parentNumber -1]//trilist_[number - 2]
        //次以降の三角形の親番号を全部書き換える
        rewriteAllNodeFrom(parentTriangle, -1)
        resetTriangles(parentTriangle.myNumber, parentTriangle)
        selectedNumber = num - 1
        lastTapNumber_ = num - 1
        lastTapSide_ = -1

        //this.cloneByScale(basepoint, myScale);
    }

    fun rotateCurrentTriLCR(): ConnParam? {
        if (lastTapNumber_ < 2) return null
        val curTri = trilist_[lastTapNumber_ - 1]
        val cParam = curTri.rotateLCRandGet().cParam_.clone()
        //if( lastTapNum_ < myTriList.size() ) myTriList.get(lastTapNum_).resetByParent( curTri, myTriList.get(lastTapNum_).cParam_ );

        // 次以降の三角形を書き換えている
        for (i in trilist_.indices) {
            val nextTri = trilist_[i]
            if (i > 0) {
                val npmi = nextTri.nodeTriangleA_!!.myNumber - 1
                if (npmi == curTri.myNumber - 1) nextTri.nodeTriangleA_ = curTri.clone()
                nextTri.resetByParent(trilist_[npmi], nextTri.cParam_)
            }
        }
        //resetMyChild( myTriList.get(lastTapNum_-1), myTriList.get(lastTapNum_-1).cParam_ );
        return cParam
    }

    fun resetNodeByID(prms: Params) {
        val doneObjectList = ArrayList<Triangle>()
        trilist_[prms.n - 1].resetNode(prms, trilist_[prms.pn - 1], doneObjectList)
    }

    fun resetFromParam(prms: Params): Boolean {
        val ci = prms.n - 1
        val tri = trilist_[ci]

        // 親番号が書き換わっている時は入れ替える。ただし現在のリストの範囲外の番号は除く。
        val pn = prms.pn
        if (pn != tri.parentNumber && pn < trilist_.size && pn > 0) tri.setNode(
            trilist_[pn - 1],
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
        curtri.myNumber = number //useless?

        // 親がいるときは、子接続を書き換える
        if (curtri.parentNumber > 0 && number - 2 >= 0) {
            val parent = trilist_[curtri.parentNumber - 1]
            parent.childSide_ = curtri.parentBC
            curtri.nodeTriangleA_ = parent //再リンクしないと位置と角度が連動しない。
            val curPareNum = trilist_[number - 1].parentNumber
            // 自分の親番号と、親の三角形の番号が一致したとき？
            if ( curPareNum == parent.myNumber) { //trilist_.get(curPareNum-1)
                parent.resetByChild(curtri)
            }
        }

        // 自身の書き換え
        val me = trilist_[number - 1]
        //me.setNode( trilist_.get( ));


        // 浮いてる場合、さらに自己番が最後でない場合、一個前の三角形の位置と角度を自分の変化にあわせて動かしたい。
        if (curtri.parentNumber <= 0 && trilist_.size != number && notHave(curtri.nodeTriangleA_)) {
            curtri.resetByChild(trilist_[number])
        }
        me.reset(curtri) // ここがへん！ 親は自身の基準角度に基づいて形状変更をするので、それにあわせてもう一回呼んでいる。


        // 子をすべて書き換える
        if (trilist_.size > 1 && number < trilist_.size && curtri.hasChild()) resetMyChild(trilist_[number - 1])

        //lastTapNum_ = 0;
        //lastTapSide_ = -1;
        return true
    }

    //　ターゲットポインターがリストの中にいたらtrue
    fun notHave(target: Triangle?): Boolean {
        if( target == null ) return false

        for (i in trilist_.indices) {
            if (trilist_[i] === target) return true
        }
        return false
        //return trilist_.contains( target );
    }

    fun resetMyChild(newParent: Triangle?) {
        //myTriList.get(2).reset(newParent, 1);
        var parent = newParent ?: return

        // 新しい親の次から
        for (i in parent.myNumber until trilist_.size) {
            val target = trilist_[i]

            // 連番と派生接続で分岐。
            if (target.parentNumber == parent.myNumber) {
                // ひとつ前の三角形の子接続を書き換える？
                if (i + 1 < trilist_.size) parent.childSide_ = trilist_[i + 1].parentBC
                if (target.resetByParent(parent, target.parentBC)) return
                // 自身が次の親になる
                parent = target
            } else { //連番でないとき
                //親を番号で参照する
                if( target.parentNumber < 1 ) return
                val recent_parent = trilist_[target.parentNumber - 1]

                if (target.resetByParent(recent_parent, target.parentBC)) return
                trilist_[target.parentNumber - 1].childSide_ = target.parentBC
            }
            //else myTriList.get(i).reload();
        }
    }

    fun replace(number: Int, pnum: Int): Boolean {
        val me = trilist_[number - 1]
        val pr = trilist_[pnum - 1]
        me.setOn(pr, me.parentBC, me.length[0], me.length[1], me.length[2])
        return true
    }


    // オブジェクトポインタを返す。
    fun getMemberByIndex(number: Int): Triangle {
        return if (number < 1 || number > trilist_.size) Triangle() //under 0 is empty. cant hook null. can hook lenA is not 0.
        else trilist_[number - 1]
    }

    // by number start 1, not index start 0.
    override operator fun get(num: Int): Triangle {
        return getMemberByIndex(num)
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
            for (i in trilist_.indices) {
                val t = trilist_[i].clone()
                if (t.color_ == colorindex) listByColor.add(
                    t,
                    false
                ) // 番号変更なしで追加する deep or shallow
                t.isColored
            }
            if (listByColor.trilist_.size > 0) listByColor.outlineList()
        }
        return listByColors
    }

    fun outlineList() {
        if (trilist_.size < 1) return
        val olplists = outlineList_
        val sb = StringBuilder()
        for (i in trilist_.indices) {
            sb.setLength(0)
            val t = trilist_[i]
            if (i == 0 || t.isFloating || t.isColored_) {
                olplists!!.add(ArrayList())
                trace(olplists[olplists.size - 1], t, true)
                olplists[olplists.size - 1].add(trilist_[i].pointAB) //今のindexでtriを取り直し
                outlineStr_ = sb.append(outlineStr_).append(trilist_[i].myNumber).append("ab, ")
                    .toString() //outlineStr_ + ( trilist_.get( i ).myNumber_ + "ab, " );
            }
        }
    }

    fun trace(olp: ArrayList<PointXY>, tri: Triangle?, first: Boolean?): ArrayList<PointXY> {
        if (tri == null) return olp
        if ((tri.isFloating || tri.isColored) && !first!!) return olp
        addOlp(tri.pointAB, "ab,", olp, tri)
        trace(olp, tri.nodeTriangleB_, false)
        addOlp(tri.pointBC, "bc,", olp, tri)
        trace(olp, tri.nodeTriangleC_, false)
        addOlp(tri.point[0], "ca,", olp, tri)
        //trace( olp, tri.nodeTriangleA_, -1 ); //ネスト抜けながら勝手にトレースしていく筈
        return olp
    }

    fun addOlp(pt: PointXY, str: String, olp: ArrayList<PointXY>, node: Triangle) {
        if (notHave(pt, olp)) {
            olp.add(pt)
            outlineStr_ += node.myNumber.toString() + str
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
            node.myNumber - 1,
            origin,
            olp,
            node
        )
    }

    fun branchOrNot(t: Triangle, origin: Int, olp: ArrayList<PointXY>) {
        if (t.nodeTriangleB_ != null && t.nodeTriangleC_ != null) if (notHave(
                t.nodeTriangleC_!!.point[0],
                olp
            ) && !t.nodeTriangleC_!!.isFloating_ && !t.nodeTriangleC_!!.isColored_
        ) traceOrJumpForward(t.nodeTriangleC_!!.myNumber - 1, origin, olp, t.nodeTriangleC_!!)
    }

    fun isCollide(tapP: PointXY): Int {
        for (i in trilist_.indices) {
            if (trilist_[i].isCollide(tapP)) return i + 1.also { lastTapCollideNum_ = it }
        }
        return 0
    }

    fun dedmapping(dedlist: DeductionList, axisY: Int) {
        for (i in trilist_.indices) trilist_[i].dedcount = 0f
        for (i in 0 until dedlist.size()) {
            for (ii in trilist_.indices) {
                val isCol =
                    trilist_[ii].isCollide(dedlist[i + 1].point.scale(PointXY(1f, axisY.toFloat())))
                if (isCol) {
                    trilist_[ii].dedcount++
                    dedlist[i + 1].overlap_to = trilist_[ii].myNumber
                }
            }
        }
    }

    fun getTapIndexArray(tapP: PointXY): IntArray {
        val tapIndexArray = IntArray(trilist_.size)
        for (i in trilist_.indices) {
            tapIndexArray[i] = trilist_[i].getTapLength(tapP, 0.6f)
        }
        return tapIndexArray
    }

    fun getTapHitCount(tapP: PointXY): Int {
        var hitC = 0
        for (i in trilist_.indices) {
            if (trilist_[i].getTapLength(tapP, 0.6f) != -1) hitC++
        }
        return hitC
    }

    fun getTap(tapP: PointXY, rangeRadius: Float): Int {
        val ltn = lastTapNumber_ + lastTapSide_
        isCollide(tapP)
        for (i in trilist_.indices) {
            if (trilist_[i].getTapLength(tapP, rangeRadius) != -1) {
                lastTapNumber_ = i + 1
                lastTapSide_ = trilist_[i].getTapLength(tapP, rangeRadius)

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
        for (i in trilist_.indices) {
            trilist_[i].move(to)
        }
        basepoint.add(to)
        return trilist_
    }

    override fun getParams(num: Int): Params {
        if (num > trilist_.size) return Params(
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
        val t = trilist_[num - 1]
        return Params(
            t.myName_(),
            "",
            t.myNumber,
            t.lengthA,
            t.lengthB,
            t.lengthC,
            t.parentNumber,
            t.parentBC,
            t.pointCenter_(),
            t.pointnumber
        )
    }

    override fun getArea(): Float {
        var area = 0f
        for (i in trilist_.indices) {
            area += trilist_[i].getArea()
        }
        return (Math.round(area * 100.0) * 0.01).toFloat()
    }

    fun addOutlinePoint(pt: PointXY, str: String, olp: ArrayList<PointXY>, node: Triangle?) {
        if (node != null && notHave(pt, olp)) {
            olp.add(pt)
            outlineStr_ += node.myNumber.toString() + str
        }
    }

    fun traceOrJumpForward(
        startindex: Int,
        origin: Int,
        olp: ArrayList<PointXY>,
        node: Triangle
    ): ArrayList<PointXY>? {
        if (startindex < 0 || startindex >= trilist_.size) return null

        // AB点を取る。すでにあったらキャンセル
        addOutlinePoint(node.pointAB, "ab,", olp, node)

        // 再起呼び出しで派生方向に右手伝いにのびていく
        traceOrNot(node.nodeTriangleB_, origin, olp)

        // BC点を取る。すでにあったらキャンセル
        addOutlinePoint(node.pointBC, "bc,", olp, node)

        // 再起呼び出しで派生方向に右手伝いにのびていく
        traceOrNot(node.nodeTriangleC_, origin, olp)

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
        if (node.myNumber <= node.parentNumber) return
        if (node.nodeTriangleA_ != null && !node.isColored && !node.isFloating) traceOrJumpBackward(
            origin,
            olp,
            node.nodeTriangleA_!!
        )
    }

    fun resetNumReverse(): TriangleList {
        val rev = clone()
        var iBackward = trilist_.size
        //マイナンバーの書き換え
        for (i in trilist_.indices) {
            rev.trilist_[i].myNumber = iBackward
            iBackward--
        }
        return rev
    }

    override fun reverse(): TriangleList {
        for (i in trilist_.indices) {
            //終端でなければ
            trilist_.size // 連番でない時は
            //return this;//akirameru
        }

        // 番号だけを全部逆順に書き換え
        val numrev = resetNumReverse()
        for (i in trilist_.indices) {
            val me = numrev.trilist_[i]

            //接続情報の書き換え
            //終端でなければ
            if (i + 1 < trilist_.size) {
                val next = numrev.trilist_[i + 1]

                // 連番の時は
                if (i + 1 == next.parentNumber) {
                    // 自身の番号-1を親とする
                    me.parentNumber = me.myNumber - 1
                    me.rotateLengthBy(next.parentSide)
                    me.setNode(next, 0)

                    //たとえば次がB辺接続だった場合、自分のB辺がA辺となるように全体の辺長配置を時計回りに回す。
                    //それから、次の接続辺指定を自身に移植する。
                    //このとき、二重断面指定はたぶん逆になる。
                    //次がB辺接続だったとしても、そのまた次もB辺接続だったら、次のやつの辺長が配置換えされるので、接続がおかしくなる
                    //次のやつのB辺はC辺にあった。
                    //同じ接続方向が続いている時は、PBCをCtoB、BtoCに反転させる。
                    if (i + 2 < trilist_.size) {
                        val nextnext = numrev.trilist_[i + 2]
                        if (next.parentSide == nextnext.parentSide) me.setReverseDefSide(
                            next.parentBC,
                            true
                        ) else if (i + 2 != nextnext.parentNumber && next.parentBC != 1) me.setReverseDefSide(
                            -next.parentBC + 3,
                            false
                        ) else me.setReverseDefSide(next.parentBC, false)
                    } else me.setReverseDefSide(next.parentBC, false)
                    next.setNode(me, me.parentSide)
                } else {
                    //連番でないときは
                    me.parentNumber = -1 //next.parentNumber_;
                    me.parentBC = -1
                    me.rotateLengthBy(2)
                    me.setNode(me.nodeTriangleA_, me.parentSide)

                    //next.parentNumber_ = trilist_.get( next.parentNumber_ - 1 ).myNumber_;
                    //next.parentBC_ =  trilist_.get( next.parentNumber_ - 1 ).parentBC_;
                }
            } else if (i + 1 == trilist_.size) {
                // 終端の時、つまり一番最初のやつ。
                me.parentNumber = 0
                // 終端は、自身の接続辺をもとにいっこまえの接続が決まっているので、それに合わせて辺長を回す。
                // 最初の三角形になるので、接続情報は要らない。
                me.rotateLengthBy(-me.parentSide + 3)
            }
        }

        // 逆順にソートし、新規に足していく。リビルドした方が考え方が楽。
        val rev = clone()
        rev.trilist_.clear()
        for (i in trilist_.size - 1 downTo -1 + 1) {
            val it = numrev.trilist_[i]

            // 自身の番号よりも親番号が大きい場合を許容する
            // 子の三角形を指定して生成する。
            //rev.add( new Triangle( trilist_.get( it.parentNumber_), it.parentBC_, it.lengthAforce_, it.lengthBforce_, it.lengthCforce_ ) );
            if (it.parentNumber < 1) rev.add(
                it.clone(),
                true
            ) //new Triangle( it.lengthAforce_, it.lengthBforce_, it.lengthCforce_, it.point[0], it.angleInGlobal_)
            else rev.add(
                Triangle(
                    rev[it.parentNumber],
                    it.parentBC,
                    it.lengthAforce_,
                    it.lengthBforce_,
                    it.lengthCforce_
                ), true
            )
        }

        // 名前の書き換え、反転後のひとつ先の三角形に移設し、1番の名前は消去
        for (i in trilist_.indices) {
            val it = numrev.trilist_[trilist_.size - i - 1]
            if (i + 2 <= trilist_.size) rev[i + 2].myName_ = it.myName_
        }
        rev[1].myName_ = ""
        return rev
    }

    fun numbered(start: Int): TriangleList {
        val rev = TriangleList()
        for (i in trilist_.indices) {
            val ti = trilist_[i].clone()
            ti.myNumber = start + i
            rev.trilist_.add(ti)
        }
        return rev
    }
} // end of class
