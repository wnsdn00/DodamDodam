package com.explorit.dodamdodam

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [RandomQuestionFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RandomQuestionFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var profileRecyclerView: RecyclerView
    private lateinit var memberProfileAdapter: MemberProfileAdapter
    private lateinit var database: DatabaseReference
    private var memberList = mutableListOf<Member>()
    private var answerList = mutableListOf<Answer>()
    private lateinit var todayQuestionTextView: TextView
    private lateinit var answerEditText: EditText
    private lateinit var submitAnswerButton: Button
    private lateinit var answersRecyclerView: RecyclerView
    private lateinit var answerAdapter: AnswerAdapter
    private lateinit var auth: FirebaseAuth
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private lateinit var viewQuestionHistoryButton : ImageButton

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
        val view = inflater.inflate(R.layout.fragment_random_question, container, false)

        profileRecyclerView = view.findViewById(R.id.profileRecyclerView)
        profileRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        memberProfileAdapter = MemberProfileAdapter(memberList)
        profileRecyclerView.adapter = memberProfileAdapter


        todayQuestionTextView = view.findViewById(R.id.todayQuestionTextView)
        answerEditText = view.findViewById(R.id.answerEditText)
        submitAnswerButton = view.findViewById(R.id.submitAnswerButton)

        answersRecyclerView = view.findViewById(R.id.answersRecyclerView)
        answersRecyclerView.layoutManager = LinearLayoutManager(context)

        answerAdapter = AnswerAdapter(answerList)
        answersRecyclerView.adapter = answerAdapter

        viewQuestionHistoryButton = view.findViewById(R.id.viewQuestionHistoryButton)

        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        fetchUserFamilyCode()

        submitAnswerButton.setOnClickListener {
            submitAnswer()
        }

        viewQuestionHistoryButton.setOnClickListener {
            // 버튼을 눌렀을 때 지난 질문들을 보여주는 함수 작성
            val questionHistoryFragment = QuestionHistoryFragment.newInstance()
            questionHistoryFragment.show(parentFragmentManager, questionHistoryFragment.tag)
        }



        return view
    }

    private fun fetchUserFamilyCode() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserUid != null) {
            database.child("users").child(currentUserUid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val familyCode = snapshot.child("familyCode").getValue(String::class.java)
                        if (familyCode != null) {
                            fetchMembers(familyCode)
                            checkAndFetchQuestion(familyCode)
                        } else {
                            Toast.makeText(context, "가족 그룹을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, "데이터를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                        Log.e("RandomQuestionFragment", "Database error: ${error.message}")
                    }
                })
        } else {
            Toast.makeText(context, "사용자가 로그인되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
            Log.e("RandomQuestionFragment", "Current user UID is null")
        }
    }


    private fun fetchMembers(familyCode: String) {
        database.child("families").child(familyCode).child("members")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    memberList.clear()
                    for (data in snapshot.children) {
                        val member = data.getValue(Member::class.java)
                        if (member != null) {
                            memberList.add(member)
                        }
                    }
                    memberProfileAdapter.notifyDataSetChanged()
                    fetchAnswers(familyCode)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "데이터를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                    Log.e("RandomQuestionFragment", "Database error: ${error.message}")
                }

            })
    }

    private fun fetchAnswers(familyCode: String) {
        database.child("families").child(familyCode).child("todayQuestion").child("answers").addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val answers = mutableMapOf<String, String>()
                for (data in snapshot.children) {
                    val answer = data.getValue(String::class.java)
                    if(answer != null) {
                        answers[data.key!!] = answer
                    }
                }
                updateAnswers(answers)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "데이터를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                Log.e("RandomQuestionFragment", "Database error: ${error.message}")
            }
        })

    }

    private fun updateAnswers(answers: Map<String, String>) {
        val answerList = memberList.map { member ->
            val answer = answers[member.nickName] ?: "?"
            Answer(member, answer)
        }
        answerAdapter.updateAnswers(answerList)

        memberList.forEach { member ->
            member.hasAnswered = answers.containsKey(member.nickName)
        }
        memberProfileAdapter.notifyDataSetChanged()
    }

    private fun checkAndFetchQuestion(famliyCode: String){
        val today = dateFormat.format(Date())

        database.child("families").child(famliyCode).child("todayQuestion").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lastDate = snapshot.child("date").getValue(String::class.java)
                if (lastDate != null && lastDate == today) {
                    showSavedQuestion(snapshot)
                } else {
                    selectAndSaveRandomQuestion(famliyCode, today)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "데이터베이스 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                Log.e("RandomQuestionFragment", "Database error: ${error.message}")
            }
        })
    }

    private fun showSavedQuestion(snapshot: DataSnapshot) {
        val question = snapshot.child("question").getValue(String::class.java)
        todayQuestionTextView.text = question ?: "질문을 불러오는데 실패했습니다."
    }

    private fun selectAndSaveRandomQuestion(familyCode: String, today: String) {
        database.child("families").child(familyCode).child("questionNoList").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val usedQuestions = snapshot.children.mapNotNull { it.key?.toInt() to it.getValue(Boolean::class.java) }.toMap()

                database.child("questionList").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val allQuestions =
                            snapshot.children.mapNotNull { it.key?.toInt() to it.getValue(String::class.java) }
                                .toMap()
                        val availableQuestions =
                            allQuestions.filter { !usedQuestions.containsKey(it.key) || !usedQuestions[it.key]!! }

                        if (availableQuestions.isNotEmpty()) {
                            val randomQuestion = availableQuestions.entries.random()
                            todayQuestionTextView.text = randomQuestion.value

                            database.child("families").child(familyCode).child("todayQuestion")
                                .setValue(
                                    mapOf(
                                        "questionNo" to randomQuestion.key,
                                        "question" to randomQuestion.value,
                                        "date" to today
                                    )
                                )

                            database.child("families").child(familyCode).child("questionNoList")
                                .child(randomQuestion.key.toString()).setValue(true)

                            database.child("families").child(familyCode).child("questionHistory")
                                .child(today).setValue(
                                    mapOf(
                                        "question" to randomQuestion.value,
                                        "questionNo" to randomQuestion.key
                                    )
                                )
                        } else {
                            resetQuestionNoList(familyCode, today)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, "데이터베이스 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                        Log.e("RandomQuestionFragment", "Database error: ${error.message}")
                    }

                })
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "데이터베이스 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                Log.e("RandomQuestionFragment", "Database error: ${error.message}")
            }
        })
    }

    private fun resetQuestionNoList(familyCode: String, today: String) {
        database.child("families").child(familyCode).child("questionNoList").removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                selectAndSaveRandomQuestion(familyCode, today)
            } else {
                Toast.makeText(context, "질문 목록을 리셋하는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                Log.e("RandomQuestionFragment", "Failed to reset questionNoList: ${task.exception?.message}")
            }
        }
    }

    private fun submitAnswer() {
        val currentUserUid = auth.currentUser?.uid
        val answer = answerEditText.text.toString()

        if (currentUserUid != null && answer.isNotEmpty()) {
            val today = dateFormat.format(Date())
            database.child("users").child(currentUserUid).child("familyCode").addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val familyCode = snapshot.getValue(String::class.java)
                    if (familyCode != null) {
                        database.child("families").child(familyCode).child("members").child(currentUserUid).child("nickName").addValueEventListener(object: ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val nickName = snapshot.getValue(String::class.java)
                                if (nickName != null) {
                                    database.child("families").child(familyCode).child("todayQuestion").child("answers").child(nickName).setValue(answer).addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            database.child("families").child(familyCode).child("questionHistory").child(today).child("answers").child(nickName).setValue(answer)
                                        } else {
                                            Toast.makeText(context, "답변을 저장하는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "닉네임을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(context, "데이터베이스 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                                Log.e("RandomQuestionFragment", "Database error: ${error.message}")
                            }
                        })
                    } else {
                        Toast.makeText(context, "가족 그룹을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "데이터베이스 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    Log.e("RandomQuestionFragment", "Database error: ${error.message}")
                }
            })
        } else {
            Toast.makeText(context, "답변을 입력하세요.", Toast.LENGTH_SHORT).show()
        }
    }




    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment RandomQuestionFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            RandomQuestionFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}

data class Answer(val member: Member, val answer: String)

