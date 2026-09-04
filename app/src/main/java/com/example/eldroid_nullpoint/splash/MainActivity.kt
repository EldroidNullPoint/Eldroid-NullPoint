package com.example.eldroid_nullpoint.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.eldroid_nullpoint.R
import com.example.eldroid_nullpoint.data.Injection
import com.example.eldroid_nullpoint.home.HomeActivity
import com.example.eldroid_nullpoint.login.LoginActivity

/**
 * Splash / router activity (the launcher). Shows the logo briefly, then asks
 * [SplashPresenter] where to go.
 */
class MainActivity : AppCompatActivity(), SplashContract.View {

    private lateinit var presenter: SplashContract.Presenter
    private val handler = Handler(Looper.getMainLooper())
    private val routeRunnable = Runnable { presenter.checkSession() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        presenter = SplashPresenter(Injection.provideAuthRepository(this))
        presenter.attachView(this)

        // Small delay purely so the splash briefly shows; not required for logic.
        handler.postDelayed(routeRunnable, SPLASH_DELAY_MS)
    }

    override fun onDestroy() {
        handler.removeCallbacks(routeRunnable)
        presenter.detachView()
        super.onDestroy()
    }

    override fun navigateToHome() = go(HomeActivity::class.java)

    override fun navigateToLogin() = go(LoginActivity::class.java)

    private fun go(destination: Class<out AppCompatActivity>) {
        startActivity(Intent(this, destination))
        finish()
    }

    private companion object {
        const val SPLASH_DELAY_MS = 400L
    }
}
