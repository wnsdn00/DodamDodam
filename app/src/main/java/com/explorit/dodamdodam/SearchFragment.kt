package com.explorit.dodamdodam

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.explorit.dodamdodam.databinding.FragmentSearchBinding

class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    private lateinit var searchAdapter: SearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        searchAdapter = SearchAdapter()
        binding.recyclerView.adapter = searchAdapter

        binding.back.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.searchButton1.setOnClickListener {
        }

        binding.searchButton2.setOnClickListener {
        }

        binding.searchButton1.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.searchButton1.text.toString()
                filterResults(query)
                true
            } else {
                false
            }
        }

        binding.searchButton1.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.recyclerView.post {
                    binding.recyclerView.scrollToPosition(0)
                }
            }
        }

        return binding.root
    }

    private fun filterResults(query: String) {
        searchAdapter.filter(query)
    }
}