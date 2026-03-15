package com.heartbeatmusic.terminal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heartbeatmusic.R
import com.heartbeatmusic.data.model.CollectionItem
import com.heartbeatmusic.data.model.SyncSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ArchiveBg = Color(0xFF1A1A2E)
private val CyanBorder = Color.Cyan.copy(alpha = 0.3f)
private val CyanText = Color.Cyan
private val UnselectedGray = Color(0xFFB3B3B3)
private val CardBg = Color(0xFF252540)
private val SwipeDeleteBg = Color(0xFF8B0000)
private val SwipeDeleteBorder = Color(0xFFDC143C)

/** Align with TerminalMode.accentColor for consistent visual language. */
private fun modeTagColors(mode: String): Pair<Color, Color> {
    val accent = when (mode.uppercase()) {
        "ZEN" -> TerminalMode.ZEN.accentColor
        "OVERDRIVE" -> TerminalMode.OVERDRIVE.accentColor
        else -> TerminalMode.SYNC.accentColor
    }
    return accent.copy(alpha = 0.3f) to accent
}

private fun formatDate(date: Date): String =
    SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(date)

private fun modeIcon(mode: String): ImageVector = when (mode.uppercase()) {
    "ZEN" -> Icons.Default.Spa
    "OVERDRIVE" -> Icons.Default.Bolt
    else -> Icons.Default.Sync
}

@Composable
private fun ArchiveSnackbar(snackbarData: SnackbarData) {
    Snackbar(
        action = {
            snackbarData.visuals.actionLabel?.let { label ->
                TextButton(
                    onClick = { snackbarData.performAction() },
                    colors = ButtonDefaults.textButtonColors(contentColor = CyanText)
                ) {
                    Text(
                        text = label,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        containerColor = Color(0xFF1A1A1A),
        contentColor = Color.White,
        actionContentColor = CyanText,
        content = {
            Text(
                text = snackbarData.visuals.message,
                color = Color.White
            )
        }
    )
}

@Composable
fun ArchiveScreen(viewModel: ArchiveViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val sessions by viewModel.sessions.collectAsStateWithLifecycle(initialValue = emptyList())
    val restoreTokens by viewModel.restoreTokens.collectAsStateWithLifecycle(initialValue = emptyMap())
    val collection by viewModel.collection.collectAsStateWithLifecycle(initialValue = emptyList())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArchiveBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)).togetherWith(fadeOut(animationSpec = tween(200)))
                },
                label = "ArchiveTab"
            ) { tab ->
                when (tab) {
                    0 -> SessionsContent(
                        sessions = sessions,
                        restoreTokens = restoreTokens,
                        viewModel = viewModel,
                        snackbarHostState = snackbarHostState
                    )
                    else -> CollectionContent(
                        items = collection,
                        viewModel = viewModel,
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) { data ->
            ArchiveSnackbar(snackbarData = data)
        }
    }
}

@Composable
private fun TabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        listOf("SESSIONS", "COLLECTION").forEachIndexed { index, label ->
            val isSelected = selectedTab == index
            Tab(
                label = label,
                isSelected = isSelected,
                onClick = { onTabSelected(index) }
            )
        }
    }
}

@Composable
private fun Tab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) CyanText else UnselectedGray
        )
        if (isSelected) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(2.dp)
                    .background(CyanText)
            )
        }
    }
}

private val TrashRevealWidth = 80.dp
private val SnapThreshold = 40.dp

