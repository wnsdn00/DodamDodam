package com.explorit.dodamdodam

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class FamilyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_family)
        // 로그인 했을 시 가족 그룹이 없다면 나오는 화면

        val btn_CreateFamily = findViewById<Button>(R.id.btnCreateFamily)
        val btn_JoinFamily = findViewById<Button>(R.id.btnJoinFamily)

        // 가족 생성 버튼
        btn_CreateFamily.setOnClickListener {
            val intent = Intent(this, CreateFamilyActivity::class.java)
            startActivity(intent)
        }

        // 가족 참여 버튼
        btn_JoinFamily.setOnClickListener {
            val intent = Intent(this, FamilyCodeActivity::class.java)
            startActivity(intent)
        }
    }
}