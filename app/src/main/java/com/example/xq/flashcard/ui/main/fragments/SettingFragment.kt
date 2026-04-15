package com.example.xq.flashcard.ui.main.fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import com.example.xq.flashcard.R
import com.example.xq.flashcard.base.BaseFragment
import com.example.xq.flashcard.databinding.FragmentSettingBinding
import com.example.xq.flashcard.ui.main.MainNavigationHost

class SettingFragment : BaseFragment<FragmentSettingBinding>() {
    override fun inflateLayout(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSettingBinding {
        return FragmentSettingBinding.inflate(layoutInflater, container, false)
    }

    override fun onStart() {
        super.onStart()
        val host = activity as? MainNavigationHost
        binding.itemUser.setOnClickListener { host?.openUser() }
        binding.itemNotification.setOnClickListener {
            Toast.makeText(requireContext(), R.string.main_coming_soon, Toast.LENGTH_SHORT).show()
        }
        binding.itemSound.setOnClickListener {
            Toast.makeText(requireContext(), R.string.main_coming_soon, Toast.LENGTH_SHORT).show()
        }
        binding.itemSync.setOnClickListener {
            Toast.makeText(requireContext(), R.string.main_coming_soon, Toast.LENGTH_SHORT).show()
        }
    }
}
