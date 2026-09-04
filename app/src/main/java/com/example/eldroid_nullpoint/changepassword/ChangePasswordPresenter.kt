package com.example.eldroid_nullpoint.changepassword

import com.example.eldroid_nullpoint.base.BasePresenter
import com.example.eldroid_nullpoint.data.AuthRepository
import com.example.eldroid_nullpoint.util.Validators

class ChangePasswordPresenter(
    private val repository: AuthRepository
) : BasePresenter<ChangePasswordContract.View>(), ChangePasswordContract.Presenter {

    override fun start() {
        val session = repository.currentUser()
        when {
            session == null -> view?.navigateToLogin()
            !session.hasPasswordProvider -> view?.showPasswordChangeUnavailable(UNAVAILABLE_MESSAGE)
        }
    }

    override fun onChangePasswordClicked(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ) {
        val view = view ?: return
        view.clearFieldErrors()

        val session = repository.currentUser()
        if (session == null) {
            view.navigateToLogin()
            return
        }
        if (!session.hasPasswordProvider) {
            view.showPasswordChangeUnavailable(UNAVAILABLE_MESSAGE)
            return
        }

        val currentError = if (currentPassword.isEmpty()) "Current password is required" else null
        val newError = Validators.newPasswordError(
            currentPassword = currentPassword,
            newPassword = newPassword,
            email = session.email,
            firstName = session.displayName
        )
        val confirmError = Validators.confirmPasswordError(newPassword, confirmPassword)

        currentError?.let(view::showCurrentPasswordError)
        newError?.let(view::showNewPasswordError)
        confirmError?.let(view::showConfirmPasswordError)
        if (currentError != null || newError != null || confirmError != null) return

        view.showLoading()
        repository.changePassword(currentPassword, newPassword) { result ->
            this.view?.hideLoading()
            result
                .onSuccess { this.view?.showPasswordChanged() }
                .onFailure { this.view?.showMessage(it.message ?: "Could not change password. Please try again.") }
        }
    }

    companion object {
        const val UNAVAILABLE_MESSAGE =
            "Password change is only available for accounts that signed up with email and password."
    }
}
