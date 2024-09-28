package com.explorit.dodamdodam

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.explorit.dodamdodam.databinding.ItemDiaryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.OkHttpClient

class DiaryDetailFragment : Fragment() {

    private var _binding: ItemDiaryBinding? = null
    private val binding get() = _binding!!

    private var user: FirebaseUser? = null
    private var firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var okHttpClient: OkHttpClient = OkHttpClient()

    // 게시물의 Firestore 문서 ID를 저장할 변수
    private var documentId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // View Binding 설정
        _binding = ItemDiaryBinding.inflate(inflater, container, false)

        // Firebase 인증 초기화
        user = FirebaseAuth.getInstance().currentUser

        // 만약 사용자가 로그인이 되어 있지 않다면, 로그인 화면으로 이동
        if (user == null) {
            redirectToLogin()
            return null
        }

        // Firestore에서 데이터 불러오기
        loadDataFromFirestore()

        // 삭제 버튼 클릭 리스너
        binding.postDelete.setOnClickListener {
            if (documentId != null) {
                showDeleteConfirmationDialog() // 삭제 확인 다이얼로그 호출
            } else {
                Toast.makeText(requireContext(), "삭제할 게시물이 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    private fun redirectToLogin() {
        activity?.let {
            val intent = Intent(it, LoginActivity::class.java)
            it.startActivity(intent)
            it.finish() // 현재 액티비티를 종료
        }
    }

    // Firestore에서 데이터를 불러오는 함수
    private fun loadDataFromFirestore() {
        documentId = arguments?.getString("documentId")

        if (documentId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "documentId가 전달되지 않았습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("posts").document(documentId!!)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val post = DiaryFragment.ContentDTO(
                        documentId = document.id,
                        userId = document.getString("userId") ?: "",
                        explain = document.getString("explain") ?: "",
                        imageUrl = document.getString("imageUrl") ?: "",
                        timestamp = document.getLong("timestamp") ?: 0,
                        familyCode = document.getString("familyCode") ?: ""
                    )

                    binding.userId.text = post.userId
                } else {
                    Toast.makeText(requireContext(), "게시물이 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("DiaryDetailFragment", "데이터 로딩 실패: ${exception.message}")
            }
    }

    // 게시물 삭제 함수
    private fun deletePost() {
        documentId?.let { id ->
            firestore.collection("posts").document(id)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "게시물이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp() // 이전 화면으로 돌아가기
                }
                .addOnFailureListener { exception ->
                    Log.e("DiaryDetailFragment", "게시물 삭제 실패: ${exception.message}")
                    Toast.makeText(requireContext(), "게시물 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
        } ?: Log.e("DiaryDetailFragment", "Document ID가 null입니다.")
    }

    // 삭제 확인 다이얼로그를 띄우는 함수
    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("게시물 삭제")
            .setMessage("이 게시물을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ -> deletePost() } // 사용자가 삭제를 확인하면 게시물 삭제
            .setNegativeButton("취소", null) // 취소 버튼을 누르면 아무 일도 하지 않음
            .show()
    }

    // 메모리 누수를 방지하기 위해 onDestroyView에서 _binding을 해제
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}