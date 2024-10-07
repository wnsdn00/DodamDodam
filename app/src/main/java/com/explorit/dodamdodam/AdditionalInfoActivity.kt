package com.explorit.dodamdodam

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore


class AdditionalInfoActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var database: DatabaseReference

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_additional_info)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        database = FirebaseDatabase.getInstance().reference

        val addInfoButton: Button = findViewById(R.id.buttonAddInfo)
        val logoutInfoButton: Button = findViewById(R.id.buttonLogout)

        addInfoButton.setOnClickListener {
                addInfo()
        }

        logoutInfoButton.setOnClickListener {
            val intent = Intent(this, LogoutActivity::class.java)
            startActivity(intent)
        }
    }

    private fun addInfo() { // xml에 있는 id의 이름을 가져와서 객체로 생성
        val username = findViewById<EditText>(R.id.additionalUserName).text.toString()
        val userbirth = findViewById<EditText>(R.id.additionalUserBirth).text.toString()
        val userEmail = auth.currentUser?.email
        val userPhone = findViewById<EditText>(R.id.additionalPhoneNumber).text.toString()

        if (username.isEmpty() || userbirth.isEmpty()) {
            Toast.makeText(this, "모든 필드를 입력하세요.", Toast.LENGTH_SHORT).show()
        } else {
            if (userEmail != null && userPhone != null) {
                saveUserInfo(username, userbirth, userEmail, userPhone)
            }
        }
    }

    private fun saveUserInfo(username: String, userbirth: String, useremail: String, userPhone: String) { // Firebase에 저장
        val user = hashMapOf( // 해시맵으로 username, email 필드에 저장
            "email" to useremail,
            "username" to username,
            "userbirth" to userbirth,
            "phone" to userPhone
        )

        // 생성된 ID로 새 문서 추가
        firestore.collection("users") // 여기서 컬렉션 이름과 같아야 합니다
            .add(user)
            .addOnSuccessListener { documentReference ->
                Log.d("AdditionalInfoActivity", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.e("AdditionalInfoActivity", "문서 추가 오류", e)
            }
        val userId = auth.currentUser?.uid
        if (userId != null) {
            database.child("users").child(userId).child("email")
                .setValue(useremail)
            database.child("users").child(userId).child("userName")
                .setValue(username)
            database.child("users").child(userId).child("userBirth")
                .setValue(userbirth)
            checkUserFamilyCode(userId)
        }

    }

    private fun checkUserFamilyCode(username: String) {
        //Firebase에서 familyCode가 있는지 확인
        database.child("users").child(username).child("familyCode")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists() && snapshot.getValue(String::class.java) != null) {
                        addInfoButtonClickToMainPage()
                    } else {
                        addInfoButtonClickToFamily()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(
                        "LoginActivity",
                        "checkUserFamilyCode:onCancelled",
                        error.toException()
                    )
                    Toast.makeText(this@AdditionalInfoActivity, "데이터베이스 오류", Toast.LENGTH_SHORT).show()
                }
            })
    }
    fun addInfoButtonClickToMainPage() {
        val intent = Intent(this, MainPageActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun addInfoButtonClickToFamily() {
        val intent = Intent(this, FamilyActivity::class.java)
        startActivity(intent)
        finish()
    }
}