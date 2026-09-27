package com.example.eldroid_nullpoint.mvp.changepassword

interface ChangePasswordContract {

    interface View {
        fun showLoading(loading: Boolean)
        fun showCurrentPasswordError(message: String)
        fun showNewPasswordError(message: String)
        fun showConfirmPasswordError(message: String)
        fun clearErrors()
        fun onPasswordChangedSuccess()
        fun showToast(message: String)
    }

    interface Presenter {
        fun onUpdatePasswordClicked(
            currentPassword: String,
            newPassword: String,
            confirmPassword: String
        )
        fun detach()
    }
}
