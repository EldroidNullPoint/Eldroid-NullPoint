package com.example.eldroid_nullpoint.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.eldroid_nullpoint.R
import com.example.eldroid_nullpoint.databinding.ItemTransactionBinding
import com.example.eldroid_nullpoint.model.Transaction
import com.example.eldroid_nullpoint.util.EquipmentImages
import com.example.eldroid_nullpoint.util.TimeFormat

/** Renders the borrower's own recent SmartDock activity, newest first. */
class TransactionAdapter(
    private val onItemClick: (Transaction) -> Unit = {}
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private val items = mutableListOf<Transaction>()

    fun submit(newItems: List<Transaction>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            val context = binding.root.context
            val equipmentName = transaction.equipmentName.ifBlank { "equipment" }

            binding.root.setOnClickListener { onItemClick(transaction) }

            val titleRes = when (transaction.type) {
                Transaction.TYPE_BORROW -> R.string.activity_borrowed
                Transaction.TYPE_RETURN -> R.string.activity_returned
                Transaction.TYPE_OVERDUE -> R.string.activity_overdue
                else -> R.string.activity_alert
            }
            binding.tvActivityTitle.text = context.getString(titleRes, equipmentName)

            val boxLabel = context.getString(R.string.box_label, transaction.boxNumber)
            binding.tvActivityMeta.text =
                "$boxLabel  ·  ${TimeFormat.dateTime(transaction.timestamp)}"

            // Overdue and alert entries get the warning treatment so they stand out;
            // ordinary borrow/return rows show the equipment photo instead.
            val isWarning = transaction.type == Transaction.TYPE_OVERDUE ||
                    transaction.type == Transaction.TYPE_ALERT
            if (isWarning) {
                binding.ivActivityIcon.setBackgroundResource(R.drawable.bg_circle_overdue)
                binding.ivActivityIcon.setImageResource(R.drawable.ic_alert)
                binding.ivActivityIcon.scaleType = ImageView.ScaleType.FIT_CENTER
                val padding = (11 * context.resources.displayMetrics.density).toInt()
                binding.ivActivityIcon.setPadding(padding, padding, padding, padding)
            } else {
                binding.ivActivityIcon.setBackgroundResource(R.drawable.bg_thumbnail)
                // Transactions carry no category, so the name alone drives the match.
                EquipmentImages.bindInto(
                    binding.ivActivityIcon,
                    transaction.equipmentName,
                    category = "",
                    fallbackPaddingDp = 11
                )
            }
            binding.tvActivityTitle.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (isWarning) R.color.error_red else R.color.text_primary
                )
            )
        }
    }
}
