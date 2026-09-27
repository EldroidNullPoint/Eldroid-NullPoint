package com.example.eldroid_nullpoint

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.eldroid_nullpoint.databinding.ActivityExtensionRequestBinding
import com.example.eldroid_nullpoint.model.ExtensionRequest
import com.example.eldroid_nullpoint.mvp.extension.ExtensionRequestContract
import com.example.eldroid_nullpoint.mvp.extension.ExtensionRequestPresenter
import com.example.eldroid_nullpoint.util.TimeFormat
import com.google.firebase.auth.FirebaseAuth

class ExtensionRequestActivity : AppCompatActivity(), ExtensionRequestContract.View {

    companion object {
        private const val EXTRA_EQUIPMENT_ID   = "extra_equipment_id"
        private const val EXTRA_EQUIPMENT_NAME = "extra_equipment_name"
        private const val EXTRA_TRANSACTION_ID = "extra_transaction_id"
        private const val EXTRA_CURRENT_DUE_AT = "extra_current_due_at"

        fun intent(
            context: Context,
            equipmentId: String,
            equipmentName: String,
            transactionId: String,
            currentDueAt: Long
        ): Intent = Intent(context, ExtensionRequestActivity::class.java)
            .putExtra(EXTRA_EQUIPMENT_ID,   equipmentId)
            .putExtra(EXTRA_EQUIPMENT_NAME, equipmentName)
            .putExtra(EXTRA_TRANSACTION_ID, transactionId)
            .putExtra(EXTRA_CURRENT_DUE_AT, currentDueAt)
    }

    private lateinit var binding: ActivityExtensionRequestBinding
    private lateinit var presenter: ExtensionRequestContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExtensionRequestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val equipmentId   = intent.getStringExtra(EXTRA_EQUIPMENT_ID).orEmpty()
        val equipmentName = intent.getStringExtra(EXTRA_EQUIPMENT_NAME).orEmpty()
        val transactionId = intent.getStringExtra(EXTRA_TRANSACTION_ID).orEmpty()
        val currentDueAt  = intent.getLongExtra(EXTRA_CURRENT_DUE_AT, 0L)
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

        if (equipmentId.isBlank() || uid.isBlank()) {
            finish()
            return
        }

        presenter = ExtensionRequestPresenter(
            view          = this,
            equipmentId   = equipmentId,
            equipmentName = equipmentName,
            transactionId = transactionId,
            currentDueAt  = currentDueAt,
            uid           = uid
        )

        binding.ivBack.setOnClickListener { finish() }

        binding.btnSubmit.setOnClickListener {
            val minutes = when (binding.rgDuration.checkedRadioButtonId) {
                R.id.rb60min -> 60
                else         -> 30
            }
            val reason = binding.etReason.text?.toString().orEmpty()
            binding.tvReasonError.visibility = View.GONE
            presenter.onSubmitClicked(minutes, reason)
        }
    }

    override fun onStart() {
        super.onStart()
        presenter.onStart()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.detach()
    }

    // ---------------------------------------------------------------
    // ExtensionRequestContract.View
    // ---------------------------------------------------------------

    override fun showLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    override fun showSubmitting(submitting: Boolean) {
        binding.btnSubmit.isEnabled  = !submitting
        binding.progressSubmit.visibility = if (submitting) View.VISIBLE else View.GONE
        binding.btnSubmit.alpha = if (submitting) 0.6f else 1f
    }

    override fun showEquipmentInfo(name: String, dueAt: Long) {
        binding.tvEquipmentName.text = name.ifBlank { getString(R.string.label_item) }
        val dueText = if (dueAt > 0L) {
            val remaining = TimeFormat.dueLabel(dueAt)
            "Due: ${TimeFormat.dateTime(dueAt)}  ·  $remaining"
        } else {
            ""
        }
        binding.tvCurrentDue.text = dueText
    }

    /**
     * Spec §15 — renders the existing request status.
     * When a pending request exists, hide the form and show the status card.
     * When approved/rejected, show the decision and keep the form hidden.
     */
    override fun renderExistingRequest(request: ExtensionRequest?) {
        if (request == null) {
            binding.cardExistingRequest.visibility = View.GONE
            binding.formContainer.visibility = View.VISIBLE
            return
        }

        binding.cardExistingRequest.visibility = View.VISIBLE
        binding.formContainer.visibility = View.GONE

        val (statusText, detail, background) = when (request.status) {
            ExtensionRequest.STATUS_PENDING -> Triple(
                "Extension request pending",
                getString(R.string.extension_pending_notice),
                R.drawable.bg_pill_due_soon
            )
            ExtensionRequest.STATUS_APPROVED -> Triple(
                "Extension approved",
                getString(R.string.extension_approved_notice,
                    TimeFormat.dateTime(request.resultingDueAt)),
                R.drawable.bg_pill_available
            )
            ExtensionRequest.STATUS_REJECTED -> Triple(
                "Extension declined",
                getString(R.string.extension_rejected_notice,
                    TimeFormat.dateTime(request.originalDueAt)),
                R.drawable.bg_pill_overdue
            )
            else -> Triple("Extension request ${request.status}", "", R.drawable.bg_pill_neutral)
        }

        binding.tvExistingRequestStatus.text = statusText
        binding.tvExistingRequestDetail.text = detail
        binding.cardExistingRequest.setBackgroundResource(background)
    }

    override fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun showError(message: String) {
        if (message.contains("reason", ignoreCase = true)) {
            binding.tvReasonError.text = message
            binding.tvReasonError.visibility = View.VISIBLE
        } else {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun close() {
        finish()
    }
}
