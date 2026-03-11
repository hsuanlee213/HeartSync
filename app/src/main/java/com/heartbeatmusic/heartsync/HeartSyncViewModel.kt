package com.heartbeatmusic.heartsync

import android.net.Uri
import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.heartbeatmusic.PlayerHolder
import com.heartbeatmusic.data.model.CollectionItem
import com.heartbeatmusic.data.model.Song
import com.heartbeatmusic.data.model.SyncSession
import com.heartbeatmusic.data.remote.ArchiveRepository
import com.heartbeatmusic.terminal.TerminalMode
import com.heartbeatmusic.terminal.TerminalModeHolder
import com.heartbeatmusic.terminal.toActivityMode
import com.heartbeatmusic.data.remote.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player

private const val TAG = "HeartSync"

/** Mock song for testing per AppMode. */
data class MockSong(
    val title: String,
    val artist: String,
    val firstTag: String,
    val coverColor: Color
)

/** Mock song database per AppMode. */
private val MOCK_SONGS: Map<TerminalMode, MockSong> = mapOf(
    TerminalMode.ZEN to MockSong(
        title = "Deep Meditation (Mock)",
        artist = "Zen Master",
        firstTag = "#Ambient",
        coverColor = Color(0xFF4A148C)
    ),
    TerminalMode.SYNC to MockSong(
        title = "Digital Flow (Mock)",
        artist = "Sync Project",
        firstTag = "#DeepHouse",
        coverColor = Color(0xFF00FFFF)
    ),
    TerminalMode.OVERDRIVE to MockSong(
        title = "System Overload (Mock)",
        artist = "Kinetic Band",
        firstTag = "#Techno",
        coverColor = Color(0xFFFF4500)
    )
)

/**
 * HeartSync ViewModel.
 * Exposes heart rate and playback state for UI observation.
 */
class HeartSyncViewModel(application: Application) : AndroidViewModel(application) {

    private val heartRateProvider = MockHeartRateProvider(viewModelScope, ActivityMode.CALM)

    private val _currentHeartRate = MutableStateFlow(0)
    val currentHeartRate: StateFlow<Int> = _currentHeartRate.asStateFlow()

    private val _currentMode = MutableStateFlow(ActivityMode.CALM)
    val currentMode: StateFlow<ActivityMode> = _currentMode.asStateFlow()

    private val _isMusicPlaying = MutableStateFlow(false)
    val isMusicPlaying: StateFlow<Boolean> = _isMusicPlaying.asStateFlow()

    /** True when we have an active track (panel expanded). Stays true during seek/pause. */
    private val _isPanelExpanded = MutableStateFlow(false)
    val isPanelExpanded: StateFlow<Boolean> = _isPanelExpanded.asStateFlow()

    private val _currentTrackTitle = MutableStateFlow("")
    val currentTrackTitle: StateFlow<String> = _currentTrackTitle.asStateFlow()

    private val _currentTrackArtist = MutableStateFlow("")
    val currentTrackArtist: StateFlow<String> = _currentTrackArtist.asStateFlow()

    private val _currentCoverUrl = MutableStateFlow<String?>(null)
    val currentCoverUrl: StateFlow<String?> = _currentCoverUrl.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    /** Display song (mock when no real track or when mode changes). */
    private val _displayTitle = MutableStateFlow("")
    val displayTitle: StateFlow<String> = _displayTitle.asStateFlow()

    private val _displayArtist = MutableStateFlow("")
    val displayArtist: StateFlow<String> = _displayArtist.asStateFlow()

    private val _displayFirstTag = MutableStateFlow("")
    val displayFirstTag: StateFlow<String> = _displayFirstTag.asStateFlow()

    private val _displayCoverColor = MutableStateFlow<Color?>(null)
    val displayCoverColor: StateFlow<Color?> = _displayCoverColor.asStateFlow()

    private val _currentSongId = MutableStateFlow("")
    val currentSongId: StateFlow<String> = _currentSongId.asStateFlow()

