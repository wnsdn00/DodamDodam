package com.explorit.dodamdodam

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class FindIdActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var verificationCodeEditText: EditText
    private lateinit var codeInputEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.find_id)

        // FirebaseAuth 인스턴스 초기화
        auth = Firebase.auth

        // 이메일 입력 필드
        emailEditText = findViewById(R.id.registerEmail)

        // 인증 코드 받기 필드
        verificationCodeEditText = findViewById(R.id.GetCode)

        // 받은 인증 코드 입력 필드
        codeInputEditText = findViewById(R.id.GotCode)

        // 뒤로가기 버튼 설정
        val backButton = findViewById<ImageButton>(R.id.back)
        backButton.setOnClickListener {
            finish()
        }

        // 아이디 확인 버튼 설정
        val registerIdCheckButton = findViewById<Button>(R.id.registerIdCheck)
        registerIdCheckButton.setOnClickListener {
            onIdCheckButtonClick(it)
        }

        // 인증 코드 보내기 버튼 설정
        verificationCodeEditText.setOnClickListener {
            sendVerificationCode()
        }
    }

    // 아이디 확인 버튼 클릭 시 호출될 메서드
    fun onIdCheckButtonClick(view: View) {
        val codeEntered = codeInputEditText.text.toString().trim()

        if (codeEntered.isEmpty()) {
            Toast.makeText(this, "인증코드를 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        // 인증코드 검증 로직
        val intent = Intent(this, FindId2Activity::class.java)
        startActivity(intent)
    }

    // 인증 코드 보내기 버튼 클릭 시 호출될 메서드
    private fun sendVerificationCode() {
        val email = emailEditText.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "이메일을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        auth.fetchSignInMethodsForEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val signInMethods = task.result.signInMethods
                if (signInMethods != null && signInMethods.isNotEmpty()) {
                    // 이메일이 이미 존재하는 경우, 인증 이메일을 발송
                    sendPasswordResetEmail(email)
                } else {
                    // 이메일이 존재하지 않는 경우
                    Toast.makeText(this, "등록되지 않은 이메일입니다", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e("FindIdActivity", "Error fetching sign-in methods", task.exception)
                Toast.makeText(this, "오류가 발생했습니다. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 비밀번호 재설정 이메일을 보내는 메서드
    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "이메일로 인증 코드를 발송했습니다", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("FindIdActivity", "Error sending password reset email", task.exception)
                    Toast.makeText(this, "인증 이메일 발송에 실패했습니다. ${task.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // 비밀번호 찾기 버튼 클릭 시 호출될 메서드
    fun onFindPwButtonClick(view: View) {
        val intent = Intent(this, FindPwActivity::class.java)
        startActivity(intent)
    }

    // 회원가입 버튼 클릭 시 호출될 메서드
    fun onRegisterButtonClick(view: View) {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }
}