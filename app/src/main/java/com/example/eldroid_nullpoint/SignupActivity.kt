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

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var googleSignInClient: GoogleSignInClient

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

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
        binding = ActivitySignupBinding.inflate(layoutInflater)
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

        binding.btnSignup.setOnClickListener { attemptSignup() }

        binding.btnGoogleSignup.setOnClickListener {
            if (isSubmitting) return@setOnClickListener
            setLoading(true)
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        binding.tvGoToLogin.setOnClickListener { finish() }

        // Each error clears as soon as the borrower starts correcting that field.
        clearErrorOnEdit(binding.etFirstName, binding.tvFirstNameError)
        clearErrorOnEdit(binding.etLastName, binding.tvLastNameError)
        clearErrorOnEdit(binding.etEmail, binding.tvEmailError)
        clearErrorOnEdit(binding.etPassword, binding.tvPasswordError)
        clearErrorOnEdit(binding.etConfirmPassword, binding.tvConfirmPasswordError)
    }

    private fun clearErrorOnEdit(field: EditText, errorView: TextView) {
        field.doOnTextChanged { _, _, _, _ -> errorView.visibility = View.GONE }
    }

    // ---------------------------------------------------------------
    // Email / password sign up
    // ---------------------------------------------------------------

    private fun attemptSignup() {
        if (isSubmitting) return
        clearErrors()

        // Names and email are trimmed and written back, so the borrower sees the
        // exact value being registered. Passwords are never trimmed.
        val firstName = trimInPlace(binding.etFirstName)
        val lastName = trimInPlace(binding.etLastName)
        val email = trimInPlace(binding.etEmail)
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        var isValid = true

        if (firstName.isBlank()) {
            showError(binding.tvFirstNameError, getString(R.string.error_first_name_required))
            isValid = false
        } else if (!Validators.isValidName(firstName)) {
            // Rejects values made only of digits or symbols, and 1-character names.
            showError(binding.tvFirstNameError, getString(R.string.error_first_name_invalid))
            isValid = false
        }

        if (lastName.isBlank()) {
            showError(binding.tvLastNameError, getString(R.string.error_last_name_required))
            isValid = false
        } else if (!Validators.isValidName(lastName)) {
            showError(binding.tvLastNameError, getString(R.string.error_last_name_invalid))
            isValid = false
        }

        if (email.isBlank()) {
            showError(binding.tvEmailError, getString(R.string.error_email_required))
            isValid = false
        } else if (!Validators.isValidEmail(email)) {
            showError(binding.tvEmailError, getString(R.string.error_invalid_email))
            isValid = false
        }

        val passwordError = Validators.passwordStrengthError(password)
        if (passwordError != null) {
            showError(binding.tvPasswordError, passwordError)
            isValid = false
        }

        if (confirmPassword.isBlank()) {
            showError(
                binding.tvConfirmPasswordError,
                getString(R.string.error_confirm_password_required)
            )
            isValid = false
        } else if (password != confirmPassword) {
            showError(
                binding.tvConfirmPasswordError,
                getString(R.string.error_passwords_dont_match)
            )
            isValid = false
        }

        // The account is only created once every local rule above passes.
        if (!isValid) return

        setLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result?.user?.uid.orEmpty()
                    val user = User(
                        uid = uid,
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        provider = "email"
                    )
                    saveUserProfile(user)
                } else {
                    setLoading(false)
                    // A duplicate email is reported inline on the email field;
                    // anything else falls back to a short, non-technical toast.
                    val exception = task.exception
                    val message = AuthErrors.messageFor(this, exception)
                    if (exception is com.google.firebase.auth.FirebaseAuthUserCollisionException ||
                        exception is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
                    ) {
                        showError(binding.tvEmailError, message)
                    } else {
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    /** Trims a field, writes the trimmed value back and returns it. */
    private fun trimInPlace(field: EditText): String {
        val raw = field.text.toString()
        val trimmed = raw.trim()
        if (raw != trimmed) {
            field.setText(trimmed)
            field.setSelection(trimmed.length)
        }
        return trimmed
    }

    private fun saveUserProfile(user: User) {
        firestore.collection("users").document(user.uid)
            .set(user)
            .addOnCompleteListener {
                setLoading(false)
                // Whether or not the Firestore write succeeds, the auth account exists,
                // so let the user in; the profile can be retried/synced later if needed.
                goToHome()
            }
    }

    private fun showError(view: TextView, message: String) {
        view.text = message
        view.visibility = View.VISIBLE
    }

    private fun clearErrors() {
        binding.tvFirstNameError.visibility = View.GONE
        binding.tvLastNameError.visibility = View.GONE
        binding.tvEmailError.visibility = View.GONE
        binding.tvPasswordError.visibility = View.GONE
        binding.tvConfirmPasswordError.visibility = View.GONE
    }

    // ---------------------------------------------------------------
    // Google sign-up
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

    private fun splitDisplayName(displayName: String?): Pair<String, String> {
        if (displayName.isNullOrBlank()) return "" to ""
        val parts = displayName.trim().split(" ", limit = 2)
        val first = parts.getOrNull(0).orEmpty()
        val last = parts.getOrNull(1).orEmpty()
        return first to last
    }

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
                // Never overwrite an existing profile - that would clobber fields
                // the administrator side owns, such as rfidCardUid.
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
        binding.btnSignup.isEnabled = !loading
        binding.btnSignup.text = if (loading) "" else getString(R.string.btn_signup)
        binding.btnGoogleSignup.isEnabled = !loading
    }
}
