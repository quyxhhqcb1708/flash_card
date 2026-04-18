package com.example.xq.flashcard.ui.main.fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import com.example.xq.flashcard.R
import com.example.xq.flashcard.base.BaseFragment
import com.example.xq.flashcard.databinding.FragmentSettingBinding
import com.example.xq.flashcard.ui.settings.UserProfileActivity
import com.google.firebase.auth.FirebaseAuth

class SettingFragment : BaseFragment<FragmentSettingBinding>() {
    override fun inflateLayout(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSettingBinding {
        return FragmentSettingBinding.inflate(layoutInflater, container, false)
    }

    override fun onStart() {
        super.onStart()
        val currentUser = FirebaseAuth.getInstance().currentUser
        binding.tvName.text = currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: currentUser?.email
            ?: getString(R.string.main_user_name_placeholder)
        binding.itemUser.setOnClickListener {
            startActivity(UserProfileActivity.createIntent(requireContext()))
        }
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
