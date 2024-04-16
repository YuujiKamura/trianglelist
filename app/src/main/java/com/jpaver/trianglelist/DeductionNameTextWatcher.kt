package com.jpaver.trianglelist

import android.text.Editable
import android.text.TextWatcher
import com.jpaver.trianglelist.util.EditTextViewLine
import com.jpaver.trianglelist.util.InputParameter

//region TextWatcher
class DeductionNameTextWatcher(
    val mELine: EditTextViewLine,
    var lastParams: InputParameter
) : TextWatcher {
    //private val afterTextChanged_: TextView = findViewById<TextView>(R.id.afterTextChanged)
    //private val beforeTextChanged_: TextView = findViewById<TextView>(R.id.beforeTextChanged)
    //private val onTextChanged_: TextView = findViewById<TextView>(R.id.onTextChanged)
    override fun afterTextChanged(s: Editable) {
        val input = mELine.name.text.toString()
//            myEditor.LineRewrite(Params(input,"",myDeductionList.length()+1,dP.a, dP.b, dP.c, dP.pn, i, PointXY(0f,0f)), myELFirst)

        if(input == "仕切弁" || input == "ソフト弁" || input == "ドレーン") {
            mELine.a.setText( 0.23f.toString() )
            mELine.b.setText("")
            mELine.pl.setSelection(2)
        }
        if(input == "消火栓" || input == "空気弁") {
            mELine.a.setText( 0.55f.toString() )
            mELine.b.setText("")
            mELine.pl.setSelection(2)
        }
        if(input == "下水") {
            mELine.a.setText( 0.72f.toString() )
            mELine.b.setText("")
            mELine.pl.setSelection(2)
        }
        if(input == "汚水") {
            mELine.a.setText( 0.67f.toString() )
            mELine.b.setText("")
            mELine.pl.setSelection(2)
        }
        if(input == "雨水枡" || input == "電柱"){
            mELine.a.setText( 0.40f.toString() )
            mELine.b.setText("")
            mELine.pl.setSelection(2)
        }
        if(input == "電気" || input == "NTT"){
            mELine.a.setText("1.0")
            mELine.b.setText("")
            mELine.pl.setSelection(2)
        }
        if(input == "基準点"){
            mELine.a.setText("0.3")
            mELine.b.setText("")
            mELine.pl.setSelection(2)
        }
        if(input == "消火栓B") {
            mELine.a.setText( 0.35f.toString() )
            mELine.b.setText( 0.45f.toString() )
            mELine.pl.setSelection(1)
        }
        if(input == "基礎") {
            mELine.a.setText( 0.50f.toString() )
            mELine.b.setText( 0.50f.toString() )
            mELine.pl.setSelection(1)
        }
        if(input == "集水桝") {
            mELine.a.setText( 0.70f.toString() )
            mELine.b.setText( 0.70f.toString() )
            mELine.pl.setSelection(1)
        }

        // 記憶した控除パラメータの復元
        if(input == lastParams.name){
            mELine.a.setText(lastParams.a.toString())
            mELine.b.setText(lastParams.b.toString())
            mELine.pl.setSelection(lastParams.pl)
        }
    }

    override fun beforeTextChanged(
        s: CharSequence,
        start: Int,
        count: Int,
        after: Int
    ) {
        ("start=" + start
                + ", count=" + count
                + ", after=" + after
                + ", s=" + s.toString())
        //beforeTextChanged_.text = input
    }

    override fun onTextChanged(
        s: CharSequence,
        start: Int,
        before: Int,
        count: Int
    ) {
        ("start=" + start
                + ", before=" + before
                + ", count=" + count
                + ", s=" + s.toString())
        //onTextChanged_.text = input
    }
}//end class

//EditTextの入力があったときにビューに反映させる
interface CustomTextWatcher: TextWatcher{
    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
}

//endregion