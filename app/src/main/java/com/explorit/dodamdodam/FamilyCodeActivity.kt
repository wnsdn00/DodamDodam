package com.explorit.dodamdodam

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FamilyCodeActivity : AppCompatActivity() {
    private lateinit var inputFamilyCode: EditText
    private lateinit var btn_CheckCode: Button
    private lateinit var database: DatabaseReference
    // 가족 참여를 원할때 가족 코드를 입력하는 화면

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_family_code)

        inputFamilyCode = findViewById(R.id.inputFamilyCode)
        btn_CheckCode = findViewById(R.id.btnCheckCode)
        database = FirebaseDatabase.getInstance().reference

        // 가족 코드 확인 버튼 클릭
        btn_CheckCode.setOnClickListener {
            val familyCode = inputFamilyCode.text.toString()
            if (familyCode.isNotEmpty()){
                checkFamilyCode(familyCode)
            } else {
                Toast.makeText(this, "코드를 입력해 주세요.", Toast.LENGTH_SHORT).show()
            }

        }
    }

    // 입력한 가족 코드를 확인 하는 함수
    private fun checkFamilyCode(familyCode: String) {
        // 초대 코드로 그룹 검색
        database.child("families").child(familyCode).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // 초대 코드가 유효한 경우 JoinFamilyActivity로 이동
                    val intent = Intent(this@FamilyCodeActivity, JoinFamilyActivity::class.java)
                    intent.putExtra("familyCode", familyCode)
                    startActivity(intent)
                } else {
                    Toast.makeText(this@FamilyCodeActivity, "코드를 다시 입력해 주세요", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@FamilyCodeActivity, "데이터베이스 오류: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

}