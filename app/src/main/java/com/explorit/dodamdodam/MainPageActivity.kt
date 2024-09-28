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
private const val TAG_SEARCH = "SearchGragment"

private lateinit var database:DatabaseReference




class MainPageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().reference



        // 초기 프래그먼트 설정
        if (savedInstanceState == null) {
            setFragment(TAG_HOME, HomeFragment())
        }

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.diaryFragment -> setFragment(TAG_DIARY, DiaryFragment())
                R.id.questionFragment -> setFragment(TAG_QUESTION, RandomQuestionFragment())
                R.id.calendarFragment -> setFragment(TAG_CALENDAR, MissionCalendarFragment())
                R.id.myPageFragment -> setFragment(TAG_MY_PAGE, MyPageFragment())

            }
            true
        }
    }

    // 지정한 프래그먼트를 보여주는 함수
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


}

