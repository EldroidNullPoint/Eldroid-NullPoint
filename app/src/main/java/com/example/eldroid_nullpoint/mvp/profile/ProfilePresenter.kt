package com.example.eldroid_nullpoint.mvp.profile

import android.content.Context
import com.example.eldroid_nullpoint.model.User
import com.example.eldroid_nullpoint.util.NotificationPrefs
import com.example.eldroid_nullpoint.util.SmartDockRepository
import com.example.eldroid_nullpoint.util.Validators
import com.example.eldroid_nullpoint.work.DueCheckWorker
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfilePresenter(
    private var view: ProfileContract.View?,
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ProfileContract.Presenter {

    private var currentUid: String = ""

    override fun onViewCreated(uid: String, displayName: String?, isGoogle: Boolean) {
        currentUid = uid
        view?.setProviderLabel(isGoogle)
        view?.showChangePasswordRow(!isGoogle)
        view?.renderName(displayName?.split(" ")?.firstOrNull().orEmpty(), "")
        loadProfile()
    }

    private fun loadProfile() {
        view?.showLoading(true)
        firestore.collection("users").document(currentUid).get()
            .addOnSuccessListener { snapshot ->
                view?.showLoading(false)
                val profile = snapshot.toObject(User::class.java) ?: User(uid = currentUid)

                if (profile.email.isNotBlank()) view?.setEmail(profile.email)
                view?.populateFields(profile)
                view?.renderName(profile.firstName, profile.lastName)
                view?.renderRfidStatus(profile.rfidCardUid.isNotBlank())
                view?.showRfidWarning(profile.rfidCardUid.isBlank())
            }
            .addOnFailureListener {
                view?.showLoading(false)
                view?.renderRfidStatus(false)
                view?.showToast("Failed to load profile. Please try again.")
            }
    }

    override fun onSaveClicked(
        firstName: String,
        lastName: String,
        rfidCardUid: String,
        notificationsEnabled: Boolean
    ) {
        view?.clearErrors()
        var valid = true

        if (firstName.isEmpty()) {
            view?.showFirstNameError("First name is required")
            valid = false
        } else if (!Validators.isValidName(firstName)) {
            view?.showFirstNameError("Enter a valid first name (2-40 letters)")
            valid = false
        }

        if (lastName.isEmpty()) {
            view?.showLastNameError("Last name is required")
            valid = false
        } else if (!Validators.isValidName(lastName)) {
            view?.showLastNameError("Enter a valid last name (2-40 letters)")
            valid = false
        }

        val normalizedRfid = rfidCardUid.uppercase()
        if (!Validators.isValidRfidUid(normalizedRfid)) {
            view?.showRfidError("Invalid RFID UID format")
            valid = false
        }

        if (!valid) return

        view?.showSubmitting(true)
        SmartDockRepository.updateProfile(
            currentUid, firstName, lastName, normalizedRfid, notificationsEnabled
        )
            .addOnSuccessListener {
                view?.showSubmitting(false)
                NotificationPrefs.setEnabled(context, notificationsEnabled)
                view?.renderName(firstName, lastName)
                view?.renderRfidStatus(normalizedRfid.isNotBlank())
                view?.showToast("Profile saved")
            }
            .addOnFailureListener { error ->
                view?.showSubmitting(false)
                val msg = if (SmartDockRepository.isPermissionDenied(error)) {
                    "Permission denied. Contact your administrator."
                } else {
                    "Failed to save profile. Please try again."
                }
                view?.showToast(msg)
            }
    }

    override fun onLogoutConfirmed() {
        DueCheckWorker.cancel(context)
        auth.signOut()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(context, gso).signOut()
    }

    override fun detach() {
        view = null
    }
}
