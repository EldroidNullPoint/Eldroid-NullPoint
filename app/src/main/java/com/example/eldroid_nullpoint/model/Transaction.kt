package com.example.eldroid_nullpoint.model

import com.google.firebase.firestore.DocumentSnapshot

/**
 * One entry in the shared `transactions/{autoId}` log written by the SmartDock
 * tower whenever a card is tapped and an item leaves or returns to a box.
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
    val timestamp: Long = 0L
) {
    companion object {
        const val COLLECTION = "transactions"
        const val TYPE_BORROW = "borrow"
        const val TYPE_RETURN = "return"
        const val TYPE_OVERDUE = "overdue"
        const val TYPE_ALERT = "alert"

        fun from(doc: DocumentSnapshot): Transaction = Transaction(
            id = doc.id,
            uid = doc.getString("uid").orEmpty(),
            userName = doc.getString("userName").orEmpty(),
            equipmentId = doc.getString("equipmentId").orEmpty(),
            equipmentName = doc.getString("equipmentName").orEmpty(),
            boxNumber = (doc.get("boxNumber") as? Number)?.toInt() ?: 0,
            type = doc.getString("type").orEmpty(),
            timestamp = (doc.get("timestamp") as? Number)?.toLong() ?: 0L
        )
    }
}
