package com.jpaver.trianglelist.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jpaver.trianglelist.controlDimHorizontal
import com.jpaver.trianglelist.controlDimVertical
import com.jpaver.trianglelist.editmodel.ConnectionSide
import com.jpaver.trianglelist.editmodel.DeductionList
import com.jpaver.trianglelist.editmodel.EditList
import com.jpaver.trianglelist.editmodel.Triangle
import com.jpaver.trianglelist.editmodel.TriangleList
import com.jpaver.trianglelist.editmodel.sideEnum

class MainViewModel : ViewModel() {

    private val _deductionMode = MutableLiveData<Boolean>()

    private val _myTriangleList = MutableLiveData<TriangleList>()

    private val _myDeductionList = MutableLiveData<DeductionList>()

    // LiveDataは初期値を設定することが推奨されます
    init {
        _deductionMode.value = false
        _myTriangleList.value = TriangleList()
        _myDeductionList.value = DeductionList()
    }

    fun setMember(dedMode: Boolean, triList: TriangleList, dedList: DeductionList) {
        _deductionMode.value = dedMode
        _myTriangleList.value = triList
        _myDeductionList.value = dedList
    }

    private fun getList(dMode: Boolean): EditList? {
        return if (dMode) _myDeductionList.value else _myTriangleList.value
    }

    fun fabDimArrange(WorH: String, refreshMethod: () -> Unit) {
        if (_deductionMode.value == true || (_myTriangleList.value?.lastTapNumber ?: 0) < 1) {
            return
        }
        _myTriangleList.value?.let { list ->
            val triAndDimside = isConnectedOrNot(list)
            when (WorH) {
                "W" -> triAndDimside.first.controlDimHorizontal(triAndDimside.second)
                "H" -> triAndDimside.first.controlDimVertical(triAndDimside.second)
            }
            _myTriangleList.value = list // 変更を通知するために再設定
            refreshMethod()
        }
    }

    private fun isConnectedOrNot(currentList: TriangleList): Pair<Triangle, Int> {
        Log.d("fabDimSide", "isConnectedOrNot")
        val dimside = currentList.lastTapSide
        val trinum = currentList.lastTapNumber
        val tri = currentList.get(trinum)
        Log.d("TriangleList", "Triangle dim rot w : $trinum$dimside")

        if (dimside == 0 && (tri.sideEnum == ConnectionSide.B || tri.sideEnum == ConnectionSide.C) && trinum > 1) {
            val dim2 = tri.sideEnum.code
            val tri2 = currentList.get(tri.parentnumber)
            Log.d("TriangleList", "Triangle dim rot w : " + tri.mynumber + dimside)
            return Pair(tri2, dim2)
        }
        return Pair(tri, dimside)
    }

    fun fabReverse(): EditList? {
        _deductionMode.value?.let { dMode ->
            if (!dMode) {
                _myTriangleList.value = _myTriangleList.value?.reverse()
            } else {
                _myDeductionList.value = _myDeductionList.value?.reverse()
            }
        }
        return getList(_deductionMode.value ?: false)
    }
}