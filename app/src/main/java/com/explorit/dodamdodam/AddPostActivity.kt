package com.explorit.dodamdodam

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.explorit.dodamdodam.databinding.ActivityAddPostBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.util.Date

class AddPostActivity : AppCompatActivity() {
    private val PICK_IMAGE_FROM_ALBUM = 0
    private lateinit var binding: ActivityAddPostBinding
    private lateinit var storage: FirebaseStorage
    private lateinit var firestore: FirebaseFirestore
    private var photoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase 초기화
        storage = FirebaseStorage.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Photo Upload 버튼 클릭 이벤트 설정
        binding.addpostBtnUpload.setOnClickListener {
            openAlbum()
        }
    }

    // 앨범 열기
    private fun openAlbum() {
        val photoPickerIntent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_FROM_ALBUM && resultCode == Activity.RESULT_OK) {
            photoUri = data?.data
            binding.addpostImage.setImageURI(photoUri)
        } else {
            finish()
        }
    }

    // 사진 업로드 함수
    private fun contentUpload() {
        photoUri?.let { uri ->
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val imageFileName = "IMAGE_$timestamp.png"

            // Get a reference to the storage
            val storageRef: StorageReference = storage.reference.child("images").child(imageFileName)

            // File upload
            storageRef.putFile(uri).addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    saveFileUrlToFirestore(downloadUrl.toString())
                }
                Toast.makeText(this, "업로드 성공", Toast.LENGTH_LONG).show()
            }.addOnFailureListener {
                Toast.makeText(this, "업로드 실패", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Firestire에 URL 저장
    private fun saveFileUrlToFirestore(downloadUrl: String) {
        val postData = hashMapOf(
            "imageUrl" to downloadUrl,
            "timestamp" to System.currentTimeMillis(),
            "userId" to "User ID", // 사용자 ID 추가
            "explain" to binding.addpostEditExplain.text.toString() // 설명 추가
        )
        firestore.collection("posts").add(postData).addOnSuccessListener {
            Toast.makeText(this, getString(R.string.upload_success), Toast.LENGTH_LONG).show()
            finish() // 업로드 성공 후 액티비티 종료
        }.addOnFailureListener {
            Toast.makeText(this, getString(R.string.upload_failed), Toast.LENGTH_LONG).show()
        }
    }
}