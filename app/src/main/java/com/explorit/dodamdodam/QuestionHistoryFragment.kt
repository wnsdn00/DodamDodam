package com.explorit.dodamdodam

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [QuestionHistoryFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class QuestionHistoryFragment : BottomSheetDialogFragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var questionsHistoryRecyclerView: RecyclerView
    private lateinit var questionsHistoryAdapter: QuestionsHistoryAdapter
    private var questionList = mutableListOf<QuestionHistory>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        val view = inflater.inflate(R.layout.fragment_question_history, container, false)

        questionsHistoryRecyclerView = view.findViewById(R.id.questionHistoryRecyclerView)
        questionsHistoryRecyclerView.layoutManager = LinearLayoutManager(context)
        questionsHistoryAdapter = QuestionsHistoryAdapter(questionList)
        questionsHistoryRecyclerView.adapter = questionsHistoryAdapter

        return view
    }

    fun updateQuestionsHistory(questions: List<QuestionHistory>) {
        questionsHistoryAdapter.updateQuestions(questions)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment QuestionHistory.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            QuestionHistoryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}

data class QuestionHistory(
    val question: String?,
    val answers: Map<String, String>
)