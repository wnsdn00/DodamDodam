package com.explorit.dodamdodam

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class MyPageEditFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var firestore: FirebaseFirestore
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private lateinit var nickNameView: TextView
    private lateinit var userNameView: TextView
    private lateinit var userBirthView: TextView
    private lateinit var familyCodeView: TextView
    private lateinit var familyNameView: TextView

    private lateinit var btnPhotoChange: Button
    private lateinit var btnNicknameChange: Button


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
        val view = inflater.inflate(R.layout.fragment_my_page_edit, container, false)
        nickNameView = view.findViewById(R.id.user_nickName)
        userNameView = view.findViewById(R.id.user_name)
        userBirthView = view.findViewById(R.id.user_birthday)
        familyCodeView = view.findViewById(R.id.family_code)
        familyNameView = view.findViewById(R.id.family_name)

        btnPhotoChange = view.findViewById(R.id.btn_photo_change)
        btnNicknameChange = view.findViewById(R.id.btn_nickname_change)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        database = FirebaseDatabase.getInstance().reference


        loadUserData()

        btnPhotoChange.setOnClickListener {
            // 사진 수정하는 다이얼로그
            //showEditPhotoDialog()
        }

        btnNicknameChange.setOnClickListener {
            // 호칭 수정하는 다이얼로그
            //showEditNicknameChangeDialog()
        }

        return view
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid

        if (userId != null) {
            database.child("users").child(userId).get().addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    val familyCode = dataSnapshot.child("familyCode").value.toString()
                    val familyName = dataSnapshot.child("familyName").value.toString()
                    if(familyCode != null) {
                        familyCodeView.text = familyCode
                        familyNameView.text = familyName
                        database.child("families").child(familyCode).child("members").child(userId).get().addOnSuccessListener { userDataSnapshot ->
                            if(userDataSnapshot.exists()){
                                val nickName = userDataSnapshot.child("nickName").value.toString()
                                val userName = userDataSnapshot.child("userName").value.toString()
                                val userBirth = userDataSnapshot.child("userBirth").value.toString()
                                nickNameView.text = nickName
                                userNameView.text = userName
                                userBirthView.text = userBirth

                            } else {
                                Toast.makeText(requireContext(), "사용자 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }.addOnFailureListener {
                            Toast.makeText(requireContext(), "데이터를 가져오는 중 오류 발생", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "사용자의 가족 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "사용자 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "데이터를 가져오는 중 오류 발생", Toast.LENGTH_SHORT).show()
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
         * @return A new instance of fragment MissionCalendarFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MyPageFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}