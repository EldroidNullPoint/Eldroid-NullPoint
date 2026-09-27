package com.example.eldroid_nullpoint

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.example.eldroid_nullpoint.databinding.ActivityChangePasswordBinding
import com.example.eldroid_nullpoint.mvp.changepassword.ChangePasswordContract
import com.example.eldroid_nullpoint.mvp.changepassword.ChangePasswordPresenter
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordActivity : AppCompatActivity(), ChangePasswordContract.View {

    private lateinit var binding: ActivityChangePasswordBinding
    private lateinit var presenter: ChangePasswordContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (FirebaseAuth.getInstance().currentUser == null) {
            finish()
            return
        }

        presenter = ChangePasswordPresenter(this)

        binding.ivBack.setOnClickListener { finish() }
        binding.btnUpdatePassword.setOnClickListener {
            presenter.onUpdatePasswordClicked(
                currentPassword = binding.etCurrentPassword.text.toString(),
                newPassword = binding.etNewPassword.text.toString(),
                confirmPassword = binding.etConfirmNewPassword.text.toString()
            )
        }

        setupPasswordToggle(binding.etCurrentPassword, binding.ivToggleCurrentPassword)
        setupPasswordToggle(binding.etNewPassword, binding.ivToggleNewPassword)
        setupPasswordToggle(binding.etConfirmNewPassword, binding.ivToggleConfirmNewPassword)

        binding.etCurrentPassword.doOnTextChanged { _, _, _, _ ->
            binding.tvCurrentPasswordError.visibility = View.GONE
        }
        binding.etNewPassword.doOnTextChanged { _, _, _, _ ->
            binding.tvNewPasswordError.visibility = View.GONE
        }
        binding.etConfirmNewPassword.doOnTextChanged { _, _, _, _ ->
            binding.tvConfirmNewPasswordError.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.detach()
    }

    private fun setupPasswordToggle(field: EditText, toggle: ImageView) {
        var visible = false
        toggle.setOnClickListener {
            visible = !visible
            field.transformationMethod = if (visible) {
                HideReturnsTransformationMethod.getInstance()
            } else {
                PasswordTransformationMethod.getInstance()
            }
            field.setSelection(field.text.length)
            toggle.setImageResource(if (visible) R.drawable.ic_eye_off else R.drawable.ic_eye)
        }
    }

    // ---------------------------------------------------------------
    // ChangePasswordContract.View
    // ---------------------------------------------------------------

    override fun showLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnUpdatePassword.isEnabled = !loading
        binding.btnUpdatePassword.text =
            if (loading) "" else getString(R.string.btn_update_password)
        binding.etCurrentPassword.isEnabled = !loading
        binding.etNewPassword.isEnabled = !loading
        binding.etConfirmNewPassword.isEnabled = !loading
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
        binding.tvConfirmNewPasswordError.text = message
        binding.tvConfirmNewPasswordError.visibility = View.VISIBLE
    }

    override fun clearErrors() {
        binding.tvCurrentPasswordError.visibility = View.GONE
        binding.tvNewPasswordError.visibility = View.GONE
        binding.tvConfirmNewPasswordError.visibility = View.GONE
    }

    override fun onPasswordChangedSuccess() {
        Toast.makeText(this, getString(R.string.password_changed), Toast.LENGTH_LONG).show()
        finish()
    }

    override fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
