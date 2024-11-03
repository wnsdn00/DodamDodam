package com.explorit.dodamdodam

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class JoinFamilyActivity : AppCompatActivity() {
    private lateinit var inputFamilyPW: EditText
    private lateinit var inputNickname: EditText
    private lateinit var btnJoinFamilyFinish: Button
    private lateinit var database: DatabaseReference
    private lateinit var firestore: FirebaseFirestore
    private lateinit var familyNameTextView: TextView
    private lateinit var userName: String
    private lateinit var userBirth: String

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_family)
        // 가족 코드 입력 후 화면

        inputFamilyPW = findViewById(R.id.inputFamilyPW)
        inputNickname = findViewById(R.id.inputNickname)
        btnJoinFamilyFinish = findViewById(R.id.btnJoinFamilyFinish)
        familyNameTextView = findViewById(R.id.familyNameText)
        database = FirebaseDatabase.getInstance().reference
        firestore = FirebaseFirestore.getInstance()

        val familyCode = intent.getStringExtra("familyCode") ?: return

        // 가족 코드로 가족 이름 가져오기
        database.child("families").child(familyCode).child("familyName").get().addOnSuccessListener { dataSnapshot ->
            val familyName = dataSnapshot.getValue(String::class.java)
            if (familyName != null) {
                familyNameTextView.text = familyName // 가져온 가족 이름을 TextView에 표시
            } else {
                Toast.makeText(this, "가족 이름을 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "가족 정보를 가져오는 중 오류 발생", Toast.LENGTH_SHORT).show()
        }

        // 가족 참여 버튼 클릭 시
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

    // 입력한 가족 비밀번호를 확인하는 함수
    private fun checkPassWordAndJoinFamily(familyCode: String, password: String, nickName: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if(userId != null) {
            database.child("families").child(familyCode).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val familyPassword =
                            dataSnapshot.child("familyPassWord").getValue(String::class.java)
                        val familyName = dataSnapshot.child("familyName").getValue(String::class.java)
                        if (familyPassword == password && familyName !== null) {
                            database.child("users").child(userId).get().addOnSuccessListener { userDataSnapshot ->
                                if (userDataSnapshot.exists()) {
                                    userName = userDataSnapshot.child("userName").value.toString()
                                    userBirth = userDataSnapshot.child("userBirth").value.toString()
                                    addMemberToFamily(userId, familyCode, nickName, userName, userBirth, familyName)
                                } else {
                                    Toast.makeText(this@JoinFamilyActivity, "사용자 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                                }
                            }.addOnFailureListener {
                                Toast.makeText(this@JoinFamilyActivity, "데이터를 가져오는 중 오류 발생", Toast.LENGTH_SHORT).show()
                            }

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

    // 올바른 비밀번호를 입력 했을 시 가족 구성원에 사용자를 추가하는 함수
    private fun addMemberToFamily(userId: String, familyCode: String?, nickName:String, userName: String, userBirth: String, familyName: String) {
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
                        val member = Member(userId, nickName, userName, userBirth)
                        familyRef.child(userId).setValue(member).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                database.child("users").child(userId).child("familyCode").setValue(familyCode)
                                database.child("users").child(userId).child("familyName").setValue(familyName)

                                // 참여한 가족 코드를 Firestore에 추가
                                    firestore.collection("users")
                                    .whereEqualTo("userId", userId)
                                    .get()
                                    .addOnSuccessListener { documents ->
                                        val document = documents.first()
                                        // Firestore에 userId 저장
                                        firestore.collection("users").document(document.id)
                                            .update("familyCode", FieldValue.arrayUnion(familyCode))
                                            .addOnSuccessListener {
                                                Log.d("LoginActivity", "userId가 Firestore에 성공적으로 저장되었습니다.")
                                            }
                                            .addOnFailureListener { e ->
                                                Log.w("LoginActivity", "Firestore에 userId 저장 실패", e)
                                            }
                                    }.addOnFailureListener { e ->
                                        Log.w("LoginActivity", "Firestore에 user 불러오기 실패", e)
                                    }

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