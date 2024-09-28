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
import androidx.activity.addCallback
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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

    private lateinit var questionProfileRecyclerView: RecyclerView
    private lateinit var memberProfileAdapter: QuestionMemberProfileAdapter
    private lateinit var database: DatabaseReference
    private var memberList = mutableListOf<Member>()
    private var answerList = mutableListOf<Answer>()
    private  lateinit var backToMainButton: ImageButton
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

        questionProfileRecyclerView = view.findViewById(R.id.questionProfileRecyclerView)
        questionProfileRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        memberProfileAdapter = QuestionMemberProfileAdapter(memberList)
        questionProfileRecyclerView.adapter = memberProfileAdapter

        backToMainButton = view.findViewById(R.id.randomQuestionBackBtn)
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

        backToMainButton.setOnClickListener {
            // 메인으로 가는 버튼 함수
            (activity as? MainPageActivity)?.setFragment(TAG_HOME, HomeFragment())
        }

        submitAnswerButton.setOnClickListener {
            // 답변 제출 버튼 클릭 함수
            submitAnswer()
        }

        viewQuestionHistoryButton.setOnClickListener {
            // 버튼을 눌렀을 때 지난 질문들을 보여주는 함수 작성
            val questionHistoryFragment = QuestionHistoryFragment.newInstance()
            questionHistoryFragment.show(parentFragmentManager, questionHistoryFragment.tag)
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            // 기기의 뒤로 가기 버튼 클릭 함수(메인으로)
            (activity as? MainPageActivity)?.setFragment(TAG_HOME, HomeFragment())
        }



        return view
    }

    // 사용자의 가족 코드를 불러오는 함수
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

    // 사용자가 속한 가족의 구성원을 불러오는 함수
    private fun fetchMembers(familyCode: String) {
        database.child("families").child(familyCode).child("members")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    memberList.clear()
                    // 구성원을 확인하여 구성원 리스트에 추가
                    for (data in snapshot.children) {
                        val member = data.getValue(Member::class.java)
                        if (member != null) {
                            memberList.add(member)
                        }
                    }
                    // 구성원 리스트를 어댑터에 전달
                    memberProfileAdapter.notifyDataSetChanged()
                    fetchAnswers(familyCode)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "데이터를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                    Log.e("RandomQuestionFragment", "Database error: ${error.message}")
                }

            })
    }

    // 사용자가 속한 가족의 구성원들의 답변을 불러오는 함수
    private fun fetchAnswers(familyCode: String) {
        database.child("families").child(familyCode).child("todayQuestion").child("answers").addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val answers = mutableMapOf<String, String>()
                // 오늘의 질문의 답변을 확인하여 답변 리스트에 저장
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

    // 불러온 답변을 화면에 띄워주는 함수 (답변이 없으면 ?로 표시)
    private fun updateAnswers(answers: Map<String, String>) {
        // 답변 리스트를 어댑터에 전달
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

    // 오늘의 질문을 불러오는 함수
    private fun checkAndFetchQuestion(famliyCode: String){
        val today = dateFormat.format(Date())

        database.child("families").child(famliyCode).child("todayQuestion").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // 날짜를 확인하고 오늘의 질문을 불러오거나 생성
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

    // 불러온 질문을 화면에 띄워주는 함수
    private fun showSavedQuestion(snapshot: DataSnapshot) {
        val question = snapshot.child("question").getValue(String::class.java)
        todayQuestionTextView.text = question ?: "질문을 불러오는데 실패했습니다."
    }

    // 오늘의 질문을 생성하고 지난 질문 목록에 추가하는 함수
    private fun selectAndSaveRandomQuestion(familyCode: String, today: String) {

        database.child("families").child(familyCode).child("questionNoList").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // 사용된 질문 리스트(questionNoList)에서 사용된 질문 목록을 불러옴
                val usedQuestions = snapshot.children.mapNotNull { it.key?.toInt() to it.getValue(Boolean::class.java) }.toMap()

                database.child("questionList").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // 모든 질문 리스트(questionList)에서 사용된 질문을 필터링하여 사용 가능한 질문을 불러옴
                        val allQuestions =
                            snapshot.children.mapNotNull { it.key?.toInt() to it.getValue(String::class.java) }
                                .toMap()
                        val availableQuestions =
                            allQuestions.filter { !usedQuestions.containsKey(it.key) || !usedQuestions[it.key]!! }

                        // 사용 가능한 질문들 중에 랜덤으로 오늘의 질문을 생성
                        if (availableQuestions.isNotEmpty()) {
                            val randomQuestion = availableQuestions.entries.random()
                            todayQuestionTextView.text = randomQuestion.value

                            // 오늘의 질문이 새로 생겼으므로 답변 여부를 false로 초기화
                            val currentUserUid = auth.currentUser?.uid
                            if(currentUserUid != null) {
                                database.child("families").child(familyCode).child("members")
                                    .child(currentUserUid).child("hasAnswered").setValue(false)
                            }
                            // 오늘의 질문을 저장
                            database.child("families").child(familyCode).child("todayQuestion")
                                .setValue(
                                    mapOf(
                                        "questionNo" to randomQuestion.key,
                                        "question" to randomQuestion.value,
                                        "date" to today
                                    )
                                )

                            // 오늘의 질문을 사용된 질문을 사용된 질문 리스트에 저장
                            database.child("families").child(familyCode).child("questionNoList")
                                .child(randomQuestion.key.toString()).setValue(true)

                            // 생성된 질문을 지난 질문 리스트에 저장
                            database.child("families").child(familyCode).child("questionHistory")
                                .child(today).setValue(
                                    mapOf(
                                        "question" to randomQuestion.value,
                                        "questionNo" to randomQuestion.key
                                    )
                                )
                        } else {
                            // 준비된 모든 질문이 사용되었으면 사용된 질문 리스트를 리셋
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

    // 모든 질문이 한번씩 사용 되었을 때 질문 사용 목록을 리셋하고 오늘의 질문을 생성하는 함수
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

    // 답변을 저장하는 함수
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
                                    // 작성한 답변을 오늘의 질문에 저장
                                    database.child("families").child(familyCode).child("todayQuestion").child("answers").child(nickName).setValue(answer).addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            // 작성한 답변을 지난 질문 리스트에 저장
                                            database.child("families").child(familyCode).child("questionHistory").child(today).child("answers").child(nickName).setValue(answer)
                                            fetchHasAnsweredAndRewardCoins(familyCode, currentUserUid)
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

    // 답변을 했을 때 답변 여부를 체크해 주고 코인을 지급 해주는 함수
    private fun fetchHasAnsweredAndRewardCoins(familyCode: String, currentUserUid: String) {
        database.child("families").child(familyCode).child("members").child(currentUserUid).child("hasAnswered").addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val hasAnswered = snapshot.getValue(Boolean::class.java)
                if(hasAnswered == false) {
                    database.child("families").child(familyCode).child("members").child(currentUserUid).child("hasAnswered").setValue(true).addOnCompleteListener{ task ->
                       if(task.isSuccessful) {
                            rewardCoinsToFamily(familyCode, 10)
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("RandomQuestionFragment", "Database error: ${error.message}")
            }
        })
    }

    // 현재 코인 값을 가져 와서 증가 시킨 후 저장 하는 함수
    private fun rewardCoinsToFamily(familyCode: String, coins: Int) {
        database.child("families").child(familyCode).child("familyCoin")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentCoins = snapshot.getValue(Int::class.java) ?: 0
                    database.child("families").child(familyCode).child("familyCoin")
                        .setValue(currentCoins + coins)
                    Toast.makeText(context, "코인이 지급되었습니다!", Toast.LENGTH_SHORT).show()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "코인 지급에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    Log.e("RandomQuestionFragment", "Database error: ${error.message}")
                }
            })
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
// 답변 데이터 클래스
data class Answer(val member: Member, val answer: String)

