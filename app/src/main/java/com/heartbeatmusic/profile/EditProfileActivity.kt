package com.heartbeatmusic.profile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/**
 * Simple activity hosting [EditProfileScreen] for future use.
 *
 * This activity is declared in the manifest but never launched from
 * existing flows, so it does not change the current user experience.
 * You can manually start it later when you are ready to integrate
 * avatar editing into the app.
 */
class EditProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EditProfileScreen(
                onBack = { finish() }
            )
        }
    }
}

