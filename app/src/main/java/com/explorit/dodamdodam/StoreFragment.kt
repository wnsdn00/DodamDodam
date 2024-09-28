package com.explorit.dodamdodam

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [RandomQuestionFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

class StoreFragment : Fragment() {

    private lateinit var storeListView: RecyclerView
    private lateinit var storeAdapter: StoreAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private  lateinit var backToMainButton: ImageButton
    private val database = FirebaseDatabase.getInstance().reference
    private var selectedCategory: String = "character"
    private lateinit var familyCoinsTextView: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_store, container, false)

        storeListView = view.findViewById(R.id.storeListView)
        storeListView.layoutManager = GridLayoutManager(context, 2)

        familyCoinsTextView = view.findViewById(R.id.coins)
        backToMainButton = view.findViewById(R.id.storeBackBtn)


        val characterButton = view.findViewById<Button>(R.id.btn_character)
        val fashionButton = view.findViewById<Button>(R.id.btn_fashion) // 추가된 버튼이라면
        val backgroundButton = view.findViewById<Button>(R.id.btn_background)

        fetchStoreItems()
        fetchFamilyCoins()
        setSelectedButton(characterButton, backgroundButton, fashionButton)

        backToMainButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            parentFragmentManager.popBackStack()
        }

        characterButton.setOnClickListener {
            setSelectedButton(characterButton, backgroundButton, fashionButton)
            selectedCategory = "character"
            fetchStoreItems()  // 캐릭터 아이템만 보여줍니다.
        }

        // 패션 버튼 클릭 시 (추가된 버튼이므로 필요 시)
        fashionButton?.setOnClickListener {
            setSelectedButton(fashionButton, characterButton, backgroundButton)
            selectedCategory = "fashion"
            fetchStoreItems()  // 패션 아이템만 보여줍니다.
        }

        // 배경 버튼 클릭 시
        backgroundButton.setOnClickListener {
            setSelectedButton(backgroundButton, characterButton, fashionButton)
            selectedCategory = "background"
            fetchStoreItems()  // 배경 아이템만 보여줍니다.
        }

        return view
    }

    private fun fetchStoreItems() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserUid != null) {
            database.child("users").child(currentUserUid).addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val familyCode = snapshot.child("familyCode").getValue(String::class.java)
                    if (familyCode != null) {
                        database.child("families").child(familyCode).child("purchasedItems").get().addOnSuccessListener { purchasedSnapshot ->
                            val purchasedItems = purchasedSnapshot.children.map { it.child("name").getValue(String::class.java) }

                            firestore.collection("storeItems")
                                .get()
                                .addOnSuccessListener { result ->
                                    val filteredItems = mutableListOf<StoreItem>()

                                    for(document in result){
                                        val item = document.toObject(StoreItem::class.java)
                                        if (item != null && item.itemCategory == selectedCategory) {
                                            filteredItems.add(item)
                                        }
                                    }

                                    storeAdapter = StoreAdapter(filteredItems, purchasedItems) {selectedItem ->
                                        savePurchasedItemToFamily(selectedItem)
                                    }
                                    storeListView.adapter = storeAdapter



                                }
                                .addOnFailureListener { exception ->
                                    Log.w("StoreFragment", "Error getting documents.", exception)
                                }

                        }
                    }
                } override fun onCancelled(error: DatabaseError) {
                    // 에러 처리
                }

            })
        }
    }

    private fun savePurchasedItemToFamily(item: StoreItem) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserUid != null) {
            database.child("users").child(currentUserUid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val familyCode = snapshot.child("familyCode").getValue(String::class.java)
                        if (familyCode != null) {
                            // 가족 그룹의 현재 코인 가져오기
                            database.child("families").child(familyCode).child("familyCoin")
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(coinSnapshot: DataSnapshot) {
                                        val currentCoins = coinSnapshot.getValue(Int::class.java) ?: 0

                                        // 아이템 가격과 비교
                                        if (currentCoins >= item.price) {
                                            // 아이템 구매 후 코인 차감
                                            val newCoinBalance = currentCoins - item.price
                                            database.child("families").child(familyCode).child("familyCoin")
                                                .setValue(newCoinBalance)
                                                .addOnSuccessListener {
                                                    // 아이템 구매 완료 처리
                                                    addPurchasedItemToFamily(familyCode, item)
                                                }.addOnFailureListener {
                                                    Toast.makeText(context, "코인 차감 실패", Toast.LENGTH_SHORT).show()
                                                }
                                        } else {
                                            Toast.makeText(context, "코인이 부족합니다.", Toast.LENGTH_SHORT).show()
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Toast.makeText(context, "코인 정보를 불러오는 중 오류 발생", Toast.LENGTH_SHORT).show()
                                    }
                                })
                        } else {
                            Toast.makeText(context, "가족 그룹을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, "데이터를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                        Log.e("RandomQuestionFragment", "Database error: ${error.message}")
                    }
                })
        } else {
            Toast.makeText(context, "사용자가 로그인되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
            Log.e("RandomQuestionFragment", "Current user UID is null")
        }
    }

    private fun addPurchasedItemToFamily(familyCode: String, item: StoreItem){
        val itemId = database.child("families").child(familyCode).child("purchasedItems").push().key

        if (itemId != null) {
            database.child("families")
                .child(familyCode)
                .child("purchasedItems")
                .child(itemId)
                .setValue(item)
                .addOnSuccessListener {
                    Toast.makeText(context, "${item.name} 구매 완료!", Toast.LENGTH_SHORT).show()
                    //item.isPurchased = true
                    storeAdapter.notifyDataSetChanged()  // 어댑터에 변경 알림
                    fetchStoreItems()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "구매 실패", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // 버튼 선택 상태 변경 함수
    private fun setSelectedButton(selected: Button, vararg others: Button) {
        // 선택된 버튼은 선택 상태로 표시
        selected.isSelected = true
        // 다른 버튼은 선택 해제
        others.forEach { it.isSelected = false }
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

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment RandomQuestionFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            RandomQuestionFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}



data class StoreItem(
    val name: String = "",
    val imageUrl: String = "",
    val price: Int = 0,
    val itemCategory: String = ""
)