package com.example.eldroid_nullpoint.model

import com.google.firebase.firestore.DocumentSnapshot

/**
 * An extension request submitted by a borrower for an active loan.
 * Stored in `extensionRequests/{requestId}`.
 *
 * Spec §15 — Android submits, admin approves/rejects via Web Admin,
 * Android reads the resulting status.
 *
 * Android must never directly edit `dueAt`. It only creates this document
 * and reads the decision from it.
 */
data class ExtensionRequest(
    val id: String = "",
    /** Firebase Auth UID of the borrower making the request. */
    val uid: String = "",
    /** ID of the active borrow transaction this extends. */
    val transactionId: String = "",
    /** ID of the equipment being borrowed. */
    val equipmentId: String = "",
    /** Human-readable equipment name, for display. */
    val equipmentName: String = "",
    /** Requested additional minutes (e.g. 30, 60). */
    val requestedMinutes: Int = 0,
    /** Borrower-supplied reason. */
    val reason: String = "",
    /** Unix epoch millis when the request was submitted. */
    val requestedAt: Long = 0L,
    /**
     * Decision status: "pending", "approved", "rejected", "cancelled".
     * Set by admin via Web Admin.
     */
    val status: String = STATUS_PENDING,
    /** Admin who reviewed; empty until reviewed. */
    val reviewedBy: String = "",
    /** Unix epoch millis when reviewed; 0 until reviewed. */
    val reviewedAt: Long = 0L,
    /** Admin's reason for rejection (optional for approval). */
    val decisionReason: String = "",
    /**
     * New due time if approved. Unix epoch millis; 0 until approved.
     * Set by backend/admin on approval.
     */
    val resultingDueAt: Long = 0L,
    /** The due time at the moment the request was submitted. */
    val originalDueAt: Long = 0L
) {
    val isPending: Boolean get() = status == STATUS_PENDING
    val isApproved: Boolean get() = status == STATUS_APPROVED
    val isRejected: Boolean get() = status == STATUS_REJECTED

    fun toMap(): Map<String, Any> = mapOf(
        "uid"              to uid,
        "transactionId"    to transactionId,
        "equipmentId"      to equipmentId,
        "equipmentName"    to equipmentName,
        "requestedMinutes" to requestedMinutes,
        "reason"           to reason,
        "requestedAt"      to requestedAt,
        "status"           to status,
        "reviewedBy"       to reviewedBy,
        "reviewedAt"       to reviewedAt,
        "decisionReason"   to decisionReason,
        "resultingDueAt"   to resultingDueAt,
        "originalDueAt"    to originalDueAt
    )

    companion object {
        const val COLLECTION = "extensionRequests"
        const val STATUS_PENDING   = "pending"
        const val STATUS_APPROVED  = "approved"
        const val STATUS_REJECTED  = "rejected"
        const val STATUS_CANCELLED = "cancelled"

        fun from(doc: DocumentSnapshot) = ExtensionRequest(
            id               = doc.id,
            uid              = doc.getString("uid").orEmpty(),
            transactionId    = doc.getString("transactionId").orEmpty(),
            equipmentId      = doc.getString("equipmentId").orEmpty(),
            equipmentName    = doc.getString("equipmentName").orEmpty(),
            requestedMinutes = (doc.get("requestedMinutes") as? Number)?.toInt() ?: 0,
            reason           = doc.getString("reason").orEmpty(),
            requestedAt      = (doc.get("requestedAt") as? Number)?.toLong() ?: 0L,
            status           = doc.getString("status") ?: STATUS_PENDING,
            reviewedBy       = doc.getString("reviewedBy").orEmpty(),
            reviewedAt       = (doc.get("reviewedAt") as? Number)?.toLong() ?: 0L,
            decisionReason   = doc.getString("decisionReason").orEmpty(),
            resultingDueAt   = (doc.get("resultingDueAt") as? Number)?.toLong() ?: 0L,
            originalDueAt    = (doc.get("originalDueAt") as? Number)?.toLong() ?: 0L
        )
    }
}
