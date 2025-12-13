package edu.northeastern.group13project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat; // <-- NEW IMPORT
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
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

        // --- 确保返回按钮显示的设置 START ---
        MaterialToolbar toolbar = findViewById(R.id.toolbar_profile);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                // 启用向上导航按钮（返回箭头）
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                // 设置标题，虽然 XML 中已有，但这里再次设置以确保
                getSupportActionBar().setTitle("Edit Profile");

                // 确保返回箭头（导航图标）是白色的，与标题颜色一致
                // 假设您的颜色资源中包含 R.color.white 或使用 android.R.color.white
                // 由于我们无法访问您的资源文件，我们使用 setNavigationIconTint
                // 请注意：在较新的 MaterialToolbar 版本中，这通常是默认设置，但我们强制它。
                try {
                    toolbar.setNavigationIconTint(ContextCompat.getColor(this, android.R.color.white));
                } catch (Exception e) {
                    // Log error if tint fails (e.g., color resource not found)
                    Log.e(TAG, "Failed to set navigation icon tint.", e);
                }
            }
        }
        // --- 确保返回按钮显示的设置 END ---

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
            // Intent to AnalyzeHistoryActivity
            Intent historyIntent = new Intent(UserProfileActivity.this, AnalyzeHistoryActivity.class);
            startActivity(historyIntent);
        });
        btnLogout.setOnClickListener(v -> performLogout());
    }

    /**
     * Handles the Up navigation (back arrow) click event.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // 结束当前 Activity，返回到堆栈中的上一个 Activity
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Loads user profile data. Now prioritizes displaying the Firebase authenticated Email to match the user login method.
     */
    private void loadUserProfileData() {
        FirebaseUser user = auth.getCurrentUser();
        String authenticatedEmail = "N/A";
        String displayUsername = prefs.getString("username", "Guest User");

        if (user != null && user.getEmail() != null) {
            authenticatedEmail = user.getEmail();

            // Display the Email in both "Username" and "Email" fields
            etUsername.setText(authenticatedEmail);
            etEmail.setText(authenticatedEmail);

            Log.d(TAG, "User logged in. Email: " + authenticatedEmail);
        } else {
            // If not logged in, display the locally stored custom username
            etUsername.setText(displayUsername);
            etEmail.setText(authenticatedEmail);
            Log.d(TAG, "No Firebase user currently logged in.");
        }

        // Logic to disable the change password button
        if (user == null || user.getEmail() == null || authenticatedEmail.equals("N/A")) {
            btnChangePassword.setEnabled(false);
            btnChangePassword.setAlpha(0.5f);
            Toast.makeText(this, "You must be logged in with an email account to change your password.", Toast.LENGTH_LONG).show();
        } else {
            btnChangePassword.setEnabled(true);
            btnChangePassword.setAlpha(1.0f);
        }
    }

    /**
     * Shows a dialog to collect current and new password, performs Firebase re-authentication,
     * updates the password, and forces a logout upon success.
     */
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

                    // Proceed with Firebase operations
                    handlePasswordChange(user, user.getEmail(), currentPwd, newPwd);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Handles the core Firebase logic: re-authentication and password update.
     */
    private void handlePasswordChange(FirebaseUser user, String email, String currentPassword, String newPassword) {
        // 1. Create a credential using the user's current email and password
        AuthCredential credential = EmailAuthProvider.getCredential(email, currentPassword);

        // 2. Re-authenticate the user
        user.reauthenticate(credential)
                .addOnCompleteListener(reauthTask -> {
                    if (reauthTask.isSuccessful()) {
                        Log.d(TAG, "User re-authenticated successfully.");
                        // 3. Update the password
                        user.updatePassword(newPassword)
                                .addOnCompleteListener(updateTask -> {
                                    if (updateTask.isSuccessful()) {
                                        Log.d(TAG, "Password updated successfully.");
                                        Toast.makeText(UserProfileActivity.this, "Password updated successfully! Please log in again.", Toast.LENGTH_LONG).show();
                                        // 4. Force logout as required
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

    /**
     * Performs complete logout, clearing local state and navigating to the Login screen.
     */
    private void performLogout() {
        // Sign out from Firebase Auth
        auth.signOut();

        // Clear user data from SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("user_id");
        editor.remove("username");
        editor.remove("email");
        editor.apply();

        // Navigate to Login Activity and clear all activity stack
        Intent loginIntent = new Intent(UserProfileActivity.this, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }
}