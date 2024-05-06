
package com.jpaver.trianglelist.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.jpaver.trianglelist.R
import com.jpaver.trianglelist.SharedViewModel
import com.jpaver.trianglelist.databinding.FragmentFirstBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */

class EditorFragment : Fragment() {

    private lateinit var binding: FragmentFirstBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment using view binding
        binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Setup listeners or modify views here
        binding.root.post {
            val tableHeight = binding.root.findViewById<TableLayout>(R.id.editTextLayout)?.height ?: 0
            ViewModelProvider(requireActivity())[SharedViewModel::class.java].setTableHeight(tableHeight)
        }
    }
}

