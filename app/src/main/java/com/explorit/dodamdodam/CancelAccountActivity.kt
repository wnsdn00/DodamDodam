package com.explorit.dodamdodam

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
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

        val backButton = findViewById<ImageButton>(R.id.back)
        backButton.setOnClickListener {
            finish()
        }

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

                            // 로그아웃
                            auth.signOut()

                            // 로그인 화면으로 이동
                            val intent = Intent(this, LoginActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                            finish() // 현재 액티비티 종료
                        } else {
                            Log.e("CancelAccountActivity", "Failed to delete user authentication.", deleteTask.exception)
                            Toast.makeText(this, "회원탈퇴에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.e("CancelAccountActivity", "Failed to delete user data.", task.exception)
                    Toast.makeText(this, "회원 정보를 삭제하는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        } ?: run {
            Log.e("CancelAccountActivity", "No user is currently logged in.")
            Toast.makeText(this, "사용자가 로그인되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeFromFamilyGroup(userId: String, callback: (Boolean) -> Unit) {
        // families 경로에서 사용자를 찾아서 삭제
        database.reference.child("families").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val updates = mutableMapOf<String, Any?>()
                    for (family in snapshot.children) {
                        val members = family.child("members").value as? Map<String, Any>
                        if (members?.containsKey(userId) == true) {
                            updates["families/${family.key}/members/$userId"] = null
                        }
                    }
                    if (updates.isNotEmpty()) {
                        database.reference.updateChildren(updates).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("CancelAccountActivity", "User removed from family groups successfully.")
                                callback(true)
                            } else {
                                Log.e("CancelAccountActivity", "Failed to remove user from family groups.", task.exception)
                                callback(false)
                            }
                        }
                    } else {
                        Log.d("CancelAccountActivity", "User is not part of any family groups.")
                        callback(true)
                    }
                } else {
                    Log.d("CancelAccountActivity", "No family groups found.")
                    callback(false)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("CancelAccountActivity", "Failed to query family groups.", exception)
                callback(false)
            }
    }

    // 뒤로가기 버튼 클릭 시 호출될 메서드
    fun onBackButtonClick(view: View) {
        val intent = Intent(this, PreferenceActivity::class.java)
        startActivity(intent)
    }
}