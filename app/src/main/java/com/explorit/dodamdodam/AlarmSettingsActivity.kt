package com.explorit.dodamdodam

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import com.explorit.dodamdodam.databinding.ActivityAlarmSettingsBinding

class AlarmSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAlarmSettingsBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        // 초기 스위치 상태 설정
        val notificationsEnabled = sharedPreferences.getBoolean("notificationsEnabled", true)
        binding.buttonAlarm.isChecked = notificationsEnabled

        // 스위치 변경 이벤트 리스너
        binding.buttonAlarm.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            // 알림 설정을 저장
            sharedPreferences.edit().putBoolean("notificationsEnabled", isChecked).apply()

            // 알림 설정을 적용하는 로직 추가
            if (isChecked) {
                // 알림 활성화 로직
            } else {
                // 알림 비활성화 로직
            }
        }
    }
}