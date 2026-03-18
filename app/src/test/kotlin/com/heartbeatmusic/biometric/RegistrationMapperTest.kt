package com.heartbeatmusic.biometric

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for RegistrationMapper - maps raw registration fields to BioProfile.
 */
class RegistrationMapperTest {

    @Test
    fun ageToMaxHeartRate_validAge_returns220MinusAge() {
        assertEquals(195, RegistrationMapper.ageToMaxHeartRate("25"))
        assertEquals(200, RegistrationMapper.ageToMaxHeartRate("20"))
        assertEquals(180, RegistrationMapper.ageToMaxHeartRate("40"))
    }

    @Test
    fun ageToMaxHeartRate_nullOrEmpty_usesDefaultAge25() {
        assertEquals(BioProfile.DEFAULT_MAX_HR, RegistrationMapper.ageToMaxHeartRate(null))
        assertEquals(BioProfile.DEFAULT_MAX_HR, RegistrationMapper.ageToMaxHeartRate(""))
    }

    @Test
    fun ageToMaxHeartRate_invalidString_usesDefault() {
        assertEquals(BioProfile.DEFAULT_MAX_HR, RegistrationMapper.ageToMaxHeartRate("abc"))
    }

    @Test
    fun ageToMaxHeartRate_clampsToValidRange() {
        assertEquals(220, RegistrationMapper.ageToMaxHeartRate("0"))
        assertEquals(60, RegistrationMapper.ageToMaxHeartRate("200"))
    }

    @Test
    fun weightToRestingBPM_validValue_returnsValue() {
        assertEquals(70, RegistrationMapper.weightToRestingBPM("70"))
        assertEquals(60, RegistrationMapper.weightToRestingBPM("60"))
    }

    @Test
    fun weightToRestingBPM_nullOrEmpty_usesDefault70() {
        assertEquals(BioProfile.DEFAULT_RESTING_BPM, RegistrationMapper.weightToRestingBPM(null))
        assertEquals(BioProfile.DEFAULT_RESTING_BPM, RegistrationMapper.weightToRestingBPM(""))
    }

    @Test
    fun weightToRestingBPM_clampsTo40_100() {
        assertEquals(40, RegistrationMapper.weightToRestingBPM("30"))
        assertEquals(100, RegistrationMapper.weightToRestingBPM("120"))
    }

    @Test
    fun parseEnergyLevel_validString_returnsLevel() {
        assertEquals(1, RegistrationMapper.parseEnergyLevel("1 - Zen"))
        assertEquals(3, RegistrationMapper.parseEnergyLevel("3 - Balanced"))
        assertEquals(5, RegistrationMapper.parseEnergyLevel("5"))
    }

    @Test
    fun parseEnergyLevel_nullOrEmpty_usesDefault3() {
        assertEquals(BioProfile.DEFAULT_ENERGY_LEVEL, RegistrationMapper.parseEnergyLevel(null))
        assertEquals(BioProfile.DEFAULT_ENERGY_LEVEL, RegistrationMapper.parseEnergyLevel(""))
    }

    @Test
    fun parseEnergyLevel_clampsTo1_5() {
        assertEquals(1, RegistrationMapper.parseEnergyLevel("0"))
        assertEquals(5, RegistrationMapper.parseEnergyLevel("9"))
    }

    @Test
    fun buildBioProfile_validInputs_createsCorrectProfile() {
        val profile = RegistrationMapper.buildBioProfile("30", "65", "4 - High Energy")
        assertEquals(190, profile.maxHeartRate)  // 220 - 30
        assertEquals(65, profile.restingBPM)
        assertEquals(4, profile.energyLevel)
    }

    @Test
    fun buildBioProfile_partialInputs_usesDefaults() {
        val profile = RegistrationMapper.buildBioProfile(null, null, null)
        assertEquals(BioProfile.DEFAULT_MAX_HR, profile.maxHeartRate)
        assertEquals(BioProfile.DEFAULT_RESTING_BPM, profile.restingBPM)
        assertEquals(BioProfile.DEFAULT_ENERGY_LEVEL, profile.energyLevel)
    }
}
