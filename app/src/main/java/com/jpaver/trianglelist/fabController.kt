package com.jpaver.trianglelist

import android.content.Context
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
//import kotlinx.android.synthetic.main.activity_main.*

class fabController(
    val myEditor :EditorTable,
    val dedmode :Boolean,
    val trilist :TriangleList,
    val dedlist :DeductionList,
    val myELFirst :EditTextViewLine,
    val myELSecond :EditTextViewLine,
    val context: Context,
    val appActivity :AppCompatActivity,
    val my_view :MyView,
    val mScale :Float,
    var lastParams_ :Params,
    var trilistStored_ :TriangleList
) {


    fun EditorClear(elist: EditList, currentNum: Int){
        //loadEditTable()
        my_view.setParentSide(elist.size(), 0)
        myEditor.lineRewrite(
            Params(
                "",
                "",
                elist.size() + 1,
                0f,
                0f,
                0f,
                elist.size(),
                0,
                PointXY(0f, 0f)
            ), myELFirst
        )
        myEditor.lineRewrite(elist.get(currentNum).getParams(), myELSecond)
    }

    fun validTriangle(dp: Params) : Boolean{
        if (dp.a <= 0.0f || dp.b <= 0.0f || dp.c <= 0.0f) return false
        if (dp.a + dp.b <= dp.c ){
            Toast.makeText(context, "Invalid!! : C > A + B", Toast.LENGTH_LONG).show()
            return false
        }
        if (dp.b + dp.c <= dp.a ){
            Toast.makeText(context, "Invalid!! : A > B + C", Toast.LENGTH_LONG).show()
            return false
        }
        if (dp.c + dp.a <= dp.b ){
            Toast.makeText(context, "Invalid!! : B > C + A", Toast.LENGTH_LONG).show()
            return false
        }
        if ( dp.pn > trilist.size() || ( dp.pn < 1 && dp.n != 1 )) {
            Toast.makeText(context, "Invalid!! : number of parent", Toast.LENGTH_LONG).show()
            return false
        }

        return true
    }

    fun validDeduction(dp: Params): Boolean {
        if( dp.name == "" || dp.a < 0.1f ) return false
        if( dp.type == "Box" && ( dp.a < 0.1f || dp.b < 0.1f ) ) return false
        return true
    }

    fun getList(dMode: Boolean) :EditList{
        if(dMode == true) return dedlist
        else return trilist
    }

    fun fabReplace(params: Params, useit: Boolean){

        val editlist = getList(dedmode)
        var readedFirst  = Params()
        var readedSecond = Params()
        myEditor.readLineTo(readedFirst, myELFirst)
        myEditor.readLineTo(readedSecond, myELSecond)
        if( useit == true ){
            readedFirst = params
            readedSecond = params
        }
        val strTopA = appActivity.findViewById<TextView>(R.id.editLengthA1).text.toString()
        val strTopB = appActivity.findViewById<TextView>(R.id.editLengthB1).text.toString()
        val strTopC = appActivity.findViewById<TextView>(R.id.editLengthC1).text.toString()

        var usedDedPoint = params.pt.clone()

        var isSucceed = false

        if( dedmode == false ) {

            if( strTopB == "" ) isSucceed = resetTrianglesBy(readedSecond)
            else
                if( strTopC == "" && useit == false ) return
                else isSucceed = addTriangleBy(readedFirst)

        } else { // if in deduction mode
            //if (validDeduction(params) == false) return


            if( strTopA == "" ) {
                isSucceed = resetDeductionsBy(readedSecond)
                usedDedPoint = my_view.myDeductionList.get(readedSecond.n).point
            }
            else{
                isSucceed = addDeductionBy(readedFirst)
                usedDedPoint = my_view.myDeductionList.get(readedFirst.n).point
            }
            appActivity.findViewById<EditText>(R.id.editName1).requestFocus()
        }

        EditorClear(editlist, editlist.getCurrent())
        my_view.setTriangleList(trilist, mScale)
        my_view.setDeductionList(dedlist, mScale)
        //AutoSaveCSV()
        //setTitles()
        if( dedmode == false ) my_view.resetView(my_view.toLastTapTriangle())
        if( dedmode == true  ) my_view.resetView(usedDedPoint.scale(PointXY(0f, 0f), 1f, -1f))//resetViewToTP()

        my_view.myTriangleList.isDoubleTap_ = false
        my_view.myTriangleList.lastTapSide_ = 0
        /*if( BuildConfig.BUILD_TYPE == "debug" ) Toast.makeText(
                this,
                isSucceed.toString(),
                Toast.LENGTH_SHORT
        ).show()*/
    }


    fun addDeductionBy(params: Params) : Boolean {
        params.pt = my_view.getTapPoint()
        params.pts = params.pt //PointXY(0f, 0f)
        params.pn = my_view.myTriangleList.isCollide(params.pt.scale(PointXY(1f, -1f)))

        //形状の自動判定
        if( params.b > 0f ) params.type = "Box"
        else params.type = "Circle"

        if (validDeduction(params) == true) {
            // 所属する三角形の判定処理
            if( params.pt != PointXY(0f, 0f) ) {
                params.pn = my_view.myTriangleList.isCollide(params.pt.scale(PointXY(1f, -1f)))
            }

            dedlist.add(params)
            my_view.setDeductionList(dedlist, mScale)
            lastParams_ = params
            return true
        }
        else return false
    }

    fun addTriangleBy(params: Params) : Boolean {
        if (validTriangle(params)) {
            trilistStored_ = trilist.clone()
            //appActivity.fab_undo.backgroundTintList = context.getColorStateList(R.color.colorLime)

            var myTri: Triangle = Triangle(
                trilist.getTriangle(params.pn),
                params
            )
            myTri.myNumber_ = params.n
            trilist.add(myTri, true)
            appActivity.findViewById<EditText>(R.id.editLengthA1).requestFocus()
            trilist.lastTapNumber_ = trilist.size()
            //my_view.resetView()
            return true
        }
        return false
    }

    fun resetTrianglesBy(params: Params) : Boolean {

        if (validTriangle(params) == true){
            trilistStored_ = trilist.clone()
            //appActivity.fab_undo.backgroundTintList = context.getColorStateList(R.color.colorLime)

            //if( dParams.n == 1 ) myTriangleList.resetTriangle( dParams.n, Triangle( dParams, myTriangleList.myAngle ) )
            //else
            return trilist.resetFromParam(params)
        } // if valid triangle
        else return false
    }

    fun resetDeductionsBy(params: Params) : Boolean {
        val prms = params
        //myEditor.ReadLineTo(prms, myELSecond)
        prms.pt = my_view.getTapPoint()
        prms.pts = dedlist.get(prms.n).pointFlag

        trilist.current = prms.pn

        if( validDeduction(prms) == true ) {
            // 所属する三角形の判定処理
            if( prms.pt != PointXY(0f, 0f) ) {
                prms.pn = my_view.myTriangleList.isCollide(prms.pt.scale(PointXY(1f, -1f)))
            }

            dedlist.replace(prms.n, prms)
            return true
        }
        else return false
    }

}