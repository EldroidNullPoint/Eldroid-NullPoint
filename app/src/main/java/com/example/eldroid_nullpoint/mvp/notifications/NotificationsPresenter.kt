package com.example.eldroid_nullpoint.mvp.notifications

import com.example.eldroid_nullpoint.model.AppNotification
import com.example.eldroid_nullpoint.util.SmartDockRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class NotificationsPresenter(
    private var view: NotificationsContract.View?,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val onOpenEquipment: (String) -> Unit
) : NotificationsContract.Presenter {

    private var listener: ListenerRegistration? = null
    private var currentUid: String = ""
    private var unreadIds: List<String> = emptyList()

    override fun onStart(uid: String) {
        currentUid = uid
        startListening()
    }

    override fun onStop() {
        stopListening()
    }

    override fun onRetry() {
        stopListening()
        startListening()
    }

    override fun onNotificationClicked(notification: AppNotification) {
        if (!notification.read) {
            SmartDockRepository.markNotificationRead(notification.id)
        }
        if (notification.equipmentId.isNotBlank()) {
            onOpenEquipment(notification.equipmentId)
        }
    }

    override fun onMarkAllReadClicked() {
        if (unreadIds.isEmpty()) return
        SmartDockRepository.markNotificationsRead(unreadIds)
    }

    private fun startListening() {
        if (listener != null) return
        view?.showLoading()

        listener = firestore.collection(AppNotification.COLLECTION)
            .whereEqualTo("uid", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    view?.showError()
                    return@addSnapshotListener
                }
                val notifications = snapshot?.documents
                    ?.map { AppNotification.from(it) }
                    ?.sortedByDescending { it.createdAt }
                    .orEmpty()

                unreadIds = notifications.filter { !it.read }.map { it.id }
                view?.renderNotifications(notifications)
                view?.showContent(notifications.isEmpty())
                view?.showMarkAllReadButton(unreadIds.isNotEmpty())
            }
    }

    private fun stopListening() {
        listener?.remove()
        listener = null
    }

    override fun detach() {
        stopListening()
        view = null
    }
}
