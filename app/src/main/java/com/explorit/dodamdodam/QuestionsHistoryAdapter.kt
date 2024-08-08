package com.explorit.dodamdodam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class QuestionsHistoryAdapter(private var questions: List<QuestionHistory>): RecyclerView.Adapter<QuestionsHistoryAdapter.QuestionsHistoryViewHolder>() {
    inner class QuestionsHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private val questionTextView: TextView = itemView.findViewById(R.id.questionTextView)
        private val answersTextView: TextView = itemView.findViewById(R.id.answersTextView)

        fun bind(questionHistory: QuestionHistory) {
            questionTextView.text = questionHistory.question

            val answersString = questionHistory.answers.entries.joinToString("\n") { "${it.key}: ${it.value}" }
            answersTextView.text = answersString

            questionTextView.setOnClickListener {
                if (answersTextView.visibility == View.GONE) {
                    answersTextView.visibility = View.VISIBLE
                } else {
                    answersTextView.visibility = View.GONE
                }
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionsHistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_question_history, parent, false)
        return QuestionsHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestionsHistoryViewHolder, position: Int) {
        val question = questions[position]
        holder.bind(question)
    }

    override fun getItemCount(): Int {
        return questions.size
    }

    fun updateQuestions(newQuestions: List<QuestionHistory>) {
        questions = newQuestions
        notifyDataSetChanged()
    }
}