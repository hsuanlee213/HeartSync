package edu.northeastern.group13project;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import edu.northeastern.group13project.data.model.HistoryItem;

import java.util.List;

/**
 * Adapter for the RecyclerView in HistoryActivity.
 * Displays each played song's title, playback time, and BPM.
 */
// yuchen... HistoryAdapter implementation
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
        // Inflate the item layout
        View view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryItem item = historyList.get(position);

        // Set song title
        holder.tvTitle.setText(item.getTitle());

        // Set formatted playback time (using the helper method in HistoryItem)
        holder.tvTimestamp.setText(item.getFormattedTime());

        // Set BPM value
        holder.tvBpm.setText(String.valueOf(item.getBpm()));
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    /**
     * ViewHolder for the history item.
     */
    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvTimestamp;
        final TextView tvBpm;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            // Bind views from item_history.xml
            tvTitle = itemView.findViewById(R.id.tv_item_title);
            tvTimestamp = itemView.findViewById(R.id.tv_item_timestamp);
            tvBpm = itemView.findViewById(R.id.tv_item_bpm);
        }
    }
}