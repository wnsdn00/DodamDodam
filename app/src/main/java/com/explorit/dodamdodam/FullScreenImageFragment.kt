package com.explorit.dodamdodam

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.explorit.dodamdodam.databinding.FragmentFullScreenImageBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class FullScreenImageFragment : Fragment() {

    private lateinit var binding: FragmentFullScreenImageBinding
    private val REQUEST_CODE = 100

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentFullScreenImageBinding.inflate(inflater, container, false)

        val imageUrl = arguments?.getString(ARG_IMAGE_URL)

        if (imageUrl != null) {
            Glide.with(this)
                .load(imageUrl)
                .into(binding.fullScreenImageView)

            binding.downloadButton.setOnClickListener {
                downloadImage(imageUrl)
            }
        } else {
            Log.e("FullScreenImageFragment", "Image URL is null")
        }

        binding.back.setOnClickListener {
            closeFragment()
        }

        return binding.root
    }

    private fun downloadImage(imageUrl: String) {
        Glide.with(this)
            .asBitmap()
            .load(imageUrl)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    saveImageToGallery(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })
    }

    private fun saveImageToGallery(bitmap: Bitmap) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentResolver = requireContext().contentResolver
            val contentValues = ContentValues().apply {
                put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    "downloaded_image_${System.currentTimeMillis()}.png"
                )
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }

            val imageUrl =
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            imageUrl?.let { uri ->
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    outputStream.flush()
                    Log.d("FullScreenImageFragment", "Image saved to gallery: $uri")
                }
            } ?: run {
                Log.e("FullScreenImageFragment", "Error saving image to gallery: Uri is null")
            }
        } else {
            val filename = "downloaded_image_${System.currentTimeMillis()}.png"
            val fos: FileOutputStream
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE
                )
            } else {
                try {
                    val dir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val file = File(dir, filename)
                    fos = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    fos.flush()
                    fos.close()
                    Log.d("FullScreenImageFragment", "Image saved to gallery: ${file.absolutePath}")
                    MediaScannerConnection.scanFile(
                        requireContext(),
                        arrayOf(file.absolutePath),
                        null,
                        null
                    )
                } catch (e: IOException) {
                    Log.e("FullScreenImageFragment", "Error saving image to gallery: ${e.message}")
                }
            }
        }
    }

    private fun closeFragment() {
        parentFragmentManager.popBackStack()
    }

    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            closeFragment()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // 권한이 허용됨
                Log.d("FullScreenImageFragment", "Permission granted.")
            } else {
                // 권한이 거부됨
                Log.e("FullScreenImageFragment", "Permission denied.")
            }
        }
    }

    companion object {
        private const val ARG_IMAGE_URL = "image_url"

        fun newInstance(imageUrl: String): FullScreenImageFragment {
            val fragment = FullScreenImageFragment()
            val args = Bundle()
            args.putString(ARG_IMAGE_URL, imageUrl)
            fragment.arguments = args
            return fragment
        }
    }
}