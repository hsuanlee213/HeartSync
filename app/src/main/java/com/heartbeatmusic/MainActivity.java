package com.heartbeatmusic;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.heartbeatmusic.biometric.BioProfile;
import com.heartbeatmusic.biometric.BioProfileStorage;
import com.heartbeatmusic.biometric.BiometricFilter;
import com.heartbeatmusic.terminal.ArchiveFragment;
import com.heartbeatmusic.terminal.SyncEngineFragment;
import com.heartbeatmusic.terminal.TerminalFragment;
import com.heartbeatmusic.terminal.TerminalMode;
import com.heartbeatmusic.terminal.TerminalModeHolder;

public class MainActivity extends AppCompatActivity {

    private static final String TAG_TERMINAL = "terminal";
    private static final String TAG_ARCHIVE = "archive";
    private static final String TAG_SYNC_ENGINE = "sync_engine";

    private SharedPreferences prefs;
    private TextView tvUsername;
    private BottomNavigationView bottomNav;

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
        bottomNav = findViewById(R.id.bottom_nav);

        setupProfileButton();
        setupToolbar();
        setupBottomNav();
        setupModeSwitcher();

        if (savedInstanceState == null) {
            showFragment(TAG_TERMINAL);
        }

        handleIntent(getIntent());
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

            var player = PlayerHolder.getInstance(this).getPlayer();
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
            } else if (id == R.id.nav_sync_engine) {
                showFragment(TAG_SYNC_ENGINE);
                return true;
            }
            return false;
        });
    }

    private void setupModeSwitcher() {
        com.google.android.material.button.MaterialButtonToggleGroup toggleGroup =
                findViewById(R.id.mode_switcher_root);

        updateModeButtonStyles(TerminalModeHolder.INSTANCE.getCurrentMode());

        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btn_mode_zen) {
                TerminalModeHolder.INSTANCE.setMode(TerminalMode.ZEN);
            } else if (checkedId == R.id.btn_mode_sync) {
                TerminalModeHolder.INSTANCE.setMode(TerminalMode.SYNC);
            } else if (checkedId == R.id.btn_mode_overdrive) {
                TerminalModeHolder.INSTANCE.setMode(TerminalMode.OVERDRIVE);
            }
        });
    }

    private void updateModeButtonStyles(TerminalMode mode) {
        com.google.android.material.button.MaterialButtonToggleGroup toggleGroup =
                findViewById(R.id.mode_switcher_root);
        int checkedId = mode == TerminalMode.ZEN ? R.id.btn_mode_zen
                : mode == TerminalMode.SYNC ? R.id.btn_mode_sync : R.id.btn_mode_overdrive;
        toggleGroup.check(checkedId);
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
            case TAG_SYNC_ENGINE:
                return new SyncEngineFragment();
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
        updateModeButtonStyles(TerminalModeHolder.INSTANCE.getCurrentMode());
    }
}
