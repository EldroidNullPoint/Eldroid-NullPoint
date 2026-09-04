package com.example.eldroid_nullpoint.data

import com.example.eldroid_nullpoint.model.User

/**
 * In-memory [AuthRepository] for presenter tests. Every call is recorded in
 * [calls] and answers with the configurable result fields below.
 *
 * Set [respondImmediately] to false to hold callbacks until [flushPending] is
 * called, which lets a test assert the loading state in between.
 */
class FakeAuthRepository : AuthRepository {

    var sessionUser: SessionUser? = null

    var loginResult: Result<SessionUser> = Result.success(EMAIL_USER)
    var googleResult: Result<SessionUser> = Result.success(GOOGLE_USER)
    var registerResult: Result<SessionUser> = Result.success(EMAIL_USER)
    var resetResult: Result<Unit> = Result.success(Unit)
    var changePasswordResult: Result<Unit> = Result.success(Unit)
    var profileResult: Result<User> = Result.success(PROFILE)

    var respondImmediately = true
    private val pending = mutableListOf<() -> Unit>()

    val calls = mutableListOf<String>()

    var lastLoginEmail: String? = null
    var lastLoginPassword: String? = null
    var lastGoogleToken: String? = null
    var lastRegisterArgs: List<String>? = null
    var lastResetEmail: String? = null
    var lastChangePasswordArgs: Pair<String, String>? = null
    var lastFetchedUid: String? = null

    override fun currentUser(): SessionUser? = sessionUser

    override fun login(email: String, password: String, callback: RepositoryCallback<SessionUser>) {
        calls += "login"
        lastLoginEmail = email
        lastLoginPassword = password
        respond { callback(loginResult) }
    }

    override fun loginWithGoogle(idToken: String, callback: RepositoryCallback<SessionUser>) {
        calls += "loginWithGoogle"
        lastGoogleToken = idToken
        respond { callback(googleResult) }
    }

    override fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        callback: RepositoryCallback<SessionUser>
    ) {
        calls += "register"
        lastRegisterArgs = listOf(firstName, lastName, email, password)
        respond { callback(registerResult) }
    }

    override fun sendPasswordReset(email: String, callback: RepositoryCallback<Unit>) {
        calls += "sendPasswordReset"
        lastResetEmail = email
        respond { callback(resetResult) }
    }

    override fun changePassword(
        currentPassword: String,
        newPassword: String,
        callback: RepositoryCallback<Unit>
    ) {
        calls += "changePassword"
        lastChangePasswordArgs = currentPassword to newPassword
        respond { callback(changePasswordResult) }
    }

    override fun fetchProfile(uid: String, callback: RepositoryCallback<User>) {
        calls += "fetchProfile"
        lastFetchedUid = uid
        respond { callback(profileResult) }
    }

    override fun logout() {
        calls += "logout"
        sessionUser = null
    }

    fun flushPending() {
        val toRun = pending.toList()
        pending.clear()
        toRun.forEach { it() }
    }

    private fun respond(block: () -> Unit) {
        if (respondImmediately) block() else pending += block
    }

    companion object {
        val EMAIL_USER = SessionUser(
            uid = "uid-email",
            email = "jane.doe@example.com",
            displayName = "Jane Doe",
            hasPasswordProvider = true,
            providerId = "password"
        )
        val GOOGLE_USER = SessionUser(
            uid = "uid-google",
            email = "gina@gmail.com",
            displayName = "Gina Google",
            hasPasswordProvider = false,
            providerId = "google.com"
        )
        val PROFILE = User(
            uid = "uid-email",
            firstName = "Jane",
            lastName = "Doe",
            email = "jane.doe@example.com",
            provider = User.PROVIDER_EMAIL,
            createdAt = 1_000_000L,
            lastLoginAt = 2_000_000L,
            loginCount = 5
        )
    }
}
