package com.explorit.dodamdodam

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.explorit.dodamdodam.databinding.FragmentDiaryBinding
import com.explorit.dodamdodam.databinding.FragmentDiaryDetailBinding
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class DiaryFragment : Fragment() {

    private var firestore: FirebaseFirestore? = null
    private lateinit var binding: FragmentDiaryBinding
    private lateinit var diaryAdapter: DiaryAdapter
    private lateinit var layoutManager: LinearLayoutManager

    private var lastVisibleDocument: DocumentSnapshot? = null
    private val pageSize = 10

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentDiaryBinding.inflate(inflater, container, false)

        firestore = FirebaseFirestore.getInstance()
        layoutManager = LinearLayoutManager(activity)
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

    private fun loadInitialData() {
        firestore?.collection("posts")?.orderBy("timestamp", Query.Direction.DESCENDING)
            ?.limit(pageSize.toLong())
            ?.get()
            ?.addOnSuccessListener { querySnapshot ->
                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    lastVisibleDocument = querySnapshot.documents[querySnapshot.size() - 1]
                    val items = querySnapshot.toObjects(ContentDTO::class.java)
                    diaryAdapter.addItems(items)
                }
            }
    }

    private fun loadMoreData() {
        if (lastVisibleDocument != null) {
            firestore?.collection("posts")?.orderBy("timestamp", Query.Direction.DESCENDING)
                ?.startAfter(lastVisibleDocument!!)
                ?.limit(pageSize.toLong())
                ?.get()
                ?.addOnSuccessListener { querySnapshot ->
                    if (querySnapshot != null && !querySnapshot.isEmpty) {
                        lastVisibleDocument = querySnapshot.documents[querySnapshot.size() - 1]
                        val items = querySnapshot.toObjects(ContentDTO::class.java)
                        diaryAdapter.addItems(items)
                    }
                }
        }
    }

    inner class DiaryAdapter : RecyclerView.Adapter<DiaryAdapter.DiaryViewHolder>() {
        private val contentDTOs: ArrayList<ContentDTO> = arrayListOf()

        inner class DiaryViewHolder(private val binding: FragmentDiaryDetailBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(contentDTO: ContentDTO) {
                binding.userProfileName.text = contentDTO.userId
                Glide.with(binding.root.context).load(contentDTO.imageUrl).into(binding.userPost)
                binding.userPostExplanation.text = contentDTO.explain
                binding.userFavoriteCounter.text = "Likes ${contentDTO.favoriteCount}"
                Glide.with(binding.root.context).load(contentDTO.imageUrl).into(binding.userProfileImage)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryViewHolder {
            val binding = FragmentDiaryDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return DiaryViewHolder(binding)
        }

        override fun onBindViewHolder(holder: DiaryViewHolder, position: Int) {
            holder.bind(contentDTOs[position])
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
    var favoriteCount: Int = 0
)