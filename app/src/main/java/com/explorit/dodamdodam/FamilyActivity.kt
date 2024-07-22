package com.explorit.dodamdodam

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class FamilyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_family)

        val btn_CreateFamily = findViewById<Button>(R.id.btnCreateFamily)
        val btn_JoinFamily = findViewById<Button>(R.id.btnJoinFamily)

        btn_CreateFamily.setOnClickListener {
            val intent = Intent(this, CreateFamilyActivity::class.java)
            startActivity(intent)
        }

        btn_JoinFamily.setOnClickListener {
            val intent = Intent(this, FamilyCodeActivity::class.java)
            startActivity(intent)
        }
    }
}