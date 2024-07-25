package com.explorit.dodamdodam

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import android.net.Uri
import java.util.Date
import com.explorit.dodamdodam.databinding.ActivityAddPostBinding
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.FirebaseFirestore

class AddPostActivity : AppCompatActivity() {
    private var PICK_IMAGE_FROM_ALBUM = 0
    private lateinit var binding: ActivityAddPostBinding
    private lateinit var storage: FirebaseStorage
    private var photoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initiate storge
        storage = FirebaseStorage.getInstance()

        // Open the album
        val photoPickerIntent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM)

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
        } else {
            // Exit the addPhotoActivity if you leave the album without selecting it
            finish()
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
            photoUri?.let { uri ->
                storageRef?.putFile(uri)?.addOnSuccessListener {
                    Toast.makeText(this, getString(R.string.upload_success), Toast.LENGTH_LONG)
                        .show()
                }?.addOnFailureListener {
                    Toast.makeText(this, getString(R.string.upload_failed), Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }
}