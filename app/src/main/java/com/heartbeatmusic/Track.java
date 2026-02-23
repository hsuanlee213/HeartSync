package com.heartbeatmusic;

public class Track {
    private final String id;
    private final String title;
    private final String url;
    private int bpm;


    public Track(String id, String title, String url, int bpm) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.bpm = bpm;
    }
    public String getId(){
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public int getBpm() { return bpm; }
    public void setBpm(int bpm) { this.bpm = bpm; }
}
