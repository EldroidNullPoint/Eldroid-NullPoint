package com.example.eldroid_nullpoint.mvp.equipmentdetail

import com.example.eldroid_nullpoint.model.Equipment

interface EquipmentDetailContract {

    interface View {
        fun renderEquipment(equipment: Equipment, currentUid: String)
        fun showBorrowButton(visible: Boolean)
        fun showReturnButton(visible: Boolean)
        /** Show/hide the Request Extension button. Spec §15. */
        fun showExtensionButton(visible: Boolean)
        fun showActionArea(visible: Boolean)
        fun setSubmitting(submitting: Boolean)
        fun showToast(message: String)
        fun showErrorDialog(message: String)
        fun navigateToBorrowConfirmation(receipt: com.example.eldroid_nullpoint.util.SmartDockRepository.BorrowReceipt)
        fun navigateToExtensionRequest(equipmentId: String, equipmentName: String, transactionId: String, currentDueAt: Long)
        fun close()
    }

    interface Presenter {
        fun onStart()
        fun onStop()
        fun onBorrowConfirmed()
        fun onReturnConfirmed()
        fun detach()
    }
}
