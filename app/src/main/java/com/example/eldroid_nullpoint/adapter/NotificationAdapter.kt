package com.example.eldroid_nullpoint.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.eldroid_nullpoint.R
import com.example.eldroid_nullpoint.databinding.ItemNotificationBinding
import com.example.eldroid_nullpoint.model.AppNotification
import com.example.eldroid_nullpoint.util.TimeFormat

/** Renders the borrower's inbox, newest first; unread rows are emphasised. */
class NotificationAdapter(
    private val onItemClick: (AppNotification) -> Unit = {}
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    private val items = mutableListOf<AppNotification>()

    fun submit(newItems: List<AppNotification>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class NotificationViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: AppNotification) {
            val context = binding.root.context

            binding.root.setOnClickListener { onItemClick(notification) }

            binding.tvNotificationTitle.text = notification.title
            binding.tvNotificationBody.text = notification.body

            val typeLabelRes = when (notification.type) {
                AppNotification.TYPE_BORROW -> R.string.notification_type_borrow
                AppNotification.TYPE_RETURN -> R.string.notification_type_return
                AppNotification.TYPE_DUE_SOON -> R.string.notification_type_due_soon
                else -> R.string.notification_type_overdue
            }
            binding.tvNotificationMeta.text =
                "${context.getString(typeLabelRes)}  ·  ${TimeFormat.dateTime(notification.createdAt)}"

            // Icon and tint follow the urgency: overdue is red, reminders amber-ish
            // via the clock, confirmations use the box icon.
            val (icon, background, tint) = when (notification.type) {
                AppNotification.TYPE_OVERDUE ->
                    Triple(R.drawable.ic_alert, R.drawable.bg_circle_overdue, R.color.error_red)
                AppNotification.TYPE_DUE_SOON ->
                    Triple(R.drawable.ic_clock, R.drawable.bg_circle_light, R.color.warning_amber)
                else ->
                    Triple(R.drawable.ic_box, R.drawable.bg_circle_light, R.color.brand_dark_green)
            }
            binding.ivNotificationIcon.setImageResource(icon)
            binding.ivNotificationIcon.setBackgroundResource(background)
            binding.ivNotificationIcon.imageTintList =
                ContextCompat.getColorStateList(context, tint)

            // Unread: bold title, dot and a white card; read: muted card, normal weight.
            binding.unreadDot.visibility = if (notification.read) View.INVISIBLE else View.VISIBLE
            binding.tvNotificationTitle.setTypeface(
                null,
                if (notification.read) android.graphics.Typeface.NORMAL
                else android.graphics.Typeface.BOLD
            )
            binding.rowRoot.setBackgroundResource(
                if (notification.read) R.drawable.bg_card_muted else R.drawable.bg_card
            )
        }
    }
}
