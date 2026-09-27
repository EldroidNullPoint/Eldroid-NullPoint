package com.example.eldroid_nullpoint

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.eldroid_nullpoint.databinding.ActivityTermsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Displays the SmartDock Borrower Terms and Conditions.
 *
 * Two modes:
 *  - AGREEMENT mode (from signup): shows Accept / Decline buttons at the bottom.
 *    The Accept button is locked until the user ticks the checkbox.
 *    Returns RESULT_OK on accept, RESULT_CANCELED on decline.
 *  - READ-ONLY mode (from Profile): shows the header back button only, no bottom bar.
 */
class TermsActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_MODE = "extra_mode"
        const val MODE_AGREEMENT = "agreement"   // shown during signup flow
        const val MODE_READ_ONLY = "read_only"   // shown from profile / info

        fun intentAgreement(context: Context): Intent =
            Intent(context, TermsActivity::class.java)
                .putExtra(EXTRA_MODE, MODE_AGREEMENT)

        fun intentReadOnly(context: Context): Intent =
            Intent(context, TermsActivity::class.java)
                .putExtra(EXTRA_MODE, MODE_READ_ONLY)
    }

    private lateinit var binding: ActivityTermsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTermsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_READ_ONLY

        binding.ivBack.setOnClickListener {
            if (mode == MODE_AGREEMENT) {
                // Back = decline in agreement mode
                confirmDecline()
            } else {
                finish()
            }
        }

        if (mode == MODE_AGREEMENT) {
            setupAgreementMode()
        }
        // Read-only: bottom bar stays GONE (default in layout)
    }

    private fun setupAgreementMode() {
        binding.bottomActions.visibility = View.VISIBLE

        // Accept button stays disabled until checkbox is ticked
        binding.cbAgree.setOnCheckedChangeListener { _, checked ->
            binding.btnAccept.isEnabled = checked
            binding.btnAccept.alpha = if (checked) 1f else 0.5f
        }

        binding.btnAccept.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }

        binding.btnDecline.setOnClickListener {
            confirmDecline()
        }
    }

    private fun confirmDecline() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.btn_decline_terms)
            .setMessage(R.string.terms_decline_message)
            .setPositiveButton(R.string.btn_decline_terms) { _, _ ->
                setResult(RESULT_CANCELED)
                finish()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }
}
