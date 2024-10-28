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
import androidx.activity.addCallback
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.common.reflect.TypeToken
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.MutableData
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.core.Transaction
import com.google.gson.Gson
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale

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
    private lateinit var backToMainButton: ImageButton
    private lateinit var missionRegistrationButton : Button
    private lateinit var missionCheckButton : Button
    private lateinit var selectedMember : Member
    private var missionCompletedDates = mutableListOf<String>()

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
        // 미션 캘린더 화면

        calendarRecyclerView = view.findViewById(R.id.calendarRecyclerView)
        monthYearText = view.findViewById(R.id.textViewMonthYear)

        backToMainButton = view.findViewById(R.id.missionCalendarBackBtn)
        nextMonthButton = view.findViewById(R.id.buttonPrevious)
        prevMonthButton = view.findViewById(R.id.buttonNext)

        selectedDate = LocalDate.now()

        missionProfileRecyclerView = view.findViewById(R.id.missionProfileRecyclerView)
        missionProfileRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        missionProfileAdapter = MissionMemberProfileAdapter(memberList) { member ->
            selectedMember = member
        }
        missionProfileRecyclerView.adapter = missionProfileAdapter

        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()


        missionRegistrationButton = view.findViewById(R.id.missionRegistrationButton)
        missionCheckButton = view.findViewById(R.id.missionCheckButton)

        fetchUserFamilyCode()
        resetMissionCompletionIfNeeded { checkTodayMissionsComplete() }
        setMonthView()


        nextMonthButton.setOnClickListener{
            selectedDate = selectedDate.minusMonths(1)
            setMonthView()
        }

        prevMonthButton.setOnClickListener{
            selectedDate = selectedDate.plusMonths(1)
            setMonthView()
        }

        missionRegistrationButton.setOnClickListener{
            if (::selectedMember.isInitialized) {
                openMissionRegistrationFragment(selectedMember)
            } else {
                Toast.makeText(context, "멤버를 선택하지 않았습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        missionCheckButton.setOnClickListener{
            if (::selectedMember.isInitialized) {
                openMissionCheckFragment(selectedMember)
            } else {
                Toast.makeText(context, "멤버를 선택하지 않았습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        backToMainButton.setOnClickListener {
            (activity as? MainPageActivity)?.setFragment(TAG_HOME, HomeFragment(), false)
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            (activity as? MainPageActivity)?.setFragment(TAG_HOME, HomeFragment(), false)
        }

        return view
    }



    private fun resetMissionCompletionIfNeeded(onComplete: () -> Unit) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserUid != null) {
            // 사용자 UID를 기반으로 고유한 SharedPreferences 이름 생성
            val sharedPreferences = requireContext().getSharedPreferences("MissionPrefs_$currentUserUid", Context.MODE_PRIVATE)
            val lastResetDate = sharedPreferences.getString("lastResetDate", null)
            val today = LocalDate.now().toString()

            // 마지막으로 저장된 날짜와 오늘 날짜가 다를 경우에만 resetMissionCompletion() 실행
            if (lastResetDate == null || lastResetDate != today) {
                resetMissionCompletion {
                    // 미션 완료 상태를 초기화한 후에 마지막 실행 날짜를 업데이트
                    sharedPreferences.edit().putString("lastResetDate", today).apply()
                    onComplete()
                }
            } else {
                // 이미 오늘 실행했으므로 그냥 onComplete 호출
                onComplete()
            }
        } else {
            // 사용자 UID가 없을 때 처리
            onComplete()
        }
    }


    // 달력 화면 생성 함수
    private fun setMonthView() {
        monthYearText.text = CalendarUtils.monthYearFromDate(selectedDate)
        val daysInMonth = CalendarUtils.daysInMonthArray(selectedDate)

        val database = FirebaseDatabase.getInstance().reference
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserUid != null) {
            database.child("users").child(currentUserUid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val familyCode = snapshot.child("familyCode").getValue(String::class.java)
                        if (familyCode != null) {
                            database.child("families").child(familyCode).child("missions").child(currentUserUid).child("completedDates").addListenerForSingleValueEvent(object: ValueEventListener{
                                override fun onDataChange(datesSnapshot: DataSnapshot) {
                                    if (datesSnapshot.exists()) {
                                        missionCompletedDates.clear()
                                        for (dateSnapshot in datesSnapshot.children) {
                                            val date = dateSnapshot.getValue(String::class.java)
                                            if (date != null) {
                                                missionCompletedDates.add(date)
                                            }
                                        }

                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(context, "데이터를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                                    Log.e("RandomQuestionFragment", "Database error: ${error.message}")
                                }

                            })
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
        val calendarAdapter = CalendarAdapter(daysInMonth,selectedDate, { position ->
            val day = daysInMonth[position]
            if (day.isNotEmpty()) {
                val selectedDay =
                    LocalDate.of(selectedDate.year, selectedDate.month, day.toInt())
                showMemoDialog(selectedDay)
            }
        }, missionCompletedDates)


        val layoutManager = GridLayoutManager(requireContext(), 7)
        calendarRecyclerView.layoutManager = layoutManager
        calendarRecyclerView.adapter = calendarAdapter

    }

    // 달력 메모 함수
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

    // 작성한 달력 메모 불러오는 함수
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

    // 입력한 메모 저장하는 함수 (Json형식)
    private fun saveMemos(date: LocalDate, memos: List<Memo>) {
        val sharedPreferences = requireContext().getSharedPreferences("memos", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(memos)
        editor.putString(date.toString(), json)
        editor.apply()
    }

    // 사용자의 가족 그룹을 확인 하는 함수
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

    // 확인한 사용자의 가족그룹 구성원을 확인하는 함수
    private fun fetchMembers(familyCode: String) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        database.child("families").child(familyCode).child("members")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    memberList.clear()
                    for (data in snapshot.children) {
                        val member = data.getValue(Member::class.java)
                        if (member != null && member.userId != currentUserUid) {
                            memberList.add(member)
                        }
                    }
                    missionProfileAdapter.notifyDataSetChanged()

                    if (memberList.isNotEmpty()) {
                        selectedMember = memberList.firstOrNull()!!
                    } else {
                        Toast.makeText(context, "멤버가 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }


                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "데이터를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                    Log.e("RandomQuestionFragment", "Database error: ${error.message}")
                }

            })
    }

    // 미션 등록 프래그먼트를 띄워주는 함수
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

    // 미션 확인 프래그먼트를 띄워주는 함수
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

    // 사용자에게 주어진 오늘의 미션이 모두 완료 되었는지 확인하는 함수
    private fun checkTodayMissionsComplete() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        val database = FirebaseDatabase.getInstance().reference
        if (currentUserUid != null) {
            database.child("users").child(currentUserUid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val familyCode = snapshot.child("familyCode").getValue(String::class.java)
                    if (familyCode != null) {
                        database.child("families").child(familyCode).child("missions").child(currentUserUid).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(missionsSnapshot: DataSnapshot) {
                                val missions = mutableListOf<Mission>()
                                if (missionsSnapshot.exists()) {
                                    for (missionSnapshot in missionsSnapshot.children) {
                                        val value = missionSnapshot.getValue(Any::class.java)
                                        if (value !is List<*> && value !is String && value !is Boolean) {
                                            val mission = missionSnapshot.getValue(Mission::class.java)
                                            if (mission != null) {
                                                missions.add(mission)
                                            }
                                        }
                                    }

                                    val today = LocalDate.now()
                                    val todayMissions = missions.filter { mission ->
                                        mission.days.contains(
                                            today.dayOfWeek.getDisplayName(
                                                TextStyle.SHORT,
                                                Locale.KOREAN
                                            )
                                        )
                                    }
                                    val missionCompleted = todayMissions.isNotEmpty() && todayMissions.all { it.complete }

                                    val completedDatesRef = database.child("families")
                                        .child(familyCode)
                                        .child("missions")
                                        .child(currentUserUid)
                                        .child("completedDates")

                                    val todayMissionCompletedRef = database.child("families")
                                        .child(familyCode)
                                        .child("missions")
                                        .child(currentUserUid)
                                        .child("todayMissionCompleted")

                                    completedDatesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            val completedDates = snapshot.getValue(object : GenericTypeIndicator<MutableList<String>>() {}) ?: mutableListOf()
                                            val todayString = today.toString()

                                            setMonthView()

                                            if (missionCompleted) {
                                                if (!completedDates.contains(todayString)) {
                                                    completedDates.add(todayString)
                                                    completedDatesRef.setValue(completedDates).addOnCompleteListener { task ->
                                                        if (task.isSuccessful) {
                                                            database.child("families").child(familyCode).child("missions").child(currentUserUid).child("completedDay").setValue(todayString)
                                                            Log.d("MissionUpdate", "Mission completed date added successfully.")
                                                        } else {
                                                            Log.e("MissionUpdate", "Failed to update completed dates.")
                                                        }
                                                        setMonthView()  // UI 업데이트 호출
                                                    }
                                                    todayMissionCompletedRef.addListenerForSingleValueEvent(object: ValueEventListener{
                                                        override fun onDataChange(completedSnapshot: DataSnapshot) {
                                                            val todayMissionCompleted = completedSnapshot.getValue(Boolean::class.java)
                                                            if (todayMissionCompleted != true){
                                                                todayMissionCompletedRef.setValue(true)
                                                                rewardCoinsToFamily(familyCode, 10)
                                                            } else {
                                                                Toast.makeText(context, "오늘의 미션 코인을 이미 획득하셨습니다.", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }

                                                        override fun onCancelled(error: DatabaseError) {
                                                            TODO("Not yet implemented")
                                                        }
                                                    })
                                                }
                                            } else {
                                                if (completedDates.contains(todayString)) {
                                                    completedDates.remove(todayString)
                                                    completedDatesRef.setValue(completedDates).addOnCompleteListener { task ->
                                                        if (task.isSuccessful) {
                                                            Log.d("MissionUpdate", "Today's completed date removed successfully.")
                                                        } else {
                                                            Log.e("MissionUpdate", "Failed to remove today's completed date.")
                                                        }
                                                        setMonthView()  // UI 업데이트 호출
                                                    }

                                                }
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            Log.e("MissionUpdate", "Failed to read completed dates: ${error.message}")
                                        }
                                    })
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("MissionCalendarFragment", "Database error: ${error.message}")
                            }
                        })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MissionCalendarFragment", "Database error: ${error.message}")
                }
            })
        }
    }

    private fun rewardCoinsToFamily(familyCode: String, coins: Int) {
        // 현재 코인 값을 가져와서 증가시킨 후 저장
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

    // 하루가 지났을때 미션 완료 상태를 초기화 하는 함수
    private fun resetMissionCompletion(onComplete:() -> Unit) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        val today = LocalDate.now()

        if (currentUserUid != null) {
            database.child("users").child(currentUserUid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val familyCode = snapshot.child("familyCode").getValue(String::class.java)
                    if (familyCode != null) {
                        database.child("families").child(familyCode).child("missions").child(currentUserUid)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(missionsSnapshot: DataSnapshot) {
                                    if (missionsSnapshot.exists()) {
                                        for (missionSnapshot in missionsSnapshot.children) {
                                            val value = missionSnapshot.getValue(Any::class.java)
                                            val completedDay = missionsSnapshot.child("completedDay").getValue(String::class.java)
                                            if (value !is List<*> && value !is String && value !is Boolean)  {
                                                val mission = missionSnapshot.getValue(Mission::class.java)
                                                mission?.let {
                                                    // 만약 미션이 완료되었고, 오늘이 해당 미션이 완료된 날의 다음 날이라면
                                                    if (completedDay == null || today.isAfter(LocalDate.parse(completedDay))) {
                                                        missionSnapshot.ref.child("complete").setValue(false)
                                                        missionsSnapshot.ref.child("todayMissionCompleted")
                                                            .setValue(false) .addOnSuccessListener {
                                                                Log.d("MissionCalendarFragment", "todayMissionCompleted reset successfully")
                                                            }
                                                            .addOnFailureListener {
                                                                Log.e("MissionCalendarFragment", "Failed to reset todayMissionCompleted: ${it.message}")
                                                            }
                                                    }
                                                }
                                            }
                                        }
                                        onComplete()
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Log.e("MissionCalendarFragment", "Database error: ${error.message}")
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MissionCalendarFragment", "Database error: ${error.message}")
                }
            })
        }
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

// 메모 데이터 클래스
data class Memo(
    var content: String,
    val date: LocalDate
)
data class CompletedDatesList(
    var completedDates: MutableList<String> = mutableListOf()
)