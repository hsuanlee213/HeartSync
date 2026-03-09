package com.heartbeatmusic;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import android.os.Bundle;
import com.heartbeatmusic.biometric.BioProfile;
import com.heartbeatmusic.biometric.BioProfileStorage;
import com.heartbeatmusic.biometric.BiometricFilter;
import com.heartbeatmusic.data.model.Song;
import com.heartbeatmusic.data.remote.FirebaseStorageManager;
import com.heartbeatmusic.data.remote.MusicRepository;
import com.heartbeatmusic.heartsync.ActivityMode;
import com.heartbeatmusic.heartsync.HeartSyncBpmContentKt;
import com.heartbeatmusic.heartsync.HeartSyncViewModel;
import androidx.compose.ui.platform.ComposeView;
import androidx.lifecycle.ViewModelProvider;

public class MainActivity extends AppCompatActivity {

    private MusicRepository repository = new MusicRepository();
    private FirebaseStorageManager storageManager;
    private ExoPlayer player;
    private Button btnLibrary;
    private TextView tvTrack;
    private TextView tvPlaybackTitle;
    private Button btnPlayPause;
    private SeekBar seekBar;

    // REMOVED: private ImageButton btnUserProfile;
    private TextView tvUsername;
    private SharedPreferences prefs;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateSeekRunnable = new Runnable() {
        @Override
        public void run() {
            if (player != null) {
                long pos = player.getCurrentPosition();
                long dur = player.getDuration();
                if (dur > 0 && dur <= Integer.MAX_VALUE) {
                    seekBar.setMax((int) dur);
                    seekBar.setProgress((int) pos);
                } else if (dur > Integer.MAX_VALUE) {
                    seekBar.setMax(Integer.MAX_VALUE);
                    seekBar.setProgress((int) Math.min(pos, Integer.MAX_VALUE));
                }
            }
            handler.postDelayed(this, 500);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        // Restore Bio-Profile from SharedPreferences (e.g. after app restart)
        BioProfile savedProfile = BioProfileStorage.INSTANCE.load(prefs);
        if (savedProfile != null) {
            BiometricFilter.INSTANCE.setBioProfile(savedProfile);
        }
        if (!prefs.contains("user_id")) {
            Intent loginIntent = new Intent(this, LoginActivity.class);
            loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginIntent);
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnLibrary = findViewById(R.id.btn_library);
        tvPlaybackTitle = findViewById(R.id.tv_playback_title);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        tvTrack = findViewById(R.id.tv_song_info);
        seekBar = findViewById(R.id.seek_bar);

        // HeartSync: Compose UI for real-time heart rate BPM (includes pulse animation)
        ComposeView composeBpm = findViewById(R.id.compose_bpm);
        HeartSyncBpmContentKt.setHeartSyncBpmContent(composeBpm);

        HeartSyncViewModel heartSyncVm = new ViewModelProvider(this).get(HeartSyncViewModel.class);

        // mode clicking logic
        ImageView btnExercise = findViewById(R.id.btn_exercise);
        ImageView btnCalm     = findViewById(R.id.btn_calm);
        ImageView btnDriving  = findViewById(R.id.btn_driving);

        // when clicked play song with given mode + update HeartSync BPM range
        if (btnExercise != null) {
            btnExercise.setOnClickListener(v -> {
                heartSyncVm.setMode(ActivityMode.EXERCISE);
                playRandomSongForMode("exercise");
            });
        }
        if (btnCalm != null) {
            btnCalm.setOnClickListener(v -> {
                heartSyncVm.setMode(ActivityMode.CALM);
                playRandomSongForMode("calm");
            });
        }
        if (btnDriving != null) {
            btnDriving.setOnClickListener(v -> {
                heartSyncVm.setMode(ActivityMode.DRIVING);
                playRandomSongForMode("driving");
            });
        }

        tvUsername = findViewById(R.id.tv_username);

        // Set username display to user's initials
        String username = prefs.getString("username", "U");
        String initials = getInitials(username);

        if (tvUsername != null) {
            tvUsername.setText(initials);

            // Bind the click listener to the TextView acting as the button
            tvUsername.setOnClickListener(v -> {
                Intent profileIntent = new Intent(MainActivity.this, UserProfileActivity.class);
                startActivity(profileIntent);
            });
        }

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean fromUserTouch = false;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                fromUserTouch = fromUser;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(updateSeekRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (player != null) {
                    player.seekTo(seekBar.getProgress());
                }
                handler.post(updateSeekRunnable);
            }
        });

