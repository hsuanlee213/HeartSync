package com.heartbeatmusic

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.heartbeatmusic.biometric.BioProfile
import com.heartbeatmusic.biometric.BioProfileStorage
import com.heartbeatmusic.biometric.BiometricFilter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFormActivity : AppCompatActivity() {

    // NOTE: etEmail corresponds to et_username in the XML and must be the user's registered email.
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSignIn: Button
    private lateinit var btnBack: ImageButton

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_form)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)

        etEmail = findViewById(R.id.et_username)
        etPassword = findViewById(R.id.et_password)
        btnSignIn = findViewById(R.id.btn_sign_in)
        btnBack = findViewById(R.id.btn_back)

        btnBack.setOnClickListener { finish() }

        btnSignIn.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            loginUser(email, password)
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                authResult.user?.let { retrieveCustomProfile(it) }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Firebase Auth Login failed", e)
                Toast.makeText(this, "Login failed: Invalid email or password.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun retrieveCustomProfile(user: FirebaseUser) {
        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                val username = if (doc.exists() && doc.contains("username"))
                    doc.getString("username") ?: "User" else "User"

                if (doc.exists() && doc.contains("bio_max_heart_rate")) {
                    val maxHr = doc.getLong("bio_max_heart_rate")
                    val resting = doc.getLong("bio_resting_bpm")
                    val energyLevel = doc.getLong("bio_energy_level")
                    if (maxHr != null && resting != null) {
                        val level = energyLevel?.toInt()?.coerceIn(1, 5) ?: 3
                        val bioProfile = BioProfile(maxHr.toInt(), resting.toInt(), level)
                        BioProfileStorage.save(prefs, bioProfile)
                        BiometricFilter.setBioProfile(bioProfile)
                        BiometricFilter.reset()
                    }
                }

                prefs.edit()
                    .putString("user_id", user.uid)
                    .putString("username", username)
                    .putString("email", user.email)
                    .apply()

                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to retrieve user profile after login.", e)
                Toast.makeText(this, "Login successful, but profile retrieval failed.", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
    }

    companion object {
        private const val TAG = "LoginFormActivity"
    }
}
