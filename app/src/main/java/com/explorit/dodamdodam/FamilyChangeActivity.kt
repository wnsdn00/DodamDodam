package com.explorit.dodamdodam

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class FamilyChangeActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_family_change)

        val buttonChange = findViewById<Button>(R.id.buttonFamilyChange)

        buttonChange.setOnClickListener {
            val intent = Intent(this, SelectFamilyActivity::class.java)
            startActivity(intent)
        }
        }

}
