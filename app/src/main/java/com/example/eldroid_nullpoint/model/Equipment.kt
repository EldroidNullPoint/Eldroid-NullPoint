package com.example.eldroid_nullpoint.model

import com.google.firebase.firestore.Exclude

/**
 * One monitored box in the SmartDock tower and the item assigned to it.
 * Stored in the Firestore collection "equipment", keyed by [id].
 *
 * The ESP32 tower updates [status], [borrowedBy], [borrowedAt] and [dueAt]
 * when a borrower taps their RFID card and removes/returns the item.
 */
data class Equipment(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val boxNumber: Int = 0,
    val status: String = STATUS_AVAILABLE,
    /** Firebase uid of the current borrower, or "" when available. */
    val borrowedBy: String = "",
    val borrowedByName: String = "",
    val borrowedAt: Long = 0L,
    val dueAt: Long = 0L
) {

    @get:Exclude
    val isAvailable: Boolean
        get() = status == STATUS_AVAILABLE

    /** Functions with parameters are never treated as Firestore properties, so no @Exclude needed. */
    fun isBorrowedBy(uid: String): Boolean = !isAvailable && borrowedBy == uid

    fun isOverdue(now: Long): Boolean = !isAvailable && dueAt in 1 until now

    companion object {
        const val STATUS_AVAILABLE = "available"
        const val STATUS_BORROWED = "borrowed"
    }
}
