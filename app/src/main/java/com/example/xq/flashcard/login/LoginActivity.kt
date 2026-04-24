package com.example.xq.flashcard.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.xq.flashcard.R
import com.example.xq.flashcard.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class LoginActivity : AuthFormActivity<ActivityLoginBinding>() {

    companion object {
        private const val EXTRA_FORCE_AUTH = "extra_force_auth"

        fun createIntent(context: Context, forceAuth: Boolean = false): Intent {
            return Intent(context, LoginActivity::class.java).apply {
                putExtra(EXTRA_FORCE_AUTH, forceAuth)
            }
        }
    }

    private val forceAuth by lazy { intent.getBooleanExtra(EXTRA_FORCE_AUTH, false) }

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken.isNullOrBlank()) {
                    setGoogleLoading(binding.authForm, false)
                    Toast.makeText(this, R.string.auth_google_missing_token, Toast.LENGTH_SHORT).show()
                    return@registerForActivityResult
                }
                authManager.signInWithGoogle(
                    idToken = idToken,
                    onSuccess = {
                        setGoogleLoading(binding.authForm, false)
                        navigateToMain()
                    },
                    onError = { message ->
                        setGoogleLoading(binding.authForm, false)
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (_: ApiException) {
                setGoogleLoading(binding.authForm, false)
                Toast.makeText(this, R.string.auth_google_cancelled, Toast.LENGTH_SHORT).show()
            }
        }

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
            switchClick = { gotoRegister() },
            googleClick = { signInWithGoogle() },
            showGoogle = false,
            guestActionClick = { navigateToMainAsGuest() },
            showGuestAction = true
        )
        cachePrimaryLabel(formBinding)
        formBinding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        if (authManager.isLoggedIn()) {
            navigateToMain()
        } else if (!forceAuth && GuestSessionStore.isGuestMode(this)) {
            navigateToMainAsGuest()
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

    private fun signInWithGoogle() {
        val webClientId = getWebClientId()
        if (webClientId.isNullOrBlank()) {
            Toast.makeText(this, R.string.auth_google_not_configured, Toast.LENGTH_LONG).show()
            return
        }
        setGoogleLoading(binding.authForm, true)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(this, gso)
        googleSignInLauncher.launch(client.signInIntent)
    }

    private fun getWebClientId(): String? {
        val resId = resources.getIdentifier("default_web_client_id", "string", packageName)
        if (resId == 0) return null
        return getString(resId).takeIf { it.isNotBlank() }
    }
}
