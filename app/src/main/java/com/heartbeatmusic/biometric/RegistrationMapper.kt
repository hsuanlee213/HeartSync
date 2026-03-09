package com.heartbeatmusic.biometric

/**
 * Maps raw registration fields to BioProfile parameters.
 */
object RegistrationMapper {

    /**
     * Maps fitness level string to algorithmSensitivity.
     * Beginner=0.5, Intermediate=1.0, Advanced=1.5
     */
    fun mapFitnessLevelToSensitivity(level: String): Float = when (level) {
        "Beginner" -> 0.5f
        "Intermediate" -> 1.0f
        "Advanced" -> 1.5f
        else -> 1.0f
    }

    /**
     * Age -> maxHeartRate (220 - age). Clamped to valid range.
     */
    fun ageToMaxHeartRate(ageStr: String?): Int {
        val age = ageStr?.toIntOrNull() ?: return BioProfile.DEFAULT_MAX_HR
        return (220 - age).coerceIn(60, 220)
    }

    /**
     * Weight -> restingBPM. Default 70 if empty/invalid.
     */
    fun weightToRestingBPM(weightStr: String?): Int {
        val value = weightStr?.toIntOrNull() ?: return BioProfile.DEFAULT_RESTING_BPM
        return value.coerceIn(40, 120)
    }

    /**
     * Height (1-200) -> energyBias (0.0 to 1.0).
     */
    fun heightToEnergyBias(heightStr: String?): Float {
        val value = heightStr?.toFloatOrNull() ?: return 0.5f
        return (value / 200f).coerceIn(BioProfile.MIN_ENERGY_BIAS, BioProfile.MAX_ENERGY_BIAS)
    }

    /**
     * Build BioProfile from raw registration inputs.
     */
    fun buildBioProfile(
        ageStr: String?,
        weightStr: String?,
        heightStr: String?,
        fitnessLevel: String
    ): BioProfile = BioProfile(
        maxHeartRate = ageToMaxHeartRate(ageStr),
        restingBPM = weightToRestingBPM(weightStr),
        energyBias = heightToEnergyBias(heightStr),
        algorithmSensitivity = mapFitnessLevelToSensitivity(fitnessLevel)
    )
}
