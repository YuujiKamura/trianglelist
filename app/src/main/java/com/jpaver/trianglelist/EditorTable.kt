package com.jpaver.trianglelist.util

import android.text.method.KeyListener
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import com.jpaver.trianglelist.EditList
import com.jpaver.trianglelist.PointXY

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
                  var pt: PointXY = PointXY(
                      0f,
                      0f
                  ),
                  var ptF: PointXY = PointXY(
                      0f,
                      0f
                  )
)//,
//                  var an: Float = 0f)

class EditorTable {
    //var dTP: TriParams = TriParams(0f, 0f, 0f, "", 0, 0, 0)
    //var dDP: DeductionParams = DeductionParams(PointXY(0f,0f), 0f, 0f, "", "", 0f, 0f)

    fun setHeaderTable(tV1: TextView, tV2: TextView, tV3: TextView, tV4: TextView, tV5: TextView, tV6: TextView, tV7: TextView, tp: TitleParams){
        tV1.setText(tp.n)
        tV2.setText(tp.name)
        tV3.setText(tp.a)
        tV4.setText(tp.b)
        tV5.setText(tp.c)
        tV6.setText(tp.pn)
        tV7.setText(tp.pl)
    }

    fun scroll(movement: Int, myList: EditList, secondly: EditTextViewLine, thirdly: EditTextViewLine){
        val max: Int = myList.size()
        val min = 1
        var current: Int = myList.retrieveCurrent()
        Keys(secondly.n.keyListener, secondly.name.keyListener, secondly.a.keyListener, secondly.b.keyListener, secondly.c.keyListener, secondly.pn.keyListener)
        Keys(thirdly.n.keyListener, thirdly.name.keyListener, thirdly.a.keyListener, thirdly.b.keyListener, thirdly.c.keyListener, thirdly.pn.keyListener)
        //keys = setKeyListener(keys, thirdly)

        if( (max > current && movement > 0) ||
          (current > min && movement < 0) ) {
            current = myList.addCurrent(movement)
            lineRewrite(myList.getParams(current), secondly)
            if(current == 1) {
                lineRewrite(
                    Params("","",0,0f,0f,0f,0,0,
                    PointXY(0f, 0f),
                    PointXY(0f, 0f)
                ), thirdly)
                //setLineEditable(false, thirdly, thirdly)
            }
            else {
//                setLineEditable(true, thirdly, thirdly)
                lineRewrite(myList.getParams(current - 1), thirdly)
            }
        }
}

    fun Float?.formattedString(fractionDigits:Int): String{
        // nullの場合は空文字
        if(this == null) return ""
        val format = "%.${fractionDigits}f"
        return format.format(this)
    }

    fun lineRewrite(prm: Params, line: EditTextViewLine){

//        if(prm.n == 0)  line.n.setText("")
        //else
        line.n.setText(prm.n.toString())
        line.name.setText(prm.name)

        if(prm.a == 0f) line.a.setText("")
        else            line.a.setText(prm.a.formattedString(2))
        if(prm.b == 0f) line.b.setText("")
        else            line.b.setText(prm.b.formattedString(2))
        if(prm.c == 0f) line.c.setText("")
        else            line.c.setText(prm.c.formattedString(2))
        if( prm.pn == 0 || prm.pn == -1 ) line.pn.setText("")
        else            line.pn.setText(prm.pn.toString())
         line.pl.setSelection(prm.pl)
    }

    fun toFloatIgnoreDot(str: String ): Float {
        if( str == "." ) return 0f
        return str.toFloat()
    }

    fun readLineTo(prm: Params, line: EditTextViewLine) : Params {

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
        prm.a = toFloatIgnoreDot(sa)
        prm.b = toFloatIgnoreDot(sb)
        prm.c = toFloatIgnoreDot(sc)
        prm.pl = line.pl.selectedItemPosition
        prm.type = line.pl.selectedItem.toString()

        return prm.copy()
    }


}