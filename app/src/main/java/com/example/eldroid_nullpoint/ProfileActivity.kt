package com.example.eldroid_nullpoint

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.eldroid_nullpoint.databinding.ActivityProfileBinding
import com.example.eldroid_nullpoint.model.User
import com.example.eldroid_nullpoint.mvp.profile.ProfileContract
import com.example.eldroid_nullpoint.mvp.profile.ProfilePresenter
import com.example.eldroid_nullpoint.util.NotificationPrefs
import com.example.eldroid_nullpoint.util.Notifier
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity(), ProfileContract.View {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var presenter: ProfileContract.Presenter

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, R.string.notifications_permission_hint, Toast.LENGTH_LONG).show()
            } else {
                Notifier.ensureChannel(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            finish()
            return
        }

        presenter = ProfilePresenter(this, this)

        binding.ivBack.setOnClickListener { finish() }

        binding.btnSave.setOnClickListener {
            presenter.onSaveClicked(
                firstName = binding.etFirstName.text.toString().trim(),
                lastName = binding.etLastName.text.toString().trim(),
                rfidCardUid = binding.etRfidCardUid.text.toString().trim(),
                notificationsEnabled = binding.switchReminders.isChecked
            )
        }

        binding.rowChangePassword.setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        binding.rowLogout.setOnClickListener { confirmLogout() }

        binding.switchReminders.isChecked = NotificationPrefs.isEnabled(this)
        binding.switchReminders.setOnCheckedChangeListener { _, checked ->
            if (checked) requestNotificationPermissionIfNeeded()
        }

        // Set email from FirebaseAuth immediately so the screen is never blank
        binding.tvEmail.text = user.email.orEmpty()

        val isGoogle = user.providerData.any { it.providerId == "google.com" }
        presenter.onViewCreated(
            uid = user.uid,
            displayName = user.displayName,
            isGoogle = isGoogle
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.detach()
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.logout_confirm_title)
            .setMessage(R.string.logout_confirm_message)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_logout) { _, _ ->
                presenter.onLogoutConfirmed()
                // Navigate to login after sign-out
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .show()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // ---------------------------------------------------------------
    // ProfileContract.View
    // ---------------------------------------------------------------

    override fun showLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.contentContainer.visibility = if (loading) View.GONE else View.VISIBLE
    }

    override fun showSubmitting(submitting: Boolean) {
        binding.btnSave.isEnabled = !submitting
        binding.btnSave.text = if (submitting) "" else getString(R.string.btn_save_profile)
        binding.progressSave.visibility = if (submitting) View.VISIBLE else View.GONE
    }

    override fun populateFields(user: User) {
        binding.etFirstName.setText(user.firstName)
        binding.etLastName.setText(user.lastName)
        binding.etRfidCardUid.setText(user.rfidCardUid)
        if (user.email.isNotBlank()) binding.tvEmail.text = user.email
        binding.switchReminders.isChecked = user.notificationsEnabled
    }

    override fun renderName(firstName: String, lastName: String) {
        val fullName = listOf(firstName.trim(), lastName.trim())
            .filter { it.isNotBlank() }
            .joinToString(" ")
        binding.tvFullName.text = fullName.ifBlank { getString(R.string.title_profile) }

        val initials = listOf(firstName, lastName)
            .mapNotNull { it.trim().firstOrNull()?.uppercaseChar() }
            .joinToString("")
        binding.tvInitials.text = initials.ifBlank { "?" }
    }

    override fun renderRfidStatus(registered: Boolean) {
        binding.tvRfidStatus.text = getString(
            if (registered) R.string.rfid_card_registered else R.string.rfid_card_missing
        )
        binding.tvRfidStatus.setBackgroundResource(
            if (registered) R.drawable.bg_pill_available else R.drawable.bg_pill_due_soon
        )
        binding.tvRfidStatus.setTextColor(
            ContextCompat.getColor(
                this,
                if (registered) R.color.brand_dark_green else R.color.warning_amber
            )
        )
    }

    override fun showFirstNameError(message: String) {
        showError(binding.tvFirstNameError, message)
    }

    override fun showLastNameError(message: String) {
        showError(binding.tvLastNameError, message)
    }

    override fun showRfidError(message: String) {
        showError(binding.tvRfidError, message)
    }

    override fun clearErrors() {
        hideError(binding.tvFirstNameError)
        hideError(binding.tvLastNameError)
        hideError(binding.tvRfidError)
    }

    override fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun showChangePasswordRow(visible: Boolean) {
        binding.rowChangePassword.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun setProviderLabel(isGoogle: Boolean) {
        binding.tvProvider.text =
            getString(if (isGoogle) R.string.provider_google else R.string.provider_email)
    }

    override fun setEmail(email: String) {
        binding.tvEmail.text = email
    }

    override fun showRfidWarning(visible: Boolean) {
        // rfidWarning view is optional – guard in case the layout omits it
        binding.root.findViewWithTag<View>("rfidWarning")?.visibility =
            if (visible) View.VISIBLE else View.GONE
    }

    private fun showError(view: TextView, message: String) {
        view.text = message
        view.visibility = View.VISIBLE
    }

    private fun hideError(view: TextView) {
        view.visibility = View.GONE
    }
}
