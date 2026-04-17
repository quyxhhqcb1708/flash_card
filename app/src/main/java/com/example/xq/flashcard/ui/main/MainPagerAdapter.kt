package com.example.xq.flashcard.ui.main

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.xq.flashcard.ui.MainActivity
import com.example.xq.flashcard.ui.main.fragments.CardFragment
import com.example.xq.flashcard.ui.main.fragments.LibraryFragment
import com.example.xq.flashcard.ui.main.fragments.ScanTextFragment
import com.example.xq.flashcard.ui.main.fragments.SettingFragment
import com.example.xq.flashcard.ui.main.fragments.UserFragment

class MainPagerAdapter(activity: MainActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CardFragment()
            1 -> LibraryFragment()
            2 -> ScanTextFragment()
            3 -> UserFragment()
            else -> SettingFragment()
        }
    }
}
