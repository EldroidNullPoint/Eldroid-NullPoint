package com.example.eldroid_nullpoint.home

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.eldroid_nullpoint.R
import com.example.eldroid_nullpoint.databinding.ItemActivityBinding
import com.example.eldroid_nullpoint.model.EquipmentTransaction

/** Renders [ActivityRow]s for the "Recent activity" list. */
class ActivityAdapter : RecyclerView.Adapter<ActivityAdapter.Holder>() {

    private val items = mutableListOf<ActivityRow>()

    fun submitList(rows: List<ActivityRow>) {
        items.clear()
        items.addAll(rows)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemActivityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    class Holder(private val binding: ItemActivityBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: ActivityRow) {
            binding.tvTitle.text = row.title
            binding.tvSubtitle.text = row.subtitle

            val (icon, tint) = when (row.type) {
                EquipmentTransaction.TYPE_BORROW -> R.drawable.ic_box to R.color.brand_green
                EquipmentTransaction.TYPE_RETURN -> R.drawable.ic_check_circle to R.color.brand_green
                EquipmentTransaction.TYPE_OVERDUE, EquipmentTransaction.TYPE_ALERT ->
                    R.drawable.ic_history to R.color.error_red
                else -> R.drawable.ic_history to R.color.text_secondary
            }
            binding.ivIcon.setImageResource(icon)
            binding.ivIcon.imageTintList =
                ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, tint))
        }
    }
}
