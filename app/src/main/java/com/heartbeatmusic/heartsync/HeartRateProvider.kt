package com.heartbeatmusic.heartsync

import kotlinx.coroutines.flow.Flow

/**
 * Heart rate data provider interface.
 * Provides real-time heart rate stream and supports start/stop monitoring.
 */
interface HeartRateProvider {

    /**
     * Heart rate stream, emits current BPM value every second.
     */
    val heartRateFlow: Flow<Int>

    /**
     * Start monitoring heart rate.
     */
    fun startMonitoring()

    /**
     * Stop monitoring heart rate.
     */
    fun stopMonitoring()
}
