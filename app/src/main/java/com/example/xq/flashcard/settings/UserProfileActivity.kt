package com.example.xq.flashcard.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.xq.flashcard.R
import com.example.xq.flashcard.base.BaseActivity
import com.example.xq.flashcard.databinding.ActivityUserProfileBinding
import com.example.xq.flashcard.ui.login.AuthService
import com.example.xq.flashcard.ui.login.GuestSessionStore
import com.example.xq.flashcard.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth

class UserProfileActivity : BaseActivity<ActivityUserProfileBinding>() {

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, UserProfileActivity::class.java)
        }
    }

    private val authService by lazy { AuthService(this) }

    override fun inflateViewBinding(layoutInflater: android.view.LayoutInflater): ActivityUserProfileBinding {
        return ActivityUserProfileBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnLogout.setOnClickListener {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                startActivity(
                    LoginActivity.createIntent(this, forceAuth = true).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )
                return@setOnClickListener
            }
            GuestSessionStore.clear(this)
            authService.signOut()
            val intent = LoginActivity.createIntent(this).apply {
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
        val isGuestMode = GuestSessionStore.isGuestMode(this) && user == null
        val isSignedIn = user != null
        binding.tvNameValue.text = user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email
            ?: if (isGuestMode) getString(R.string.auth_guest_name) else null
            ?: getString(R.string.main_user_name_placeholder)
        binding.tvEmailValue.text = user?.email
            ?: if (isGuestMode) getString(R.string.auth_guest_email_placeholder) else null
            ?: getString(R.string.main_user_email_placeholder)
        binding.btnLogout.text = getString(
            if (isSignedIn) R.string.main_logout else R.string.auth_sign_in_to_sync
        )
        binding.btnDeleteAccount.visibility = if (isSignedIn) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
    }
}
