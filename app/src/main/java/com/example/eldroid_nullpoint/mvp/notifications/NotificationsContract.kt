package com.example.eldroid_nullpoint.mvp.notifications

import com.example.eldroid_nullpoint.model.AppNotification

interface NotificationsContract {

    interface View {
        fun showLoading()
        fun showContent(isEmpty: Boolean)
        fun showError()
        fun renderNotifications(notifications: List<AppNotification>)
        fun showMarkAllReadButton(visible: Boolean)
    }

    interface Presenter {
        fun onStart(uid: String)
        fun onStop()
        fun onRetry()
        fun onNotificationClicked(notification: AppNotification)
        fun onMarkAllReadClicked()
        fun detach()
    }
}
