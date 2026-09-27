package com.example.eldroid_nullpoint.model

import com.google.firebase.firestore.DocumentSnapshot

/**
 * One SmartDock box and the equipment inside it: `equipment/{boxId}`.
 *
 * Created by the administrator side (or the dashboard's one-time seed) and moved
 * between "available" and "borrowed" by the SmartDock tower - or, until the
 * hardware exists, by the app's Borrow / Return actions via SmartDockRepository.
 */
data class Equipment(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val boxNumber: Int = 0,
    /** Exactly "available" or "borrowed". */
    val status: String = STATUS_AVAILABLE,
    val borrowedBy: String = "",
    val borrowedByName: String = "",
    /** Unix epoch millis, or 0 when not borrowed. */
    val borrowedAt: Long = 0L,
    /** Unix epoch millis, or 0 when not borrowed. */
    val dueAt: Long = 0L,
    /**
     * Optional data-URL image uploaded via the admin dashboard, e.g.
     * "data:image/webp;base64,AAAA...". Empty string means no admin photo.
     */
    val imageData: String = ""
) {
    val isBorrowed: Boolean get() = status == STATUS_BORROWED

    fun isBorrowedBy(uid: String): Boolean = uid.isNotBlank() && borrowedBy == uid

    /** True once the due time has passed while the item is still out. */
    fun isOverdue(now: Long = System.currentTimeMillis()): Boolean =
        isBorrowed && dueAt > 0L && now > dueAt

    companion object {
        const val COLLECTION = "equipment"
        const val STATUS_AVAILABLE = "available"
        const val STATUS_BORROWED = "borrowed"

        /**
         * Reads a document defensively: the tower may write numeric fields as Long
         * or Double, and older documents may be missing fields entirely.
         */
        fun from(doc: DocumentSnapshot): Equipment = Equipment(
            id = doc.id,
            name = doc.getString("name").orEmpty(),
            category = doc.getString("category").orEmpty(),
            boxNumber = (doc.get("boxNumber") as? Number)?.toInt() ?: 0,
            status = doc.getString("status") ?: STATUS_AVAILABLE,
            borrowedBy = doc.getString("borrowedBy").orEmpty(),
            borrowedByName = doc.getString("borrowedByName").orEmpty(),
            borrowedAt = (doc.get("borrowedAt") as? Number)?.toLong() ?: 0L,
            dueAt = (doc.get("dueAt") as? Number)?.toLong() ?: 0L,
            imageData = doc.getString("imageData").orEmpty()
        )
    }
}
