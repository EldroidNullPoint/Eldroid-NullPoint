package com.example.eldroid_nullpoint.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.eldroid_nullpoint.EquipmentDetailActivity
import com.example.eldroid_nullpoint.R
import com.example.eldroid_nullpoint.model.AppNotification

/** Posts FR-06 due-soon / overdue reminders as system notifications. */
object Notifier {

    private const val CHANNEL_ID = "smartdock_due_dates"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    /** True when the OS will actually display a notification from this app. */
    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun show(context: Context, notification: AppNotification) {
        if (!canPost(context)) return
        ensureChannel(context)

        val tapIntent = if (notification.equipmentId.isNotBlank()) {
            EquipmentDetailActivity.intent(context, notification.equipmentId)
        } else {
            context.packageManager.getLaunchIntentForPackage(context.packageName)
        }
        val pendingIntent = tapIntent?.let {
            PendingIntent.getActivity(
                context,
                notification.id.hashCode(),
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val isOverdue = notification.type == AppNotification.TYPE_OVERDUE
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(if (isOverdue) R.drawable.ic_alert else R.drawable.ic_clock)
            .setColor(ContextCompat.getColor(context, R.color.brand_green))
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        if (pendingIntent != null) builder.setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context)
                .notify(notification.id.hashCode(), builder.build())
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post - nothing to do.
        }
    }
}
