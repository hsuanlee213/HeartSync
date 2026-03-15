package com.heartbeatmusic.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HistoryItem(
    val title: String,
    val timestamp: Long,
    val bpm: Int
) {
    fun getFormattedTime(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}
