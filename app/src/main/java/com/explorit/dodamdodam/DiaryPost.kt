package com.explorit.dodamdodam

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

fun addPost(explanation: String, imageUrl: String) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val firestore = FirebaseFirestore.getInstance()

    if (userId != null) {
        val post = ContentDTO(
            userId = userId,
            explain = explanation,
            imageUrl = imageUrl,
            timestamp = System.currentTimeMillis()
        )

        firestore.collection("post")
            .add(post)
            .addOnSuccessListener { documentReference ->
                val newPostId = documentReference.id
                firestore.collection("post").document(newPostId)
                    .update("documentId", newPostId)
            }
            .addOnFailureListener { exception ->
                Log.e("DiaryFragment", "게시글 추가 실패: ${exception.message}")
            }
    } else {
        Log.e("DiaryFragment", "사용자가 로그인되어 있지 않습니다.")
    }
}