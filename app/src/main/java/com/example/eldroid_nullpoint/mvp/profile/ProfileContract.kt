package com.example.eldroid_nullpoint.mvp.profile

import com.example.eldroid_nullpoint.model.User

interface ProfileContract {

    interface View {
        fun showLoading(loading: Boolean)
        fun showSubmitting(submitting: Boolean)
        fun populateFields(user: User)
        fun renderName(firstName: String, lastName: String)
        fun renderRfidStatus(registered: Boolean)
        /** Spec §4 — show the user-friendly RFID status label (not issued/active/disabled/lost). */
        fun renderRfidStatusLabel(rfidStatus: String)
        fun showAccountStatusLabel(accountStatus: String)
        fun showFirstNameError(message: String)
        fun showLastNameError(message: String)
        fun showRfidError(message: String)
        fun clearErrors()
        fun showToast(message: String)
        fun showChangePasswordRow(visible: Boolean)
        fun setProviderLabel(isGoogle: Boolean)
        fun setEmail(email: String)
        fun showRfidWarning(visible: Boolean)
    }

    interface Presenter {
        fun onViewCreated(uid: String, displayName: String?, isGoogle: Boolean)
        fun onSaveClicked(firstName: String, lastName: String, rfidCardUid: String, notificationsEnabled: Boolean)
        fun onLogoutConfirmed()
        fun detach()
    }
}
