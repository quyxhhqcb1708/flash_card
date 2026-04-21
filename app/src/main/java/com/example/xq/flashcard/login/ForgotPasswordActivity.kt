package com.example.xq.flashcard.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.example.xq.flashcard.R
import com.example.xq.flashcard.databinding.ActivityForgotPasswordBinding

class ForgotPasswordActivity : AuthFormActivity<ActivityForgotPasswordBinding>() {

    override fun inflateViewBinding(layoutInflater: LayoutInflater): ActivityForgotPasswordBinding {
        return ActivityForgotPasswordBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.ivBack.setOnClickListener { finish() }
        binding.btnResetPassword.tag = binding.btnResetPassword.text
        binding.btnResetPassword.setOnClickListener { submitReset() }
        binding.btnConfirm.setOnClickListener { finish() }
        attachFieldState(
            binding.etEmail,
            binding.ivEmailIcon,
            binding.emailContainer,
            R.drawable.bg_auth_input_normal,
            R.drawable.bg_auth_input_focused
        )
    }

    private fun submitReset() {
        clearError()
        val email = binding.etEmail.text.toString().trim()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailContainer.setBackgroundResource(R.drawable.bg_auth_input_error)
            binding.ivEmailIcon.imageTintList =
                android.content.res.ColorStateList.valueOf(getColor(R.color.auth_error))
            binding.tvEmailError.text = getString(R.string.auth_invalid_email)
            binding.tvEmailError.isVisible = true
            return
        }
        setLoading(true)
        authManager.sendPasswordResetEmail(
            email = email,
            onSuccess = {
                setLoading(false)
                binding.groupRequest.isVisible = false
                binding.groupSuccess.isVisible = true
            },
            onError = { message ->
                setLoading(false)
                binding.emailContainer.setBackgroundResource(R.drawable.bg_auth_input_error)
                binding.ivEmailIcon.imageTintList =
                    android.content.res.ColorStateList.valueOf(getColor(R.color.auth_error))
                binding.tvEmailError.text = message
                binding.tvEmailError.isVisible = true
            }
        )
    }

    private fun clearError() {
        binding.emailContainer.setBackgroundResource(R.drawable.bg_auth_input_normal)
        binding.ivEmailIcon.imageTintList =
            android.content.res.ColorStateList.valueOf(getColor(R.color.auth_icon_neutral))
        binding.tvEmailError.isVisible = false
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressReset.isVisible = isLoading
        binding.btnResetPassword.text =
            if (isLoading) "" else binding.btnResetPassword.tag?.toString().orEmpty()
        binding.btnResetPassword.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
    }
}
