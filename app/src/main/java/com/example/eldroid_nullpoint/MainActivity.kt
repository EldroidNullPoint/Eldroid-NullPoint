package com.example.eldroid_nullpoint

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

/**
 * Splash activity. Every launch starts on the Landing page; Landing decides
 * whether "Get Started" continues into onboarding or straight to Home for a
 * borrower who is already signed in.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Small delay purely so the splash briefly shows; not required for logic.
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, LandingActivity::class.java))
            finish()
        }, 400)
    }
}