@Composable
private fun SessionsContent(
    sessions: List<SyncSession>,
    restoreTokens: Map<String, Int>,
    viewModel: ArchiveViewModel,
    snackbarHostState: SnackbarHostState
) {
    var expandedSessionId by remember { mutableStateOf<String?>(null) }
    var deletingSessionIds by remember { mutableStateOf(setOf<String>()) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    fun handleTrashClick(session: SyncSession) {
        scope.launch {
            deletingSessionIds = deletingSessionIds + session.id
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(350)
            viewModel.removeSessionFromUI(session.id)
            deletingSessionIds = deletingSessionIds - session.id
            // Persist delete immediately so state stays consistent across tabs and restarts.
            viewModel.deleteFromDb(session.id)
            val result = snackbarHostState.showSnackbar(
                message = "Session removed",
                actionLabel = "UNDO",
                duration = SnackbarDuration.Long
            )
            when (result) {
                SnackbarResult.ActionPerformed -> {
                    viewModel.restoreSession(session)
                    viewModel.saveSession(session)
                }
                SnackbarResult.Dismissed -> { /* already deleted above */ }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(sessions, key = { "${it.id}_${restoreTokens[it.id] ?: 0}" }) { session ->
            SessionListItemWithReveal(
                session = session,
                isExpanded = expandedSessionId == session.id,
                onToggleExpand = {
                    expandedSessionId = if (expandedSessionId == session.id) null else session.id
                },
                isDeleting = session.id in deletingSessionIds,
                onTrashClick = { handleTrashClick(session) },
                viewModel = viewModel,
                density = density
            )
        }
    }
}

@Composable
private fun SwipeToRevealContainer(
    isDeleting: Boolean,
    onTrashClick: () -> Unit,
    density: androidx.compose.ui.unit.Density,
    content: @Composable () -> Unit
) {
    val offsetPx = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val maxOffsetPx = with(density) { TrashRevealWidth.toPx() }
    val thresholdPx = with(density) { SnapThreshold.toPx() }

    LaunchedEffect(isDeleting) {
        if (isDeleting) offsetPx.snapTo(0f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(350))
            .heightIn(min = if (isDeleting) 0.dp else 1.dp)
    ) {
        if (isDeleting) return@Box

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(TrashRevealWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SwipeDeleteBg)
                    .border(1.dp, SwipeDeleteBorder, RoundedCornerShape(12.dp))
                    .clickable(onClick = onTrashClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offsetPx.value.toInt(), 0) }
                    .clickable(
                        enabled = offsetPx.value != 0f,
                        onClick = {
                            scope.launch {
                                offsetPx.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                    )
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    val target = if (offsetPx.value < -thresholdPx) -maxOffsetPx else 0f
                                    offsetPx.animateTo(
                                        targetValue = target,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                scope.launch {
                                    offsetPx.snapTo(
                                        (offsetPx.value + dragAmount).coerceIn(-maxOffsetPx, 0f)
                                    )
                                }
                            }
                        )
                    }
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SessionListItemWithReveal(
    session: SyncSession,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    isDeleting: Boolean,
    onTrashClick: () -> Unit,
    viewModel: ArchiveViewModel,
    density: androidx.compose.ui.unit.Density
) {
    SwipeToRevealContainer(isDeleting, onTrashClick, density) {
        SessionCard(session = session, isExpanded = isExpanded, onToggleExpand = onToggleExpand, viewModel = viewModel)
    }
}

private val detailLabelStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 14.sp,
    fontWeight = FontWeight.Medium,
    color = Color.White,
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

@Composable
private fun SessionCard(
    session: SyncSession,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    viewModel: ArchiveViewModel
) {
    val shortId = session.id.takeLast(8).uppercase()
    val dateStr = formatDate(Date(session.endTimestamp))
    val durationSec = ((session.endTimestamp - session.startTimestamp) / 1_000).toInt().coerceAtLeast(0)
    val durationStr = if (durationSec >= 60) "${durationSec / 60}m" else "${durationSec}s"
    val songCount = session.songIds.size
    val songs = session.songIds.indices.map { i ->
        Triple(
            session.songIds[i],
            session.songTitles.getOrElse(i) { "" },
            session.songArtists.getOrElse(i) { "" }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(200))
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .border(1.dp, CyanBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onToggleExpand
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "[ SESSION_$shortId ]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanText
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = modeIcon(session.mode),
                        contentDescription = session.mode,
                        tint = CyanBorder,
                        modifier = Modifier.size(18.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "DATE: $dateStr",
                            style = detailLabelStyle
                        )
                        Text(
                            text = "DURATION: $durationStr",
                            style = detailLabelStyle
                        )
                        Text(
                            text = "SONGS: $songCount",
                            style = detailLabelStyle
                        )
                    }
                }
            }
        }
        if (isExpanded && songs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                songs.forEach { (id, title, artist) ->
                    SessionSongItem(
                        songId = id,
                        title = title,
                        artist = artist,
                        mode = session.mode,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

private val expandedTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 14.sp,
    color = Color.White,
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

private val expandedButtonStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 12.sp,
    fontWeight = FontWeight.Bold,
    color = CyanText,
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

@Composable
private fun SessionSongItem(
    songId: String,
    title: String,
    artist: String,
    mode: String,
    viewModel: ArchiveViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CyanBorder.copy(alpha = 0.1f))
            .border(1.dp, CyanBorder.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.ifEmpty { "Unknown" },
                style = expandedTextStyle
            )
            Text(
                text = "+ COLLECTION",
                style = expandedButtonStyle,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {
                        viewModel.addToCollection(songId, title, artist, mode)
                    }
                )
            )
        }
    }
}

@Composable
private fun CollectionContent(
    items: List<CollectionItem>,
    viewModel: ArchiveViewModel,
    snackbarHostState: SnackbarHostState
) {
    var deletingCollectionIds by remember { mutableStateOf(setOf<String>()) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    fun handleTrashClick(item: CollectionItem) {
        scope.launch {
            deletingCollectionIds = deletingCollectionIds + item.id
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(350)
            deletingCollectionIds = deletingCollectionIds - item.id
            viewModel.removeFromCollection(item.songId, item.mode)
            val result = snackbarHostState.showSnackbar(
                message = "Song removed from collection",
                actionLabel = "UNDO",
                duration = SnackbarDuration.Long
            )
            when (result) {
                SnackbarResult.ActionPerformed -> {
                    viewModel.addToCollection(
                        item.songId,
                        item.title,
                        item.artist,
                        item.mode,
                        item.coverUrl
                    )
                }
                SnackbarResult.Dismissed -> { }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items, key = { it.id }) { item ->
            CollectionListItemWithReveal(
                item = item,
                isDeleting = item.id in deletingCollectionIds,
                onTrashClick = { handleTrashClick(item) },
                density = density
            )
        }
    }
}

@Composable
private fun CollectionListItemWithReveal(
    item: CollectionItem,
    isDeleting: Boolean,
    onTrashClick: () -> Unit,
    density: androidx.compose.ui.unit.Density
) {
    SwipeToRevealContainer(isDeleting, onTrashClick, density) {
        CollectionCard(item = item, onClick = { /* TODO: play or show detail */ })
    }
}

@Composable
private fun CollectionCard(
    item: CollectionItem,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(16.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.15f))
        ) {
            if (item.coverUrl.isNotEmpty()) {
                AsyncImage(
                    model = item.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.ic_music_note),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.6f))
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.title.ifEmpty { "Unknown" },
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.artist.ifEmpty { "Unknown" },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                val (bgColor, textColor) = modeTagColors(item.mode)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(bgColor)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.mode.uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
        }
    }
}
