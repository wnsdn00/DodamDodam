package com.explorit.dodamdodam

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
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
import java.io.File
import java.io.FileOutputStream
import com.yalantis.ucrop.UCrop

class AddPostActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_READ_EXTERNAL_STORAGE = 100
        private const val FILE_PICKER_REQUEST_CODE = 1
    }

    private lateinit var binding: ActivityAddPostBinding
    private lateinit var storage: FirebaseStorage
    private lateinit var firestore: FirebaseFirestore
    private var photoUri: Uri? = null

    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            photoUri = it
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startCrop(uri)
            displayMedia()
            Toast.makeText(this, "미디어가 선택되었습니다.", Toast.LENGTH_LONG).show()
        } ?: run {
            Toast.makeText(this, "미디어 선택이 취소되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UCrop.REQUEST_CROP && resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(data!!) // 자른 이미지 URI 가져오기
            if (resultUri != null) {
                photoUri = resultUri // 자른 이미지를 photoUri에 저장
                displayMedia() // 자른 이미지를 표시
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            Toast.makeText(this, "이미지 자르기 실패: ${cropError?.message}", Toast.LENGTH_SHORT).show()
        } else if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startCrop(uri) // 파일 선택 후 바로 자르기 시작
            }
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

    // uCrop을 사용하여 이미지를 자를 수 있도록 함
    private fun startCrop(uri: Uri) {
        val originalBitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))

        // 원본 이미지의 크기 가져오기
        val originalWidth = originalBitmap.width
        val originalHeight = originalBitmap.height

        // 비율 계산
        val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()

        // 최소 크기 정의
        val minSize = 400

        // 자를 크기 결정 (해상도 유지)
        val cropWidth: Int
        val cropHeight: Int

        if (originalWidth < minSize && originalHeight < minSize) {
            cropWidth = originalWidth
            cropHeight = originalHeight
        } else if (originalWidth > originalHeight) {
            cropWidth = Math.max(originalWidth, minSize)
            cropHeight = (cropWidth / aspectRatio).toInt()
        } else {
            cropHeight = Math.max(originalHeight, minSize)
            cropWidth = (cropHeight * aspectRatio).toInt()
        }

        // 자를 크기를 결정하는 로직
        val destinationUri = Uri.fromFile(File(cacheDir, "croppedImage.jpg"))
        val options = UCrop.Options().apply {
            setCompressionQuality(100)
            setMaxBitmapSize(1000)
            setHideBottomControls(true)
            setFreeStyleCropEnabled(true)
        }

        UCrop.of(uri, destinationUri)
            .withOptions(options)
            .withAspectRatio(aspectRatio, 1f) // 원본 비율 유지
            .withMaxResultSize(cropWidth, cropHeight) // 자를 수 있는 최대 크기 설정
            .start(this)
    }

    // 업로드된 파일이 이미지인지 비디오인지 확인하여 표시
    private fun displayMedia() {
        photoUri?.let { uri ->
            val mimeType = contentResolver.getType(uri)

            if (mimeType != null && mimeType.startsWith("image")) {
                binding.addpostImage.setImageURI(uri)
                binding.addpostImage.visibility = View.VISIBLE
                binding.addpostVideo.visibility = View.GONE
            } else if (mimeType != null && mimeType.startsWith("video")) {
                binding.addpostVideo.setVideoURI(uri)
                binding.addpostVideo.setOnPreparedListener { mp ->
                    mp.isLooping = true
                    binding.addpostVideo.start()
                }
                binding.addpostVideo.visibility = View.VISIBLE
                binding.addpostImage.visibility = View.GONE
            }
        }
    }

    // 권한 확인 및 앨범 열기
    private fun checkPermissionAndOpenAlbum() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                val permissions = mutableListOf<String>()
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                }
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
                }
                if (permissions.isNotEmpty()) {
                    ActivityCompat.requestPermissions(this, permissions.toTypedArray(), REQUEST_READ_EXTERNAL_STORAGE)
                } else {
                    openAlbum()
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_READ_EXTERNAL_STORAGE)
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
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                openAlbum()
            } else {
                Toast.makeText(this, "파일 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openAlbum() {
        pickMediaLauncher.launch(arrayOf("image/*", "video/*"))
    }
    // 사진 업로드 함수
    private fun contentUpload() {
        photoUri?.let { uri ->
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val fileExtension = contentResolver.getType(uri)?.substringAfterLast("/")
            val mediaFileName = "MEDIA_$timestamp.$fileExtension"

            val storageRef: StorageReference =
                storage.reference.child("images").child(mediaFileName)

            // 파일 업로드
            storageRef.putFile(uri).addOnSuccessListener {
                Toast.makeText(this, "업로드 성공", Toast.LENGTH_LONG).show()
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    val mimeType = contentResolver.getType(uri)
                    saveFileUrlToFirestore(downloadUrl.toString(), mimeType ?: "image")
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(this, "업로드 실패: ${exception.message}", Toast.LENGTH_LONG).show()
                Log.e("AddPostActivity", "파일 업로드 실패", exception)
            }
        }
    }

    // Firestore에 URL 저장 후 게시물 목록 새로 고침
// Firestore에 URL 저장 후 게시물 목록 새로 고침
    private fun saveFileUrlToFirestore(downloadUrl: String, mimeType: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: "Unknown User"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) // "MM"을 사용하세요.
        val formattedDate = dateFormat.format(Date())

        getFamilyCodeAndNickName(userId) { familyCode, nickName, userProfile ->

            // 미디어 타입에 따라 imageUrl 또는 videoUrl에 다운로드 URL 저장
            val postData = DiaryFragment.ContentDTO(
                userId = userId,
                nickName = nickName,
                explain = binding.addpostEditExplain.text.toString(),
                imageUrl = if (mimeType.startsWith("image")) downloadUrl else null,
                videoUrl = if (mimeType.startsWith("video")) downloadUrl else null,
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
    private fun getFamilyCodeAndNickName(userId: String, callback: (String, String, String) -> Unit) {
        val database = FirebaseDatabase.getInstance().reference // Realtime Database 초기화
        database.child("users").child(userId).child("familyCode").get()
            .addOnSuccessListener { dataSnapshot ->
                val familyCode = dataSnapshot.value.toString()
                Log.d("AddPostActivity", "familyCode 저장: $familyCode")


                database.child("families").child(familyCode).child("members").child(userId).child("nickName").get()
                    .addOnSuccessListener { nickNameSnapshot ->
                        val nickName = nickNameSnapshot.value.toString()
                        Log.d("AddPostActivity", "nickName 가져오기 성공: $nickName")
                        database.child("families").child(familyCode).child("members").child(userId).child("profileUrl").get()
                            .addOnSuccessListener { profileSnapshot ->
                                val profileImage= profileSnapshot.value.toString()
                                Log.d("AddPostActivity", "profile가져오기 성공: $profileImage")
                                callback(familyCode, nickName, profileImage)
                            }
                            .addOnFailureListener { exception ->
                                Log.e("AddPostActivity", "nickName 가져오기 실패", exception)
                                callback(familyCode, "Unknown", "https://firebasestorage.googleapis.com/v0/b/dodamdodam-b1e37.appspot.com/o/profileImages%2Fic_profile.png?alt=media&token=8b78b600-1fa7-410c-8674-d6ec06e21e7a")
                            }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("AddPostActivity", "nickName 가져오기 실패", exception)
                        callback(familyCode, "Unknown", "https://firebasestorage.googleapis.com/v0/b/dodamdodam-b1e37.appspot.com/o/profileImages%2Fic_profile.png?alt=media&token=8b78b600-1fa7-410c-8674-d6ec06e21e7a")

                    }
            }
            .addOnFailureListener { exception ->
                Log.e("AddPostActivity", "사용자 familyCode 정보 가져오기 실패", exception)
                callback("defaultFamilyCode", "Unknown", "https://firebasestorage.googleapis.com/v0/b/dodamdodam-b1e37.appspot.com/o/profileImages%2Fic_profile.png?alt=media&token=8b78b600-1fa7-410c-8674-d6ec06e21e7a")
            }
    }
}

class NewPostEvent {
    // NewPostEvent 정의
}