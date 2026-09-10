package com.example.eldroid_nullpoint

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * Splash / router activity. Decides whether the borrower should land on the
 * Home dashboard (session already exists) or the Landing page.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Small delay purely so the splash briefly shows; not required for logic.
        Handler(Looper.getMainLooper()).postDelayed({
            routeToNextScreen()
        }, 400)
    }

    private fun routeToNextScreen() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        // Signed-out borrowers see the Landing page first; an existing session
        // still goes straight to the dashboard.
        val destination = if (currentUser != null) {
            Intent(this, HomeActivity::class.java)
        } else {
            Intent(this, LandingActivity::class.java)
        }
        startActivity(destination)
        finish()
    }
}
