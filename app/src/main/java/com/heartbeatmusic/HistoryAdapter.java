package com.heartbeatmusic;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.heartbeatmusic.data.model.HistoryItem;

import java.util.List;

/**
 * Adapter for the RecyclerView in HistoryActivity.
 * Displays each played song's title, playback time, and BPM.
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final Context context;
    private final List<HistoryItem> historyList;

    public HistoryAdapter(Context context, List<HistoryItem> historyList) {
        this.context = context;
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryItem item = historyList.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvTimestamp.setText(item.getFormattedTime());
        holder.tvBpm.setText(String.valueOf(item.getBpm()));
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvTimestamp;
        final TextView tvBpm;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_item_title);
            tvTimestamp = itemView.findViewById(R.id.tv_item_timestamp);
            tvBpm = itemView.findViewById(R.id.tv_item_bpm);
        }
    }
}
