package com.example.eldroid_nullpoint.mvp.activityhistory

import com.example.eldroid_nullpoint.model.Transaction

interface ActivityHistoryContract {

    interface View {
        fun showLoading()
        fun showContent(isEmpty: Boolean)
        fun showError()
        fun renderTransactions(transactions: List<Transaction>, isDemo: Boolean)
    }

    interface Presenter {
        fun onStart(uid: String)
        fun onStop()
        fun onRetry()
        fun detach()
    }
}
