package com.explorit.dodamdodam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class StoreAdapter(private val storeItems: List<StoreItem>, private val onBuyClick: (StoreItem) -> Unit) : RecyclerView.Adapter<StoreAdapter.StoreViewHolder>(){

    class StoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImage: ImageView = itemView.findViewById(R.id.storeItemImage)
        val itemName: TextView = itemView.findViewById(R.id.storeItemName)
        val itemPrice: TextView = itemView.findViewById(R.id.storeItemPrice)
        val buyButton: Button = itemView.findViewById(R.id.buyButton)
    }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoreViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_store, parent, false)
            return StoreViewHolder(view)
        }

    override fun onBindViewHolder(holder: StoreViewHolder, position: Int) {
        val item = storeItems[position]
        holder.itemName.text = item.name
        holder.itemPrice.text = "${item.price}"

        // FirebaseUI 또는 Glide를 사용해 이미지 로드
        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .into(holder.itemImage)

        holder.buyButton.setOnClickListener {
            onBuyClick(item)  // 아이템 구매 콜백 호출
        }
    }

    override fun getItemCount(): Int {
        return storeItems.size
    }

}