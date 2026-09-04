package com.example.eldroid_nullpoint.login

import com.example.eldroid_nullpoint.base.BaseView

/**
 * Agreement between the Login screen and its presenter.
 * The View only renders; every decision lives in the Presenter.
 */
interface LoginContract {

    interface View : BaseView {
        fun clearFieldErrors()
        fun showEmailError(message: String)
        fun showPasswordError(message: String)
        fun launchGoogleSignIn()
        fun navigateToHome()
        fun navigateToRegister()
        /** Opens the Forgot Password screen, pre-filling whatever email was typed. */
        fun navigateToForgotPassword(prefilledEmail: String)
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()

        fun onLoginClicked(email: String, password: String)
        fun onGoogleSignInClicked()
        fun onGoogleSignInResult(idToken: String?)
        fun onGoogleSignInCancelled()
        fun onGoogleSignInFailed(reason: String?)
        fun onForgotPasswordClicked(email: String)
        fun onRegisterClicked()
    }
}
