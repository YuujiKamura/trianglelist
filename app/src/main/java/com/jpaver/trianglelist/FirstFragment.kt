
package com.jpaver.trianglelist

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import com.jpaver.trianglelist.databinding.FragmentFirstBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */

class FirstFragment : Fragment() {

    private lateinit var bMyView: FragmentFirstBinding


    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_first, container, false)
        Log.d( "myView", "Instance Check in FirstFragment inflater: " + view )

        bMyView = FragmentFirstBinding.bind( view )
        Log.d( "myView", "Instance Check in FirstFragment: " + bMyView.myView )


        // Inflate the layout for this fragment
        return view

    }

}
