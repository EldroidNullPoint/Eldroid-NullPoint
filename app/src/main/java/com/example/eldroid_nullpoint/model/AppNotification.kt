package com.example.eldroid_nullpoint.model

import com.google.firebase.firestore.DocumentSnapshot

/**
 * One entry in the borrower's in-app inbox: `notifications/{id}`.
 *
 * Written by the app itself - on borrow/return (FR-05 confirmation) and by the
 * background due-date check (FR-06 reminders). Named `AppNotification` so it never
 * collides with `android.app.Notification`.
 */
data class AppNotification(
    val id: String = "",
    val uid: String = "",
    /** One of the TYPE_* constants. */
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val equipmentId: String = "",
    val equipmentName: String = "",
    val boxNumber: Int = 0,
    val read: Boolean = false,
    /** Unix epoch millis. */
    val createdAt: Long = 0L
) {
    fun toMap(): Map<String, Any> = mapOf(
        "uid" to uid,
        "type" to type,
        "title" to title,
        "body" to body,
        "equipmentId" to equipmentId,
        "equipmentName" to equipmentName,
        "boxNumber" to boxNumber,
        "read" to read,
        "createdAt" to createdAt
    )

    companion object {
        const val COLLECTION = "notifications"
        const val TYPE_BORROW = "borrow"
        const val TYPE_RETURN = "return"
        const val TYPE_DUE_SOON = "due_soon"
        const val TYPE_OVERDUE = "overdue"

        /**
         * Deterministic document id: one notification of each type per loan.
         * Writing with `set()` under this id is therefore idempotent, which is how
         * the periodic due-date worker avoids sending the same reminder twice.
         */
        fun docId(uid: String, equipmentId: String, borrowedAt: Long, type: String): String =
            "${uid}_${equipmentId}_${borrowedAt}_$type"

        fun from(doc: DocumentSnapshot): AppNotification = AppNotification(
            id = doc.id,
            uid = doc.getString("uid").orEmpty(),
            type = doc.getString("type").orEmpty(),
            title = doc.getString("title").orEmpty(),
            body = doc.getString("body").orEmpty(),
            equipmentId = doc.getString("equipmentId").orEmpty(),
            equipmentName = doc.getString("equipmentName").orEmpty(),
            boxNumber = (doc.get("boxNumber") as? Number)?.toInt() ?: 0,
            read = doc.getBoolean("read") ?: false,
            createdAt = (doc.get("createdAt") as? Number)?.toLong() ?: 0L
        )
    }
}
