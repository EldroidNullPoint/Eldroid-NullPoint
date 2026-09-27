package com.example.eldroid_nullpoint

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.eldroid_nullpoint.databinding.ActivityEquipmentDetailBinding
import com.example.eldroid_nullpoint.model.Equipment
import com.example.eldroid_nullpoint.mvp.equipmentdetail.EquipmentDetailContract
import com.example.eldroid_nullpoint.mvp.equipmentdetail.EquipmentDetailPresenter
import com.example.eldroid_nullpoint.util.EquipmentImages
import com.example.eldroid_nullpoint.util.SmartDockRepository
import com.example.eldroid_nullpoint.util.TimeFormat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth

class EquipmentDetailActivity : AppCompatActivity(), EquipmentDetailContract.View {

    companion object {
        private const val EXTRA_EQUIPMENT_ID = "extra_equipment_id"

        fun intent(context: Context, equipmentId: String): Intent =
            Intent(context, EquipmentDetailActivity::class.java)
                .putExtra(EXTRA_EQUIPMENT_ID, equipmentId)
    }

    private lateinit var binding: ActivityEquipmentDetailBinding
    private lateinit var presenter: EquipmentDetailPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEquipmentDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val equipmentId = intent.getStringExtra(EXTRA_EQUIPMENT_ID).orEmpty()
        if (equipmentId.isBlank()) {
            finish()
            return
        }

        val user = FirebaseAuth.getInstance().currentUser
        val currentUid = user?.uid.orEmpty()
        val borrowerName = user?.displayName.orEmpty()

        presenter = EquipmentDetailPresenter(
            view = this,
            context = this,
            equipmentId = equipmentId,
            currentUid = currentUid,
            borrowerName = borrowerName
        )
        presenter.loadBorrowerName()

        binding.ivBack.setOnClickListener { finish() }
        binding.btnBorrow.setOnClickListener { confirmBorrow() }
        binding.btnReturn.setOnClickListener { confirmReturn() }
    }

    override fun onStart() {
        super.onStart()
        presenter.onStart()
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.detach()
    }

    private fun confirmBorrow() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.borrow_confirm_title, binding.tvName.text.toString()))
            .setMessage(getString(R.string.borrow_confirm_message, binding.tvBox.text.toString()))
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_confirm) { _, _ -> presenter.onBorrowConfirmed() }
            .show()
    }

    private fun confirmReturn() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.return_confirm_title, binding.tvName.text.toString()))
            .setMessage(getString(R.string.return_confirm_message, binding.tvBox.text.toString()))
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_confirm) { _, _ -> presenter.onReturnConfirmed() }
            .show()
    }

    // ---------------------------------------------------------------
    // EquipmentDetailContract.View
    // ---------------------------------------------------------------

    override fun renderEquipment(equipment: Equipment, currentUid: String) {
        val isMine = equipment.isBorrowedBy(currentUid)
        val isOverdue = equipment.isOverdue()

        binding.tvName.text = equipment.name.ifBlank { getString(R.string.title_equipment_detail) }
        EquipmentImages.bindInto(binding.ivPhoto, equipment.name, equipment.category, fallbackPaddingDp = 60)

        val (statusLabel, pillBackground, pillTextColor) = when {
            isMine && isOverdue -> Triple(
                getString(R.string.status_overdue),
                R.drawable.bg_pill_overdue,
                R.color.error_red
            )
            isMine -> Triple(
                getString(R.string.status_yours),
                R.drawable.bg_pill_yours,
                R.color.white
            )
            equipment.isBorrowed -> Triple(
                getString(R.string.status_borrowed),
                R.drawable.bg_pill_neutral,
                R.color.text_secondary
            )
            else -> Triple(
                getString(R.string.status_available),
                R.drawable.bg_pill_available,
                R.color.brand_dark_green
            )
        }
        binding.tvStatusPill.text = statusLabel
        binding.tvStatusPill.setBackgroundResource(pillBackground)
        binding.tvStatusPill.setTextColor(ContextCompat.getColor(this, pillTextColor))

        binding.tvHint.text = when {
            isMine && isOverdue -> getString(R.string.detail_hint_overdue, equipment.boxNumber)
            isMine -> getString(R.string.detail_hint_return, equipment.boxNumber)
            equipment.isBorrowed -> getString(R.string.detail_hint_unavailable, equipment.boxNumber)
            else -> getString(R.string.detail_hint_borrow, equipment.boxNumber)
        }

        binding.rowCategory.visibility = if (equipment.category.isBlank()) View.GONE else View.VISIBLE
        binding.tvCategory.text = equipment.category
        binding.tvBox.text = getString(R.string.box_label, equipment.boxNumber)

        if (equipment.isBorrowed) {
            binding.rowHolder.visibility = View.VISIBLE
            binding.tvHolder.text = if (isMine) {
                getString(R.string.detail_holder_you)
            } else {
                getString(R.string.detail_holder_other)
            }
        } else {
            binding.rowHolder.visibility = View.GONE
        }

        if (isMine && equipment.borrowedAt > 0L) {
            binding.rowBorrowedAt.visibility = View.VISIBLE
            binding.tvBorrowedAt.text = TimeFormat.dateTime(equipment.borrowedAt)
        } else {
            binding.rowBorrowedAt.visibility = View.GONE
        }

        if (isMine && equipment.dueAt > 0L) {
            binding.rowDueAt.visibility = View.VISIBLE
            binding.tvDueAt.text =
                "${TimeFormat.dateTime(equipment.dueAt)}  ·  ${TimeFormat.dueLabel(equipment.dueAt)}"
            binding.tvDueAt.setTextColor(
                ContextCompat.getColor(this, if (isOverdue) R.color.error_red else R.color.text_primary)
            )
        } else {
            binding.rowDueAt.visibility = View.GONE
        }
    }

    override fun showBorrowButton(visible: Boolean) {
        binding.btnBorrow.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun showReturnButton(visible: Boolean) {
        binding.btnReturn.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun showActionArea(visible: Boolean) {
        binding.actionArea.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun setSubmitting(submitting: Boolean) {
        binding.progressAction.visibility = if (submitting) View.VISIBLE else View.GONE
        binding.btnBorrow.isEnabled = !submitting
        binding.btnReturn.isEnabled = !submitting
        binding.btnBorrow.alpha = if (submitting) 0.6f else 1f
        binding.btnReturn.alpha = if (submitting) 0.6f else 1f
    }

    override fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun navigateToBorrowConfirmation(receipt: SmartDockRepository.BorrowReceipt) {
        startActivity(BorrowConfirmationActivity.intent(this, receipt))
    }

    override fun close() {
        finish()
    }
}
