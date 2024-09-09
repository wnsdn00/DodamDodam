package com.explorit.dodamdodam

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView
import java.time.LocalDate

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MissionRegistrationFragment.newInstance] factory method to
 * create an instance of this fragment.
 */


class MissionRegistrationFragment : Fragment() {


    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var selectedMember: Member
    private lateinit var missionCompleteButton: Button
    private var isMissionComplete: Boolean = false

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
        val view = inflater.inflate(R.layout.fragment_mission_registration, container, false)



        missionCompleteButton = view.findViewById(R.id.missionCompleteButton)
        selectedMember = arguments?.getSerializable("selectedMember", Member::class.java)!!

        val profileImageView: CircleImageView = view.findViewById(R.id.registrationMemberProfile)
        val nameTextView: TextView = view.findViewById(R.id.registrationMemberNameTextView)
        val timeEditText: EditText = view.findViewById(R.id.timeEditText)
        val contentEditText: EditText = view.findViewById(R.id.editTextMissionContent)
        val saveMissionButton = view.findViewById<Button>(R.id.saveMissionButton)

        nameTextView.text = selectedMember.nickName

        selectedMember.profileUrl?.let { loadMissionRegistrationProfileImage(profileImageView, it) }

        loadExistingMission(timeEditText, contentEditText)


        missionCompleteButton.setOnClickListener {
            isMissionComplete = !isMissionComplete
            updateMissionCompleteButton()
        }

