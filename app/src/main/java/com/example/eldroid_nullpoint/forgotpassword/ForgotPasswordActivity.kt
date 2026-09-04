package com.example.eldroid_nullpoint.forgotpassword

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.eldroid_nullpoint.R
import com.example.eldroid_nullpoint.data.Injection
import com.example.eldroid_nullpoint.databinding.ActivityForgotPasswordBinding

/**
 * Forgot Password screen. Two visual states: the email form, and a
 * "check your inbox" confirmation once the reset link has been sent.
 */
class ForgotPasswordActivity : AppCompatActivity(), ForgotPasswordContract.View {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var presenter: ForgotPasswordContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        presenter = ForgotPasswordPresenter(Injection.provideAuthRepository(this))
        presenter.attachView(this)

        binding.etEmail.setText(intent.getStringExtra(EXTRA_EMAIL).orEmpty())

        binding.ivBack.setOnClickListener { presenter.onBackToLoginClicked() }
        binding.tvGoToLogin.setOnClickListener { presenter.onBackToLoginClicked() }
        binding.btnBackToLogin.setOnClickListener { presenter.onBackToLoginClicked() }
        binding.btnSendReset.setOnClickListener {
            presenter.onSendResetClicked(binding.etEmail.text.toString())
        }
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }

    // ---------------------------------------------------------------
    // ForgotPasswordContract.View
    // ---------------------------------------------------------------

    override fun showLoading() = setLoading(true)

    override fun hideLoading() = setLoading(false)

    override fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun clearFieldErrors() {
        binding.tvEmailError.visibility = View.GONE
    }

    override fun showEmailError(message: String) {
        binding.tvEmailError.text = message
        binding.tvEmailError.visibility = View.VISIBLE
    }

    override fun showResetEmailSent(email: String) {
        binding.tvSentMessage.text = getString(R.string.subtitle_reset_sent, email)
        binding.layoutForm.visibility = View.GONE
        binding.layoutSent.visibility = View.VISIBLE
    }

    /** Login is the activity underneath this one, so closing returns to it. */
    override fun navigateToLogin() {
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSendReset.isEnabled = !loading
        binding.btnSendReset.text = if (loading) "" else getString(R.string.btn_send_reset)
    }

    companion object {
        private const val EXTRA_EMAIL = "extra_email"

        fun newIntent(context: Context, prefilledEmail: String): Intent =
            Intent(context, ForgotPasswordActivity::class.java).putExtra(EXTRA_EMAIL, prefilledEmail)
    }
}
