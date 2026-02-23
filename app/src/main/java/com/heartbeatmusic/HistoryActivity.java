package com.heartbeatmusic;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import com.heartbeatmusic.data.model.HistoryItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class HistoryActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private TextView tvAverageBpm;
    private TextView tvTotalPlays;
    private RecyclerView rvHistoryList;

    private List<HistoryItem> historyItemList = new ArrayList<>();
    private HistoryAdapter historyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbar_history);

        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Playback History");
        }

        tvAverageBpm = findViewById(R.id.tv_average_bpm);
        tvTotalPlays = findViewById(R.id.tv_total_plays);
        rvHistoryList = findViewById(R.id.rv_history_list);

        rvHistoryList.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter(this, historyItemList);
        rvHistoryList.setAdapter(historyAdapter);

        loadUserHistory();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadUserHistory() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (userId == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("history")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        tvTotalPlays.setText("0");
                        tvAverageBpm.setText("N/A");
                        Toast.makeText(this, "No history records found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Set<String> uniqueSongIds = queryDocumentSnapshots.getDocuments().stream()
                            .map(doc -> doc.getString("songId"))
                            .filter(id -> id != null && !id.isEmpty())
                            .collect(Collectors.toSet());

                    tvTotalPlays.setText(String.valueOf(queryDocumentSnapshots.size()));

                    if (uniqueSongIds.isEmpty()) {
                        Toast.makeText(this, "No valid songs found in history.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> songIdsToQuery = new ArrayList<>(uniqueSongIds).subList(0, Math.min(uniqueSongIds.size(), 10));

                    db.collection("songs")
                            .whereIn("id", songIdsToQuery)
                            .get()
                            .addOnSuccessListener(songSnapshots -> {
                                Map<String, Integer> songIdToBpm = new HashMap<>();
                                for (DocumentSnapshot doc : songSnapshots.getDocuments()) {
                                    Integer bpm = doc.getLong("bpm") != null ? doc.getLong("bpm").intValue() : 0;
                                    String songId = doc.getString("id");
                                    if (songId == null) {
                                        songId = doc.getId();
                                    }
                                    songIdToBpm.put(songId, bpm);
                                }

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

    private void combineDataAndCalculateAvgBPM(List<DocumentSnapshot> historyDocs, Map<String, Integer> songIdToBpm) {
        historyItemList.clear();
        int totalBPM = 0;
        int validBpmCount = 0;

        for (DocumentSnapshot historyDoc : historyDocs) {
            String songId = historyDoc.getString("songId");
            String title = historyDoc.getString("title");
            long timestamp = historyDoc.getLong("timestamp") != null ? historyDoc.getLong("timestamp") : 0;

            int bpm = songIdToBpm.getOrDefault(songId, 0);

            historyItemList.add(new HistoryItem(title, timestamp, bpm));

            if (bpm > 0) {
                totalBPM += bpm;
                validBpmCount++;
            }
        }

        historyAdapter.notifyDataSetChanged();

        if (validBpmCount > 0) {
            double averageBpm = (double) totalBPM / validBpmCount;
            tvAverageBpm.setText(String.format("%.1f", averageBpm));
        } else {
            tvAverageBpm.setText("N/A");
        }
    }
}
