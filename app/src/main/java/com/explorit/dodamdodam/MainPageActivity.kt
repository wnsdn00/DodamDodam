package com.explorit.dodamdodam

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.replace
import com.explorit.dodamdodam.databinding.ActivityMainPageBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

const val TAG_HOME = "HomeFragment"
private const val TAG_QUESTION = "RandomQuestionFragment"
private const val TAG_CALENDAR = "MissionCalendarFragment"
private const val TAG_DIARY = "DiaryFragment"
private const val TAG_MY_PAGE = "MyPageFragment"


private lateinit var database: DatabaseReference

class MainPageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().reference

        // 초기 프래그먼트 설정
        if (savedInstanceState == null) {
            setFragment(TAG_HOME, HomeFragment(), false) // 백 스택에 추가하지 않음
        }

        // 바텀 네비게이션 아이템 선택 리스너
        binding.bottomNavigationView.setOnItemSelectedListener { item ->

            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            }

            when (item.itemId) {
                R.id.homeFragment -> setFragment(TAG_HOME, HomeFragment(), false)
                R.id.diaryFragment -> setFragment(TAG_DIARY, DiaryFragment(), false)
                R.id.questionFragment -> setFragment(TAG_QUESTION, RandomQuestionFragment(), false)
                R.id.calendarFragment -> setFragment(TAG_CALENDAR, MissionCalendarFragment(), false)
                R.id.myPageFragment -> setFragment(TAG_MY_PAGE, MyPageFragment(), false)
            }
            true
        }
    }

    // 지정한 프래그먼트를 보여주는 함수
    fun setFragment(tag: String, fragment: Fragment, addToBackStack: Boolean) {
        val manager: FragmentManager = supportFragmentManager
        val fragTransaction = manager.beginTransaction()

        val currentFragment = manager.findFragmentByTag(tag)

        // 이미 추가된 프래그먼트가 있으면 숨김
        val fragments = listOf(
            manager.findFragmentByTag(TAG_HOME),
            manager.findFragmentByTag(TAG_QUESTION),
            manager.findFragmentByTag(TAG_CALENDAR),
            manager.findFragmentByTag(TAG_DIARY),
            manager.findFragmentByTag(TAG_MY_PAGE)
        )

        for (frag in fragments) {
            if (frag != null) {
                fragTransaction.hide(frag)
            }
        }

        // 새로운 프래그먼트를 보여줌
        if (currentFragment == null) {
            fragTransaction.add(R.id.mainFrameLayout, fragment, tag)
            if (addToBackStack) {
                fragTransaction.addToBackStack(tag) // 백 스택에 추가
            }
        } else {
            fragTransaction.show(currentFragment)
        }

        fragTransaction.commitAllowingStateLoss()
    }

    // 다른 기능을 위한 프래그먼트를 띄우는 함수
    fun showNewFragment(fragment: Fragment) {
        val fragTransaction = supportFragmentManager.beginTransaction()
        fragTransaction.add(R.id.mainFrameLayout, fragment)
        fragTransaction.addToBackStack(null) // 백 스택에 추가
        fragTransaction.commitAllowingStateLoss()
    }
}
