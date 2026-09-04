package com.example.eldroid_nullpoint.register

import com.example.eldroid_nullpoint.base.BaseView

interface RegisterContract {

    interface View : BaseView {
        fun clearFieldErrors()
        fun showFirstNameError(message: String)
        fun showLastNameError(message: String)
        fun showEmailError(message: String)
        fun showPasswordError(message: String)
        fun showConfirmPasswordError(message: String)
        fun launchGoogleSignIn()
        fun navigateToHome()
        fun navigateToLogin()
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()

        fun onRegisterClicked(
            firstName: String,
            lastName: String,
            email: String,
            password: String,
            confirmPassword: String
        )

        fun onGoogleSignUpClicked()
        fun onGoogleSignInResult(idToken: String?)
        fun onGoogleSignInCancelled()
        fun onGoogleSignInFailed(reason: String?)
        fun onLoginClicked()
    }
}
