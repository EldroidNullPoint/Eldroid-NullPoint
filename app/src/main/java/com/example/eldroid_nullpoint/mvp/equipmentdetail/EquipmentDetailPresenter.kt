package com.example.eldroid_nullpoint.mvp.equipmentdetail

import com.example.eldroid_nullpoint.model.Equipment
import com.example.eldroid_nullpoint.util.DemoData
import com.example.eldroid_nullpoint.util.LoanPolicy
import com.example.eldroid_nullpoint.util.SmartDockRepository
import com.example.eldroid_nullpoint.work.DueCheckWorker
import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class EquipmentDetailPresenter(
    private var view: EquipmentDetailContract.View?,
    private val context: Context,
    private val equipmentId: String,
    private val currentUid: String,
    private var borrowerName: String,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : EquipmentDetailContract.Presenter {

    private var listener: ListenerRegistration? = null
    private var currentEquipment: Equipment? = null
    private var isSubmitting = false

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
                    view?.showToast("Failed to load equipment details")
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) {
                    view?.showToast("Equipment not found")
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
        setSubmitting(true)

        android.util.Log.d("DetailPresenter", "Attempting borrow: id=${equipment.id} uid=$currentUid name=$borrowerName")
        com.example.eldroid_nullpoint.util.DebugBroadcaster.broadcast("Borrow attempt: ${equipment.name}")

        SmartDockRepository.borrow(equipment.id, currentUid, borrowerName)
            .addOnSuccessListener { receipt ->
                android.util.Log.d("DetailPresenter", "Borrow SUCCESS: ${receipt.equipment.name}")
                com.example.eldroid_nullpoint.util.DebugBroadcaster.broadcast("✅ Borrow SUCCESS: ${receipt.equipment.name}")
                setSubmitting(false)
                DueCheckWorker.runNow(context)
                view?.navigateToBorrowConfirmation(receipt)
            }
            .addOnFailureListener { error ->
                android.util.Log.e("DetailPresenter", "Borrow FAILURE", error)
                val errorMsg = resolveActionError(error)
                com.example.eldroid_nullpoint.util.DebugBroadcaster.broadcast("❌ Borrow FAILED: $errorMsg")
                setSubmitting(false)
                view?.showToast(errorMsg)
            }
    }

    override fun onReturnConfirmed() {
        val equipment = currentEquipment ?: return
        if (isSubmitting) return
        setSubmitting(true)

        android.util.Log.d("DetailPresenter", "Attempting return: id=${equipment.id} uid=$currentUid name=$borrowerName")
        com.example.eldroid_nullpoint.util.DebugBroadcaster.broadcast("Return attempt: ${equipment.name}")

        SmartDockRepository.returnItem(equipment.id, currentUid, borrowerName)
            .addOnSuccessListener { returned ->
                android.util.Log.d("DetailPresenter", "Return SUCCESS: ${returned.name}")
                com.example.eldroid_nullpoint.util.DebugBroadcaster.broadcast("✅ Return SUCCESS: ${returned.name}")
                setSubmitting(false)
                view?.showToast("${returned.name} returned to Box ${returned.boxNumber}")
            }
            .addOnFailureListener { error ->
                android.util.Log.e("DetailPresenter", "Return FAILURE", error)
                val errorMsg = resolveActionError(error)
                com.example.eldroid_nullpoint.util.DebugBroadcaster.broadcast("❌ Return FAILED: $errorMsg")
                setSubmitting(false)
                view?.showToast(errorMsg)
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
    }

    private fun renderActions(equipment: Equipment) {
        if (isSubmitting) return
        val canBorrow = !DemoData.isDemoId(equipment.id) &&
                LoanPolicy.canBorrow(equipment, currentUid) == LoanPolicy.BorrowCheck.OK
        val canReturn = !DemoData.isDemoId(equipment.id) &&
                LoanPolicy.canReturn(equipment, currentUid) == LoanPolicy.ReturnCheck.OK

        view?.showBorrowButton(canBorrow)
        view?.showReturnButton(canReturn)
        view?.showActionArea(canBorrow || canReturn)
    }

    private fun setSubmitting(submitting: Boolean) {
        isSubmitting = submitting
        view?.setSubmitting(submitting)
        currentEquipment?.let { if (!submitting) renderActions(it) }
    }

    private fun resolveActionError(error: Throwable): String {
        val loanError = SmartDockRepository.loanExceptionOf(error)
        // Log full error so Logcat shows the exact cause
        android.util.Log.e("DetailPresenter", "Action error: ${error.javaClass.simpleName}: ${error.message}", error)

        // Build detailed diagnostic message for the user
        val errorType = error.javaClass.simpleName
        val errorMsg = error.message ?: "Unknown error"
        
        val diagnostic = buildString {
            append("❌ ERROR DIAGNOSTIC ❌\n\n")
            append("Error Type: $errorType\n")
            append("Message: $errorMsg\n\n")
            
            when {
                loanError?.borrowCheck == LoanPolicy.BorrowCheck.ALREADY_BORROWED ->
                    append("Reason: Equipment already borrowed")
                loanError?.borrowCheck == LoanPolicy.BorrowCheck.NOT_FOUND ||
                        loanError?.returnCheck == LoanPolicy.ReturnCheck.NOT_FOUND ->
                    append("Reason: Equipment not found in database")
                loanError?.returnCheck == LoanPolicy.ReturnCheck.NOT_YOURS ->
                    append("Reason: You did not borrow this item")
                loanError?.returnCheck == LoanPolicy.ReturnCheck.NOT_BORROWED ->
                    append("Reason: Equipment is not currently borrowed")
                SmartDockRepository.isPermissionDenied(error) ->
                    append("Reason: PERMISSION DENIED by Firestore rules\n\nYour Firestore security rules are blocking this operation. Check Firebase Console > Firestore Database > Rules")
                else ->
                    append("Reason: Unknown/Unexpected error\n\nFull details:\n${error.stackTraceToString().take(500)}")
            }
        }
        
        // Show the full diagnostic in a dialog
        view?.showErrorDialog(diagnostic)
        
        // Return short message for Toast
        return when {
            loanError?.borrowCheck == LoanPolicy.BorrowCheck.ALREADY_BORROWED ->
                "Already borrowed"
            loanError?.borrowCheck == LoanPolicy.BorrowCheck.NOT_FOUND ||
                    loanError?.returnCheck == LoanPolicy.ReturnCheck.NOT_FOUND ->
                "Equipment not found"
            loanError?.returnCheck == LoanPolicy.ReturnCheck.NOT_YOURS ->
                "Not your item"
            loanError?.returnCheck == LoanPolicy.ReturnCheck.NOT_BORROWED ->
                "Not currently borrowed"
            SmartDockRepository.isPermissionDenied(error) ->
                "PERMISSION_DENIED"
            else -> "FAILED: $errorType"
        }
    }

    override fun detach() {
        listener?.remove()
        listener = null
        view = null
    }
}
