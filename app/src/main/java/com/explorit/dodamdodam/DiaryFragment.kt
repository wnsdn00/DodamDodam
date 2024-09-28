package com.explorit.dodamdodam

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import com.explorit.dodamdodam.databinding.FragmentDiaryBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class DiaryFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var binding: FragmentDiaryBinding
    private lateinit var diaryAdapter: DiaryAdapter
    private var postData: MutableList<ContentDTO> = mutableListOf()
    private var lastVisibleDocument: DocumentSnapshot? = null
    private val pageSize = 10
    private lateinit var database: DatabaseReference
    private var currentUserFamilyCode: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentDiaryBinding.inflate(inflater, container, false)

        EventBus.getDefault().register(this)

        firestore = FirebaseFirestore.getInstance()
        database = FirebaseDatabase.getInstance().reference
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        diaryAdapter = DiaryAdapter(postData, requireContext())
        binding.recyclerView.adapter = diaryAdapter

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1)) {
                    loadMoreData()
                }
            }
        })

        binding.addPost.setOnClickListener {
            startActivity(Intent(activity, AddPostActivity::class.java))
        }

        binding.search.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.mainFrameLayout, SearchFragment())
                .addToBackStack(null)
                .commit()
        }

        fetchCurrentUserFamilyCode()

        return binding.root
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNewPostEvent(event: NewPostEvent) {
        loadInitialData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        EventBus.getDefault().unregister(this)
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
                    loadInitialData()
                } else {
                    Log.e("DiaryFragment", "familyCode가 존재하지 않습니다.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("DiaryFragment", "Failed to fetch user's familyCode: ${e.message}")
            }
    }

    private fun loadInitialData() {
        if (currentUserFamilyCode.isNullOrEmpty()) {
            Log.e("DiaryFragment", "Family code is null or empty.")
            return
        }

        Log.d("DiaryFragment", "Loading data for familyCode: $currentUserFamilyCode")
        firestore.collection("posts")
            .whereEqualTo("familyCode", currentUserFamilyCode)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(pageSize.toLong())
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot != null) {
                    Log.d("DiaryFragment", "Total documents found: ${querySnapshot.size()}")
                    if (querySnapshot.documents.isNotEmpty()) {
                        Log.d("DiaryFragment", "Loaded documents: ${querySnapshot.documents.map { it.data }}")
                        lastVisibleDocument = querySnapshot.documents.last()
                        val items = mapDocuments(querySnapshot.documents)
                        diaryAdapter.updatePostList(items)
                        Log.d("DiaryFragment", "Successfully loaded posts: ${items.size} posts")
                    } else {
                        Log.d("DiaryFragment", "No posts found.")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("DiaryFragment", "Failed to load data: ${e.message}")
            }
    }

    private fun loadMoreData() {
        if (currentUserFamilyCode.isNullOrEmpty()) {
            Log.e("DiaryFragment", "Family code is null or empty.")
            return
        }

        lastVisibleDocument?.let { document ->
            firestore.collection("posts")
                .whereEqualTo("familyCode", currentUserFamilyCode)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(document)
                .limit(pageSize.toLong())
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot != null && querySnapshot.documents.isNotEmpty()) {
                        lastVisibleDocument = querySnapshot.documents.last()
                        val items = mapDocuments(querySnapshot.documents)
                        diaryAdapter.addItems(items)
                    } else {
                        Log.d("DiaryFragment", "No more posts to load.")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("DiaryFragment", "Failed to load more data: ${e.message}")
                }
        } ?: Log.e("DiaryFragment", "Last document is null.")
    }

    private fun mapDocuments(documents: List<DocumentSnapshot>): List<ContentDTO> {
        return documents.map { document ->
            ContentDTO(
                documentId = document.id,
                userId = document.getString("userId") ?: "",
                explain = document.getString("explain") ?: "",
                imageUrl = document.getString("imageUrl") ?: "",
                timestamp = document.getLong("timestamp") ?: System.currentTimeMillis(),
                familyCode = document.getString("familyCode") ?: ""
            )
        }
    }

    private fun onSearchButtonClick() {
        // SearchFragment로 이동
        val searchFragment = SearchFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, searchFragment) // fragment_container는 실제 컨테이너 ID로 변경
            .addToBackStack(null)
            .commit()
    }

    data class ContentDTO(
        var documentId: String? = null,
        var userId: String? = null,
        var explain: String? = null,
        var imageUrl: String? = null,
        var timestamp: Long = System.currentTimeMillis(),
        var profileImageUrl: String? = null,
        var familyCode: String = ""
    ) {
        override fun toString(): String {
            return "ContentDTO(documentId=$documentId, userId=$userId, explain=$explain, imageUrl=$imageUrl, timestamp=$timestamp, profileImageUrl=$profileImageUrl, familyCode=$familyCode)"
        }
    }
}