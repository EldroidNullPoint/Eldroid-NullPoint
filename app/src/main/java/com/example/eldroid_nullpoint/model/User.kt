package com.example.eldroid_nullpoint.model

import com.google.firebase.firestore.Exclude

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
    val provider: String = PROVIDER_EMAIL,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = 0L,
    val loginCount: Long = 0L
) {

    /** "First Last", skipping whichever part is blank. Not persisted to Firestore. */
    @get:Exclude
    val fullName: String
        get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")

    companion object {
        const val PROVIDER_EMAIL = "email"
        const val PROVIDER_GOOGLE = "google.com"
    }
}
