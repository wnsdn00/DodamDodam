package com.explorit.dodamdodam

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.explorit.dodamdodam.databinding.ItemGalleryBinding

class GalleryAdapter(
    private val imageUrls: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {

    inner class GalleryViewHolder(private val binding: ItemGalleryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(imageUrl: String) {
            Glide.with(binding.root.context)
                .load(imageUrl)
                .into(binding.imageView)

            binding.root.setOnClickListener {
                onClick(imageUrl)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val binding = ItemGalleryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GalleryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
            holder.bind(imageUrls[position])
    }

    override fun getItemCount(): Int = imageUrls.size
}