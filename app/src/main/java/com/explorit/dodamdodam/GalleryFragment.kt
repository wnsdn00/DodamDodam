package com.explorit.dodamdodam

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.explorit.dodamdodam.databinding.FragmentGalleryBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query


class GalleryFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var binding: FragmentGalleryBinding
    private lateinit var galleryAdapter: GalleryAdapter
    private var photoList: MutableList<String> = mutableListOf()

    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentGalleryBinding.inflate(inflater, container, false)

        binding.back.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        recyclerView = binding.galleryRecyclerView
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        galleryAdapter = GalleryAdapter(photoList) { imageUrl ->
            openFullScreenImageFragment(imageUrl)
        }
        recyclerView.adapter = galleryAdapter

        firestore = FirebaseFirestore.getInstance()
        fetchPhotos()

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as GridLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                if (!isLoading && totalItemCount <= lastVisibleItemPosition + 2) {
                    fetchPhotos()
                }
            }
        })

        return binding.root
    }

    private fun fetchPhotos() {
        if (isLoading) return

        isLoading = true
        firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    for (document in querySnapshot.documents) {
                        val imageUrl = document.getString("imageUrl")
                        if (!imageUrl.isNullOrEmpty() && !photoList.contains(imageUrl)) {
                            photoList.add(imageUrl)
                        }
                    }
                    galleryAdapter.notifyDataSetChanged()
                }
                isLoading = false
            }
            .addOnFailureListener { e ->
                Log.e("GalleryFragment", "Failed to fetch photos: ${e.message}")
                isLoading = false
            }
    }

    private fun openFullScreenImageFragment(imageUrl: String) {
        val fullScreenFragment = FullScreenImageFragment.newInstance(imageUrl)

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fullScreenFragment)
            .addToBackStack(null)
            .commit()
    }
}