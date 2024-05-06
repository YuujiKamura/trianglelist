package com.jpaver.trianglelist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    private val _tableHeight = MutableLiveData<Int>()
    val tableHeight: LiveData<Int> get() = _tableHeight

    fun setTableHeight(height: Int) {
        _tableHeight.value = height
    }
}
