package com.example.eldroid_nullpoint.mvp.signup

import com.example.eldroid_nullpoint.model.User
import com.example.eldroid_nullpoint.util.Validators
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class SignupPresenter(
    private var view: SignupContract.View?,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : SignupContract.Presenter {

    override fun onSignupClicked(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        view?.clearErrors()
        var isValid = true

        if (firstName.isBlank()) {
            view?.showFirstNameError("First name is required")
            isValid = false
        } else if (!Validators.isValidName(firstName)) {
            view?.showFirstNameError("Enter a valid first name (2-40 letters)")
            isValid = false
        }

        if (lastName.isBlank()) {
            view?.showLastNameError("Last name is required")
            isValid = false
        } else if (!Validators.isValidName(lastName)) {
            view?.showLastNameError("Enter a valid last name (2-40 letters)")
            isValid = false
        }

        if (email.isBlank()) {
            view?.showEmailError("Email is required")
            isValid = false
        } else if (!Validators.isValidEmail(email)) {
            view?.showEmailError("Enter a valid email address")
            isValid = false
        }

        val passwordError = Validators.passwordStrengthError(password)
        if (passwordError != null) {
            view?.showPasswordError(passwordError)
            isValid = false
        }

        if (confirmPassword.isBlank()) {
            view?.showConfirmPasswordError("Please confirm your password")
            isValid = false
        } else if (password != confirmPassword) {
            view?.showConfirmPasswordError("Passwords do not match")
            isValid = false
        }

        if (!isValid) return

        view?.showLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result?.user?.uid.orEmpty()
                    saveUserProfile(
                        User(
                            uid = uid,
                            firstName = firstName,
                            lastName = lastName,
                            email = email,
                            provider = "email"
                        )
                    )
                } else {
                    view?.showLoading(false)
                    val exception = task.exception
                    val message = when (exception) {
                        is FirebaseAuthUserCollisionException ->
                            "An account with this email already exists"
                        is FirebaseAuthInvalidCredentialsException ->
                            "Invalid email format"
                        else -> exception?.message ?: "Registration failed. Please try again."
                    }
                    if (exception is FirebaseAuthUserCollisionException ||
                        exception is FirebaseAuthInvalidCredentialsException
                    ) {
                        view?.showEmailError(message)
                    } else {
                        view?.showToast(message)
                    }
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

    private fun saveUserProfile(user: User) {
        firestore.collection("users").document(user.uid)
            .set(user)
            .addOnCompleteListener {
                view?.showLoading(false)
                view?.navigateToHome()
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
                view?.showLoading(false)
                if (!snapshot.exists()) {
                    docRef.set(
                        User(
                            uid = uid,
                            firstName = firstName,
                            lastName = lastName,
                            email = email,
                            provider = provider
                        )
                    )
                }
                view?.navigateToHome()
            }
            .addOnFailureListener {
                view?.showLoading(false)
                view?.navigateToHome()
            }
    }

    private fun splitDisplayName(displayName: String?): Pair<String, String> {
        if (displayName.isNullOrBlank()) return "" to ""
        val parts = displayName.trim().split(" ", limit = 2)
        return parts.getOrNull(0).orEmpty() to parts.getOrNull(1).orEmpty()
    }

    override fun detach() {
        view = null
    }
}
