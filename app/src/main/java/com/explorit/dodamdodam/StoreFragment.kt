package com.explorit.dodamdodam

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
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


    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_store, container, false)

        storeListView = view.findViewById(R.id.storeListView)
        storeListView.layoutManager = GridLayoutManager(context, 2)

        backToMainButton = view.findViewById(R.id.storeBackBtn)

        fetchStoreItems()

        backToMainButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            parentFragmentManager.popBackStack()
        }

        return view
    }

    private fun fetchStoreItems() {
        firestore.collection("storeItems")
            .get()
            .addOnSuccessListener { result ->
                val items = result.toObjects(StoreItem::class.java)
                storeAdapter = StoreAdapter(items) {selectedItem ->
                    savePurchasedItemToFamily(selectedItem)
                }
                storeListView.adapter = storeAdapter
            }
            .addOnFailureListener { exception ->
                Log.w("StoreFragment", "Error getting documents.", exception)
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
                            val itemId = database.child("families").child(familyCode).child("purchasedItems").push().key

                            if (itemId != null) {
                                database.child("families")
                                    .child(familyCode)
                                    .child("purchasedItems")
                                    .child(itemId)
                                    .setValue(item)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "${item.name} 구매 완료!", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "구매 실패", Toast.LENGTH_SHORT).show()
                                    }
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
    val price: Int = 0
)