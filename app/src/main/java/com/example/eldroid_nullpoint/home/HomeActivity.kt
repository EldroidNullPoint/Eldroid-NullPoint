package com.example.eldroid_nullpoint.home

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.eldroid_nullpoint.R
import com.example.eldroid_nullpoint.data.Injection
import com.example.eldroid_nullpoint.databinding.ActivityHomeBinding
import com.example.eldroid_nullpoint.login.LoginActivity

/**
 * Home / Dashboard screen. Renders the [DashboardData] the presenter provides.
 * The current layout only has a greeting and email; the remaining dashboard
 * fields (provider, member since, last login, login count) are wired up in the UI pass.
 */
class HomeActivity : AppCompatActivity(), HomeContract.View {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var presenter: HomeContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        presenter = HomePresenter(Injection.provideAuthRepository(this))
        presenter.attachView(this)

        binding.btnLogout.setOnClickListener { presenter.onLogoutClicked() }

        presenter.loadDashboard()
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }

    // ---------------------------------------------------------------
    // HomeContract.View
    // ---------------------------------------------------------------

    override fun showLoading() {
        binding.tvWelcome.text = getString(R.string.welcome_message)
    }

    override fun hideLoading() {
        // Nothing to hide yet; the layout has no progress indicator.
    }

    override fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun showDashboard(data: DashboardData) {
        val welcome = getString(R.string.welcome_message)
        binding.tvWelcome.text = if (data.firstName.isNotBlank()) "$welcome, ${data.firstName}!" else "$welcome!"
        binding.tvEmail.text = data.email
    }

    override fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
