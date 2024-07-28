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

        // Initialize Firebase storage and Firestore
        storage = FirebaseStorage.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Select photo button event
        binding.addpostImage.setOnClickListener {
            val photoPickerIntent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM)
        }

        // Add image upload event
        binding.addpostBtnUpload.setOnClickListener {
            contentUpload()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_FROM_ALBUM && resultCode == Activity.RESULT_OK) {
            // This is the path to the selected image
            photoUri = data?.data
            binding.addpostImage.setImageURI(photoUri)
        }
    }

    private fun contentUpload() {
        // Make filename
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
                Toast.makeText(this, getString(R.string.upload_success), Toast.LENGTH_LONG).show()
            }.addOnFailureListener {
                Toast.makeText(this, getString(R.string.upload_failed), Toast.LENGTH_LONG).show()
            }
        }
    }

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