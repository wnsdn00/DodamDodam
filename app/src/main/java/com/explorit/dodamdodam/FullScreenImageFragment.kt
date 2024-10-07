package com.explorit.dodamdodam

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL

class FullScreenImageFragment : Fragment() {

    private lateinit var imageView: ImageView
    private lateinit var downloadButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_full_screen_image, container, false)
        imageView = view.findViewById(R.id.fullScreenImageView)
        downloadButton = view.findViewById(R.id.downloadButton)

        val imageUrl = arguments?.getString("imageUrl")
        Log.d("FullScreenImageFragment", "Image URL: $imageUrl")

        Glide.with(this).load(imageUrl).into(imageView)

        imageView.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        downloadButton.setOnClickListener {
            if (checkStoragePermissions()) {
                imageUrl?.let { url -> downloadImage(url) }
            } else {
                requestStoragePermissions()
            }
        }

        return view
    }

    private fun checkStoragePermissions(): Boolean {
        val permissionCheck = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return permissionCheck == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermissions() {
        ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1001)
    }

    private fun downloadImage(imageUrl: String) {
        Thread {
            try {
                val url = URL(imageUrl)
                val inputStream: InputStream = url.openStream()

                val folder = File(requireContext().getExternalFilesDir(null), "도담도담")
                if (!folder.exists()) {
                    folder.mkdirs()
                }

                val file = File(folder, "downloaded_image.jpg")
                val outputStream = FileOutputStream(file)

                inputStream.copyTo(outputStream)
                outputStream.close()
                inputStream.close()

                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "이미지가 '도담도담' 폴더에 다운로드되었습니다.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "다운로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    companion object {
        fun newInstance(imageUrl: String): FullScreenImageFragment {
            val fragment = FullScreenImageFragment()
            val args = Bundle()
            args.putString("imageUrl", imageUrl)
            fragment.arguments = args
            return fragment
        }
    }
}