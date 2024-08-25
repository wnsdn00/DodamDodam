package com.explorit.dodamdodam

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
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
import com.kakao.sdk.user.UserApiClient

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var firestore: FirebaseFirestore
    private lateinit var kakaoAuthViewModel: KakaoAuthViewModel
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

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
        firestore = FirebaseFirestore.getInstance()

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
            val username = editTextEmail.text.toString() // 아이디 입력 필드
            val password = editTextPassword.text.toString() // 비밀번호 입력 필드

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "아이디와 비밀번호를 입력하시오", Toast.LENGTH_SHORT).show()
            } else {
                loginWithUsername(username, password)
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

        // ActivityResultLauncher 설정
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    firebaseAuthWithGoogle(account)
                } catch (e: ApiException) {
                    Log.w("LoginActivity", "Google sign in failed", e)
                    Toast.makeText(this, "Google 로그인 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 카카오 로그인 버튼 추가
        val buttonKaKao = findViewById<ImageButton>(R.id.buttonKaKao)
        buttonKaKao.setOnClickListener {
            signInWithKakao()
        }
    }
    
    private fun signInWithKakao() {
        UserApiClient.instance.loginWithKakaoAccount(this) { token, error ->
            if (error != null) {
                Log.e("LoginActivity", "카카오 로그인 실패", error)
                Toast.makeText(this, "카카오 로그인 실패", Toast.LENGTH_SHORT).show()
            } else if (token != null) {
                Log.i("LoginActivity", "카카오 로그인 성공: ${token.accessToken}")
                fetchFirebaseCustomToken(token.accessToken)
            }
        }
    }

    private fun fetchFirebaseCustomToken(kakaoAccessToken: String) {
        // 예시: 서버에 요청하여 Firebase Custom Token을 받는 과정
        val url = "YOUR_SERVER_URL_TO_GET_FIREBASE_CUSTOM_TOKEN"
        val request = object : StringRequest(Method.POST, url,
            Response.Listener { response ->
                val firebaseCustomToken = response // 서버로부터 받은 Firebase Custom Token
                firebaseAuthWithCustomToken(firebaseCustomToken)
            },
            Response.ErrorListener { error ->
                Log.e("LoginActivity", "서버로부터 Firebase Custom Token을 받는 데 실패했습니다.", error)
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["kakaoAccessToken"] = kakaoAccessToken
                return params
            }
        }

        // RequestQueue에 추가
        Volley.newRequestQueue(this).add(request)
    }

    private fun firebaseAuthWithCustomToken(customToken: String) {
        auth.signInWithCustomToken(customToken)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("LoginActivity", "Firebase Custom Token으로 로그인 성공")
                    val user = auth.currentUser
                    user?.let {
                        val username = it.uid
                        database.child("users").child(username).child("email").setValue(it.email)
                        checkUserFamilyCode(username)
                    }
                } else {
                    Log.w("LoginActivity", "Firebase Custom Token으로 로그인 실패", task.exception)
                    Toast.makeText(this, "Firebase 로그인 실패", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
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
                        val username = it.uid
                        database.child("users").child(username).child("email").setValue(it.email)
                        checkUserFamilyCode(username)
                    }
                } else {
                    Log.w("LoginActivity", "signInWithCredential:failure", task.exception)
                    Toast.makeText(baseContext, "Google 로그인 실패", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loginWithUsername(username: String, password: String) {
        firestore.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // 이메일을 가져와서 로그인 시도
                    var email = documents.first().getString("email") ?: ""
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                // 로그인 성공
                                Log.d("LoginActivity", "signInWithEmail:success")
                                val user = auth.currentUser
                                user?.let {
                                    val username = it.uid
                                    database.child("users").child(username).child("email").setValue(email)
                                    checkUserFamilyCode(username)
                                }
                            } else {
                                // 로그인 실패
                                Log.w("LoginActivity", "signInWithEmail:failure", task.exception)
                                Toast.makeText(baseContext, "아이디 또는 비밀번호가 잘못되었습니다.", Toast.LENGTH_SHORT).show()
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
        database.child("users").child(username).child("familyCode").addListenerForSingleValueEvent(object : ValueEventListener {
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