package edu.northeastern.group13project;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat; // <-- Re-added for tinting
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Import necessary Firebase modules
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.material.appbar.MaterialToolbar; // <-- Re-added Toolbar import

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Activity responsible for calculating and displaying user playback metrics directly
 * from the 'history' and 'songs' Firestore collections.
 */
// yuchen... Modified to calculate metrics directly instead of reading pre-calculated data
public class AnalyzeHistoryActivity extends AppCompatActivity {

    private static final String TAG = "AnalyzeHistoryActivity";
    // tvTotalTime will now display "Total Plays"
    private TextView tvTotalTime;
    private TextView tvAvgBpm;
    private TextView tvTopMode;

    // Firebase instances
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Assuming your layout file is named 'activity_analyze_history.xml'
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_analyze_history);

        // --- Back Button Implementation START ---
        // 1. Find the custom Toolbar defined in XML (using ID from the previous response)
        MaterialToolbar toolbar = findViewById(R.id.toolbar_history);

        if (toolbar != null) {
            // 2. Set the custom Toolbar as the Activity's ActionBar
            setSupportActionBar(toolbar);

            if (getSupportActionBar() != null) {
                // 3. Enable the Up button (the standard back arrow icon)
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Listening Analysis");

                // 4. Ensure the navigation icon (back arrow) is white
                try {
                    toolbar.setNavigationIconTint(ContextCompat.getColor(this, android.R.color.white));
                } catch (Exception e) {
                    Log.e(TAG, "Failed to set navigation icon tint.", e);
                }
            }
        }
        // --- Back Button Implementation END ---

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.analyze_history), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase services
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Bind views
        tvTotalTime = findViewById(R.id.tv_total_time);
        tvAvgBpm = findViewById(R.id.tv_avg_bpm);
        tvTopMode = findViewById(R.id.tv_top_mode);

        // Start loading the user's analysis data
        loadUserMetrics();
    }

    // Handle the Up button (back arrow) click event
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Finish the current activity to return to the parent activity
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Loads the user's playback history and calculates Total Plays, Average BPM, and Top Mode.
     */
    // yuchen... New method to perform client-side calculation of metrics
    private void loadUserMetrics() {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Log.w(TAG, "No authenticated user found. Showing default metrics.");
            // Display default data
            displayMetrics("0 Plays", "0 BPM", "N/A");
            // login for other info
            Toast.makeText(this, "Please log in to view personalized analysis.", Toast.LENGTH_LONG).show();
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Authenticated User ID: " + userId);

        // Step 1: Query the user's playback history records
        // IMPORTANT: Composite Index must be: Collection: history, Field: userId (Ascending), Field: timestamp (Descending)
        db.collection("history")
                .whereEqualTo("userId", userId)
                // Order by the timestamp field, ensuring the field name exactly matches the one in Firestore
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "History query successful. Documents found: " + queryDocumentSnapshots.size());

                    List<DocumentSnapshot> historyDocs = queryDocumentSnapshots.getDocuments();

                    if (historyDocs.isEmpty()) {
                        displayMetrics("0 Plays", "N/A", "N/A");
                        Toast.makeText(this, "No playback history records found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Calculation 1: Total Plays
                    int totalPlays = historyDocs.size();
                    tvTotalTime.setText(totalPlays + " Plays");

                    // Extract all unique songIds for BPM and Category (Mode) lookup
                    // These songIds are the Document IDs in the 'songs' collection
                    Set<String> uniqueSongIds = historyDocs.stream()
                            .map(doc -> doc.getString("songId"))
                            .filter(id -> id != null && !id.isEmpty())
                            .collect(Collectors.toSet());

                    Log.d(TAG, "Total unique song IDs to lookup: " + uniqueSongIds.size());

                    if (uniqueSongIds.isEmpty()) {
                        // If no valid songId is found, cannot calculate Avg BPM or Top Mode (Category)
                        tvAvgBpm.setText("N/A");
                        tvTopMode.setText("N/A");
                        Log.w(TAG, "No valid songIds found in history documents. Cannot calculate Avg BPM or Top Mode.");
                        return;
                    }

                    // Step 2: Query songs by their Document ID (since 'songId' matches the Document ID)
                    // We use FieldPath.documentId() to query by the document's ID.

                    List<String> songIdsList = new ArrayList<>(uniqueSongIds);

                    // Limit query to 10 unique IDs due to Firestore constraint
                    List<String> queryBatch = songIdsList.subList(0, Math.min(songIdsList.size(), 10));

                    if (songIdsList.size() > 10) {
                        Toast.makeText(AnalyzeHistoryActivity.this,
                                "Note: Due to Firestore query limits, only the first 10 unique songs' BPM and Mode are analyzed.",
                                Toast.LENGTH_LONG).show();
                    }

                    Log.d(TAG, "Querying BPMs and Categories for song Document IDs (limited to 10): " + queryBatch.toString());

                    // MODIFICATION: Using the special field name for Document ID query
                    // This query assumes 'songId' in history matches the 'songs' Document ID.
                    db.collection("songs")
                            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), queryBatch)
                            .get()
                            .addOnSuccessListener(songSnapshots -> {
                                Log.d(TAG, "Songs query successful. BPM/Mode documents found: " + songSnapshots.size());
                                Map<String, Integer> songIdToBpm = new HashMap<>();
                                Map<String, List<String>> songIdToTags = new HashMap<>();

                                for (DocumentSnapshot doc : songSnapshots.getDocuments()) {
                                    // BPM is assumed to be stored as a number (Long)
                                    Long bpmLong = doc.getLong("bpm");
                                    Integer bpm = (bpmLong != null) ? bpmLong.intValue() : 0;

                                    // IMPORTANT: Use the Document ID as the key for the map
                                    String songId = doc.getId();

                                    // Mode is stored in the 'category' field
                                    List<String> tags = (List<String>) doc.get("tags");
                                    if (tags == null) tags = new ArrayList<>();
                                    songIdToTags.put(songId, tags);

                                    // Store BPM
                                    songIdToBpm.put(songId, bpm);
                                    Log.d(TAG, "Mapped Song ID: " + songId + " to BPM: " + bpm);
                                }

                                // Calculation 2: Average BPM
                                calculateAndDisplayAvgBPM(historyDocs, songIdToBpm);

                                // Calculation 3: Top Mode (using songIdToMode map)
                                calculateAndDisplayTopMode(historyDocs, songIdToTags);

                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to load song BPMs and Categories: ", e);
                                // If song query fails, BPM and Mode both fail
                                tvAvgBpm.setText("Error");
                                tvTopMode.setText("Error");
                                // Display error message to user
                                Toast.makeText(AnalyzeHistoryActivity.this, "Failed to load song BPM/Mode information. Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load history: ", e);
                    displayMetrics("Error", "Error", "Error");

                    // Display the specific error message to the user
                    String errorMsg = "Failed to load history data. Error: " + (e.getMessage() != null ? e.getMessage() : "Unknown error. Please check the Firestore composite index.");
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Calculates the average BPM based on the user's play history and corresponding song BPMs.
     */
    // yuchen... Helper method to calculate average BPM (Confirmed: uses song's BPM for average)
    private void calculateAndDisplayAvgBPM(List<DocumentSnapshot> historyDocs, Map<String, Integer> songIdToBpm) {
        int totalBPM = 0;
        int validBpmCount = 0;

        for (DocumentSnapshot historyDoc : historyDocs) {
            String songId = historyDoc.getString("songId");
            // Retrieve BPM using the songId from the map. Defaults to 0 if the BPM was not found/queried.
            int bpm = songIdToBpm.getOrDefault(songId, 0);

            Log.d(TAG, "Processing history: Song ID=" + songId + " (History), Found BPM=" + bpm);

            // Only count songs with a valid BPM (> 0) for the average calculation.
            if (bpm > 0) {
                totalBPM += bpm;
                validBpmCount++;
            }
        }

        Log.d(TAG, "Total BPM Sum: " + totalBPM + ", Valid Count: " + validBpmCount);

        if (validBpmCount > 0) {
            double averageBpm = (double) totalBPM / validBpmCount;
            // Display with one decimal place
            tvAvgBpm.setText(String.format(Locale.US, "%.1f BPM", averageBpm));
            Log.d(TAG, "Calculated Average BPM: " + String.format(Locale.US, "%.1f", averageBpm));
        } else {
            // This is the source of "N/A" if no valid BPMs (> 0) were found.
            tvAvgBpm.setText("N/A");
            Log.w(TAG, "Valid BPM count is zero. Displaying N/A.");
        }
    }

    /**
     * Calculates and displays the most frequent 'mode' from the history records,
     * by looking up the song's 'category' (mode) using the provided map.
     */
    private void calculateAndDisplayTopMode(List<DocumentSnapshot> historyDocs,
                                            Map<String, List<String>> songIdToTags) {

        Map<String, Integer> tagCounts = new HashMap<>();
        String topTag = "N/A";
        int maxCount = 0;

        for (DocumentSnapshot historyDoc : historyDocs) {
            String songId = historyDoc.getString("songId");

            List<String> tags = songIdToTags.get(songId);
            if (tags == null) continue;

            for (String tag : tags) {
                if (tag == null || tag.isEmpty()) continue;

                tagCounts.put(tag, tagCounts.getOrDefault(tag, 0) + 1);
            }
        }

        // Find the most frequent tag
        for (Map.Entry<String, Integer> entry : tagCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                topTag = entry.getKey();
            }
        }

        Log.d(TAG, "Top Tag = " + topTag + " (count " + maxCount + ")");
        tvTopMode.setText(topTag);
    }

    private void displayMetrics(String totalTime, String avgBpm, String topMode) {
        // Renamed totalTime parameter usage to reflect Total Plays
        tvTotalTime.setText(totalTime);
        tvAvgBpm.setText(avgBpm);
        tvTopMode.setText(topMode);
    }
}