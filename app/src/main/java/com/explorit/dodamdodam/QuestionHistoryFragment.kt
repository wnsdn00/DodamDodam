package com.explorit.dodamdodam

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

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
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var questionHistoryList = mutableListOf<QuestionHistory>()

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

        questionsHistoryAdapter = QuestionsHistoryAdapter(questionHistoryList)
        questionsHistoryRecyclerView.adapter = questionsHistoryAdapter

        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        fetchQuestionHistory()

        return view
    }

    private fun fetchQuestionHistory() {
        val currentUserUid = auth.currentUser?.uid
        if(currentUserUid != null) {
            database.child("users").child(currentUserUid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val familyCode = snapshot.child("familyCode").getValue(String::class.java)
                    if(familyCode != null) {
                        database.child("families").child(familyCode).child("questionHistory").get().addOnSuccessListener { questionSnapshot ->
                            questionHistoryList.clear()
                            for (question in questionSnapshot.children) {
                                val date = question.key ?: "Unknown Date"
                                val questionText = question.child("question").getValue(String::class.java)
                                val answers = question.child("answers").children.mapNotNull {
                                    val nickName = it.key
                                    val answer = it.getValue(String::class.java)
                                    if (nickName != null && answer != null) nickName to answer else null
                                }.toMap()
                                questionHistoryList.add(QuestionHistory(date, questionText, answers))
                            }
                            questionsHistoryAdapter.notifyDataSetChanged()
                        }.addOnFailureListener() {
                            Toast.makeText(context, "데이터를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "가족 코드를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    Log.e("RandomQuestionFragment", "Database error: ${error.message}")
                }
            })
        } else {
            Toast.makeText(context, "사용자가 로그인되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
        }
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
        fun newInstance() = QuestionHistoryFragment()
    }
}

data class QuestionHistory(
    val date: String,
    val question: String?,
    val answers: Map<String, String>
)
