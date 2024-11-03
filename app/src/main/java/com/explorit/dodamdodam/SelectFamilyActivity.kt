package com.explorit.dodamdodam

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class SelectFamilyActivity : AppCompatActivity() {
    private lateinit var familyRecyclerView: RecyclerView
    private lateinit var familyAdapter: FamilyCodeAdapter
    private val familyCodeNameList = mutableListOf<Triple<String, String, String>>() // 가족 코드와 이름 리스트
    private lateinit var familyAddBtn: ImageButton
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_family)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        familyAddBtn = findViewById(R.id.familyAdd_Btn)
        getUserFamilyCode()

        familyRecyclerView = findViewById(R.id.familyCodeRecyclerView)
        familyAdapter = FamilyCodeAdapter(familyCodeNameList,
            onJoinClick = { familyCode, familyName ->
                confirmJoinFamily(familyCode, familyName)
            },
            onDeleteClick = { familyCode, position ->
                showDeleteConfirmationDialog(familyCode, position)
            }
        )
        familyRecyclerView.adapter = familyAdapter
        familyRecyclerView.layoutManager = LinearLayoutManager(this)

        familyAddBtn.setOnClickListener {
            val intent = Intent(this, FamilyActivity::class.java)
            startActivity(intent)
        }
    }

    private fun confirmJoinFamily(familyCode: String, familyName: String) {
        AlertDialog.Builder(this)
            .setTitle("가족 참여")
            .setMessage("정말 이 가족에 참여하시겠습니까?")
            .setPositiveButton("확인") { _, _ ->
                saveSelectedFamilyCodeName(familyCode, familyName)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showDeleteConfirmationDialog(familyCode: String, position: Int) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("삭제 확인")
            .setMessage("정말 이 가족을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                // 다이얼로그에서 삭제을 눌렀을 때만 삭제 수행
                removeUserFromFamily(familyCode)
                familyAdapter.removeItem(position)
            }
            .setNegativeButton("취소", null)
            .create()
        dialog.show()
    }



    // 사용자가 참여하고 있는 가족코드들을 가져오는 함수
    private fun getUserFamilyCode() {
        val userId = auth.currentUser?.uid

        firestore.collection("users")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.first()
                    val familyCodes = document.get("familyCode") as? List<String>

                    if (familyCodes != null) {
                        fetchFamilyNames(familyCodes)
                    } else {
                        Log.d("FamilySelection", "가족 코드가 없습니다.")
                    }
                } else {
                    Log.d("FamilySelection", "해당 사용자 문서를 찾을 수 없습니다.")
                }
            }.addOnFailureListener { e ->
                Log.e("FamilySelection", "가족 코드 가져오기 실패", e)
            }
    }

    // 가져온 가족들의 코드에 맞는 이름을 가져와서 코드와 이름을 저장하고 가족 이름을 리스트에 나타내주는 함수
    private fun fetchFamilyNames(familyCodes: List<String>) {
        familyCodes.forEach { code ->
                    database.child("families").child(code).child("familyName")
                        .get()
                        .addOnSuccessListener { nameSnapshot ->
                            val familyName = nameSnapshot.value as? String ?: "알 수 없는 가족"
                            database.child("families").child(code).child("members")
                                .child(auth.currentUser?.uid ?: "").child("nickName")
                                .get()
                                .addOnSuccessListener { nicknameSnapshot ->
                                    val nickname = nicknameSnapshot.value as? String ?: "호칭 없음"

                                    familyCodeNameList.add(Triple(code, familyName, nickname)) // 리스트에 코드와 이름 추가

                                    if (familyCodeNameList.size == familyCodes.size) {
                                        familyAdapter.notifyDataSetChanged()
                                    }
                                }.addOnFailureListener { e ->
                                Log.e("FamilySelection", "호칭 가져오기 실패: $code", e)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("FamilySelection", "가족 이름 가져오기 실패: $code", e)
                        }
                }

    }

    // 선택한 가족에 접속하고 데이터베이스를 접속한 가족으로 업데이트 해주는 하ㅏㅁ수
    private fun saveSelectedFamilyCodeName(selectedFamilyCode: String, selectedFamilyName: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val updates = mapOf(
                "familyCode" to selectedFamilyCode,
                "familyName" to selectedFamilyName
            )
            database.child("users").child(userId).updateChildren(updates)
                .addOnSuccessListener {
                    val intent = Intent(this, MainPageActivity::class.java)
                    startActivity(intent)
                    finish()
                }.addOnFailureListener { e ->
                    Log.e("FamilySelection", "가족 코드 저장 실패", e)
                }
        }
    }

    private fun removeUserFromFamily(familyCode: String) {
        val userId = auth.currentUser?.uid ?: return

        // Realtime Database에서 가족 목록에서 사용자 제거
        database.child("families").child(familyCode).child("members").child(userId).removeValue()
            .addOnSuccessListener {
                firestore.collection("users")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            val document = documents.first()
                            firestore.collection("users").document(document.id)
                                .update("familyCode", FieldValue.arrayRemove(familyCode))
                                .addOnSuccessListener {
                                    Log.d("FamilySelection", "가족 코드 삭제 성공")
                                    // 업데이트된 리스트 반영
                                    getUserFamilyCode()
                                    familyAdapter.notifyDataSetChanged()
                                }
                                .addOnFailureListener { e ->
                                    Log.e("FamilySelection", "Firestore 가족 코드 삭제 실패", e)
                                }
                            }
                    }.addOnFailureListener { e ->
                        Log.e("FamilySelection", "user 문서 가져 오기 실패", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("FamilySelection", "Realtime Database 가족 목록에서 사용자 제거 실패", e)
            }
    }
}
