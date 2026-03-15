package com.heartbeatmusic

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.heartbeatmusic.biometric.BioProfileStorage
import com.heartbeatmusic.biometric.BiometricFilter
import com.heartbeatmusic.biometric.RegistrationMapper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etAge: EditText
    private lateinit var etWeight: EditText
    private lateinit var dropdownEnergyLevel: AutoCompleteTextView
    private lateinit var btnCreateAccount: Button
    private lateinit var btnBack: ImageButton

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)

        etEmail = findViewById(R.id.et_reg_email)
        etPassword = findViewById(R.id.et_reg_password)
        etAge = findViewById(R.id.et_age)
        etWeight = findViewById(R.id.et_weight)
        dropdownEnergyLevel = findViewById(R.id.dropdown_energy_level)
        btnCreateAccount = findViewById(R.id.btn_create_account)
        btnBack = findViewById(R.id.btn_back_register)

        val energyAdapter = ArrayAdapter.createFromResource(
            this, R.array.energy_levels, android.R.layout.simple_dropdown_item_1line
        )
        dropdownEnergyLevel.setAdapter(energyAdapter)
        dropdownEnergyLevel.setText("3", false)
        dropdownEnergyLevel.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position).toString()
            dropdownEnergyLevel.setText(selected.substring(0, 1), false)
        }

        btnBack.setOnClickListener { finish() }
        btnCreateAccount.setOnClickListener { if (validateInputs()) registerUser() }
    }

    private fun validateInputs(): Boolean {
        if (etEmail.text.toString().trim().isEmpty() || etPassword.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun registerUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                authResult.user?.let { saveUserProfile(it) }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Firebase Auth Registration failed", e)
                Toast.makeText(this, "Registration failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveUserProfile(user: FirebaseUser) {
        val ageStr = etAge.text.toString().trim()
        val weightStr = etWeight.text.toString().trim()
        val energyLevelStr = dropdownEnergyLevel.text.toString().trim()

        val bioProfile = RegistrationMapper.buildBioProfile(
            ageStr.ifEmpty { null },
            weightStr.ifEmpty { null },
            energyLevelStr.ifEmpty { null }
        )

        val profileData = mapOf(
            "email" to user.email,
            "age" to ageStr,
            "weight" to weightStr,
            "energy_level" to energyLevelStr,
            "bio_max_heart_rate" to bioProfile.maxHeartRate,
            "bio_resting_bpm" to bioProfile.restingBPM,
            "bio_energy_level" to bioProfile.energyLevel,
            "created_at" to System.currentTimeMillis()
        )

        db.collection("users").document(user.uid)
            .set(profileData)
            .addOnSuccessListener {
                BioProfileStorage.save(prefs, bioProfile)
                BiometricFilter.setBioProfile(bioProfile)
                BiometricFilter.reset()

                prefs.edit()
                    .putString("user_id", user.uid)
                    .putString("email", user.email)
                    .apply()

                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Firestore Profile Save failed", e)
                Toast.makeText(this, "Profile save failed: ${e.message}", Toast.LENGTH_LONG).show()
                user.delete()
            }
    }

    companion object {
        private const val TAG = "RegisterActivity"
    }
}
