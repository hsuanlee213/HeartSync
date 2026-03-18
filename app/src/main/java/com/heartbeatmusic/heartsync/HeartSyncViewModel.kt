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
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.google.firebase.auth.FirebaseAuth
import com.heartbeatmusic.PlayerHolder
import com.heartbeatmusic.data.model.Achievement
import com.heartbeatmusic.data.model.CollectionItem
import com.heartbeatmusic.data.model.DailyGoal
import com.heartbeatmusic.data.model.Song
import com.heartbeatmusic.data.model.SyncSession
import com.heartbeatmusic.data.local.AchievementRepository
import com.heartbeatmusic.data.local.CollectionRepository
import com.heartbeatmusic.data.local.DailyGoalRepository
import com.heartbeatmusic.data.local.EssentialAudioRepository
import com.heartbeatmusic.data.local.SessionRepository
import com.heartbeatmusic.data.remote.ArchiveRepository
import com.heartbeatmusic.data.remote.JamendoRepository
import com.heartbeatmusic.terminal.TerminalMode
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val TAG = "HeartSync"

/**
 * Represents the intent behind an imminent playback state change, so that
 * [HeartSyncViewModel] can distinguish between user-driven pauses and
 * transient interruptions caused by network fallback switches.
 */
sealed class PlaybackTransition {
    /** User explicitly pressed pause. Allow UI to reflect the paused state. */
    object UserPause : PlaybackTransition()
    /** Player is momentarily stopping while switching to a fallback audio source.
     *  Suppress the isPlaying=false event so the UI does not flicker. */
    object NetworkFallback : PlaybackTransition()
    /** Player is stopping because the user switched terminal mode.
     *  Suppress the isPlaying=false event so heart rate and animation continue uninterrupted. */
    object ModeSwitch : PlaybackTransition()
    /** No pending transition — normal playback flow. */
    object Idle : PlaybackTransition()
}


/**
 * HeartSync ViewModel.
 * Exposes heart rate and playback state for UI observation.
 */
