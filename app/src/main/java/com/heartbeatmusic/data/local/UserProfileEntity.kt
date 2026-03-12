package com.heartbeatmusic.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local user profile data used by UI as the source of truth.
 *
 * - avatarLocalUri: points to a locally-copied file under app internal storage (file://...).
 * - avatarRemoteUrl: Firebase Storage download URL when upload completes.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val userId: String,
    val avatarLocalUri: String? = null,
    val avatarRemoteUrl: String? = null,
    val updatedAtMs: Long = 0L
)

