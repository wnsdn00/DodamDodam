package com.explorit.dodamdodam

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class FindPw2Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.find_pw2)

        val backButton = findViewById<ImageButton>(R.id.back)
        backButton.setOnClickListener {
            onBackPressed() // 뒤로가기 버튼과 동일한 동작 수행
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // 뒤로가기 버튼이 눌렸을 때 LoginActivity로 이동
        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish()
    }

    // 이 메서드는 회원가입 버튼 클릭 시 LoginActivity로 이동
    fun onRegisterButtonClick(view: View) {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }
}