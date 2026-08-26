package com.example.eldroid_nullpoint.util

import android.util.Patterns

/**
 * Central place for all form-validation rules used across Login and Sign Up.
 */
object Validators {

    /** A strong password: at least 8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special char. */
    private val STRONG_PASSWORD_REGEX =
        Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#\$%^&*()_+\\-={}\\[\\]:;\"'<>,.?/~`|\\\\]).{8,}\$")

    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidName(name: String): Boolean {
        return name.trim().length in 2..40
    }

    fun isStrongPassword(password: String): Boolean {
        return STRONG_PASSWORD_REGEX.matches(password)
    }

    /**
     * Returns a human-readable reason why [password] is not strong, or null if it is valid.
     */
    fun passwordStrengthError(password: String): String? {
        if (password.length < 8) return "Password must be at least 8 characters"
        if (!password.any { it.isUpperCase() }) return "Add at least one uppercase letter"
        if (!password.any { it.isLowerCase() }) return "Add at least one lowercase letter"
        if (!password.any { it.isDigit() }) return "Add at least one number"
        if (password.none { !it.isLetterOrDigit() }) return "Add at least one special character"
        return null
    }
}
