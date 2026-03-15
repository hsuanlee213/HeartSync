package com.heartbeatmusic

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.heartbeatmusic.data.model.HistoryItem

/**
 * Adapter for the RecyclerView in HistoryActivity.
 * Displays each played song's title, playback time, and BPM.
 */
class HistoryAdapter(
    private val context: Context,
    private val historyList: List<HistoryItem>
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = historyList[position]
        holder.tvTitle.text = item.title
        holder.tvTimestamp.text = item.getFormattedTime()
        holder.tvBpm.text = item.bpm.toString()
    }

    override fun getItemCount(): Int = historyList.size

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tv_item_title)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tv_item_timestamp)
        val tvBpm: TextView = itemView.findViewById(R.id.tv_item_bpm)
    }
}
