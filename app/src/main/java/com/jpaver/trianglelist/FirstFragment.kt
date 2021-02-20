
package com.jpaver.trianglelist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */

class FirstFragment : Fragment() {



    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
/*
        var myTriangleList: TriangleList = TriangleList(Triangle(3f, 4f,5f, PointXY(0f, 0f), 180f))

        myTriangleList.add(Triangle(myTriangleList.getTriangle(1), 2, 3f, 4f))
        //myTriangleList.scale(PointXY(0f,0f),1f)
        my_view.setTriangleList(myTriangleList, 5f)
        my_view.setDP(myTriangleList.getTriangle(2).getPointCenter())

        var ParentTriNum: Int = 1
        var CurrentTriNum: Int = 2
        var NewTriNum: Int = 3

        view.findViewById<EditText>(R.id.triNumber).setText(NewTriNum.toString())
        view.findViewById<EditText>(R.id.editText).setText("0")
        view.findViewById<EditText>(R.id.editText2).setText("0")
        view.findViewById<EditText>(R.id.editText3).setText("0")
        view.findViewById<EditText>(R.id.ParentNumber).setText(CurrentTriNum.toString())

        view.findViewById<EditText>(R.id.triNumber2).setText(myTriangleList.getTriangle(2).getMyNumber().toString())
        view.findViewById<EditText>(R.id.editText4).setText(myTriangleList.getTriangle(2).getLengthA().toString())
        view.findViewById<EditText>(R.id.editText5).setText(myTriangleList.getTriangle(2).getLengthB().toString())
        view.findViewById<EditText>(R.id.editText6).setText(myTriangleList.getTriangle(2).getLengthC().toString())
        view.findViewById<EditText>(R.id.ParentNumber2).setText(myTriangleList.getTriangle(2).getParentNumber().toString())
        view.findViewById<Spinner>(R.id.editParent2).setSelection(myTriangleList.getTriangle(2).getParentBC())

        view.findViewById<EditText>(R.id.triNumber3).setText(myTriangleList.getTriangle(1).getMyNumber().toString())
        view.findViewById<EditText>(R.id.editText7).setText(myTriangleList.getTriangle(1).getLengthA().toString())
        view.findViewById<EditText>(R.id.editText8).setText(myTriangleList.getTriangle(1).getLengthB().toString())
        view.findViewById<EditText>(R.id.editText9).setText(myTriangleList.getTriangle(1).getLengthC().toString())
        view.findViewById<EditText>(R.id.ParentNumber3).setText(myTriangleList.getTriangle(1).getParentNumber().toString())
        view.findViewById<Spinner>(R.id.editParent3).setSelection(myTriangleList.getTriangle(1).getParentBC())
*/

        //view.findViewById<Button>(R.id.button_first).setOnClickListener {
          //  findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        //}
    }
}
