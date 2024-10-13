package com.explorit.dodamdodam

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore

@Suppress("DEPRECATION")
class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var firestore: FirebaseFirestore

    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var googleSignInButton: ImageButton

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        Log.d("LOGIN--", task.toString())
        try {
            // Google 로그인이 성공하면, Firebase로 인증합니다.
            val account = task.getResult(ApiException::class.java)!!
            Log.d("LOGIN--22", account.idToken!!)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            // Google 로그인 실패
            Log.e("GoogleSignIn", "로그인 실패, 코드: ${e.statusCode}")
            Toast.makeText(this, "Google 로그인에 실패했습니다.", Toast.LENGTH_SHORT).show()
        }

    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Firebase 인증 초기화
        auth = FirebaseAuth.getInstance()

        // Firebase 데이터베이스 참조 초기화
        database = FirebaseDatabase.getInstance().reference
        firestore = FirebaseFirestore.getInstance()

        googleSignInButton = findViewById(R.id.buttonGoogle)


        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestServerAuthCode(getString(R.string.web_client_id))
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()

        // GoogleSignInClient를 초기화합니다.
        googleSignInClient = GoogleSignIn.getClient(this, gso)


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

        val editTextEmail = findViewById<EditText>(R.id.editTextEmail)
        val editTextPassword = findViewById<EditText>(R.id.editTextPassword)
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)

        buttonLogin.setOnClickListener {
            val userEmail = editTextEmail.text.toString() // 아이디 입력 필드
            val password = editTextPassword.text.toString() // 비밀번호 입력 필드

            if (userEmail.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "아이디와 비밀번호를 입력하시오", Toast.LENGTH_SHORT).show()
            } else {
                loginWithUsername(userEmail, password)
            }
        }

        googleSignInButton.setOnClickListener{
            googleSignIn()
        }

        // 로그인 되어있는 사용자를 확인
        val currentUser = auth.currentUser
        val userId = auth.currentUser?.uid
        if (currentUser != null && userId != null) {
            // 자동 로그인
            checkIfNewUser(userId)
            return
        }
    }





    private fun googleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        Log.d("LOGIN--3",idToken)
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // 로그인 성공
                    val userUid = auth.currentUser?.uid
                    if (userUid != null) {
                        checkIfNewUser(userUid)
                    }
                } else {
                    // 로그인 실패
                    Toast.makeText(this, "Firebase 인증에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkIfNewUser(userId: String) {
        database.child("users").child(userId)
        .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // 이미 데이터베이스에 사용자가 있음
                    checkUserFamilyCode(userId)
                } else {
                    // 새로운 사용자이므로 추가 정보 입력 페이지로 이동
                    goToAdditionalInfoPage()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@LoginActivity, "오류가 발생했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun goToAdditionalInfoPage() {
        val intent = Intent(this, AdditionalInfoActivity::class.java)
        startActivity(intent)
        finish()
    }


        private fun loginWithUsername(userEmail: String, password: String) {
            firestore.collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        // 이메일을 가져와서 로그인 시도
                        var email = documents.first().getString("email") ?: ""
                        var userName = documents.first().getString("username") ?: ""
                        var userBirth = documents.first().getString("userbirth") ?: ""
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    // 로그인 성공
                                    Log.d("LoginActivity", "signInWithEmail:success")
                                    val user = auth.currentUser
                                    user?.let {
                                        val userId = it.uid
                                        database.child("users").child(userId).child("email")
                                            .setValue(email)
                                        database.child("users").child(userId).child("userName")
                                            .setValue(userName)
                                        database.child("users").child(userId).child("userBirth")
                                            .setValue(userBirth)
                                        checkUserFamilyCode(userId)
                                    }
                                } else {
                                    // 로그인 실패
                                    Log.w(
                                        "LoginActivity",
                                        "signInWithEmail:failure",
                                        task.exception
                                    )
                                    Toast.makeText(
                                        baseContext,
                                        "아이디 또는 비밀번호가 잘못되었습니다.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    } else {
                        // 아이디가 존재하지 않음
                        Toast.makeText(this, "존재하지 않는 아이디입니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("LoginActivity", "Error getting user email", e)
                    Toast.makeText(this, "오류가 발생했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }
        }

        private fun checkUserFamilyCode(username: String) {
            //Firebase에서 familyCode가 있는지 확인
            database.child("users").child(username).child("familyCode")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists() && snapshot.getValue(String::class.java) != null) {
                            onLoginButtonClickToMainPage()
                        } else {
                            onLoginButtonClickToFamily()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w(
                            "LoginActivity",
                            "checkUserFamilyCode:onCancelled",
                            error.toException()
                        )
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
            Log.d("LoginActivity", "Find ID button clicked")
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