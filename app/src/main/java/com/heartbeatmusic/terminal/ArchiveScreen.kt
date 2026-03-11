package com.heartbeatmusic.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heartbeatmusic.data.model.CollectionItem
import com.heartbeatmusic.data.model.SyncSession
import com.heartbeatmusic.data.remote.ArchiveRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ArchiveBg = Color(0xFF1A1A2E)
private val CyanBorder = Color.Cyan.copy(alpha = 0.3f)
private val CyanText = Color.Cyan
private val UnselectedGray = Color(0xFFB3B3B3)
private val CardBg = Color(0x1AFFFFFF)

private fun formatDate(date: Date): String =
    SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(date)

private fun modeIcon(mode: String): ImageVector = when (mode.uppercase()) {
    "ZEN" -> Icons.Default.Spa
    "OVERDRIVE" -> Icons.Default.Bolt
    else -> Icons.Default.Sync
}

@Composable
fun ArchiveScreen(
    archiveRepository: ArchiveRepository
) {
    var selectedTab by remember { mutableStateOf(0) }
    val sessionsFlow = remember { archiveRepository.sessionsFlow() }
    val collectionFlow = remember { archiveRepository.collectionFlow() }
    val sessions by sessionsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val collection by collectionFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ArchiveBg)
    ) {
        TabRow(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        when (selectedTab) {
            0 -> SessionsContent(sessions = sessions, archiveRepository = archiveRepository)
            1 -> CollectionContent(items = collection)
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

@Composable
private fun SessionsContent(
    sessions: List<SyncSession>,
    archiveRepository: ArchiveRepository
) {
    var expandedSessionId by remember { mutableStateOf<String?>(null) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(sessions, key = { it.id }) { session ->
            SessionCard(
                session = session,
                isExpanded = expandedSessionId == session.id,
                onToggleExpand = {
                    expandedSessionId = if (expandedSessionId == session.id) null else session.id
                },
                archiveRepository = archiveRepository
            )
        }
    }
}

@Composable
private fun SessionCard(
    session: SyncSession,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    archiveRepository: ArchiveRepository
) {
    val shortId = session.id.takeLast(8).uppercase()
    val dateStr = formatDate(Date(session.endTimestamp))
    val durationStr = "${session.durationMinutes}m"
    val songs = session.songIds.zip(session.songTitles) { id, title -> id to title }

    Column(
        modifier = Modifier
            .fillMaxWidth()
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
                Spacer(modifier = Modifier.height(8.dp))
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
                    Text(
                        text = "$dateStr | $durationStr",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = UnselectedGray
                    )
                }
            }
        }
        if (isExpanded && songs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                songs.forEach { (id, title) ->
                    SessionSongItem(
                        songId = id,
                        title = title,
                        mode = session.mode,
                        archiveRepository = archiveRepository
                    )
                }
            }
        }
    }
}

private val expandedTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 14.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

private val expandedButtonStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 12.sp,
    fontWeight = FontWeight.Bold,
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

@Composable
private fun SessionSongItem(
    songId: String,
    title: String,
    mode: String,
    archiveRepository: ArchiveRepository
) {
    val scope = rememberCoroutineScope()
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
                style = expandedTextStyle,
                color = Color.White
            )
            Text(
                text = "+ COLLECTION",
                style = expandedButtonStyle,
                color = CyanText,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            archiveRepository.addToCollection(
                                songId = songId,
                                title = title,
                                artist = "",
                                mode = mode
                            )
                        }
                    }
                )
            )
        }
    }
}

@Composable
private fun CollectionContent(items: List<CollectionItem>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.id }) { item ->
            CollectionCard(item = item)
        }
    }
}

@Composable
private fun CollectionCard(item: CollectionItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .border(1.dp, CyanBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title.ifEmpty { "Unknown" },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = item.artist.ifEmpty { "Unknown" },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = UnselectedGray
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CyanBorder.copy(alpha = 0.2f))
                    .border(1.dp, CyanBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = item.mode.uppercase(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanText
                )
            }
        }
    }
}
