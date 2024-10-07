package com.explorit.dodamdodam

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query


class GalleryFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var galleryAdapter: GalleryAdapter
    private var photoList: MutableList<String> = mutableListOf()

    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_gallery, container, false)

        recyclerView = view.findViewById(R.id.galleryRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        galleryAdapter = GalleryAdapter(photoList) { imageUrl ->
            val fullScreenFragment = FullScreenImageFragment.newInstance(imageUrl)
            val fragmentContainer = view.findViewById<FrameLayout>(R.id.fragmentContainer)

            fragmentContainer.visibility = View.VISIBLE

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fullScreenFragment)
                .addToBackStack(null)
                .commit()
            Log.d("GalleryFragment", "Fragment transaction committed")
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

        return view
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
}