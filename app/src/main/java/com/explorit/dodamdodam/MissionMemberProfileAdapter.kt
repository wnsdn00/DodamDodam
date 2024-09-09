package com.explorit.dodamdodam

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView

class MissionMemberProfileAdapter(
    private val memberList: List<Member>,
    private val onMemberSelected: (Member) -> Unit
) : RecyclerView.Adapter<MissionMemberProfileAdapter.MemberViewHolder>() {

    private var selectedPosition : Int = 0

    inner class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        private val profileImageView: CircleImageView = itemView.findViewById(R.id.profileImageView)

        @SuppressLint("SuspiciousIndentation")
        fun bind(member: Member, position: Int) {
            nameTextView.text = member.nickName
            member.profileUrl?.let { loadMissionProfileImage(profileImageView, it) }

            if (position == selectedPosition) {
                profileImageView.alpha = 1.0f
                nameTextView.setTypeface(null, Typeface.BOLD)
            } else {
                profileImageView.alpha = 0.5f
                nameTextView.setTypeface(null, Typeface.NORMAL)
            }

            itemView.setOnClickListener {
                selectedPosition = bindingAdapterPosition
                if(selectedPosition != RecyclerView.NO_POSITION)
                notifyDataSetChanged()
                onMemberSelected(member)
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_mission_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(memberList[position], position)
    }

    override fun getItemCount(): Int = memberList.size

}

fun loadMissionProfileImage(imageView: ImageView, url: String) {
    Glide.with(imageView.context)
        .load(url)
        .error(R.drawable.ic_profile)
        .into(imageView)
}