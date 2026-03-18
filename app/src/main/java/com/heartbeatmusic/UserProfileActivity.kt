package com.heartbeatmusic

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.heartbeatmusic.terminal.TerminalProfileScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UserProfileActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "UserProfileActivity"
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        auth = FirebaseAuth.getInstance()
        prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)

        val user = auth.currentUser
        val email = user?.email ?: "N/A"
        val username = prefs.getString("username", "User") ?: "User"
        val canChangePassword = user != null && user.email != null && email != "N/A"

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TerminalProfileScreen(
                        username = username,
                        email = email,
                        canChangePassword = canChangePassword,
                        onBack = { finish() },
                        onChangePassword = { showChangePasswordDialog() },
                        onLogout = { performLogout() }
                    )
                }
            }
        }
    }

    private fun showChangePasswordDialog() {
        val user = auth.currentUser ?: run {
            Toast.makeText(this, "You must be logged in with an email account to change your password.", Toast.LENGTH_SHORT).show()
            return
        }
        val userEmail = user.email ?: run {
            Toast.makeText(this, "You must be logged in with an email account to change your password.", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val dialog = AlertDialog.Builder(this, R.style.TechChangePasswordDialog)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val currentPassword = dialogView.findViewById<TextInputEditText>(R.id.et_current_password)
        val newPassword = dialogView.findViewById<TextInputEditText>(R.id.et_new_password)
        val confirmNewPassword = dialogView.findViewById<TextInputEditText>(R.id.et_confirm_new_password)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_dialog_cancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_dialog_save)

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val currentPwd = currentPassword.text?.toString() ?: ""
            val newPwd = newPassword.text?.toString() ?: ""
            val confirmPwd = confirmNewPassword.text?.toString() ?: ""

            when {
                currentPwd.isEmpty() || newPwd.isEmpty() || confirmPwd.isEmpty() ->
                    Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show()
                newPwd != confirmPwd ->
                    Toast.makeText(this, "New passwords do not match.", Toast.LENGTH_SHORT).show()
                else -> {
                    dialog.dismiss()
                    handlePasswordChange(user, userEmail, currentPwd, newPwd)
                }
            }
        }
        dialog.show()
    }

    private fun handlePasswordChange(user: FirebaseUser, email: String, currentPassword: String, newPassword: String) {
        val credential: AuthCredential = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    Log.d(TAG, "User re-authenticated successfully.")
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                Log.d(TAG, "Password updated successfully.")
                                Toast.makeText(this, "Password updated successfully. Please log in again.", Toast.LENGTH_LONG).show()
                                performLogout()
                            } else {
                                Log.e(TAG, "Error updating password: ${updateTask.exception?.message}")
                                Toast.makeText(this, "Failed to update password. ${updateTask.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Log.e(TAG, "Re-authentication failed: ${reauthTask.exception?.message}")
                    Toast.makeText(this, "Current password is incorrect or re-authentication failed.", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun performLogout() {
        auth.signOut()
        prefs.edit()
            .remove("user_id")
            .remove("username")
            .remove("email")
            .apply()
        PlayerHolder.getInstance(this).player.apply {
            stop()
            clearMediaItems()
        }
        stopService(Intent(this, PlayerService::class.java))
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
