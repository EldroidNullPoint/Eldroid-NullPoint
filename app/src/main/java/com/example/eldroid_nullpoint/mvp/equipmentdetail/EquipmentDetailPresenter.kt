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

        // Guard: don't register a second listener if one is already active
        // (can happen when the confirm dialog dismisses and the Activity resumes).
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
        listener?.remove()
        listener = null
    }

    override fun onBorrowConfirmed() {
        val equipment = currentEquipment ?: return
        if (isSubmitting) return
        setSubmitting(true)

        SmartDockRepository.borrow(equipment.id, currentUid, borrowerName)
            .addOnSuccessListener { receipt ->
                setSubmitting(false)
                DueCheckWorker.runNow(context)
                view?.navigateToBorrowConfirmation(receipt)
            }
            .addOnFailureListener { error ->
                setSubmitting(false)
                view?.showToast(resolveActionError(error))
            }
    }

    override fun onReturnConfirmed() {
        val equipment = currentEquipment ?: return
        if (isSubmitting) return
        setSubmitting(true)

        SmartDockRepository.returnItem(equipment.id, currentUid, borrowerName)
            .addOnSuccessListener { returned ->
                // Clear submitting flag BEFORE showing toast so the snapshot
                // listener that immediately fires sees isSubmitting = false and
                // can correctly re-evaluate action buttons.
                setSubmitting(false)
                view?.showToast("${returned.name} returned to Box ${returned.boxNumber}")
            }
            .addOnFailureListener { error ->
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
        return when {
            loanError?.borrowCheck == LoanPolicy.BorrowCheck.ALREADY_BORROWED ->
                "This item has already been borrowed"
            loanError?.borrowCheck == LoanPolicy.BorrowCheck.NOT_FOUND ||
                    loanError?.returnCheck == LoanPolicy.ReturnCheck.NOT_FOUND ->
                "Equipment not found"
            loanError?.returnCheck == LoanPolicy.ReturnCheck.NOT_YOURS ->
                "You cannot return an item you didn't borrow"
            loanError?.returnCheck == LoanPolicy.ReturnCheck.NOT_BORROWED ->
                "This item is not currently borrowed"
            SmartDockRepository.isPermissionDenied(error) ->
                "Permission denied. Contact your administrator."
            else -> "Action failed. Please try again."
        }
    }

    override fun detach() {
        listener?.remove()
        listener = null
        view = null
    }
}
