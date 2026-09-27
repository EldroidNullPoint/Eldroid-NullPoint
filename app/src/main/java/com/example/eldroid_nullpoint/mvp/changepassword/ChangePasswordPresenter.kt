package com.example.eldroid_nullpoint.mvp.changepassword

import com.example.eldroid_nullpoint.util.Validators
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class ChangePasswordPresenter(
    private var view: ChangePasswordContract.View?,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ChangePasswordContract.Presenter {

    override fun onUpdatePasswordClicked(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ) {
        view?.clearErrors()
        var isValid = true

        if (currentPassword.isBlank()) {
            view?.showCurrentPasswordError("Current password is required")
            isValid = false
        }

        val strengthError = Validators.passwordStrengthError(newPassword)
        if (strengthError != null) {
            view?.showNewPasswordError(strengthError)
            isValid = false
        } else if (newPassword == currentPassword) {
            view?.showNewPasswordError("New password must differ from the current one")
            isValid = false
        }

        if (confirmPassword.isBlank()) {
            view?.showConfirmPasswordError("Please confirm your new password")
            isValid = false
        } else if (newPassword != confirmPassword) {
            view?.showConfirmPasswordError("Passwords do not match")
            isValid = false
        }

        if (!isValid) return

        val user = auth.currentUser
        val email = user?.email
        if (user == null || email.isNullOrBlank()) {
            view?.showToast("Unable to change password for this account type")
            return
        }

        view?.showLoading(true)
        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (!reauthTask.isSuccessful) {
                    view?.showLoading(false)
                    val msg = when (reauthTask.exception) {
                        is FirebaseAuthInvalidUserException,
                        is FirebaseAuthInvalidCredentialsException ->
                            "Incorrect current password"
                        else -> reauthTask.exception?.message ?: "Re-authentication failed"
                    }
                    view?.showCurrentPasswordError(msg)
                    return@addOnCompleteListener
                }

                user.updatePassword(newPassword)
                    .addOnCompleteListener { updateTask ->
                        view?.showLoading(false)
                        if (updateTask.isSuccessful) {
                            view?.onPasswordChangedSuccess()
                        } else {
                            val msg = updateTask.exception?.message ?: "Failed to update password"
                            view?.showNewPasswordError(msg)
                        }
                    }
            }
    }

    override fun detach() {
        view = null
    }
}
