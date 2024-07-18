package com.explorit.dodamdodam

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class FindIdActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.find_id)

        val backButton = findViewById<ImageButton>(R.id.back)
        backButton.setOnClickListener {
            finish()
        }

        val registerIdCheckButton = findViewById<Button>(R.id.registerIdCheck)
        registerIdCheckButton.setOnClickListener {
            onIdCheckButtonClick(it)
        }
    }

    // 뒤로가기 버튼 클릭 시 호출될 메서드
    fun onRegisterButtonClick(view: View) {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    // 아이디 확인 버튼 클릭 시 호출될 메서드
    fun onIdCheckButtonClick(view: View) {
        val intent = Intent(this, FindId2Activity::class.java)
        startActivity(intent)
    }
}
