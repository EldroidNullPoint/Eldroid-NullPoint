package com.example.eldroid_nullpoint

import android.app.Application
import com.example.eldroid_nullpoint.util.CrashHandler

/**
 * Application class for SmartDock borrower app.
 * Installs global crash handler for debugging.
 */
class SmartDockApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Install crash handler to catch and broadcast crashes
        CrashHandler.install()
        
        android.util.Log.d("SmartDockApp", "Application started with crash handler")
    }
}
