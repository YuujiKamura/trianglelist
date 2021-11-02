package com.jpaver.trianglelist

import kotlin.collections.ArrayList

class TriangleListK() :Cloneable {



    constructor( tri1: TriangleK ) : this() {
        //trilist_ = ArrayList()
        //trilistStored_ = ArrayList()
        trilist_.add( tri1 )
        tri1.setNumber(1)
        //current = trilist_.size

    }

    val triarray :Array<TriangleK?> = arrayOfNulls(0)

    var trilist_: MutableList<TriangleK?> //= MutableList()//ArrayList<TriangleK?> = arrayListOf()
    var trilistStored_: ArrayList<TriangleK>? = null
    var myCollisionList: ArrayList<Collision>? = null


    var lastTapNum_ = 0
    var lastTapSide_ = -1
    var lastTapCollideNum_ = 0

    var current = 0
    var myScale = 1f

    var myAngle = 0f
    var basepoint = PointXY(0f, 0f)

    var myBounds = Bounds(0f, 0f, 0f, 0f)
    var myCenter = PointXY(0f, 0f)
    var myLength = PointXY(0f, 0f)

    var outlineStr_ = ""

    var isDoubleTap_ = false

    init{
        trilist_ = ArrayList()
        trilistStored_ = ArrayList()
        myCollisionList = ArrayList()
        current = trilist_.size
    }


    public override fun clone(): TriangleListK {
        val b = TriangleListK()
        try {
            b.basepoint = basepoint.clone()
            b.myBounds = myBounds
            b.myCenter = myCenter.clone()
            b.myLength = myLength.clone()
            //b = new TriangleListK();
            b.current = current
            b.lastTapNum_ = lastTapNum_
            b.lastTapSide_ = lastTapSide_
            b.myScale = myScale
            b.myAngle = myAngle
            b.basepoint = basepoint.clone()
            b.trilist_ = trilist_.toMutableList()// as ArrayList<TriangleK?>
        } catch (e: Exception) {
            e.printStackTrace()
        }

        //ノードポインターのリコネクト
        if (trilist_.size > 0) {
            b.resetAllNodes()
        }
        return b
    }

    private fun trilist_clone(): java.util.ArrayList<TriangleK?> {
        val list = ArrayList<TriangleK?>()
        for (i in trilist_.indices) {
            list.add(trilist_[i]!!.clone())
            //b.myTriListAtView.add(this.myTriListAtView.get(i).clone());
        }
        return list
    }


    fun add(pnum: Int, pbc: Int, A: Float, B: Float, C: Float): Boolean {
        return add(TriangleK(get(pnum), pbc, A, B, C))
    }

    fun add(pnum: Int, pbc: Int, B: Float, C: Float): Boolean {
        return add(TriangleK(get(pnum), pbc, B, C))
    }

    fun add(next: TriangleK?): Boolean {
        if(next == null) return false
        if(validTriangle(next) == false) return false

        //trilistStored_ = (ArrayList<TriangleK>) trilist_.clone();

        // 番号を受け取る
        next.myNumber_ = trilist_.size + 1
        val pbc = next.parentBC_
        if (next.parentNumber_ > 0) {
            val parent = getTriangle(next.parentNumber_)
            if (parent.alreadyHaveChild(pbc) == true) {
                // すでに親の接続辺上に子供がいたら、挿入処理
                //nextTriangle.myNumber_ = nextTriangle.parentNumber_ +1;
                insertAndSlide(next)
            } else {
                trilist_.add(next) // add by arraylist
                // 自身の番号が付く
                //nextTriangle.setNumber(myTriList.size());
            }

            // 親に告知する
            //if( nextTriangle.myNumber_ > 1 ) trilist_.get( nextTriangle.parentNumber_ -1 ).setChild(nextTriangle, nextTriangle.getParentBC());
        } else {
            trilist_.add(next) // add by arraylist
        }
        current = next.myNumber_
        //lastTapNum_ = 0;
        //lastTapSide_ = -1;
        return true
    }

    fun resetAllNodes() {
        for (i in trilist_.indices) {
            val tri = trilist_[i]
            if (tri!!.nodeTriangle[0] != null) {
                tri.setNode(trilist_[tri.nodeTriangle[0]!!.myNumber_ - 1], 0, false)
            }
            if (tri.nodeTriangle[1] != null) {
                tri.setNode(trilist_[tri.nodeTriangle[1]!!.myNumber_ - 1], 1, false)
            }
            if (tri.nodeTriangle[2] != null) {
                tri.setNode(trilist_[tri.nodeTriangle[2]!!.myNumber_ - 1], 2, false)
            }
        }
    }

    fun setChildsToAllParents() {
        for (i in trilist_.indices) {
            val pnForMe = trilist_[i]!!.parentNumber_
            val me = trilist_[i]
            if (pnForMe > -1) {
                // 改善版
                val parent = trilist_[pnForMe - 1] //batu->//index指定のget関数をnumberで呼んでいる。しかしなぜか上手くいっている。ふしぎー
                // 親に対して、
                parent!!.setChild(me!!, me.parentBC_)
            }
        }
    }

