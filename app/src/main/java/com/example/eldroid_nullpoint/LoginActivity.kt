package com.example.eldroid_nullpoint

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.example.eldroid_nullpoint.databinding.ActivityLoginBinding
import com.example.eldroid_nullpoint.mvp.login.LoginContract
import com.example.eldroid_nullpoint.mvp.login.LoginPresenter
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class LoginActivity : AppCompatActivity(), LoginContract.View {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var presenter: LoginContract.Presenter
    private lateinit var googleSignInClient: GoogleSignInClient

    private var isPasswordVisible = false

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
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        presenter = LoginPresenter(this)

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
        binding.ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePasswordVisibility(isPasswordVisible)
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email != binding.etEmail.text.toString()) {
                binding.etEmail.setText(email)
                binding.etEmail.setSelection(email.length)
            }
            presenter.onLoginClicked(
                email = binding.etEmail.text.toString(),
                password = binding.etPassword.text.toString()
            )
        }

        binding.tvForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            intent.putExtra(
                ForgotPasswordActivity.EXTRA_EMAIL,
                binding.etEmail.text.toString().trim()
            )
            startActivity(intent)
        }

        binding.btnGoogleLogin.setOnClickListener {
            showLoading(true)
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        binding.tvGoToSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        binding.etEmail.doOnTextChanged { _, _, _, _ ->
            binding.tvEmailError.visibility = View.GONE
        }
        binding.etPassword.doOnTextChanged { _, _, _, _ ->
            binding.tvPasswordError.visibility = View.GONE
        }
    }

    private fun togglePasswordVisibility(visible: Boolean) {
        binding.etPassword.transformationMethod = if (visible) {
            HideReturnsTransformationMethod.getInstance()
        } else {
            PasswordTransformationMethod.getInstance()
        }
        binding.etPassword.setSelection(binding.etPassword.text.length)
        binding.ivTogglePassword.setImageResource(
            if (visible) R.drawable.ic_eye_off else R.drawable.ic_eye
        )
    }

    // ---------------------------------------------------------------
    // LoginContract.View
    // ---------------------------------------------------------------

    override fun showLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        binding.btnLogin.text = if (loading) "" else getString(R.string.btn_login)
        binding.btnGoogleLogin.isEnabled = !loading
    }

    override fun showEmailError(message: String) {
        binding.tvEmailError.text = message
        binding.tvEmailError.visibility = View.VISIBLE
    }

    override fun showPasswordError(message: String) {
        binding.tvPasswordError.text = message
        binding.tvPasswordError.visibility = View.VISIBLE
    }

    override fun clearErrors() {
        binding.tvEmailError.visibility = View.GONE
        binding.tvPasswordError.visibility = View.GONE
    }

    override fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun navigateToTerms() {
        val intent = Intent(this, TermsActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
