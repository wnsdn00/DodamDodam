package com.explorit.dodamdodam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView

class MissionMemberProfileAdapter(private val memberList: List<Member>) :
    RecyclerView.Adapter<MissionMemberProfileAdapter.MemberViewHolder>() {

    inner class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        private val profileImageView: CircleImageView = itemView.findViewById(R.id.profileImageView)

        fun bind(member: Member) {
            nameTextView.text = member.nickName
            member.profileUrl?.let { loadMissionProfileImage(profileImageView, it) }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(memberList[position])
    }

    override fun getItemCount(): Int = memberList.size

}

fun loadMissionProfileImage(imageView: ImageView, url: String) {
    Glide.with(imageView.context)
        .load(url)
        .error(R.drawable.ic_profile)
        .into(imageView)
}