package com.example.eldroid_nullpoint.model

import com.google.firebase.firestore.DocumentSnapshot

/**
 * One entry in the shared `transactions/{autoId}` log, appended whenever an item
 * leaves or returns to a box - by the SmartDock tower after an RFID tap, or by the
 * app's Borrow / Return actions while the tower is being built.
 */
data class Transaction(
    val id: String = "",
    val uid: String = "",
    val userName: String = "",
    val equipmentId: String = "",
    val equipmentName: String = "",
    val boxNumber: Int = 0,
    /** "borrow", "return", "overdue" or "alert". */
    val type: String = "",
    /** Unix epoch millis. */
    val timestamp: Long = 0L,
    /** Original due time when the loan was created. Unix epoch millis, 0 for return records. */
    val originalDueAt: Long = 0L,
    /** Current due time, updated if an extension was approved. Unix epoch millis. */
    val currentDueAt: Long = 0L,
    /** Number of approved extensions on this loan. */
    val extensionCount: Int = 0
) {
    companion object {
        const val COLLECTION = "transactions"
        const val TYPE_BORROW = "borrow"
        const val TYPE_RETURN = "return"
        const val TYPE_OVERDUE = "overdue"
        const val TYPE_ALERT = "alert"

        fun from(doc: DocumentSnapshot): Transaction = Transaction(
            id            = doc.id,
            uid           = doc.getString("uid").orEmpty(),
            userName      = doc.getString("userName").orEmpty(),
            equipmentId   = doc.getString("equipmentId").orEmpty(),
            equipmentName = doc.getString("equipmentName").orEmpty(),
            boxNumber     = (doc.get("boxNumber") as? Number)?.toInt() ?: 0,
            type          = doc.getString("type").orEmpty(),
            timestamp     = (doc.get("timestamp") as? Number)?.toLong() ?: 0L,
            originalDueAt = (doc.get("originalDueAt") as? Number)?.toLong() ?: 0L,
            currentDueAt  = (doc.get("currentDueAt") as? Number)?.toLong() ?: 0L,
            extensionCount = (doc.get("extensionCount") as? Number)?.toInt() ?: 0
        )
    }
}
