package com.example.eldroid_nullpoint.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Formatting helpers for the epoch-millis timestamps the SmartDock tower writes
 * to Firestore (`borrowedAt`, `dueAt`, `transactions.timestamp`).
 */
object TimeFormat {

    private const val DUE_SOON_WINDOW_MS = 60 * 60 * 1000L // 1 hour

    private val dateTimeFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

    /** "Sep 10, 3:12 PM", or an em dash when the tower has not set the field yet. */
    fun dateTime(epochMillis: Long): String =
        if (epochMillis <= 0L) "—" else dateTimeFormat.format(Date(epochMillis))

    /** Time-of-day aware greeting prefix, e.g. "Good morning". */
    fun greetingHour(calendar: Calendar = Calendar.getInstance()): Greeting =
        when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> Greeting.MORNING
            in 12..17 -> Greeting.AFTERNOON
            else -> Greeting.EVENING
        }

    enum class Greeting { MORNING, AFTERNOON, EVENING }

    /**
     * How a borrowed item reads on the dashboard relative to [now].
     * [OVERDUE] gets the loudest treatment, [DUE_SOON] the amber one.
     */
    enum class DueState { NO_DUE_DATE, DUE_LATER, DUE_SOON, OVERDUE }

    fun dueState(dueAt: Long, now: Long = System.currentTimeMillis()): DueState = when {
        dueAt <= 0L -> DueState.NO_DUE_DATE
        now > dueAt -> DueState.OVERDUE
        dueAt - now <= DUE_SOON_WINDOW_MS -> DueState.DUE_SOON
        else -> DueState.DUE_LATER
    }

    /**
     * Short human label for a due time: "Due in 3h 20m", "Due soon",
     * "Overdue by 2d 4h", or "No due time" when the tower left `dueAt` at 0.
     */
    fun dueLabel(dueAt: Long, now: Long = System.currentTimeMillis()): String =
        when (dueState(dueAt, now)) {
            DueState.NO_DUE_DATE -> "No due time"
            DueState.OVERDUE -> "Overdue by ${duration(now - dueAt)}"
            DueState.DUE_SOON -> "Due soon"
            DueState.DUE_LATER -> "Due in ${duration(dueAt - now)}"
        }

    /** Compact duration: "2d 4h", "3h 20m", "12m", "just now". */
    fun duration(millis: Long): String {
        if (millis < TimeUnit.MINUTES.toMillis(1)) return "less than a minute"
        val days = TimeUnit.MILLISECONDS.toDays(millis)
        val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return when {
            days > 0 -> if (hours > 0) "${days}d ${hours}h" else "${days}d"
            hours > 0 -> if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
            else -> "${minutes}m"
        }
    }
}
