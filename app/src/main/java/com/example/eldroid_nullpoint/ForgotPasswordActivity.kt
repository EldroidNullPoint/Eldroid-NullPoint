package com.example.eldroid_nullpoint

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.example.eldroid_nullpoint.databinding.ActivityForgotPasswordBinding
import com.example.eldroid_nullpoint.util.AuthErrors
import com.example.eldroid_nullpoint.util.Validators
import com.google.firebase.auth.FirebaseAuth

/**
 * Sends a Firebase Authentication password-reset email so a borrower who is
 * locked out can set a new password from their inbox.
 */
class ForgotPasswordActivity : AppCompatActivity() {

    companion object {
        /** Optional email to pre-fill, passed in from the Login screen. */
        const val EXTRA_EMAIL = "extra_email"
    }

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var auth: FirebaseAuth

    private var isSubmitting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Carry over whatever the borrower already typed on the Login screen.
        intent.getStringExtra(EXTRA_EMAIL)?.takeIf { it.isNotBlank() }?.let {
            binding.etEmail.setText(it)
        }

        binding.ivBack.setOnClickListener { finish() }
        binding.tvBackToLogin.setOnClickListener { finish() }
        binding.btnSendReset.setOnClickListener { attemptReset() }

        // Clear the error as soon as the borrower starts fixing the value.
        binding.etEmail.doOnTextChanged { _, _, _, _ ->
            binding.tvEmailError.visibility = View.GONE
        }
    }

    private fun attemptReset() {
        if (isSubmitting) return

        binding.tvEmailError.visibility = View.GONE
        binding.tvSuccess.visibility = View.GONE

        val email = binding.etEmail.text.toString().trim()

        if (email.isBlank()) {
            showEmailError(getString(R.string.error_email_required))
            return
        }
        if (!Validators.isValidEmail(email)) {
            showEmailError(getString(R.string.error_invalid_email))
            return
        }

        setLoading(true)
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    binding.tvSuccess.text = getString(R.string.reset_email_sent, email)
                    binding.tvSuccess.visibility = View.VISIBLE
                } else {
                    showEmailError(AuthErrors.messageFor(this, task.exception))
                }
            }
    }

    private fun showEmailError(message: String) {
        binding.tvEmailError.text = message
        binding.tvEmailError.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        isSubmitting = loading
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSendReset.isEnabled = !loading
        binding.btnSendReset.text = if (loading) "" else getString(R.string.btn_send_reset_link)
        binding.etEmail.isEnabled = !loading
    }
}
