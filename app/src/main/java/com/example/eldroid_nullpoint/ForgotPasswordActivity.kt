package com.example.eldroid_nullpoint

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.example.eldroid_nullpoint.databinding.ActivityForgotPasswordBinding
import com.example.eldroid_nullpoint.mvp.forgotpassword.ForgotPasswordContract
import com.example.eldroid_nullpoint.mvp.forgotpassword.ForgotPasswordPresenter

class ForgotPasswordActivity : AppCompatActivity(), ForgotPasswordContract.View {

    companion object {
        const val EXTRA_EMAIL = "extra_email"
    }

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var presenter: ForgotPasswordContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        presenter = ForgotPasswordPresenter(this)

        // Pre-fill email carried over from Login screen
        intent.getStringExtra(EXTRA_EMAIL)?.takeIf { it.isNotBlank() }?.let {
            binding.etEmail.setText(it)
        }

        binding.ivBack.setOnClickListener { finish() }
        binding.tvBackToLogin.setOnClickListener { finish() }
        binding.btnSendReset.setOnClickListener {
            presenter.onSendResetClicked(binding.etEmail.text.toString())
        }

        binding.etEmail.doOnTextChanged { _, _, _, _ ->
            presenter.clearEmailError()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.detach()
    }

    // ---------------------------------------------------------------
    // ForgotPasswordContract.View
    // ---------------------------------------------------------------

    override fun showLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSendReset.isEnabled = !loading
        binding.btnSendReset.text = if (loading) "" else getString(R.string.btn_send_reset_link)
        binding.etEmail.isEnabled = !loading
    }

    override fun showEmailError(message: String) {
        binding.tvEmailError.text = message
        binding.tvEmailError.visibility = View.VISIBLE
    }

    override fun showSuccess(email: String) {
        binding.tvSuccess.text = getString(R.string.reset_email_sent, email)
        binding.tvSuccess.visibility = View.VISIBLE
    }

    override fun clearEmailError() {
        binding.tvEmailError.visibility = View.GONE
    }
}
