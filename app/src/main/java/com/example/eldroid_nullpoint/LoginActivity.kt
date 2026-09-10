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
import com.example.eldroid_nullpoint.model.User
import com.example.eldroid_nullpoint.util.AuthErrors
import com.example.eldroid_nullpoint.util.Validators
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var googleSignInClient: GoogleSignInClient

    private var isPasswordVisible = false

    /** Guards against a second submission while a request is already in flight. */
    private var isSubmitting = false

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    firebaseAuthWithGoogle(idToken)
                } else {
                    setLoading(false)
                }
            } catch (e: ApiException) {
                setLoading(false)
                Toast.makeText(this, getString(R.string.auth_error_generic), Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupGoogleSignIn()
        setupListeners()
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

        binding.btnLogin.setOnClickListener { attemptLogin() }

        binding.tvForgotPassword.setOnClickListener { goToForgotPassword() }

        binding.btnGoogleLogin.setOnClickListener {
            if (isSubmitting) return@setOnClickListener
            setLoading(true)
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        binding.tvGoToSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        // Errors clear as soon as the borrower starts correcting the field.
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
    // Email / password login
    // ---------------------------------------------------------------

    private fun attemptLogin() {
        if (isSubmitting) return

        binding.tvEmailError.visibility = View.GONE
        binding.tvPasswordError.visibility = View.GONE

        // Accidental leading/trailing spaces are stripped, and the trimmed value is
        // written back so the borrower sees exactly what is being submitted.
        val email = binding.etEmail.text.toString().trim()
        if (email != binding.etEmail.text.toString()) {
            binding.etEmail.setText(email)
            binding.etEmail.setSelection(email.length)
        }
        val password = binding.etPassword.text.toString()

        var isValid = true

        if (email.isBlank()) {
            showError(binding.tvEmailError, getString(R.string.error_email_required))
            isValid = false
        } else if (!Validators.isValidEmail(email)) {
            showError(binding.tvEmailError, getString(R.string.error_invalid_email))
            isValid = false
        }

        if (password.isBlank()) {
            showError(binding.tvPasswordError, getString(R.string.error_password_required))
            isValid = false
        }

        if (!isValid) return

        setLoading(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    goToHome()
                } else {
                    setLoading(false)
                    // Shown inline on the password field rather than as a raw
                    // Firebase message, so the borrower knows what to correct.
                    showError(
                        binding.tvPasswordError,
                        AuthErrors.signInMessageFor(this, task.exception)
                    )
                }
            }
    }

    private fun goToForgotPassword() {
        val intent = Intent(this, ForgotPasswordActivity::class.java)
        // Pre-fill whatever email has already been typed here.
        intent.putExtra(
            ForgotPasswordActivity.EXTRA_EMAIL,
            binding.etEmail.text.toString().trim()
        )
        startActivity(intent)
    }

    // ---------------------------------------------------------------
    // Google sign-in
    // ---------------------------------------------------------------

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    val nameParts = splitDisplayName(firebaseUser?.displayName)
                    ensureUserDocument(
                        uid = firebaseUser?.uid.orEmpty(),
                        firstName = nameParts.first,
                        lastName = nameParts.second,
                        email = firebaseUser?.email.orEmpty(),
                        provider = "google.com"
                    )
                } else {
                    setLoading(false)
                    Toast.makeText(
                        this,
                        AuthErrors.messageFor(this, task.exception),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }


    // ---------------------------------------------------------------
    // Shared helpers
    // ---------------------------------------------------------------

    private fun showError(view: android.widget.TextView, message: String) {
        view.text = message
        view.visibility = View.VISIBLE
    }

    private fun splitDisplayName(displayName: String?): Pair<String, String> {
        if (displayName.isNullOrBlank()) return "" to ""
        val parts = displayName.trim().split(" ", limit = 2)
        val first = parts.getOrNull(0).orEmpty()
        val last = parts.getOrNull(1).orEmpty()
        return first to last
    }

    /** Creates the Firestore profile document the first time a social account logs in. */
    private fun ensureUserDocument(
        uid: String,
        firstName: String,
        lastName: String,
        email: String,
        provider: String
    ) {
        if (uid.isBlank()) {
            setLoading(false)
            goToHome()
            return
        }
        val userDocRef = firestore.collection("users").document(uid)
        userDocRef.get()
            .addOnSuccessListener { snapshot ->
                setLoading(false)
                if (!snapshot.exists()) {
                    val user = User(
                        uid = uid,
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        provider = provider
                    )
                    userDocRef.set(user)
                }
                goToHome()
            }
            .addOnFailureListener {
                setLoading(false)
                // Even if Firestore write/read fails, the user is already authenticated.
                goToHome()
            }
    }

    private fun goToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setLoading(loading: Boolean) {
        isSubmitting = loading
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        binding.btnLogin.text = if (loading) "" else getString(R.string.btn_login)
        binding.btnGoogleLogin.isEnabled = !loading
    }
}
