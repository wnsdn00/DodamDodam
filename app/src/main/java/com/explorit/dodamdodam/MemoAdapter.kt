package com.explorit.dodamdodam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MemoAdapter(
    private val memos: MutableList<Memo>,
    private val onDeleteClick: (Int) -> Unit,
    private val onEditClick: (Int) -> Unit
) : RecyclerView.Adapter<MemoAdapter.MemoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_memo, parent, false)
        return MemoViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemoViewHolder, position: Int) {
        val memo = memos[position]
        holder.memoTextView.text = memo.content

        holder.deleteButton.setOnClickListener {
            onDeleteClick(position)
        }

        holder.editButton.setOnClickListener {
            onEditClick(position)
        }
    }

    override fun getItemCount(): Int = memos.size

    inner class MemoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val memoTextView: TextView = itemView.findViewById(R.id.memoTextView)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
        val editButton: Button = itemView.findViewById(R.id.editButton)
    }
}
