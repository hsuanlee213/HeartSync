package com.heartbeatmusic.terminal

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

// Profile screen palette: match main screen Deep Purple style
private val ProfileBackgroundDeepPurple = Color(0xFF1A0033)
private val NeonCyan = Color(0xFF00FFFF)
private val NeonPink = Color(0xFFFF1493)
private val AvatarGradientStart = Color(0xFF1A0033)
private val AvatarGradientEnd = Color(0xFFFF1493)
private val DividerDark = Color(0xFF0D001A)
private val TextSecondary = Color(0xFFB3B3CC)
/** Bright neon red for Logout button: full opacity, high contrast on deep purple */
private val NeonRed = Color(0xFFFF1744)

/**
 * HeartSync Profile screen: all-English, Deep Purple theme, neon cyan accents,
 * avatar gradient (deep purple → neon pink), Change Password menu row, Logout at bottom.
 */
@Composable
fun TerminalProfileScreen(
    username: String,
    email: String,
    canChangePassword: Boolean,
    onBack: () -> Unit,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ProfileBackgroundDeepPurple)
    ) {
        // Top bar: Back button (neon blue)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = NeonCyan
                )
            }
        }

        // Header: Avatar + User + Email
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Avatar: 100.dp circle, gradient Deep Purple → Neon Pink, white bold letter
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
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayLetter,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
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

        // Menu: Change Password row (Lock + text + Arrow, all neon cyan; divider below)
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .then(
                        if (canChangePassword) Modifier.clickable(onClick = onChangePassword)
                        else Modifier
                    ),
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

        // Push content up so Logout sits at bottom
        Spacer(modifier = Modifier.weight(1f))

        // Bottom: Logout (OutlinedButton, neon red, full opacity, high contrast)
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
