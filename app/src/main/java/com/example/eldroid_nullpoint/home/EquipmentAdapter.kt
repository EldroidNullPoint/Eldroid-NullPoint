package com.example.eldroid_nullpoint.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.eldroid_nullpoint.R
import com.example.eldroid_nullpoint.databinding.ItemEquipmentBinding

/** Renders [EquipmentRow]s for both the "My borrowed items" and "Equipment availability" lists. */
class EquipmentAdapter : RecyclerView.Adapter<EquipmentAdapter.Holder>() {

    private val items = mutableListOf<EquipmentRow>()

    fun submitList(rows: List<EquipmentRow>) {
        items.clear()
        items.addAll(rows)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemEquipmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    class Holder(private val binding: ItemEquipmentBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: EquipmentRow) {
            binding.tvBox.text = row.boxLabel
            binding.tvName.text = row.name
            binding.tvDetail.text = row.detail
            binding.tvBadge.text = row.badge

            val (background, textColor) = when (row.status) {
                EquipmentStatus.AVAILABLE -> R.drawable.bg_chip_available to R.color.brand_dark_green
                EquipmentStatus.BORROWED -> R.drawable.bg_chip_borrowed to R.color.status_amber
                EquipmentStatus.OVERDUE -> R.drawable.bg_chip_overdue to R.color.error_red
            }
            binding.tvBadge.setBackgroundResource(background)
            binding.tvBadge.setTextColor(ContextCompat.getColor(binding.root.context, textColor))
        }
    }
}
