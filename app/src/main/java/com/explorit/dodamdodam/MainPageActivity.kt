package com.explorit.dodamdodam

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.explorit.dodamdodam.databinding.ActivityMainPageBinding

private const val TAG_HOME = "HomeFragment"
private const val TAG_QUESTION = "RandomQuestionFragment"
private const val TAG_CALENDAR = "MissionCalendarFragment"
private const val TAG_DIARY = "DiaryFragment"
private const val TAG_MY_PAGE = "MyPageFragment"

class MainPageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            setFragment(TAG_HOME, HomeFragment())
        }

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.diaryFragment -> setFragment(TAG_DIARY, DiaryFragment())
                R.id.questionFragment -> setFragment(TAG_QUESTION, RandomQuestionFragment())
                R.id.calendarFragment -> setFragment(TAG_CALENDAR, MissionCalendarFragment())
                R.id.myPageFragment-> setFragment(TAG_MY_PAGE, MyPageFragment())
            }
            true
        }
    }

    private fun setFragment(tag: String, fragment: Fragment) {
        val manager: FragmentManager = supportFragmentManager
        val fragTransaction = manager.beginTransaction()

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

}


