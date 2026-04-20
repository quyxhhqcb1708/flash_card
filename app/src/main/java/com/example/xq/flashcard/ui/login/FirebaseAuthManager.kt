package com.example.xq.flashcard.ui.login

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

class FirebaseAuthManager(
    private val context: Context? = null,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    fun isLoggedIn(): Boolean = firebaseAuth.currentUser != null

    fun signIn(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                onError(
                    context?.let { value ->
                        AuthErrorResolver.resolve(value, it, AuthAction.SIGN_IN)
                    } ?: it.localizedMessage.orEmpty()
                )
            }
    }

    fun register(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                onError(
                    context?.let { value ->
                        AuthErrorResolver.resolve(value, it, AuthAction.REGISTER)
                    } ?: it.localizedMessage.orEmpty()
                )
            }
    }
}
