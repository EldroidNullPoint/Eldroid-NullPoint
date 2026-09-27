package com.example.eldroid_nullpoint.model

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
    /**
     * Primary lifecycle state written by the admin dashboard or IoT tower.
     * Values: "available", "borrowed", "maintenance", "damaged", "retired",
     *         "lost", "sensor_fault", "status_unknown", "unconfigured".
     * The app treats any value other than "available"/"borrowed" as unavailable.
     */
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

    /** True when the equipment is in any state that prevents borrowing. */
    val isUnavailable: Boolean
        get() = status in setOf(
            STATUS_MAINTENANCE, STATUS_DAMAGED, STATUS_RETIRED,
            STATUS_LOST, STATUS_SENSOR_FAULT, STATUS_UNKNOWN, STATUS_UNCONFIGURED
        )

    fun isBorrowedBy(uid: String): Boolean = uid.isNotBlank() && borrowedBy == uid

    /** True once the due time has passed while the item is still out. */
    fun isOverdue(now: Long = System.currentTimeMillis()): Boolean =
        isBorrowed && dueAt > 0L && now > dueAt

    companion object {
        const val COLLECTION = "equipment"
        const val STATUS_AVAILABLE = "available"
        const val STATUS_BORROWED = "borrowed"
        // Admin / IoT-written states — borrower app treats these as unavailable
        const val STATUS_MAINTENANCE = "maintenance"
        const val STATUS_DAMAGED = "damaged"
        const val STATUS_RETIRED = "retired"
        const val STATUS_LOST = "lost"
        const val STATUS_SENSOR_FAULT = "sensor_fault"
        const val STATUS_UNKNOWN = "status_unknown"
        const val STATUS_UNCONFIGURED = "unconfigured"

        /**
         * Reads a document defensively: the tower may write numeric fields as Long
         * or Double, and older documents may be missing fields entirely.
         */
        fun from(doc: com.google.firebase.firestore.DocumentSnapshot): Equipment = Equipment(
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
