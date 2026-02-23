package com.heartbeatmusic.data.model;

import java.util.List;

public class Playlist {

    private String id;
    private String modeId;
    private String name;
    private List<String> songIds;
    private long createdAt;
    private long updatedAt;

    public Playlist() {
    }

    public Playlist(String id, String modeId, String name,
                    List<String> songIds, long createdAt, long updatedAt) {
        this.id = id;
        this.modeId = modeId;
        this.name = name;
        this.songIds = songIds;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModeId() {
        return modeId;
    }

    public void setModeId(String modeId) {
        this.modeId = modeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getSongIds() {
        return songIds;
    }

    public void setSongIds(List<String> songIds) {
        this.songIds = songIds;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
