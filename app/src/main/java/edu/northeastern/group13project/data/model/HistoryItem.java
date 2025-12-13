package edu.northeastern.group13project.data.model;

/**
 * Model class to represent a single combined history record,
 * including playback time from 'history' and BPM from 'songs'.
 */
public class HistoryItem {
    private String title;
    private long timestamp; // Stored as long (milliseconds)
    private int bpm; // BPM value fetched from the 'songs' collection

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

    // Helper method to format the time for display
    public String getFormattedTime() {
        // yuchen... Using standard date format for displaying playback time
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new java.util.Date(timestamp));
    }
}