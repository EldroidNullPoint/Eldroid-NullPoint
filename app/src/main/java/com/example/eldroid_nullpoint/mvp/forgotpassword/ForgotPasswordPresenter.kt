package com.example.eldroid_nullpoint.mvp.forgotpassword

import com.example.eldroid_nullpoint.util.Validators
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordPresenter(
    private var view: ForgotPasswordContract.View?,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ForgotPasswordContract.Presenter {

    override fun onSendResetClicked(email: String) {
        view?.clearEmailError()

        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            view?.showEmailError("Email is required")
            return
        }
        if (!Validators.isValidEmail(trimmedEmail)) {
            view?.showEmailError("Enter a valid email address")
            return
        }

        view?.showLoading(true)
        auth.sendPasswordResetEmail(trimmedEmail)
            .addOnCompleteListener { task ->
                view?.showLoading(false)
                if (task.isSuccessful) {
                    view?.showSuccess(trimmedEmail)
                } else {
                    val msg = when (task.exception) {
                        is com.google.firebase.auth.FirebaseAuthInvalidUserException ->
                            "No account found with that email"
                        is com.google.firebase.FirebaseNetworkException ->
                            "No internet connection. Please check your network."
                        else -> task.exception?.message ?: "Failed to send reset email"
                    }
                    view?.showEmailError(msg)
                }
            }
    }

    override fun clearEmailError() {
        view?.clearEmailError()
    }

    override fun detach() {
        view = null
    }
}
