package com.explorit.dodamdodam

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.explorit.dodamdodam.databinding.ItemSearchBinding

class SearchAdapter : RecyclerView.Adapter<SearchAdapter.SearchViewHolder>() {

    private var items: List<String> = emptyList()
    private var filteredItems: List<String> = emptyList()

    init {
        filteredItems = items
    }

    class SearchViewHolder(private val binding: ItemSearchBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: String) {
            binding.searchItemText.text = item
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        // ViewHolder 생성 로직
        val binding = ItemSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchViewHolder(binding)
    }

    fun updateData(newItems: List<String>) {
        items = newItems // 새로운 데이터 업데이트
        notifyDataSetChanged() // RecyclerView 갱신
    }

    fun filter(query: String) {
        filteredItems = if (query.isEmpty()) {
            items // 검색어가 없으면 원본 리스트 반환
        } else {
            items.filter { it.contains(query, ignoreCase = true) } // 검색어에 따라 필터링
        }
        notifyDataSetChanged() // RecyclerView 갱신
    }

    override fun getItemCount(): Int {
        return filteredItems.size // 필터링된 아이템 수 반환
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val item = filteredItems[position] // 필터링된 아이템 바인딩
        holder.bind(item)
    }
}