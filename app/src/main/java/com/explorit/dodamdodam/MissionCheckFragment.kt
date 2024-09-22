package com.explorit.dodamdodam

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MissionCheckFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MissionCheckFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var selectedMember: Member
    private lateinit var backButton: ImageButton

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
        val view = inflater.inflate(R.layout.fragment_mission_check, container, false)

        selectedMember = arguments?.getSerializable("selectedMember", Member::class.java)!!
        backButton = view.findViewById(R.id.missionCheckBackBtn)

        val profileImageView: CircleImageView = view.findViewById(R.id.checkMemberProfileImageView)
        val nameTextView: TextView = view.findViewById(R.id.checkMemberNameTextView)

        nameTextView.text = selectedMember.nickName
        selectedMember.profileUrl?.let { loadMissionCheckProfileImage(profileImageView, it) }
        loadMission()

        val buttonMissionCheck = view.findViewById<Button>(R.id.buttonMissionCheck)
        buttonMissionCheck.setOnClickListener{
            parentFragmentManager.popBackStack()
        }

        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            parentFragmentManager.popBackStack()
        }

        return view
    }

    private fun loadMission() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        val database = FirebaseDatabase.getInstance().reference
        if (currentUserUid != null) {
            database.child("users").child(currentUserUid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val familyCode = snapshot.child("familyCode").getValue(String::class.java)
                        val selectedMemberUid = selectedMember.userId
                        if (familyCode != null && selectedMemberUid != null) {
                            database.child("families").child(familyCode).child("missions").child(currentUserUid).orderByChild("registeredById").equalTo(selectedMemberUid)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if(snapshot.exists()){
                                            for (missionSnapshot in snapshot.children) {
                                                try {
                                                    Log.d("MissionCheckFragment", "Mission Snapshot: ${missionSnapshot.value}")
                                                        val mission = missionSnapshot.getValue(Mission::class.java)
                                                        if (mission != null) {
                                                            displayMission(mission)
                                                        } else {
                                                            Toast.makeText(context, "미션을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                                                        }

                                                } catch (e: Exception) {
                                                    Log.e(
                                                        "MissionCheckFragment",
                                                        "Mission object conversion error: ${e.message}"
                                                    )
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "사용자에게 등록된 미션이 없습니다.", Toast.LENGTH_SHORT).show()
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


    }

    private fun displayMission(mission: Mission) {
        val missionContentTextView = view?.findViewById<TextView>(R.id.missionContentTextView)
        val missionTimeTextView = view?.findViewById<TextView>(R.id.missionTimeTextView)
        val missionDaysTextView = view?.findViewById<TextView>(R.id.missionDaysTextView)
        val missionCompleteButton = view?.findViewById<ImageButton>(R.id.missionCompleteCheckButton)

        missionContentTextView?.text = mission.content
        missionTimeTextView?.text = mission.time
        missionDaysTextView?.text = mission.days.joinToString(", ")

        val colorResId = if (mission.complete) R.drawable.check_green else R.drawable.check_gray
        if (missionCompleteButton != null) {
            missionCompleteButton.setBackgroundResource(colorResId)
        }
    }

    private fun loadMissionCheckProfileImage(imageView: ImageView, url: String) {
        Glide.with(imageView.context)
            .load(url)
            .error(R.drawable.ic_profile)
            .into(imageView)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MissionCheckFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MissionCheckFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}