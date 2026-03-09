package com.heartbeatmusic.login

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heartbeatmusic.R

private val DeepPurple = Color(0xFF2A004D)
private val ColdWhite = Color(0xFFF0F4F8)
private val CyanGlow = Color(0xFF00FFFF)

@Composable
fun LoginScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepPurple)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "HeartSync",
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = ColdWhite
        )

        Text(
            text = "AUTHENTICATION PROTOCOL",
            fontSize = 12.sp,
            color = ColdWhite.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Image(
            painter = painterResource(R.drawable.login_logo),
            contentDescription = "HeartSync Logo",
            modifier = Modifier
                .size(160.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
        )

        Spacer(modifier = Modifier.height(40.dp))

        WireframeLoginButton(
            isLoading = isLoading,
            onClick = {
                isLoading = true
                onLoginClick()
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "First time user?",
            fontSize = 14.sp,
            color = ColdWhite.copy(alpha = 0.8f)
        )

        Text(
            text = "INITIATE NEW USER PROTOCOL",
            fontSize = 12.sp,
            color = ColdWhite,
            modifier = Modifier
                .padding(top = 4.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onRegisterClick() }
        )
    }
}

@Composable
private fun WireframeLoginButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val loadAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loadAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth(0.85f)
            .height(56.dp)
            .drawBehind {
                val chamfer = 12f
                val outerPath = Path().apply {
                    moveTo(chamfer, 0f)
                    lineTo(size.width - chamfer, 0f)
                    lineTo(size.width, chamfer)
                    lineTo(size.width, size.height - chamfer)
                    lineTo(size.width - chamfer, size.height)
                    lineTo(chamfer, size.height)
                    lineTo(0f, size.height - chamfer)
                    lineTo(0f, chamfer)
                    close()
                }
                drawPath(
                    path = outerPath,
                    color = CyanGlow,
                    style = Stroke(width = 2f)
                )
                val innerChamfer = 6f
                val innerPath = Path().apply {
                    moveTo(chamfer + innerChamfer, innerChamfer)
                    lineTo(size.width - chamfer - innerChamfer, innerChamfer)
                    lineTo(size.width - innerChamfer, chamfer + innerChamfer)
                    lineTo(size.width - innerChamfer, size.height - chamfer - innerChamfer)
                    lineTo(size.width - chamfer - innerChamfer, size.height - innerChamfer)
                    lineTo(chamfer + innerChamfer, size.height - innerChamfer)
                    lineTo(innerChamfer, size.height - chamfer - innerChamfer)
                    lineTo(innerChamfer, chamfer + innerChamfer)
                    close()
                }
                drawPath(
                    path = innerPath,
                    color = CyanGlow,
                    style = Stroke(width = 1.5f)
                )
            }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isLoading) "LOADING..." else "Login",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = CyanGlow,
            modifier = Modifier.alpha(if (isLoading) loadAlpha else 1f)
        )
    }
}
