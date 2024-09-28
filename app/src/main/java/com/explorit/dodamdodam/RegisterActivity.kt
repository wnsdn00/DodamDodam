package com.explorit.dodamdodam

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth // Firebase를 사용하는 권한
    private lateinit var firestore: FirebaseFirestore
    private var isEmailChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance() // Firebase에서 인스턴스를 가져올 것이다!
        firestore = FirebaseFirestore.getInstance()

        val registerButton: Button = findViewById(R.id.buttonRegister) // 회원가입 버튼 객체 생성
        val checkEmailButton: Button = findViewById(R.id.buttonCheckEmail)

        checkEmailButton.setOnClickListener {
            checkEmailDuplication()
        }

        registerButton.setOnClickListener { // 눌렀을 때 registerUser 함수를 쓸 것이다!
            if (!isEmailChecked) {
                Toast.makeText(this, "이메일 중복확인을 해주세요.", Toast.LENGTH_SHORT).show()
            } else {
                registerUser()
            }
        }

        val backButton = findViewById<ImageButton>(R.id.back)
        backButton.setOnClickListener {
            finish()
        }
    }


    private fun checkEmailDuplication() {
        val email = findViewById<EditText>(R.id.registerEmail).text.toString()
        if (email.isEmpty()) {
            Toast.makeText(this, "이메일을 입력하세요.", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "사용 가능한 이메일입니다.", Toast.LENGTH_SHORT).show()
                    isEmailChecked = true
                } else {
                    Toast.makeText(this, "이미 사용 중인 이메일입니다.", Toast.LENGTH_SHORT).show()
                    isEmailChecked = false
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "이메일 확인 중 오류 발생: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun registerUser() { // xml에 있는 id의 이름을 가져와서 객체로 생성
        val useremail = findViewById<EditText>(R.id.registerEmail).text.toString()
        val password = findViewById<EditText>(R.id.registerPw).text.toString()
        val passwordconfirm = findViewById<EditText>(R.id.registerPwConfirm).text.toString()
        val username = findViewById<EditText>(R.id.registerUserName).text.toString()
        val userbirth = findViewById<EditText>(R.id.registerUserBirth).text.toString()

        if (password != passwordconfirm) {
            Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        auth.createUserWithEmailAndPassword(useremail, password) // Firebase 권한으로 email, password를 만든다.
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Firestore에 사용자 세부 정보 저장
                    saveUserData(useremail, username, userbirth)
                    // 회원가입 성공 메시지 표시
                    Toast.makeText(this, "회원가입 성공", Toast.LENGTH_SHORT).show() // Toast는 아래에 메시지를 띄워줍니다.
                    // 로그인 액티비티로 이동
                    navigateToLoginActivity()
                } else {
                    // 회원가입 실패 시 사용자에게 메시지 표시
                    Toast.makeText(this, "회원가입 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserData(useremail: String, username: String, userbirth: String) { // Firebase에 저장
        val user = hashMapOf( // 해시맵으로 username, email 필드에 저장
            "email" to useremail,
            "username" to username,
            "userbirth" to userbirth
        )

        // 생성된 ID로 새 문서 추가
        firestore.collection("users") // 여기서 컬렉션 이름과 같아야 합니다
            .add(user)
            .addOnSuccessListener { documentReference ->
                Log.d("RegisterActivity", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.e("RegisterActivity", "문서 추가 오류", e)
            }
    }

    private fun navigateToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java) // 로그인 화면으로 돌아가게 하는 intent 이용!
        startActivity(intent)
        finish()  // 현재 액티비티를 종료하여 뒤로가기 버튼으로 다시 돌아오지 않도록 한다.
    }

    private fun proceedToMainPage(userName: String, userBirthday: String) {
        val intent = Intent(this, MainPageActivity::class.java).apply {
            putExtra("user_name", userName)
            putExtra("user_birthday", userBirthday)
        }
        startActivity(intent)
        finish()
    }
}