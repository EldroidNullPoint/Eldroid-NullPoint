package com.example.eldroid_nullpoint.mvp.login

import com.example.eldroid_nullpoint.model.User
import com.example.eldroid_nullpoint.util.AuthErrors
import com.example.eldroid_nullpoint.util.Validators
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class LoginPresenter(
    private var view: LoginContract.View?,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : LoginContract.Presenter {

    override fun onLoginClicked(email: String, password: String) {
        view?.clearErrors()

        var isValid = true

        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            view?.showEmailError("Email is required")
            isValid = false
        } else if (!Validators.isValidEmail(trimmedEmail)) {
            view?.showEmailError("Enter a valid email address")
            isValid = false
        }

        if (password.isBlank()) {
            view?.showPasswordError("Password is required")
            isValid = false
        }

        if (!isValid) return

        view?.showLoading(true)
        auth.signInWithEmailAndPassword(trimmedEmail, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    checkTermsAcceptance()
                } else {
                    view?.showLoading(false)
                    view?.showPasswordError(resolveSignInError(task.exception))
                }
            }
    }

    override fun onGoogleTokenReceived(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    val nameParts = splitDisplayName(firebaseUser?.displayName)
                    ensureUserDocument(
                        uid = firebaseUser?.uid.orEmpty(),
                        firstName = nameParts.first,
                        lastName = nameParts.second,
                        email = firebaseUser?.email.orEmpty(),
                        provider = "google.com"
                    )
                } else {
                    view?.showLoading(false)
                    view?.showToast(task.exception?.message ?: "Google sign-in failed")
                }
            }
    }

    private fun ensureUserDocument(
        uid: String,
        firstName: String,
        lastName: String,
        email: String,
        provider: String
    ) {
        if (uid.isBlank()) {
            view?.showLoading(false)
            view?.navigateToHome()
            return
        }
        val docRef = firestore.collection("users").document(uid)
        docRef.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    val user = User(
                        uid = uid,
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        provider = provider
                    )
                    docRef.set(user)
                        .addOnSuccessListener {
                            checkTermsAcceptance()
                        }
                        .addOnFailureListener {
                            view?.showLoading(false)
                            view?.navigateToHome()
                        }
                } else {
                    checkTermsAcceptance()
                }
            }
            .addOnFailureListener {
                view?.showLoading(false)
                // Auth succeeded – let the user in even if the profile write failed.
                view?.navigateToHome()
            }
    }

    private fun checkTermsAcceptance() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            view?.showLoading(false)
            view?.navigateToHome()
            return
        }

        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                view?.showLoading(false)
                val termsAccepted = snapshot.getBoolean("termsAccepted") ?: false
                if (termsAccepted) {
                    view?.navigateToHome()
                } else {
                    view?.navigateToTerms()
                }
            }
            .addOnFailureListener {
                view?.showLoading(false)
                // If we can't check, show terms to be safe
                view?.navigateToTerms()
            }
    }

    private fun splitDisplayName(displayName: String?): Pair<String, String> {
        if (displayName.isNullOrBlank()) return "" to ""
        val parts = displayName.trim().split(" ", limit = 2)
        return parts.getOrNull(0).orEmpty() to parts.getOrNull(1).orEmpty()
    }

    /**
     * Maps Firebase sign-in exceptions to the same neutral message so the screen
     * cannot be used to probe which emails are registered.
     */
    private fun resolveSignInError(throwable: Throwable?): String {
        return when (throwable) {
            is com.google.firebase.auth.FirebaseAuthInvalidUserException,
            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
                "Incorrect email or password. Please try again."
            is com.google.firebase.FirebaseNetworkException ->
                "No internet connection. Please check your network."
            is com.google.firebase.FirebaseTooManyRequestsException ->
                "Too many attempts. Please try again later."
            else -> "Login failed. Please try again."
        }
    }

    override fun detach() {
        view = null
    }
}
