package com.example.eldroid_nullpoint.util

import android.content.Context
import com.example.eldroid_nullpoint.R
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

/**
 * Turns Firebase exceptions into short, borrower-friendly sentences.
 *
 * Nothing here ever surfaces a raw exception message or stack trace: those leak
 * implementation detail and read as noise to the person using the app.
 */
object AuthErrors {

    fun messageFor(context: Context, throwable: Throwable?): String {
        val resId = when (throwable) {
            is FirebaseAuthWeakPasswordException -> R.string.auth_error_weak_password
            is FirebaseAuthRecentLoginRequiredException -> R.string.auth_error_requires_recent_login
            is FirebaseAuthUserCollisionException -> R.string.auth_error_email_in_use
            is FirebaseAuthInvalidUserException -> R.string.auth_error_user_not_found
            is FirebaseAuthInvalidCredentialsException -> R.string.auth_error_invalid_credentials
            is FirebaseNetworkException -> R.string.auth_error_network
            is FirebaseTooManyRequestsException -> R.string.auth_error_too_many_requests
            else -> R.string.auth_error_generic
        }
        return context.getString(resId)
    }

    /**
     * Sign-in variant. Firebase intentionally reports "no such account" and
     * "wrong password" with different exception types; both are shown as the same
     * neutral message so the screen cannot be used to probe which emails exist.
     */
    fun signInMessageFor(context: Context, throwable: Throwable?): String = when (throwable) {
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException ->
            context.getString(R.string.auth_error_invalid_credentials)
        else -> messageFor(context, throwable)
    }
}
