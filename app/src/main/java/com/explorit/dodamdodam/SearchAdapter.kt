package com.explorit.dodamdodam

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.explorit.dodamdodam.databinding.ItemSearchBinding

class SearchAdapter : RecyclerView.Adapter<SearchAdapter.SearchViewHolder>() {

    private var items: List<DiaryFragment.ContentDTO> = emptyList()
    private var filteredItems: List<DiaryFragment.ContentDTO> = emptyList()

    init {
        filteredItems = items
    }

    class SearchViewHolder(private val binding: ItemSearchBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DiaryFragment.ContentDTO) {
            binding.userId.text = item.userId
            binding.userPostExplanation.text = item.explain ?: ""

            Glide.with(binding.root.context)
                .load(item.imageUrl)
                .into(binding.userPost)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        // ViewHolder 생성 로직
        val binding = ItemSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val item = filteredItems[position] // 필터링된 아이템 바인딩
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return filteredItems.size // 필터링된 아이템 수 반환
    }

    fun updatePostList(newItems: List<DiaryFragment.ContentDTO>) {
        items = newItems // 전체 데이터 업데이트
        filteredItems = items // 새로운 데이터 업데이트
        notifyDataSetChanged() // RecyclerView 갱신
    }

    fun filter(query: String) {
        filteredItems = if (query.isEmpty()) {
            items // 검색어가 없으면 원본 리스트 반환
        } else {
            items.filter { it.userId?.contains(query, ignoreCase = true) == true } // 작성자 필터링
        }
        notifyDataSetChanged() // RecyclerView 갱신
    }
}