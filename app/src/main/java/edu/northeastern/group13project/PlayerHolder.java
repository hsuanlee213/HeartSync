package edu.northeastern.group13project;

import android.content.Context;
 

import androidx.annotation.NonNull;

 
import androidx.media3.exoplayer.ExoPlayer;

/**
 * Simple application-scoped holder for a single ExoPlayer instance so different Activities
 * can share playback state and UI.
 */
public class PlayerHolder {

    private static PlayerHolder instance;
    private final ExoPlayer player;

    private PlayerHolder(@NonNull Context appContext) {
        player = new ExoPlayer.Builder(appContext).build();
        // Do not set a default media item here; Activities will set MediaItem with proper metadata
        // -Note by Boxun :)
    }

    public static synchronized PlayerHolder getInstance(@NonNull Context context) {
        if (instance == null) {
            Context appContext = context.getApplicationContext();
            instance = new PlayerHolder(appContext);
        }
        return instance;
    }

    public ExoPlayer getPlayer() {
        return player;
    }

    public void release() {
        player.release();
    }
}
