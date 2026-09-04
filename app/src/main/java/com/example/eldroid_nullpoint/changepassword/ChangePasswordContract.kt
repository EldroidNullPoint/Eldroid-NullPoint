package com.example.eldroid_nullpoint.changepassword

import com.example.eldroid_nullpoint.base.BaseView

/**
 * Change Password screen for signed-in email/password accounts.
 * The Activity/layout for this screen is added in the UI pass; the presenter is
 * complete and unit-tested already.
 */
interface ChangePasswordContract {

    interface View : BaseView {
        fun clearFieldErrors()
        fun showCurrentPasswordError(message: String)
        fun showNewPasswordError(message: String)
        fun showConfirmPasswordError(message: String)
        /** Shown (with the form disabled) for Google accounts, which have no password to change. */
        fun showPasswordChangeUnavailable(message: String)
        fun showPasswordChanged()
        fun navigateToLogin()
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()

        /** Call once the view is ready; decides whether the form can be used at all. */
        fun start()

        fun onChangePasswordClicked(
            currentPassword: String,
            newPassword: String,
            confirmPassword: String
        )
    }
}
