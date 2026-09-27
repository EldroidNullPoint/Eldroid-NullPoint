package com.example.eldroid_nullpoint.mvp.equipmentlist

import com.example.eldroid_nullpoint.model.Equipment
import com.example.eldroid_nullpoint.util.DemoData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class EquipmentListPresenter(
    private var view: EquipmentListContract.View?,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : EquipmentListContract.Presenter {

    private var listener: ListenerRegistration? = null
    private var currentUid: String = ""

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

    private fun startListening() {
        if (listener != null) return
        view?.showLoading()

        listener = firestore.collection(Equipment.COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (DemoData.ENABLED) {
                        render(DemoData.equipment(currentUid), isDemo = true)
                    } else {
                        view?.showError()
                    }
                    return@addSnapshotListener
                }
                val equipment = snapshot?.documents
                    ?.map { Equipment.from(it) }
                    ?.sortedBy { it.boxNumber }
                    .orEmpty()

                if (equipment.isEmpty() && DemoData.ENABLED) {
                    render(DemoData.equipment(currentUid), isDemo = true)
                } else {
                    render(equipment, isDemo = false)
                }
            }
    }

    private fun render(equipment: List<Equipment>, isDemo: Boolean) {
        view?.renderEquipment(equipment, isDemo)
        view?.showContent(equipment.isEmpty())

        val subtitle = when {
            equipment.isEmpty() -> "All SmartDock boxes"
            isDemo -> "Showing sample data"
            else -> "${equipment.count { !it.isBorrowed }} of ${equipment.size} available"
        }
        view?.updateSubtitle(subtitle)
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
