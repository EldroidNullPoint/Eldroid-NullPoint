package com.example.eldroid_nullpoint.model

/**
 * Firestore user profile document stored under the "users" collection,
 * keyed by the Firebase Auth uid.
 *
 * A no-argument constructor (all default values) is required so that
 * Firestore's automatic POJO <-> document mapping (toObject) works.
 */
data class User(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val provider: String = "email", // "email" or "google.com"
    /**
     * UID of the physical RFID card the borrower taps on the SmartDock tower.
     * Normally assigned by the administrator side; the Profile screen also lets a
     * borrower enter it for demonstrations. The default keeps existing user
     * documents readable unchanged.
     */
    val rfidCardUid: String = "",
    /** Whether due-soon / overdue reminders may be shown on the borrower's phone (FR-06). */
    val notificationsEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    /** "First Last", or whichever half is present. Empty when the profile has no name. */
    val fullName: String
        get() = listOf(firstName, lastName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
}
