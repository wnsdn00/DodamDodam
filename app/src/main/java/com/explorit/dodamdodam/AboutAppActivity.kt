package com.explorit.dodamdodam

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.explorit.dodamdodam.databinding.AboutAppBinding

class AboutAppActivity : AppCompatActivity() {
    private lateinit var binding: AboutAppBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AboutAppBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 뒤로가기 버튼 클릭 시 현재 액티비티를 종료하여 이전 페이지로 이동
        binding.Back.setOnClickListener {
            finish()
        }
    }
}