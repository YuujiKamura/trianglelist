package com.jpaver.trianglelist

import android.util.Log
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jpaver.trianglelist.util.EditTextViewLine
import com.jpaver.trianglelist.util.EditorTable
import com.jpaver.trianglelist.util.Params

class MainViewModel {

    var deductionMode = false
    var myTriangleList = TriangleList()
    var myDeductionList = DeductionList()
    private var trilistUndo = TriangleList()

    private var myEditor: EditorTable = EditorTable()
    private var dParams: Params = Params("", "", 0, 0f, 0f, 0f, 0, 0,
        PointXY(0f, 0f)
    )

    private lateinit var myELFirst: EditTextViewLine
    private lateinit var myELSecond: EditTextViewLine

    fun setMember( dedMode: Boolean, triList: TriangleList, dedList: DeductionList){
        deductionMode = dedMode
        myTriangleList = triList
        myDeductionList = dedList
    }

    private fun getList(dMode: Boolean) : EditList {
        return if(dMode) myDeductionList
        else myTriangleList
    }

    fun fabReplace(params: Params = dParams, useit: Boolean = false, strTop: Triple<String,String,String> ){
        //val editor = myEditor
        val dedmode = deductionMode
        val editlist = getList(deductionMode)

        var readedFirst  = Params()
        var readedSecond = Params()
        myEditor.readLineTo(readedFirst, myELFirst)
        myEditor.readLineTo(readedSecond, myELSecond)
        if(useit){
            readedFirst = params
            readedSecond = params
        }

        var usedDedPoint = params.pt.clone()

        //var isSucceed = false

        if(!dedmode) {

            if( strTop.second == "" ) resetTrianglesBy(readedSecond)
            else
                if(strTop.third == "" && !useit) return
                else  addTriangleBy(readedFirst)

        } else { // if in deduction mode
            //if (validDeduction(params) == false) return

/*
            usedDedPoint = if( strTop.first == "" ) {
                resetDeductionsBy(readedSecond)
                my_view.myDeductionList.get(readedSecond.n).point
            } else{
                addDeductionBy(readedFirst)
                my_view.myDeductionList.get(readedFirst.n).point
            }
            findViewById<EditText>(R.id.editName1).requestFocus()*/
        }
/*
        editorClear(editlist, editlist.getCurrent())
        my_view.setTriangleList(myTriangleList, mScale)
        my_view.setDeductionList(myDeductionList, mScale)
        printDebugConsole()
        autoSaveCSV()
        setTitles()
        if(!dedmode) my_view.resetView(my_view.toLastTapTriangle())
        if(dedmode) my_view.resetView(usedDedPoint.scale(
            PointXY(
                0f,
                0f
            ), 1f, -1f))//resetViewToTP()

        my_view.myTriangleList.isDoubleTap_ = false
        my_view.myTriangleList.lastTapSide_ = 0
*/
    }

    private fun setFabColor(fab: FloatingActionButton, colorIndex: Int ){
        //fab.backgroundTintList = getColorStateList( colorIndex )
    }

    private fun trilistSaving( from: TriangleList ){
        trilistUndo = from.clone()
    }
    private fun createNewTriangle( params: Params, parentTri: Triangle ): Triangle{
        val newTri = Triangle(
            parentTri,
            params
        )
        newTri.myNumber_ = params.n
        return newTri
    }

    private fun trilistAdd(params: Params, triList: TriangleList ){
        val newTri = createNewTriangle( params, triList.getMemberByIndex(params.pn) )
        triList.add(newTri, true)
        triList.lastTapNumber_ = triList.size()
    }

    private fun setUI(){
        //setFabColor( fab_undo, R.color.colorLime )
        //indViewById<EditText>(R.id.editLengthA1).requestFocus()
    }

    private fun addTriangleBy(params: Params) : Boolean {
        if ( isValid( params ) ) {

            trilistSaving( myTriangleList )
            trilistAdd( params, myTriangleList )
            setUI()

            return true
        }
        return false
    }

    private fun resetTrianglesBy(params: Params) : Boolean {

        return if (isValid(params)){
            trilistUndo = myTriangleList.clone()
            //fab_undo.backgroundTintList = getColorStateList(R.color.colorLime)

            //if( dParams.n == 1 ) myTriangleList.resetTriangle( dParams.n, Triangle( dParams, myTriangleList.myAngle ) )
            //else
            myTriangleList.resetFromParam(params)
        } // if valid triangle
        else false
    }

    fun isValid(dp: Params) : Boolean{
        if (dp.a <= 0.0f || dp.b <= 0.0f || dp.c <= 0.0f) return false
        if (dp.a + dp.b <= dp.c ){
            //Toast.makeText(this, "Invalid!! : C > A + B", Toast.LENGTH_LONG).show()
            return false
        }
        if (dp.b + dp.c <= dp.a ){
            //Toast.makeText(this, "Invalid!! : A > B + C", Toast.LENGTH_LONG).show()
            return false
        }
        if (dp.c + dp.a <= dp.b ){
            //Toast.makeText(this, "Invalid!! : B > C + A", Toast.LENGTH_LONG).show()
            return false
        }

        if ( dp.pn > myTriangleList.size() || ( dp.pn < 1 && dp.n != 1 )) {
            //Toast.makeText(this, "Invalid!! : number of parent", Toast.LENGTH_LONG).show()
            return false
        }
        if (  dp.pl < 1 && dp.n != 1  ) {
            //Toast.makeText(this, "Invalid!! : connection in parent", Toast.LENGTH_LONG).show()
            return false
        }

        return true
    }

    fun fabDimSide( WorH: String, refreshMethod:()->Unit ){
        if(deductionMode || myTriangleList.lastTapNumber_ < 1 ) return

        val triAndDimside = isConnectedOrNot()

        when (WorH){
            "W" -> triAndDimside.first.rotateDimSideAlign(triAndDimside.second)
            "H" -> triAndDimside.first.flipDimAlignH(triAndDimside.second)
        }

        refreshMethod()
    }

    fun isConnectedOrNot(): Pair<Triangle,Int> {
        val dimside = myTriangleList.lastTapSide_
        val trinum  = myTriangleList.lastTapNumber_
        val tri = myTriangleList.get(trinum)
        Log.d("TriangleList", "Triangle dim rot w : $trinum$dimside")

        if( dimside == 0 && ( tri.parentBC_ == 1 ||  tri.parentBC_ == 2 ) && trinum > 1 ) {
            val dim2  = tri.parentBC_
            val tri2 = myTriangleList.get( tri.parentNumber )
            Log.d("TriangleList", "Triangle dim rot w : " + tri.myNumber_ + dimside )
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