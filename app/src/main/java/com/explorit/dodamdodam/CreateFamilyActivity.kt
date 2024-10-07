package com.explorit.dodamdodam

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.io.Serializable

class CreateFamilyActivity : AppCompatActivity() {
    private lateinit var familyNameEditText : EditText
    private lateinit var familyPasswordEditText : EditText
    private lateinit var nicknameEditText : EditText
    private lateinit var btnCreateFamilyFinish : Button
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private lateinit var userName: String
    private lateinit var userBirth: String

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_family)
        // 가족 생성 화면

        familyNameEditText = findViewById(R.id.editFamilyName)
        familyPasswordEditText = findViewById(R.id.editFamilyPW)
        nicknameEditText = findViewById(R.id.editNickname)
        btnCreateFamilyFinish = findViewById(R.id.btnCreateFamilyFinish)
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        // 생성 버튼 클릭 시
        btnCreateFamilyFinish.setOnClickListener {
            val familyName = familyNameEditText.text.toString()
            val familyPassword = familyPasswordEditText.text.toString()
            val nickName = nicknameEditText.text.toString()

            val userId = auth.currentUser?.uid
            if (userId != null) {
                database.child("users").child(userId).get().addOnSuccessListener { dataSnapshot ->
                    if (dataSnapshot.exists()) {
                        userName = dataSnapshot.child("userName").value.toString()
                        userBirth = dataSnapshot.child("userBirth").value.toString()
                        createFamily(familyName, familyPassword, nickName, userName, userBirth)
                    } else {
                        Toast.makeText(this, "사용자 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "데이터를 가져오는 중 오류 발생", Toast.LENGTH_SHORT).show()
                }
            }


        }
    }

    // 가족 그룹 생성 함수
    private fun createFamily(familyName: String, familyPassword: String, nickName: String, userName: String, userBirth: String){
        val userId: String? = FirebaseAuth.getInstance().currentUser?.uid
        val familyCode: String? = database.child("families").push().key

        if (userId != null && familyCode != null) {
            val family = Family(familyCode, familyName, familyPassword, userId)
            val member = Member(userId, nickName, userName, userBirth)

            database.child("families").child(familyCode).setValue(family).addOnCompleteListener{ task ->
                    if(task.isSuccessful) {
                    // 가족 생성 성공
                    database.child("families").child(familyCode).child("members").child(userId).setValue(member).addOnCompleteListener { memberTask ->
                        if (memberTask.isSuccessful) {
                            database.child("users").child(userId).child("familyCode").setValue(familyCode)
                            database.child("users").child(userId).child("familyName").setValue(familyName)

                            val defaultCharacter = StoreItem(
                                name = "복실이",
                                imageUrl = "https://firebasestorage.googleapis.com/v0/b/dodamdodam-b1e37.appspot.com/o/storeItemImages%2FCharacter01.png?alt=media&token=57eefb5b-e428-49de-981b-70e6089272c2",
                                price = 200,
                                itemCategory = "character"
                            )
                            val itemId = database.child("families").child(familyCode).child("purchasedItems").push().key
                            if (itemId != null) {
                                database.child("families")
                                    .child(familyCode)
                                    .child("purchasedItems")
                                    .child(itemId)
                                    .setValue(defaultCharacter)
                            }
                            database.child("families").child(familyCode).child("mainScreenItems").setValue(defaultCharacter)

                            // 생성된 가족 코드를 클립보드에 복사
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Family Code", familyCode)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(this, "가족 코드: $familyCode 가 클립보드에 저장되었습니다.", Toast.LENGTH_LONG).show()

                            val intent = Intent(this, finishCreateFamilyActivity::class.java)
                            startActivity(intent)
                        }
                    }
                } else {
                    // 가족 생성 실패
                        Toast.makeText(this, "가족 생성에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

// 가족 데이터 클래스
data class Family(
    val familyCode: String,
    val familyName: String,
    val familyPassWord: String,
    val creatorId: String,
    val members: MutableList<String> = mutableListOf(),
    val questionNoList: MutableList<Int> = mutableListOf(),
    val todayQuestion: MutableList<String> = mutableListOf(),
    val questionHistory: MutableList<String> = mutableListOf(),
    val familyCoin: Int = 100
)

// 가족 구성원 데이터 클래스
data class Member(
    val userId: String? = null,
    val nickName: String? = null,
    val userName: String? = null,
    val userBirth: String? = null,
    val familyName: String? = null,
    val profileUrl: String? = "https://firebasestorage.googleapis.com/v0/b/dodamdodam-b1e37.appspot.com/o/profile.png?alt=media&token=9da0e69f-7001-4bae-a286-e16f6680ee1e",
    var hasAnswered: Boolean = false
) : Serializable

{
    // 기본 생성자
    constructor() : this(null, null, null)
}