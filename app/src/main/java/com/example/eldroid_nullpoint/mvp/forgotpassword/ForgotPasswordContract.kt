package com.example.eldroid_nullpoint.mvp.forgotpassword

interface ForgotPasswordContract {

    interface View {
        fun showLoading(loading: Boolean)
        fun showEmailError(message: String)
        fun showSuccess(email: String)
        fun clearEmailError()
    }

    interface Presenter {
        fun onSendResetClicked(email: String)
        fun clearEmailError()
        fun detach()
    }
}