    fun insertAndSlide(nextTriangle: TriangleK) {
        trilist_.add(nextTriangle.myNumber_ - 1, nextTriangle)
        getTriangle(nextTriangle.parentNumber_).setChild(nextTriangle, nextTriangle.parentBC_)

        //次以降の三角形の親番号を全部書き換える、ただし連続しない親で、かつ自分より若い親の場合はそのままにする。
        rewriteAllNodeFrom(nextTriangle, +1)
        resetConnectedTriangles(nextTriangle.myNumber_, nextTriangle)
    }

    //次以降の三角形の親番号を全部書き換える、ただし連続しない親で、かつ自分より若い親の場合はそのままにする。
    // この関数自体は、どの三角形も書き換えない。
    fun rewriteAllNodeFrom(target: TriangleK?, numberChange: Int) {
        for (i in target!!.myNumber_ until trilist_.size) {
            val parent = trilist_[i]
            if (parent!!.hasConstantParent() == false && parent.parentNumber_ <= target.myNumber_) {
                parent.myNumber_ += numberChange
            } else {
                parent.parentNumber_ += numberChange
                parent.myNumber_ += numberChange
            }
        }
    }

    fun insert(index_: Int, nextTriangle: TriangleK?): Boolean {
        var index = index_
        if (nextTriangle == null || !nextTriangle.validTriangle()) return false
        trilist_.add(index - 1, nextTriangle) // add(insert) by arraylist
        getTriangle(index).setNumber(index)

        // all child node change
        for (iNext in index + 1..trilist_.size) {
            if (getTriangle(iNext).parentNumber_ == getTriangle(index).myNumber_) {
                getTriangle(iNext).resetByParent(getTriangle(iNext).nodeTriangle[0]!!, getTriangle(iNext).parentBC_)
                getTriangle(iNext - 1).setChildSide_(getTriangle(iNext).parentBC_)
            }
            index++
        }
        return true
    }

    fun remove(number: Int) {
        //number = lastTapNum_;

        //１番目以下は消せないことにする。
        if (number <= 1) return
        trilist_[number - 1]!!.nodeTriangle[0]!!.removeNode(trilist_[number - 1]!!) //removeTheirNode();
        trilist_.removeAt(number - 1)

        //ひとつ前の三角形を基準にして
        val parentTriangle = trilist_[number - 2]
        //次以降の三角形の親番号を全部書き換える
        rewriteAllNodeFrom(parentTriangle, -1)
        resetConnectedTriangles(parentTriangle!!.myNumber_, parentTriangle)
        current = number - 1
        lastTapNum_ = number - 1
        lastTapSide_ = -1

        //this.cloneByScale(basepoint, myScale);
    }

    fun rotateCurrentTriLCR(): ConnParam? {
        if (lastTapNum_ < 2) return null
        val curTri = trilist_[lastTapNum_ - 1]
        val cParam = curTri!!.rotateLCRandGet().cParam_.clone()
        //if( lastTapNum_ < myTriList.size() ) myTriList.get(lastTapNum_).resetByParent( curTri, myTriList.get(lastTapNum_).cParam_ );

        // 次以降の三角形を書き換えている
        for (i in trilist_.indices) {
            val nextTri = trilist_[i]
            if (i > 0) {
                val npmi = nextTri!!.nodeTriangle[0]!!.myNumber_ - 1
                if (npmi == curTri.myNumber_ - 1) nextTri.setParent(curTri.clone(), nextTri.parentBC_)
                nextTri.resetByParent(trilist_[npmi]!!, nextTri.cParam_)
            }
        }
        //resetMyChild( myTriList.get(lastTapNum_-1), myTriList.get(lastTapNum_-1).cParam_ );
        return cParam
    }

    fun resetNodeByID(prms: Params) {
        val doneObjectList = ArrayList<TriangleK>()
        trilist_[prms.n - 1]!!.resetNode(prms, trilist_[prms.pn - 1]!!, doneObjectList)
    }

    fun resetFromParam(prms: Params): Boolean {
        val ci = prms.n - 1
        val tri = trilist_[ci]

        // 親番号が書き換わっている時は入れ替える。ただし現在のリストの範囲外の番号は除く。
        val pn = prms.pn
        if (pn != tri!!.parentNumber_ && pn < trilist_.size && pn > 0) tri.setNode(trilist_[pn - 1], 0, false)
        tri.reset(prms)

        // 関係する三角形を書き換える
        resetConnectedTriangles(prms.n, tri)
        return true
    }

