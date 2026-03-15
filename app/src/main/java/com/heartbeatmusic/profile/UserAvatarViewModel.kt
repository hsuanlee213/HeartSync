package com.heartbeatmusic.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.heartbeatmusic.data.local.AvatarRepository
import com.heartbeatmusic.data.local.UserProfileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AvatarUiState(
    val avatarLocalUri: String? = null,
    val avatarRemoteUrl: String? = null
)

/**
 * Holds avatar state and performs local-first updates with background Firebase sync.
 */
@HiltViewModel
class UserAvatarViewModel @Inject constructor(
    private val repo: AvatarRepository
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(AvatarUiState())
    val uiState: StateFlow<AvatarUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            repo.userProfileFlow(userId)
                .onEach { entity: UserProfileEntity? ->
                    _uiState.value = AvatarUiState(
                        avatarLocalUri = entity?.avatarLocalUri,
                        avatarRemoteUrl = entity?.avatarRemoteUrl
                    )
                }
                .launchIn(viewModelScope)
        }
    }

    /**
     * User confirmed a picked image. Updates local storage immediately and syncs to Firebase in background.
     */
    fun confirmAvatar(pickedUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.setAvatarFromPickedUri(pickedUri)
                .onSuccess { _events.tryEmit("Profile photo updated.") }
                .onFailure { _events.tryEmit("Failed to update photo: ${it.message ?: "Unknown error"}") }
        }
    }
}

