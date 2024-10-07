package com.explorit.dodamdodam

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class MyPageEditFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var firestore: FirebaseFirestore
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private lateinit var profileImageView: ImageView
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

        profileImageView = view.findViewById(R.id.profile_image)
        nickNameView = view.findViewById(R.id.nickName)
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
            showEditPhotoDialog()
        }

        btnNicknameChange.setOnClickListener {
            // 호칭 수정하는 다이얼로그
            showEditNicknameChangeDialog()
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

    private fun showEditPhotoDialog() {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("프로필 사진 변경")
            .setMessage("프로필 사진을 변경하시겠습니까?")
            .setPositiveButton("갤러리에서 선택") { _, _ ->
                selectImageFromGallery()
            }
            .setNegativeButton("취소", null)
            .create()

        dialog.show()
    }

    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            // 선택한 이미지 처리
            val imageUri = data?.data
            // 이미지 뷰 업데이트
            val storageRef = FirebaseStorage.getInstance().reference.child("profileImages/${auth.currentUser?.uid}.jpg")
            storageRef.putFile(imageUri!!)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        // 이미지 URL을 Realtime Database에 저장
                        val imageUrl = uri.toString()
                        val familyCode = familyCodeView.text.toString()


                            database.child("families").child(familyCode).child("members").child(auth.currentUser?.uid!!)
                                .child("profileUrl").setValue(imageUrl)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        requireContext(),
                                        "프로필 사진이 업데이트되었습니다.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        requireContext(),
                                        "프로필 사진 업데이트에 실패했습니다.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "이미지 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    @SuppressLint("MissingInflatedId")
    private fun showEditNicknameChangeDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_edit_nickname, null)
        val editText = dialogLayout.findViewById<EditText>(R.id.editTextNickname)

        builder.setTitle("닉네임 수정")
        builder.setView(dialogLayout)
        builder.setPositiveButton("저장") { _, _ ->
            val newNickname = editText.text.toString()

            // Realtime Database에 닉네임 저장
            val userId = auth.currentUser?.uid
            val familyCode = familyCodeView.text.toString()

            if (userId != null) {
                database.child("families").child(familyCode).child("members").child(userId).child("nickName").setValue(newNickname)
                    .addOnSuccessListener {
                        nickNameView.text = newNickname
                        Toast.makeText(requireContext(), "닉네임이 업데이트되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "닉네임 업데이트에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
            }
        }
        builder.setNegativeButton("취소", null)
        builder.show()
    }



    companion object {
        private const val REQUEST_IMAGE_PICK = 1001
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