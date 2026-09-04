package com.example.eldroid_nullpoint.changepassword

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.eldroid_nullpoint.R
import com.example.eldroid_nullpoint.data.Injection
import com.example.eldroid_nullpoint.databinding.ActivityChangePasswordBinding
import com.example.eldroid_nullpoint.login.LoginActivity

/**
 * Change Password screen for signed-in email/password accounts.
 */
class ChangePasswordActivity : AppCompatActivity(), ChangePasswordContract.View {

    private lateinit var binding: ActivityChangePasswordBinding
    private lateinit var presenter: ChangePasswordContract.Presenter

    private var isCurrentVisible = false
    private var isNewVisible = false
    private var isConfirmVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        presenter = ChangePasswordPresenter(Injection.provideAuthRepository(this))
        presenter.attachView(this)

        setupListeners()
        presenter.start()
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener { finish() }

        binding.ivToggleCurrentPassword.setOnClickListener {
            isCurrentVisible = !isCurrentVisible
            applyVisibility(binding.etCurrentPassword, binding.ivToggleCurrentPassword, isCurrentVisible)
        }
        binding.ivToggleNewPassword.setOnClickListener {
            isNewVisible = !isNewVisible
            applyVisibility(binding.etNewPassword, binding.ivToggleNewPassword, isNewVisible)
        }
        binding.ivToggleConfirmPassword.setOnClickListener {
            isConfirmVisible = !isConfirmVisible
            applyVisibility(binding.etConfirmPassword, binding.ivToggleConfirmPassword, isConfirmVisible)
        }

        binding.btnChangePassword.setOnClickListener {
            presenter.onChangePasswordClicked(
                currentPassword = binding.etCurrentPassword.text.toString(),
                newPassword = binding.etNewPassword.text.toString(),
                confirmPassword = binding.etConfirmPassword.text.toString()
            )
        }
    }

    private fun applyVisibility(field: EditText, toggle: ImageView, visible: Boolean) {
        field.transformationMethod = if (visible) {
            HideReturnsTransformationMethod.getInstance()
        } else {
            PasswordTransformationMethod.getInstance()
        }
        field.setSelection(field.text.length)
        toggle.setImageResource(if (visible) R.drawable.ic_eye_off else R.drawable.ic_eye)
    }

    // ---------------------------------------------------------------
    // ChangePasswordContract.View
    // ---------------------------------------------------------------

    override fun showLoading() = setLoading(true)

    override fun hideLoading() = setLoading(false)

    override fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun clearFieldErrors() {
        binding.tvCurrentPasswordError.visibility = View.GONE
        binding.tvNewPasswordError.visibility = View.GONE
        binding.tvConfirmPasswordError.visibility = View.GONE
    }

    override fun showCurrentPasswordError(message: String) {
        binding.tvCurrentPasswordError.text = message
        binding.tvCurrentPasswordError.visibility = View.VISIBLE
    }

    override fun showNewPasswordError(message: String) {
        binding.tvNewPasswordError.text = message
        binding.tvNewPasswordError.visibility = View.VISIBLE
    }

    override fun showConfirmPasswordError(message: String) {
        binding.tvConfirmPasswordError.text = message
        binding.tvConfirmPasswordError.visibility = View.VISIBLE
    }

    override fun showPasswordChangeUnavailable(message: String) {
        binding.tvUnavailable.text = message
        binding.tvUnavailable.visibility = View.VISIBLE
        listOf(binding.etCurrentPassword, binding.etNewPassword, binding.etConfirmPassword).forEach {
            it.isEnabled = false
        }
        binding.btnChangePassword.isEnabled = false
    }

    override fun showPasswordChanged() {
        Toast.makeText(this, R.string.password_changed, Toast.LENGTH_LONG).show()
        finish()
    }

    override fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnChangePassword.isEnabled = !loading
        binding.btnChangePassword.text = if (loading) "" else getString(R.string.btn_change_password)
    }
}
