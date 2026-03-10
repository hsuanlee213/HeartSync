package com.heartbeatmusic.biometric

/**
 * Maps raw registration fields to BioProfile parameters.
 * All fields are optional with stable defaults.
 */
object RegistrationMapper {

    /**
     * Age -> maxHeartRate (220 - age). Default 25 if empty.
     */
    fun ageToMaxHeartRate(ageStr: String?): Int {
        val age = ageStr?.toIntOrNull() ?: BioProfile.DEFAULT_AGE
        return (220 - age).coerceIn(60, 220)
    }

    /**
     * Weight -> restingBPM. Default 70 if empty. Range: 40-100 BPM.
     */
    fun weightToRestingBPM(weightStr: String?): Int {
        val value = weightStr?.toIntOrNull() ?: BioProfile.DEFAULT_RESTING_BPM
        return value.coerceIn(40, 100)
    }

    /**
     * Energy level string (e.g. "3 - Balanced") -> 1-5. Default 3 if empty.
     */
    fun parseEnergyLevel(energyStr: String?): Int {
        val level = energyStr?.trim()?.take(1)?.toIntOrNull() ?: BioProfile.DEFAULT_ENERGY_LEVEL
        return level.coerceIn(1, 5)
    }

    /**
     * Build BioProfile from raw registration inputs.
     * Uses toIntOrNull() ?: defaultValue for safe parsing.
     */
    fun buildBioProfile(
        ageStr: String?,
        weightStr: String?,
        energyLevelStr: String?
    ): BioProfile = BioProfile(
        maxHeartRate = ageToMaxHeartRate(ageStr),
        restingBPM = weightToRestingBPM(weightStr),
        energyLevel = parseEnergyLevel(energyLevelStr)
    )
}
