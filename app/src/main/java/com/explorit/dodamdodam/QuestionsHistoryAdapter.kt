package com.explorit.dodamdodam

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class QuestionsHistoryAdapter(private var questionHistoryList: List<QuestionHistory>): RecyclerView.Adapter<QuestionsHistoryAdapter.QuestionsHistoryViewHolder>() {

    private var expandedPosition = -1

    inner class QuestionsHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val questionTextView: TextView = itemView.findViewById(R.id.questionTextView)
        val answerLayout: ViewGroup = itemView.findViewById(R.id.answerLayout)

        @SuppressLint("MissingInflatedId")
        fun bind(questionHistory: QuestionHistory) {
            dateTextView.text = questionHistory.date
            questionTextView.text = questionHistory.question
            answerLayout.removeAllViews()

            questionHistory.answers.forEach { (nickName, answer) ->
                val answerView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_answerlist, answerLayout, false)
                answerView.findViewById<TextView>(R.id.nickNameTextView).text = nickName
                answerView.findViewById<TextView>(R.id.answerHistoryTextView).text = answer
                answerLayout.addView(answerView)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): QuestionsHistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_question_history, parent, false)
        return QuestionsHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestionsHistoryViewHolder, position: Int) {
        val questionHistoryItem = questionHistoryList[position]
        holder.bind(questionHistoryItem)

        val isExpanded = position == expandedPosition
        holder.answerLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            expandedPosition = if (isExpanded) -1 else position
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return questionHistoryList.size
    }
}
