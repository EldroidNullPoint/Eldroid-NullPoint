package com.example.eldroid_nullpoint.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.eldroid_nullpoint.R
import com.example.eldroid_nullpoint.databinding.ItemEquipmentBinding
import com.example.eldroid_nullpoint.model.Equipment
import com.example.eldroid_nullpoint.util.EquipmentImages
import com.example.eldroid_nullpoint.util.TimeFormat

/**
 * Renders SmartDock boxes for the borrower.
 *
 * The status pill is borrower-relative: an item the signed-in borrower is holding
 * reads "Yours" (or "Overdue" once its due time has passed) rather than the raw
 * Firestore "borrowed" status, and no Firestore document IDs are ever shown.
 */
class EquipmentAdapter(
    private val currentUid: String,
    private val onItemClick: (Equipment) -> Unit = {}
) : RecyclerView.Adapter<EquipmentAdapter.EquipmentViewHolder>() {

    private val items = mutableListOf<Equipment>()

    fun submit(newItems: List<Equipment>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EquipmentViewHolder {
        val binding = ItemEquipmentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EquipmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EquipmentViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class EquipmentViewHolder(
        private val binding: ItemEquipmentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(equipment: Equipment) {
            val context = binding.root.context

            binding.root.setOnClickListener { onItemClick(equipment) }

            binding.tvEquipmentName.text = equipment.name.ifBlank { "Unnamed equipment" }

            // Photo resolved from the category/name, falling back to a generic icon.
            EquipmentImages.bindInto(
                binding.ivEquipmentPhoto,
                equipment.name,
                equipment.category
            )

            val boxLabel = context.getString(R.string.box_label, equipment.boxNumber)
            binding.tvEquipmentMeta.text = if (equipment.category.isBlank()) {
                boxLabel
            } else {
                "$boxLabel  ·  ${equipment.category}"
            }

            val isMine = equipment.isBorrowedBy(currentUid)

            // Due times are only the borrower's own business: another borrower's
            // schedule is not shown here.
            if (isMine && equipment.dueAt > 0L) {
                binding.tvEquipmentDue.visibility = View.VISIBLE
                binding.tvEquipmentDue.text =
                    "${TimeFormat.dateTime(equipment.dueAt)}  ·  ${TimeFormat.dueLabel(equipment.dueAt)}"
            } else {
                binding.tvEquipmentDue.visibility = View.GONE
            }

            val statusLabel: String
            val pillBackground: Int
            val pillTextColor: Int

            when {
                // Overdue is a borrower-facing state only for the signed-in
                // borrower's own item; other people's items just read "Borrowed".
                isMine && equipment.isOverdue() -> {
                    statusLabel = context.getString(R.string.status_overdue)
                    pillBackground = R.drawable.bg_pill_overdue
                    pillTextColor = R.color.error_red
                }
                isMine -> {
                    statusLabel = context.getString(R.string.status_yours)
                    pillBackground = R.drawable.bg_pill_yours
                    pillTextColor = R.color.white
                }
                equipment.isBorrowed -> {
                    statusLabel = context.getString(R.string.status_borrowed)
                    pillBackground = R.drawable.bg_pill_neutral
                    pillTextColor = R.color.text_secondary
                }
                else -> {
                    statusLabel = context.getString(R.string.status_available)
                    pillBackground = R.drawable.bg_pill_available
                    pillTextColor = R.color.brand_dark_green
                }
            }

            binding.tvEquipmentStatus.text = statusLabel
            binding.tvEquipmentStatus.setBackgroundResource(pillBackground)
            binding.tvEquipmentStatus.setTextColor(
                androidx.core.content.ContextCompat.getColor(context, pillTextColor)
            )
        }
    }
}
