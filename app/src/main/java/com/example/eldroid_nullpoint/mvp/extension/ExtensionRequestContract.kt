package com.example.eldroid_nullpoint.mvp.extension

import com.example.eldroid_nullpoint.model.ExtensionRequest

interface ExtensionRequestContract {

    interface View {
        fun showLoading(loading: Boolean)
        fun showSubmitting(submitting: Boolean)
        /** Render current extension request status (pending/approved/rejected/null). */
        fun renderExistingRequest(request: ExtensionRequest?)
        fun showEquipmentInfo(name: String, dueAt: Long)
        fun showSuccess(message: String)
        fun showError(message: String)
        fun close()
    }

    interface Presenter {
        fun onStart()
        fun onSubmitClicked(requestedMinutes: Int, reason: String)
        fun detach()
    }
}
