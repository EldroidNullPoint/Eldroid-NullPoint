package com.example.eldroid_nullpoint.mvp.activityhistory

import com.example.eldroid_nullpoint.model.Transaction
import com.example.eldroid_nullpoint.util.DemoData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ActivityHistoryPresenter(
    private var view: ActivityHistoryContract.View?,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ActivityHistoryContract.Presenter {

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

        listener = firestore.collection(Transaction.COLLECTION)
            .whereEqualTo("uid", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (DemoData.ENABLED) {
                        render(DemoData.transactions(currentUid, ""), isDemo = true)
                    } else {
                        view?.showError()
                    }
                    return@addSnapshotListener
                }
                val transactions = snapshot?.documents
                    ?.map { Transaction.from(it) }
                    ?.filter { it.uid.isNotBlank() }
                    ?.sortedByDescending { it.timestamp }
                    .orEmpty()

                if (transactions.isEmpty() && DemoData.ENABLED) {
                    render(DemoData.transactions(currentUid, ""), isDemo = true)
                } else {
                    render(transactions, isDemo = false)
                }
            }
    }

    private fun render(transactions: List<Transaction>, isDemo: Boolean) {
        view?.renderTransactions(transactions, isDemo)
        view?.showContent(transactions.isEmpty())
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
