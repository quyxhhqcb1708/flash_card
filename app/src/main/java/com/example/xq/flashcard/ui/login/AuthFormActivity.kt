package com.example.xq.flashcard.ui.login

import android.content.Intent
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import com.example.xq.flashcard.R
import com.example.xq.flashcard.base.BaseActivity
import com.example.xq.flashcard.databinding.LayoutAuthFormBinding
import com.example.xq.flashcard.ui.MainActivity

abstract class AuthFormActivity<VB : ViewBinding> : BaseActivity<VB>() {

    protected val authManager = AuthService()

    protected fun setupCommonUi(
        formBinding: LayoutAuthFormBinding,
        title: String,
        subtitle: String,
        primaryAction: String,
        switchPrefix: String,
        switchAction: String,
        isLogin: Boolean,
        primaryButtonClick: () -> Unit,
        switchClick: () -> Unit,
        googleClick: (() -> Unit)? = null,
        showGoogle: Boolean = isLogin
    ) {
        formBinding.tvTitle.text = title
        formBinding.tvSubtitle.text = subtitle
        formBinding.btnPrimary.text = primaryAction
        formBinding.tvSwitchPrefix.text = switchPrefix
        formBinding.tvSwitchAction.text = switchAction
        formBinding.tvForgotPassword.isVisible = isLogin
        formBinding.tvGoogleButton.text = getString(R.string.auth_google_cta)
        formBinding.googleContainer.isVisible = showGoogle
        formBinding.dividerContainer.isVisible = showGoogle
        formBinding.btnPrimary.setOnClickListener { primaryButtonClick() }
        formBinding.llSwitchAccount.setOnClickListener { switchClick() }
        formBinding.googleContainer.setOnClickListener {
            if (googleClick != null) {
                googleClick.invoke()
            } else {
                Toast.makeText(this, R.string.auth_google_placeholder, Toast.LENGTH_SHORT).show()
            }
        }

        attachFieldState(
            formBinding.etEmail,
            formBinding.ivEmailIcon,
            formBinding.emailContainer,
            R.drawable.bg_auth_input_normal,
            R.drawable.bg_auth_input_focused
        )
        attachFieldState(
            formBinding.etPassword,
            formBinding.ivPasswordIcon,
            formBinding.passwordContainer,
            R.drawable.bg_auth_input_normal,
            R.drawable.bg_auth_input_focused
        )

        formBinding.ivTogglePassword.setOnClickListener {
            togglePasswordField(formBinding.etPassword, formBinding.ivTogglePassword)
        }
    }

    protected fun validate(
        formBinding: LayoutAuthFormBinding,
        checkConfirmPassword: Boolean = false
    ): Boolean {
        var isValid = true
        clearFieldError(
            formBinding.emailContainer,
            formBinding.ivEmailIcon,
            formBinding.tvEmailError
        )
        clearFieldError(
            formBinding.passwordContainer,
            formBinding.ivPasswordIcon,
            formBinding.tvPasswordError
        )
        if (checkConfirmPassword) {
            clearFieldError(
                formBinding.confirmPasswordContainer,
                formBinding.ivConfirmPasswordIcon,
                formBinding.tvConfirmPasswordError
            )
        }

        val email = formBinding.etEmail.text.toString().trim()
        val password = formBinding.etPassword.text.toString()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showFieldError(
                formBinding.emailContainer,
                formBinding.ivEmailIcon,
                formBinding.tvEmailError,
                getString(R.string.auth_invalid_email)
            )
            isValid = false
        }

        if (password.length < 6) {
            showFieldError(
                formBinding.passwordContainer,
                formBinding.ivPasswordIcon,
                formBinding.tvPasswordError,
                getString(R.string.auth_invalid_password)
            )
            isValid = false
        }

        if (checkConfirmPassword) {
            val confirmPassword = formBinding.etConfirmPassword.text.toString()
            if (confirmPassword != password) {
                showFieldError(
                    formBinding.confirmPasswordContainer,
                    formBinding.ivConfirmPasswordIcon,
                    formBinding.tvConfirmPasswordError,
                    getString(R.string.auth_password_not_match)
                )
                isValid = false
            }
        }
        return isValid
    }

    protected fun setLoading(formBinding: LayoutAuthFormBinding, isLoading: Boolean) {
        formBinding.progressPrimary.isVisible = isLoading
        formBinding.btnPrimary.text = if (isLoading) "" else formBinding.btnPrimary.tag?.toString()
            ?: formBinding.btnPrimary.text.toString()
        formBinding.btnPrimary.isEnabled = !isLoading
        formBinding.etEmail.isEnabled = !isLoading
        formBinding.etPassword.isEnabled = !isLoading
        formBinding.ivTogglePassword.isEnabled = !isLoading
        formBinding.googleContainer.isEnabled = !isLoading
        if (formBinding.confirmPasswordContainer.isVisible) {
            formBinding.etConfirmPassword.isEnabled = !isLoading
            formBinding.ivToggleConfirmPassword.isEnabled = !isLoading
        }
    }

    protected fun setGoogleLoading(formBinding: LayoutAuthFormBinding, isLoading: Boolean) {
        formBinding.progressGoogle.isVisible = isLoading
        formBinding.googleContent.alpha = if (isLoading) 0f else 1f
        formBinding.googleContainer.isEnabled = !isLoading
    }

    protected fun cachePrimaryLabel(formBinding: LayoutAuthFormBinding) {
        formBinding.btnPrimary.tag = formBinding.btnPrimary.text
    }

    protected fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    protected fun gotoRegister() {
        startActivity(Intent(this, RegisterActivity::class.java))
    }

    protected fun gotoLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    protected fun attachFieldState(
        editText: EditText,
        iconView: ImageView,
        container: android.view.View,
        @DrawableRes normalBackground: Int,
        @DrawableRes focusedBackground: Int
    ) {
        editText.setOnFocusChangeListener { _, hasFocus ->
            container.setBackgroundResource(if (hasFocus) focusedBackground else normalBackground)
            iconView.imageTintList =
                android.content.res.ColorStateList.valueOf(
                    getColor(if (hasFocus) R.color.auth_gradient_end else R.color.auth_icon_neutral)
                )
        }
    }

    protected fun togglePasswordField(editText: EditText, iconView: ImageView) {
        val selection = editText.selectionStart
        val hidden = editText.transformationMethod != null
        editText.transformationMethod =
            if (hidden) null else android.text.method.PasswordTransformationMethod.getInstance()
        iconView.setImageResource(if (hidden) R.drawable.ic_auth_eye_off else R.drawable.ic_auth_eye)
        editText.setSelection(selection.coerceAtLeast(0))
    }

    private fun clearFieldError(
        container: android.view.View,
        iconView: ImageView,
        errorView: TextView
    ) {
        container.setBackgroundResource(R.drawable.bg_auth_input_normal)
        iconView.imageTintList =
            android.content.res.ColorStateList.valueOf(getColor(R.color.auth_icon_neutral))
        errorView.isVisible = false
    }

    protected fun showFieldError(
        container: android.view.View,
        iconView: ImageView,
        errorView: TextView,
        message: String
    ) {
        container.setBackgroundResource(R.drawable.bg_auth_input_error)
        iconView.imageTintList =
            android.content.res.ColorStateList.valueOf(getColor(R.color.auth_error))
        errorView.text = message
        errorView.isVisible = true
    }
}
