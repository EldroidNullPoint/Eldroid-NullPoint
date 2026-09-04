package com.example.eldroid_nullpoint.forgotpassword

import com.example.eldroid_nullpoint.base.BasePresenter
import com.example.eldroid_nullpoint.data.AuthRepository
import com.example.eldroid_nullpoint.util.Validators

class ForgotPasswordPresenter(
    private val repository: AuthRepository
) : BasePresenter<ForgotPasswordContract.View>(), ForgotPasswordContract.Presenter {

    override fun onSendResetClicked(email: String) {
        val view = view ?: return
        view.clearFieldErrors()

        val emailError = Validators.emailError(email)
        if (emailError != null) {
            view.showEmailError(emailError)
            return
        }

        val trimmed = email.trim()
        view.showLoading()
        repository.sendPasswordReset(trimmed) { result ->
            this.view?.hideLoading()
            result
                .onSuccess { this.view?.showResetEmailSent(trimmed) }
                .onFailure { this.view?.showMessage(it.message ?: "Could not send reset email. Please try again.") }
        }
    }

    override fun onBackToLoginClicked() {
        view?.navigateToLogin()
    }
}
