package com.explorit.dodamdodam

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class finishCreateFamilyActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finish_create_family)


        val btn_GoMain = findViewById<Button>(R.id.btnGoMain)

        btn_GoMain.setOnClickListener {
            val intent = Intent(this, MainPageActivity::class.java)
            startActivity(intent)
        }

    }
}