        saveMissionButton.setOnClickListener {
            val selectedDays = getSelectedDays()
            val time = timeEditText.text.toString()
            val content = contentEditText.text.toString()
            val receivedById = selectedMember.userId
            val receivedByName = selectedMember.nickName
            val registeredById = FirebaseAuth.getInstance().currentUser?.uid


            if (registeredById != null && receivedById != null && receivedByName != null &&selectedDays.isNotEmpty() && time.isNotBlank() && content.isNotBlank()) {
                saveMission(
                registeredById = registeredById,
                receivedById = receivedById,
                receivedByName = receivedByName,
                selectedDays = selectedDays,
                time = time,
                content = content
            )
            } else {
                Toast.makeText(context, "모든 필드를 채워주세요.", Toast.LENGTH_SHORT).show()
            }
        }
        return view
    }

    private fun loadExistingMission(timeEditText: EditText, contentEditText: EditText) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        val database = FirebaseDatabase.getInstance().reference

        if(currentUserUid != null) {
            database.child("users").child(currentUserUid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val familyCode = snapshot.child("familyCode").getValue(String::class.java)
                    val selectedMemberUid = selectedMember.userId
                    if (familyCode != null && selectedMemberUid != null) {
                        database.child("families").child(familyCode).child("missions").child(selectedMemberUid).child(currentUserUid).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(missionSnapshot: DataSnapshot) {
                                if(missionSnapshot.exists()) {
                                    val mission = missionSnapshot.getValue(Mission::class.java)
                                    mission?.let {
                                        val today = LocalDate.now()

                                        if (today.isAfter(LocalDate.parse(it.timestamp))) {
                                            it.complete = false
                                            it.timestamp = today.toString() // 현재 날짜로 업데이트
                                            database.child("families").child(familyCode).child("missions").child(selectedMemberUid).child(currentUserUid).setValue(it)
                                        }

                                        timeEditText.setText(it.time)
                                        contentEditText.setText(it.content)
                                        view?.post {
                                            fillMissionDays(it.days)
                                            isMissionComplete = it.complete
                                            updateMissionCompleteButton()
                                        }
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("MissionRegistrationFragment", "Database error: ${error.message}")
                            }
                        })
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("MissionRegistrationFragment", "Database error: ${error.message}")
                }
            })
        }
    }

    private fun fillMissionDays(days: List<String>) {
        val dayCheckBoxes = getDayCheckBoxes()
        dayCheckBoxes.forEach {checkBox ->
            val day = checkBox.text.toString()
            val isChecked = days.contains((day))
            Log.d("fillMissionDays", "Day: $day, Should be checked: $isChecked")

            checkBox.isChecked = isChecked
        }
    }

    private fun getDayCheckBoxes(): List<CheckBox> {
        val rootView = view
        if (rootView == null) {
            Log.e("MissionRegistrationFragment", "Root view is null")
            return emptyList() // rootView가 null이면 빈 리스트 반환
        }
        return listOfNotNull(
            rootView.findViewById(R.id.checkBoxMonday),
            rootView.findViewById(R.id.checkBoxTuesday),
            rootView.findViewById(R.id.checkBoxWednesday),
            rootView.findViewById(R.id.checkBoxThursday),
            rootView.findViewById(R.id.checkBoxFriday),
            rootView.findViewById(R.id.checkBoxSaturday),
            rootView.findViewById(R.id.checkBoxSunday)
        )
    }

    private fun updateMissionCompleteButton() {
        if(isMissionComplete) {
            missionCompleteButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green))
        } else {
            missionCompleteButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray))
        }
    }

    private fun saveMission(
        registeredById: String,
        receivedById: String,
        receivedByName: String,
        selectedDays: List<String>,
        time: String,
        content: String,
    ) {
        val database = FirebaseDatabase.getInstance().reference
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        val today = LocalDate.now()
        if (currentUserUid != null) {
            database.child("users").child(currentUserUid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val familyCode = snapshot.child("familyCode").getValue(String::class.java)
                        if (familyCode != null) {
                            val mission = Mission(
                                registeredById = registeredById,
                                receivedById = receivedById,
                                receivedByName = receivedByName,
                                days = selectedDays,
                                time = time,
                                content = content,
                                complete = isMissionComplete,
                                timestamp = today.toString()// 현재 시간 저장
                            )


                            // 기존 미션 덮어 쓰기
                            database.child("families").child(familyCode).child("missions")
                                .child(receivedById).child(currentUserUid).setValue(mission)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Toast.makeText(context, "미션이 저장되었습니다.", Toast.LENGTH_SHORT)
                                            .show()
                                        parentFragmentManager.popBackStack()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "미션 저장에 실패했습니다.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }

                        } else {
                            Toast.makeText(context, "가족 그룹을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                        }


                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, "데이터를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                        Log.e("MissionRegistrationFragment", "Database error: ${error.message}")
                    }


                })
        } else {
            Toast.makeText(context, "사용자가 로그인되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
            Log.e("MissionRegistrationFragment", "Current user UID is null")
        }

    }

    private fun getSelectedDays(): List<String> {
        val selectedDays = mutableListOf<String>()

        if (view?.findViewById<CheckBox>(R.id.checkBoxMonday)?.isChecked == true) {
            selectedDays.add("월")
        }
        if (view?.findViewById<CheckBox>(R.id.checkBoxTuesday)?.isChecked == true) {
            selectedDays.add("화")
        }
        if (view?.findViewById<CheckBox>(R.id.checkBoxWednesday)?.isChecked == true) {
            selectedDays.add("수")
        }
        if (view?.findViewById<CheckBox>(R.id.checkBoxThursday)?.isChecked == true) {
            selectedDays.add("목")
        }
        if (view?.findViewById<CheckBox>(R.id.checkBoxFriday)?.isChecked == true) {
            selectedDays.add("금")
        }
        if (view?.findViewById<CheckBox>(R.id.checkBoxSaturday)?.isChecked == true) {
            selectedDays.add("토")
        }
        if (view?.findViewById<CheckBox>(R.id.checkBoxSunday)?.isChecked == true) {
            selectedDays.add("일")
        }

        return selectedDays
    }



    private fun loadMissionRegistrationProfileImage(imageView: ImageView, url: String) {
        Glide.with(imageView.context)
            .load(url)
            .error(R.drawable.ic_profile)
            .into(imageView)
    }




    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MissionRegistrationFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MissionRegistrationFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}

data class Mission(
    val registeredById: String = "", // 미션을 등록한 사람의 ID
    val receivedById: String = "", // 미션을 받은 사람의 ID
    val receivedByName: String = "", // 미션을 받은 사람의 이름
    val days: List<String> = listOf(), // 선택한 요일
    val time: String = "", // 미션 시간
    val content: String = "", // 미션 내용
    var complete: Boolean = false, // 미션 완료 여부 추가
    var timestamp: String = ""  // 서버 타임 스탬프(날짜가 지나면 완료 여부 false로 바꾸기 위함)
)



