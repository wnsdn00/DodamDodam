package com.explorit.dodamdodam

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlin.random.Random

class FindIdActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var findIdName: EditText
    private lateinit var findIdPhoneNumber: EditText
    private lateinit var idCheckBtn: Button

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.find_id)

        // FirebaseAuth 인스턴스 초기화
        auth = Firebase.auth
        firestore =FirebaseFirestore.getInstance()

        findIdName= findViewById(R.id.findIdName)
        findIdPhoneNumber= findViewById(R.id.findIdPhoneNumber)
        idCheckBtn = findViewById<Button>(R.id.idCheckBtn)

        idCheckBtn.setOnClickListener {
            val name = findIdName.text.toString()
            val phoneNumber = findIdPhoneNumber.text.toString()

            if (name.isNotEmpty() && phoneNumber.isNotEmpty()) {
                findUserId(name, phoneNumber)
            } else {
                Toast.makeText(this, "모든 필드를 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }

        // 뒤로가기 버튼 설정
        val backButton = findViewById<ImageButton>(R.id.back)
        backButton.setOnClickListener {
            finish()
        }

    }

    private fun findUserId(name: String, phoneNumber: String) {
        firestore.collection("users")
            .whereEqualTo("username", name)
            .whereEqualTo("phone", phoneNumber)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "해당 사용자를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    for (document in documents) {
                        val email = document.getString("email")
                        val intent = Intent(this, FindId2Activity::class.java)
                        intent.putExtra("userEmail", email) // 이메일을 Intent에 추가
                        startActivity(intent) // 새로운 액티비티 시작
                        return@addOnSuccessListener // 이메일이 발견되면 더 이상 진행하지 않음
                    }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "오류 발생: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }


}