package edu.northeastern.group13project.data.model;

public class HeartRateLog {

    private String id;
    private String userId;
    private long timestamp;
    private int bpm;
    private String source;
    private String modeId;
    private String sessionId;

    public HeartRateLog() {
    }

    public HeartRateLog(String id, String userId, long timestamp, int bpm,
                        String source, String modeId, String sessionId) {
        this.id = id;
        this.userId = userId;
        this.timestamp = timestamp;
        this.bpm = bpm;
        this.source = source;
        this.modeId = modeId;
        this.sessionId = sessionId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getBpm() {
        return bpm;
    }

    public void setBpm(int bpm) {
        this.bpm = bpm;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getModeId() {
        return modeId;
    }

    public void setModeId(String modeId) {
        this.modeId = modeId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
