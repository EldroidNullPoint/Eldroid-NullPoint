package com.example.eldroid_nullpoint.forgotpassword

import com.example.eldroid_nullpoint.base.BaseView

/**
 * Forgot Password screen: the user enters their email and receives a reset link.
 * The Activity/layout for this screen is added in the UI pass; the presenter is
 * complete and unit-tested already.
 */
interface ForgotPasswordContract {

    interface View : BaseView {
        fun clearFieldErrors()
        fun showEmailError(message: String)
        /** Switch to the "check your inbox" state. */
        fun showResetEmailSent(email: String)
        fun navigateToLogin()
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()

        fun onSendResetClicked(email: String)
        fun onBackToLoginClicked()
    }
}
