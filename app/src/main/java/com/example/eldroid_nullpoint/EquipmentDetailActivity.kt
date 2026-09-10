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
import com.example.eldroid_nullpoint.util.DemoData
import com.example.eldroid_nullpoint.util.EquipmentImages
import com.example.eldroid_nullpoint.util.TimeFormat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Detail view for a single SmartDock box.
 *
 * Covers FR-05 (item name, borrow time, required return time and box location) and
 * keeps a live listener on the document so the screen follows the tower in real
 * time (FR-09/FR-10). Read-only by design: the borrow and return actions belong to
 * the RFID tap at the tower, never to the phone.
 */
class EquipmentDetailActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_EQUIPMENT_ID = "extra_equipment_id"

        fun intent(context: Context, equipmentId: String): Intent =
            Intent(context, EquipmentDetailActivity::class.java)
                .putExtra(EXTRA_EQUIPMENT_ID, equipmentId)
    }

    private lateinit var binding: ActivityEquipmentDetailBinding
    private lateinit var firestore: FirebaseFirestore

    private var listener: ListenerRegistration? = null
    private var equipmentId: String = ""
    private var currentUid: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEquipmentDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        equipmentId = intent.getStringExtra(EXTRA_EQUIPMENT_ID).orEmpty()
        if (equipmentId.isBlank()) {
            finish()
            return
        }

        firestore = FirebaseFirestore.getInstance()
        currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

        binding.ivBack.setOnClickListener { finish() }
    }

    override fun onStart() {
        super.onStart()
        if (equipmentId.isBlank()) return

        // Placeholder rows have no Firestore document behind them, so they render
        // straight from DemoData instead of opening a listener that would 404.
        if (DemoData.isDemoId(equipmentId)) {
            val demo = DemoData.equipmentById(equipmentId, currentUid)
            if (demo == null) {
                finish()
            } else {
                render(demo)
            }
            return
        }

        listener = firestore.collection(Equipment.COLLECTION).document(equipmentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(
                        this,
                        getString(R.string.error_load_dashboard),
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) {
                    Toast.makeText(
                        this,
                        getString(R.string.detail_not_found),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    return@addSnapshotListener
                }
                render(Equipment.from(snapshot))
            }
    }

    override fun onStop() {
        super.onStop()
        listener?.remove()
        listener = null
    }

    private fun render(equipment: Equipment) {
        val isMine = equipment.isBorrowedBy(currentUid)
        val isOverdue = equipment.isOverdue()

        binding.tvName.text = equipment.name.ifBlank { getString(R.string.title_equipment_detail) }
        EquipmentImages.bindInto(
            binding.ivPhoto,
            equipment.name,
            equipment.category,
            fallbackPaddingDp = 60
        )

        // Status pill mirrors the dashboard wording exactly, so the two never disagree.
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

        // Tells the borrower what to physically do at the tower next.
        binding.tvHint.text = when {
            isMine && isOverdue -> getString(R.string.detail_hint_overdue, equipment.boxNumber)
            isMine -> getString(R.string.detail_hint_return, equipment.boxNumber)
            equipment.isBorrowed -> getString(R.string.detail_hint_unavailable, equipment.boxNumber)
            else -> getString(R.string.detail_hint_borrow, equipment.boxNumber)
        }

        binding.rowCategory.visibility = if (equipment.category.isBlank()) View.GONE else View.VISIBLE
        binding.tvCategory.text = equipment.category

        binding.tvBox.text = getString(R.string.box_label, equipment.boxNumber)

        // Who is holding it. Another borrower's name is never shown - that is
        // administrator information, so it stays deliberately vague here.
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

        // Times are only meaningful for the borrower's own active loan.
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
                ContextCompat.getColor(
                    this,
                    if (isOverdue) R.color.error_red else R.color.text_primary
                )
            )
        } else {
            binding.rowDueAt.visibility = View.GONE
        }
    }
}
