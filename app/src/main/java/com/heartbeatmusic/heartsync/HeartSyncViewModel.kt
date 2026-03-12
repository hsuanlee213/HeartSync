package com.heartbeatmusic.heartsync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.app.Application
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.heartbeatmusic.PlayerHolder
import com.heartbeatmusic.data.model.CollectionItem
import com.heartbeatmusic.data.model.Song
import com.heartbeatmusic.data.model.SyncSession
import com.heartbeatmusic.data.local.CollectionRepository
import com.heartbeatmusic.data.remote.ArchiveRepository
import com.heartbeatmusic.terminal.TerminalMode
import com.heartbeatmusic.terminal.TerminalModeHolder
import com.heartbeatmusic.terminal.toActivityMode
import com.heartbeatmusic.R
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
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import java.net.SocketTimeoutException
import java.net.UnknownHostException

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

    /** "API" | "Local" | "Idle" - for debug UI when BuildConfig.DEBUG */
    private val _playbackSource = MutableStateFlow("Idle")
    val playbackSource: StateFlow<String> = _playbackSource.asStateFlow()

    private var suppressIsPlayingFalseForNetworkFallback = false
    /** When true, allow onIsPlayingChanged to set _isMusicPlaying=false (user pressed Pause). */
    private var userRequestedPause = false

    /** Mode that owns the current playback. Null when nothing playing. */
    private val _playingMode = MutableStateFlow<TerminalMode?>(null)
    val playingMode: StateFlow<TerminalMode?> = _playingMode.asStateFlow()

    /** True only when current mode is the one playing. Other modes show Play. */
    val isPlayingInCurrentMode: StateFlow<Boolean> = combine(
        _isMusicPlaying,
        _playingMode,
        TerminalModeHolder.selectedMode
    ) { playing, pm, current ->
        playing && pm == current
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

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

    /** Heart filled only when current song + current mode exist in collection. */
    val isCurrentSongInCollection: StateFlow<Boolean> = combine(
        _currentSongId,
        _collection,
        TerminalModeHolder.selectedMode
    ) { songId, items, mode ->
        songId.isNotEmpty() && items.any { it.songId == songId && it.mode == mode.name }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val player = PlayerHolder.getInstance(application).getPlayer()
    private val libraryRepository = LibraryRepository()
    private val archiveRepository = ArchiveRepository()
    private val collectionRepository = CollectionRepository(application, archiveRepository)
    private var progressJob: Job? = null

    private var sessionStartTime: Long? = null
    private var sessionMode: TerminalMode? = null
    private val sessionSongs = mutableListOf<Pair<String, String>>()

    init {
        collectHeartRate()
        startMonitoring()
        observePlaybackState()
        initPlaybackState()
        collectionRepository.collectionFlow()
            .onEach { _collection.value = it }
            .launchIn(viewModelScope)
        viewModelScope.launch(Dispatchers.IO) {
            collectionRepository.syncFromFirestore()
        }
    }

    private fun observePlaybackState() {
        val vm = this
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                viewModelScope.launch(Dispatchers.Main.immediate) {
                    if (!isPlaying) {
                        if (suppressIsPlayingFalseForNetworkFallback) return@launch
                        if (_playbackSource.value == "Local" && !userRequestedPause) {
                            Log.d(TAG, "HeartSync_Debug: Ignoring isPlaying=false (Local source, not user pause)")
                            return@launch
                        }
                    }
                    userRequestedPause = false
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

            override fun onPlayerError(error: PlaybackException) {
                viewModelScope.launch(Dispatchers.Main.immediate) {
                    val cause = error.cause
                    if (isNetworkRelatedError(cause)) {
                        Log.d(TAG, "HeartSync_Debug: Network down, switching to Assets path.")
                        suppressIsPlayingFalseForNetworkFallback = true
                        vm.fallbackToEssentialsOnNetworkError()
                    } else {
                        Log.e(TAG, "HeartSync_Debug: ERROR: Asset filename mismatch or missing.", error)
                        if (_playbackSource.value == "Local") {
                            suppressIsPlayingFalseForNetworkFallback = true
                            vm.fallbackToResRaw()
                        }
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
                    if (mediaItem == null) {
                        _playingMode.value = null
                    } else {
                        inferPlayingModeFromMediaId(mediaItem.mediaId)
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
    }

    private fun isNetworkRelatedError(throwable: Throwable?): Boolean {
        var t = throwable
        while (t != null) {
            if (t is UnknownHostException || t is SocketTimeoutException ||
                t is java.net.ConnectException) {
                return true
            }
            t = t.cause
        }
        return false
    }

    /** Called from Player.Listener when network fails. */
    internal fun fallbackToEssentialsOnNetworkError() {
        player.stop()
        player.clearMediaItems()
        stopProgressUpdates()
        doPlayEssentials(useResRaw = false)
    }

    /** Called from Player.Listener when asset load fails. Uses res/raw fallback. */
    internal fun fallbackToResRaw() {
        doPlayEssentials(useResRaw = true)
    }

    private fun doPlayEssentials(useResRaw: Boolean) {
        val mode = TerminalModeHolder.getCurrentMode()
        _playingMode.value = mode
        _isPanelExpanded.value = true
        _playbackSource.value = "Local"
        val pkg = getApplication<android.app.Application>().packageName
        val (uri, title, artist) = when (mode) {
            TerminalMode.ZEN -> Triple(
                if (useResRaw) Uri.parse("android.resource://$pkg/${R.raw.essential_zen}")
                else Uri.parse("file:///android_asset/essentials/zen.mp3"),
                "Eternal Peace (Offline)", "Calm Master"
            )
            TerminalMode.SYNC -> Triple(
                if (useResRaw) Uri.parse("android.resource://$pkg/${R.raw.essential_sync}")
                else Uri.parse("file:///android_asset/essentials/sync.mp3"),
                "Digital Pulse (Offline)", "Sync Theory"
            )
            TerminalMode.OVERDRIVE -> Triple(
                if (useResRaw) Uri.parse("android.resource://$pkg/${R.raw.essential_overdrive}")
                else Uri.parse("file:///android_asset/essentials/overdrive.mp3"),
                "System Overload (Offline)", "Kinetic Power"
            )
        }
        Log.d(TAG, "HeartSync_Debug: ExoPlayer loading: $uri")
        val songId = "essential_${mode.name}"
        val mediaItem = MediaItem.Builder()
            .setMediaId(songId)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .build()
            )
            .build()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        _currentSongId.value = songId
        _displayTitle.value = title
        _displayArtist.value = artist
        _displayFirstTag.value = ""
        _displayCoverColor.value = null
        _isMusicPlaying.value = true
        suppressIsPlayingFalseForNetworkFallback = false
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getApplication<android.app.Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun inferPlayingModeFromMediaId(mediaId: String?) {
        if (mediaId?.startsWith("essential_") == true) {
            val modeName = mediaId.removePrefix("essential_")
            _playingMode.value = when (modeName) {
                "ZEN" -> TerminalMode.ZEN
                "SYNC" -> TerminalMode.SYNC
                "OVERDRIVE" -> TerminalMode.OVERDRIVE
                else -> null
            }
        }
        // API songs: _playingMode set explicitly in playFromApi when we start playback
    }

    private fun initPlaybackState() {
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
                inferPlayingModeFromMediaId(mediaItem.mediaId)
                _playbackSource.value = if (mediaItem.mediaId?.startsWith("essential_") == true) "Local" else "API"
            } else {
                _playbackSource.value = "Idle"
            }
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
                inferPlayingModeFromMediaId(mediaItem.mediaId)
                _playbackSource.value = if (mediaItem.mediaId?.startsWith("essential_") == true) "Local" else "API"
                _displayTitle.value = md?.title?.toString() ?: ""
                _displayArtist.value = md?.artist?.toString() ?: ""
                _displayFirstTag.value = ""
                _displayCoverColor.value = null
            } else {
                _playingMode.value = null
                _playbackSource.value = "Idle"
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

    /** Play or pause. Option A: switching mode keeps playing until user presses Play in new mode. */
    fun playPause() {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            val currentMode = TerminalModeHolder.getCurrentMode()
            val playingModeNow = _playingMode.value
            val isThisModePlaying = player.isPlaying && playingModeNow == currentMode

            if (isThisModePlaying) {
                Log.d(TAG, "HeartSync_Audio: Pausing mode=$currentMode")
                userRequestedPause = true
                player.pause()
            } else {
                if (player.currentMediaItem == null) {
                    loadAndPlayFirstSong()
                } else {
                    Log.d(TAG, "HeartSync_Audio: Switching from $playingModeNow to $currentMode, stopping previous tracks...")
                    stopCurrentPlayback()
                    loadAndPlayFirstSong()
                }
            }
        }
    }

    private fun stopCurrentPlayback() {
        player.stop()
        player.clearMediaItems()
        stopProgressUpdates()
        _isMusicPlaying.value = false
        _playingMode.value = null
    }

    private fun loadAndPlayFirstSong() {
        val vm = this
        viewModelScope.launch(Dispatchers.Main.immediate) {
            val mode = TerminalModeHolder.getCurrentMode()
            Log.d(TAG, "HeartSync_Audio: Playing: $mode, stopping previous tracks...")
            player.stop()
            player.clearMediaItems()
            stopProgressUpdates()

            val activityMode = mode.toActivityMode()
            _currentMode.value = activityMode
            if (heartRateProvider is MockHeartRateProvider) {
                heartRateProvider.setMode(activityMode)
            }
            MOCK_SONGS[mode]?.let { mock ->
                _displayTitle.value = mock.title
                _displayArtist.value = mock.artist
                _displayFirstTag.value = mock.firstTag
                _displayCoverColor.value = mock.coverColor
            }
            Log.i(TAG, "Mode Changed: $mode, Fetching Tags: ${mode.musicTags}, Target BPM: ${_currentHeartRate.value}")
            _isPanelExpanded.value = true

            if (isNetworkAvailable()) {
                Log.d(TAG, "HeartSync_Debug: Attempting API fetch...")
                libraryRepository.getAllSongs(
                    object : LibraryRepository.SongsCallback {
                        override fun onSuccess(songs: List<Song>?) {
                            viewModelScope.launch(Dispatchers.Main.immediate) {
                                vm.handleSongsLoaded(songs)
                            }
                        }
                        override fun onError(e: Exception?) {
                            Log.d(TAG, "HeartSync_Debug: Network down, switching to Assets path.")
                            viewModelScope.launch(Dispatchers.Main.immediate) {
                                vm.playLocalAsset()
                            }
                        }
                    }
                )
            } else {
                Log.d(TAG, "HeartSync_Debug: Network down, switching to Assets path.")
                vm.playLocalAsset()
            }
        }
    }

    internal fun handleSongsLoaded(songs: List<Song>?) {
        val list = songs ?: emptyList()
        val song = list.firstOrNull()
        val url = song?.audioUrl?.takeIf { it.isNotEmpty() }
        if (url != null && song != null) {
            playFromApi(song, url)
        } else {
            playFromEssentials()
        }
    }

    /** Play local asset from assets/essentials (e.g. zen.mp3, sync.mp3, overdrive.mp3). */
    internal fun playLocalAsset() {
        Log.d(TAG, "HeartSync_Debug: Offline Mode Active")
        val mode = TerminalModeHolder.getCurrentMode()
        val assetPath = when (mode) {
            TerminalMode.ZEN -> "essentials/zen.mp3"
            TerminalMode.SYNC -> "essentials/sync.mp3"
            TerminalMode.OVERDRIVE -> "essentials/overdrive.mp3"
        }
        try {
            getApplication<Application>().assets.open(assetPath).use { }
            Log.d(TAG, "HeartSync_Debug: Asset file found and opened successfully: $assetPath")
        } catch (e: Exception) {
            Log.e(TAG, "HeartSync_Debug: FATAL: Cannot find asset file: ${e.message}")
        }
        playFromEssentials()
    }

    internal fun playFromApi(song: Song, url: String) {
        val mode = TerminalModeHolder.getCurrentMode()
        _playingMode.value = mode
        _playbackSource.value = "API"

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

    /** Play from assets/essentials or res/raw. useResRaw=true for fallback when assets fail. */
    internal fun playFromEssentials(useResRaw: Boolean = false) {
        doPlayEssentials(useResRaw)
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

            Log.d(TAG, "HeartSync_Audio: Stopping playback, clearing tracks...")
            player.stop()
            player.clearMediaItems()
            _playingMode.value = null
            _playbackSource.value = "Idle"
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
        // Capture mode on Main thread at click time (not hardcoded) - ensures ZEN/SYNC/OVERDRIVE all work
        val currentMode = TerminalModeHolder.getCurrentMode().name.ifEmpty { "SYNC" }
        Log.d(TAG, "HeartSync_Debug: toggleCollection mode=$currentMode songId=${_currentSongId.value}")

        viewModelScope.launch(Dispatchers.IO) {
            val songId = _currentSongId.value
            if (songId.isEmpty()) {
                Log.w(TAG, "HeartSync_Debug: songId empty, aborting")
                return@launch
            }
            val inCollection = _collection.value.any { it.songId == songId && it.mode == currentMode }
            val title = _currentTrackTitle.value.ifEmpty { _displayTitle.value }.ifEmpty { "Unknown" }
            val artist = _currentTrackArtist.value.ifEmpty { _displayArtist.value }
            val coverUrl = _currentCoverUrl.value ?: ""
            val mode = currentMode

            if (inCollection) {
                runCatching { collectionRepository.removeFromCollection(songId, mode) }
                    .onFailure { Log.e(TAG, "HeartSync_Debug: Failed to remove from collection", it) }
            } else {
                Log.d(TAG, "HeartSync_Debug: Saving song with mode: $mode (songId=$songId title=$title)")
                runCatching { collectionRepository.addToCollection(songId, title, artist, mode, coverUrl) }
                    .onSuccess { Log.d(TAG, "HeartSync_Debug: Added to collection successfully") }
                    .onFailure { Log.e(TAG, "HeartSync_Debug: Failed to add to collection (check login)", it) }
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
