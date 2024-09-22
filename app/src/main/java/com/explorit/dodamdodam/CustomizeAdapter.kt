package com.explorit.dodamdodam

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class CustomizeAdapter(private val items: List<StoreItem>, private val onItemClick: (StoreItem) -> Unit) : RecyclerView.Adapter<CustomizeAdapter.ViewHolder>() {

    private var selectedPosition = -1

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemName: TextView = itemView.findViewById(R.id.itemNameTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_customize_name, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val item = items[position]
        holder.itemName.text = item.name

        // 배경색 설정
        val colorResId = if (selectedPosition == position) R.drawable.selected_item_name else R.drawable.default_item_name
        holder.itemView.setBackgroundResource(colorResId)



        // 아이템 클릭 이벤트
        holder.itemView.setOnClickListener {
            // 선택된 아이템의 배경을 바꾸고, 이전 선택은 초기화
            val previousPosition = selectedPosition
            selectedPosition = position
            notifyItemChanged(previousPosition) // 이전 선택된 아이템 초기화
            notifyItemChanged(position) // 현재 선택된 아이템 업데이트

            // 클릭된 아이템을 콜백으로 처리
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

}