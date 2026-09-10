package com.example.eldroid_nullpoint

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.example.eldroid_nullpoint.databinding.ActivityChangePasswordBinding
import com.example.eldroid_nullpoint.util.AuthErrors
import com.example.eldroid_nullpoint.util.Validators
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

/**
 * Lets a signed-in borrower change their password.
 *
 * Firebase requires a recent login before `updatePassword`, so the current
 * password is used to re-authenticate first. Passwords only ever live in the
 * input fields and the Firebase call - nothing is logged or persisted locally.
 */
class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding
    private lateinit var auth: FirebaseAuth

    private var isSubmitting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            finish()
            return
        }

        binding.ivBack.setOnClickListener { finish() }
        binding.btnUpdatePassword.setOnClickListener { attemptChangePassword() }

        setupPasswordToggle(binding.etCurrentPassword, binding.ivToggleCurrentPassword)
        setupPasswordToggle(binding.etNewPassword, binding.ivToggleNewPassword)
        setupPasswordToggle(binding.etConfirmNewPassword, binding.ivToggleConfirmNewPassword)

        binding.etCurrentPassword.doOnTextChanged { _, _, _, _ ->
            binding.tvCurrentPasswordError.visibility = View.GONE
        }
        binding.etNewPassword.doOnTextChanged { _, _, _, _ ->
            binding.tvNewPasswordError.visibility = View.GONE
        }
        binding.etConfirmNewPassword.doOnTextChanged { _, _, _, _ ->
            binding.tvConfirmNewPasswordError.visibility = View.GONE
        }
    }

    private fun setupPasswordToggle(field: EditText, toggle: ImageView) {
        var visible = false
        toggle.setOnClickListener {
            visible = !visible
            field.transformationMethod = if (visible) {
                HideReturnsTransformationMethod.getInstance()
            } else {
                PasswordTransformationMethod.getInstance()
            }
            field.setSelection(field.text.length)
            toggle.setImageResource(if (visible) R.drawable.ic_eye_off else R.drawable.ic_eye)
        }
    }

    private fun attemptChangePassword() {
        if (isSubmitting) return
        clearErrors()

        val currentPassword = binding.etCurrentPassword.text.toString()
        val newPassword = binding.etNewPassword.text.toString()
        val confirmPassword = binding.etConfirmNewPassword.text.toString()

        var isValid = true

        if (currentPassword.isBlank()) {
            showError(binding.tvCurrentPasswordError, getString(R.string.error_current_password_required))
            isValid = false
        }

        // Same strict complexity rules the Register screen enforces.
        val strengthError = Validators.passwordStrengthError(newPassword)
        if (strengthError != null) {
            showError(binding.tvNewPasswordError, strengthError)
            isValid = false
        } else if (newPassword == currentPassword) {
            showError(binding.tvNewPasswordError, getString(R.string.error_password_same_as_current))
            isValid = false
        }

        if (confirmPassword.isBlank()) {
            showError(binding.tvConfirmNewPasswordError, getString(R.string.error_confirm_password_required))
            isValid = false
        } else if (newPassword != confirmPassword) {
            showError(binding.tvConfirmNewPasswordError, getString(R.string.error_passwords_dont_match))
            isValid = false
        }

        if (!isValid) return

        val user = auth.currentUser
        val email = user?.email
        if (user == null || email.isNullOrBlank()) {
            // Google-only accounts have no email/password credential to re-authenticate with.
            Toast.makeText(this, getString(R.string.auth_error_generic), Toast.LENGTH_LONG).show()
            return
        }

        setLoading(true)
        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (!reauthTask.isSuccessful) {
                    setLoading(false)
                    // A failed re-auth here almost always means a wrong current password.
                    showError(
                        binding.tvCurrentPasswordError,
                        AuthErrors.signInMessageFor(this, reauthTask.exception)
                    )
                    return@addOnCompleteListener
                }

                user.updatePassword(newPassword)
                    .addOnCompleteListener { updateTask ->
                        setLoading(false)
                        if (updateTask.isSuccessful) {
                            Toast.makeText(
                                this,
                                getString(R.string.password_changed),
                                Toast.LENGTH_LONG
                            ).show()
                            finish() // back to the dashboard the borrower came from
                        } else {
                            showError(
                                binding.tvNewPasswordError,
                                AuthErrors.messageFor(this, updateTask.exception)
                            )
                        }
                    }
            }
    }

    private fun showError(view: android.widget.TextView, message: String) {
        view.text = message
        view.visibility = View.VISIBLE
    }

    private fun clearErrors() {
        binding.tvCurrentPasswordError.visibility = View.GONE
        binding.tvNewPasswordError.visibility = View.GONE
        binding.tvConfirmNewPasswordError.visibility = View.GONE
    }

    private fun setLoading(loading: Boolean) {
        isSubmitting = loading
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnUpdatePassword.isEnabled = !loading
        binding.btnUpdatePassword.text =
            if (loading) "" else getString(R.string.btn_update_password)
        binding.etCurrentPassword.isEnabled = !loading
        binding.etNewPassword.isEnabled = !loading
        binding.etConfirmNewPassword.isEnabled = !loading
    }
}
