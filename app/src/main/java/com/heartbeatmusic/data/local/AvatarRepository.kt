package com.heartbeatmusic.data.local

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "AvatarRepository"

/**
 * Local-first avatar repository.
 *
 * - UI reads avatar state from Room.
 * - When user picks an image, we copy it into app internal storage and update Room immediately.
 * - Upload to Firebase Storage + update Firestore happens on a background thread (best-effort).
 */
class AvatarRepository(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val dao = AppDatabase.getInstance(context).userProfileDao()
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun userProfileFlow(userId: String): Flow<UserProfileEntity?> = dao.getByUserIdFlow(userId)

    suspend fun setAvatarFromPickedUri(pickedUri: Uri): Result<Unit> = runCatching {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Not logged in")

        val localUri = withContext(Dispatchers.IO) {
            copyToInternalStorageAsFileUri(userId, pickedUri)
        }

        dao.upsert(
            UserProfileEntity(
                userId = userId,
                avatarLocalUri = localUri.toString(),
                avatarRemoteUrl = null,
                updatedAtMs = System.currentTimeMillis()
            )
        )

        // Background sync: upload and update Firestore. Local state remains the source of truth.
        syncScope.launch {
            uploadAndUpdateRemote(userId, localUri)
        }
    }

    private fun copyToInternalStorageAsFileUri(userId: String, pickedUri: Uri): Uri {
        val dir = File(context.filesDir, "avatars").apply { mkdirs() }
        val file = File(dir, "${userId}_${System.currentTimeMillis()}.jpg")

        context.contentResolver.openInputStream(pickedUri).use { input ->
            requireNotNull(input) { "Unable to open input stream for $pickedUri" }
            file.outputStream().use { output -> input.copyTo(output) }
        }

        return Uri.fromFile(file)
    }

    private suspend fun uploadAndUpdateRemote(userId: String, localFileUri: Uri) {
        runCatching {
            val ref = storage.reference.child("users/$userId/avatar_${System.currentTimeMillis()}.jpg")
            Tasks.await(ref.putFile(localFileUri))
            val downloadUrl = Tasks.await(ref.downloadUrl).toString()

            // Update local cache with remote URL
            dao.upsert(
                UserProfileEntity(
                    userId = userId,
                    avatarLocalUri = localFileUri.toString(),
                    avatarRemoteUrl = downloadUrl,
                    updatedAtMs = System.currentTimeMillis()
                )
            )

            // Update Firestore user profile document (best-effort)
            firestore.collection("users")
                .document(userId)
                .set(mapOf("avatarUrl" to downloadUrl), com.google.firebase.firestore.SetOptions.merge())
                .let { Tasks.await(it) }
        }.onFailure {
            Log.w(TAG, "Avatar upload sync failed", it)
        }
    }
}

