package com.example.eldroid_nullpoint.util

import android.content.Context

/**
 * Remembers whether the borrower has already walked through the "How it works"
 * onboarding flow.
 *
 * Nothing bypasses onboarding on this flag yet - Landing's "Get Started" always
 * opens the flow. It is written now so that skipping the tutorial for returning
 * users is a one-line change later.
 */
object OnboardingPrefs {

    private const val PREFS_NAME = "smartdock_onboarding"
    private const val KEY_COMPLETED = "onboarding_completed"

    fun isCompleted(context: Context): Boolean =
        prefs(context).getBoolean(KEY_COMPLETED, false)

    fun setCompleted(context: Context) {
        prefs(context).edit().putBoolean(KEY_COMPLETED, true).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
