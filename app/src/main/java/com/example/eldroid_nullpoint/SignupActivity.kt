package com.example.eldroid_nullpoint

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.example.eldroid_nullpoint.databinding.ActivitySignupBinding
import com.example.eldroid_nullpoint.mvp.signup.SignupContract
import com.example.eldroid_nullpoint.mvp.signup.SignupPresenter
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class SignupActivity : AppCompatActivity(), SignupContract.View {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var presenter: SignupContract.Presenter
    private lateinit var googleSignInClient: GoogleSignInClient

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    presenter.onGoogleTokenReceived(idToken)
                } else {
                    showLoading(false)
                }
            } catch (e: ApiException) {
                showLoading(false)
                showToast(getString(R.string.auth_error_generic))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        presenter = SignupPresenter(this)

        setupGoogleSignIn()
        setupListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.detach()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener { finish() }

        binding.ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            binding.etPassword.transformationMethod = if (isPasswordVisible) {
                HideReturnsTransformationMethod.getInstance()
            } else {
                PasswordTransformationMethod.getInstance()
            }
            binding.etPassword.setSelection(binding.etPassword.text.length)
            binding.ivTogglePassword.setImageResource(
                if (isPasswordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye
            )
        }

        binding.ivToggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            binding.etConfirmPassword.transformationMethod = if (isConfirmPasswordVisible) {
                HideReturnsTransformationMethod.getInstance()
            } else {
                PasswordTransformationMethod.getInstance()
            }
            binding.etConfirmPassword.setSelection(binding.etConfirmPassword.text.length)
            binding.ivToggleConfirmPassword.setImageResource(
                if (isConfirmPasswordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye
            )
        }

        binding.btnSignup.setOnClickListener {
            presenter.onSignupClicked(
                firstName = trimInPlace(binding.etFirstName),
                lastName = trimInPlace(binding.etLastName),
                email = trimInPlace(binding.etEmail),
                password = binding.etPassword.text.toString(),
                confirmPassword = binding.etConfirmPassword.text.toString()
            )
        }

        binding.btnGoogleSignup.setOnClickListener {
            showLoading(true)
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        binding.tvGoToLogin.setOnClickListener { finish() }

        clearErrorOnEdit(binding.etFirstName, binding.tvFirstNameError)
        clearErrorOnEdit(binding.etLastName, binding.tvLastNameError)
        clearErrorOnEdit(binding.etEmail, binding.tvEmailError)
        clearErrorOnEdit(binding.etPassword, binding.tvPasswordError)
        clearErrorOnEdit(binding.etConfirmPassword, binding.tvConfirmPasswordError)
    }

    private fun clearErrorOnEdit(field: EditText, errorView: TextView) {
        field.doOnTextChanged { _, _, _, _ -> errorView.visibility = View.GONE }
    }

    private fun trimInPlace(field: EditText): String {
        val raw = field.text.toString()
        val trimmed = raw.trim()
        if (raw != trimmed) {
            field.setText(trimmed)
            field.setSelection(trimmed.length)
        }
        return trimmed
    }

    // ---------------------------------------------------------------
    // SignupContract.View
    // ---------------------------------------------------------------

    override fun showLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSignup.isEnabled = !loading
        binding.btnSignup.text = if (loading) "" else getString(R.string.btn_signup)
        binding.btnGoogleSignup.isEnabled = !loading
    }

    override fun showFirstNameError(message: String) {
        binding.tvFirstNameError.text = message
        binding.tvFirstNameError.visibility = View.VISIBLE
    }

    override fun showLastNameError(message: String) {
        binding.tvLastNameError.text = message
        binding.tvLastNameError.visibility = View.VISIBLE
    }

    override fun showEmailError(message: String) {
        binding.tvEmailError.text = message
        binding.tvEmailError.visibility = View.VISIBLE
    }

    override fun showPasswordError(message: String) {
        binding.tvPasswordError.text = message
        binding.tvPasswordError.visibility = View.VISIBLE
    }

    override fun showConfirmPasswordError(message: String) {
        binding.tvConfirmPasswordError.text = message
        binding.tvConfirmPasswordError.visibility = View.VISIBLE
    }

    override fun clearErrors() {
        binding.tvFirstNameError.visibility = View.GONE
        binding.tvLastNameError.visibility = View.GONE
        binding.tvEmailError.visibility = View.GONE
        binding.tvPasswordError.visibility = View.GONE
        binding.tvConfirmPasswordError.visibility = View.GONE
    }

    override fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
