package com.example.eldroid_nullpoint.util

/**
 * Global debug message broadcaster — lets any screen post debug info
 * that HomeActivity can display in a visible banner.
 *
 * DELETE THIS FILE when debugging is complete.
 */
object DebugBroadcaster {
    private val listeners = mutableSetOf<(String) -> Unit>()

    fun addListener(listener: (String) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (String) -> Unit) {
        listeners.remove(listener)
    }

    fun broadcast(message: String) {
        android.util.Log.d("DebugBroadcaster", "Broadcasting: $message")
        listeners.forEach { it(message) }
    }
}
