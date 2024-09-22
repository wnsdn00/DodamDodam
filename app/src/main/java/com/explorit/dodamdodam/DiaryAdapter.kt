package com.explorit.dodamdodam

import android.content.Context
import android.app.AlertDialog
import android.widget.EditText
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.explorit.dodamdodam.databinding.ItemDiaryBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

class DiaryAdapter(private var postData: MutableList<ContentDTO>, private val context: Context) :
    RecyclerView.Adapter<DiaryAdapter.DiaryViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    inner class DiaryViewHolder(private val binding: ItemDiaryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: ContentDTO) {
            binding.userId.text = post.userId
            binding.userPostExplanation.text = post.explain ?: ""
            Glide.with(binding.root.context).load(post.imageUrl).into(binding.userPost)

            if (post.userId == currentUserId) {
                binding.postDelete.visibility = View.VISIBLE
                binding.postEdit.visibility = View.VISIBLE
            } else {
                binding.postDelete.visibility = View.GONE
                binding.postEdit.visibility = View.GONE
            }

            binding.postDelete.setOnClickListener {
                val position = bindingAdapterPosition
                deletePost(post, position)
            }

            binding.postEdit.setOnClickListener {
                val position = bindingAdapterPosition
                updatePost(post, position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryViewHolder {
        val binding = ItemDiaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DiaryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DiaryViewHolder, position: Int) {
        holder.bind(postData[position])
    }

    override fun getItemCount(): Int = postData.size

    // 게시물 삭제 함수
    private fun deletePost(post: ContentDTO, position: Int) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("게시물 삭제")
            .setMessage("게시물을 삭제하시겠습니까?")
            .setPositiveButton("예") { _, _ ->
                val documentId = post.documentId ?: ""
                if (documentId.isNotEmpty()) {
                    // Firestore에서 게시물 삭제
                    firestore.collection("posts").document(documentId)
                        .delete()
                        .addOnSuccessListener {
                            postData.removeAt(position)
                            notifyItemRemoved(position)
                            Toast.makeText(context, "게시물 삭제 완료", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Log.e("DiaryAdapter", "게시물 삭제 실패: ${e.message}")
                            Toast.makeText(context, "게시물 삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "게시물 ID가 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("아니오") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    // 게시물 수정 함수
    private fun updatePost(post: ContentDTO, position: Int) {
        val builder = AlertDialog.Builder(context)
        val input = EditText(context).apply {
            setText(post.explain)
        }
        builder.setView(input)

        builder.setTitle("게시물 수정")
            .setMessage("새로운 설명을 입력하세요.")
            .setPositiveButton("확인") { _, _ ->
                val newExplanation = input.text.toString()
                val documentId = post.documentId ?: ""
                if (documentId.isNotEmpty()) {
                    // Firestore에서 게시물 수정
                    firestore.collection("posts").document(documentId)
                        .update("explain", newExplanation)
                        .addOnSuccessListener {
                            post.explain = newExplanation // 로컬 데이터 업데이트
                            notifyItemChanged(position) // 특정 아이템만 업데이트
                            Toast.makeText(context, "게시물이 수정되었습니다.", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Log.e("DiaryAdapter", "게시물 수정 실패: ${e.message}")
                            Toast.makeText(context, "게시물 수정 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "게시물 ID가 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소") { dialog, _ -> dialog.cancel() }

        builder.create().show()
    }

    fun addItems(newItems: List<ContentDTO>) {
        val startPosition = postData.size
        postData.addAll(newItems)
        notifyItemRangeInserted(startPosition, newItems.size)
    }

    fun updatePostList(newPostList: List<ContentDTO>) {
        postData.clear()
        postData.addAll(newPostList)
        notifyDataSetChanged()
    }
}