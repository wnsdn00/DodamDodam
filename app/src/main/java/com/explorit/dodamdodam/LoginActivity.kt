package com.explorit.dodamdodam

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val editTextEmail = findViewById<EditText>(R.id.editTextId)
        val editTextPassword = findViewById<EditText>(R.id.editTextPassword)
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)

        buttonLogin.setOnClickListener {
            val email = editTextEmail.text.toString()
            val password = editTextPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            } else {
                // For now, just log the email and password
                Log.d("LoginActivity", "Email: $email")
                Log.d("LoginActivity", "Password: $password")
                // Call method to start MainPageActivity
                onLoginButtonClick(buttonLogin)
            }
        }
    }

    // 로그인 버튼 클릭 시 호출될 메서드
    fun onLoginButtonClick(view: View) {
        val intent = Intent(this, MainPageActivity::class.java)
        startActivity(intent)
    }
}