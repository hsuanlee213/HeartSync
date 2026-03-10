package com.heartbeatmusic;

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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import com.heartbeatmusic.data.remote.LibraryRepository;
import com.heartbeatmusic.data.model.Song;

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

    private FirebaseFirestore db;
    private FirebaseAuth auth;

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

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbar_library);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_music_library);
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        androidx.recyclerview.widget.RecyclerView rv = findViewById(R.id.rv_tracks);
        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));

        adapter = new TrackAdapter(this, trackList, track -> {
            if (player != null && track != null) {
                String artist = track.getArtist();
                String coverUrl = track.getCoverUrl();
                MediaMetadata.Builder metaBuilder = new MediaMetadata.Builder()
                        .setTitle(track.getTitle())
                        .setArtist(artist != null ? artist : "");
                if (coverUrl != null && !coverUrl.isEmpty()) {
                    metaBuilder.setArtworkUri(Uri.parse(coverUrl));
                }
                MediaItem mi = new MediaItem.Builder()
                        .setUri(Uri.parse(track.getUrl()))
                        .setMediaMetadata(metaBuilder.build())
                        .build();
                player.setMediaItem(mi);
                player.prepare();
                player.play();
                tvPlaybackTitle.setText(track.getTitle());
                btnPlayPause.setText("Pause");

                logPlaybackToFirebase(track);
            }
        });
        rv.setAdapter(adapter);

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

    private void loadSongsFromFirebase() {
        repository.getAllSongs(new LibraryRepository.SongsCallback() {
            @Override
            public void onSuccess(List<Song> songs) {
                trackList.clear();
                songIdToBpmMap.clear();

                for (Song s : songs) {
                    String docId = s.getId();
                    String title = s.getTitle();
                    String artist = s.getArtist();
                    String coverUrl = s.getCoverUrl();
                    String url = s.getAudioUrl();
                    Integer bpm = s.getBpm() != null ? s.getBpm().intValue() : 0;

                    if (docId != null && title != null && url != null && !url.isEmpty()) {
                        trackList.add(new Track(docId, title, artist, coverUrl, url, bpm));
                        songIdToBpmMap.put(docId, bpm);
                    }
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(MusicLibraryActivity.this,
                        "Failed to load songs: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logPlaybackToFirebase(Track track) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (userId == null) {
            return;
        }

        Integer bpm = songIdToBpmMap.getOrDefault(track.getId(), 0);

        Map<String, Object> historyEntry = new HashMap<>();
        historyEntry.put("userId", userId);
        historyEntry.put("songId", track.getId());
        historyEntry.put("title", track.getTitle());
        historyEntry.put("timestamp", System.currentTimeMillis());
        historyEntry.put("bpm", bpm);

        db.collection("history")
                .add(historyEntry)
                .addOnSuccessListener(documentReference -> {
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MusicLibraryActivity.this, "Failed to log history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
