package com.example.eldroid_nullpoint.login

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.eldroid_nullpoint.R
import com.example.eldroid_nullpoint.data.Injection
import com.example.eldroid_nullpoint.databinding.ActivityLoginBinding
import com.example.eldroid_nullpoint.home.HomeActivity
import com.example.eldroid_nullpoint.register.RegisterActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException

/**
 * Login screen. As the MVP "View" it only forwards user actions to
 * [LoginPresenter] and renders whatever the presenter tells it to.
 */
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
                presenter.onGoogleSignInResult(account?.idToken)
            } catch (e: ApiException) {
                if (e.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                    presenter.onGoogleSignInCancelled()
                } else {
                    presenter.onGoogleSignInFailed(e.message)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        presenter = LoginPresenter(Injection.provideAuthRepository(this))
        presenter.attachView(this)

        setupGoogleSignIn()
        setupListeners()
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
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
            presenter.onLoginClicked(
                email = binding.etEmail.text.toString(),
                password = binding.etPassword.text.toString()
            )
        }

        binding.tvForgotPassword.setOnClickListener {
            presenter.onForgotPasswordClicked(binding.etEmail.text.toString())
        }

        binding.btnGoogleLogin.setOnClickListener { presenter.onGoogleSignInClicked() }

        binding.tvGoToSignup.setOnClickListener { presenter.onRegisterClicked() }
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

    override fun showLoading() = setLoading(true)

    override fun hideLoading() = setLoading(false)

    override fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun clearFieldErrors() {
        binding.tvEmailError.visibility = View.GONE
        binding.tvPasswordError.visibility = View.GONE
    }

    override fun showEmailError(message: String) {
        binding.tvEmailError.text = message
        binding.tvEmailError.visibility = View.VISIBLE
    }

    override fun showPasswordError(message: String) {
        binding.tvPasswordError.text = message
        binding.tvPasswordError.visibility = View.VISIBLE
    }

    override fun launchGoogleSignIn() {
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    override fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun navigateToRegister() {
        startActivity(Intent(this, RegisterActivity::class.java))
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        binding.btnLogin.text = if (loading) "" else getString(R.string.btn_login)
        binding.btnGoogleLogin.isEnabled = !loading
    }
}
