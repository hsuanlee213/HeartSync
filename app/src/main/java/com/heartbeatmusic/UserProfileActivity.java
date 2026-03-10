package com.heartbeatmusic;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UserProfileActivity extends AppCompatActivity {

    private static final String TAG = "UserProfileActivity";
    private TextInputEditText etUsername;
    private TextInputEditText etEmail;
    private Button btnChangePassword;
    private Button btnAnalyzeHistory;
    private Button btnLogout;
    private SharedPreferences prefs;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);

        // Back button
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.user_profile), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Auth and SharedPreferences
        auth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        // Bind views
        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        btnChangePassword = findViewById(R.id.btn_change_password);
        btnAnalyzeHistory = findViewById(R.id.btn_analyze_history);
        btnLogout = findViewById(R.id.btn_logout);

        // 1. Load and display user profile data
        loadUserProfileData();

        // 2. Set button click listeners
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        btnAnalyzeHistory.setOnClickListener(v -> {
            Intent historyIntent = new Intent(UserProfileActivity.this, AnalyzeHistoryActivity.class);
            startActivity(historyIntent);
        });
        btnLogout.setOnClickListener(v -> performLogout());
    }

    private void loadUserProfileData() {
        FirebaseUser user = auth.getCurrentUser();
        String authenticatedEmail = "N/A";
        String displayUsername = prefs.getString("username", "Guest User");

        if (user != null && user.getEmail() != null) {
            authenticatedEmail = user.getEmail();

            etUsername.setText(authenticatedEmail);
            etEmail.setText(authenticatedEmail);

            Log.d(TAG, "User logged in. Email: " + authenticatedEmail);
        } else {
            etUsername.setText(displayUsername);
            etEmail.setText(authenticatedEmail);
            Log.d(TAG, "No Firebase user currently logged in.");
        }

        if (user == null || user.getEmail() == null || authenticatedEmail.equals("N/A")) {
            btnChangePassword.setEnabled(false);
            btnChangePassword.setAlpha(0.5f);
            Toast.makeText(this, "You must be logged in with an email account to change your password.", Toast.LENGTH_LONG).show();
        } else {
            btnChangePassword.setEnabled(true);
            btnChangePassword.setAlpha(1.0f);
        }
    }

    private void showChangePasswordDialog() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "You must be logged in with an email account to change your password.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        final EditText currentPassword = dialogView.findViewById(R.id.et_current_password);
        final EditText newPassword = dialogView.findViewById(R.id.et_new_password);
        final EditText confirmNewPassword = dialogView.findViewById(R.id.et_confirm_new_password);

        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String currentPwd = currentPassword.getText().toString();
                    String newPwd = newPassword.getText().toString();
                    String confirmPwd = confirmNewPassword.getText().toString();

                    if (currentPwd.isEmpty() || newPwd.isEmpty() || confirmPwd.isEmpty()) {
                        Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!newPwd.equals(confirmPwd)) {
                        Toast.makeText(this, "New passwords do not match.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    handlePasswordChange(user, user.getEmail(), currentPwd, newPwd);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handlePasswordChange(FirebaseUser user, String email, String currentPassword, String newPassword) {
        AuthCredential credential = EmailAuthProvider.getCredential(email, currentPassword);

        user.reauthenticate(credential)
                .addOnCompleteListener(reauthTask -> {
                    if (reauthTask.isSuccessful()) {
                        Log.d(TAG, "User re-authenticated successfully.");
                        user.updatePassword(newPassword)
                                .addOnCompleteListener(updateTask -> {
                                    if (updateTask.isSuccessful()) {
                                        Log.d(TAG, "Password updated successfully.");
                                        Toast.makeText(UserProfileActivity.this, "Password updated successfully! Please log in again.", Toast.LENGTH_LONG).show();
                                        performLogout();
                                    } else {
                                        Log.e(TAG, "Error updating password: " + updateTask.getException().getMessage());
                                        Toast.makeText(UserProfileActivity.this, "Failed to update password. " + updateTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        Log.e(TAG, "Re-authentication failed: " + reauthTask.getException().getMessage());
                        Toast.makeText(UserProfileActivity.this, "Current password is incorrect or re-authentication failed.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void performLogout() {
        auth.signOut();

        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("user_id");
        editor.remove("username");
        editor.remove("email");
        editor.apply();

        Intent loginIntent = new Intent(UserProfileActivity.this, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }
}
