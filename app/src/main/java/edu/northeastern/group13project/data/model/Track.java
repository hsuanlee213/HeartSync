package edu.northeastern.group13project.data.model;

/**
 * Model class for a single song/track in the music library.
 * This file must exist in your project structure.
 */
public class Track {
    private String id;
    private String title;
    private String url;
    // IMPORTANT: Ensure this field exists and is correctly initialized from Firestore
    private int bpm;

    // Required empty constructor for Firestore deserialization
    public Track() {
    }

    public Track(String id, String title, String url, int bpm) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.bpm = bpm;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public int getBpm() {
        return bpm;
    }

    // Setters (required if using reflection for deserialization)
    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setBpm(int bpm) {
        this.bpm = bpm;
    }
}