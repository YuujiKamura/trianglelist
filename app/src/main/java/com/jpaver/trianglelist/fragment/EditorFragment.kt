
package com.jpaver.trianglelist.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.jpaver.trianglelist.R
import com.jpaver.trianglelist.databinding.FragmentFirstBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */

class EditorFragment : Fragment() {

    private lateinit var binding: FragmentFirstBinding


    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_first, container, false)
        Log.d( "myView", "Instance Check in FirstFragment inflater: " + view )

        binding = FragmentFirstBinding.bind( view )
        //Log.d( "myView", "Instance Check in FirstFragment: " + bMyView.myView )


        // Inflate the layout for this fragment
        return view

    }



}
