package com.example.eldroid_nullpoint.mvp.equipmentdetail

import android.content.Context
import android.util.Log
import com.example.eldroid_nullpoint.model.Equipment
import com.example.eldroid_nullpoint.util.DemoData
import com.example.eldroid_nullpoint.util.LoanPolicy
import com.example.eldroid_nullpoint.util.NetworkMonitor
import com.example.eldroid_nullpoint.util.SmartDockRepository
import com.example.eldroid_nullpoint.work.DueCheckWorker
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration

class EquipmentDetailPresenter(
    private var view: EquipmentDetailContract.View?,
    private val context: Context,
    private val equipmentId: String,
    private val currentUid: String,
    private var borrowerName: String,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : EquipmentDetailContract.Presenter {

    companion object {
        private const val TAG = "EquipDetailPresenter"
    }

    private var listener: ListenerRegistration? = null
    private var currentEquipment: Equipment? = null
    private var isSubmitting = false

    private var currentTransactionId: String = ""

    override fun onStart() {
        if (equipmentId.isBlank()) {
            view?.close()
            return
        }

        // Guard: don't register a second listener if one is already active.
        if (listener != null) return

        if (DemoData.isDemoId(equipmentId)) {
            val demo = DemoData.equipmentById(equipmentId, currentUid)
            if (demo == null) view?.close() else bind(demo)
            return
        }

        listener = firestore.collection(Equipment.COLLECTION).document(equipmentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Snapshot error for $equipmentId", error)
                    view?.showToast(context.getString(com.example.eldroid_nullpoint.R.string.error_action_failed))
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) {
                    view?.showToast(context.getString(com.example.eldroid_nullpoint.R.string.detail_not_found))
                    view?.close()
                    return@addSnapshotListener
                }
                bind(Equipment.from(snapshot))
            }
    }

    override fun onStop() {
        // Do NOT remove the listener on stop — dialogs (confirm borrow/return)
        // trigger onStop/onStart on the Activity. Removing the listener here
        // kills the real-time update and causes a white screen after a return.
        // The listener is only cleaned up in detach() when the screen fully closes.
    }

    override fun onBorrowConfirmed() {
        val equipment = currentEquipment ?: return
        if (isSubmitting) return

        // Spec §4.5 — disable live-dependent actions when offline
        if (!NetworkMonitor.isOnline(context)) {
            view?.showToast(context.getString(com.example.eldroid_nullpoint.R.string.error_offline_action))
            return
        }

        setSubmitting(true)
        Log.d(TAG, "Borrow attempt: id=${equipment.id} uid=$currentUid name=$borrowerName")

        SmartDockRepository.borrow(equipment.id, currentUid, borrowerName)
            .addOnSuccessListener { receipt ->
                Log.d(TAG, "Borrow success: ${receipt.equipment.name}")
                setSubmitting(false)
                DueCheckWorker.runNow(context)
                view?.navigateToBorrowConfirmation(receipt)
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Borrow failed", error)
                setSubmitting(false)
                view?.showToast(resolveActionError(error))
            }
    }

    override fun onReturnConfirmed() {
        val equipment = currentEquipment ?: return
        if (isSubmitting) return

        // Spec §4.5 — disable live-dependent actions when offline
        if (!NetworkMonitor.isOnline(context)) {
            view?.showToast(context.getString(com.example.eldroid_nullpoint.R.string.error_offline_action))
            return
        }

        setSubmitting(true)
        Log.d(TAG, "Return attempt: id=${equipment.id} uid=$currentUid name=$borrowerName")

        SmartDockRepository.returnItem(equipment.id, currentUid, borrowerName)
            .addOnSuccessListener { returned ->
                Log.d(TAG, "Return success: ${returned.name}")
                setSubmitting(false)
                view?.showToast(
                    context.getString(com.example.eldroid_nullpoint.R.string.return_success,
                        returned.name, returned.boxNumber)
                )
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Return failed", error)
                setSubmitting(false)
                view?.showToast(resolveActionError(error))
            }
    }

    fun loadBorrowerName() {
        if (currentUid.isBlank()) return
        firestore.collection("users").document(currentUid).get()
            .addOnSuccessListener { snapshot ->
                val name = listOf(
                    snapshot.getString("firstName").orEmpty().trim(),
                    snapshot.getString("lastName").orEmpty().trim()
                ).filter { it.isNotBlank() }.joinToString(" ")
                if (name.isNotBlank()) borrowerName = name
            }
    }

    private fun bind(equipment: Equipment) {
        currentEquipment = equipment
        view?.renderEquipment(equipment, currentUid)
        renderActions(equipment)
        // Fetch the active transaction ID so the extension screen can reference it
        if (equipment.isBorrowedBy(currentUid) && currentTransactionId.isBlank()) {
            loadActiveTransactionId(equipment.id)
        }
    }

    private fun loadActiveTransactionId(equipmentId: String) {
        firestore.collection(com.example.eldroid_nullpoint.model.Transaction.COLLECTION)
            .whereEqualTo("uid", currentUid)
            .whereEqualTo("equipmentId", equipmentId)
            .whereEqualTo("type", com.example.eldroid_nullpoint.model.Transaction.TYPE_BORROW)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                currentTransactionId = snapshot.documents.firstOrNull()?.id.orEmpty()
            }
    }

    private fun renderActions(equipment: Equipment) {
        if (isSubmitting) return

        val canBorrow = !DemoData.isDemoId(equipment.id) &&
                LoanPolicy.canBorrow(equipment, currentUid) == LoanPolicy.BorrowCheck.OK
        val canReturn = !DemoData.isDemoId(equipment.id) &&
                LoanPolicy.canReturn(equipment, currentUid) == LoanPolicy.ReturnCheck.OK

        // Spec §15 — show Request Extension when borrower owns the active loan
        // and it is not a demo item. The extension screen handles the
        // pending-request check itself.
        val canRequestExtension = !DemoData.isDemoId(equipment.id) &&
                equipment.isBorrowedBy(currentUid) && equipment.isBorrowed

        view?.showBorrowButton(canBorrow)
        view?.showReturnButton(canReturn)
        view?.showExtensionButton(canRequestExtension)
        view?.showActionArea(canBorrow || canReturn || canRequestExtension)
    }

    fun onExtensionClicked() {
        val equipment = currentEquipment ?: return
        view?.navigateToExtensionRequest(
            equipmentId   = equipment.id,
            equipmentName = equipment.name,
            transactionId = currentTransactionId,
            currentDueAt  = equipment.dueAt
        )
    }

    private fun setSubmitting(submitting: Boolean) {
        isSubmitting = submitting
        view?.setSubmitting(submitting)
        currentEquipment?.let { if (!submitting) renderActions(it) }
    }

    /**
     * Maps every known failure mode to a user-friendly string.
     * Spec §15.2 — never expose raw internal error details to the borrower.
     */
    private fun resolveActionError(error: Throwable): String {
        val loanError = SmartDockRepository.loanExceptionOf(error)
        Log.e(TAG, "Action error: ${error.javaClass.simpleName}: ${error.message}", error)

        return context.getString(
            when {
                loanError?.borrowCheck == LoanPolicy.BorrowCheck.ALREADY_BORROWED ->
                    com.example.eldroid_nullpoint.R.string.error_already_borrowed
                loanError?.borrowCheck == LoanPolicy.BorrowCheck.UNAVAILABLE ->
                    com.example.eldroid_nullpoint.R.string.error_contact_admin
                loanError?.returnCheck == LoanPolicy.ReturnCheck.NOT_YOURS ->
                    com.example.eldroid_nullpoint.R.string.error_not_your_item
                loanError?.returnCheck == LoanPolicy.ReturnCheck.NOT_BORROWED ->
                    com.example.eldroid_nullpoint.R.string.error_not_borrowed
                SmartDockRepository.isPermissionDenied(error) ->
                    com.example.eldroid_nullpoint.R.string.error_permission_denied
                isOfflineError(error) ->
                    com.example.eldroid_nullpoint.R.string.error_offline_action
                else ->
                    com.example.eldroid_nullpoint.R.string.error_action_failed
            }
        )
    }

    private fun isOfflineError(error: Throwable): Boolean {
        val code = (error as? FirebaseFirestoreException)?.code
        return code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
    }

    override fun detach() {
        listener?.remove()
        listener = null
        view = null
    }
}
