package com.explorit.dodamdodam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FamilyCodeAdapter(
    private val familyCodeNameList: MutableList<Triple<String, String, String>>, // 가족 코드와 이름 쌍의 리스트
    private val onJoinClick: (String, String) -> Unit, // 가족 코드와 이름을 함께 전달
    private val onDeleteClick: (String, Int) -> Unit
) : RecyclerView.Adapter<FamilyCodeAdapter.FamilyCodeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FamilyCodeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_family_code, parent, false)
        return FamilyCodeViewHolder(view)
    }

    override fun onBindViewHolder(holder: FamilyCodeViewHolder, position: Int) {
        val (familyCode, familyName, nickname) = familyCodeNameList[position]
        holder.bind(familyName, nickname) // 가족 이름을 텍스트뷰에 표시

        holder.itemView.findViewById<Button>(R.id.joinFamilyButton).setOnClickListener {
            onJoinClick(familyCode, familyName)
        }

        holder.itemView.findViewById<Button>(R.id.deleteFamilyButton).setOnClickListener {
            onDeleteClick(familyCode, position)
        }
    }

    override fun getItemCount() = familyCodeNameList.size
    fun removeItem(position: Int) {
        familyCodeNameList.removeAt(position)
        notifyItemRemoved(position)
    }

    class FamilyCodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val familyCodeTextView: TextView = itemView.findViewById(R.id.familyNameTextView)
        private val nicknameTextView: TextView = itemView.findViewById(R.id.userNicknameOfFamily)

        fun bind(familyName: String, nickname: String) {
            familyCodeTextView.text = familyName
            nicknameTextView.text = nickname
        }
    }
}
