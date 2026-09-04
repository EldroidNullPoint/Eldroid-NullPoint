package com.example.eldroid_nullpoint.register

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.eldroid_nullpoint.R
import com.example.eldroid_nullpoint.data.Injection
import com.example.eldroid_nullpoint.databinding.ActivitySignupBinding
import com.example.eldroid_nullpoint.home.HomeActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException

/**
 * Register (sign up) screen. Uses the existing activity_signup.xml layout.
 */
class RegisterActivity : AppCompatActivity(), RegisterContract.View {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var presenter: RegisterContract.Presenter
    private lateinit var googleSignInClient: GoogleSignInClient

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

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
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        presenter = RegisterPresenter(Injection.provideAuthRepository(this))
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
        binding.ivBack.setOnClickListener { presenter.onLoginClicked() }

        binding.ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            applyPasswordVisibility(binding.etPassword, binding.ivTogglePassword, isPasswordVisible)
        }

        binding.ivToggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            applyPasswordVisibility(
                binding.etConfirmPassword,
                binding.ivToggleConfirmPassword,
                isConfirmPasswordVisible
            )
        }

        binding.btnSignup.setOnClickListener {
            presenter.onRegisterClicked(
                firstName = binding.etFirstName.text.toString(),
                lastName = binding.etLastName.text.toString(),
                email = binding.etEmail.text.toString(),
                password = binding.etPassword.text.toString(),
                confirmPassword = binding.etConfirmPassword.text.toString()
            )
        }

        binding.btnGoogleSignup.setOnClickListener { presenter.onGoogleSignUpClicked() }

        binding.tvGoToLogin.setOnClickListener { presenter.onLoginClicked() }
    }

    private fun applyPasswordVisibility(field: EditText, toggle: ImageView, visible: Boolean) {
        field.transformationMethod = if (visible) {
            HideReturnsTransformationMethod.getInstance()
        } else {
            PasswordTransformationMethod.getInstance()
        }
        field.setSelection(field.text.length)
        toggle.setImageResource(if (visible) R.drawable.ic_eye_off else R.drawable.ic_eye)
    }

    // ---------------------------------------------------------------
    // RegisterContract.View
    // ---------------------------------------------------------------

    override fun showLoading() = setLoading(true)

    override fun hideLoading() = setLoading(false)

    override fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun clearFieldErrors() {
        binding.tvFirstNameError.visibility = View.GONE
        binding.tvLastNameError.visibility = View.GONE
        binding.tvEmailError.visibility = View.GONE
        binding.tvPasswordError.visibility = View.GONE
        binding.tvConfirmPasswordError.visibility = View.GONE
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

    override fun launchGoogleSignIn() {
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    override fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /** Login is the activity underneath this one, so simply closing returns to it. */
    override fun navigateToLogin() {
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSignup.isEnabled = !loading
        binding.btnSignup.text = if (loading) "" else getString(R.string.btn_signup)
        binding.btnGoogleSignup.isEnabled = !loading
    }
}
