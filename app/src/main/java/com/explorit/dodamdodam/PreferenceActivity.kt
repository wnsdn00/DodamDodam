package com.explorit.dodamdodam

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.explorit.dodamdodam.databinding.ActivityPreferenceBinding

class PreferenceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPreferenceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreferenceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonAlarm.setOnClickListener {
            // 알림설정 버튼 클릭 시 AlarmSettingsActivity로 이동
            val intent = Intent(this, AlarmSettingsActivity::class.java)
            startActivity(intent)
        }

        binding.buttonLogout.setOnClickListener {
            // 로그아웃 버튼 클릭 시 LogoutActivity로 이동
            val intent = Intent(this, LogoutActivity::class.java)
            startActivity(intent)
        }

        binding.buttonUnregister.setOnClickListener {
            // 회원탈퇴 버튼 클릭 시 CancelAccountActivity로 이동
            val intent = Intent(this, CancelAccountActivity::class.java)
            startActivity(intent)
        }
    }
}