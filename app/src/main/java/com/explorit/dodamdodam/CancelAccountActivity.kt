package com.explorit.dodamdodam

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class CancelAccountActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cancel_account)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val btnDeleteAccount: Button = findViewById(R.id.btnDeleteAccount)
        btnDeleteAccount.setOnClickListener {
            showDeleteAccountDialog()
        }
    }

    private fun showDeleteAccountDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("회원탈퇴")
        builder.setMessage("정말로 회원탈퇴를 하시겠습니까?")
        builder.setPositiveButton("예") { dialog, which ->
            deleteAccount()
        }
        builder.setNegativeButton("아니오") { dialog, which ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun deleteAccount() {
        val user = auth.currentUser
        user?.let {
            val userId = user.uid
            // 데이터베이스에서 사용자 데이터 삭제
            database.reference.child("users").child(userId).removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 사용자 인증 삭제
                    user.delete().addOnCompleteListener { deleteTask ->
                        if (deleteTask.isSuccessful) {
                            Toast.makeText(this, "회원탈퇴가 완료되었습니다.", Toast.LENGTH_SHORT).show()
                            // 로그아웃 및 로그인 화면으로 이동
                            auth.signOut()
                            finish() // 현재 액티비티 종료
                        } else {
                            Toast.makeText(this, "회원탈퇴에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "회원 정보를 삭제하는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        } ?: run {
            Toast.makeText(this, "사용자가 로그인되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}