    fun resetConnectedTriangles(cNum: Int, curtri: TriangleK?): Boolean {
        if (curtri == null) return false
        if (!curtri.validTriangle()) return false

        //trilistStored_ = (ArrayList<TriangleK>) trilist_.clone();
        curtri.myNumber_ = cNum //useless?

        // 親がいるときは、子接続を書き換える
        if (curtri.parentNumber_ > 0 && cNum - 2 >= 0) {
            val parent = trilist_[curtri.parentNumber_ - 1]
            parent!!.childSide_ = curtri.parentBC_
            curtri.nodeTriangle[0] = parent //再リンクしないと位置と角度が連動しない。
            val curPareNum = trilist_[cNum - 1]!!.parentNumber_
            // 自分の親番号と、親の三角形の番号が一致したとき？
            if (cNum > 1 && curPareNum == parent.myNumber_) { //trilist_.get(curPareNum-1)
                parent.resetByChild(curtri)
            }
        }

        // 自身の書き換え
        val me = trilist_.get(cNum - 1)
        //me.setNode( trilist_.get( ));


        // 浮いてる場合、さらに自己番が最後でない場合、一個前の三角形の位置と角度を自分の変化にあわせて動かしたい。
        //if (curtri.parentNumber_ <= 0 && trilist_.size != cNum && exist( curtri.nodeTriangle[0] ) ) {
            //curtri.resetByChild(trilist_[cNum]!!)
        //}
        me!!.reset(curtri) // ここがへん！ 親は自身の基準角度に基づいて形状変更をするので、それにあわせてもう一回呼んでいる。


        // 子をすべて書き換える
        if (trilist_.size > 1 && cNum < trilist_.size && curtri.hasChild() == true) resetMyChild(trilist_[cNum - 1])

        //lastTapNum_ = 0;
        //lastTapSide_ = -1;
        return true
    }

    //　ターゲットポインターがリストの中にいたらtrue
    fun exist(target: TriangleK?): Boolean {
        if( target == null ) return false

        for (i in trilist_.indices) {
            if (trilist_[i] === target) return true
        }
        return false
        //return trilist_.contains( target );
    }

    fun resetMyChild(newParent_: TriangleK?) {
        //myTriList.get(2).reset(newParent, 1);
        var newParent: TriangleK? = newParent_ ?: return

        // 新しい親の次から
        for (i in newParent!!.myNumber_ until trilist_.size) {
            // 連番と派生接続で分岐。
            if (trilist_[i]!!.parentNumber_ == newParent.myNumber_) {
                // ひとつ前の三角形の子接続を書き換える？
                if (i + 1 < trilist_.size) newParent.childSide_ = trilist_[i + 1]!!.parentBC_
                if (!trilist_[i]!!.resetByParent(newParent, trilist_[i]!!.parentBC_)) return
                // 自身が次の親になる
                newParent = trilist_[i]
            } else { //連番でないとき
                //親を番号で参照する
                if (!trilist_[i]!!.resetByParent(trilist_[trilist_[i]!!.parentNumber_ - 1]!!, trilist_[i]!!.parentBC_)) return
                trilist_[trilist_[i]!!.parentNumber_ - 1]!!.childSide_ = trilist_[i]!!.parentBC_
            }
            //else myTriList.get(i).reload();
        }
    }

    fun resetChildsFrom(){//rTri: TriangleK?) {
        if (lastTapNum_ <= 0) return
        for (i in lastTapNum_ until trilist_.size) {
            val nextTri = trilist_[i]
            nextTri!!.resetByParent(nextTri.nodeTriangle[0]!!, nextTri.cParam_)
        }
    }


    fun replace(number: Int, pnum: Int): Boolean {
        if (trilist_[number - 1] == null) return false
        val me = trilist_[number - 1]
        val pr = trilist_[pnum - 1]!!
        me!![pr, me.parentBC_, me.length[0], me.length[1]] = me.length[2]
        return true
    }


    fun undo() {
        //if( trilistStored_ != null ) trilist_ = (ArrayList<TriangleK>) trilistStored_.clone();
    }

    fun getSokutenList(start: Int, pitch: Int): ArrayList<TriangleK> {
        val allSokTriList = getAllSokutenList()
        val sokTriList = ArrayList<TriangleK>()
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
        if (getSokutenListVector() > 0 && sokTriList.size > 1) {
            val lasttri = sokTriList[sokTriList.size - 1]
            val pasttri = sokTriList[sokTriList.size - 2]
            val lastx = lasttri.point[0].x
            val pastx = pasttri.point[0].x
            sokTriList.add(TriangleK(5f, 5f, 5f))
            sokTriList[sokTriList.size - 1].point[0].x = lastx + (lastx - pastx)
            sokTriList[sokTriList.size - 1].lengthAforce_ = lasttri.lengthAforce_
            sokTriList[sokTriList.size - 1].length[0] = lasttri.length[0]
        }

        // 最初にいっこ足す
        if (getSokutenListVector() < 0 && sokTriList.size > 1) {
            val firsttri = sokTriList[0]
            val secondtri = sokTriList[1]
            val first = firsttri.point[0]
            val second = secondtri.point[0]
            sokTriList.add(0, TriangleK(5f, 5f, 5f))
            sokTriList[0].point[0] = first.minus(second).plus(first)
            sokTriList[0].lengthAforce_ = firsttri.lengthAforce_
            sokTriList[0].length[0] = firsttri.length[0]
        }
        return sokTriList
    }

