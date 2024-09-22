package com.explorit.dodamdodam

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.explorit.dodamdodam.databinding.FamilyCardEditBinding

class FamilyCardEditActivity : AppCompatActivity() {

    private lateinit var binding: FamilyCardEditBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // View Binding을 사용하여 레이아웃 설정
        binding = FamilyCardEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 필요한 초기화 작업 수행
    }
}