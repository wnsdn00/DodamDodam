package com.explorit.dodamdodam

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class FindPwActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.find_pw)

        val backButton = findViewById<ImageButton>(R.id.back)
        backButton.setOnClickListener {
            finish()
        }

        val registerPwCheckButton = findViewById<Button>(R.id.registerPwCheck)
        registerPwCheckButton.setOnClickListener {
            onPwCheckButtonClick(it)
        }
    }

    // 뒤로가기 버튼 클릭 시 호출될 메서드
    fun onBackButtonClick(view: View) {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    // 비밀번호 확인 버튼 클릭 시 호출될 메서드
    fun onPwCheckButtonClick(view: View) {
        val intent = Intent(this, FindPw2Activity::class.java)
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