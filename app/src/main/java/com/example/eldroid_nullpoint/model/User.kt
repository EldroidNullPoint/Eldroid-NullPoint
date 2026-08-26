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
    val provider: String = "email", // "email", "google.com" or "facebook.com"
    val createdAt: Long = System.currentTimeMillis()
)
