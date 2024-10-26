package com.explorit.dodamdodam

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.explorit.dodamdodam.databinding.ItemDiaryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import okhttp3.OkHttpClient

class DiaryDetailFragment : Fragment() {

    private var _binding: ItemDiaryBinding? = null
    private val binding get() = _binding!!

    private var user: FirebaseUser? = null
    private var database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private var okHttpClient: OkHttpClient = OkHttpClient()

    // 게시물의 Realtime Database 키를 저장할 변수
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

        // Realtime Database에서 데이터 불러오기
        loadDataFromDatabase()

        return binding.root
    }

    private fun redirectToLogin() {
        activity?.let {
            val intent = Intent(it, LoginActivity::class.java)
            it.startActivity(intent)
            it.finish() // 현재 액티비티를 종료
        }
    }

    // Realtime Database에서 데이터를 불러오는 함수
    private fun loadDataFromDatabase() {
        documentId = arguments?.getString("documentId")

        if (documentId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "documentId가 전달되지 않았습니다.", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        val postRef: DatabaseReference = database.getReference("posts").child(documentId!!)
        postRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val post = dataSnapshot.getValue(DiaryFragment.ContentDTO::class.java)
                if (post != null) {
                    binding.nickName.text = post.nickName
                    binding.postExplain.text = post.explain

                    // 이미지 및 비디오 처리

                } else {
                    Toast.makeText(requireContext(), "게시물이 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("DiaryDetailFragment", "데이터 로딩 실패: ${databaseError.message}")
                Toast.makeText(requireContext(), "데이터 로딩에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }



    // 메모리 누수를 방지하기 위해 onDestroyView에서 _binding을 해제
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}