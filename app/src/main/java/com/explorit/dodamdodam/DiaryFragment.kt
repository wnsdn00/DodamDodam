package com.explorit.dodamdodam

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

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

        binding.gallery.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.mainFrameLayout, GalleryFragment())
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
                        mapDocuments(querySnapshot.documents)
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
                        mapDocuments(querySnapshot.documents)
                    } else {
                        Log.d("DiaryFragment", "No more posts to load.")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("DiaryFragment", "Failed to load more data: ${e.message}")
                }
        } ?: Log.e("DiaryFragment", "Last document is null.")
    }

    private fun mapDocuments(documents: List<DocumentSnapshot>) {
        val items = mutableListOf<ContentDTO>()
        val fetchCount = documents.size
        var completedFetches = 0 // 닉네임 가져오기 완료 카운트

        for (document in documents) {
            val userId = document.getString("userId") ?: ""
            val userNickname = document.getString("nickName") ?: ""
            val contentDTO = ContentDTO(
                documentId = document.id,
                userId = userId,
                nickName = userNickname,
                explain = document.getString("explain") ?: "",
                imageUrl = document.getString("imageUrl") ?: "",
                timestamp = document.getLong("timestamp") ?: System.currentTimeMillis(),
                familyCode = document.getString("familyCode") ?: ""
            )

            items.add(contentDTO)
            diaryAdapter.updatePostList(items)
        }
    }

    private fun fetchnickName(userId: String, callback: (String?) -> Unit) {
        currentUserFamilyCode?.let { familyCode ->
            database.child("families").child(familyCode).child("members").child(userId).child("nickname").get()
                .addOnSuccessListener { dataSnapshot ->
                    if (dataSnapshot.exists()) {
                        val nickname = dataSnapshot.value as? String
                        Log.d("DiaryFragment", "Fetched nickname for userId $userId: $nickname")
                        callback(nickname)
                    } else {
                        Log.e("DiaryFragment", "Nickname not found for userId: $userId")
                        callback(null)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("DiaryFragment", "Failed to fetch nickname for userId: $userId, error: ${e.message}")
                    callback(null)
                }
        } ?: callback(null)
    }

    private fun formatTimestampToDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    data class ContentDTO(
        var documentId: String? = null,
        var userId: String? = null,
        var nickName: String? = null,
        var explain: String? = null,
        var imageUrl: String? = null,
        var timestamp: Long = System.currentTimeMillis(),
        var profileImageUrl: String? = null,
        var familyCode: String = "",
        var likeCount: Int = 0
    ) {
        override fun toString(): String {
            return "ContentDTO(documentId=$documentId, userId=$userId, nickName=$nickName, " +
                    "explain=$explain, imageUrl=$imageUrl, timestamp=$timestamp, " +
                    "profileImageUrl=$profileImageUrl, familyCode=$familyCode, likeCount=$likeCount)"
        }
    }
}