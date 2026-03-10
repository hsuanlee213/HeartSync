package com.heartbeatmusic;

public class Track {
    private final String id;
    private final String title;
    private final String artist;
    private final String coverUrl;
    private final String url;
    private int bpm;

    public Track(String id, String title, String url, int bpm) {
        this(id, title, null, null, url, bpm);
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

    public String getCoverUrl() {
        return coverUrl;
    }

    public String getUrl() {
        return url;
    }

    public int getBpm() {
        return bpm;
    }

    public void setBpm(int bpm) {
        this.bpm = bpm;
    }
}
