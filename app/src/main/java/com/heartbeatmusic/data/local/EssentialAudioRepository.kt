package com.heartbeatmusic.data.local

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.heartbeatmusic.terminal.TerminalMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "EssentialAudio"
private const val STORAGE_PATH_PREFIX = "essentials/"
private const val LOCAL_DIR = "essentials"

/**
 * Provides essential audio (ZEN, SYNC, OVERDRIVE) from Firebase Storage with local cache.
 * - First checks local storage (app files dir)
 * - If not cached, downloads from Firebase Storage and saves locally
 * - Returns null if no cache and download fails (e.g. offline)
 */
class EssentialAudioRepository(private val context: Context) {

    private val storage = FirebaseStorage.getInstance().reference
    private val localDir: File

    init {
        localDir = File(context.filesDir, LOCAL_DIR).apply { mkdirs() }
    }

    private fun storagePath(mode: TerminalMode): String =
        when (mode) {
            TerminalMode.ZEN -> "${STORAGE_PATH_PREFIX}zen.mp3"
            TerminalMode.SYNC -> "${STORAGE_PATH_PREFIX}sync.mp3"
            TerminalMode.OVERDRIVE -> "${STORAGE_PATH_PREFIX}overdrive.mp3"
        }

    private fun localFile(mode: TerminalMode): File =
        File(localDir, "${mode.name.lowercase()}.mp3")

    /** Returns true if the file is cached locally. */
    fun isCached(mode: TerminalMode): Boolean =
        localFile(mode).exists()

    /**
     * Gets Uri for playback. Prefers local cache; downloads from Firebase if needed.
     * @return Uri to local file, or null if unavailable (offline + not cached)
     */
    suspend fun getUriForMode(mode: TerminalMode): Uri? = withContext(Dispatchers.IO) {
        val local = localFile(mode)
        if (local.exists()) {
            Log.d(TAG, "Using cached: ${local.absolutePath}")
            return@withContext Uri.fromFile(local)
        }

        val storageRef = storage.child(storagePath(mode))
        return@withContext try {
            storageRef.getFile(local).await()
            Log.d(TAG, "Downloaded and cached: ${local.absolutePath}")
            Uri.fromFile(local)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch ${mode.name}: ${e.message}")
            null
        }
    }
}
