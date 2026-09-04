package com.example.eldroid_nullpoint.util

/**
 * Central place for all form-validation rules used by the presenters.
 *
 * Every function returns a human-readable error message, or null when the input
 * is valid. This file deliberately has NO Android imports (no android.util.Patterns)
 * so the rules run in plain JVM unit tests.
 */
object Validators {

    const val PASSWORD_MIN_LENGTH = 10
    const val PASSWORD_MAX_LENGTH = 64
    const val NAME_MIN_LENGTH = 2
    const val NAME_MAX_LENGTH = 40
    private const val EMAIL_MAX_LENGTH = 254

    /** RFC-5322-ish practical email pattern: local@domain.tld with a 2+ letter TLD. */
    private val EMAIL_REGEX =
        Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")

    /** Letters (any language) separated by single spaces, hyphens or apostrophes. */
    private val NAME_REGEX = Regex("^\\p{L}+(?:[ '\\-]\\p{L}+)*$")

    /**
     * Words that make a password guessable even when it satisfies the character rules.
     * Checked against a leet-speak-normalised, letters-only version of the password.
     */
    private val COMMON_PASSWORD_WORDS = listOf(
        "password", "passwd", "qwerty", "letmein", "iloveyou", "changeme", "welcome"
    )

    /** Keyboard rows used to detect lazy sequences such as "qwer" or "asdf". */
    private val KEYBOARD_ROWS = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")

    private const val MAX_REPEAT_RUN = 2       // "aaa" is rejected, "aa" is fine
    private const val SEQUENCE_LENGTH = 4      // "1234" / "abcd" / "qwer" are rejected

    // ---------------------------------------------------------------
    // Email
    // ---------------------------------------------------------------

    fun emailError(email: String): String? {
        val value = email.trim()
        if (value.isEmpty()) return "Email is required"
        if (value.length > EMAIL_MAX_LENGTH) return "Email is too long"
        if (value.contains("..")) return "Email cannot contain consecutive dots"
        if (!EMAIL_REGEX.matches(value)) return "Enter a valid email address"
        val localPart = value.substringBefore('@')
        if (localPart.startsWith('.') || localPart.endsWith('.')) return "Enter a valid email address"
        return null
    }

    fun isValidEmail(email: String): Boolean = emailError(email) == null

    // ---------------------------------------------------------------
    // Names
    // ---------------------------------------------------------------

    fun nameError(name: String, fieldLabel: String): String? {
        val value = name.trim()
        if (value.isEmpty()) return "$fieldLabel is required"
        if (value.length < NAME_MIN_LENGTH) return "$fieldLabel must be at least $NAME_MIN_LENGTH characters"
        if (value.length > NAME_MAX_LENGTH) return "$fieldLabel must be $NAME_MAX_LENGTH characters or fewer"
        if (value.any { it.isDigit() }) return "$fieldLabel cannot contain numbers"
        if (!NAME_REGEX.matches(value)) {
            return "$fieldLabel can only contain letters, single spaces, hyphens and apostrophes"
        }
        return null
    }

    fun isValidName(name: String): Boolean = nameError(name, "Name") == null

    // ---------------------------------------------------------------
    // Passwords
    // ---------------------------------------------------------------

    /** Login only checks presence; complexity rules apply when a password is created. */
    fun loginPasswordError(password: String): String? =
        if (password.isEmpty()) "Password is required" else null

    /**
     * Strong-password rule used by Register and Change Password.
     *
     * Requirements, in the order they are reported:
     *  1. present, no whitespace, 10..64 characters
     *  2. at least one uppercase, one lowercase, one digit, one special character
     *  3. no character repeated 3+ times in a row (e.g. "aaa", "111")
     *  4. no 4-character straight sequence (e.g. "1234", "abcd", "dcba", "qwer")
     *  5. not based on a common password word (password, qwerty, letmein, ...)
     *  6. does not contain the user's first name, last name or email username
     */
    fun passwordError(
        password: String,
        email: String = "",
        firstName: String = "",
        lastName: String = ""
    ): String? {
        if (password.isEmpty()) return "Password is required"
        if (password.any { it.isWhitespace() }) return "Password cannot contain spaces"
        if (password.length < PASSWORD_MIN_LENGTH) {
            return "Password must be at least $PASSWORD_MIN_LENGTH characters"
        }
        if (password.length > PASSWORD_MAX_LENGTH) {
            return "Password must be $PASSWORD_MAX_LENGTH characters or fewer"
        }
        if (password.none { it.isUpperCase() }) return "Add at least one uppercase letter"
        if (password.none { it.isLowerCase() }) return "Add at least one lowercase letter"
        if (password.none { it.isDigit() }) return "Add at least one number"
        if (password.none { !it.isLetterOrDigit() }) {
            return "Add at least one special character (e.g. ! @ # \$ %)"
        }
        if (hasRepeatedRun(password)) {
            return "Password cannot repeat the same character ${MAX_REPEAT_RUN + 1} or more times in a row"
        }
        if (hasSequentialRun(password)) {
            return "Password cannot contain sequences like 1234, abcd or qwer"
        }
        if (isCommonPassword(password)) return "This password is too common or easy to guess"
        if (containsPersonalInfo(password, email, firstName, lastName)) {
            return "Password must not contain your name or email"
        }
        return null
    }

