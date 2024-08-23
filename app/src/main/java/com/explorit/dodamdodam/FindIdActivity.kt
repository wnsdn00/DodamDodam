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
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.random.Random

class FindIdActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var codeInputEditText: EditText
    private lateinit var verificationCodeButton: Button

    private val firestore = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.find_id)

        // FirebaseAuth 인스턴스 초기화
        auth = Firebase.auth

        // 이메일 입력 필드
        emailEditText = findViewById(R.id.registerEmail)

        // 인증 코드 받기 필드
        verificationCodeButton = findViewById(R.id.GetCode)

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
        verificationCodeButton.setOnClickListener {
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

        // Firestore에서 인증 코드 확인
        val email = emailEditText.text.toString().trim()
        firestore.collection("verificationCodes")
            .document(email)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val savedCode = document.getString("code")
                    if (savedCode == codeEntered) {
                        // 인증 코드가 맞으면 Firestore에서 해당 이메일로 등록된 아이디 가져오기
                        firestore.collection("users")
                            .whereEqualTo("email", email)
                            .get()
                            .addOnSuccessListener { documents ->
                                if (!documents.isEmpty) {
                                    val userId = documents.documents[0].getString("userid")
                                    // 아이디를 FindId2Activity로 전달
                                    val intent = Intent(this, FindId2Activity::class.java).apply {
                                        putExtra("USER_ID", userId)
                                    }
                                    startActivity(intent)
                                } else {
                                    Toast.makeText(this, "등록된 아이디를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.e("FindIdActivity", "Error getting user ID", exception)
                                Toast.makeText(this, "오류가 발생했습니다. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "잘못된 인증 코드입니다", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "인증 코드가 존재하지 않습니다", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FindIdActivity", "Error checking verification code", exception)
                Toast.makeText(this, "오류가 발생했습니다. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }
    }

    // 인증 코드 보내기 버튼 클릭 시 호출될 메서드
    private fun sendVerificationCode() {
        val email = emailEditText.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "이메일을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        // Firestore에서 사용자 데이터 확인
        firestore.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // 이메일이 존재하는 경우, 인증 코드를 생성하고 저장
                    val verificationCode = generateVerificationCode()
                    saveVerificationCode(email, verificationCode)
                    // 이메일로 인증 코드 발송
                    sendVerificationCodeEmail(email, verificationCode)
                } else {
                    // 이메일이 존재하지 않는 경우
                    Toast.makeText(this, "등록되지 않은 이메일입니다", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FindIdActivity", "Error getting documents: ", exception)
                Toast.makeText(this, "오류가 발생했습니다. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }
    }

    // 인증 코드를 생성하는 메서드
    private fun generateVerificationCode(): String {
        val random = Random.nextInt(100000, 999999)
        return random.toString()
    }

    // 인증 코드를 Firestore에 저장하는 메서드
    private fun saveVerificationCode(email: String, code: String) {
        val data = hashMapOf(
            "code" to code
        )
        firestore.collection("verificationCodes")
            .document(email)
            .set(data)
            .addOnSuccessListener {
                Log.d("FindIdActivity", "Verification code successfully written!")
            }
            .addOnFailureListener { e ->
                Log.w("FindIdActivity", "Error writing verification code", e)
            }
    }

    // 이메일로 인증 코드를 보내는 메서드
    private fun sendVerificationCodeEmail(email: String, code: String) {
        val auth = FirebaseAuth.getInstance()

        // 사용자에게 이메일 확인 링크를 보내는 메서드
        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://dodamdodam.page.link/explorit")
            .setHandleCodeInApp(true) // 앱 내에서 코드 처리
            .setAndroidPackageName(
                "com.explorit.dodamdodam",
                true,  /* installIfNotAvailable */
                "12"   /* minimumVersion */
            )
            .build()

        auth.sendSignInLinkToEmail(email, actionCodeSettings)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 이메일 링크가 성공적으로 전송되었음을 사용자에게 알림
                    Log.d("FindIdActivity", "인증 코드가 이메일로 전송되었습니다.")
                    Toast.makeText(this, "인증 코드가 이메일로 전송되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("FindIdActivity", "Error sending email", task.exception)
                    Toast.makeText(this, "이메일 전송에 실패했습니다.", Toast.LENGTH_SHORT).show()
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