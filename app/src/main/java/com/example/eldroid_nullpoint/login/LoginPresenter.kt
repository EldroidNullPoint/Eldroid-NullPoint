package com.example.eldroid_nullpoint.login

import com.example.eldroid_nullpoint.base.BasePresenter
import com.example.eldroid_nullpoint.data.AuthRepository
import com.example.eldroid_nullpoint.util.Validators

class LoginPresenter(
    private val repository: AuthRepository
) : BasePresenter<LoginContract.View>(), LoginContract.Presenter {

    override fun onLoginClicked(email: String, password: String) {
        val view = view ?: return
        view.clearFieldErrors()

        val emailError = Validators.emailError(email)
        val passwordError = Validators.loginPasswordError(password)
        emailError?.let(view::showEmailError)
        passwordError?.let(view::showPasswordError)
        if (emailError != null || passwordError != null) return

        view.showLoading()
        repository.login(email.trim(), password) { result ->
            this.view?.hideLoading()
            result
                .onSuccess { this.view?.navigateToHome() }
                .onFailure { this.view?.showMessage(it.message ?: GENERIC_LOGIN_ERROR) }
        }
    }

    override fun onGoogleSignInClicked() {
        view?.showLoading()
        view?.launchGoogleSignIn()
    }

    override fun onGoogleSignInResult(idToken: String?) {
        if (idToken.isNullOrBlank()) {
            view?.hideLoading()
            view?.showMessage("Google sign-in failed: no ID token was returned.")
            return
        }
        repository.loginWithGoogle(idToken) { result ->
            view?.hideLoading()
            result
                .onSuccess { view?.navigateToHome() }
                .onFailure { view?.showMessage(it.message ?: "Google sign-in failed.") }
        }
    }

    override fun onGoogleSignInCancelled() {
        view?.hideLoading()
    }

    override fun onGoogleSignInFailed(reason: String?) {
        view?.hideLoading()
        val detail = reason?.takeIf { it.isNotBlank() }
        view?.showMessage(if (detail != null) "Google sign-in failed: $detail" else "Google sign-in failed.")
    }

    /** Hands off to the Forgot Password screen; the email typed here is carried over as a convenience. */
    override fun onForgotPasswordClicked(email: String) {
        view?.navigateToForgotPassword(email.trim())
    }

    override fun onRegisterClicked() {
        view?.navigateToRegister()
    }

    private companion object {
        const val GENERIC_LOGIN_ERROR = "Login failed. Please try again."
    }
}
