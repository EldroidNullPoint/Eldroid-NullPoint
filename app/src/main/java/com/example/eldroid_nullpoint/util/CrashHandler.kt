package com.example.eldroid_nullpoint.util

import android.app.Application

/**
 * Global crash handler that broadcasts crash info to DebugBroadcaster
 * so crashes show up in the debug banner instead of just silently closing.
 * 
 * DELETE THIS FILE when debugging is complete.
 */
class CrashHandler private constructor() : Thread.UncaughtExceptionHandler {
    
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val crashMessage = buildString {
                append("💥 CRASH DETECTED!\n")
                append("${throwable.javaClass.simpleName}: ${throwable.message}\n")
                append("\nStack trace:\n")
                throwable.stackTrace.take(5).forEach { element ->
                    append("  at ${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})\n")
                }
            }
            
            android.util.Log.e("CrashHandler", crashMessage, throwable)
            DebugBroadcaster.broadcast(crashMessage)
            
            // Give the broadcast a moment to propagate
            Thread.sleep(500)
        } catch (e: Exception) {
            android.util.Log.e("CrashHandler", "Error in crash handler", e)
        }
        
        // Call the default handler to actually crash the app
        defaultHandler?.uncaughtException(thread, throwable)
    }
    
    companion object {
        fun install() {
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler())
        }
    }
}
