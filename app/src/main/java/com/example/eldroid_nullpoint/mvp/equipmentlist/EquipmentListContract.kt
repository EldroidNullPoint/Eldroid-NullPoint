package com.example.eldroid_nullpoint.mvp.equipmentlist

import com.example.eldroid_nullpoint.model.Equipment

interface EquipmentListContract {

    interface View {
        fun showLoading()
        fun showContent(isEmpty: Boolean)
        fun showError()
        fun renderEquipment(equipment: List<Equipment>, isDemo: Boolean)
        fun updateSubtitle(text: String)
    }

    interface Presenter {
        fun onStart(uid: String)
        fun onStop()
        fun onRetry()
        fun detach()
    }
}
