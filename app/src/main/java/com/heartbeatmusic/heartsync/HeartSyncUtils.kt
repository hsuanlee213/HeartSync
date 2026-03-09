package com.heartbeatmusic.heartsync

/**
 * Converts BPM to animation duration in milliseconds.
 * Formula: one beat = 60000ms / bpm (e.g. 60 BPM = 1000ms per beat).
 */
fun bpmToDurationMs(bpm: Int): Int = (60000 / bpm.coerceAtLeast(1)).toInt()
