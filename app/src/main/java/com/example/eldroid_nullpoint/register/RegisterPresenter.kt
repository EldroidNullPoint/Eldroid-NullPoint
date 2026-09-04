package com.example.eldroid_nullpoint.register

import com.example.eldroid_nullpoint.base.BasePresenter
import com.example.eldroid_nullpoint.data.AuthRepository
import com.example.eldroid_nullpoint.util.Validators

class RegisterPresenter(
    private val repository: AuthRepository
) : BasePresenter<RegisterContract.View>(), RegisterContract.Presenter {

    override fun onRegisterClicked(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        val view = view ?: return
        view.clearFieldErrors()

        val cleanFirst = firstName.trim().replace(WHITESPACE, " ")
        val cleanLast = lastName.trim().replace(WHITESPACE, " ")
        val cleanEmail = email.trim()

        // Every field is validated so the user sees all problems at once.
        val errors = listOfNotNull(
            Validators.nameError(cleanFirst, "First name")?.also(view::showFirstNameError),
            Validators.nameError(cleanLast, "Last name")?.also(view::showLastNameError),
            Validators.emailError(cleanEmail)?.also(view::showEmailError),
            Validators.passwordError(password, cleanEmail, cleanFirst, cleanLast)
                ?.also(view::showPasswordError),
            Validators.confirmPasswordError(password, confirmPassword)
                ?.also(view::showConfirmPasswordError)
        )
        if (errors.isNotEmpty()) return

        view.showLoading()
        repository.register(cleanFirst, cleanLast, cleanEmail, password) { result ->
            this.view?.hideLoading()
            result
                .onSuccess { this.view?.navigateToHome() }
                .onFailure { this.view?.showMessage(it.message ?: "Sign up failed. Please try again.") }
        }
    }

    override fun onGoogleSignUpClicked() {
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

    override fun onLoginClicked() {
        view?.navigateToLogin()
    }

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}
