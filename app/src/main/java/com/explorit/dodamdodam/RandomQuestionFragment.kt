package com.explorit.dodamdodam

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    private lateinit var recyclerView: RecyclerView
    private lateinit var memberProfileAdapter: MemberProfileAdapter
    private lateinit var database: DatabaseReference
    private var memberList = mutableListOf<Member>()
    private lateinit var todayQuestionTextView: TextView
    private lateinit var auth: FirebaseAuth
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())


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

        recyclerView = view.findViewById(R.id.profileRecyclerView)
        recyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        memberProfileAdapter = MemberProfileAdapter(memberList)
        recyclerView.adapter = memberProfileAdapter

        todayQuestionTextView = view.findViewById(R.id.todayQuestionTextView)
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        fetchUserFamilyCode()

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
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "데이터를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                    Log.e("RandomQuestionFragment", "Database error: ${error.message}")
                }

            })
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
                                        "question" to randomQuestion.value,
                                        "date" to today
                                    )
                                )

                            database.child("families").child(familyCode).child("questionNoList")
                                .child(randomQuestion.key.toString()).setValue(true)

                            database.child("families").child(familyCode).child("questionHistory")
                                .child(today).setValue(randomQuestion.value)
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