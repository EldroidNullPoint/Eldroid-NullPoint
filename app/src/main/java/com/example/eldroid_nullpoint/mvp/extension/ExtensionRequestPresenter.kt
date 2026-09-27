package com.example.eldroid_nullpoint.mvp.extension

import android.util.Log
import com.example.eldroid_nullpoint.model.ExtensionRequest
import com.example.eldroid_nullpoint.util.SmartDockRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Presenter for the Extension Request screen.
 *
 * Spec §15:
 * - Shows the borrower the current extension request status for an active loan.
 * - Allows submitting a new request if none is pending.
 * - Android must never directly edit `dueAt`. It only creates this request document.
 * - A pending request does not change the current due time.
 */
class ExtensionRequestPresenter(
    private var view: ExtensionRequestContract.View?,
    private val equipmentId: String,
    private val equipmentName: String,
    private val transactionId: String,
    private val currentDueAt: Long,
    private val uid: String,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ExtensionRequestContract.Presenter {

    companion object {
        private const val TAG = "ExtensionPresenter"
    }

    private var listener: ListenerRegistration? = null

    override fun onStart() {
        view?.showEquipmentInfo(equipmentName, currentDueAt)
        listenToExistingRequest()
    }

    private fun listenToExistingRequest() {
        view?.showLoading(true)
        listener = firestore.collection(ExtensionRequest.COLLECTION)
            .whereEqualTo("uid", uid)
            .whereEqualTo("equipmentId", equipmentId)
            .whereEqualTo("status", ExtensionRequest.STATUS_PENDING)
            .addSnapshotListener { snapshot, error ->
                view?.showLoading(false)
                if (error != null) {
                    Log.w(TAG, "Extension request listener error", error)
                    return@addSnapshotListener
                }
                val pending = snapshot?.documents
                    ?.map { ExtensionRequest.from(it) }
                    ?.firstOrNull()
                view?.renderExistingRequest(pending)
            }
    }

    override fun onSubmitClicked(requestedMinutes: Int, reason: String) {
        if (reason.isBlank()) {
            view?.showError(
                com.example.eldroid_nullpoint.util.StringRes.extensionReasonRequired
            )
            return
        }

        val now = System.currentTimeMillis()
        val requestId = "${uid}_${equipmentId}_${now}_extension"

        val request = ExtensionRequest(
            id = requestId,
            uid = uid,
            transactionId = transactionId,
            equipmentId = equipmentId,
            equipmentName = equipmentName,
            requestedMinutes = requestedMinutes,
            reason = reason.trim(),
            requestedAt = now,
            status = ExtensionRequest.STATUS_PENDING,
            originalDueAt = currentDueAt
        )

        view?.showSubmitting(true)
        SmartDockRepository.submitExtensionRequest(request)
            .addOnSuccessListener {
                Log.d(TAG, "Extension request submitted: $requestId")
                view?.showSubmitting(false)
                // Listener will pick up the new pending request automatically
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Extension request failed", e)
                view?.showSubmitting(false)
                view?.showError("Failed to submit request. Please try again.")
            }
    }

    override fun detach() {
        listener?.remove()
        listener = null
        view = null
    }
}
