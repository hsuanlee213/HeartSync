package com.heartbeatmusic.biometric

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton that calculates music intensity from current BPM using BioProfile.
 * Includes smoothing to prevent rapid music switching.
 *
 * Formula: intensity = (currentBPM - restingBPM) / (maxHeartRate - restingBPM) * energyBias
 * Divide-by-zero safe: when maxHR == restingBPM, returns 0.5f (neutral).
 */
object BiometricFilter {

    private var _smoothedIntensity = 0.5f
    private var _profile: BioProfile? = null

    private val _intensityFlow = MutableStateFlow(0.5f)
    val intensityFlow: StateFlow<Float> = _intensityFlow.asStateFlow()

    /**
     * Update the active BioProfile. Call when user registers or profile changes.
     */
    fun setBioProfile(profile: BioProfile) {
        _profile = profile
    }

    /**
     * Get current BioProfile, or null if not set.
     */
    fun getBioProfile(): BioProfile? = _profile

    /**
     * Calculate raw intensity from current BPM (no smoothing).
     * Returns 0.0 to 1.0. Safe when maxHeartRate == restingBPM.
     */
    fun calculateRawIntensity(currentBPM: Int): Float {
        val profile = _profile ?: return 0.5f
        val denominator = profile.maxHeartRate - profile.restingBPM
        if (denominator <= 0) return 0.5f // Avoid divide-by-zero
        val raw = (currentBPM - profile.restingBPM).toFloat() / denominator * profile.energyBias
        return raw.coerceIn(0f, 1f)
    }

    /**
     * Calculate smoothed music intensity. Uses exponential moving average.
     * Higher algorithmSensitivity = faster response (less smoothing).
     * Alpha = min(algorithmSensitivity / 2, 1.0) for response rate.
     */
    fun calculateMusicIntensity(currentBPM: Int): Float {
        val profile = _profile ?: return 0.5f
        val raw = calculateRawIntensity(currentBPM)
        val alpha = (profile.algorithmSensitivity / 2f).coerceIn(0.1f, 1f)
        _smoothedIntensity = _smoothedIntensity * (1 - alpha) + raw * alpha
        val result = _smoothedIntensity.coerceIn(0f, 1f)
        _intensityFlow.value = result
        return result
    }

    /**
     * Reset internal state (e.g. when profile changes).
     */
    fun reset() {
        _smoothedIntensity = 0.5f
    }
}
