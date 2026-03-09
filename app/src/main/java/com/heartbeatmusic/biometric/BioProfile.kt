package com.heartbeatmusic.biometric

/**
 * Physiological profile derived from registration data.
 * Used by the music intensity engine.
 */
data class BioProfile(
    val maxHeartRate: Int,
    val restingBPM: Int,
    val energyBias: Float,
    val algorithmSensitivity: Float
) {
    companion object {
        const val DEFAULT_RESTING_BPM = 70
        const val DEFAULT_MAX_HR = 220
        const val MIN_ENERGY_BIAS = 0f
        const val MAX_ENERGY_BIAS = 1f
    }
}
