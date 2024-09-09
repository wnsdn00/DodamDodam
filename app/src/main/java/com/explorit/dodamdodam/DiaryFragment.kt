package com.explorit.dodamdodam

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.explorit.dodamdodam.databinding.FragmentDiaryBinding
import com.explorit.dodamdodam.databinding.FragmentDiaryDetailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class DiaryFragment : Fragment() {

    private var firestore: FirebaseFirestore? = null
    private lateinit var binding: FragmentDiaryBinding
    private lateinit var diaryAdapter: DiaryAdapter
    private var lastVisibleDocument: DocumentSnapshot? = null
    private val pageSize = 10

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentDiaryBinding.inflate(inflater, container, false)

        EventBus.getDefault().register(this)

        firestore = FirebaseFirestore.getInstance()
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.layoutManager = layoutManager

        diaryAdapter = DiaryAdapter()
        binding.recyclerView.adapter = diaryAdapter

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1)) {
                    loadMoreData()
                }
            }
        })

        // Set up the click listener for the add_post button
        binding.addPost.setOnClickListener {
            // Start AddPostActivity
            val intent = Intent(activity, AddPostActivity::class.java)
            startActivity(intent)
        }

        loadInitialData()
        return binding.root
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNewPostEvent(event: NewPostEvent) {
        // 새 게시물 이벤트를 받았을 때 데이터를 다시 로드하거나 추가
        loadInitialData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // EventBus에서 이 Fragment 등록 해제
        EventBus.getDefault().unregister(this)
    }

    private fun loadInitialData() {
        firestore?.collection("posts")
            ?.orderBy("timestamp", Query.Direction.DESCENDING)
            ?.limit(pageSize.toLong())
            ?.get()
            ?.addOnSuccessListener { querySnapshot ->
                if (querySnapshot != null && querySnapshot.documents.isNotEmpty()) {
                    lastVisibleDocument = querySnapshot.documents.last()
                    val items = querySnapshot.documents.map { document ->
                        ContentDTO(
                            explain = document.getString("explain"),
                            imageUrl = document.getString("imageUrl"),
                            userId = document.getString("userId"),
                            timestamp = document.getLong("timestamp"),
                            favoriteCount = document.getLong("favoriteCount")?.toInt() ?: 0,
                            documentId = document.id // 문서 ID를 포함
                        )
                    }
                    diaryAdapter.addItems(items)
                }
            }
    }

    private fun loadMoreData() {
        lastVisibleDocument?.let {
            firestore?.collection("posts")
                ?.orderBy("timestamp", Query.Direction.DESCENDING)
                ?.startAfter(it)
                ?.limit(pageSize.toLong())
                ?.get()
                ?.addOnSuccessListener { querySnapshot ->
                    if (querySnapshot != null && querySnapshot.documents.isNotEmpty()) {
                        lastVisibleDocument = querySnapshot.documents.last()
                        val items = querySnapshot.documents.map { document ->
                            ContentDTO(
                                explain = document.getString("explain"),
                                imageUrl = document.getString("imageUrl"),
                                userId = document.getString("userId"),
                                timestamp = document.getLong("timestamp"),
                                favoriteCount = document.getLong("favoriteCount")?.toInt() ?: 0,
                                documentId = document.id // 문서 ID를 포함
                            )
                        }
                        diaryAdapter.addItems(items)
                    }
                }
        }
    }

    inner class DiaryAdapter : RecyclerView.Adapter<DiaryAdapter.DiaryViewHolder>() {
        private val contentDTOs: ArrayList<ContentDTO> = arrayListOf()

        inner class DiaryViewHolder(private val binding: FragmentDiaryDetailBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(contentDTO: ContentDTO) {
                binding.userProfileName.text = contentDTO.userId
                Glide.with(binding.root.context).load(contentDTO.imageUrl).into(binding.userPost)
                binding.userPostExplanation.text = contentDTO.explain
                binding.userFavoriteCounter.text = "Likes ${contentDTO.favoriteCount}"
                Glide.with(binding.root.context).load(contentDTO.imageUrl)
                    .into(binding.userProfileImage)

            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryViewHolder {
            val binding = FragmentDiaryDetailBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return DiaryViewHolder(binding)
        }

        override fun onBindViewHolder(holder: DiaryViewHolder, position: Int) {
            val contentDTO = contentDTOs[position]
            holder.bind(contentDTO)
        }

        override fun getItemCount(): Int = contentDTOs.size

        fun addItems(newItems: List<ContentDTO>) {
            val startPosition = contentDTOs.size
            contentDTOs.addAll(newItems)
            notifyItemRangeInserted(startPosition, newItems.size)
        }
    }
}

data class ContentDTO(
    var explain: String? = null,
    var imageUrl: String? = null,
    var userId: String? = null,
    var timestamp: Long? = null,
    var favoriteCount: Int = 0,
    var documentId: String? = null
)