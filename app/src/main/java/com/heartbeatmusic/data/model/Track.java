package com.heartbeatmusic.data.model;

/**
 * Model class for a single song/track in the music library.
 */
public class Track {
    private String id;
    private String title;
    private String artist;
    private String coverUrl;
    private String url;
    private int bpm;

    public Track() {
    }

    public Track(String id, String title, String url, int bpm) {
        this(id, title, null, url, bpm);
    }

    public Track(String id, String title, String artist, String url, int bpm) {
        this(id, title, artist, null, url, bpm);
    }

    public Track(String id, String title, String artist, String coverUrl, String url, int bpm) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.coverUrl = coverUrl;
        this.url = url;
        this.bpm = bpm;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getUrl() {
        return url;
    }

    public int getBpm() {
        return bpm;
    }

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
