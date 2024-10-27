package com.explorit.dodamdodam

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import android.text.method.LinkMovementMethod
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.explorit.dodamdodam.databinding.FragmentMyPageBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
* A simple [Fragment] subclass.
* Use the [MissionRegistrationFragment.newInstance] factory method to
* create an instance of this fragment.
*/

class MyPageFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var backToMainButton: ImageButton
    private lateinit var btnStore: ImageButton
    private lateinit var btnCustomize: ImageButton
    private lateinit var btnCustomerService: Button
    private lateinit var btnAppInfo: Button
    private lateinit var btnPreference: Button

    private lateinit var profileImageView: ImageView
    private lateinit var nickNameView: TextView
    private lateinit var userNameView: TextView
    private lateinit var userBirthView: TextView
    private lateinit var familyCodeView: TextView
    private lateinit var familyNameView: TextView
    private lateinit var copyButton: ImageButton
    private lateinit var editButton: ImageButton

    private lateinit var firestore: FirebaseFirestore
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

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
        val view = inflater.inflate(R.layout.fragment_my_page, container, false)
        backToMainButton = view.findViewById(R.id.myPageBackBtn)
        btnStore = view.findViewById(R.id.btn_store)
        btnCustomize = view.findViewById(R.id.btn_customize)
        btnCustomerService = view.findViewById(R.id.btn_customer_service)
        btnAppInfo = view.findViewById(R.id.btn_app_info)
        btnPreference = view.findViewById(R.id.btn_setting)

        profileImageView = view.findViewById(R.id.profile_image)
        nickNameView = view.findViewById(R.id.user_nickName)
        userNameView = view.findViewById(R.id.user_name)
        userBirthView = view.findViewById(R.id.user_birthday)
        familyCodeView = view.findViewById(R.id.family_code)
        familyNameView = view.findViewById(R.id.family_name)

        copyButton = view.findViewById(R.id.btn_copy_family_code)
        editButton = view.findViewById(R.id.btn_edit_family_card)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        database = FirebaseDatabase.getInstance().reference



        loadUserData()

        copyButton.setOnClickListener {
            copyFamilyCodeToClipboard()
        }

        editButton.setOnClickListener {
            openEditFragment()
        }

       btnStore.setOnClickListener {
            openStoreFragment()
        }

       btnCustomize.setOnClickListener {
            openCustomizeFragment()
        }

        btnCustomerService.setOnClickListener {
            // 고객센터 버튼 클릭 이벤트 처리
            val intent = Intent(activity, ServiceCenterActivity::class.java)
            startActivity(intent)
        }

        btnAppInfo.setOnClickListener {
            // 앱 정보 버튼 클릭 이벤트 처리
            val intent = Intent(activity, AboutAppActivity::class.java)
            startActivity(intent)
        }

        btnPreference.setOnClickListener {
            val intent = Intent(context, PreferenceActivity::class.java)
            startActivity(intent)
        }


        backToMainButton.setOnClickListener {
            (activity as? MainPageActivity)?.setFragment(TAG_HOME, HomeFragment(), false)
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            (activity as? MainPageActivity)?.setFragment(TAG_HOME, HomeFragment(), false)
        }


        return view
    }

    private fun copyFamilyCodeToClipboard() {
        val familyCode = familyCodeView.text.toString()
        if (familyCode.isNotEmpty()) {
            // 클립보드 관리자 가져오기
            val clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            // 클립보드에 복사할 텍스트 데이터 생성
            val clip = ClipData.newPlainText("Family Code", familyCode)
            // 클립보드에 복사
            clipboard.setPrimaryClip(clip)
            // 사용자에게 복사 완료 메시지 표시
            Toast.makeText(requireContext(), "가족코드가 복사되었습니다.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "가족코드가 없습니다.", Toast.LENGTH_SHORT).show()
        }
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
                                val profileImageUrl = userDataSnapshot.child("profileUrl").value.toString()

                                nickNameView.text = nickName
                                userNameView.text = userName
                                userBirthView.text = userBirth

                                if (profileImageUrl.isNotEmpty()) {
                                    Glide.with(this)
                                        .load(profileImageUrl)
                                        .into(profileImageView!!)
                                } else {
                                    // 기본 이미지 설정
                                    profileImageView?.setImageResource(R.drawable.ic_profile)
                                }

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

    private fun openEditFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.mainFrameLayout, MyPageEditFragment())
            .addToBackStack(null)
            .commit()
    }


    private fun openStoreFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.mainFrameLayout, StoreFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun openCustomizeFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.mainFrameLayout, CustomizeFragment())
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
            MyPageFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

}