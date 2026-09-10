package com.example.eldroid_nullpoint

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.eldroid_nullpoint.databinding.ActivityOnboardingBinding
import com.example.eldroid_nullpoint.util.OnboardingPrefs

/**
 * Six-step "How it works" tutorial shown after Landing's "Get Started".
 *
 * All six steps share one layout: each step is a full-bleed piece of artwork that
 * already carries its own step number, title and description, so the activity only
 * swaps the image and toggles the controls underneath it. Steps 1-5 show
 * Skip + Next; step 6 shows Create Account plus the log-in link.
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    /**
     * Steps in order. The last entry is the final step, which shows the auth
     * actions instead of Skip/Next.
     */
    private val steps = listOf(
        R.drawable.onboarding_1,
        R.drawable.onboarding_2,
        R.drawable.onboarding_3,
        R.drawable.onboarding_4,
        R.drawable.onboarding_5,
        R.drawable.onboarding_6
    )

    private val lastStep get() = steps.lastIndex

    private var currentStep = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applySystemBarInsets()

        binding.tvSkip.setOnClickListener { goToStep(lastStep) }
        binding.btnNext.setOnClickListener { goToStep(currentStep + 1) }

        // Routed through the dispatcher so the arrow and the system back gesture
        // can never drift apart.
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.btnCreateAccount.setOnClickListener {
            leaveOnboardingFor(SignupActivity::class.java)
        }
        binding.tvGoToLogin.setOnClickListener {
            leaveOnboardingFor(LoginActivity::class.java)
        }

        registerBackNavigation()

        currentStep = savedInstanceState?.getInt(STATE_CURRENT_STEP) ?: 0
        showStep(currentStep)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_CURRENT_STEP, currentStep)
    }

    private fun goToStep(step: Int) {
        if (step !in steps.indices || step == currentStep) return
        currentStep = step
        showStep(step)
    }

    private fun showStep(step: Int) {
        val isFinalStep = step == lastStep

        binding.ivOnboarding.setImageResource(steps[step])
        binding.ivOnboarding.contentDescription =
            getString(R.string.onboarding_image_description, step + 1, steps.size)

        // INVISIBLE, not GONE: keeps the artwork from jumping up on the last step.
        binding.tvSkip.visibility = if (isFinalStep) View.INVISIBLE else View.VISIBLE
        binding.btnNext.visibility = if (isFinalStep) View.GONE else View.VISIBLE
        binding.authActions.visibility = if (isFinalStep) View.VISIBLE else View.GONE
    }

    /**
     * Back walks the tutorial in reverse one step at a time, so any earlier step
     * can be read again - including after Skip, which is why this follows the
     * step order rather than the screens actually visited. From the first step it
     * falls through to the default behaviour, which finishes this activity and
     * returns to Landing rather than jumping ahead to Login.
     */
    private fun registerBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentStep == 0) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    return
                }
                goToStep(currentStep - 1)
            }
        })
    }

    /**
     * Opens the existing Login / Sign Up screen and closes the tutorial, so Back
     * from there returns to Landing instead of re-entering onboarding.
     */
    private fun leaveOnboardingFor(destination: Class<*>) {
        OnboardingPrefs.setCompleted(this)
        startActivity(Intent(this, destination))
        finish()
    }

    /**
     * Keeps Skip clear of the status bar and the buttons clear of the
     * gesture/navigation area, on top of the padding already set in the layout.
     */
    private fun applySystemBarInsets() {
        val root = binding.onboardingRoot
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

    private companion object {
        const val STATE_CURRENT_STEP = "current_step"
    }
}
