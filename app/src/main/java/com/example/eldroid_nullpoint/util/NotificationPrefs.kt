package com.example.eldroid_nullpoint.util

import android.content.Context

/**
 * Local cache of the borrower's "due-date reminders" preference.
 *
 * The source of truth is `users/{uid}.notificationsEnabled` in Firestore (edited on
 * the Profile screen); this copy lets the background worker decide whether to post
 * a system notification without an extra network read.
 */
object NotificationPrefs {

    private const val PREFS_NAME = "smartdock_notifications"
    private const val KEY_ENABLED = "reminders_enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }
}
