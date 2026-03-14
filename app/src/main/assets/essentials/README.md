# Essentials - Upload Source (not bundled in app)

Test audio is stored in Firebase Storage. The app downloads and caches locally.

To upload audio files:

1. Create `scripts/essentials/` and place:
   - **zen.mp3** – ZEN mode (ambient, meditation)
   - **sync.mp3** – SYNC mode (deep house, lofi)
   - **overdrive.mp3** – OVERDRIVE mode (techno, high-tempo)

2. Run: `cd scripts && node upload-essentials-to-storage.js`

These files are not bundled in the app.
