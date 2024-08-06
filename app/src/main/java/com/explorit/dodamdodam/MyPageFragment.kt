package com.explorit.dodamdodam

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import android.text.method.LinkMovementMethod
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.explorit.dodamdodam.databinding.FragmentMyPageBinding

class MyPageFragment : Fragment(R.layout.fragment_my_page) {
    private var _binding: FragmentMyPageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMyPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnStore.setOnClickListener {
            // 상점 버튼 클릭 이벤트 처리
            Toast.makeText(activity, "상점 버튼 클릭됨", Toast.LENGTH_SHORT).show()
            // 예: 상점 액티비티로 이동
            // startActivity(Intent(activity, StoreActivity::class.java))
        }

        binding.btnCustomize.setOnClickListener {
            // 꾸미기 버튼 클릭 이벤트 처리
            Toast.makeText(activity, "꾸미기 버튼 클릭됨", Toast.LENGTH_SHORT).show()
            // 예: 꾸미기 액티비티로 이동
            // startActivity(Intent(activity, CustomizeActivity::class.java))
        }

        binding.btnCustomerService.setOnClickListener {
            // 고객센터 버튼 클릭 이벤트 처리
            Toast.makeText(activity, "고객센터 버튼 클릭됨", Toast.LENGTH_SHORT).show()
            // 예: 고객센터 액티비티로 이동
            // startActivity(Intent(activity, CustomerServiceActivity::class.java))
        }

        binding.btnAppInfo.setOnClickListener {
            // 앱 정보 버튼 클릭 이벤트 처리
            Toast.makeText(activity, "앱 정보 버튼 클릭됨", Toast.LENGTH_SHORT).show()
            // 예: 앱 정보 액티비티로 이동
            // startActivity(Intent(activity, AppInfoActivity::class.java))
        }

        binding.buttonPreference.setOnClickListener {
            val intent = Intent(activity, PreferenceActivity::class.java)
            startActivity(intent)
        }

        // SpannableString 설정
        val fullText = "준우네 가족 >"
        val spannableString = SpannableString(fullText)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(p0: View) {
                Toast.makeText(activity, "수정하기", Toast.LENGTH_SHORT).show()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = Color.BLUE
                ds.isUnderlineText = false
            }
        }

        // '>' 문자를 클릭 가능한 부분으로 설정
        val end = fullText.length
        spannableString.setSpan(clickableSpan, fullText.indexOf(">"), end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        val familyNameTextView: TextView = binding.familyCardModify
        familyNameTextView.text = spannableString
        familyNameTextView.movementMethod = LinkMovementMethod.getInstance() // 클릭 가능하게 만들기
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}