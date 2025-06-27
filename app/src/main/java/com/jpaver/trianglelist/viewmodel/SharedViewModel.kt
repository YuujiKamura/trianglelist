package com.jpaver.trianglelist.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    private val _tableHeight = MutableLiveData<Int>()

    fun setTableHeight(height: Int) {
        _tableHeight.value = height
    }
}
