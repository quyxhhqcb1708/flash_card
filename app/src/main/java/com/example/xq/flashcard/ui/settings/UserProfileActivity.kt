package com.example.xq.flashcard.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.xq.flashcard.R
import com.example.xq.flashcard.base.BaseActivity
import com.example.xq.flashcard.databinding.ActivityUserProfileBinding
import com.example.xq.flashcard.ui.login.AuthService
import com.example.xq.flashcard.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth

class UserProfileActivity : BaseActivity<ActivityUserProfileBinding>() {

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, UserProfileActivity::class.java)
        }
    }

    private val authService = AuthService()

    override fun inflateViewBinding(layoutInflater: android.view.LayoutInflater): ActivityUserProfileBinding {
        return ActivityUserProfileBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnLogout.setOnClickListener {
            authService.signOut()
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
        binding.btnDeleteAccount.setOnClickListener {
            Toast.makeText(this, R.string.main_coming_soon, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        bindUser()
    }

    private fun bindUser() {
        val user = FirebaseAuth.getInstance().currentUser
        binding.tvNameValue.text = user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email
            ?: getString(R.string.main_user_name_placeholder)
        binding.tvEmailValue.text = user?.email ?: getString(R.string.main_user_email_placeholder)
    }
}
