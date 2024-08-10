package com.explorit.dodamdodam

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class JoinFamilyActivity : AppCompatActivity() {
    private lateinit var inputFamilyPW: EditText
    private lateinit var inputNickname: EditText
    private lateinit var btnJoinFamilyFinish: Button
    private lateinit var database: DatabaseReference

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_family)

        inputFamilyPW = findViewById(R.id.inputFamilyPW)
        inputNickname = findViewById(R.id.inputNickname)
        btnJoinFamilyFinish = findViewById(R.id.btnJoinFamilyFinish)
        database = FirebaseDatabase.getInstance().reference

        val familyCode = intent.getStringExtra("familyCode") ?: return

        btnJoinFamilyFinish.setOnClickListener {
            val familyPW = inputFamilyPW.text.toString()
            val nickName = inputNickname.text.toString()

            if (familyPW.isNotEmpty() && nickName.isNotEmpty()) {
                checkPassWordAndJoinFamily(familyCode, familyPW, nickName)
            } else {
                Toast.makeText(this, "비밀번호와 호칭을 둘 다 입력해 주세요.", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun checkPassWordAndJoinFamily(familyCode: String, password: String, nickName: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if(userId != null) {
            database.child("families").child(familyCode).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val familyPassword =
                            dataSnapshot.child("familyPassWord").getValue(String::class.java)
                        if (familyPassword == password) {
                            addMemberToFamily(userId, familyCode, nickName)
                        } else {
                            Toast.makeText(this@JoinFamilyActivity, "비밀번호가 틀렸습니다.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@JoinFamilyActivity, "코드가 잘못 됐습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(this@JoinFamilyActivity, "참여에 실패했습니다: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                }
            })


        }
    }

    private fun addMemberToFamily(userId: String, familyCode: String?, nickName:String) {
        if(familyCode != null) {
            // 가족 그룹 내에서 호칭이 이미 사용 중인지 확인
            val familyRef = database.child("families").child(familyCode).child("members")
            familyRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    var nickNameExists = false

                    // 모든 가족 구성원의 닉네임을 확인
                    for (memberSnapshot in dataSnapshot.children) {
                        val member = memberSnapshot.getValue(Member::class.java)
                        if (member != null && member.nickName == nickName) {
                            nickNameExists = true
                            break
                        }
                    }

                    if (nickNameExists) {
                        Toast.makeText(this@JoinFamilyActivity, "이미 사용 중인 호칭입니다. 다른 호칭을 선택해 주세요.", Toast.LENGTH_SHORT).show()
                    } else {
                        // 중복되지 않으면 멤버를 추가
                        val member = Member(userId, nickName)
                        familyRef.child(userId).setValue(member).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                database.child("users").child(userId).child("familyCode").setValue(familyCode)
                                val intent = Intent(this@JoinFamilyActivity, MainPageActivity::class.java)
                                startActivity(intent)
                            } else {
                                Toast.makeText(this@JoinFamilyActivity, "가족 참여에 실패 했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(this@JoinFamilyActivity, "참여에 실패했습니다: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}