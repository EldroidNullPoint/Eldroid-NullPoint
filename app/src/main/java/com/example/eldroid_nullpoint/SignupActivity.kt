package com.example.eldroid_nullpoint

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.eldroid_nullpoint.databinding.ActivitySignupBinding
import com.example.eldroid_nullpoint.model.User
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

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { firebaseAuthWithGoogle(it) }
            } catch (e: ApiException) {
                setLoading(false)
                Toast.makeText(this, "Google sign-in failed: ${e.message}", Toast.LENGTH_LONG).show()
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
            setLoading(true)
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        binding.tvGoToLogin.setOnClickListener { finish() }
    }

    // ---------------------------------------------------------------
    // Email / password sign up
    // ---------------------------------------------------------------

    private fun attemptSignup() {
        clearErrors()

        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        var isValid = true

        if (!Validators.isValidName(firstName)) {
            binding.tvFirstNameError.text = getString(R.string.error_first_name_required)
            binding.tvFirstNameError.visibility = View.VISIBLE
            isValid = false
        }

        if (!Validators.isValidName(lastName)) {
            binding.tvLastNameError.text = getString(R.string.error_last_name_required)
            binding.tvLastNameError.visibility = View.VISIBLE
            isValid = false
        }

        if (!Validators.isValidEmail(email)) {
            binding.tvEmailError.text = getString(R.string.error_invalid_email)
            binding.tvEmailError.visibility = View.VISIBLE
            isValid = false
        }

        val passwordError = Validators.passwordStrengthError(password)
        if (passwordError != null) {
            binding.tvPasswordError.text = passwordError
            binding.tvPasswordError.visibility = View.VISIBLE
            isValid = false
        }

        if (password != confirmPassword) {
            binding.tvConfirmPasswordError.text = getString(R.string.error_passwords_dont_match)
            binding.tvConfirmPasswordError.visibility = View.VISIBLE
            isValid = false
        }

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
                    Toast.makeText(
                        this,
                        task.exception?.localizedMessage ?: "Sign up failed. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
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
                        task.exception?.localizedMessage ?: "Google sign-in failed",
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
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSignup.isEnabled = !loading
        binding.btnSignup.text = if (loading) "" else getString(R.string.btn_signup)
        binding.btnGoogleSignup.isEnabled = !loading
    }
}
