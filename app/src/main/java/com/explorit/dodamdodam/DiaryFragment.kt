package com.explorit.dodamdodam

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.activity.addCallback
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
import com.google.firebase.Timestamp

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

        binding.back.setOnClickListener {
            // 메인으로 가는 버튼 함수
            (activity as? MainPageActivity)?.setFragment(TAG_HOME, HomeFragment(), false)
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            // 기기의 뒤로 가기 버튼 클릭 함수(메인으로)
            (activity as? MainPageActivity)?.setFragment(TAG_HOME, HomeFragment(), false)
        }

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

        for (document in documents) {
            val userId = document.getString("userId") ?: ""
            val userNickname = document.getString("nickName") ?: ""
            val userProfileImageUrl = document.getString("profileImageUrl") ?: "https://firebasestorage.googleapis.com/v0/b/dodamdodam-b1e37.appspot.com/o/profileImages%2Fic_profile.png?alt=media&token=8b78b600-1fa7-410c-8674-d6ec06e21e7a"
            val currentLikeCount = document.getLong("likeCount")?.toInt() ?: 0

            val contentDTO = ContentDTO(
                documentId = document.id,
                userId = userId,
                nickName = userNickname,
                explain = document.getString("explain") ?: "",
                imageUrl = document.getString("imageUrl") ?: "",
                videoUrl = document.getString("videoUrl") ?: "",
                timestamp = Timestamp.now(),
                familyCode = document.getString("familyCode") ?: "",
                profileImageUrl = userProfileImageUrl,
                likeCount = currentLikeCount
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

    data class ContentDTO(
        var documentId: String? = null,
        var userId: String? = null,
        var nickName: String? = null,
        var explain: String? = null,
        var imageUrl: String? = null,
        var videoUrl: String? = null,
        var timestamp: Timestamp = Timestamp.now(),
        var profileImageUrl: String? = null,
        var familyCode: String = "",
        var likeCount: Int = 0,
    ) {
        override fun toString(): String {
            return "ContentDTO(documentId=$documentId, userId=$userId, nickName=$nickName, " +
                    "explain=$explain, imageUrl=$imageUrl, videoUrl=$videoUrl, timestamp=$timestamp, " +
                    "profileImageUrl=$profileImageUrl, familyCode=$familyCode, likeCount=$likeCount)"
        }
    }
}