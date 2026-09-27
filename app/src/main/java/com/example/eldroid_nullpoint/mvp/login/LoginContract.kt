package com.example.eldroid_nullpoint.mvp.login

interface LoginContract {

    interface View {
        fun showLoading(loading: Boolean)
        fun showEmailError(message: String)
        fun showPasswordError(message: String)
        fun clearErrors()
        fun navigateToHome()
        fun showToast(message: String)
    }

    interface Presenter {
        fun onLoginClicked(email: String, password: String)
        fun onGoogleTokenReceived(idToken: String)
        fun detach()
    }
}
