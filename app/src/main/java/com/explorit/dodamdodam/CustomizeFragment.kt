package com.explorit.dodamdodam

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
 * Use the [RandomQuestionFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

class CustomizeFragment : Fragment() {

    private lateinit var itemNameListView: RecyclerView
    private lateinit var customizeAdapter: CustomizeAdapter
    private lateinit var selectedItemView: ImageView
    private lateinit var applyButton: Button
    private  lateinit var backToMainButton: ImageButton
    private val database = FirebaseDatabase.getInstance().reference
    private lateinit var auth: FirebaseAuth
    private var selectedItem: StoreItem? = null
    private var selectedCategory: String = "character"

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_customize, container, false)

        itemNameListView = view.findViewById(R.id.itemNameRecyclerView)
        selectedItemView = view.findViewById(R.id.selectedItemImage)
        applyButton = view.findViewById(R.id.btn_apply)
        applyButton.visibility = View.GONE // 버튼 처음에는 숨김 처리

        val characterButton = view.findViewById<Button>(R.id.btn_character)
        val fashionButton = view.findViewById<Button>(R.id.btn_fashion) // 추가된 버튼이라면
        val backgroundButton = view.findViewById<Button>(R.id.btn_background)



        itemNameListView.layoutManager = GridLayoutManager(context, 3)

        fetchPurchasedItems()
        setSelectedButton(characterButton, backgroundButton, fashionButton)

        backToMainButton = view.findViewById(R.id.customizeBackBtn)

        backToMainButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            parentFragmentManager.popBackStack()
        }

        applyButton.setOnClickListener {
            selectedItem?.let { item ->
                saveSelectedItemToMainScreen(item)
                Toast.makeText(context, "메인 화면에 적용되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        characterButton.setOnClickListener {
            setSelectedButton(characterButton, backgroundButton, fashionButton)
            selectedCategory = "character"
            fetchPurchasedItems()  // 캐릭터 아이템만 보여줍니다.
        }

        // 패션 버튼 클릭 시 (추가된 버튼이므로 필요 시)
        fashionButton?.setOnClickListener {
            setSelectedButton(fashionButton, characterButton, backgroundButton)
            selectedCategory = "fashion"
            fetchPurchasedItems()  // 패션 아이템만 보여줍니다.
        }

        // 배경 버튼 클릭 시
        backgroundButton.setOnClickListener {
            setSelectedButton(backgroundButton, characterButton, fashionButton)
            selectedCategory = "background"
            fetchPurchasedItems()  // 배경 아이템만 보여줍니다.
        }

        return view
    }


    private fun fetchPurchasedItems() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserUid != null) {
            database.child("users").child(currentUserUid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val familyCode = snapshot.child("familyCode").getValue(String::class.java)
                        if (familyCode != null) {
                            database.child("families")
                                .child(familyCode)
                                .child("purchasedItems")
                                .get()
                                .addOnSuccessListener { dataSnapshot ->
                                    val items = mutableListOf<StoreItem>()
                                    for (itemSnapshot in dataSnapshot.children) {
                                        val item = itemSnapshot.getValue(StoreItem::class.java)
                                        if (item != null && item.itemCategory == selectedCategory) {
                                            items.add(item)
                                        }
                                    }

                                    // 아이템을 RecyclerView 어댑터에 연결
                                    customizeAdapter = CustomizeAdapter(items) { selectedItem ->
                                        // 아이템 이름 클릭 시 중앙에 이미지 표시
                                        Glide.with(requireContext()).load(selectedItem.imageUrl).into(selectedItemView)

                                        this@CustomizeFragment.selectedItem = selectedItem

                                        applyButton.visibility = View.VISIBLE
                                    }
                                    itemNameListView.adapter = customizeAdapter

                                }
                                .addOnFailureListener { exception ->
                                    Log.w("CustomizeFragment", "Error getting purchased items.", exception)
                                }
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

    private fun saveSelectedItemToMainScreen(selectedItem: StoreItem) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserUid != null) {
            database.child("users").child(currentUserUid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val familyCode = snapshot.child("familyCode").getValue(String::class.java)
                        if (familyCode != null) {
                            if (selectedItem.itemCategory == "character") {
                                // 선택한 아이템을 가족의 'mainScreenItems'에 저장
                                database.child("families").child(familyCode)
                                    .child("mainScreenItems")
                                    .setValue(selectedItem)
                            } else if (selectedItem.itemCategory == "background") {
                                database.child("families").child(familyCode)
                                    .child("mainScreenBackground")
                                    .setValue(selectedItem)
                            }
                        }

                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, "데이터 저장 실패", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun setSelectedButton(selected: Button, vararg others: Button) {
        // 선택된 버튼은 선택 상태로 표시
        selected.isSelected = true
        // 다른 버튼은 선택 해제
        others.forEach { it.isSelected = false }
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
