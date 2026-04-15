package com.example.xq.flashcard.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import com.example.xq.flashcard.R
import com.example.xq.flashcard.databinding.ActivityLoginBinding

class LoginActivity : AuthFormActivity<ActivityLoginBinding>() {

    override fun inflateViewBinding(layoutInflater: LayoutInflater): ActivityLoginBinding {
        return ActivityLoginBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val formBinding = binding.authForm
        setupCommonUi(
            formBinding = formBinding,
            title = getString(R.string.auth_login_title),
            subtitle = getString(R.string.auth_login_subtitle),
            primaryAction = getString(R.string.auth_login_action),
            switchPrefix = getString(R.string.auth_no_account),
            switchAction = getString(R.string.auth_sign_up_link),
            isLogin = true,
            primaryButtonClick = { login() },
            switchClick = { gotoRegister() }
        )
        cachePrimaryLabel(formBinding)
        formBinding.tvForgotPassword.setOnClickListener {
            Toast.makeText(this, R.string.auth_forgot_password_placeholder, Toast.LENGTH_SHORT)
                .show()
        }

        if (authManager.isLoggedIn()) {
            navigateToMain()
        }
    }

    private fun login() {
        val formBinding = binding.authForm
        if (!validate(formBinding)) return
        setLoading(formBinding, true)
        authManager.signIn(
            email = formBinding.etEmail.text.toString().trim(),
            password = formBinding.etPassword.text.toString(),
            onSuccess = {
                setLoading(formBinding, false)
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
