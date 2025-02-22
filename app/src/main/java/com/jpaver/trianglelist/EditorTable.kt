package com.jpaver.trianglelist.util

import android.text.method.KeyListener
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import com.jpaver.trianglelist.EditList
import com.example.trilib.PointXY

data class DeductionParams(var num: Int, var name: String, var lengthX: Float, var lengthY: Float, var parentNum: Int, var type: String, var angle: Float, var point: com.example.trilib.PointXY)

data class TitleParams(var type: Int, var n: Int, var name: Int, var a: Int, var b: Int, var c: Int, var pn: Int, var pl: Int)

class TitleParamStr(var type: String="", var n: String="", var name: String="", var a: String="", var b: String="", var c: String="", var pn: String="", var pl: String="")


data class EditTextViewLine(var n: EditText, var name: EditText, var a: EditText, var b: EditText, var c: EditText, var pn: EditText, var pl: Spinner)

data class Keys(var n: KeyListener, var name: KeyListener, var a: KeyListener, var b: KeyListener, var c: KeyListener, var pn: KeyListener)

data class InputParameter(var name: String = "",
                          var type: String = "",
                          var number: Int = 0,
                          var a: Float = 0f,
                          var b: Float = 0f,
                          var c: Float = 0f,
                          var pn: Int = 0,
                          var pl: Int = 0,
                          var point: com.example.trilib.PointXY = com.example.trilib.PointXY(
                              0f,
                              0f
                          ),
                          var pointflag: com.example.trilib.PointXY = com.example.trilib.PointXY(
                              0f,
                              0f
                          )
)

class EditorTable {
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
            write(myList.getParams(current), secondly)
            if(current == 1) {
                write(
                    InputParameter("","",0,0f,0f,0f,0,0,
                        com.example.trilib.PointXY(0f, 0f),
                        com.example.trilib.PointXY(0f, 0f)
                ), thirdly)
            }
            else {
                write(myList.getParams(current - 1), thirdly)
            }
        }
}

    fun Float?.formattedString(fractionDigits:Int): String{
        // nullの場合は空文字
        if(this == null) return ""
        val format = "%.${fractionDigits}f"
        return format.format(this)
    }

    fun write(prm: InputParameter, line: EditTextViewLine){

        line.n.setText(prm.number.toString())
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

    fun read(inputParams: InputParameter, line: EditTextViewLine): InputParameter {
        // より簡潔な方法で空文字をデフォルト値に置き換える
        fun String.defaultIfEmpty(default: String): String = if (this.isEmpty()) default else this

        // 入力されたテキストを適切な型に変換するヘルパー関数
        fun String.toIntOrDefault(): Int = this.defaultIfEmpty("0").toInt()
        fun String.toFloatOrDefault(): Float = toFloatIgnoreDot(this.defaultIfEmpty("0.0"))

        // パラメータを読み取り、適切な型に変換
        with(inputParams) {
            number = line.n.text.toString().toIntOrDefault()
            pn = line.pn.text.toString().toIntOrDefault()
            name = line.name.text.toString()
            a = line.a.text.toString().toFloatOrDefault()
            b = line.b.text.toString().toFloatOrDefault()
            c = line.c.text.toString().toFloatOrDefault()
            pl = line.pl.selectedItemPosition
            type = line.pl.selectedItem.toString()
        }

        // 元のオブジェクトのコピーを返す
        return inputParams.copy()
    }

}