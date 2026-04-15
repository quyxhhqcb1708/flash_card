package com.example.xq.flashcard.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import com.example.xq.flashcard.R
import com.example.xq.flashcard.databinding.ActivityRegisterBinding

class RegisterActivity : AuthFormActivity<ActivityRegisterBinding>() {

    override fun inflateViewBinding(layoutInflater: LayoutInflater): ActivityRegisterBinding {
        return ActivityRegisterBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val formBinding = binding.authForm
        formBinding.confirmPasswordContainer.visibility = android.view.View.VISIBLE
        formBinding.tvConfirmPassword.visibility = android.view.View.VISIBLE
        formBinding.ivToggleConfirmPassword.visibility = android.view.View.VISIBLE
        setupCommonUi(
            formBinding = formBinding,
            title = getString(R.string.auth_register_title),
            subtitle = getString(R.string.auth_register_subtitle),
            primaryAction = getString(R.string.auth_register_action),
            switchPrefix = getString(R.string.auth_have_account),
            switchAction = getString(R.string.auth_sign_in_link),
            isLogin = false,
            primaryButtonClick = { register() },
            switchClick = { gotoLogin() }
        )
        attachFieldState(
            formBinding.etConfirmPassword,
            formBinding.ivConfirmPasswordIcon,
            formBinding.confirmPasswordContainer,
            R.drawable.bg_auth_input_normal,
            R.drawable.bg_auth_input_focused
        )
        cachePrimaryLabel(formBinding)
        formBinding.ivToggleConfirmPassword.setOnClickListener {
            togglePasswordField(formBinding.etConfirmPassword, formBinding.ivToggleConfirmPassword)
        }
    }

    private fun register() {
        val formBinding = binding.authForm
        if (!validate(formBinding, checkConfirmPassword = true)) return
        setLoading(formBinding, true)
        authManager.register(
            email = formBinding.etEmail.text.toString().trim(),
            password = formBinding.etPassword.text.toString(),
            onSuccess = {
                setLoading(formBinding, false)
                Toast.makeText(this, R.string.auth_register_success, Toast.LENGTH_SHORT).show()
                navigateToMain()
            },
            onError = { message ->
                setLoading(formBinding, false)
                showFieldError(
                    formBinding.emailContainer,
                    formBinding.ivEmailIcon,
                    formBinding.tvEmailError,
                    message
                )
            }
        )
    }
}
