package com.explorit.dodamdodam

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.common.reflect.TypeToken
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import java.time.LocalDate

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MissionCalendarFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@Suppress("DEPRECATION")
class MissionCalendarFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var monthYearText: TextView
    private lateinit var selectedDate: LocalDate
    private lateinit var nextMonthButton: ImageButton
    private lateinit var prevMonthButton: ImageButton
    private lateinit var memoAdapter: MemoAdapter
    private lateinit var missionProfileRecyclerView: RecyclerView
    private lateinit var missionProfileAdapter: MissionMemberProfileAdapter
    private var memberList = mutableListOf<Member>()
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var missionRegistrationButton : Button
    private lateinit var missionCheckButton : Button

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
        val view = inflater.inflate(R.layout.fragment_mission_calendar, container, false)
        calendarRecyclerView = view.findViewById(R.id.calendarRecyclerView)
        monthYearText = view.findViewById(R.id.textViewMonthYear)

        nextMonthButton = view.findViewById(R.id.buttonPrevious)
        prevMonthButton = view.findViewById(R.id.buttonNext)

        selectedDate = LocalDate.now()
        setMonthView()

        missionProfileRecyclerView = view.findViewById(R.id.missionProfileRecyclerView)
        missionProfileRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        missionProfileAdapter = MissionMemberProfileAdapter(memberList)
        missionProfileRecyclerView.adapter = missionProfileAdapter

        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        missionRegistrationButton = view.findViewById(R.id.missionRegistrationButton)
        missionCheckButton = view.findViewById(R.id.missionCheckButton)

        fetchUserFamilyCode()
        val selectedMember = memberList.firstOrNull()

        nextMonthButton.setOnClickListener{
            selectedDate = selectedDate.minusMonths(1)
            setMonthView()
        }

        prevMonthButton.setOnClickListener{
            selectedDate = selectedDate.plusMonths(1)
            setMonthView()
        }

        missionRegistrationButton.setOnClickListener{
            selectedMember?.let { member ->
                openMissionRegistrationFragment(member)
            }
        }

        missionCheckButton.setOnClickListener{
            selectedMember?.let { member ->
                openMissionCheckFragment(member)
            }
        }



        return view
    }

    private fun setMonthView() {
        monthYearText.text = CalendarUtils.monthYearFromDate(selectedDate)
        val daysInMonth = CalendarUtils.daysInMonthArray(selectedDate)

        val calendarAdapter = CalendarAdapter(daysInMonth) { position ->
            val day = daysInMonth[position]
            if (day.isNotEmpty()) {
                val selectedDay = LocalDate.of(selectedDate.year, selectedDate.month, day.toInt())
                showMemoDialog(selectedDay)
            }
        }
        val layoutManager = GridLayoutManager(requireContext(), 7)
        calendarRecyclerView.layoutManager = layoutManager
        calendarRecyclerView.adapter = calendarAdapter
    }


    private fun showMemoDialog(date: LocalDate) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_memo, null)
        val memoEditText = dialogView.findViewById<EditText>(R.id.editTextMemo)
        val recyclerViewMemos = dialogView.findViewById<RecyclerView>(R.id.recyclerViewMemos)

        val memos = loadMemos(date)

        memoAdapter = MemoAdapter(memos,
            onDeleteClick = { position ->
                memos.removeAt(position)
                saveMemos(date, memos)
                memoAdapter.notifyDataSetChanged()
            },
            onEditClick = { position ->
                memoEditText.setText(memos[position].content)
                memos.removeAt(position)
                saveMemos(date, memos)
                memoAdapter.notifyDataSetChanged()
            })

        recyclerViewMemos.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewMemos.adapter = memoAdapter

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("${CalendarUtils.monthYearFromDate(date)} ${date.dayOfMonth}의 메모")
            .setView(dialogView)
            .setPositiveButton("저장") { _, _ ->
                val memoContent = memoEditText.text.toString()
                if (memoContent.isNotEmpty()) {
                    val memo = Memo(memoContent, date)
                    memos.add(memo)
                    saveMemos(date, memos)
                    memoAdapter.notifyDataSetChanged()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun loadMemos(date: LocalDate): MutableList<Memo> {
        val sharedPreferences = requireContext().getSharedPreferences("memos", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString(date.toString(), "[]")
        return if (json != null) {
            val type = object : TypeToken<MutableList<Memo>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }
    private fun saveMemos(date: LocalDate, memos: List<Memo>) {
        val sharedPreferences = requireContext().getSharedPreferences("memos", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(memos)
        editor.putString(date.toString(), json)
        editor.apply()
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
                    missionProfileAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "데이터를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                    Log.e("RandomQuestionFragment", "Database error: ${error.message}")
                }

            })
    }

    private fun openMissionRegistrationFragment(member: Member) {
        val fragment = MissionRegistrationFragment().apply {
            arguments = Bundle().apply {
                putSerializable("selectedMember", member)
            }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.mainFrameLayout, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun openMissionCheckFragment(member: Member) {
        val fragment = MissionCheckFragment().apply {
            arguments = Bundle().apply {
                putSerializable("selectedMember", member)
            }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.mainFrameLayout, fragment)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MissionCalendarFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MissionCalendarFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}

data class Memo(
    var content: String,
    val date: LocalDate
)