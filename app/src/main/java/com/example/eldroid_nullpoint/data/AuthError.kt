package com.example.eldroid_nullpoint.data

/**
 * A repository failure with a message that is already safe to show to the user.
 * The Firebase-specific exception (if any) is kept as [cause] for logging.
 */
class AuthError(message: String, cause: Throwable? = null) : Exception(message, cause)
