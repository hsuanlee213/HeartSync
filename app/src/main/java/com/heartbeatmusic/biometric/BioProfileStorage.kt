package com.heartbeatmusic.biometric

import android.content.SharedPreferences

private const val KEY_MAX_HR = "bio_max_heart_rate"
private const val KEY_RESTING_BPM = "bio_resting_bpm"
private const val KEY_ENERGY_BIAS = "bio_energy_bias"
private const val KEY_ALGORITHM_SENSITIVITY = "bio_algorithm_sensitivity"

/**
 * Persists and loads BioProfile from SharedPreferences.
 */
object BioProfileStorage {

    fun save(prefs: SharedPreferences, profile: BioProfile) {
        prefs.edit()
            .putInt(KEY_MAX_HR, profile.maxHeartRate)
            .putInt(KEY_RESTING_BPM, profile.restingBPM)
            .putFloat(KEY_ENERGY_BIAS, profile.energyBias)
            .putFloat(KEY_ALGORITHM_SENSITIVITY, profile.algorithmSensitivity)
            .apply()
    }

    fun load(prefs: SharedPreferences): BioProfile? {
        if (!prefs.contains(KEY_MAX_HR)) return null
        return BioProfile(
            maxHeartRate = prefs.getInt(KEY_MAX_HR, BioProfile.DEFAULT_MAX_HR),
            restingBPM = prefs.getInt(KEY_RESTING_BPM, BioProfile.DEFAULT_RESTING_BPM),
            energyBias = prefs.getFloat(KEY_ENERGY_BIAS, 0.5f),
            algorithmSensitivity = prefs.getFloat(KEY_ALGORITHM_SENSITIVITY, 1f)
        )
    }
}
