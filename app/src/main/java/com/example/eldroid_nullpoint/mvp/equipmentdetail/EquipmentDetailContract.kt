package com.example.eldroid_nullpoint.mvp.equipmentdetail

import com.example.eldroid_nullpoint.model.Equipment

interface EquipmentDetailContract {

    interface View {
        fun renderEquipment(equipment: Equipment, currentUid: String)
        fun showBorrowButton(visible: Boolean)
        fun showReturnButton(visible: Boolean)
        fun showActionArea(visible: Boolean)
        fun setSubmitting(submitting: Boolean)
        fun showToast(message: String)
        fun navigateToBorrowConfirmation(receipt: com.example.eldroid_nullpoint.util.SmartDockRepository.BorrowReceipt)
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
