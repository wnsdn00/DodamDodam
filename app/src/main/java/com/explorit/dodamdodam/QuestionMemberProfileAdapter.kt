package com.explorit.dodamdodam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView

class QuestionMemberProfileAdapter(private val memberList: List<Member>) :
    RecyclerView.Adapter<QuestionMemberProfileAdapter.MemberViewHolder>() {

        inner class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
            val answerStatusView: View = itemView.findViewById(R.id.answerStatusView)
            private val profileImageView: CircleImageView = itemView.findViewById(R.id.profileImageView)

            fun bind(member: Member) {
                nameTextView.text = member.nickName
                member.profileUrl?.let { loadQuestionProfileImage(profileImageView, it) }

            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = memberList[position]
        holder.bind(memberList[position])

        val colorResId = if (member.hasAnswered) R.color.green else R.color.gray
        holder.answerStatusView.setBackgroundResource(colorResId)
    }

    override fun getItemCount(): Int = memberList.size

    }

fun loadQuestionProfileImage(imageView: ImageView, url: String) {
    Glide.with(imageView.context)
        .load(url)
        .error(R.drawable.ic_profile)
        .into(imageView)
}