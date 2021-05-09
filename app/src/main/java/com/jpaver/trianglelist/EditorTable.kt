package com.jpaver.trianglelist

import android.provider.Settings.Global.getString
import android.text.method.KeyListener
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.res.TypedArrayUtils.getString

data class TriParams(var name: String, var lengthA: Float, var lengthB: Float, var lengthC: Float, var ParentTriSt: String, var ParentTriSide: Int, var myNum: Int, var ParNum:Int )

data class DeductionParams(var num: Int, var name: String, var lengthX: Float, var lengthY: Float, var parentNum: Int, var type: String, var angle: Float, var point: PointXY)

data class TitleParams(var type: Int, var n: Int, var name: Int, var a: Int, var b: Int, var c: Int, var pn: Int, var pl: Int)

class TitleParamStr(var type: String, var n: String, var name: String, var a: String, var b: String, var c: String, var pn: String, var pl: String)


data class EditTextViewLine(var n: EditText, var name: EditText, var a: EditText, var b: EditText, var c: EditText, var pn: EditText, var pl: Spinner)

data class Keys(var n: KeyListener, var name: KeyListener, var a: KeyListener, var b: KeyListener, var c: KeyListener, var pn: KeyListener)

data class Params(var name: String = "",
                  var type: String = "",
                  var n: Int = 0,
                  var a: Float = 0f,
                  var b: Float = 0f,
                  var c: Float = 0f,
                  var pn: Int = 0,
                  var pl: Int = 0,
                  var pt: PointXY = PointXY(0f, 0f),
                  var pts: PointXY = PointXY(0f, 0f))//,
//                  var an: Float = 0f)

data class OutlinePoints( var points: ArrayList<PointXY>, var opstr: String )

class EditorTable {
    var myCurrentTriNumber: Int = 0
    var myCurrentDeductionNumber: Int = 0
    //var dTP: TriParams = TriParams(0f, 0f, 0f, "", 0, 0, 0)
    //var dDP: DeductionParams = DeductionParams(PointXY(0f,0f), 0f, 0f, "", "", 0f, 0f)

    fun setTriNumber(i: Int){myCurrentTriNumber = i}
    fun setDeductionNumber(i: Int){myCurrentDeductionNumber = i}

    fun setHeaderTable(tV1: TextView, tV2: TextView, tV3: TextView, tV4: TextView, tV5: TextView, tV6: TextView, tV7: TextView, tp: TitleParams){
        tV1.setText(tp.n)
        tV2.setText(tp.name)
        tV3.setText(tp.a)
        tV4.setText(tp.b)
        tV5.setText(tp.c)
        tV6.setText(tp.pn)
        tV7.setText(tp.pl)
    }

    fun scroll(movement: Int, myList: EditList, secondline: EditTextViewLine, thirdline: EditTextViewLine){
        val max: Int = myList.size()
        val min: Int = 1
        var current: Int = myList.getCurrent()
        val secondkeys: Keys = Keys(secondline.n.keyListener, secondline.name.keyListener, secondline.a.keyListener, secondline.b.keyListener, secondline.c.keyListener, secondline.pn.keyListener)
        val thirdkeys: Keys = Keys(thirdline.n.keyListener, thirdline.name.keyListener, thirdline.a.keyListener, thirdline.b.keyListener, thirdline.c.keyListener, thirdline.pn.keyListener)
        //keys = setKeyListener(keys, thirdline)

        if( (max > current && movement > 0) ||
          (current > min && movement < 0) ) {
            current = myList.addCurrent(movement)
            LineRewrite(myList.getParams(current), secondline)
            if(current == 1) {
                LineRewrite(Params("","",0,0f,0f,0f,0,0, PointXY(0f,0f),PointXY(0f,0f)), thirdline)
                //setLineEditable(false, thirdkeys, thirdline)
            }
            else {
//                setLineEditable(true, thirdkeys, thirdline)
                LineRewrite(myList.getParams(current - 1), thirdline)
            }
        }
}

    fun setKeyListener(keys: Keys, line: EditTextViewLine) :Keys{
        keys.n = line.n.keyListener
        keys.name = line.name.keyListener
        keys.a = line.a.keyListener
        keys.b = line.b.keyListener
        keys.c = line.c.keyListener
        keys.pn = line.pn.keyListener

        return keys
    }

    fun setLineEditable(bool: Boolean, keys: Keys, line: EditTextViewLine){

        if(bool == false) {
            line.n.keyListener = null
        }
        else {
            line.n.keyListener = keys.n
        }
    }

    fun LineRewrite(prm: Params, line: EditTextViewLine){

//        if(prm.n == 0)  line.n.setText("")
        //else
        line.n.setText(prm.n.toString())
        line.name.setText(prm.name.toString())

        if(prm.a == 0f) line.a.setText("")
        else            line.a.setText(prm.a.toString())
        if(prm.b == 0f) line.b.setText("")
        else            line.b.setText(prm.b.toString())
        if(prm.c == 0f) line.c.setText("")
        else            line.c.setText(prm.c.toString())
        if(prm.pn == 0) line.pn.setText("")
        else            line.pn.setText(prm.pn.toString())
        line.pl.setSelection(prm.pl)
    }

    fun ReadLineTo(prm: Params, line: EditTextViewLine) :Params {

        var sa: String = line.a.text.toString()
        var sb: String = line.b.text.toString()
        var sc: String = line.c.text.toString()
        if(sa == "") sa = "0.0"
        if(sb == "") sb = "0.0"
        if(sc == "") sc = "0.0"

        var sn: String = line.n.text.toString()
        var spn: String = line.pn.text.toString()
        if (sn == "") sn = "0"
        if (spn == "") spn = "0"

        prm.n = sn.toInt()
        prm.pn = spn.toInt()

        prm.name = line.name.text.toString()
        prm.a = sa.toFloat()
        prm.b = sb.toFloat()
        prm.c = sc.toFloat()
        prm.pl = line.pl.selectedItemPosition
        prm.type = line.pl.selectedItem.toString()

        return prm.copy()
    }


}