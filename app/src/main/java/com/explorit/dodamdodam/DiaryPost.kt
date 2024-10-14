package com.explorit.dodamdodam

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

fun addPost(explanation: String, imageUrl: String) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val database = FirebaseDatabase.getInstance().reference

    if (userId != null) {
        // 현재 사용자의 familyCode를 Realtime Database에서 가져옴
        database.child("users").child(userId).child("familyCode").get()
            .addOnSuccessListener { dataSnapshot ->
                val familyCode = dataSnapshot.value as? String ?: ""

                // 게시물 객체 생성
                val post = DiaryFragment.ContentDTO(
                    userId = userId,
                    explain = explanation,
                    imageUrl = imageUrl,
                    timestamp = Timestamp.now(),
                    familyCode = familyCode  // familyCode 추가
                )

                // Firestore에 게시물 추가
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("posts")  // "posts"로 컬렉션명 수정
                    .add(post)
                    .addOnSuccessListener { documentReference ->
                        val newPostId = documentReference.id
                        // 생성된 문서의 ID를 documentId 필드에 업데이트
                        firestore.collection("posts").document(newPostId)
                            .update("documentId", newPostId)
                    }
                    .addOnFailureListener { exception ->
                        Log.e("DiaryFragment", "게시글 추가 실패: ${exception.message}")
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("DiaryFragment", "사용자 정보 가져오기 실패: ${exception.message}")
            }
    } else {
        Log.e("DiaryFragment", "사용자가 로그인되어 있지 않습니다.")
    }
}