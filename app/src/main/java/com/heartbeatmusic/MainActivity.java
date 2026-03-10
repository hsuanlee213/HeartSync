package com.heartbeatmusic;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.heartbeatmusic.biometric.BioProfile;
import com.heartbeatmusic.biometric.BioProfileStorage;
import com.heartbeatmusic.biometric.BiometricFilter;
import com.heartbeatmusic.terminal.ArchiveFragment;
import com.heartbeatmusic.terminal.SettingsFragment;
import com.heartbeatmusic.terminal.TerminalFragment;
import com.heartbeatmusic.data.remote.LibraryRepository;
import com.heartbeatmusic.data.model.Song;
import com.heartbeatmusic.terminal.TerminalMode;
import com.heartbeatmusic.terminal.TerminalModeHolder;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG_TERMINAL = "terminal";
    private static final String TAG_ARCHIVE = "archive";
    private static final String TAG_SETTINGS = "settings";

    private SharedPreferences prefs;
    private TextView tvUsername;
    private TextView tvPlaybackTitle;
    private Button btnPlayPause;
    private BottomNavigationView bottomNav;
    private ExoPlayer player;
    private LibraryRepository libraryRepository = new LibraryRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
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

        tvUsername = findViewById(R.id.tv_username);
        tvPlaybackTitle = findViewById(R.id.tv_playback_title);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        bottomNav = findViewById(R.id.bottom_nav);
        player = PlayerHolder.getInstance(this).getPlayer();

        setupProfileButton();
        setupPlayback();
        setupToolbar();
        setupBottomNav();
        setupModeSwitcher();

        if (savedInstanceState == null) {
            showFragment(TAG_TERMINAL);
        }

        handleIntent(getIntent());

        player.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                if (mediaItem == null) return;
                MediaMetadata md = mediaItem.mediaMetadata;
                String title = (md != null && md.title != null) ? md.title.toString() : "Unknown";
                runOnUiThread(() -> {
                    if (tvPlaybackTitle != null) tvPlaybackTitle.setText(title);
                });
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                runOnUiThread(() -> {
                    if (btnPlayPause != null) {
                        btnPlayPause.setText(isPlaying ? "Pause" : "Play");
                    }
                });
            }
        });
    }

    private void setupPlayback() {
        btnPlayPause.setOnClickListener(v -> {
            if (player.isPlaying()) {
                player.pause();
                return;
            }
            if (player.getCurrentMediaItem() != null) {
                player.play();
                return;
            }
            loadAndPlayFirstSong();
        });
    }

    private void loadAndPlayFirstSong() {
        libraryRepository.getAllSongs(new LibraryRepository.SongsCallback() {
            @Override
            public void onSuccess(List<Song> songs) {
                runOnUiThread(() -> {
                    if (songs == null || songs.isEmpty()) {
                        Toast.makeText(MainActivity.this,
                                R.string.no_songs_available,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Song song = songs.get(0);
                    String url = song.getAudioUrl();
                    String title = song.getTitle();
                    String artist = song.getArtist();
                    String coverUrl = song.getCoverUrl();
                    if (url == null || url.isEmpty()) {
                        Toast.makeText(MainActivity.this,
                                R.string.no_songs_available,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String displayTitle = (title != null && !title.isEmpty()) ? title : "Unknown";
                    String displayArtist = (artist != null && !artist.isEmpty()) ? artist : "Unknown";
                    MediaMetadata.Builder metaBuilder = new MediaMetadata.Builder()
                            .setTitle(displayTitle)
                            .setArtist(displayArtist);
                    if (coverUrl != null && !coverUrl.isEmpty()) {
                        metaBuilder.setArtworkUri(Uri.parse(coverUrl));
                    }
                    MediaItem mediaItem = new MediaItem.Builder()
                            .setUri(Uri.parse(url))
                            .setMediaMetadata(metaBuilder.build())
                            .build();
                    player.setMediaItem(mediaItem);
                    player.prepare();
                    player.play();
                    tvPlaybackTitle.setText(displayTitle);
                    btnPlayPause.setText("Pause");
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        "Failed to load songs: " + (e != null ? e.getMessage() : "Unknown"),
                        Toast.LENGTH_SHORT).show());
            }
        });
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

        if (url != null && !url.isEmpty()) {
            String displayTitle = (title != null && !title.isEmpty()) ? title : "Unknown";
            String displayArtist = (artist != null && !artist.isEmpty()) ? artist : "Unknown";
            tvPlaybackTitle.setText(displayTitle);

            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(Uri.parse(url))
                    .setMediaMetadata(
                            new MediaMetadata.Builder()
                                    .setTitle(displayTitle)
                                    .setArtist(displayArtist)
                                    .build())
                    .build();
            player.setMediaItem(mediaItem);
            player.prepare();
            player.play();
            btnPlayPause.setText("Pause");
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_library) {
                startActivity(new Intent(this, MusicLibraryActivity.class));
                return true;
            }
            return false;
        });
    }

    private void setupProfileButton() {
        String username = prefs.getString("username", "U");
        String initials = getInitials(username);
        tvUsername.setText(initials);
        tvUsername.setOnClickListener(v -> {
            startActivity(new Intent(this, UserProfileActivity.class));
        });
    }

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_terminal) {
                showFragment(TAG_TERMINAL);
                return true;
            } else if (id == R.id.nav_archive) {
                showFragment(TAG_ARCHIVE);
                return true;
            } else if (id == R.id.nav_settings) {
                showFragment(TAG_SETTINGS);
                return true;
            }
            return false;
        });
    }

    private void setupModeSwitcher() {
        MaterialButton btnZen = findViewById(R.id.btn_mode_zen);
        MaterialButton btnSync = findViewById(R.id.btn_mode_sync);
        MaterialButton btnOverdrive = findViewById(R.id.btn_mode_overdrive);

        updateModeButtonStyles(TerminalModeHolder.INSTANCE.getCurrentMode());

        btnZen.setOnClickListener(v -> {
            TerminalModeHolder.INSTANCE.setMode(TerminalMode.ZEN);
            updateModeButtonStyles(TerminalMode.ZEN);
        });
        btnSync.setOnClickListener(v -> {
            TerminalModeHolder.INSTANCE.setMode(TerminalMode.SYNC);
            updateModeButtonStyles(TerminalMode.SYNC);
        });
        btnOverdrive.setOnClickListener(v -> {
            TerminalModeHolder.INSTANCE.setMode(TerminalMode.OVERDRIVE);
            updateModeButtonStyles(TerminalMode.OVERDRIVE);
        });
    }

    private void updateModeButtonStyles(TerminalMode mode) {
        MaterialButton btnZen = findViewById(R.id.btn_mode_zen);
        MaterialButton btnSync = findViewById(R.id.btn_mode_sync);
        MaterialButton btnOverdrive = findViewById(R.id.btn_mode_overdrive);

        btnZen.setChecked(mode == TerminalMode.ZEN);
        btnSync.setChecked(mode == TerminalMode.SYNC);
        btnOverdrive.setChecked(mode == TerminalMode.OVERDRIVE);
    }

    private void showFragment(String tag) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        if (fragment == null) {
            fragment = createFragmentForTag(tag);
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    private Fragment createFragmentForTag(String tag) {
        switch (tag) {
            case TAG_TERMINAL:
                return new TerminalFragment();
            case TAG_ARCHIVE:
                return new ArchiveFragment();
            case TAG_SETTINGS:
                return new SettingsFragment();
            default:
                return new TerminalFragment();
        }
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "U";
        String[] parts = name.trim().split("\\s+");
        if (parts.length > 1) {
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                if (!part.isEmpty()) sb.append(part.toUpperCase().charAt(0));
            }
            return sb.length() > 0 ? sb.toString() : name.substring(0, 1).toUpperCase();
        }
        return name.substring(0, 1).toUpperCase();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String username = prefs.getString("username", "U");
        tvUsername.setText(getInitials(username));
        if (player != null) {
            if (player.getCurrentMediaItem() != null) {
                MediaMetadata md = player.getCurrentMediaItem().mediaMetadata;
                String title = (md != null && md.title != null) ? md.title.toString() : "Not playing";
                tvPlaybackTitle.setText(title);
            } else {
                tvPlaybackTitle.setText("Not playing");
            }
            btnPlayPause.setText(player.isPlaying() ? "Pause" : "Play");
        }
    }
}
