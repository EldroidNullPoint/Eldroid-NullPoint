package com.example.eldroid_nullpoint.model

/**
 * One borrow / return / alert event, stored in the Firestore collection
 * "transactions". Written by the SmartDock tower (via Cloud Functions) and
 * read by the borrower app for the "Recent activity" list.
 */
data class EquipmentTransaction(
    val id: String = "",
    val uid: String = "",
    val userName: String = "",
    val equipmentId: String = "",
    val equipmentName: String = "",
    val boxNumber: Int = 0,
    /** One of [TYPE_BORROW], [TYPE_RETURN], [TYPE_OVERDUE], [TYPE_ALERT]. */
    val type: String = TYPE_BORROW,
    val timestamp: Long = 0L
) {
    companion object {
        const val TYPE_BORROW = "borrow"
        const val TYPE_RETURN = "return"
        const val TYPE_OVERDUE = "overdue"
        const val TYPE_ALERT = "alert"
    }
}
