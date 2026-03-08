package com.heartbeatmusic.heartsync

/**
 * Activity mode, each maps to a different BPM range.
 */
enum class ActivityMode(val minBpm: Int, val maxBpm: Int) {
    EXERCISE(120, 160),
    DRIVING(80, 120),
    CALM(50, 80);
}
