package com.example.eldroid_nullpoint.util

/**
 * Global debug message broadcaster — lets any screen post debug info
 * that HomeActivity can display in a visible banner.
 *
 * DELETE THIS FILE when debugging is complete.
 */
object DebugBroadcaster {
    private val listeners = mutableSetOf<(String) -> Unit>()
    private val messageHistory = mutableListOf<String>()
    private const val MAX_HISTORY = 20
    
    var lastMessage: String = "Waiting for events..."
        private set

    fun addListener(listener: (String) -> Unit) {
        listeners.add(listener)
        // Send the last message immediately to new listeners
        listener(getFullHistory())
    }

    fun removeListener(listener: (String) -> Unit) {
        listeners.remove(listener)
    }

    fun broadcast(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
            .format(java.util.Date())
        val timestampedMessage = "[$timestamp] $message"
        
        android.util.Log.d("DebugBroadcaster", "Broadcasting: $timestampedMessage")
        
        lastMessage = timestampedMessage
        messageHistory.add(timestampedMessage)
        
        // Keep only last MAX_HISTORY messages
        if (messageHistory.size > MAX_HISTORY) {
            messageHistory.removeAt(0)
        }
        
        listeners.forEach { it(getFullHistory()) }
    }
    
    private fun getFullHistory(): String {
        return messageHistory.takeLast(5).joinToString("\n")
    }
    
    fun clearHistory() {
        messageHistory.clear()
        lastMessage = "History cleared"
    }
}
