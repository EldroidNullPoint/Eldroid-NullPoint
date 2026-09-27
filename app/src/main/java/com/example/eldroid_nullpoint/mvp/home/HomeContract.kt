package com.example.eldroid_nullpoint.mvp.home

import com.example.eldroid_nullpoint.model.Equipment
import com.example.eldroid_nullpoint.model.Transaction

interface HomeContract {

    interface View {
        fun showLoading()
        fun showContent()
        fun showError()
        fun updateGreeting(text: String)
        fun showRfidWarning(visible: Boolean)
        fun renderEquipmentList(equipment: List<Equipment>)
        fun renderMyItems(myItems: List<Equipment>)
        fun renderCurrentItem(item: Equipment?)
        fun renderActivity(transactions: List<Transaction>)
        fun updateUnreadBadge(unread: Int)
        fun setSeeding(seeding: Boolean)
        fun showToast(message: String)
        fun showSeedConfirmDialog()
        fun navigateToLogin()
        /** Show or hide the offline banner. Spec §4.5 */
        fun showOfflineBanner(offline: Boolean)
        /**
         * Show account/RFID status messaging. Spec §3, §35.
         * @param accountStatus the raw `accountStatus` field from Firestore
         * @param rfidStatus the raw `rfidStatus` field from Firestore
         */
        fun showAccountStatusBanner(accountStatus: String, rfidStatus: String)
    }

    interface Presenter {
        fun onStart(uid: String)
        fun onStop()
        fun onRefresh()
        fun onSeedConfirmed()
        fun detach()
    }
}
