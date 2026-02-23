package com.heartbeatmusic.data.model;

/**
 * Model class to represent a single combined history record,
 * including playback time from 'history' and BPM from 'songs'.
 */
public class HistoryItem {
    private String title;
    private long timestamp;
    private int bpm;

    public HistoryItem(String title, long timestamp, int bpm) {
        this.title = title;
        this.timestamp = timestamp;
        this.bpm = bpm;
    }

    public String getTitle() {
        return title;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getBpm() {
        return bpm;
    }

    public String getFormattedTime() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new java.util.Date(timestamp));
    }
}
