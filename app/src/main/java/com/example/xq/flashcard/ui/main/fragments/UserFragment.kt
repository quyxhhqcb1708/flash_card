package com.example.xq.flashcard.ui.main.fragments

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.xq.flashcard.base.BaseFragment
import com.example.xq.flashcard.databinding.FragmentUserBinding
import com.example.xq.flashcard.ui.login.AuthService
import com.example.xq.flashcard.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth

class UserFragment : BaseFragment<FragmentUserBinding>() {

    private val authService = AuthService()

    override fun inflateLayout(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentUserBinding {
        return FragmentUserBinding.inflate(layoutInflater, container, false)
    }

    override fun onStart() {
        super.onStart()
        val user = FirebaseAuth.getInstance().currentUser
        binding.tvNameValue.text = user?.displayName?.takeIf { it.isNotBlank() } ?: "Name User"
        binding.tvEmailValue.text = user?.email ?: "example@gmail.com"
        binding.btnLogout.setOnClickListener {
            authService.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }
}
