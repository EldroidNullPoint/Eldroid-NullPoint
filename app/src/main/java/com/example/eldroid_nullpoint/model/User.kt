package com.example.eldroid_nullpoint.model

/**
 * Firestore user profile document stored under the "users" collection,
 * keyed by the Firebase Auth uid.
 *
 * Spec §2: borrower account lifecycle states.
 * Spec §4: RFID card status states.
 */
data class User(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val provider: String = "email",  // "email" or "google.com"

    /**
     * Account lifecycle state, written/updated by the admin dashboard.
     * Spec §2 recommended states.
     */
    val accountStatus: String = ACCOUNT_PENDING,

    /**
     * RFID card status, written/updated by the admin dashboard.
     * Spec §4 RFID status values.
     */
    val rfidStatus: String = RFID_NOT_ISSUED,

    /**
     * Raw UID of the physical RFID card assigned by admin.
     * Empty until admin assigns a card.
     */
    val rfidCardUid: String = "",

    /** Whether due-soon / overdue reminders may be shown on the borrower's phone. */
    val notificationsEnabled: Boolean = true,

    val createdAt: Long = System.currentTimeMillis()
) {
    /** "First Last", or whichever half is present. */
    val fullName: String
        get() = listOf(firstName, lastName)
            .filter { it.isNotBlank() }
            .joinToString(" ")

    /** True when the account is fully active and RFID is assigned and active. */
    val canBorrow: Boolean
        get() = accountStatus == ACCOUNT_ACTIVE && rfidStatus == RFID_ACTIVE

    /** True when the borrower may still return equipment (even if suspended). */
    val canReturn: Boolean
        get() = accountStatus != ACCOUNT_REJECTED

    companion object {
        // ── Account status constants (spec §2) ────────────────────
        const val ACCOUNT_PENDING  = "pending_verification"
        const val ACCOUNT_APPROVED_NO_RFID = "approved_no_rfid"
        const val ACCOUNT_ACTIVE   = "active"
        const val ACCOUNT_REJECTED = "rejected"
        const val ACCOUNT_SUSPENDED = "suspended"
        const val ACCOUNT_OVERDUE_RESTRICTED = "overdue_restricted"
        const val ACCOUNT_INACTIVE = "inactive"

        // ── RFID status constants (spec §4) ────────────────────────
        const val RFID_NOT_ISSUED  = "not_issued"
        const val RFID_ACTIVE      = "active"
        const val RFID_DISABLED    = "disabled"
        const val RFID_LOST        = "lost"
        const val RFID_REPLACEMENT_REQUIRED = "replacement_required"
    }
}
