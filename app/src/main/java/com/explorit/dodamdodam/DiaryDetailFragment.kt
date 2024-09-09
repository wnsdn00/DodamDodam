package com.explorit.dodamdodam

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.explorit.dodamdodam.databinding.FragmentDiaryDetailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.OkHttpClient

class DiaryDetailFragment : Fragment() {

    private var _binding: FragmentDiaryDetailBinding? = null
    private val binding get() = _binding!!

    private var user: FirebaseUser? = null
    private var firestore: FirebaseFirestore? = null
    private var okHttpClient: OkHttpClient? = null

    // 게시물의 Firestore 문서 ID를 저장할 변수
    private var documentId: String? = null
    private var postOwnerId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // View Binding 설정
        _binding = FragmentDiaryDetailBinding.inflate(inflater, container, false)

        // Firebase 인증 및 Firestore 초기화
        user = FirebaseAuth.getInstance().currentUser
        firestore = FirebaseFirestore.getInstance()
        okHttpClient = OkHttpClient()

        // 만약 사용자가 로그인이 되어 있지 않다면, 로그인 화면으로 이동
        if (user == null) {
            activity?.let {
                val intent = Intent(it, LoginActivity::class.java)
                it.startActivity(intent)
                it.finish() // 현재 액티비티를 종료
            }
            return null
        }

        // Firestore에서 데이터 불러오기
        loadDataFromFirestore()

        // 삭제 버튼 클릭 리스너 설정
        binding.postDelete.setOnClickListener {
            Log.d("DiaryDetailFragment", "Delete button clicked")
            deletePost()
        }

        return binding.root
    }

    // Firestore에서 데이터를 불러오는 함수
    private fun loadDataFromFirestore() {
        // 인텐트나 번들로 전달된 documentId를 가져옴
        documentId = arguments?.getString("documentId")
        documentId?.let { id ->
            firestore?.collection("posts")?.document(id)
                ?.get()
                ?.addOnSuccessListener { document ->
                    if (document != null) {
                        // 데이터를 가져와서 UI에 반영하는 작업
                        binding.userProfileName.text = document.getString("userId")
                        postOwnerId = document.getString("userId")  // 게시물 작성자 ID 저장

                        // 현재 로그인한 사용자와 게시물 작성자가 동일하면 삭제 버튼 표시
                        if (user?.uid == postOwnerId) {
                            binding.postDelete.visibility = View.VISIBLE
                        } else {
                            binding.postDelete.visibility = View.GONE
                        }
                    }
                }
                ?.addOnFailureListener { exception ->
                    Log.e("DiaryDetailFragment", "데이터 로딩 실패: ${exception.message}")
                }
        }
    }

    // 게시물을 삭제하는 함수
    private fun deletePost() {
        documentId?.let { id ->
            firestore?.collection("posts")?.document(id)
                ?.delete()
                ?.addOnSuccessListener {
                    // 삭제 성공 시 사용자에게 알림 표시
                    Toast.makeText(requireContext(), "게시물이 삭제되었습니다.", Toast.LENGTH_SHORT).show()

                    // 삭제 후 이전 화면으로 돌아가기
                    findNavController().navigateUp() // 현재 Fragment에서 이전 Fragment로 돌아감
                }
                ?.addOnFailureListener { exception ->
                    // 삭제 실패 시 오류 메시지 출력
                    Log.e("DiaryDetailFragment", "게시물 삭제 실패: ${exception.message}")
                    Toast.makeText(requireContext(), "게시물 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // 메모리 누수를 방지하기 위해 onDestroyView에서 _binding을 해제
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}