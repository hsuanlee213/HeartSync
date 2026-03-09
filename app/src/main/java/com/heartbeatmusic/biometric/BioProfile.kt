package com.heartbeatmusic.biometric

/**
 * Physiological profile derived from registration data.
 * Used by the music intensity engine.
 */
data class BioProfile(
    val maxHeartRate: Int,
    val restingBPM: Int,
    val energyLevel: Int  // 1-5: Zen, Chill, Balanced, High Energy, Insane
) {
    companion object {
        const val DEFAULT_AGE = 25
        const val DEFAULT_MAX_HR = 195  // 220 - 25
        const val DEFAULT_RESTING_BPM = 70
        const val DEFAULT_ENERGY_LEVEL = 3  // Balanced
        const val SAFE_DENOMINATOR = 110  // Used when maxHR <= restingBPM
    }

    /** Energy multiplier: Level 1-5 -> [0.6, 0.8, 1.0, 1.2, 1.4] */
    val energyMultiplier: Float
        get() = when (energyLevel) {
            1 -> 0.6f
            2 -> 0.8f
            3 -> 1.0f
            4 -> 1.2f
            5 -> 1.4f
            else -> 1.0f
        }
}
