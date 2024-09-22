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

        // 생성 버튼 클릭 시
        btnCreateFamilyFinish.setOnClickListener {
            val familyName = familyNameEditText.text.toString()
            val familyPassword = familyPasswordEditText.text.toString()
            val nickName = nicknameEditText.text.toString()

            createFamily(familyName, familyPassword, nickName)

        }
    }

    // 가족 그룹 생성 함수
    private fun createFamily(familyName: String, familyPassword: String, nickName: String){
        val userId: String? = FirebaseAuth.getInstance().currentUser?.uid
        val familyCode: String? = database.child("families").push().key

        if (userId != null && familyCode != null) {
            val family = Family(familyCode, familyName, familyPassword, userId)
            val member = Member(userId, nickName)

            database.child("families").child(familyCode).setValue(family).addOnCompleteListener{ task ->
                    if(task.isSuccessful) {
                    // 가족 생성 성공
                    database.child("families").child(familyCode).child("members").child(userId).setValue(member).addOnCompleteListener { memberTask ->
                        if (memberTask.isSuccessful) {
                            database.child("users").child(userId).child("familyCode").setValue(familyCode)

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
    val profileUrl: String? = "android.resource://com.explorit.dodamdodam/drawable/ic_profile",
    var hasAnswered: Boolean = false
) : Serializable

{
    // 기본 생성자
    constructor() : this(null, null, null)
}