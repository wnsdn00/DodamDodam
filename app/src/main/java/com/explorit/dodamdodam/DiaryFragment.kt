package com.explorit.dodamdodam

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import com.explorit.dodamdodam.databinding.FragmentDiaryBinding

class DiaryFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var binding: FragmentDiaryBinding
    private lateinit var diaryAdapter: DiaryAdapter
    private var postData: MutableList<ContentDTO> = mutableListOf()
    private var lastVisibleDocument: DocumentSnapshot? = null
    private val pageSize = 10

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentDiaryBinding.inflate(inflater, container, false)

        EventBus.getDefault().register(this)

        firestore = FirebaseFirestore.getInstance()
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
            val intent = Intent(activity, AddPostActivity::class.java)
            startActivity(intent)
        }

        loadInitialData()
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

    private fun loadInitialData() {
        firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(pageSize.toLong())
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot != null && querySnapshot.documents.isNotEmpty()) {
                    lastVisibleDocument = querySnapshot.documents.last()
                    val items = querySnapshot.documents.map { document ->
                        ContentDTO(
                            documentId = document.id,
                            userId = document.getString("userId") ?: "",
                            explain = document.getString("explain") ?: "",
                            imageUrl = document.getString("imageUrl") ?: "",
                            timestamp = document.getLong("timestamp") ?: 0
                        )
                    }
                    diaryAdapter.updatePostList(items)
                }
            }
            .addOnFailureListener { e ->
                Log.e("DiaryFragment", "데이터 로드 실패: ${e.message}")
            }
    }

    private fun loadMoreData() {
        lastVisibleDocument?.let {
            firestore.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(it)
                .limit(pageSize.toLong())
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot != null && querySnapshot.documents.isNotEmpty()) {
                        lastVisibleDocument = querySnapshot.documents.last()
                        val items = querySnapshot.documents.map { document ->
                            ContentDTO(
                                documentId = document.id,
                                userId = document.getString("userId") ?: "",
                                explain = document.getString("explain") ?: "",
                                imageUrl = document.getString("imageUrl") ?: "",
                                timestamp = document.getLong("timestamp") ?: 0
                            )
                        }
                        diaryAdapter.addItems(items)
                    }
                }
        }
    }
}

data class ContentDTO(
    var documentId: String? = null,
    var userId: String = "",
    var explain: String? = "",
    var imageUrl: String = "",
    var timestamp: Long = 0,
    var profileImageUrl: String = ""
)