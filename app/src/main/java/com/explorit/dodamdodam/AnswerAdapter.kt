package com.explorit.dodamdodam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView

class AnswerAdapter(private var answers: List<Answer>) : RecyclerView.Adapter<AnswerAdapter.AnswerViewHolder>() {

    class AnswerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val answerProfileImageView: CircleImageView = itemView.findViewById(R.id.answerProfileImageView)
        val answerNameTextView: TextView = itemView.findViewById(R.id.answerNameTextView)
        val answerTextView: TextView = itemView.findViewById(R.id.answerTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnswerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_answer, parent, false)
        return AnswerViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnswerViewHolder, position: Int) {
        val answer = answers[position]
        answer.member.profileUrl?.let { loadAnswerImage(holder.answerProfileImageView, it) }
        holder.answerNameTextView.text = answer.member.nickName
        holder.answerTextView.text = answer.answer
    }

    override fun getItemCount(): Int {
        return answers.size
    }

    fun updateAnswers(newAnswers: List<Answer>) {
        answers = newAnswers
        notifyDataSetChanged()
    }
}

fun loadAnswerImage(imageView: ImageView, url: String) {
    Glide.with(imageView.context)
        .load(url)
        .error(R.drawable.ic_profile)
        .into(imageView)
}