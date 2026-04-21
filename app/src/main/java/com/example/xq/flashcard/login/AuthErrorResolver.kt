package com.example.xq.flashcard.ui.login

import android.content.Context
import androidx.annotation.StringRes
import com.example.xq.flashcard.R
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException

enum class AuthAction(@StringRes val fallbackRes: Int) {
    SIGN_IN(R.string.auth_error_sign_in),
    REGISTER(R.string.auth_error_register),
    RESET(R.string.auth_error_reset),
    GOOGLE(R.string.auth_error_google)
}

object AuthErrorResolver {

    fun resolve(context: Context, throwable: Throwable, action: AuthAction): String {
        return when (throwable) {
            is FirebaseAuthInvalidCredentialsException -> resolveInvalidCredentials(context, throwable, action)
            is FirebaseAuthInvalidUserException -> context.getString(R.string.auth_error_account_not_found)
            is FirebaseAuthUserCollisionException -> context.getString(R.string.auth_error_email_in_use)
            is FirebaseNetworkException -> context.getString(R.string.auth_error_network)
            is FirebaseTooManyRequestsException -> context.getString(R.string.auth_error_too_many_requests)
            is FirebaseAuthException -> resolveByErrorCode(context, throwable.errorCode, action)
            else -> context.getString(action.fallbackRes)
        }
    }

    private fun resolveInvalidCredentials(
        context: Context,
        throwable: FirebaseAuthInvalidCredentialsException,
        action: AuthAction
    ): String {
        return when (throwable.errorCode) {
            "ERROR_INVALID_EMAIL" -> context.getString(R.string.auth_invalid_email)
            "ERROR_WRONG_PASSWORD",
            "ERROR_INVALID_CREDENTIAL" -> context.getString(R.string.auth_error_invalid_credentials)

            else -> context.getString(action.fallbackRes)
        }
    }

    private fun resolveByErrorCode(
        context: Context,
        errorCode: String,
        action: AuthAction
    ): String {
        return when (errorCode) {
            "ERROR_EMAIL_ALREADY_IN_USE" -> context.getString(R.string.auth_error_email_in_use)
            "ERROR_USER_NOT_FOUND" -> context.getString(R.string.auth_error_account_not_found)
            else -> context.getString(action.fallbackRes)
        }
    }
}
