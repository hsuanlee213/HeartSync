package com.heartbeatmusic

import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.heartbeatmusic.data.model.HistoryItem
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HistoryActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var tvAverageBpm: TextView
    private lateinit var tvTotalPlays: TextView
    private lateinit var rvHistoryList: RecyclerView

    private val historyItemList = mutableListOf<HistoryItem>()
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_history)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Playback History"
        }

        tvAverageBpm = findViewById(R.id.tv_average_bpm)
        tvTotalPlays = findViewById(R.id.tv_total_plays)
        rvHistoryList = findViewById(R.id.rv_history_list)

        historyAdapter = HistoryAdapter(this, historyItemList)
        rvHistoryList.layoutManager = LinearLayoutManager(this)
        rvHistoryList.adapter = historyAdapter

        loadUserHistory()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }

    private fun loadUserHistory() {
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("history")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { queryDocumentSnapshots ->
                if (queryDocumentSnapshots.isEmpty) {
                    tvTotalPlays.text = "0"
                    tvAverageBpm.text = "N/A"
                    Toast.makeText(this, "No history records found.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val uniqueSongIds = queryDocumentSnapshots.documents
                    .mapNotNull { it.getString("songId") }
                    .filter { it.isNotEmpty() }
                    .toSet()

                tvTotalPlays.text = queryDocumentSnapshots.size().toString()

                if (uniqueSongIds.isEmpty()) {
                    Toast.makeText(this, "No valid songs found in history.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val songIdsToQuery = uniqueSongIds.take(10).toList()

                db.collection("songs")
                    .whereIn("id", songIdsToQuery)
                    .get()
                    .addOnSuccessListener { songSnapshots ->
                        val songIdToBpm = songSnapshots.documents.associate { doc ->
                            val songId = doc.getString("id") ?: doc.id
                            songId to (doc.getLong("bpm")?.toInt() ?: 0)
                        }
                        combineDataAndCalculateAvgBpm(queryDocumentSnapshots.documents, songIdToBpm)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to load song BPMs: ${e.message}", Toast.LENGTH_LONG).show()
                        tvAverageBpm.text = "Error"
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load history: ${e.message}", Toast.LENGTH_LONG).show()
                tvAverageBpm.text = "Error"
            }
    }

    private fun combineDataAndCalculateAvgBpm(
        historyDocs: List<DocumentSnapshot>,
        songIdToBpm: Map<String, Int>
    ) {
        historyItemList.clear()
        var totalBpm = 0
        var validBpmCount = 0

        for (doc in historyDocs) {
            val songId = doc.getString("songId") ?: ""
            val title = doc.getString("title") ?: ""
            val timestamp = doc.getLong("timestamp") ?: 0L
            val bpm = songIdToBpm.getOrDefault(songId, 0)

            historyItemList.add(HistoryItem(title, timestamp, bpm))
            if (bpm > 0) { totalBpm += bpm; validBpmCount++ }
        }

        historyAdapter.notifyDataSetChanged()
        tvAverageBpm.text = if (validBpmCount > 0)
            "%.1f".format(totalBpm.toDouble() / validBpmCount)
        else "N/A"
    }
}
