package com.explorit.dodamdodam

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.explorit.dodamdodam.databinding.FragmentSearchBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    private lateinit var searchAdapter: SearchAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var database: DatabaseReference
    private var currentUserFamilyCode: String? = null

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

        database = FirebaseDatabase.getInstance().reference
        fetchCurrentUserFamilyCode()

        val searchTypes = arrayOf("검색 기준", "작성자", "작성 날짜", "내용")
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, searchTypes) // 커스텀 레이아웃 사용
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item) // 드롭다운 아이템 레이아웃
        binding.searchTypeSpinner.adapter = adapter

        binding.searchButton.setOnClickListener {
            val searchType = binding.searchTypeSpinner.selectedItem.toString()
            val query = binding.searchInput.text.toString()

            if (query.isNotEmpty()) {
                when (searchType) {
                    "검색 기준" -> Toast.makeText(requireContext(), "검색 기준을 선택해 주세요", Toast.LENGTH_SHORT).show()
                    "작성자" -> searchByUser(query)
                    "작성 날짜" -> searchByDate(query)
                    "내용" -> searchByContent(query)
                }
            } else {
                Toast.makeText(requireContext(), "검색어를 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }
        binding.back.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return binding.root
    }

    private fun fetchCurrentUserFamilyCode() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: return

        // Realtime Database에서 familyCode 가져오기
        database.child("users").child(userId).child("familyCode").get()
            .addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    currentUserFamilyCode = dataSnapshot.value as? String
                    Log.d("DiaryFragment", "Current user familyCode: $currentUserFamilyCode")
                } else {
                    Log.e("DiaryFragment", "familyCode가 존재하지 않습니다.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("DiaryFragment", "Failed to fetch user's familyCode: ${e.message}")
            }
    }

    private fun searchByUser(nickName: String) {
        if (currentUserFamilyCode == null) {
            Toast.makeText(requireContext(), "가족 그룹 정보를 불러오는 중입니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("posts")
            .whereEqualTo("nickName", nickName)
            .whereEqualTo("familyCode", currentUserFamilyCode)
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

    private fun searchByDate(dateString: String) {
        if (currentUserFamilyCode == null) {
            Toast.makeText(requireContext(), "가족 그룹 정보를 불러오는 중입니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormat.parse(dateString)

            // startOfDay와 endOfDay를 Timestamp로 변환
            val startOfDay = Calendar.getInstance().apply {
                time = date!!
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            val endOfDay = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.time

            val startTimestamp = Timestamp(startOfDay)
            val endTimestamp = Timestamp(endOfDay)

            // 먼저 familyCode로 필터링
            firestore.collection("posts")
                .whereEqualTo("familyCode", currentUserFamilyCode)
                .get()
                .addOnSuccessListener { documents ->
                    // familyCode로 필터링된 게시물 리스트에서 날짜로 다시 필터링
                    val filteredPosts = documents.mapNotNull { document ->
                        val post = document.toObject(DiaryFragment.ContentDTO::class.java)
                        // 날짜가 일치하는지 확인
                        if (post.timestamp in startTimestamp..endTimestamp) {
                            post
                        } else {
                            null
                        }
                    }

                    // 필터링된 게시물 리스트 업데이트
                    if (filteredPosts.isEmpty()) {
                        Toast.makeText(requireContext(), "해당 날짜의 게시물이 없습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        searchAdapter.updatePostList(filteredPosts)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "검색 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "날짜 형식이 잘못되었습니다. yyyy-MM-dd 형식으로 입력하세요.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun searchByContent(content: String) {
        // 현재 사용자의 가족 코드와 일치하는 게시물만 가져오기
        firestore.collection("posts")
            .whereEqualTo("familyCode", currentUserFamilyCode)
            .get()
            .addOnSuccessListener { documents ->
                // 가족 코드로 필터링된 게시물 리스트에서 내용으로 다시 필터링
                val filteredPosts = documents.mapNotNull { document ->
                    val post = document.toObject(DiaryFragment.ContentDTO::class.java)
                    // 게시물 내용이 입력한 단어를 포함하는지 확인
                    if (post.explain?.contains(content, ignoreCase = true) == true) {
                        post // 포함될 경우 게시물 추가
                    } else {
                        null // 포함되지 않을 경우 null 반환
                    }
                }

                // 필터링된 게시물 리스트 업데이트
                if (filteredPosts.isEmpty()) {
                    Toast.makeText(requireContext(), "해당 내용의 게시물이 없습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    searchAdapter.updatePostList(filteredPosts)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "검색 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
    }


}