package edu.northeastern.group13project;

import android.os.Bundle;
import android.view.MenuItem; // <-- IMPORTED
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull; // <-- IMPORTED
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // <-- IMPORTED for MaterialToolbar compatibility
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import edu.northeastern.group13project.data.model.HistoryItem; // Ensure the correct package name is imported

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// yuchen... This activity handles displaying the user's music playback history
// and calculates the average BPM of all played songs.
public class HistoryActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private TextView tvAverageBpm;
    private TextView tvTotalPlays;
    private RecyclerView rvHistoryList;

    private List<HistoryItem> historyItemList = new ArrayList<>();
    private HistoryAdapter historyAdapter; // Assumed HistoryAdapter constructor

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history); // Assumes your layout file is named activity_history

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // UI Binding
        MaterialToolbar toolbar = findViewById(R.id.toolbar_history); // Assumed Toolbar ID

        // 1. Set the custom Toolbar as the Activity's ActionBar
        setSupportActionBar(toolbar);

        // 2. Enable the Up button (back arrow)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Playback History"); // Set a title for clarity
        }

        // NOTE: We remove the direct toolbar.setNavigationOnClickListener(v -> finish())
        // and handle it in onOptionsItemSelected below for standard Android Up Navigation behavior.

        tvAverageBpm = findViewById(R.id.tv_average_bpm);
        tvTotalPlays = findViewById(R.id.tv_total_plays);
        rvHistoryList = findViewById(R.id.rv_history_list);

        // Setup RecyclerView
        rvHistoryList.setLayoutManager(new LinearLayoutManager(this));
        // Ensure HistoryAdapter is correctly initialized (assuming constructor takes Context and List)
        // historyAdapter = new HistoryAdapter(this, historyItemList);
        // Placeholder initialization since HistoryAdapter definition is not provided:
        // You MUST replace 'null' with your actual HistoryAdapter initialization logic.
        historyAdapter = new HistoryAdapter(this, historyItemList);
        rvHistoryList.setAdapter(historyAdapter);

        // yuchen... Start loading and processing history data
        loadUserHistory();
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

    // yuchen... Method to fetch user history and song BPMs
    private void loadUserHistory() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (userId == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Step 1: Query the user's playback history records
        db.collection("history")
                .whereEqualTo("userId", userId)
                // Order by timestamp descending, showing the latest records first
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        tvTotalPlays.setText("0");
                        tvAverageBpm.setText("N/A");
                        Toast.makeText(this, "No history records found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Extract all unique songIds
                    Set<String> uniqueSongIds = queryDocumentSnapshots.getDocuments().stream()
                            .map(doc -> doc.getString("songId"))
                            .filter(id -> id != null && !id.isEmpty())
                            .collect(Collectors.toSet());

                    tvTotalPlays.setText(String.valueOf(queryDocumentSnapshots.size()));

                    if (uniqueSongIds.isEmpty()) {
                        Toast.makeText(this, "No valid songs found in history.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Step 2: Batch query BPM information for all relevant songs
                    // WARNING: Firestore whereIn limits to a maximum of 10 IDs.
                    // If more than 10 unique songs are played, the average BPM will only be calculated
                    // based on the first 10 queried songs.
                    List<String> songIdsToQuery = new ArrayList<>(uniqueSongIds).subList(0, Math.min(uniqueSongIds.size(), 10));

                    db.collection("songs")
                            .whereIn("id", songIdsToQuery)
                            .get()
                            .addOnSuccessListener(songSnapshots -> {
                                // Create a Map for fast lookup of songId to BPM
                                Map<String, Integer> songIdToBpm = new HashMap<>();
                                for (DocumentSnapshot doc : songSnapshots.getDocuments()) {
                                    // BPM is stored in the songs collection
                                    Integer bpm = doc.getLong("bpm") != null ? doc.getLong("bpm").intValue() : 0;
                                    // Assumes 'id' field in 'songs' matches the 'songId' field in 'history'
                                    String songId = doc.getString("id");
                                    if (songId == null) {
                                        songId = doc.getId(); // Fallback to Document ID if 'id' field is missing
                                    }
                                    songIdToBpm.put(songId, bpm);
                                }

                                // yuchen... Call method to combine data and calculate results
                                combineDataAndCalculateAvgBPM(queryDocumentSnapshots.getDocuments(), songIdToBpm);

                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to load song BPMs: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                tvAverageBpm.setText("Error");
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load history: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    tvAverageBpm.setText("Error");
                });
    }

    /**
     * Step 3: Combine data, calculate average BPM, and update the list
     * @param historyDocs List of history documents
     * @param songIdToBpm Map of songId to BPM
     */
    // yuchen... Method to process the fetched data (Confirmed: uses song's BPM for average)
    private void combineDataAndCalculateAvgBPM(List<DocumentSnapshot> historyDocs, Map<String, Integer> songIdToBpm) {
        historyItemList.clear();
        int totalBPM = 0;
        int validBpmCount = 0;

        for (DocumentSnapshot historyDoc : historyDocs) {
            String songId = historyDoc.getString("songId");
            String title = historyDoc.getString("title");
            long timestamp = historyDoc.getLong("timestamp") != null ? historyDoc.getLong("timestamp") : 0;

            // Retrieve BPM using the songId from the map. Defaults to 0 if the BPM was not found/queried.
            int bpm = songIdToBpm.getOrDefault(songId, 0);

            // Create HistoryItem and add it to the list
            historyItemList.add(new HistoryItem(title, timestamp, bpm));

            // Calculate average BPM: Only count songs with a valid BPM (> 0)
            if (bpm > 0) {
                totalBPM += bpm;
                validBpmCount++;
            }
        }

        // Update RecyclerView UI
        historyAdapter.notifyDataSetChanged();

        // Calculate and display average BPM
        if (validBpmCount > 0) {
            double averageBpm = (double) totalBPM / validBpmCount;
            // Display with one decimal place
            tvAverageBpm.setText(String.format("%.1f", averageBpm));
        } else {
            // This is the source of "N/A" if no valid BPMs (> 0) were found.
            tvAverageBpm.setText("N/A");
        }
    }
}