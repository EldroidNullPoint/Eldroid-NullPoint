package com.example.eldroid_nullpoint

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.eldroid_nullpoint.databinding.ActivityBorrowConfirmationBinding
import com.example.eldroid_nullpoint.util.EquipmentImages
import com.example.eldroid_nullpoint.util.SmartDockRepository
import com.example.eldroid_nullpoint.util.TimeFormat

/**
 * FR-05 borrow confirmation receipt. Purely a View — the borrow has already been
 * committed by EquipmentDetailPresenter. This screen only renders the extras.
 */
class BorrowConfirmationActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_EQUIPMENT_ID = "extra_equipment_id"
        private const val EXTRA_NAME = "extra_name"
        private const val EXTRA_CATEGORY = "extra_category"
        private const val EXTRA_BOX = "extra_box"
        private const val EXTRA_BORROWED_AT = "extra_borrowed_at"
        private const val EXTRA_DUE_AT = "extra_due_at"

        fun intent(context: Context, receipt: SmartDockRepository.BorrowReceipt): Intent =
            Intent(context, BorrowConfirmationActivity::class.java)
                .putExtra(EXTRA_EQUIPMENT_ID, receipt.equipment.id)
                .putExtra(EXTRA_NAME, receipt.equipment.name)
                .putExtra(EXTRA_CATEGORY, receipt.equipment.category)
                .putExtra(EXTRA_BOX, receipt.equipment.boxNumber)
                .putExtra(EXTRA_BORROWED_AT, receipt.borrowedAt)
                .putExtra(EXTRA_DUE_AT, receipt.dueAt)
    }

    private lateinit var binding: ActivityBorrowConfirmationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBorrowConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val equipmentId = intent.getStringExtra(EXTRA_EQUIPMENT_ID).orEmpty()
        val name = intent.getStringExtra(EXTRA_NAME).orEmpty()
        val category = intent.getStringExtra(EXTRA_CATEGORY).orEmpty()
        val boxNumber = intent.getIntExtra(EXTRA_BOX, 0)
        val borrowedAt = intent.getLongExtra(EXTRA_BORROWED_AT, 0L)
        val dueAt = intent.getLongExtra(EXTRA_DUE_AT, 0L)

        EquipmentImages.bindInto(binding.ivPhoto, name, category, fallbackPaddingDp = 40)
        binding.tvItemName.text = name.ifBlank { getString(R.string.label_item) }
        binding.tvBox.text = getString(R.string.box_label, boxNumber)
        binding.tvBorrowedAt.text = TimeFormat.dateTime(borrowedAt)
        binding.tvDueAt.text = TimeFormat.dateTime(dueAt)

        binding.btnBackToDashboard.setOnClickListener { goHome() }

        // Back also goes to the dashboard so flow reads: Borrow → Confirmed → Home.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })

        binding.btnViewItem.setOnClickListener {
            if (equipmentId.isNotBlank()) {
                startActivity(EquipmentDetailActivity.intent(this, equipmentId))
            }
            finish()
        }
    }

    private fun goHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
}