        player = PlayerHolder.getInstance(this).getPlayer();

        // Update UI when the current media item changes (e.g., moving to next track in playlist)
        player.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                if (mediaItem == null) return;
                MediaMetadata md = mediaItem.mediaMetadata;
                String displayTitle = (md != null && md.title != null) ? md.title.toString() : "Unknown";
                String displayArtist = (md != null && md.artist != null) ? md.artist.toString() : "Unknown";

                runOnUiThread(() -> {
                    if (tvPlaybackTitle != null) tvPlaybackTitle.setText(displayTitle);
                    if (tvTrack != null) tvTrack.setText(displayTitle + " - " + displayArtist);
                });

                // BPM displayed by HeartSync Compose UI
            }
        });

        handleIntent(getIntent());

        btnPlayPause.setOnClickListener(v -> {
            if (player.isPlaying()) {
                player.pause();
                btnPlayPause.setText("Play");
            } else {
                player.play();
                btnPlayPause.setText("Pause");
            }
        });

        handler.post(updateSeekRunnable);

        btnLibrary.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, MusicLibraryActivity.class)));

        repository.getAllSongs(new MusicRepository.SongsCallback() {
            @Override
            public void onSuccess(List<Song> songs) {
                Log.d("MainActivity", "Fetched all songs");
            }

            @Override
            public void onError(Exception e) {

            }
        });

        // TODO: Remove this sample code
        storageManager = new FirebaseStorageManager();
