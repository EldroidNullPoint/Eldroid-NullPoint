package com.example.eldroid_nullpoint.data

import com.example.eldroid_nullpoint.model.User

/** Callback used by every asynchronous repository call. Always invoked on the main thread. */
typealias RepositoryCallback<T> = (Result<T>) -> Unit

/**
 * The Model layer of the MVP triad.
 *
 * Presenters depend only on this interface, never on Firebase directly, which is
 * what makes them unit-testable (see FakeAuthRepository in the test sources).
 * [FirebaseAuthRepository] is the production implementation.
 */
interface AuthRepository {

    /** The signed-in user, or null when nobody is logged in. Synchronous. */
    fun currentUser(): SessionUser?

    fun login(email: String, password: String, callback: RepositoryCallback<SessionUser>)

    fun loginWithGoogle(idToken: String, callback: RepositoryCallback<SessionUser>)

    fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        callback: RepositoryCallback<SessionUser>
    )

    fun sendPasswordReset(email: String, callback: RepositoryCallback<Unit>)

    /** Re-authenticates with [currentPassword] and then sets [newPassword]. Email/password accounts only. */
    fun changePassword(currentPassword: String, newPassword: String, callback: RepositoryCallback<Unit>)

    fun fetchProfile(uid: String, callback: RepositoryCallback<User>)

    fun logout()
}
