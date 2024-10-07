package com.explorit.dodamdodam

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FindPwActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var findPwName: EditText
    private lateinit var findPwPhoneNumber: EditText
    private lateinit var findPwEmail: EditText
    private lateinit var pwCheckBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.find_pw)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        findPwName = findViewById(R.id.findPwName)
        findPwPhoneNumber = findViewById(R.id.findPwPhoneNumber)
        findPwEmail = findViewById(R.id.findPwEmail)
        pwCheckBtn = findViewById(R.id.pwCheckBtn)

        pwCheckBtn.setOnClickListener {
            val name = findPwName.text.toString()
            val phoneNumber = findPwPhoneNumber.text.toString()
            val email = findPwEmail.text.toString()

            if (name.isNotEmpty() && phoneNumber.isNotEmpty() && email.isNotEmpty()) {
                findUserAndSendResetEmail(name, phoneNumber, email)
            } else {
                Toast.makeText(this, "모든 필드를 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }

        val backButton = findViewById<ImageButton>(R.id.back)
        backButton.setOnClickListener {
            finish()
        }

    }

    private fun findUserAndSendResetEmail(name: String, phoneNumber: String, email: String) {
        firestore.collection("users")
            .whereEqualTo("username", name)
            .whereEqualTo("phone", phoneNumber)
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "해당 사용자를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    sendPasswordResetEmail(email)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "오류 발생: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val intent = Intent(this, FindPw2Activity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "이메일 전송 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }


    // 뒤로가기 버튼 클릭 시 호출될 메서드
    fun onBackButtonClick(view: View) {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }


    // 아이디 찾기 버튼 클릭 시 호출될 메서드
    fun onFindIdButtonClick(view: View) {
        val intent = Intent(this, FindIdActivity::class.java)
        startActivity(intent)
    }

    // 회원가입 버튼 클릭 시 호출될 메서드
    fun onRegisterButtonClick(view: View) {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }
}