//        String testSong = "songs/test/slow_song_1.mp3";
//        storageManager.getSongUrl(testSong,
//                uri -> {
//                    String url = uri.toString();
//                    Log.d("Firebase", "Get url: " + url);
//                },
//                e -> {
//                    Log.e("Firebase", "Failed to load song", e);
//                }
//        );

    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) {
            return "U";
        }

        String[] parts = name.trim().split("\\s+");
        if (parts.length > 1) {
            StringBuilder initials = new StringBuilder();
            for (String part : parts) {
                if (!part.isEmpty()) {
                    initials.append(part.toUpperCase().charAt(0));
                }
            }
            return initials.toString().isEmpty() ? name.substring(0, 1).toUpperCase() : initials.toString();
        } else {
            return name.substring(0, 1).toUpperCase();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        String title = intent.getStringExtra("track_title");
        String url = intent.getStringExtra("track_url");
        String artist = intent.getStringExtra("track_artist");
        int bpm = intent.getIntExtra("track_bpm", -1);

        if (url != null && !url.isEmpty()) {
            tvPlaybackTitle.setText(title != null ? title : "Unknown");
            String displayTitle  = (title  != null && !title.isEmpty())  ? title  : "Unknown";
            String displayArtist = (artist != null && !artist.isEmpty()) ? artist : "Unknown";

            tvPlaybackTitle.setText(displayTitle);

            // now playing card - song title + artist name
            if (tvTrack != null) {
                tvTrack.setText(displayTitle + " - " + displayArtist);
            }

            // BPM displayed by HeartSync Compose UI

            Bundle extras = new Bundle();
            if (bpm > 0) {
                extras.putInt("bpm", bpm);
            }

            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(Uri.parse(url))
                    .setMediaMetadata(
                            new MediaMetadata.Builder()
                                    .setTitle(displayTitle)
                                    .setArtist(displayArtist)
                                    .setExtras(extras)
                                    .build()
                    )
                    .build();

            player.setMediaItem(mediaItem);
            player.prepare();
            player.play();
            btnPlayPause.setText("Pause");
        } else {
            tvPlaybackTitle.setText("Not playing");
            btnPlayPause.setText("Play");
        }
    }

    // Play all songs for a specific mode as a playlist (queue)
    private void playRandomSongForMode(String modeTag) {
        repository.getSongsForMode(modeTag, new MusicRepository.SongsCallback() {
            @Override
            public void onSuccess(List<Song> songs) {
                if (songs == null || songs.isEmpty()) {
                    android.widget.Toast.makeText(
                            MainActivity.this,
                            "No songs for mode: " + modeTag,
                            android.widget.Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                ArrayList<MediaItem> items = new ArrayList<>();
                // build MediaItem list from returned songs
                for (Song s : songs) {
                    String url = s.getAudioUrl();
                    if (url == null || url.isEmpty()) continue;

                    String title  = (s.getTitle()  != null && !s.getTitle().isEmpty()) ? s.getTitle() : "Unknown";
                    String artist = (s.getArtist() != null && !s.getArtist().isEmpty()) ? s.getArtist() : "Unknown";
                    int bpm = (s.getBpm() != null) ? s.getBpm().intValue() : -1;

                    Bundle extras = new Bundle();
                    if (bpm > 0) {
                        extras.putInt("bpm", bpm);
                    }
                    if (s.getCoverUrl() != null && !s.getCoverUrl().isEmpty()) {
                        extras.putString("coverUrl", s.getCoverUrl());
                    }

                    MediaItem mediaItem = new MediaItem.Builder()
                            .setUri(android.net.Uri.parse(url))
                            .setMediaMetadata(
                                    new MediaMetadata.Builder()
                                            .setTitle(title)
                                            .setArtist(artist)
                                            .setExtras(extras)
                                            .build()
                            )
                            .build();

                    items.add(mediaItem);
                }

                if (items.isEmpty()) {
                    android.widget.Toast.makeText(
                            MainActivity.this,
                            "No playable songs for mode: " + modeTag,
                            android.widget.Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                // shuffle playlist for random playback
                Collections.shuffle(items);
                player.setShuffleModeEnabled(true);

                // set the full playlist and start playback (plays sequentially but shuffled)
                player.setMediaItems(items, /* resetPosition= */ true);
                player.prepare();
                player.play();

                // update UI using the current item (after shuffle the first item is current)
                MediaItem first = player.getCurrentMediaItem();
                if (first == null) {
                    first = items.get(0);
                }
                MediaMetadata md = (first != null) ? first.mediaMetadata : null;
                String displayTitle = (md != null && md.title != null)
                        ? md.title.toString()
                        : "Unknown";
                String displayArtist = (md != null && md.artist != null)
                        ? md.artist.toString()
                        : "Unknown";

                tvPlaybackTitle.setText(displayTitle);
                btnPlayPause.setText("Pause");

                if (tvTrack != null) {
                    tvTrack.setText(displayTitle + " - " + displayArtist);
                }

                // BPM displayed by HeartSync Compose UI
            }

            @Override
            public void onError(Exception e) {
                android.util.Log.e("MainActivity", "Failed to load songs for mode " + modeTag, e);
                android.widget.Toast.makeText(
                        MainActivity.this,
                        "Failed to load songs",
                        android.widget.Toast.LENGTH_SHORT
                ).show();
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateSeekRunnable);
        player = null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (player != null) {
            if (player.getCurrentMediaItem() != null) {
                MediaMetadata md = player.getCurrentMediaItem().mediaMetadata;

                String displayTitle = (md != null && md.title != null)
                        ? md.title.toString()
                        : "Unknown";

                String displayArtist = (md != null && md.artist != null)
                        ? md.artist.toString()
                        : "Unknown";

                // bottom display bar title
                tvPlaybackTitle.setText(displayTitle);

                // below NOW PLAYING : song title / singer
                if (tvTrack != null) {
                    tvTrack.setText(displayTitle + " - " + displayArtist);
                }

                // BPM displayed by HeartSync Compose UI
            }

            btnPlayPause.setText(player.isPlaying() ? "Pause" : "Play");
        }

        handler.post(updateSeekRunnable);

        // Refresh initials on resume
        String username = prefs.getString("username", "U");
        String initials = getInitials(username);
        if (tvUsername != null) {
            tvUsername.setText(initials);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateSeekRunnable);
    }
}
