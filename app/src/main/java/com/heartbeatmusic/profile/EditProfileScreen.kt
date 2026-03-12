package com.heartbeatmusic.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

/**
 * Placeholder Edit Profile screen prepared for future avatar selection.
 *
 * This composable is not yet wired into any navigation graph or activity,
 * so creating it does not change current app behavior. It is safe to keep
 * in the project until you decide to hook it up.
 */
@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel = viewModel(),
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    // System photo picker launcher. It only updates local state; no upload yet.
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onAvatarSelected(uri)
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            AvatarSection(
                avatarUri = uiState.avatarUri,
                isUploading = uiState.isUploadingAvatar,
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Placeholder content to make it clear this is a separate test screen.
            Text(
                text = "Edit Profile (placeholder screen)",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}

/**
 * Avatar area that can either show a placeholder icon or a selected image.
 * The visuals are simple on purpose and can be styled to match your theme later.
 */
@Composable
private fun AvatarSection(
    avatarUri: Uri?,
    isUploading: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(Color.DarkGray)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUri != null) {
            AsyncImage(
                model = avatarUri,
                contentDescription = "Avatar",
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Avatar placeholder",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }

        if (isUploading) {
            // Reserved for a future loading indicator overlay (e.g. CircularProgressIndicator).
        }
    }
}

