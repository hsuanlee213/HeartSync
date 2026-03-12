package com.heartbeatmusic.terminal

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import coil.compose.AsyncImage
import com.heartbeatmusic.profile.UserAvatarViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

// Profile screen palette: match main screen Deep Purple style
private val ProfileBackgroundDeepPurple = Color(0xFF1A0033)
private val NeonCyan = Color(0xFF00FFFF)
private val NeonPink = Color(0xFFFF1493)
private val AvatarGradientStart = Color(0xFF1A0033)
private val AvatarGradientEnd = Color(0xFFFF1493)
private val DividerDark = Color(0xFF0D001A)
private val TextSecondary = Color(0xFFB3B3CC)
private val NeonRed = Color(0xFFFF1744)

/**
 * HeartSync Profile: Scaffold + TopAppBar (back in navigationIcon), statusBarsPadding + navigationBarsPadding on Scaffold.
 * Content: Avatar, Change Password row, Logout at bottom. No debug backgrounds or zIndex.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalProfileScreen(
    username: String,
    email: String,
    canChangePassword: Boolean,
    onBack: () -> Unit,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavController? = null,
    avatarViewModel: UserAvatarViewModel = viewModel()
) {
    var showDialog by remember { mutableStateOf(false) }
    var pendingAvatarUri by remember { mutableStateOf<Uri?>(null) }
    val avatarState by avatarViewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        avatarViewModel.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        pendingAvatarUri = uri
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = ProfileBackgroundDeepPurple,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (navController != null) {
                                navController.popBackStack()
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NeonCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ProfileBackgroundDeepPurple,
                    navigationIconContentColor = NeonCyan
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header: Avatar + User + Email
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val displayLetter = username
                    .trim()
                    .uppercase()
                    .firstOrNull()
                    ?.toString()
                    ?: "?"
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AvatarGradientStart, AvatarGradientEnd)
                            )
                        )
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val model = avatarState.avatarLocalUri ?: avatarState.avatarRemoteUrl
                    if (!model.isNullOrEmpty()) {
                        AsyncImage(
                            model = model,
                            contentDescription = "Profile photo",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = displayLetter,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = username.ifEmpty { "User" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = email.ifEmpty { "—" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Change Password row
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = if (canChangePassword) {
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDialog = true
                                onChangePassword()
                            }
                            .padding(16.dp)
                    } else {
                        Modifier.fillMaxWidth().padding(16.dp)
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = NeonCyan
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Change Password",
                        style = MaterialTheme.typography.bodyLarge,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = NeonCyan
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = DividerDark
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Logout at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NeonRed
                    ),
                    border = BorderStroke(1.dp, NeonRed)
                ) {
                    Text(
                        text = "Logout",
                        style = MaterialTheme.typography.bodyLarge,
                        color = NeonRed
                    )
                }
            }
        }
    }

    if (pendingAvatarUri != null) {
        AlertDialog(
            onDismissRequest = { pendingAvatarUri = null },
            title = { Text(text = "Use this photo?") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = pendingAvatarUri,
                        contentDescription = "Selected photo",
                        modifier = Modifier.size(160.dp).clip(CircleShape)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = pendingAvatarUri
                        pendingAvatarUri = null
                        if (uri != null) avatarViewModel.confirmAvatar(uri)
                    }
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { pendingAvatarUri = null }) { Text("Cancel") }
            },
            containerColor = ProfileBackgroundDeepPurple
        )
    }
}
