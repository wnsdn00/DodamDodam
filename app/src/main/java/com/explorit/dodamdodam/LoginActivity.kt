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
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var kakaoAuthViewModel: KakaoAuthViewModel
    private lateinit var googleSignInClient: GoogleSignInClient

    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // ViewModel 초기화
        kakaoAuthViewModel = ViewModelProvider(this).get(KakaoAuthViewModel::class.java)

        // Firebase 인증 초기화
        auth = FirebaseAuth.getInstance()

        // Firebase 데이터베이스 참조 초기화
        database = FirebaseDatabase.getInstance().reference

        // Id 찾기 버튼
        val findIdButton = findViewById<Button>(R.id.findID)
        findIdButton.setOnClickListener {
            onFindIdButtonClick(it)
        }

        // 비밀번호 찾기 버튼
        val findPwButton = findViewById<Button>(R.id.findPW)
        findPwButton.setOnClickListener {
            onFindPwButtonClick(it)
        }

        val editTextEmail = findViewById<EditText>(R.id.editTextId)
        val editTextPassword = findViewById<EditText>(R.id.editTextPassword)
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)

        buttonLogin.setOnClickListener {
            val email = editTextEmail.text.toString()
            val password = editTextPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 입력하시오", Toast.LENGTH_SHORT).show()
            } else {
                // Firebase 인증을 사용한 로그인
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // 로그인 성공
                            Log.d("LoginActivity", "signInWithEmail:success")
                            val user = auth.currentUser
                            // 데이터베이스에 사용자 정보 저장
                            user?.let {
                                val userId = it.uid
                                database.child("users").child(userId).child("email").setValue(email)
                                checkUserFamilyCode(userId)
                            }
                        } else {
                            // 로그인 실패
                            Log.w("LoginActivity", "signInWithEmail:failure", task.exception)
                            Toast.makeText(baseContext, "로그인 실패",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        // Google Sign In 옵션 설정
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Google 로그인 버튼 추가
        val buttonGoogle = findViewById<ImageButton>(R.id.buttonGoogle)
        buttonGoogle.setOnClickListener {
            signInWithGoogle()
        }

        // 카카오 로그인 버튼 추가
        val buttonKaKao = findViewById<ImageButton>(R.id.buttonKaKao)
        buttonKaKao.setOnClickListener {
            kakaoAuthViewModel.handleKakaoLogin()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Log.w("LoginActivity", "Google sign in failed", e)
                Toast.makeText(this, "Google 로그인 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        Log.d("LoginActivity", "firebaseAuthWithGoogle:" + account?.id)

        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("LoginActivity", "signInWithCredential:success")
                    val user = auth.currentUser
                    user?.let {
                        val userId = it.uid
                        database.child("users").child(userId).child("email").setValue(it.email)
                        checkUserFamilyCode(userId)
                    }
                } else {
                    Log.w("LoginActivity", "signInWithCredential:failure", task.exception)
                    Toast.makeText(baseContext, "Google 로그인 실패", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkUserFamilyCode(userId: String) {
        //Firebase에서 familyCode가 있는지 확인
        database.child("users").child(userId).child("familyCode").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists() && snapshot.getValue(String::class.java) != null) {
                    onLoginButtonClickToMainPage()
                } else {
                    onLoginButtonClickToFamily()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("LoginActivity", "checkUserFamilyCode:onCancelled", error.toException())
                Toast.makeText(this@LoginActivity, "데이터베이스 오류", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // 로그인 버튼 클릭 시 가족 그룹이 있을때 호출될 메서드
    fun onLoginButtonClickToMainPage() {
        val intent = Intent(this, MainPageActivity::class.java)
        startActivity(intent)
        finish()
    }

    // 로그인 버튼 클릭 시 가족 그룹이 없을때 호출될 메서드
    fun onLoginButtonClickToFamily() {
        val intent = Intent(this, FamilyActivity::class.java)
        startActivity(intent)
        finish()
    }

    // 아이디 찾기 버튼 클릭 시 호출될 메서드
    fun onFindIdButtonClick(view: View) {
        val intent = Intent(this, FindIdActivity::class.java)
        startActivity(intent)
    }

    // 비밀번호 찾기 버튼 클릭 시 호출될 메서드
    fun onFindPwButtonClick(view: View) {
        val intent = Intent(this, FindPwActivity::class.java)
        startActivity(intent)
    }

    // 회원가입 버튼 클릭 시 호출될 메서드
    fun onRegisterButtonClick(view: View) {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }
}