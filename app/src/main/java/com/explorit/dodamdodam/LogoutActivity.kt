package com.explorit.dodamdodam

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.explorit.dodamdodam.databinding.ActivityLogoutBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

@Suppress("DEPRECATION")
class LogoutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLogoutBinding
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestServerAuthCode(getString(R.string.web_client_id))
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()

        // GoogleSignInClient를 초기화합니다.
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.buttonLogout.setOnClickListener {
            // 로그아웃 처리를 수행
            performLogout()
            googleSignOut()
        }
    }

    private fun performLogout() {
        // 예: 세션 종료, 저장된 사용자 데이터 삭제 등
        FirebaseAuth.getInstance().signOut()
        // 로그인 화면으로 이동
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // 현재 액티비티 종료
    }

    private fun googleSignOut() {
        googleSignInClient.signOut()
            .addOnCompleteListener(this) {
                // 로그아웃 성공
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish() // 현재 액티비티 종료
            }

        googleSignInClient.revokeAccess()
    }
}