package com.heartbeatmusic.data.model;

import java.util.List;
public class ActivityMode {
    private String id;
    private String name;
    private String description;

    private int minBpm;
    private int maxBpm;
    private List<String> baseGenres;
    private long createdAt;

    public ActivityMode() {
    }

    public ActivityMode(String id, String name, String description,
                        int minBpm, int maxBpm,
                        List<String> baseGenres, long createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.minBpm = minBpm;
        this.maxBpm = maxBpm;
        this.baseGenres = baseGenres;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getMinBpm() {
        return minBpm;
    }

    public void setMinBpm(int minBpm) {
        this.minBpm = minBpm;
    }

    public int getMaxBpm() {
        return maxBpm;
    }

    public void setMaxBpm(int maxBpm) {
        this.maxBpm = maxBpm;
    }

    public List<String> getBaseGenres() {
        return baseGenres;
    }

    public void setBaseGenres(List<String> baseGenres) {
        this.baseGenres = baseGenres;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
