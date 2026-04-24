package com.example.xq.flashcard.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.viewpager2.widget.ViewPager2
import com.example.xq.flashcard.R
import com.example.xq.flashcard.base.BaseActivity
import com.example.xq.flashcard.databinding.ActivityMainBinding

class MainActivity : BaseActivity<ActivityMainBinding>(), MainNavigationHost {

    companion object {
        private const val EXTRA_START_TAB = "extra_start_tab"
        private const val TAB_PRACTICE = "tab_practice"

        fun createPracticeIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_START_TAB, TAB_PRACTICE)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        }
    }

    override fun inflateViewBinding(layoutInflater: android.view.LayoutInflater): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupViewPager()
        setupBottomNavigation()
        handleStartDestination(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleStartDestination(intent)
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = MainPagerAdapter(this)
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateBottomBar(position)
            }
        })
    }

    private fun setupBottomNavigation() {
        binding.navHome.setOnClickListener { openScanText() }
        binding.homeButtonWrapper.setOnClickListener { openScanText() }
        binding.navCard.setOnClickListener { openPractice() }
        binding.navLibrary.setOnClickListener { openLibrary() }
        binding.navUser.setOnClickListener { openProgress() }
        binding.navSetting.setOnClickListener { openSetting() }
    }

    override fun openPractice() {
        binding.viewPager.currentItem = 0
    }

    override fun openLibrary() {
        binding.viewPager.currentItem = 1
    }

    override fun openScanText() {
        binding.viewPager.currentItem = 2
    }

    override fun openProgress() {
        binding.viewPager.currentItem = 3
    }

    override fun openSetting() {
        binding.viewPager.currentItem = 4
    }

    private fun handleStartDestination(intent: Intent?) {
        when (intent?.getStringExtra(EXTRA_START_TAB)) {
            TAB_PRACTICE -> openPractice()
            else -> openScanText()
        }
    }

    private fun updateBottomBar(position: Int) {
        val selectedColor = getColor(R.color.main_nav_selected)
        val normalColor = getColor(R.color.main_nav_unselected)

        binding.ivCard.setColorFilter(if (position == 0) selectedColor else normalColor)
        binding.tvCard.setTextColor(if (position == 0) selectedColor else normalColor)

        binding.ivLibrary.setColorFilter(if (position == 1) selectedColor else normalColor)
        binding.tvLibrary.setTextColor(if (position == 1) selectedColor else normalColor)

        binding.ivUser.setColorFilter(if (position == 3) selectedColor else normalColor)
        binding.tvUser.setTextColor(if (position == 3) selectedColor else normalColor)

        binding.ivSetting.setColorFilter(if (position == 4) selectedColor else normalColor)
        binding.tvSetting.setTextColor(if (position == 4) selectedColor else normalColor)

        binding.homeButtonWrapper.alpha = if (position == 2) 1f else 0.75f
    }
}
