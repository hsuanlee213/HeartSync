package com.heartbeatmusic.biometric

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for BioProfile - energy multiplier and constants.
 */
class BioProfileTest {

    @Test
    fun energyMultiplier_level1_returns06() {
        val profile = BioProfile(195, 70, 1)
        assertEquals(0.6f, profile.energyMultiplier, 0.001f)
    }

    @Test
    fun energyMultiplier_level2_returns08() {
        val profile = BioProfile(195, 70, 2)
        assertEquals(0.8f, profile.energyMultiplier, 0.001f)
    }

    @Test
    fun energyMultiplier_level3_returns10() {
        val profile = BioProfile(195, 70, 3)
        assertEquals(1.0f, profile.energyMultiplier, 0.001f)
    }

    @Test
    fun energyMultiplier_level4_returns12() {
        val profile = BioProfile(195, 70, 4)
        assertEquals(1.2f, profile.energyMultiplier, 0.001f)
    }

    @Test
    fun energyMultiplier_level5_returns14() {
        val profile = BioProfile(195, 70, 5)
        assertEquals(1.4f, profile.energyMultiplier, 0.001f)
    }

    @Test
    fun energyMultiplier_invalidLevel_returns10() {
        val profile = BioProfile(195, 70, 0)
        assertEquals(1.0f, profile.energyMultiplier, 0.001f)
    }
}
