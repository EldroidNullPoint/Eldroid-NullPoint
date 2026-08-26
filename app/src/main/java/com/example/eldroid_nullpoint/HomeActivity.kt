package com.example.eldroid_nullpoint

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.eldroid_nullpoint.databinding.ActivityHomeBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Placeholder screen shown after a successful login / sign up.
 * Replace this with the real app content.
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            goToLogin()
            return
        }

        binding.tvEmail.text = currentUser.email ?: ""

        // Pull the first name from Firestore for a nicer greeting; fall back to
        // the Firebase Auth display name (set for Google/Facebook accounts).
        FirebaseFirestore.getInstance().collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val firstName = snapshot.getString("firstName")
                binding.tvWelcome.text = if (!firstName.isNullOrBlank()) {
                    getString(R.string.welcome_message) + ", $firstName!"
                } else {
                    getString(R.string.welcome_message) + "!"
                }
            }
            .addOnFailureListener {
                binding.tvWelcome.text = getString(R.string.welcome_message) + "!"
            }

        binding.btnLogout.setOnClickListener { logout() }
    }

    private fun logout() {
        auth.signOut()

        // Also sign out of Google so the account picker shows again next time.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(this, gso).signOut()

        goToLogin()
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
