package com.heartbeatmusic.data.remote

import com.heartbeatmusic.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Mock Jamendo API service for music discovery.
 * Uses mock URLs instead of real API requests.
 * Replace with actual Jamendo API when ready:
 * https://api.jamendo.com/v3.0/tracks/?client_id=YOUR_CLIENT_ID&format=json&limit=200
 */
object JamendoApiService {

    private const val MOCK_NETWORK_DELAY_MS = 300L

    /**
     * Mock tracks with various BPM ranges for discovery.
     * Real Jamendo API returns: id, name, duration, artist_name, album_image, audio, etc.
     * Note: Jamendo does not expose BPM directly; we simulate it for discovery.
     */
    private val mockTracks: List<Song> = listOf(
        createMockSong("calm-1", "Morning Dew", "Ambient Dreams", 55, "https://example.com/mock/calm1.mp3", "https://example.com/cover1.jpg"),
        createMockSong("calm-2", "Soft Rain", "Nature Sounds", 62, "https://example.com/mock/calm2.mp3", "https://example.com/cover2.jpg"),
        createMockSong("calm-3", "Gentle Waves", "Ocean Breeze", 68, "https://example.com/mock/calm3.mp3", "https://example.com/cover3.jpg"),
        createMockSong("calm-4", "Zen Garden", "Meditation Collective", 72, "https://example.com/mock/calm4.mp3", "https://example.com/cover4.jpg"),
        createMockSong("driving-1", "Highway Cruiser", "Road Trip Band", 85, "https://example.com/mock/drive1.mp3", "https://example.com/cover5.jpg"),
        createMockSong("driving-2", "Open Road", "Wanderlust", 95, "https://example.com/mock/drive2.mp3", "https://example.com/cover6.jpg"),
        createMockSong("driving-3", "City Lights", "Urban Beats", 105, "https://example.com/mock/drive3.mp3", "https://example.com/cover7.jpg"),
        createMockSong("driving-4", "Night Drive", "Synthwave", 115, "https://example.com/mock/drive4.mp3", "https://example.com/cover8.jpg"),
        createMockSong("exercise-1", "Pump It Up", "Fitness Crew", 125, "https://example.com/mock/ex1.mp3", "https://example.com/cover9.jpg"),
        createMockSong("exercise-2", "Run Faster", "Cardio Kings", 135, "https://example.com/mock/ex2.mp3", "https://example.com/cover10.jpg"),
        createMockSong("exercise-3", "Beast Mode", "Workout Warriors", 145, "https://example.com/mock/ex3.mp3", "https://example.com/cover11.jpg"),
        createMockSong("exercise-4", "Peak Performance", "Energy Squad", 155, "https://example.com/mock/ex4.mp3", "https://example.com/cover12.jpg"),
    )

    private fun createMockSong(
        id: String,
        title: String,
        artist: String,
        bpm: Int,
        audioUrl: String,
        coverUrl: String
    ): Song {
        val song = Song()
        song.id = id
        song.title = title
        song.artist = artist
        song.bpm = bpm.toLong()
        song.durationSec = 180 + (bpm % 60)
        song.genre = "electronic"
        song.tags = listOf("discovery")
        song.audioSourceType = "jamendo"
        song.localFileId = ""
        song.audioUrl = audioUrl
        song.coverUrl = coverUrl
        return song
    }

    /**
     * Fetches tracks from Jamendo (mock). In production, replace with:
     * val url = "https://api.jamendo.com/v3.0/tracks/?client_id=$clientId&format=json&limit=200"
     * then parse JSON and map to Song.
     */
    suspend fun fetchTracks(): Result<List<Song>> = withContext(Dispatchers.IO) {
        delay(MOCK_NETWORK_DELAY_MS)
        Result.success(mockTracks)
    }

    /**
     * Find tracks whose BPM is within tolerance of target.
     * @param targetBpm Target BPM
     * @param tolerance Allowed deviation (default 15)
     */
    suspend fun findTracksByBpm(targetBpm: Int, tolerance: Int = 15): Result<List<Song>> =
        withContext(Dispatchers.IO) {
            delay(MOCK_NETWORK_DELAY_MS)
            val min = (targetBpm - tolerance).coerceAtLeast(0)
            val max = targetBpm + tolerance
            val matches = mockTracks.filter { song ->
                val bpm = song.bpm?.toInt() ?: 0
                bpm in min..max
            }
            Result.success(if (matches.isEmpty()) mockTracks else matches)
        }
}
