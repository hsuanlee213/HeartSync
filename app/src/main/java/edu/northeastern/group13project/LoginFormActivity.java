package edu.northeastern.group13project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginFormActivity extends AppCompatActivity {

    private static final String TAG = "LoginFormActivity";
    // NOTE: This EditText (etEmail) corresponds to et_username in the XML, and MUST be the user's registered email.
    private EditText etEmail;
    private EditText etPassword;
    private Button btnSignIn;
    private ImageButton btnBack;

    // Firebase instances
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_form);

        // Initialize Firebase services and SharedPreferences
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        // Note: et_username is used here for Email input. YOU MUST change the hint/label in the XML layout to "User Email".
        etEmail = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnSignIn = findViewById(R.id.btn_sign_in);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());

        btnSignIn.setOnClickListener(v -> {
            // Must be Email
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            loginUser(email, password);
        });
    }

    private void loginUser(String email, String password) {
        // Step 1: Sign in using Firebase Auth
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        // Step 2: Authentication successful, retrieve custom profile data (if any)
                        retrieveCustomProfile(user);
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle login failure (e.g., wrong password, user not found)
                    Log.e(TAG, "Firebase Auth Login failed", e);
                    Toast.makeText(this, "Login failed: Invalid email or password.", Toast.LENGTH_SHORT).show();
                });
    }

    private void retrieveCustomProfile(FirebaseUser user) {
        // We still retrieve the user document to get additional info, even if we don't use 'username' for login/display.
        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    // Fetch the username if it exists in Firestore, otherwise default to a generic name
                    String username = "User";
                    if (documentSnapshot.exists() && documentSnapshot.contains("username")) {
                        username = documentSnapshot.getString("username");
                    }

                    // Step 4: Save local preferences and navigate
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("user_id", user.getUid());
                    // Save custom username (if retrieved)
                    editor.putString("username", username);
                    // Ensure Email is saved correctly
                    editor.putString("email", user.getEmail());
                    editor.apply();

                    // Navigate to the main activity
                    Intent intent = new Intent(LoginFormActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to retrieve user profile after login.", e);
                    // Even if Firestore fails, the user is still logged in via Auth. Navigate anyway.
                    Toast.makeText(this, "Login successful, but profile retrieval failed. Some features may be limited.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(LoginFormActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
    }
}