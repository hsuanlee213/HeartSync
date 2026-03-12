package com.heartbeatmusic.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * UI state for the Edit Profile screen.
 *
 * This class is future-friendly for avatar uploads: it already exposes
 * a nullable [avatarUri] plus basic upload status and error fields.
 * It does not change any existing behavior because nothing references it yet.
 */
data class EditProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val avatarUri: Uri? = null,
    val isUploadingAvatar: Boolean = false,
    val avatarUploadError: String? = null,
)

/**
 * ViewModel prepared for future avatar selection and upload.
 *
 * Right now it only holds local in-memory state; it is not wired to
 * any navigation, backend, or storage, so the rest of the app is unaffected.
 */
class EditProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState

    /**
     * Called when the user selects a new avatar image from a picker.
     * For now this only updates local state; real upload can be added later.
     */
    fun onAvatarSelected(uri: Uri) {
        _uiState.update { current ->
            current.copy(
                avatarUri = uri,
                avatarUploadError = null,
            )
        }
    }

    /**
     * Hook for future backend/storage integration when an avatar upload succeeds.
     * You can pass the final URL or any other identifier.
     */
    fun onAvatarUploadSuccess(finalUrl: String?) {
        _uiState.update { current ->
            current.copy(
                isUploadingAvatar = false,
                avatarUploadError = null,
            )
        }
    }

    /**
     * Hook for future backend/storage integration when an avatar upload fails.
     */
    fun onAvatarUploadFailed(message: String) {
        _uiState.update { current ->
            current.copy(
                isUploadingAvatar = false,
                avatarUploadError = message,
            )
        }
    }
}

