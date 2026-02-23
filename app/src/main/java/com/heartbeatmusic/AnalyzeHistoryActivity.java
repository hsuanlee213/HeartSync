package com.heartbeatmusic;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.android.material.appbar.MaterialToolbar;

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
public class AnalyzeHistoryActivity extends AppCompatActivity {

    private static final String TAG = "AnalyzeHistoryActivity";
    private TextView tvTotalTime;
    private TextView tvAvgBpm;
    private TextView tvTopMode;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_analyze_history);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_history);

        if (toolbar != null) {
            setSupportActionBar(toolbar);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Listening Analysis");

                try {
                    toolbar.setNavigationIconTint(ContextCompat.getColor(this, android.R.color.white));
                } catch (Exception e) {
                    Log.e(TAG, "Failed to set navigation icon tint.", e);
                }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.analyze_history), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        tvTotalTime = findViewById(R.id.tv_total_time);
        tvAvgBpm = findViewById(R.id.tv_avg_bpm);
        tvTopMode = findViewById(R.id.tv_top_mode);

        loadUserMetrics();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadUserMetrics() {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Log.w(TAG, "No authenticated user found. Showing default metrics.");
            displayMetrics("0 Plays", "0 BPM", "N/A");
            Toast.makeText(this, "Please log in to view personalized analysis.", Toast.LENGTH_LONG).show();
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Authenticated User ID: " + userId);

        db.collection("history")
                .whereEqualTo("userId", userId)
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

                    int totalPlays = historyDocs.size();
                    tvTotalTime.setText(totalPlays + " Plays");

                    Set<String> uniqueSongIds = historyDocs.stream()
                            .map(doc -> doc.getString("songId"))
                            .filter(id -> id != null && !id.isEmpty())
                            .collect(Collectors.toSet());

                    Log.d(TAG, "Total unique song IDs to lookup: " + uniqueSongIds.size());

                    if (uniqueSongIds.isEmpty()) {
                        tvAvgBpm.setText("N/A");
                        tvTopMode.setText("N/A");
                        Log.w(TAG, "No valid songIds found in history documents. Cannot calculate Avg BPM or Top Mode.");
                        return;
                    }

                    List<String> songIdsList = new ArrayList<>(uniqueSongIds);
                    List<String> queryBatch = songIdsList.subList(0, Math.min(songIdsList.size(), 10));

                    if (songIdsList.size() > 10) {
                        Toast.makeText(AnalyzeHistoryActivity.this,
                                "Note: Due to Firestore query limits, only the first 10 unique songs' BPM and Mode are analyzed.",
                                Toast.LENGTH_LONG).show();
                    }

                    Log.d(TAG, "Querying BPMs and Categories for song Document IDs (limited to 10): " + queryBatch.toString());

                    db.collection("songs")
                            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), queryBatch)
                            .get()
                            .addOnSuccessListener(songSnapshots -> {
                                Log.d(TAG, "Songs query successful. BPM/Mode documents found: " + songSnapshots.size());
                                Map<String, Integer> songIdToBpm = new HashMap<>();
                                Map<String, List<String>> songIdToTags = new HashMap<>();

                                for (DocumentSnapshot doc : songSnapshots.getDocuments()) {
                                    Long bpmLong = doc.getLong("bpm");
                                    Integer bpm = (bpmLong != null) ? bpmLong.intValue() : 0;

                                    String songId = doc.getId();

                                    List<String> tags = (List<String>) doc.get("tags");
                                    if (tags == null) tags = new ArrayList<>();
                                    songIdToTags.put(songId, tags);

                                    songIdToBpm.put(songId, bpm);
                                    Log.d(TAG, "Mapped Song ID: " + songId + " to BPM: " + bpm);
                                }

                                calculateAndDisplayAvgBPM(historyDocs, songIdToBpm);
                                calculateAndDisplayTopMode(historyDocs, songIdToTags);

                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to load song BPMs and Categories: ", e);
                                tvAvgBpm.setText("Error");
                                tvTopMode.setText("Error");
                                Toast.makeText(AnalyzeHistoryActivity.this, "Failed to load song BPM/Mode information. Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load history: ", e);
                    displayMetrics("Error", "Error", "Error");

                    String errorMsg = "Failed to load history data. Error: " + (e.getMessage() != null ? e.getMessage() : "Unknown error. Please check the Firestore composite index.");
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                });
    }

    private void calculateAndDisplayAvgBPM(List<DocumentSnapshot> historyDocs, Map<String, Integer> songIdToBpm) {
        int totalBPM = 0;
        int validBpmCount = 0;

        for (DocumentSnapshot historyDoc : historyDocs) {
            String songId = historyDoc.getString("songId");
            int bpm = songIdToBpm.getOrDefault(songId, 0);

            Log.d(TAG, "Processing history: Song ID=" + songId + " (History), Found BPM=" + bpm);

            if (bpm > 0) {
                totalBPM += bpm;
                validBpmCount++;
            }
        }

        Log.d(TAG, "Total BPM Sum: " + totalBPM + ", Valid Count: " + validBpmCount);

        if (validBpmCount > 0) {
            double averageBpm = (double) totalBPM / validBpmCount;
            tvAvgBpm.setText(String.format(Locale.US, "%.1f BPM", averageBpm));
            Log.d(TAG, "Calculated Average BPM: " + String.format(Locale.US, "%.1f", averageBpm));
        } else {
            tvAvgBpm.setText("N/A");
            Log.w(TAG, "Valid BPM count is zero. Displaying N/A.");
        }
    }

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
        tvTotalTime.setText(totalTime);
        tvAvgBpm.setText(avgBpm);
        tvTopMode.setText(topMode);
    }
}
