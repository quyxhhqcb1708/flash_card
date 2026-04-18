package com.example.xq.flashcard.ui.main

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.xq.flashcard.ui.MainActivity
import com.example.xq.flashcard.ui.main.fragments.LibraryFragment
import com.example.xq.flashcard.ui.main.fragments.PracticeFragment
import com.example.xq.flashcard.ui.main.fragments.ProgressFragment
import com.example.xq.flashcard.ui.main.fragments.ScanTextFragment
import com.example.xq.flashcard.ui.main.fragments.SettingFragment

class MainPagerAdapter(activity: MainActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PracticeFragment()
            1 -> LibraryFragment()
            2 -> ScanTextFragment()
            3 -> ProgressFragment()
            else -> SettingFragment()
        }
    }
}
