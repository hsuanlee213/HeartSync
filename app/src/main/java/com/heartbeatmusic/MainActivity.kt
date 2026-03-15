package com.heartbeatmusic

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.heartbeatmusic.biometric.BioProfileStorage
import com.heartbeatmusic.biometric.BiometricFilter
import com.heartbeatmusic.terminal.ArchiveFragment
import com.heartbeatmusic.terminal.SyncEngineFragment
import com.heartbeatmusic.terminal.TerminalFragment
import com.heartbeatmusic.terminal.TerminalMode
import com.heartbeatmusic.terminal.TerminalModeHolder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var tvUsername: TextView
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Global exception handler: log and prevent app freeze on Firebase/Google API failures
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception", throwable)
            runOnUiThread {
                try {
                    Toast.makeText(this, "An error occurred. Please try again.", Toast.LENGTH_LONG).show()
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to show error toast", t)
                }
            }
            Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(thread, throwable)
        }

        prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        BioProfileStorage.load(prefs)?.let { BiometricFilter.setBioProfile(it) }

        if (!prefs.contains("user_id")) {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }

        startService(Intent(this, PlayerService::class.java))
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvUsername = findViewById(R.id.tv_username)
        bottomNav = findViewById(R.id.bottom_nav)

        setupProfileButton()
        setupBottomNav()
        setupModeSwitcher()

        if (savedInstanceState == null) showFragment(TAG_TERMINAL)

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val url = intent?.getStringExtra("track_url")?.takeIf { it.isNotEmpty() } ?: return
        val title = intent.getStringExtra("track_title")?.takeIf { it.isNotEmpty() } ?: "Unknown"
        val artist = intent.getStringExtra("track_artist")?.takeIf { it.isNotEmpty() } ?: "Unknown"

        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(url))
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist).build())
            .build()
        PlayerHolder.getInstance(this).player.run {
            setMediaItem(mediaItem)
            prepare()
            play()
        }
    }

    private fun setupProfileButton() {
        tvUsername.text = getInitials(prefs.getString("username", "U") ?: "U")
        tvUsername.setOnClickListener {
            try {
                startActivity(Intent(this, UserProfileActivity::class.java))
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to open Profile", t)
                Toast.makeText(this, "Could not open profile. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_terminal -> { showFragment(TAG_TERMINAL); true }
                R.id.nav_archive -> { showFragment(TAG_ARCHIVE); true }
                R.id.nav_sync_engine -> { showFragment(TAG_SYNC_ENGINE); true }
                else -> false
            }
        }
    }

    private fun setupModeSwitcher() {
        val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.mode_switcher_root)
        updateModeButtonStyles(TerminalModeHolder.getCurrentMode())
        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.btn_mode_zen -> TerminalModeHolder.setMode(TerminalMode.ZEN)
                R.id.btn_mode_sync -> TerminalModeHolder.setMode(TerminalMode.SYNC)
                R.id.btn_mode_overdrive -> TerminalModeHolder.setMode(TerminalMode.OVERDRIVE)
            }
        }
    }

    private fun updateModeButtonStyles(mode: TerminalMode) {
        val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.mode_switcher_root)
        val checkedId = when (mode) {
            TerminalMode.ZEN -> R.id.btn_mode_zen
            TerminalMode.SYNC -> R.id.btn_mode_sync
            TerminalMode.OVERDRIVE -> R.id.btn_mode_overdrive
        }
        toggleGroup.check(checkedId)
    }

    private fun showFragment(tag: String) {
        val fragment = supportFragmentManager.findFragmentByTag(tag) ?: createFragmentForTag(tag)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, tag)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .commit()
    }

    private fun createFragmentForTag(tag: String): Fragment = when (tag) {
        TAG_TERMINAL -> TerminalFragment()
        TAG_ARCHIVE -> ArchiveFragment()
        TAG_SYNC_ENGINE -> SyncEngineFragment()
        else -> TerminalFragment()
    }

    private fun getInitials(name: String): String {
        if (name.isEmpty()) return "U"
        val parts = name.trim().split("\\s+".toRegex())
        return if (parts.size > 1) {
            parts.filter { it.isNotEmpty() }.joinToString("") { it[0].uppercaseChar().toString() }
                .ifEmpty { name[0].uppercaseChar().toString() }
        } else {
            name[0].uppercaseChar().toString()
        }
    }

    override fun onResume() {
        super.onResume()
        tvUsername.text = getInitials(prefs.getString("username", "U") ?: "U")
        updateModeButtonStyles(TerminalModeHolder.getCurrentMode())
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val TAG_TERMINAL = "terminal"
        private const val TAG_ARCHIVE = "archive"
        private const val TAG_SYNC_ENGINE = "sync_engine"
    }
}
