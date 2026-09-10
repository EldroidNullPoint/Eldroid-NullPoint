package com.example.eldroid_nullpoint.util

import android.util.Patterns

/**
 * Central place for all form-validation rules used across Login, Sign Up,
 * Forgot Password and Change Password.
 */
object Validators {

    /**
     * A strong password: at least 8 chars, 1 uppercase, 1 lowercase, 1 digit,
     * 1 special char and no whitespace anywhere.
     */
    private val STRONG_PASSWORD_REGEX =
        Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#\$%^&*()_+\\-={}\\[\\]:;\"'<>,.?/~`|\\\\])(?!.*\\s).{8,}\$")

    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * A usable person name: 2-40 characters that contain at least one letter, so
     * values made up purely of digits or symbols ("123", "---") are rejected.
     */
    fun isValidName(name: String): Boolean {
        val trimmed = name.trim()
        return trimmed.length in 2..40 && trimmed.any { it.isLetter() }
    }

    fun isStrongPassword(password: String): Boolean {
        return STRONG_PASSWORD_REGEX.matches(password)
    }

    /**
     * Returns a human-readable reason why [password] is not strong, or null if it is valid.
     * The order matters: the borrower is told about one fixable problem at a time.
     */
    fun passwordStrengthError(password: String): String? {
        if (password.isBlank()) return "Password is required"
        if (password.any { it.isWhitespace() }) return "Password cannot contain spaces"
        if (password.length < 8) return "Password must be at least 8 characters"
        if (!password.any { it.isUpperCase() }) return "Add at least one uppercase letter"
        if (!password.any { it.isLowerCase() }) return "Add at least one lowercase letter"
        if (!password.any { it.isDigit() }) return "Add at least one number"
        if (password.none { !it.isLetterOrDigit() }) return "Add at least one special character"
        return null
    }
}
