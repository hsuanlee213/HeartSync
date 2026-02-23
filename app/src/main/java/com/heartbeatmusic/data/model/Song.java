package com.heartbeatmusic.data.model;

import java.util.List;
public class Song {


    private String id;
    private String title;
    private String artist;
    private Long bpm;
    private int durationSec;
    private String genre;
    private List<String> tags;
    private String audioSourceType;
    private String localFileId;
    private String audioUrl;
    private String coverUrl;

    public Song() {
    }

    public Song(String id, String title, String artist, Long bpm, int durationSec,
                String genre, List<String> tags, String audioSourceType,
                String localFileId, String audioUrl, String coverUrl) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.bpm = bpm;
        this.durationSec = durationSec;
        this.genre = genre;
        this.tags = tags;
        this.audioSourceType = audioSourceType;
        this.localFileId = localFileId;
        this.audioUrl = audioUrl;
        this.coverUrl = coverUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public Long getBpm() {
        return bpm;
    }

    public void setBpm(Long bpm) {
        this.bpm = bpm;
    }

    public int getDurationSec() {
        return durationSec;
    }

    public void setDurationSec(int durationSec) {
        this.durationSec = durationSec;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getAudioSourceType() {
        return audioSourceType;
    }

    public void setAudioSourceType(String audioSourceType) {
        this.audioSourceType = audioSourceType;
    }

    public String getLocalFileId() {
        return localFileId;
    }

    public void setLocalFileId(String localFileId) {
        this.localFileId = localFileId;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

}
