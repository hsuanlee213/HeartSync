package edu.northeastern.group13project;

import android.os.Bundle;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.exoplayer.ExoPlayer;

// Firebase Imports for Authentication and Firestore Write
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import edu.northeastern.group13project.data.remote.LibraryRepository;
import edu.northeastern.group13project.data.model.Song;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MusicLibraryActivity extends AppCompatActivity {

    private ExoPlayer player;
    private TextView tvPlaybackTitle;
    private Button btnPlayPause;
    private SeekBar seekBar;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateSeekRunnable = new Runnable() {
        @Override
        public void run() {
            if (player != null && seekBar != null) {
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

    private LibraryRepository repository = new LibraryRepository();
    private java.util.List<Track> trackList = new java.util.ArrayList<>();
    private TrackAdapter adapter;

    // Firebase instances
    private FirebaseFirestore db;
    private FirebaseAuth auth; // yuchen: Added auth for user ID retrieval

    // yuchen: Map to store Song ID to BPM mapping, allowing us to record BPM to history upon playback
    private final Map<String, Integer> songIdToBpmMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_music_library);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.music_library), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance(); // yuchen: Added Firestore init
        auth = FirebaseAuth.getInstance(); // yuchen: Added Auth init

        // Set the MaterialToolbar as the support ActionBar and enable Up navigation
        MaterialToolbar toolbar = findViewById(R.id.toolbar_library);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_music_library);
        }

        // Also handle navigation click (covers the toolbar navigation icon)
        toolbar.setNavigationOnClickListener(v -> finish());

        // Setup RecyclerView with track(s)
        androidx.recyclerview.widget.RecyclerView rv = findViewById(R.id.rv_tracks);
        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));

        adapter = new TrackAdapter(this, trackList, track -> {
            // play selected track using shared player and update UI
            if (player != null && track != null) {
                MediaItem mi = new MediaItem.Builder()
                        .setUri(Uri.parse(track.getUrl()))
                        .setMediaMetadata(
                                new MediaMetadata.Builder()
                                        .setTitle(track.getTitle())
                                        .build()
                        )
                        .build();
                player.setMediaItem(mi);
                player.prepare();
                player.play();
                tvPlaybackTitle.setText(track.getTitle());
                btnPlayPause.setText("Pause");

                // --- NEW: Log playback event to Firebase ---
                logPlaybackToFirebase(track);
                // ------------------------------------------
            }
        });
        rv.setAdapter(adapter);

        // Playback UI bindings (bottom bar)
        tvPlaybackTitle = findViewById(R.id.tv_playback_title);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        seekBar = findViewById(R.id.seek_bar);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { handler.removeCallbacks(updateSeekRunnable); }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (player != null) player.seekTo(seekBar.getProgress());
                handler.post(updateSeekRunnable);
            }
        });

        // Use shared player
        player = PlayerHolder.getInstance(this).getPlayer();

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

        loadSongsFromFirebase();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateSeekRunnable);
        // Do not release shared player here; owned by PlayerHolder
        // -Boxun :)
        player = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            if (player.getCurrentMediaItem() != null && player.getCurrentMediaItem().mediaMetadata != null && player.getCurrentMediaItem().mediaMetadata.title != null) {
                tvPlaybackTitle.setText(player.getCurrentMediaItem().mediaMetadata.title);
            }
            btnPlayPause.setText(player.isPlaying() ? "Pause" : "Play");
        }
        handler.post(updateSeekRunnable);

    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateSeekRunnable);
    }

    // load song from firebase
    private void loadSongsFromFirebase() {
        repository.getAllSongs(new LibraryRepository.SongsCallback() {
            @Override
            public void onSuccess(List<Song> songs) {
                trackList.clear();
                // yuchen: Clear the BPM cache map
                songIdToBpmMap.clear();

                for (Song s : songs) {
                    String docId = s.getId();       //FireBase's document Id
                    String title = s.getTitle();    //FireBase's Title
                    String url = s.getAudioUrl();   //Firebase's URL
                    // Assume Song class has a getBpm() method returning BPM (Long/Integer)
                    Integer bpm = s.getBpm() != null ? s.getBpm().intValue() : 0; // yuchen: Get BPM

                    if (docId != null && title != null && url != null && !url.isEmpty()) {
                        trackList.add(new Track(docId, title, url, bpm));
                        // yuchen: Cache Song ID and its BPM
                        songIdToBpmMap.put(docId, bpm); // TODO: might not need this line
                    }
                }

                adapter.notifyDataSetChanged();   //update UI
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(MusicLibraryActivity.this,
                        "Failed to load songs: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Logs the current track playback event to a 'history' collection in Firebase.
     * This method requires the user to be signed in.
     */
    // yuchen: New method to log track playback events to Firebase 'history' collection
    private void logPlaybackToFirebase(Track track) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (userId == null) {
            // User is not logged in, cannot save history.
            return;
        }

        // yuchen: Get BPM from the cache; default to 0 if not found
        Integer bpm = songIdToBpmMap.getOrDefault(track.getId(), 0);

        Map<String, Object> historyEntry = new HashMap<>();
        historyEntry.put("userId", userId);
        historyEntry.put("songId", track.getId());
        historyEntry.put("title", track.getTitle());
        historyEntry.put("timestamp", System.currentTimeMillis());
        // yuchen: Store BPM directly in the history record. This fixes the issue of not finding BPM in the analysis activity.
        historyEntry.put("bpm", bpm);

        // Use a dedicated 'history' collection
        db.collection("history")
                .add(historyEntry)
                .addOnSuccessListener(documentReference -> {
                    // Successfully logged, optional: Log.d(TAG, "History logged: " + track.getTitle());
                })
                .addOnFailureListener(e -> {
                    // Failed to log history
                    Toast.makeText(MusicLibraryActivity.this, "Failed to log history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}