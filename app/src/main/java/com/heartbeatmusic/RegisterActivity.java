package com.heartbeatmusic;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.heartbeatmusic.biometric.BioProfile;
import com.heartbeatmusic.biometric.BioProfileStorage;
import com.heartbeatmusic.biometric.BiometricFilter;
import com.heartbeatmusic.biometric.RegistrationMapper;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private EditText etEmail, etPassword, etAge, etWeight;
    private AutoCompleteTextView dropdownEnergyLevel;
    private Button btnCreateAccount;
    private ImageButton btnBack;

    // Firebase Auth is the primary authentication method
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase services
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        // NOTE: et_reg_username element MUST be removed from activity_register.xml
        // etUsername = findViewById(R.id.et_reg_username); // Removed binding

        etEmail = findViewById(R.id.et_reg_email);
        etPassword = findViewById(R.id.et_reg_password);
        etAge = findViewById(R.id.et_age);
        etWeight = findViewById(R.id.et_weight);
        dropdownEnergyLevel = findViewById(R.id.dropdown_energy_level);
        btnCreateAccount = findViewById(R.id.btn_create_account);
        btnBack = findViewById(R.id.btn_back_register);

        ArrayAdapter<CharSequence> energyAdapter = ArrayAdapter.createFromResource(this,
                R.array.energy_levels, android.R.layout.simple_dropdown_item_1line);
        dropdownEnergyLevel.setAdapter(energyAdapter);
        dropdownEnergyLevel.setText("3", false);  // Collapsed: show number only
        dropdownEnergyLevel.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            String numOnly = selected.substring(0, 1);  // "1", "2", "3", "4", or "5"
            dropdownEnergyLevel.setText(numOnly, false);
        });

        btnBack.setOnClickListener(v -> finish());

        btnCreateAccount.setOnClickListener(v -> {
            if (validateInputs()) {
                registerUser();
            }
        });
    }

    private boolean validateInputs() {
        // Only checking Email and Password, as Username field is removed.
        if (etEmail.getText().toString().trim().isEmpty() ||
                etPassword.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void registerUser() {
        // final String username = etUsername.getText().toString().trim(); // Removed
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        // Step 1: Create user using Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        // Step 2: Store profile data in Firestore. No separate 'username' needed from UI input.
                        // We will use the email as the primary display identifier.
                        saveUserProfile(user);
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle common registration errors
                    Log.e(TAG, "Firebase Auth Registration failed", e);
                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveUserProfile(FirebaseUser user) {
        String userId = user.getUid();
        String ageStr = etAge.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();
        String energyLevelStr = dropdownEnergyLevel.getText().toString().trim();

        // Build Bio-Profile with defaults (age=25, RHR=70, energyLevel=3)
        BioProfile bioProfile = RegistrationMapper.INSTANCE.buildBioProfile(
                ageStr.isEmpty() ? null : ageStr,
                weightStr.isEmpty() ? null : weightStr,
                energyLevelStr.isEmpty() ? null : energyLevelStr);

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("email", user.getEmail());
        profileData.put("age", ageStr);
        profileData.put("weight", weightStr);
        profileData.put("energy_level", energyLevelStr);
        profileData.put("bio_max_heart_rate", bioProfile.getMaxHeartRate());
        profileData.put("bio_resting_bpm", bioProfile.getRestingBPM());
        profileData.put("bio_energy_level", bioProfile.getEnergyLevel());
        profileData.put("created_at", System.currentTimeMillis());

        // Use set() and UID to create a unique document for this user
        db.collection("users").document(userId)
                .set(profileData)
                .addOnSuccessListener(aVoid -> {
                    // Save Bio-Profile to local SharedPreferences
                    BioProfileStorage.INSTANCE.save(prefs, bioProfile);
                    BiometricFilter.INSTANCE.setBioProfile(bioProfile);
                    BiometricFilter.INSTANCE.reset();

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("user_id", userId);
                    editor.putString("email", user.getEmail());
                    editor.apply();

                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();

                    // Navigate to the main activity
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore Profile Save failed", e);
                    Toast.makeText(this, "Profile save failed after authentication: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // Optional: Delete the FirebaseAuth user here for cleanup
                    user.delete();
                });
    }
}
