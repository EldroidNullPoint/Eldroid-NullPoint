package com.example.eldroid_nullpoint.mvp.signup

interface SignupContract {

    interface View {
        fun showLoading(loading: Boolean)
        fun showFirstNameError(message: String)
        fun showLastNameError(message: String)
        fun showEmailError(message: String)
        fun showPasswordError(message: String)
        fun showConfirmPasswordError(message: String)
        fun clearErrors()
        fun navigateToHome()
        fun showToast(message: String)
    }

    interface Presenter {
        fun onSignupClicked(
            firstName: String,
            lastName: String,
            email: String,
            password: String,
            confirmPassword: String
        )
        fun onGoogleTokenReceived(idToken: String)
        fun detach()
    }
}
