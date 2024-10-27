package com.explorit.dodamdodam

import android.content.Context
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.widget.EditText
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.explorit.dodamdodam.databinding.ItemDiaryBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

class DiaryAdapter(
    private var postData: MutableList<DiaryFragment.ContentDTO>,
    private val context: Context
) : RecyclerView.Adapter<DiaryAdapter.DiaryViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    inner class DiaryViewHolder(private val binding: ItemDiaryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: DiaryFragment.ContentDTO) {

            Glide.with(binding.root.context).load(post.profileImageUrl).into(binding.userProfile)
            binding.nickName.text = post.nickName
            binding.postExplain.text = post.explain ?: ""
            val mimeType = itemView.context.contentResolver.getType(Uri.parse(post.videoUrl))
            // 이미지 및 비디오 처리
            if (post.imageUrl?.isNotEmpty() == true) {
                binding.userPostImage.visibility = View.VISIBLE
                binding.userPostVideo.visibility = View.GONE
                Glide.with(binding.root.context).load(post.imageUrl).into(binding.userPostImage)
            } else if (post.videoUrl?.isNotEmpty() == true) {
                binding.userPostImage.visibility = View.GONE
                binding.userPostVideo.visibility = View.VISIBLE
                binding.userPostVideo.setVideoURI(Uri.parse(post.videoUrl))
                binding.userPostVideo.setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.isLooping = true
                    binding.userPostVideo.start()
                }
                binding.userPostVideo.start()
            } else {
                binding.userPostImage.visibility = View.GONE
                binding.userPostVideo.visibility = View.GONE
            }

            binding.postDelete.visibility =
                if (post.userId == currentUserId) View.VISIBLE else View.GONE
            binding.postEdit.visibility =
                if (post.userId == currentUserId) View.VISIBLE else View.GONE
            binding.divider.visibility =
                if (post.userId == currentUserId) View.VISIBLE else View.GONE

            binding.postDelete.setOnClickListener {
                val position = bindingAdapterPosition
                deletePost(post, position)
            }

            binding.postEdit.setOnClickListener {
                val position = bindingAdapterPosition
                updatePost(post, position)
            }

            binding.loveCounter.text = "like ${post.likeCount}"
            binding.loveButton.setOnClickListener {
                updateLikeCount(post, bindingAdapterPosition)
            }
        }

        private fun updateLikeCount(post: DiaryFragment.ContentDTO, position: Int) {
            val documentId = post.documentId ?: return
            val newLikeCount = post.likeCount + 1

            firestore.collection("posts").document(documentId)
                .update("likeCount", newLikeCount)
                .addOnSuccessListener {
                    postData[position].likeCount = newLikeCount
                    notifyItemChanged(position)
                    Log.d("DiaryAdapter", "Like count updated successfully: $documentId")
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Like 업데이트 실패", Toast.LENGTH_SHORT).show()
                    Log.e("DiaryAdapter", "Failed to update like count: ${e.message}")
                }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryViewHolder {
        val binding = ItemDiaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DiaryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DiaryViewHolder, position: Int) {
        val item = postData[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = postData.size

    // 게시물 삭제 함수
    private fun deletePost(post: DiaryFragment.ContentDTO, position: Int) {
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
                            Log.d("DiaryAdapter", "게시물 삭제 성공: $documentId")
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "게시물 삭제 실패", Toast.LENGTH_SHORT).show()
                            Log.e("DiaryAdapter", "게시물 삭제 실패: ${e.message}")
                        }
                }
            }
            .setNegativeButton("아니요", null)
            .show()
    }

    // 게시물 수정 함수
    private fun updatePost(post: DiaryFragment.ContentDTO, position: Int) {
        val editText = EditText(context)
        editText.setText(post.explain)

        val builder = AlertDialog.Builder(context)
        builder.setTitle("게시물 수정")
            .setView(editText)
            .setPositiveButton("확인") { _, _ ->
                val updatedExplain = editText.text.toString()
                val documentId = post.documentId ?: ""
                if (documentId.isNotEmpty()) {
                    firestore.collection("posts").document(documentId)
                        .update("explain", updatedExplain)
                        .addOnSuccessListener {
                            postData[position].explain = updatedExplain
                            notifyItemChanged(position)
                            Toast.makeText(context, "게시물 수정 완료", Toast.LENGTH_SHORT).show()
                            Log.d("DiaryAdapter", "게시물 수정 성공: $documentId")
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "게시물 수정 실패", Toast.LENGTH_SHORT).show()
                            Log.e("DiaryAdapter", "게시물 수정 실패: ${e.message}")
                        }
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    fun updatePostList(newPosts: List<DiaryFragment.ContentDTO>) {
        postData.clear()
        postData.addAll(newPosts)
        notifyDataSetChanged()
    }

    fun addItems(newPosts: List<DiaryFragment.ContentDTO>) {
        val startPosition = postData.size
        postData.addAll(newPosts)
        notifyItemRangeInserted(startPosition, newPosts.size)
    }
}