package com.heartbeatmusic.biometric

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for BiometricFilter - music intensity calculation from BPM.
 */
class BiometricFilterTest {

    @Before
    fun setUp() {
        BiometricFilter.setBioProfile(BioProfile(maxHeartRate = 195, restingBPM = 70, energyLevel = 3))
    }

    @Test
    fun calculateMusicIntensity_atRestingBPM_returnsZero() {
        val intensity = BiometricFilter.calculateMusicIntensity(70)
        assertEquals(0f, intensity, 0.001f)
    }

    @Test
    fun calculateMusicIntensity_atMaxHeartRate_returnsOne() {
        val intensity = BiometricFilter.calculateMusicIntensity(195)
        assertEquals(1.0f, intensity, 0.001f)
    }

    @Test
    fun calculateMusicIntensity_midpoint_returnsHalf() {
        // (132.5 - 70) / (195 - 70) = 62.5/125 = 0.5
        val intensity = BiometricFilter.calculateMusicIntensity(132)
        assertEquals(0.496f, intensity, 0.01f)
    }

    @Test
    fun calculateMusicIntensity_belowResting_clampedToZero() {
        val intensity = BiometricFilter.calculateMusicIntensity(50)
        assertEquals(0f, intensity, 0.001f)
    }

    @Test
    fun calculateMusicIntensity_aboveMax_clampedToOne() {
        val intensity = BiometricFilter.calculateMusicIntensity(220)
        assertEquals(1.0f, intensity, 0.001f)
    }

    @Test
    fun calculateMusicIntensity_energyLevel4_multipliesIntensity() {
        BiometricFilter.setBioProfile(BioProfile(maxHeartRate = 195, restingBPM = 70, energyLevel = 4))
        val intensity = BiometricFilter.calculateMusicIntensity(195)
        assertEquals(1.0f, intensity, 0.001f)  // clamped at 1
    }

    @Test
    fun calculateMusicIntensity_maxHREqualsResting_usesSafeDenominator() {
        BiometricFilter.setBioProfile(BioProfile(maxHeartRate = 70, restingBPM = 70, energyLevel = 3))
        val intensity = BiometricFilter.calculateMusicIntensity(100)
        // denominator = 0, so SAFE_DENOMINATOR 110. intensityRatio = (100-70)/110 ≈ 0.273
        assertEquals(0.273f, intensity, 0.01f)
    }
}