@HiltViewModel
class HeartSyncViewModel @Inject constructor(
    application: Application,
    private val jamendoRepository: JamendoRepository,
    private val libraryRepository: LibraryRepository,
    private val archiveRepository: ArchiveRepository,
    private val essentialAudioRepository: EssentialAudioRepository,
    private val collectionRepository: CollectionRepository,
    private val sessionRepository: SessionRepository,
    private val dailyGoalRepository: DailyGoalRepository,
    private val achievementRepository: AchievementRepository,
    private val firebaseAuth: FirebaseAuth
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE)

    private val _currentMode = MutableStateFlow(TerminalMode.SYNC)
    val currentMode: StateFlow<TerminalMode> = _currentMode.asStateFlow()

    private val heartRateProvider = MockHeartRateProvider(viewModelScope, _currentMode.value)

    private val _currentHeartRate = MutableStateFlow(0)
    val currentHeartRate: StateFlow<Int> = _currentHeartRate.asStateFlow()

    private val _isMusicPlaying = MutableStateFlow(false)
    val isMusicPlaying: StateFlow<Boolean> = _isMusicPlaying.asStateFlow()

    /** "API" | "Local" | "Idle" - for debug UI when BuildConfig.DEBUG */
    private val _playbackSource = MutableStateFlow("Idle")
    val playbackSource: StateFlow<String> = _playbackSource.asStateFlow()

    private var pendingTransition: PlaybackTransition = PlaybackTransition.Idle

    /** Mode that owns the current playback. Null when nothing playing. */
    private val _playingMode = MutableStateFlow<TerminalMode?>(null)
    val playingMode: StateFlow<TerminalMode?> = _playingMode.asStateFlow()

    /** True only when current mode is the one playing. Other modes show Play. */
    val isPlayingInCurrentMode: StateFlow<Boolean> = combine(
        _isMusicPlaying,
        _playingMode,
        _currentMode
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
        _currentMode
    ) { songId, items, mode ->
        songId.isNotEmpty() && items.any { it.songId == songId && it.mode == mode.name }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val player = PlayerHolder.getInstance(application).player
    private var progressJob: Job? = null

    private data class SongEntry(val id: String, val title: String, val artist: String, val coverUrl: String)

    private var sessionStartTime: Long? = null
    private var sessionMode: TerminalMode? = null
    private val sessionSongs = mutableListOf<SongEntry>()

    // Dedicated scope for session saves — outlives viewModelScope so onCleared() saves complete
    private val sessionSaveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        collectHeartRate()
        observePlaybackState()
        initPlaybackState()
        collectionRepository.collectionFlow()
            .onEach { _collection.value = it }
            .launchIn(viewModelScope)
        viewModelScope.launch(Dispatchers.IO) {
            recordAchievementForLastMonth(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
        }
    }

    private fun observePlaybackState() {
        val vm = this
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                viewModelScope.launch(Dispatchers.Main.immediate) {
                    if (!isPlaying) {
                        if (pendingTransition is PlaybackTransition.NetworkFallback) return@launch
                        if (pendingTransition is PlaybackTransition.ModeSwitch) return@launch
                        if (_playbackSource.value == "Local" && pendingTransition !is PlaybackTransition.UserPause) {
                            Log.d(TAG, "HeartSync_Debug: Ignoring isPlaying=false (Local source, not user pause)")
                            return@launch
                        }
                    }
                    pendingTransition = PlaybackTransition.Idle
                    _isMusicPlaying.value = isPlaying
                    if (isPlaying) {
                        heartRateProvider.startMonitoring()
                    } else {
                        heartRateProvider.stopMonitoring()
                        _currentHeartRate.value = 0
                    }
                    if (isPlaying) {
                        if (sessionStartTime == null) {
                            sessionStartTime = System.currentTimeMillis()
                            sessionMode = _currentMode.value
                            val mediaItem = player.currentMediaItem
                            if (mediaItem != null) {
                                val songId = mediaItem.mediaId.takeIf { it?.isNotEmpty() == true } ?: ""
                                val title = mediaItem.mediaMetadata.title?.toString() ?: ""
                                val artist = mediaItem.mediaMetadata.artist?.toString() ?: ""
                                val coverUrl = mediaItem.mediaMetadata.artworkUri?.toString() ?: ""
                                if (title.isNotEmpty() && sessionSongs.none { it.id == songId }) {
                                    sessionSongs.add(SongEntry(songId, title, artist, coverUrl))
                                    autoSaveSession()
                                }
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
                        pendingTransition = PlaybackTransition.NetworkFallback
                        vm.fallbackToEssentialsOnNetworkError()
                    } else {
                        Log.e(TAG, "HeartSync_Debug: ERROR: Asset filename mismatch or missing.", error)
                        if (_playbackSource.value == "Local") {
                            pendingTransition = PlaybackTransition.NetworkFallback
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
                        if (sessionStartTime != null) {
                            val songId = mediaItem.mediaId.takeIf { it?.isNotEmpty() == true } ?: ""
                            val title = md?.title?.toString() ?: ""
                            val artist = md?.artist?.toString() ?: ""
                            val coverUrl = md?.artworkUri?.toString() ?: ""
                            if (title.isNotEmpty() && sessionSongs.none { it.id == songId }) {
                                sessionSongs.add(SongEntry(songId, title, artist, coverUrl))
                                autoSaveSession()
                            }
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
        doPlayEssentials()
    }

    /** Called from Player.Listener when asset load fails. Retries from local cache. */
    internal fun fallbackToResRaw() {
        doPlayEssentials()
    }

    private fun doPlayEssentials() {
        val mode = _currentMode.value
        _playingMode.value = mode
        _isPanelExpanded.value = true
        _playbackSource.value = "Local"
        val (title, artist) = when (mode) {
            TerminalMode.ZEN -> "Eternal Peace (Offline)" to "Calm Master"
            TerminalMode.SYNC -> "Digital Pulse (Offline)" to "Sync Theory"
            TerminalMode.OVERDRIVE -> "System Overload (Offline)" to "Kinetic Power"
        }
        viewModelScope.launch {
            val uri = essentialAudioRepository.getUriForMode(mode)
            if (uri == null) {
                Log.w(TAG, "HeartSync_Debug: No local cache for ${mode.name}. Connect to network to download from Firebase Storage.")
                launch(Dispatchers.Main.immediate) {
                    _displayTitle.value = title
                    _displayArtist.value = artist
                    _displayFirstTag.value = ""
                    _isPanelExpanded.value = true
                }
                return@launch
            }
            Log.d(TAG, "HeartSync_Debug: ExoPlayer loading from cache: $uri")
            launch(Dispatchers.Main.immediate) {
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
                pendingTransition = PlaybackTransition.Idle
            }
        }
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
            syncStateFromPlayer()
        }
    }

    /** Reads current player state into all relevant StateFlows. Must be called on Main thread. */
    private fun syncStateFromPlayer(updateDisplay: Boolean = false) {
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
            if (updateDisplay) {
                _displayTitle.value = md?.title?.toString() ?: ""
                _displayArtist.value = md?.artist?.toString() ?: ""
                _displayFirstTag.value = ""
                _displayCoverColor.value = null
            }
        } else {
            _playingMode.value = null
            _playbackSource.value = "Idle"
        }
        if (player.isPlaying) startProgressUpdates()
    }

    /** Saves current session snapshot without ending it. Safe to call repeatedly (upserts by id). */
    fun autoSaveSession() {
        val start = sessionStartTime ?: return
        val mode = sessionMode ?: return
        val uid = firebaseAuth.currentUser?.uid ?: prefs.getString("user_id", null) ?: return
        var songs = sessionSongs.toList()
        if (songs.isEmpty()) {
            val mediaItem = player.currentMediaItem
            if (mediaItem != null) {
                val songId = mediaItem.mediaId.takeIf { it?.isNotEmpty() == true } ?: ""
                val title = mediaItem.mediaMetadata.title?.toString() ?: _displayTitle.value.ifEmpty { _currentTrackTitle.value }
                val artist = mediaItem.mediaMetadata.artist?.toString() ?: _displayArtist.value.ifEmpty { _currentTrackArtist.value }
                val coverUrl = mediaItem.mediaMetadata.artworkUri?.toString() ?: ""
                if (title.isNotEmpty()) {
                    val entry = SongEntry(songId, title, artist, coverUrl)
                    if (sessionSongs.none { it.id == songId }) sessionSongs.add(entry)
                    songs = listOf(entry)
                }
            }
        }
        if (songs.isEmpty()) return
        val endTime = System.currentTimeMillis()
        val durationMinutes = ((endTime - start) / 60_000).toInt().coerceAtLeast(0)
        val session = SyncSession(
            id = "session_${start}",
            userId = uid,
            mode = mode.name,
            startTimestamp = start,
            endTimestamp = endTime,
            durationMinutes = durationMinutes,
            songIds = songs.map { it.id },
            songTitles = songs.map { it.title },
            songArtists = songs.map { it.artist },
            songCoverUrls = songs.map { it.coverUrl }
        )
        sessionSaveScope.launch { sessionRepository.saveSession(session) }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            var autoSaveTick = 0
            var lastGoalTickMs = System.currentTimeMillis()
            while (isActive) {
                val dur = player.duration
                val pos = player.currentPosition
                _playbackProgress.value = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
                autoSaveTick++
                if (autoSaveTick >= 100) { // 100 × 300ms = 30s
                    autoSaveSession()
                    autoSaveTick = 0
                }
                val now = System.currentTimeMillis()
                if (now - lastGoalTickMs >= 1000) {
                    lastGoalTickMs = now
                    accumulateGoalProgress()
                }
                delay(300)
            }
        }
    }

    /**
     * When music is playing, accumulate time toward today's goal for the current playing mode.
     * When accumulatedSeconds >= targetMinutes * 60, mark goal completed and trigger monthly achievement check.
     */
    private suspend fun accumulateGoalProgress() {
        val userId = firebaseAuth.currentUser?.uid ?: return
        val playingMode = _playingMode.value ?: return
        if (!player.isPlaying) return

        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val goals = dailyGoalRepository.getGoalsByDate(userId, today)
        val matchingGoal = goals.find { it.mode == playingMode.name && !it.isCompleted } ?: return

        val newAccumulated = matchingGoal.accumulatedSeconds + 1
        val targetSeconds = matchingGoal.targetMinutes * 60
        val isNowCompleted = newAccumulated >= targetSeconds

        val updatedGoal = matchingGoal.copy(
            accumulatedSeconds = newAccumulated.coerceAtMost(targetSeconds),
            isCompleted = isNowCompleted
        )
        dailyGoalRepository.updateGoal(updatedGoal)

        if (isNowCompleted) {
            Log.d(TAG, "HeartSync_Goals: Goal completed: ${matchingGoal.mode} ${matchingGoal.targetMinutes}min")
            recordAchievementForMonth(today.substring(0, 7)) // current month "2026-03"
            recordAchievementForLastMonth(today)
        }
    }

    /** Upsert achievement record for a given month (yearMonth = "2026-03"). */
    private suspend fun recordAchievementForMonth(yearMonth: String) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        val goals = dailyGoalRepository.getGoalsByMonth(userId, yearMonth)
        if (goals.isEmpty()) return
        val completed = goals.count { it.isCompleted }
        val parts = yearMonth.split("-")
        if (parts.size != 2) return
        val year = parts[0].toIntOrNull() ?: return
        val month = parts[1].toIntOrNull() ?: return
        achievementRepository.insertAchievement(
            Achievement(
                id = "${userId}_$yearMonth",
                userId = userId,
                year = year,
                month = month,
                completedCount = completed,
                totalCount = goals.size
            )
        )
    }

    /** Ensure last month's achievement is recorded (Step 6 trigger on goal completion). */
    private suspend fun recordAchievementForLastMonth(today: String) {
        val lastMonth = LocalDate.parse(today).minusMonths(1)
        val yearMonth = "${lastMonth.year}-${lastMonth.monthValue.toString().padStart(2, '0')}"
        recordAchievementForMonth(yearMonth)
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    /** Sync playback state from player (e.g. when UI becomes visible). */
    fun syncPlaybackState() {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            syncStateFromPlayer(updateDisplay = true)
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
            val currentMode = _currentMode.value
            val playingModeNow = _playingMode.value
            val isThisModePlaying = player.isPlaying && playingModeNow == currentMode

            if (isThisModePlaying) {
                Log.d(TAG, "HeartSync_Audio: Pausing mode=$currentMode")
                pendingTransition = PlaybackTransition.UserPause
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
            val mode = _currentMode.value
            Log.d(TAG, "HeartSync_Audio: Playing: $mode, stopping previous tracks...")
            pendingTransition = PlaybackTransition.ModeSwitch
            player.stop()
            player.clearMediaItems()
            stopProgressUpdates()

            _currentMode.value = mode
            if (heartRateProvider is MockHeartRateProvider) {
                heartRateProvider.setMode(mode)
            }
            Log.i(TAG, "Mode Changed: $mode, Fetching Tags: ${mode.musicTags}, Target BPM: ${_currentHeartRate.value}")
            _isPanelExpanded.value = true

            if (isNetworkAvailable()) {
                Log.d(TAG, "HeartSync_Debug: Attempting Jamendo API fetch for mode=$mode...")
                runCatching { jamendoRepository.fetchTracksForMode(mode) }
                    .onSuccess { handleSongsLoaded(it) }
                    .onFailure {
                        Log.d(TAG, "HeartSync_Debug: Jamendo fetch failed, switching to Assets path.")
                        playLocalAsset()
                    }
            } else {
                Log.d(TAG, "HeartSync_Debug: Network down, switching to Assets path.")
                vm.playLocalAsset()
            }
        }
    }

    internal fun handleSongsLoaded(songs: List<Song>?) {
        val list = (songs ?: emptyList()).filter { !it.audioUrl.isNullOrEmpty() }.shuffled()
        if (list.isEmpty()) {
            playFromEssentials()
            return
        }
        playFromApi(list)
    }

    /** Play essential audio from local cache (Firebase Storage). */
    internal fun playLocalAsset() {
        Log.d(TAG, "HeartSync_Debug: Offline Mode Active - using cache or bundled fallback")
        playFromEssentials()
    }

    internal fun playFromApi(songs: List<Song>) {
        val mode = _currentMode.value
        _playingMode.value = mode
        _playbackSource.value = "API"

        val mediaItems = songs.map { song ->
            val title = song.title?.takeIf { it.isNotEmpty() } ?: "Unknown"
            val artist = song.artist?.takeIf { it.isNotEmpty() } ?: "Unknown"
            val songId = song.id?.takeIf { it.isNotEmpty() } ?: ""
            MediaItem.Builder()
                .setMediaId(songId)
                .setUri(Uri.parse(song.audioUrl!!))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setArtist(artist)
                        .setArtworkUri(song.coverUrl?.takeIf { it.isNotEmpty() }?.let { Uri.parse(it) })
                        .build()
                )
                .build()
        }
        player.shuffleModeEnabled = true
        player.setMediaItems(mediaItems)
        player.prepare()
        player.play()

        val first = songs.first()
        val firstTitle = first.title?.takeIf { it.isNotEmpty() } ?: "Unknown"
        val firstArtist = first.artist?.takeIf { it.isNotEmpty() } ?: "Unknown"
        _isPanelExpanded.value = true
        _currentSongId.value = first.id?.takeIf { it.isNotEmpty() } ?: ""
        _displayTitle.value = firstTitle
        _displayArtist.value = firstArtist
        _displayFirstTag.value = ""
        _displayCoverColor.value = null
    }

    /** Play from Firebase Storage local cache. */
    internal fun playFromEssentials() {
        doPlayEssentials()
    }

    /** Stop playback and clear media. Collapses the panel. Saves session if one was active. */
    fun stop() {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            autoSaveSession()
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

            autoSaveSession()
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
     * Switch terminal mode, affects BPM range emitted by Mock provider.
     */
    fun setMode(mode: TerminalMode) {
        _currentMode.value = mode
        if (heartRateProvider is MockHeartRateProvider) {
            heartRateProvider.setMode(mode)
        }
    }

    /** Toggle current track in Collection: add if not in, remove if in. Syncs with Archive tab. */
    fun toggleCollection() {
        // Capture mode on Main thread at click time (not hardcoded) - ensures ZEN/SYNC/OVERDRIVE all work
        val currentMode = _currentMode.value.name
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
     * Switch Terminal mode.
     * If music is active, immediately fetches and loads tracks for the new mode.
     */
    fun setTerminalMode(mode: TerminalMode) {
        setMode(mode)
        val tags = mode.musicTags
        val bpm = _currentHeartRate.value
        Log.i(TAG, "Mode Changed: $mode, Fetching Tags: $tags, Target BPM: $bpm")
        if (player.currentMediaItem != null) {
            loadAndPlayFirstSong()
        }
    }

    override fun onCleared() {
        autoSaveSession()
        heartRateProvider.stopMonitoring()
        super.onCleared()
    }
}