    // 全ての測点リストを返す
    fun getAllSokutenList(): ArrayList<TriangleK> {
        val numTriList = ArrayList<TriangleK>()
        for (i in trilist_.indices) {
            if (trilist_[i]!!.myName_.contains("No.")) {
                numTriList.add(trilist_[i]!!.clone())
            }
        }
        return numTriList
    }

    // 測点のリストの順逆を返す。正の時は三角形リストと同じ方向、負の時は逆方向
    fun getSokutenListVector(): Int {
        val allSokutenList = getAllSokutenList()
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

    fun getUnScaledPointOfName(name: String): PointXY? {
        for (i in 1 until trilist_.size + 1) {
            if (trilist_[i]!!.myName_ == name) return trilist_[i]!!.point[0].scale(1 / myScale)
        }
        return PointXY(0f, 0f)
    }

    fun getPrintTextScale(drawingScale: Float, type: String): Float {
        var pdfts = 10f
        var dxfts = 0.45f
        when ((getPrintScale(drawingScale) * 10).toInt()) {
            150 -> pdfts = 3f
            100 -> pdfts = 3f
            90 -> pdfts = 3f
            80 -> pdfts = 3f
            70 -> pdfts = 3f
            60 -> pdfts = 3f
            50 -> pdfts = 3f
            45 -> pdfts = 3f
            40 -> pdfts = 5f
            30 -> pdfts = 6f
            25 -> pdfts = 6f
            20 -> pdfts = 8f
            15 -> {
                pdfts = 8f
                dxfts = 0.35f
            }
            10 -> {
                pdfts = 10f
                dxfts = 0.35f
            }
            5 -> {
                pdfts = 10f
                dxfts = 0.25f
            }
        }
        return if (type == "dxf" || type == "sfc") dxfts else pdfts
    }

    fun getPrintScale(drawingScale: Float): Float { // ex. 1/100 is w40m h27m drawing in range.
        scale(PointXY(0f, 0f), 1 / drawingScale)
        val longsideX = measureMostLongLine().x
        val longsideY = measureMostLongLine().y
        scale(PointXY(0f, 0f), drawingScale)
        val paperWidth = 38f
        val paperHeight = 25f

//        float printScale = 1f; //drawingScale;
        //if( longsideX <= paperWidth*0.2 && longsideY <= paperHeight*0.2 ) return printScale *= 0.2f;
        if (longsideX <= paperWidth * 0.5 && longsideY <= paperHeight * 0.4) return 0.5f
        if (longsideX <= paperWidth && longsideY <= paperHeight) return 1.0f
        if (longsideX <= paperWidth * 1.5 && longsideY <= paperHeight * 1.4) return 1.5f
        if (longsideX <= paperWidth * 2.0 && longsideY <= paperHeight * 1.9) return 2.0f
        if (longsideX <= paperWidth * 2.5 && longsideY <= paperHeight * 2.5) return 2.5f
        if (longsideX <= paperWidth * 3.0 && longsideY <= paperHeight * 3.0) return 3.0f
        if (longsideX <= paperWidth * 4.0 && longsideY <= paperHeight * 4.0) return 4.0f
        if (longsideX <= paperWidth * 4.5 && longsideY <= paperHeight * 4.5) return 4.5f
        if (longsideX <= paperWidth * 5.0 && longsideY <= paperHeight * 5.0) return 5.0f
        if (longsideX <= paperWidth * 6.0 && longsideY <= paperHeight * 5.0) return 6.0f
        if (longsideX <= paperWidth * 7.0 && longsideY <= paperHeight * 5.0) return 7.0f
        if (longsideX <= paperWidth * 8.0 && longsideY <= paperHeight * 5.0) return 8.0f
        if (longsideX <= paperWidth * 9.0 && longsideY <= paperHeight * 5.0) return 9.0f
        return if (longsideX <= paperWidth * 10.0 && longsideY <= paperHeight * 10.0) 10.0f else 15f
    }

    fun getAngle(): Float {
        return myAngle
    }

    fun setAngle(angle: Float) {
        myAngle = angle
    }

    fun roundByUnderTwo(fp: Float): Float {
        val ip = fp * 100f
        return Math.round(ip) / 100f
    }

    fun getNotSelectedDims(): ArrayList<Collision>? {
        myCollisionList = ArrayList()
        for (i in trilist_.indices) {
            myCollisionList!!.add(getChildAdress(i))
        }
        return myCollisionList
    }

    fun getChildAdress(i: Int): Collision {
        val col = Collision()
        return Collision()
    }

    fun calcBounds(): Bounds {
        myBounds = Bounds(0f, 0f, 0f, 0f)
        for (i in trilist_.indices) {
            myBounds = trilist_[i]!!.expandBoundaries(myBounds)
        }
        return myBounds
    }

    fun getCenter(): PointXY {
        calcBounds()
        myCenter[(myBounds.right + myBounds.left) / 2] = (myBounds.top + myBounds.bottom) / 2
        return myCenter
    }

    fun measureMostLongLine(): PointXY {
        calcBounds()
        myLength[myBounds.right - myBounds.left] = myBounds.top - myBounds.bottom
        return myLength
    }

    fun measureLongLineNotScaled(): PointXY? {
        return measureMostLongLine().scale(1 / myScale)
    }

    fun rotateByLength(align: String): Float {
        var rot = 0f
        if (align === "laydown") {
            while (true) {
                rot -= 10f
                val beforeY = measureMostLongLine().y
                rotate(basepoint, rot)
                if (measureMostLongLine().y >= beforeY) {
                    rotate(basepoint, -rot)
                    return rot + 10f
                }
            }
        }
        if (align === "riseup") {
            while (true) {
                rot += 10f
                val beforeY = measureMostLongLine().y
                rotate(basepoint, rot)
                if (measureMostLongLine().y <= beforeY) {
                    rotate(basepoint, -rot)
                    return rot - 10f
                }
            }
        }
        return rot
    }

    fun getScale(): Float {
        return myScale
    }

    fun setScale(bp: PointXY, sc: Float) {
        myScale = sc
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

    /*public void cloneByScale(PointXY basepoint, float scale){
        myTriListAtView.clear();
        for (int i = 0; i < trilist_.size(); i++ ) {
            myTriListAtView.add(trilist_.get(i).clone());
            myTriListAtView.get(i).scale(basepoint, scale);
        }
    }*/
    fun scale(basepoint: PointXY?, scale: Float) {
        myScale *= scale
        for (i in trilist_.indices) {
            trilist_[i]!!.scale(basepoint, scale)
        }
    }

    fun scaleAndSetPath(basepoint: PointXY?, scale: Float, ts: Float) {
        myScale *= scale
        for (i in trilist_.indices) {
            trilist_[i]!!.scale(basepoint, scale)
            trilist_[i]!!.setDimPath(ts)
        }
    }

    fun setPath(ts: Float) {
        for (i in trilist_.indices) {
            trilist_[i]!!.setDimPath(ts)
        }
    }


    fun rotate(bp: PointXY, angle: Float) {
        myAngle += angle
        basepoint = bp.clone()
        for (i in trilist_.indices) {
            trilist_[i]!!.rotate(basepoint, angle)
            trilist_[i]!!.pointNumber_ = trilist_[i]!!.pointNumber_.rotate(basepoint, angle)
        }
    }

    fun recoverState(bp: PointXY) {
        basepoint = bp.clone()
        for (i in trilist_.indices) {
            trilist_[i]!!.rotate(basepoint, myAngle - 180)
        }
    }

    fun size(): Int {
        return trilist_.size
    }

    fun addCurrent(i: Int): Int {
        current = current + i
        return current
    }

    //fun getCurrent(): Int {
    //  return current
    //}

    //fun setCurrent(c: Int) {
    //  current = c
    //}

    fun validTriangle(tri: TriangleK): Boolean {
        return if (tri.length[0] <= 0.0f || tri.length[1] <= 0.0f || tri.length[2] <= 0.0f) false else tri.length[0] + tri.length[1] > tri.length[2] &&
                tri.length[1] + tri.length[2] > tri.length[0] &&
                tri.length[2] + tri.length[0] > tri.length[1]
    }

    fun vectorToNextFrom(i: Int): PointXY? {
        return if (i < 0 || i >= trilist_.size) PointXY(0f, 0f) else trilist_[i]!!.point[0].vectorTo(trilist_[i + 1]!!.point[0])
    }

    fun getLengthFromSide(sideNum: Int, tri: TriangleK): Float {
        if (sideNum == 1 || sideNum == 3 || sideNum == 4 || sideNum == 7 || sideNum == 9) return tri.length[1]
        return if (sideNum == 2 || sideNum == 5 || sideNum == 6 || sideNum == 8 || sideNum == 10) tri.length[2] else 0f
    }

    fun validTriangle(prm: Params): Boolean {
        return if (prm.a <= 0.0f || prm.b <= 0.0f || prm.c <= 0.0f) false else prm.a + prm.b > prm.c &&
                prm.b + prm.c > prm.a &&
                prm.c + prm.a > prm.b
    }

    // オブジェクトポインタを返す。
    fun getTriangle(number: Int): TriangleK {
        return if (number < 1 || number > trilist_.size) TriangleK() //under 0 is empty. cant hook null. can hook lenA is not 0.
        else trilist_[number - 1]!!
    }

    // by number start 1, not index start 0.
    operator fun get(number: Int): TriangleK {
        return getTriangle(number)
    }

    fun spritByColors(): ArrayList<TriangleListK> {
        val listByColors = ArrayList<TriangleListK>()
        listByColors.add(TriangleListK()) //0
        listByColors.add(TriangleListK())
        listByColors.add(TriangleListK())
        listByColors.add(TriangleListK())
        listByColors.add(TriangleListK()) //4
        for (colorindex in 4 downTo -1 + 1) {
            for (i in trilist_.indices) {
                if (trilist_[i]!!.color_ == colorindex) listByColors[colorindex].add(trilist_[i]!!)
            }
        }
        return listByColors
    }

    fun isCollide(tapP: PointXY?): Int {
        for (i in trilist_.indices) {
            if (trilist_[i]!!.isCollide(tapP!!)) return i + 1.also { lastTapCollideNum_ = it }
        }
        return 0
    }

    fun getTapIndexArray(tapP: PointXY?): IntArray {
        val tapIndexArray = IntArray(trilist_.size)
        for (i in trilist_.indices) {
            tapIndexArray[i] = trilist_[i]!!.getTapLength(tapP!!)
        }
        return tapIndexArray
    }

    fun getTapHitCount(tapP: PointXY?): Int {
        var hitC = 0
        for (i in trilist_.indices) {
            if (trilist_[i]!!.getTapLength(tapP!!) != -1) hitC++
        }
        return hitC
    }

    fun getTap(tapP: PointXY?) {
        val ltn = lastTapNum_ + lastTapSide_
        isCollide(tapP)
        for (i in trilist_.indices) {
            if (trilist_[i]!!.getTapLength(tapP!!) != -1) {
                lastTapNum_ = i + 1
                lastTapSide_ = trilist_[i]!!.getTapLength(tapP)
                if (ltn == lastTapSide_ + lastTapNum_) {
                    isDoubleTap_ = true
                    if (i > 0 && lastTapSide_ == 0) {
                        lastTapNum_ = i
                        lastTapSide_ = trilist_[i - 1]!!.getTapLength(tapP)
                    }
                } else isDoubleTap_ = false
            }
        }
        if (getTapHitCount(tapP!!) == 0) {
            lastTapNum_ = 0
            isDoubleTap_ = false
        }
    }

    fun move(to: PointXY?): MutableList<TriangleK?> {
        for (i in trilist_.indices) {
            trilist_[i]!!.move(to!!)
        }
        basepoint.add(to)
        return trilist_
    }

    fun getParams(i: Int): Params {
        if (i > trilist_.size) return Params("", "", 0, 0f, 0f, 0f, 0, 0, PointXY(0f, 0f), PointXY(0f, 0f))
        val t = trilist_[i - 1]
        return Params(t!!.myName_, "", t.myNumber_, t.length[0], t.length[1], t.length[2], t.parentNumber_, t.parentBC_, t.pointCenter_, t.pointNumberAutoAligned_)
    }

    fun getArea(): Float {
        var area = 0f
        for (i in trilist_.indices) {
            area += trilist_[i]!!.getArea()
        }
        return (Math.round(area * 100.0) / 100.0).toFloat()
    }

    fun getAreaI(number: Int): Float {
        var area = 0f
        for (i in 0 until number) {
            area += trilist_[i]!!.getArea()
        }
        return (Math.round(area * 100.0) / 100.0).toFloat()
    }

    fun getOutlineLists(): ArrayList<ArrayList<PointXY>> {
        val olplists = ArrayList<ArrayList<PointXY>>()
        olplists.add(ArrayList())
        traceOrJumpForward(0, 0, olplists[0])
        for (i in trilist_.indices) {
            if (trilist_[i]!!.isFloating) {
                olplists.add(ArrayList())
                traceOrJumpForward(i, i, olplists[olplists.size - 1])
            }
        }
        return olplists
    }

    fun traceOrJumpForward(startindex: Int, origin: Int, olp: ArrayList<PointXY>): ArrayList<PointXY>? {
        val t = trilist_[startindex]

        //AB点を取る。すでにあったらキャンセル
        if (exist(t!!.point[1], olp) == false) {
            olp.add(t.point[1])
            outlineStr_ += startindex.toString() + "ab,"
        }

        // 再起呼び出しで派生方向に右手伝いにのびていく
        if (t.isChildB_ == true && !t.nodeTriangle[1]!!.isFloating) traceOrJumpForward(t.nodeTriangle[1]!!.myNumber_ - 1, origin, olp)

        //BC点を取る。すでにあったらキャンセル
        if (exist(t.point[2], olp) == false) {
            olp.add(t.point[2])
            outlineStr_ += startindex.toString() + "bc,"
        }
        if (t.isChildC_ == true && !t.nodeTriangle[2]!!.isFloating) traceOrJumpForward(t.nodeTriangle[2]!!.myNumber_ - 1, origin, olp)
        traceOrJumpBackward(startindex, origin, olp)
        return olp
    }

    fun traceOrJumpBackward(startindex: Int, origin: Int, olp: ArrayList<PointXY>): ArrayList<PointXY> {
        val t = trilist_[startindex]!!

        // C派生（ふたつとも接続）していたらそっちに伸びる、フロート接続だったり、すでに持っている点を見つけたらスルー
        if (t.isChildB_ == true && t.isChildC_ == true) if (exist(t.nodeTriangle[2]!!.point[0], olp) == false && !t.nodeTriangle[2]!!.isFloating) traceOrJumpForward(t.nodeTriangle[2]!!.myNumber_ - 1, origin, olp)

        //BC点を取る。すでにあったらキャンセル
        if (exist(t.point[2], olp) == false) {
            olp.add(t.point[2])
            outlineStr_ += startindex.toString() + "bc,"
        }

        //CA点を取る。すでにあったらキャンセル
        if (exist(t.point[0], olp) == false) {
            olp.add(t.point[0])
            outlineStr_ += startindex.toString() + "ca,"
        }

        // 0まで戻る。
        if (t.parentNumber_ > origin) traceOrJumpBackward(t.parentNumber_ - 1, origin, olp)
        return olp
    }

    // 同じポイントは二ついらない
    fun exist(it: PointXY, inthis: ArrayList<PointXY>): Boolean {
        for (i in inthis.indices) if (true == it.nearBy(inthis[i], 0.001f)) return true
        return false
    }


    //壁に右手をあてて洞窟を回る方式。
    fun getOutLinePoints(start: Int): ArrayList<PointXY> {
        var olp = ArrayList<PointXY>()
        val endnum = searchFloatintTriangleFrom(start)

        //olp = outlineForward( trilist_, start)
        //olp = outlineBackward( trilist_, endnum)
        // 右回りに前進。順繰り戻るのではなく子の番号に移る。接続がなくなるまで。フロート接続を含む。
        // 右回りに後退。順繰り戻るのではなく親の番号に移る。子接続があったら、上に戻って前進する。接続がなくなるまで。フロート接続を含む。

        //往路
        olp = goAroundCaveBySetRightHand(start, endnum)

        // 戻る場合。派生接続に行き当たった時は
        for (i in endnum - 1 downTo start - 2 + 1) {
            val t = trilist_[i]
            val before = olp[olp.size - 1]

            // 先端に行き当たったら全点足す ただし図形の終点ではないとき。
            if (t!!.isChildB_ == false && t.isChildC_ == false && t.myNumber_ != endnum) {
                if (before == t.point[1] == false) olp.add(t.point[1])
                if (before == t.point[2] == false) olp.add(t.point[2])
                if (before == t.point[0] == false) olp.add(t.point[0])
                continue
            }

            // いっこ先のが二重断面ならBC点を足す
            if (i + 1 < trilist_.size) if (trilist_[i + 1]!!.parentBC_ == 4 || trilist_[i + 1]!!.parentBC_ == 7) olp.add(t.point[2])

            // いっこ先のが非連続のときは回す
            if (i + 1 < trilist_.size) if (trilist_[i + 1]!!.parentNumber_ != t.myNumber_) continue

            // 基本CA点がダブらないときは足していく方式。
            if (before == t.point[0] == false) olp.add(t.point[0])

            // C接続があるときは。しかしこれだと次の番号がいっこ減る。たいていの場合接続先は進んだ番号なので、違う処理が必要。
            if (t.isChildC_ == true) if (t.nodeTriangle[2]!!.isFloating == false) continue
            olp.add(t.point[0])
        }
        return olp
    }

    fun goAroundCaveBySetRightHand(start: Int, endnum: Int): ArrayList<PointXY> {
        val olp = ArrayList<PointXY>()
        val t1 = trilist_[start]
        olp.add(t1!!.point[1])

        // 0:not use, 1:B, 2:C, 3:BR, 4:BL, 5:CR, 6:CL, 7:BC, 8: CC, 9:FB, 10:FC
        var i = start
        while (i < endnum) {
            val t = trilist_[i]
            val before = olp[olp.size - 1]
            //AB点を取る。すでにあったらキャンセル
            if (before == t!!.point[1] == false) olp.add(t.point[1])

            //単独の突点の場合。
            if (t.isChildB_ == false && t.isChildC_ == false && t.myNumber_ != endnum) {
                if (before == t.point[2] == false) olp.add(t.point[2])
                if (before == t.point[0] == false) olp.add(t.point[0])
                i++
                continue
            }

            //派生接続のとき。B接続に沿って進む。
            if (t.isChildB_ == true && t.isChildC_ == true) {
                i = t.nodeTriangle[1]!!.myNumber_ - 2
                i++
                continue
            }
            i++
        }
        return olp
    }

    fun searchFloatintTriangleFrom(start: Int): Int {
        var start = start
        start += 1
        for (i in start until trilist_.size) {
            val t = trilist_[i]
            if (t!!.isFloating) return t.parentNumber_
        }
        return trilist_.size
    }

    fun numReverse(): MutableList<TriangleK?> {
        val rev = trilist_.toMutableList()
        var iBackward = trilist_.size
        //マイナンバーの書き換え
        for (i in trilist_.indices) {
            if( rev[i] != null ) rev[i]!!.myNumber_ = iBackward
            iBackward--
        }
        return rev
    }


    fun reverse(): TriangleListK{
        if( trilist_.size < 2 ) return this

        val re = this.clone()

        re.trilist_.forEach { it!!.myNumber_ = trilist_.size + 1 - it.myNumber_ }

        re.trilist_.reverse()

        re.trilist_[0]!!.autoRotateNode()

        re.trilist_.forEach {
            //it!!.rotateNode()
            if( it!!.nodeTriangle[0] != null ) it.parentNumber_ = it.nodeTriangle[0]!!.myNumber_
            else it.parentNumber_ = -1
        }

        return re
    }

    fun reverse_(): TriangleListK {
        for (i in trilist_.indices) {
            //終端でなければ
            if (i + 1 < trilist_.size) {
                val next = trilist_[i + 1]
                // 連番でない時は
                if (i + 1 > next!!.parentNumber_) {
                    //return this;//akirameru
                }
            }
        }

        // 番号だけを全部逆順に書き換え
        val numrev = numReverse()
        for (i in trilist_.indices) {
            val me = numrev.get(i)!!

            //接続情報の書き換え
            //終端でなければ
            if (i + 1 < trilist_.size) {
                val next = numrev.get(i + 1)!!

                // 連番の時は
                if (i + 1 == next.parentNumber_) {
                        // 自身の番号-1を親とする
                    me.parentNumber_ = me.myNumber_ - 1
                    me.rotateLengthBy(next.parentSide)
                    me.setNode(next, 0, false)

                        //たとえば次がB辺接続だった場合、自分のB辺がA辺となるように全体の辺長配置を時計回りに回す。
                        //それから、次の接続辺指定を自身に移植する。
                        //このとき、二重断面指定はたぶん逆になる。
                        //次がB辺接続だったとしても、そのまた次もB辺接続だったら、次のやつの辺長が配置換えされるので、接続がおかしくなる
                        //次のやつのB辺はC辺にあった。
                        //同じ接続方向が続いている時は、PBCをCtoB、BtoCに反転させる。
                        if (i + 2 < trilist_.size) {
                            val nextnext = numrev.get(i + 2)!!
                            if (next.parentSide == nextnext.parentSide) me.setReverseDefSide(next.parentBC_, true) else if (i + 2 != nextnext.parentNumber_ && next.parentBC_ != 1) me.setReverseDefSide(-next.parentBC_ + 3, false) else me.setReverseDefSide(next.parentBC_, false)
                        } else {
                            me.setReverseDefSide(next.parentBC_, false)
                        }
                        next.setNode(me, me.parentSide, false)
                    } else {
                        //連番でないときは
                        me.parentNumber_ = -1 //next.parentNumber_;
                        me.parentBC_ = -1
                        val prev = numrev.get(i)
                        me.rotateLengthBy(2)
                        me.setNode(me.nodeTriangle[0], me.parentSide, false)

                        //next.parentNumber_ = trilist_.get( next.parentNumber_ - 1 ).myNumber_;
                        //next.parentBC_ =  trilist_.get( next.parentNumber_ - 1 ).parentBC_;
                    }
            } else if (i + 1 == trilist_.size) {
                // 終端の時、つまり一番最初のやつ。
                me.parentNumber_ = 0
                // 終端は、自身の接続辺をもとにいっこまえの接続が決まっているので、それに合わせて辺長を回す。
                // 最初の三角形になるので、接続情報は要らない。
                me.rotateLengthBy(-me.parentSide + 3)
            }
        }

        // 逆順にソートし、新規に足していく。リビルドした方が考え方が楽。
        val rev = clone()
        rev.trilist_.clear()
        for (i in trilist_.size - 1 downTo -1 + 1) {
            val it = numrev.get(i)!!

            // 自身の番号よりも親番号が大きい場合を許容する
            if (it.parentNumber_ > it.myNumber_) {
                    // 子の三角形を指定して生成する。
                    //rev.add( new TriangleK( trilist_.get( it.parentNumber_), it.parentBC_, it.lengthAforce_, it.lengthBforce_, it.lengthCforce_ ) );
                }
            if (it.parentNumber_ < 1) rev.add(it.clone()) //new TriangleK( it.lengthAforce_, it.lengthBforce_, it.lengthCforce_, it.point[0], it.angleInGlobal_)
            else {
                rev.add(TriangleK(rev[it.parentNumber_], it.parentBC_, it.lengthAforce_, it.lengthBforce_, it.lengthCforce_))
            }
        }

        // 名前の書き換え、反転後のひとつ先の三角形に移設し、1番の名前は消去
        for (i in trilist_.indices) {
            val it = numrev.get(trilist_.size - i - 1)
            if (i + 2 <= trilist_.size) rev[i + 2].myName_ = it!!.myName_
        }
        rev[1].myName_ = ""
        return rev
    }

    fun numbered(start: Int): TriangleListK {
        val rev = TriangleListK()
        for (i in trilist_.indices) {
            val ti = trilist_[i]!!.clone()
            ti.myNumber_ = start + i
            rev.trilist_.add(ti)
        }
        return rev
    }

}