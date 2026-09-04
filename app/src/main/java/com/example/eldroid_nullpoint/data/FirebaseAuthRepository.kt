package com.example.eldroid_nullpoint.data

import android.content.Context
import com.example.eldroid_nullpoint.model.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Production [AuthRepository] backed by Firebase Auth + Cloud Firestore.
 *
 * All Firebase details (exceptions, Tasks, FirebaseUser, Firestore documents)
 * are translated here into plain Kotlin types so nothing above this layer
 * needs to import Firebase.
 */
class FirebaseAuthRepository(
    private val appContext: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    private val usersCollection get() = firestore.collection(USERS_COLLECTION)

    override fun currentUser(): SessionUser? = auth.currentUser?.toSessionUser()

    // ---------------------------------------------------------------
    // Login
    // ---------------------------------------------------------------

    override fun login(email: String, password: String, callback: RepositoryCallback<SessionUser>) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    callback(Result.failure(AuthError(GENERIC_ERROR)))
                    return@addOnSuccessListener
                }
                recordLogin(user.uid) { callback(Result.success(user.toSessionUser())) }
            }
            .addOnFailureListener { callback(Result.failure(it.toAuthError())) }
    }

    override fun loginWithGoogle(idToken: String, callback: RepositoryCallback<SessionUser>) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    callback(Result.failure(AuthError(GENERIC_ERROR)))
                    return@addOnSuccessListener
                }
                ensureProfileExists(user) {
                    recordLogin(user.uid) { callback(Result.success(user.toSessionUser())) }
                }
            }
            .addOnFailureListener { callback(Result.failure(it.toAuthError())) }
    }

    // ---------------------------------------------------------------
    // Register
    // ---------------------------------------------------------------

    override fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        callback: RepositoryCallback<SessionUser>
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    callback(Result.failure(AuthError(GENERIC_ERROR)))
                    return@addOnSuccessListener
                }

                // Store the name on the Auth profile too so it is available even if
                // the Firestore write below fails.
                user.updateProfile(userProfileChangeRequest { displayName = "$firstName $lastName".trim() })

                val now = System.currentTimeMillis()
                val profile = User(
                    uid = user.uid,
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    provider = User.PROVIDER_EMAIL,
                    createdAt = now,
                    lastLoginAt = now,
                    loginCount = 1
                )
                usersCollection.document(user.uid).set(profile)
                    .addOnCompleteListener {
                        // The Auth account exists either way, so let the user in. If the
                        // profile write failed, fetchProfile() falls back to Auth data.
                        callback(Result.success(user.toSessionUser()))
                    }
            }
            .addOnFailureListener { callback(Result.failure(it.toAuthError())) }
    }

    // ---------------------------------------------------------------
    // Password management
    // ---------------------------------------------------------------

    override fun sendPasswordReset(email: String, callback: RepositoryCallback<Unit>) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it.toAuthError())) }
    }

    override fun changePassword(
        currentPassword: String,
        newPassword: String,
        callback: RepositoryCallback<Unit>
    ) {
        val user = auth.currentUser
        if (user == null) {
            callback(Result.failure(AuthError("You are not signed in.")))
            return
        }
        val email = user.email
        if (email.isNullOrBlank() || !user.hasPasswordProvider()) {
            callback(Result.failure(AuthError(PASSWORD_CHANGE_UNAVAILABLE)))
            return
        }

        // Firebase requires a recent login before sensitive operations, so we
        // re-authenticate with the current password first. This also verifies it.
        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener { callback(Result.success(Unit)) }
                    .addOnFailureListener { callback(Result.failure(it.toAuthError())) }
            }
            .addOnFailureListener { e ->
                val error = if (e is FirebaseAuthInvalidCredentialsException) {
                    AuthError("Current password is incorrect.", e)
                } else {
                    e.toAuthError()
                }
                callback(Result.failure(error))
            }
    }

    // ---------------------------------------------------------------
    // Profile
    // ---------------------------------------------------------------

    override fun fetchProfile(uid: String, callback: RepositoryCallback<User>) {
        usersCollection.document(uid).get()
            .addOnSuccessListener { snapshot ->
                val profile = snapshot.toObject(User::class.java)
                callback(Result.success(profile ?: profileFromAuth()))
            }
            .addOnFailureListener { callback(Result.failure(it.toAuthError())) }
    }

    override fun logout() {
        auth.signOut()
        // Also sign out of Google so the account picker shows again next time.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(appContext, gso).signOut()
    }

    // ---------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------

    /** Creates users/{uid} the first time a Google account signs in. */
    private fun ensureProfileExists(user: FirebaseUser, onDone: () -> Unit) {
        val docRef = usersCollection.document(user.uid)
        docRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    onDone()
                    return@addOnSuccessListener
                }
                val (first, last) = splitDisplayName(user.displayName)
                val profile = User(
                    uid = user.uid,
                    firstName = first,
                    lastName = last,
                    email = user.email.orEmpty(),
                    provider = User.PROVIDER_GOOGLE,
                    createdAt = System.currentTimeMillis()
                )
                docRef.set(profile).addOnCompleteListener { onDone() }
            }
            .addOnFailureListener { onDone() }
    }

    /** Bumps loginCount and lastLoginAt; never blocks the login on failure. */
    private fun recordLogin(uid: String, onDone: () -> Unit) {
        val update = mapOf(
            "lastLoginAt" to System.currentTimeMillis(),
            "loginCount" to FieldValue.increment(1)
        )
        usersCollection.document(uid)
            .set(update, SetOptions.merge())
            .addOnCompleteListener { onDone() }
    }

    /** Fallback profile built from Firebase Auth when the Firestore document is missing. */
    private fun profileFromAuth(): User {
        val user = auth.currentUser ?: return User()
        val (first, last) = splitDisplayName(user.displayName)
        return User(
            uid = user.uid,
            firstName = first,
            lastName = last,
            email = user.email.orEmpty(),
            provider = if (user.hasPasswordProvider()) User.PROVIDER_EMAIL else User.PROVIDER_GOOGLE,
            createdAt = user.metadata?.creationTimestamp ?: 0L,
            lastLoginAt = user.metadata?.lastSignInTimestamp ?: 0L
        )
    }

    private fun splitDisplayName(displayName: String?): Pair<String, String> {
        if (displayName.isNullOrBlank()) return "" to ""
        val parts = displayName.trim().split(" ", limit = 2)
        return parts.getOrNull(0).orEmpty() to parts.getOrNull(1).orEmpty()
    }

    private fun FirebaseUser.hasPasswordProvider(): Boolean =
        providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }

    private fun FirebaseUser.toSessionUser(): SessionUser {
        val hasPassword = hasPasswordProvider()
        val providerId = if (hasPassword) {
            EmailAuthProvider.PROVIDER_ID
        } else {
            providerData.firstOrNull { it.providerId != "firebase" }?.providerId
                ?: GoogleAuthProvider.PROVIDER_ID
        }
        return SessionUser(
            uid = uid,
            email = email.orEmpty(),
            displayName = displayName.orEmpty(),
            hasPasswordProvider = hasPassword,
            providerId = providerId
        )
    }

    /** Maps Firebase exceptions to messages that are safe and helpful to show to users. */
    private fun Throwable.toAuthError(): AuthError {
        val message = when (this) {
            is FirebaseAuthWeakPasswordException -> "Password is too weak."
            is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
            is FirebaseAuthInvalidUserException -> "No account found for this email, or it has been disabled."
            is FirebaseAuthUserCollisionException -> "An account already exists with this email."
            is FirebaseAuthRecentLoginRequiredException -> "Please log in again before changing your password."
            is FirebaseTooManyRequestsException -> "Too many attempts. Please try again later."
            is FirebaseNetworkException -> "No internet connection. Check your network and try again."
            else -> localizedMessage?.takeIf { it.isNotBlank() } ?: GENERIC_ERROR
        }
        return AuthError(message, this)
    }

    companion object {
        const val USERS_COLLECTION = "users"
        const val GENERIC_ERROR = "Something went wrong. Please try again."
        const val PASSWORD_CHANGE_UNAVAILABLE =
            "Password change is only available for accounts that signed up with email and password."
    }
}
