package com.explorit.dodamdodam

import android.os.Bundle
import android.view.View
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

class AlarmSettingsActivity : AppCompatActivity() {

    private lateinit var alarm: Switch
    private lateinit var missionAlarm: Switch
    private lateinit var questionAlarm: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_settings)

        alarm = findViewById(R.id.buttonAlarm)
        missionAlarm = findViewById(R.id.missionAlarm)
        questionAlarm = findViewById(R.id.questionAlarm)

        alarm.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                missionAlarm.visibility = View.VISIBLE
                questionAlarm.visibility = View.VISIBLE
            } else {
                missionAlarm.visibility = View.GONE
                questionAlarm.visibility = View.GONE
            }
        }

        missionAlarm.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                enableMissionAlarm()
            } else {
                disableMissionAlarm()
            }
        }

        questionAlarm.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                enableQuestionAlarm()
            } else {
                disableQuestionAlarm()
            }
        }

        if (!alarm.isChecked) {
            missionAlarm.visibility = View.GONE
            questionAlarm.visibility = View.GONE
        }
    }

    private fun enableAllAlarms() {
        // 전체 알림 설정 로직
    }

    private fun disableAllAlarms() {
        // 전체 알림 해제 로직
    }

    private fun enableMissionAlarm() {
        // 미션 알림 설정 로직
    }

    private fun disableMissionAlarm() {
        // 미션 알림 해제 로직
    }

    private fun enableQuestionAlarm() {
        // 오늘의 질문 알림 설정 로직
    }

    private fun disableQuestionAlarm() {
        // 오늘의 질문 알림 해제 로직
    }
}