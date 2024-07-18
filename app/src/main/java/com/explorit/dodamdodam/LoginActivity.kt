package com.explorit.dodamdodam

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Firebase 인증 초기화
        auth = FirebaseAuth.getInstance()

        // Firebase 데이터베이스 참조 초기화
        database = FirebaseDatabase.getInstance().reference

        val editTextEmail = findViewById<EditText>(R.id.editTextId)
        val editTextPassword = findViewById<EditText>(R.id.editTextPassword)
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)

        buttonLogin.setOnClickListener {
            val email = editTextEmail.text.toString()
            val password = editTextPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            } else {
                // Firebase 인증을 사용한 로그인
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // 로그인 성공
                            Log.d("LoginActivity", "signInWithEmail:success")
                            val user = auth.currentUser
                            // 데이터베이스에 사용자 정보 저장
                            user?.let {
                                val userId = it.uid
                                database.child("users").child(userId).child("email").setValue(email)
                            }
                            onLoginButtonClick(buttonLogin)
                        } else {
                            // 로그인 실패
                            Log.w("LoginActivity", "signInWithEmail:failure", task.exception)
                            Toast.makeText(baseContext, "Authentication failed.",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

    // 로그인 버튼 클릭 시 호출될 메서드
    fun onLoginButtonClick(view: View) {
        val intent = Intent(this, MainPageActivity::class.java)
        startActivity(intent)
    }

    // 회원가입 버튼 클릭 시 호출될 메서드
    fun onRegisterButtonClick(view: View) {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }
}