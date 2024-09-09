package com.explorit.dodamdodam

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.explorit.dodamdodam.databinding.ActivityMainPageBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

const val TAG_HOME = "HomeFragment"
private const val TAG_QUESTION = "RandomQuestionFragment"
private const val TAG_CALENDAR = "MissionCalendarFragment"
private const val TAG_DIARY = "DiaryFragment"
private const val TAG_MY_PAGE = "MyPageFragment"

private lateinit var database:DatabaseReference
private lateinit var familyCoinsTextView: TextView



class MainPageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        familyCoinsTextView = findViewById(R.id.familyCoinsTextView)
        database = FirebaseDatabase.getInstance().reference

        fetchFamilyCoins()

        // 사용자 정보 가져오기
        val userName = intent.getStringExtra("user_name") ?: "Unkown"
        val userBirthday = intent.getStringExtra("user_birthday") ?: "0000.00.00"

        // 초기 프래그먼트 설정
        if (savedInstanceState == null) {
            setFragment(TAG_HOME, HomeFragment())
        }

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.diaryFragment -> setFragment(TAG_DIARY, DiaryFragment())
                R.id.questionFragment -> setFragment(TAG_QUESTION, RandomQuestionFragment())
                R.id.calendarFragment -> setFragment(TAG_CALENDAR, MissionCalendarFragment())
                R.id.myPageFragment -> {
                    val myPageFragment = MyPageFragment.newInstance(userName, userBirthday)
                    setFragment(TAG_MY_PAGE, myPageFragment)
                }
            }
            true
        }
    }


    fun setFragment(tag: String, fragment: Fragment) {
        val manager: FragmentManager = supportFragmentManager
        val fragTransaction = manager.beginTransaction()

        fragTransaction.replace(R.id.mainFrameLayout, fragment, tag)

        fragTransaction.commitAllowingStateLoss()
        /*
        val currentFragment = manager.findFragmentByTag(tag)

        if(currentFragment == null){
            fragTransaction.add(R.id.mainFrameLayout, fragment, tag)
        }

        val fragments = listOf (
            manager.findFragmentByTag(TAG_QUESTION),
            manager.findFragmentByTag(TAG_CALENDAR),
            manager.findFragmentByTag(TAG_DIARY),
            manager.findFragmentByTag(TAG_MY_PAGE),
        )

        for (frag in fragments) {
            if (frag != null) {
                fragTransaction.hide(frag)
            }
        }

        if (currentFragment != null) {
            fragTransaction.show(currentFragment)
        } else {
            fragTransaction.show(fragment)
        }

        fragTransaction.commitAllowingStateLoss()

    }

        */
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
                                        Toast.makeText(this@MainPageActivity, "코인 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                })
                        } else {
                            Toast.makeText(this@MainPageActivity, "가족 그룹을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@MainPageActivity, "데이터를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    fun updateFamilyCoins() {
        fetchFamilyCoins() // 코인 정보를 업데이트하는 메서드 호출
    }

}

