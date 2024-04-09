package com.jpaver.trianglelist

import android.util.Log

class MainViewModel {

    var deductionMode = false
    var myTriangleList = TriangleList()
    var myDeductionList = DeductionList()

    //private var myEditor: EditorTable = EditorTable()
    //private var dParams: Params = Params("", "", 0, 0f, 0f, 0f, 0, 0,
     //   PointXY(0f, 0f)
    //)

    //private lateinit var myELFirst: EditTextViewLine
    //private lateinit var myELSecond: EditTextViewLine

    fun setMember( dedMode: Boolean, triList: TriangleList, dedList: DeductionList){
        deductionMode = dedMode
        myTriangleList = triList
        myDeductionList = dedList
    }

    private fun getList(dMode: Boolean) : EditList {
        return if(dMode) myDeductionList
        else myTriangleList
    }


    fun fabDimArrange(WorH: String, refreshMethod:()->Unit ){
        if(deductionMode || myTriangleList.lastTapNumber_ < 1 ){
            return
        }
        val triAndDimside = isConnectedOrNot()
        when (WorH){
            "W" -> triAndDimside.first.controlDimHorizontal(triAndDimside.second)
            "H" -> triAndDimside.first.controlDimVertical(triAndDimside.second)
        }
        refreshMethod()
    }

    fun isConnectedOrNot(): Pair<Triangle,Int> {
        Log.d("fabDimSide", "isConnectedOrNot")
        val dimside = myTriangleList.lastTapSide_
        val trinum  = myTriangleList.lastTapNumber_
        val tri = myTriangleList.get(trinum)
        Log.d("TriangleList", "Triangle dim rot w : $trinum$dimside")

        if( dimside == 0 && ( tri.connectionType == 1 ||  tri.connectionType == 2 ) && trinum > 1 ) {
            val dim2  = tri.connectionType
            val tri2 = myTriangleList.get( tri.parentnumber )
            Log.d("TriangleList", "Triangle dim rot w : " + tri.mynumber + dimside )
            return Pair(tri2, dim2)
        }

        return Pair(tri, dimside)
    }

    fun fabReverse( ) :EditList{

        when(deductionMode){
            false -> myTriangleList = myTriangleList.reverse()
            true -> myDeductionList = myDeductionList.reverse()
        }

        return getList( deductionMode )
    }

}