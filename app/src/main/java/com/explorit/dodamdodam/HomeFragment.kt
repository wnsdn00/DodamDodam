package com.explorit.dodamdodam

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var familyCoinsTextView: TextView
    private lateinit var mainScreenImageView: ImageView
    private val database = FirebaseDatabase.getInstance().reference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        familyCoinsTextView = view.findViewById(R.id.familyCoinsTextView)
        mainScreenImageView = view.findViewById(R.id.mainScreenImageView)

        // 선택된 아이템 불러오기
        loadMainScreenItems()
        fetchFamilyCoins()

        return view
    }

    private fun loadMainScreenItems() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserUid != null) {
            database.child("users").child(currentUserUid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val familyCode = snapshot.child("familyCode").getValue(String::class.java)
                        if (familyCode != null) {
                            database.child("families").child(familyCode)
                                .child("mainScreenItems")
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                                        val item = dataSnapshot.getValue(StoreItem::class.java)
                                        if (item != null) {
                                            // 메인 화면에 이미지 표시
                                            Glide.with(requireContext())
                                                .load(item.imageUrl)
                                                .into(mainScreenImageView)
                                        } else {
                                            Toast.makeText(context, "선택된 아이템이 없습니다.", Toast.LENGTH_SHORT).show()
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Log.e("MainFragment", "Error loading main screen items", error.toException())
                                    }
                                })
                        } else {
                            Toast.makeText(context, "가족 그룹을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, "데이터 불러오기 실패", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun fetchFamilyCoins() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserUid != null) {
            database.child("users").child(currentUserUid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val familyCode = snapshot.child("familyCode").getValue(String::class.java)
                        if (familyCode != null) {
                            database.child("families").child(familyCode).child("familyCoin")
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        val coins = snapshot.getValue(Int::class.java) ?: 0
                                        familyCoinsTextView.text = "$coins"
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Toast.makeText(requireContext(), "코인 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                })
                        } else {
                            Toast.makeText(requireContext(), "가족 그룹을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(requireContext(), "데이터를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    fun updateFamilyCoins() {
        fetchFamilyCoins() // 코인 정보를 업데이트하는 메서드 호출
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}