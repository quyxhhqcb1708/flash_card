package com.example.xq.flashcard.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.xq.flashcard.R
import com.example.xq.flashcard.databinding.ActivityMainBinding
import com.example.xq.flashcard.ui.main.MainNavigationHost
import com.example.xq.flashcard.ui.main.MainPagerAdapter

class MainActivity : AppCompatActivity(), MainNavigationHost {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupViewPager()
        setupBottomNavigation()
        openScanText()
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
        binding.navCard.setOnClickListener { openCard() }
        binding.navLibrary.setOnClickListener { openLibrary() }
        binding.navUser.setOnClickListener { openUser() }
        binding.navSetting.setOnClickListener { openSetting() }
    }

    override fun openCard() {
        binding.viewPager.currentItem = 0
    }

    override fun openLibrary() {
        binding.viewPager.currentItem = 1
    }

    override fun openScanText() {
        binding.viewPager.currentItem = 2
    }

    override fun openUser() {
        binding.viewPager.currentItem = 3
    }

    override fun openSetting() {
        binding.viewPager.currentItem = 4
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