    private val _collection = MutableStateFlow<List<CollectionItem>>(emptyList())
    val collection: StateFlow<List<CollectionItem>> = _collection.asStateFlow()

    val isCurrentSongInCollection: StateFlow<Boolean> = combine(
        _currentSongId,
        _collection
    ) { songId, items ->
        songId.isNotEmpty() && items.any { it.songId == songId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val player = PlayerHolder.getInstance(application).getPlayer()
    private val libraryRepository = LibraryRepository()
    private val archiveRepository = ArchiveRepository()
    private var progressJob: Job? = null

    private var sessionStartTime: Long? = null
    private var sessionMode: TerminalMode? = null
    private val sessionSongs = mutableListOf<Pair<String, String>>()

    init {
        collectHeartRate()
        startMonitoring()
        observePlaybackState()
        archiveRepository.collectionFlow()
            .onEach { _collection.value = it }
            .launchIn(viewModelScope)
    }

    private fun observePlaybackState() {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                viewModelScope.launch(Dispatchers.Main.immediate) {
                    _isMusicPlaying.value = isPlaying
                    if (isPlaying) {
                        if (sessionStartTime == null) {
                            sessionStartTime = System.currentTimeMillis()
                            sessionMode = TerminalModeHolder.getCurrentMode()
                            val mediaItem = player.currentMediaItem
                            if (mediaItem != null) {
                                val songId = mediaItem.mediaId.takeIf { it?.isNotEmpty() == true } ?: ""
                                val title = mediaItem.mediaMetadata.title?.toString() ?: ""
                                if (title.isNotEmpty()) sessionSongs.add(songId to title)
                            }
                        }
                        startProgressUpdates()
                    } else {
                        stopProgressUpdates()
                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                viewModelScope.launch(Dispatchers.Main.immediate) {
                    val md = mediaItem?.mediaMetadata
                    _currentTrackTitle.value = md?.title?.toString() ?: ""
                    _currentTrackArtist.value = md?.artist?.toString() ?: ""
                    _currentCoverUrl.value = md?.artworkUri?.toString()
                    _isPanelExpanded.value = mediaItem != null
                    _currentSongId.value = mediaItem?.mediaId?.takeIf { it.isNotEmpty() } ?: ""
                    if (mediaItem != null) {
                        _displayTitle.value = md?.title?.toString() ?: ""
                        _displayArtist.value = md?.artist?.toString() ?: ""
                        _displayFirstTag.value = ""
                        _displayCoverColor.value = null
                        if (player.isPlaying && sessionStartTime != null) {
                            val songId = mediaItem.mediaId.takeIf { it?.isNotEmpty() == true } ?: ""
                            val title = md?.title?.toString() ?: ""
                            if (title.isNotEmpty()) sessionSongs.add(songId to title)
                        }
                    }
                }
            }
        })
        viewModelScope.launch(Dispatchers.Main.immediate) {
            _isMusicPlaying.value = player.isPlaying
            val mediaItem = player.currentMediaItem
            val md = mediaItem?.mediaMetadata
            _currentTrackTitle.value = md?.title?.toString() ?: ""
            _currentTrackArtist.value = md?.artist?.toString() ?: ""
            _currentCoverUrl.value = md?.artworkUri?.toString()
            _isPanelExpanded.value = mediaItem != null
            _currentSongId.value = mediaItem?.mediaId?.takeIf { it.isNotEmpty() } ?: ""
            if (player.isPlaying) startProgressUpdates()
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                val dur = player.duration
                val pos = player.currentPosition
                _playbackProgress.value = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
                delay(300)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    /** Sync playback state from player (e.g. when UI becomes visible). */
    fun syncPlaybackState() {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            _isMusicPlaying.value = player.isPlaying
            val mediaItem = player.currentMediaItem
            val md = mediaItem?.mediaMetadata
            _currentTrackTitle.value = md?.title?.toString() ?: ""
            _currentTrackArtist.value = md?.artist?.toString() ?: ""
            _currentCoverUrl.value = md?.artworkUri?.toString()
            _isPanelExpanded.value = mediaItem != null
            _currentSongId.value = mediaItem?.mediaId?.takeIf { it.isNotEmpty() } ?: ""
            if (mediaItem != null) {
                _displayTitle.value = md?.title?.toString() ?: ""
                _displayArtist.value = md?.artist?.toString() ?: ""
                _displayFirstTag.value = ""
                _displayCoverColor.value = null
            }
            if (player.isPlaying) startProgressUpdates()
        }
    }

    private fun collectHeartRate() {
        heartRateProvider.heartRateFlow
            .onEach { bpm ->
                _currentHeartRate.value = bpm
            }
            .launchIn(viewModelScope)
    }

    /**
     * Start monitoring heart rate.
     */
    fun startMonitoring() {
        viewModelScope.launch {
            heartRateProvider.startMonitoring()
        }
    }

    /**
     * Stop monitoring heart rate.
     */
    fun stopMonitoring() {
        heartRateProvider.stopMonitoring()
    }

    /** Play or pause playback. Loads first song if no media. */
    fun playPause() {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.currentMediaItem == null) {
                    loadAndPlayFirstSong()
                } else {
                    player.play()
                }
            }
        }
    }

    private fun loadAndPlayFirstSong() {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            setTerminalMode(TerminalModeHolder.getCurrentMode())
            _isPanelExpanded.value = true
            libraryRepository.getAllSongs(
                object : LibraryRepository.SongsCallback {
                    override fun onSuccess(songs: List<Song>?) {
                        viewModelScope.launch(Dispatchers.Main.immediate) {
                            val list = songs ?: emptyList()
                            if (list.isEmpty()) return@launch
                            val song = list[0]
                            val url = song.audioUrl ?: return@launch
                            if (url.isEmpty()) return@launch
                            val title = song.title?.takeIf { it.isNotEmpty() } ?: "Unknown"
                            val artist = song.artist?.takeIf { it.isNotEmpty() } ?: "Unknown"
                            val coverUrl = song.coverUrl
                            val songId = song.id?.takeIf { it.isNotEmpty() } ?: ""
                            val mediaItem = MediaItem.Builder()
                                .setMediaId(songId)
                                .setUri(Uri.parse(url))
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(title)
                                        .setArtist(artist)
                                        .setArtworkUri(coverUrl?.takeIf { it.isNotEmpty() }?.let { Uri.parse(it) })
                                        .build()
                                )
                                .build()
                            player.setMediaItem(mediaItem)
                            player.prepare()
                            player.play()
                            _isPanelExpanded.value = true
                            _currentSongId.value = songId
                            _displayTitle.value = title
                            _displayArtist.value = artist
                            _displayFirstTag.value = ""
                            _displayCoverColor.value = null
                        }
                    }
                    override fun onError(e: Exception?) {}
                }
            )
        }
    }

    /** Stop playback and clear media. Collapses the panel. Saves session if one was active. */
    fun stop() {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            val start = sessionStartTime
            val mode = sessionMode
            val songs = sessionSongs.toList()
            sessionStartTime = null
            sessionMode = null
            sessionSongs.clear()

            player.stop()
            player.clearMediaItems()
            _isMusicPlaying.value = false
            _isPanelExpanded.value = false
            _currentTrackTitle.value = ""
            _currentTrackArtist.value = ""
            _currentCoverUrl.value = null
            _displayTitle.value = ""
            _displayArtist.value = ""
            _displayFirstTag.value = ""
            _displayCoverColor.value = null
            _currentSongId.value = ""
            _playbackProgress.value = 0f
            stopProgressUpdates()

            if (start != null && mode != null) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    val endTime = System.currentTimeMillis()
                    val durationMinutes = ((endTime - start) / 60_000).toInt().coerceAtLeast(0)
                    val session = SyncSession(
                        id = "session_${start}",
                        userId = uid,
                        mode = mode.name,
                        startTimestamp = start,
                        endTimestamp = endTime,
                        durationMinutes = durationMinutes,
                        songIds = songs.map { it.first },
                        songTitles = songs.map { it.second }
                    )
                    viewModelScope.launch {
                        archiveRepository.saveSession(session)
                            .onFailure { Log.e(TAG, "Failed to save session", it) }
                    }
                }
            }
        }
    }

    /** Go to previous track or restart current. */
    fun previous() {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            if (player.hasPreviousMediaItem()) {
                player.seekToPrevious()
            } else {
                player.seekTo(0)
            }
        }
    }

    /** Go to next track. */
    fun next() {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            if (player.hasNextMediaItem()) {
                player.seekToNext()
            }
        }
    }

    /** Seek to position (0f..1f). */
    fun seekToProgress(progress: Float) {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            val dur = player.duration
            if (dur > 0) {
                val pos = (progress * dur).toLong().coerceIn(0L, dur)
                player.seekTo(pos)
            }
        }
    }

    /**
     * Switch activity mode, affects BPM range emitted by Mock provider.
     */
    fun setMode(mode: ActivityMode) {
        _currentMode.value = mode
        if (heartRateProvider is MockHeartRateProvider) {
            heartRateProvider.setMode(mode)
        }
    }

    /** Toggle current track in Collection: add if not in, remove if in. Syncs with Archive tab. */
    fun toggleCollection() {
        viewModelScope.launch(Dispatchers.IO) {
            val songId = _currentSongId.value
            if (songId.isEmpty()) return@launch
            val inCollection = _collection.value.any { it.songId == songId }
            val title = _currentTrackTitle.value.ifEmpty { _displayTitle.value }.ifEmpty { "Unknown" }
            val artist = _currentTrackArtist.value.ifEmpty { _displayArtist.value }
            val coverUrl = _currentCoverUrl.value ?: ""
            val mode = TerminalModeHolder.getCurrentMode().name

            if (inCollection) {
                val removed = _collection.value.find { it.songId == songId }
                _collection.value = _collection.value.filter { it.songId != songId }
                archiveRepository.removeFromCollection(songId)
                    .onFailure {
                        Log.e(TAG, "Failed to remove from collection", it)
                        withContext(Dispatchers.Main.immediate) {
                            Toast.makeText(getApplication(), "無法從收藏移除", Toast.LENGTH_SHORT).show()
                        }
                        removed?.let { _collection.value = _collection.value + it }
                    }
            } else {
                val newItem = CollectionItem(
                    id = songId,
                    songId = songId,
                    title = title,
                    artist = artist,
                    mode = mode,
                    coverUrl = coverUrl
                )
                _collection.value = _collection.value + newItem
                archiveRepository.addToCollection(songId, title, artist, mode, coverUrl)
                    .onFailure {
                        Log.e(TAG, "Failed to add to collection", it)
                        withContext(Dispatchers.Main.immediate) {
                            Toast.makeText(getApplication(), "無法加入收藏（請確認已登入）", Toast.LENGTH_SHORT).show()
                        }
                        _collection.value = _collection.value.filter { it.songId != songId }
                    }
            }
        }
    }

    /**
     * Switch Terminal mode. Updates display to mock song and logs.
     */
    fun setTerminalMode(mode: TerminalMode) {
        setMode(mode.toActivityMode())
        val mock = MOCK_SONGS[mode] ?: return
        _displayTitle.value = mock.title
        _displayArtist.value = mock.artist
        _displayFirstTag.value = mock.firstTag
        _displayCoverColor.value = mock.coverColor
        val tags = mode.musicTags
        val bpm = _currentHeartRate.value
        Log.i(TAG, "Mode Changed: $mode, Fetching Tags: $tags, Target BPM: $bpm")
    }

    override fun onCleared() {
        super.onCleared()
        heartRateProvider.stopMonitoring()
    }
}
