package com.explorit.dodamdodam

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.explorit.dodamdodam.databinding.ActivityAddPostBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.util.Date
import org.greenrobot.eventbus.EventBus

class AddPostActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_READ_EXTERNAL_STORAGE = 100
    }

    private lateinit var binding: ActivityAddPostBinding
    private lateinit var storage: FirebaseStorage
    private lateinit var firestore: FirebaseFirestore
    private var photoUri: Uri? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                photoUri = it
                binding.addpostImage.setImageURI(photoUri)
                Toast.makeText(this, "이미지가 선택되었습니다.", Toast.LENGTH_LONG).show()
            } ?: run {
                Toast.makeText(this, "이미지 선택이 취소되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase 초기화
        storage = FirebaseStorage.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Photo Upload 버튼 클릭
        binding.addpostBtnUpload.setOnClickListener {
            checkPermissionAndOpenAlbum()
        }

        // 게시글 업로드 버튼 클릭
        binding.uploadBtn.setOnClickListener {
            if (photoUri != null) {
                contentUpload()
            } else {
                Toast.makeText(this, "이미지를 선택하세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 권한 확인 및 앨범 열기
    private fun checkPermissionAndOpenAlbum() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                        REQUEST_READ_EXTERNAL_STORAGE
                    )
                } else {
                    openAlbum()
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        REQUEST_READ_EXTERNAL_STORAGE
                    )
                } else {
                    openAlbum()
                }
            }
            else -> {
                openAlbum()
            }
        }
    }

    // 권한 요청 결과 처리
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openAlbum()
            } else {
                Toast.makeText(this, "파일 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 앨범 열기
    private fun openAlbum() {
        pickImageLauncher.launch("image/*")
    }

    // 사진 업로드 함수
    private fun contentUpload() {
        photoUri?.let { uri ->
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val imageFileName = "IMAGE_$timestamp.png"

            val storageRef: StorageReference =
                storage.reference.child("images").child(imageFileName)

            // 파일 업로드
            storageRef.putFile(uri).addOnSuccessListener {
                Toast.makeText(this, "업로드 성공", Toast.LENGTH_LONG).show()
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    saveFileUrlToFirestore(downloadUrl.toString())
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(this, "업로드 실패: ${exception.message}", Toast.LENGTH_LONG).show()
                Log.e("AddPostActivity", "파일 업로드 실패", exception)
            }
        }
    }

    // Firestore에 URL 저장 후 게시물 목록 새로 고침
    private fun saveFileUrlToFirestore(downloadUrl: String) {

        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: "Unknown User"
        val userProfile = currentUser?.photoUrl?.toString() ?: ""
        val dateFormat = SimpleDateFormat("yyyy-mm-dd", java.util.Locale.getDefault())
        val formattedDate = dateFormat.format(Date())

        getFamilyCodeAndNickName(userId) { familyCode, nickName ->

            val postData = DiaryFragment.ContentDTO(
                userId = userId,
                nickName = nickName,
                explain = binding.addpostEditExplain.text.toString(),
                imageUrl = downloadUrl,
                timestamp = Timestamp.now(),
                profileImageUrl = userProfile,
                familyCode = familyCode
            )

            firestore.collection("posts").add(postData).addOnSuccessListener { documentReference ->
                postData.documentId = documentReference.id
                Log.d("AddPostActivity", "게시물 업로드 성공, ID: ${postData.documentId}")
                EventBus.getDefault().post(NewPostEvent())
                finish()
            }.addOnFailureListener { exception ->
                Log.e("AddPostActivity", "Firestore 저장 실패", exception)
            }
        }
    }

    // familyCode와 nickName을 가져오는 함수
    private fun getFamilyCodeAndNickName(userId: String, callback: (String, String) -> Unit) {
        val database = FirebaseDatabase.getInstance().reference // Realtime Database 초기화
        database.child("users").child(userId).child("familyCode").get()
            .addOnSuccessListener { dataSnapshot ->
                val familyCode = dataSnapshot.value.toString()
                Log.d("AddPostActivity", "familyCode 저장: $familyCode")


                database.child("families").child(familyCode).child("members").child(userId).child("nickName").get()
                    .addOnSuccessListener { nickNameSnapshot ->
                        val nickName = nickNameSnapshot.value.toString()
                        Log.d("AddPostActivity", "nickName 가져오기 성공: $nickName")
                        callback(familyCode, nickName)
                    }
                    .addOnFailureListener { exception ->
                        Log.e("AddPostActivity", "nickName 가져오기 실패", exception)
                        callback(familyCode, "Unknown")
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("AddPostActivity", "사용자 familyCode 정보 가져오기 실패", exception)
                callback("defaultFamilyCode", "Unknown")
            }
    }
}

class NewPostEvent {
    // NewPostEvent 정의
}