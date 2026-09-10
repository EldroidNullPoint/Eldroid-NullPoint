package com.example.eldroid_nullpoint

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.eldroid_nullpoint.databinding.ActivityLandingBinding

/**
 * First screen of the app for a signed-out borrower: the SmartDock artwork on the
 * brand dark green, with a single "Get Started" button into the onboarding flow.
 */
class LandingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLandingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLandingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applySystemBarInsets()

        binding.btnGetStarted.setOnClickListener {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }
    }

    /**
     * Keeps the artwork and the button clear of the status bar and the
     * gesture/navigation area, on top of the padding already set in the layout.
     */
    private fun applySystemBarInsets() {
        val root = binding.landingRoot
        val basePaddingTop = root.paddingTop
        val basePaddingBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = basePaddingTop + bars.top,
                bottom = basePaddingBottom + bars.bottom
            )
            insets
        }
    }
}
