package com.example.eldroid_nullpoint.splash

interface SplashContract {

    interface View {
        fun navigateToHome()
        fun navigateToLogin()
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()

        /** Routes to Home when a session exists, otherwise to Login. */
        fun checkSession()
    }
}
