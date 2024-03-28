package com.jpaver.trianglelist

import com.jpaver.trianglelist.util.DeductionParams
import com.jpaver.trianglelist.util.Params

class DeductionList internal constructor() : EditList(), Cloneable {
    var dedlist_ = ArrayList<Deduction>()
    var current = 0
    var lastTapIndex_ = -1
    var myAngle = 0f
    fun scale(basepoint: PointXY?, sx: Float, sy: Float) {
        for (i in dedlist_.indices) {
            dedlist_[i].scale(basepoint!!, sx, sy)
        }
    }

    fun setScale(s: Float) {
        for (i in dedlist_.indices) {
            dedlist_[i].myscale = s
        }
    }

    // 汎用的な処理を行う高階関数
    fun <T> doSomething(list: List<T>, action: (T) -> Unit) {
        list.forEach(action)
    }

    // scale関数はdoSomethingを使って定義
    fun scale(basepoint: PointXY, s: Float) {
        doSomething(dedlist_) { it.scale(basepoint, s, s) }
    }

    fun move(to: PointXY?) {
        for (i in dedlist_.indices) {
            dedlist_[i].move(to!!)
        }
    }

    companion object {
        const val INVALID_INDEX = -11
    }

    fun getTapIndex(tapP: PointXY?): Int {
        tapP ?: return INVALID_INDEX.also { lastTapIndex_ = it }

        dedlist_.forEachIndexed { index, deduction ->
            if (deduction.getTap(tapP)) return index.also { lastTapIndex_ = it }
        }

        return INVALID_INDEX.also { lastTapIndex_ = it }
    }


    override fun clone(): DeductionList {
        val b = DeductionList()
        for (i in dedlist_.indices) {
            b.add(dedlist_[i].clone())
        }
        b.lastTapIndex_ = lastTapIndex_
        return b
    }

    fun rotate(bp: PointXY?, angle: Float) {
        myAngle += angle
        for (i in dedlist_.indices) {
            dedlist_[i].rotate(bp!!, angle)
        }
    }

    override fun getArea(): Float {
        return dedlist_.filter { it.overlap_to != 0 }
            .map { it.getArea() }.sum()
    }

    override fun addCurrent(num: Int): Int {
        current = current + num
        return current
    }

    override fun retrieveCurrent(): Int {
        return current
    }

    fun clear() {
        dedlist_.clear()
    }

    fun add(dd: Deduction) {
        processDeduction(dd.apply { sameDedcount = searchSameDed(getParams()) })
    }

    fun add(ddp: DeductionParams?) {
        ddp?.let { processDeduction(Deduction(it)) } ?: throw IllegalArgumentException("DeductionParams cannot be null")
    }

    fun add(dp: Params?) {
        dp?.let {
            val deduction = Deduction(it)
            val sameCount = searchSameDed(it)
            if (sameCount > 1) deduction.sameDedcount = sameCount
            processDeduction(deduction)
        } ?: throw IllegalArgumentException("Params cannot be null")
    }

    private fun processDeduction(deduction: Deduction) {
        dedlist_.add(deduction)
        current = dedlist_.size
    }

    fun searchSameDed(dp: Params?): Int {
        var count = 1
        for (i in dedlist_.indices) {
            if (dedlist_[i].verify(dp!!)) {
                count++
            }
        }
        return count
    }

    override operator fun get(num: Int): Deduction {
        return if (num < 1 || num > dedlist_.size) Deduction() else getDeduction(num)!!
    }

    override fun getParams(num: Int): Params {
        return dedlist_[num - 1].getParams()
    }

    fun getDeduction(num: Int): Deduction? {
        return if (num < 1 || num > dedlist_.size) null else dedlist_[num - 1]
    }

    override fun size(): Int {
        return dedlist_.size
    }

    override fun remove(num: Int) {
        var number = num
        if (number < 1) return
        dedlist_.removeAt(number - 1) // 0 1 (2) 3  to 0 1 3
        while (number - 1 < dedlist_.size) {    // index 2 size 3
            dedlist_[number - 1].num = number // get [2] renum to 3
            number++
        }
        current = dedlist_.size
    }

    fun replace(index: Int, dp: Params?) {
        if (index < 1) return
        dedlist_[index - 1].setParam(dp!!)
    }

    override fun reverse(): DeductionList {
        val rev = DeductionList()
        var iBackward = dedlist_.size - 1
        for (i in dedlist_.indices) {
            rev.add(dedlist_[iBackward])
            rev.dedlist_[i].setNumAndInfo(i + 1)
            iBackward--
        }
        dedlist_ = rev.dedlist_
        return rev
    }
}
