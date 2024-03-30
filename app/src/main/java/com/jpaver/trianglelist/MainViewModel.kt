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


    fun fabDimSide( WorH: String, refreshMethod:()->Unit ){
        Log.d("fabDimSide", "fabDimSide 1")
        if(deductionMode || myTriangleList.lastTapNumber_ < 1 ){
            Log.d("fabDimSide", "fabDimSide 2")
            return
        }

        Log.d("fabDimSide", "fabDimSide 3")
        val triAndDimside = isConnectedOrNot()
        Log.d("fabDimSide", "fabDimSide 4")
        when (WorH){
            "W" -> triAndDimside.first.rotateDimSideAlign(triAndDimside.second)
            "H" -> triAndDimside.first.flipDimAlignH(triAndDimside.second)
        }

        refreshMethod()
    }

    fun isConnectedOrNot(): Pair<Triangle,Int> {
        Log.d("fabDimSide", "isConnectedOrNot")
        val dimside = myTriangleList.lastTapSide_
        val trinum  = myTriangleList.lastTapNumber_
        val tri = myTriangleList.get(trinum)
        Log.d("TriangleList", "Triangle dim rot w : $trinum$dimside")

        if( dimside == 0 && ( tri.parentBC == 1 ||  tri.parentBC == 2 ) && trinum > 1 ) {
            val dim2  = tri.parentBC
            val tri2 = myTriangleList.get( tri.parentNumber )
            Log.d("TriangleList", "Triangle dim rot w : " + tri.myNumber + dimside )
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