    fun isStrongPassword(password: String): Boolean = passwordError(password) == null

    fun confirmPasswordError(password: String, confirmPassword: String): String? = when {
        confirmPassword.isEmpty() -> "Please confirm your password"
        confirmPassword != password -> "Passwords do not match"
        else -> null
    }

    /** Change Password: the new password must be strong AND different from the current one. */
    fun newPasswordError(
        currentPassword: String,
        newPassword: String,
        email: String = "",
        firstName: String = "",
        lastName: String = ""
    ): String? {
        passwordError(newPassword, email, firstName, lastName)?.let { return it }
        if (newPassword == currentPassword) return "New password must be different from your current password"
        return null
    }

    /** A 0..4 score suitable for driving a strength meter in the UI. */
    fun passwordStrength(password: String): PasswordStrength {
        if (password.isEmpty()) return PasswordStrength.EMPTY
        var score = 0
        if (password.length >= PASSWORD_MIN_LENGTH) score++
        if (password.length >= 14) score++
        val classes = listOf(
            password.any { it.isUpperCase() },
            password.any { it.isLowerCase() },
            password.any { it.isDigit() },
            password.any { !it.isLetterOrDigit() && !it.isWhitespace() }
        ).count { it }
        if (classes >= 3) score++
        if (classes == 4) score++
        if (hasRepeatedRun(password) || hasSequentialRun(password) || isCommonPassword(password)) {
            score = (score - 2).coerceAtLeast(0)
        }
        return when (score) {
            0 -> PasswordStrength.WEAK
            1 -> PasswordStrength.WEAK
            2 -> PasswordStrength.FAIR
            3 -> PasswordStrength.STRONG
            else -> PasswordStrength.VERY_STRONG
        }
    }

    enum class PasswordStrength { EMPTY, WEAK, FAIR, STRONG, VERY_STRONG }

    // ---------------------------------------------------------------
    // Password helpers (internal so tests can target them if needed)
    // ---------------------------------------------------------------

    internal fun hasRepeatedRun(password: String): Boolean {
        var run = 1
        for (i in 1 until password.length) {
            run = if (password[i] == password[i - 1]) run + 1 else 1
            if (run > MAX_REPEAT_RUN) return true
        }
        return false
    }

    internal fun hasSequentialRun(password: String): Boolean {
        val lower = password.lowercase()
        if (lower.length < SEQUENCE_LENGTH) return false
        for (start in 0..lower.length - SEQUENCE_LENGTH) {
            val window = lower.substring(start, start + SEQUENCE_LENGTH)
            if (isStraightSequence(window)) return true
            if (KEYBOARD_ROWS.any { row -> row.contains(window) || row.reversed().contains(window) }) return true
        }
        return false
    }

    /** True for windows like "abcd", "1234" (ascending) or "dcba", "4321" (descending). */
    private fun isStraightSequence(window: String): Boolean {
        if (!window.all { it.isLetterOrDigit() }) return false
        val ascending = (1 until window.length).all { window[it] - window[it - 1] == 1 }
        val descending = (1 until window.length).all { window[it - 1] - window[it] == 1 }
        return ascending || descending
    }

    internal fun isCommonPassword(password: String): Boolean {
        val normalised = password.lowercase()
            .map { LEET_MAP[it] ?: it }
            .filter { it.isLetter() }
            .joinToString("")
        return COMMON_PASSWORD_WORDS.any { normalised.contains(it) }
    }

    private val LEET_MAP = mapOf(
        '@' to 'a', '4' to 'a', '3' to 'e', '1' to 'i', '!' to 'i', '|' to 'l',
        '0' to 'o', '$' to 's', '5' to 's', '7' to 't', '+' to 't'
    )

    private fun containsPersonalInfo(
        password: String,
        email: String,
        firstName: String,
        lastName: String
    ): Boolean {
        val lower = password.lowercase()
        val tokens = mutableListOf(firstName, lastName, email.substringBefore('@'))
        // Also check the individual words of multi-part names ("Mary Ann" -> "mary", "ann").
        tokens += firstName.split(' ', '-') + lastName.split(' ', '-')
        return tokens
            .map { it.trim().lowercase() }
            .filter { it.length >= 3 }
            .any { lower.contains(it) }
    }
}
