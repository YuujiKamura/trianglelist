package com.jpaver.trianglelist.editmodel

import android.util.Log
import com.jpaver.trianglelist.viewmodel.InputParameter

class DeductionList : EditList() {
    var dedlist_ = ArrayList<Deduction>()
    var current = 0
    var lastTapIndex_ = -1
    var myAngle = 0f
    fun scale(basepoint: com.example.trilib.PointXY?, sx: Float, sy: Float) {
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
    fun scale(basepoint: com.example.trilib.PointXY, s: Float) {
        doSomething(dedlist_) { it.scale(basepoint, s, s) }
    }

    fun move(to: com.example.trilib.PointXY?) {
        for (i in dedlist_.indices) {
            dedlist_[i].move(to!!)
        }
    }

    companion object {
        const val INVALID_INDEX = -11
    }

    fun getTapIndex(tapP: com.example.trilib.PointXY?): Int {
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

    fun rotate(bp: com.example.trilib.PointXY?, angle: Float) {
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

    fun add(deduction: Deduction) {
        deduction.setInfo( searchSameDed(deduction) )

        dedlist_.add(deduction)
        current = dedlist_.size
    }


    fun add(dp: InputParameter) {
        val deduction = Deduction(dp)
        add(deduction)

    }

    fun searchSameDed(deduction: Deduction): Int {
        return dedlist_.count { it.verify(deduction) }
    }


    override operator fun get(num: Int): Deduction {
        return if (num < 1 || num > dedlist_.size) Deduction() else getDeduction(num)!!
    }

    override fun getParams(num: Int): InputParameter {
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

    fun replace(parameter: InputParameter) {
        dedlist_[parameter.number-1].set(parameter)
    }

    fun replace(number: Int, ded: Deduction?) {
        if (ded == null) return

        if (number in 1..dedlist_.size) {
            dedlist_[number - 1] = ded
        } else {
            // インデックスが範囲外の場合の処理
            Log.e("DeductionList", "Index out of bounds: index=$number, size=${dedlist_.size}")
        }
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
