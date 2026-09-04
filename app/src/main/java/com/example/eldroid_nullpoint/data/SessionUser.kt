package com.example.eldroid_nullpoint.data

/**
 * The minimal, framework-free description of who is currently signed in.
 * Presenters work with this instead of FirebaseUser so they stay testable.
 */
data class SessionUser(
    val uid: String,
    val email: String,
    val displayName: String,
    /** True when the account can sign in with an email + password (so it can change its password). */
    val hasPasswordProvider: Boolean,
    /** "password" or "google.com". */
    val providerId: String
)
