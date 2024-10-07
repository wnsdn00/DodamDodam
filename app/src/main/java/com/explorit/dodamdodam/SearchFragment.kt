package com.explorit.dodamdodam

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.explorit.dodamdodam.databinding.FragmentSearchBinding
import com.google.firebase.firestore.FirebaseFirestore

class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    private lateinit var searchAdapter: SearchAdapter
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        requireActivity().window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        )
        binding = FragmentSearchBinding.inflate(inflater, container, false)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        searchAdapter = SearchAdapter()
        binding.recyclerView.adapter = searchAdapter

        binding.back.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.userSearchButton.setOnClickListener {
            val query = binding.userSearch.text.toString()
            if (query.isNotEmpty()) {
                searchByUser(query)
            } else {
                Toast.makeText(requireContext(), "작성자를 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }

        binding.dateSearchButton.setOnClickListener {
            val query = binding.dateSearch.text.toString()
            if (query.isNotEmpty()) {
                searchByDate(query)
            } else {
                Toast.makeText(requireContext(), "날짜를 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }

        binding.userSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.userSearch.text.toString()
                if (query.isNotEmpty()) {
                    searchByUser(query)
                } else {
                    Toast.makeText(requireContext(), "작성자를 입력하세요", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }

        return binding.root
    }

    private fun searchByUser(nickName: String) {
        firestore.collection("posts")
            .whereEqualTo("nickName", nickName)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(requireContext(), "해당 작성자의 게시물이 없습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    val filteredPosts = documents.map { document ->
                        document.toObject(DiaryFragment.ContentDTO::class.java)
                    }
                    searchAdapter.updatePostList(filteredPosts)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "검색 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun searchByDate(date: String) {
        firestore.collection("posts")
            .whereEqualTo("date", date)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(requireContext(), "해당 날짜의 게시물이 없습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    val filteredPosts = documents.map { document ->
                        document.toObject(DiaryFragment.ContentDTO::class.java)
                    }
                    searchAdapter.updatePostList(filteredPosts)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "검색 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
    }
}