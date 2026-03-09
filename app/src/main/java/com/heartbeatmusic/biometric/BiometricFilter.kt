package com.heartbeatmusic.biometric

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton that calculates music intensity from current BPM using BioProfile.
 *
 * Formula:
 * - intensityRatio = (currentBPM - restingBPM) / (maxHeartRate - restingBPM)
 * - energyMultiplier from Level 1-5: [0.6, 0.8, 1.0, 1.2, 1.4]
 * - finalIntensity = intensityRatio * energyMultiplier
 *
 * Safety: If maxHeartRate <= restingBPM, use SAFE_DENOMINATOR (110) to avoid divide-by-zero.
 */
object BiometricFilter {

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
     * Calculate music intensity from current BPM.
     * intensityRatio = (currentBPM - restingBPM) / denominator
     * finalIntensity = intensityRatio * energyMultiplier
     * If maxHeartRate <= restingBPM, denominator = SAFE_DENOMINATOR (110).
     */
    fun calculateMusicIntensity(currentBPM: Int): Float {
        val profile = _profile ?: return 0.5f
        val denominator = (profile.maxHeartRate - profile.restingBPM).takeIf { it > 0 }
            ?: BioProfile.SAFE_DENOMINATOR
        val intensityRatio = ((currentBPM - profile.restingBPM).toFloat() / denominator).coerceIn(0f, 1f)
        val finalIntensity = (intensityRatio * profile.energyMultiplier).coerceIn(0f, 1f)
        _intensityFlow.value = finalIntensity
        return finalIntensity
    }

    /**
     * Reset internal state (e.g. when profile changes).
     */
    fun reset() {
        // No persistent state to reset in new formula
    }
}
