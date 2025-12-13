package edu.northeastern.group13project;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.SetOptions;


import java.util.Collections;
import java.util.List;
import android.widget.Toast;
import android.app.AlertDialog;
import android.widget.EditText;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.ViewHolder> {

    public interface OnTrackClickListener {
        void onTrackClick(Track track);
    }

    private final List<Track> tracks;
    private final Context context;
    private final OnTrackClickListener listener;

    public TrackAdapter(Context context, List<Track> tracks, OnTrackClickListener listener) {
        this.context = context;
        this.tracks = tracks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_track, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Track t = tracks.get(position);
        holder.title.setText(t.getTitle());
        // Display Song name and BPM
        holder.bpm.setText(t.getBpm() + " bpm");
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTrackClick(t);
            } else {
                // fallback to previous behavior: start MainActivity
                Intent intent = new Intent(context, MainActivity.class);
                intent.putExtra("track_title", t.getTitle());
                intent.putExtra("track_url", t.getUrl());
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                context.startActivity(intent);
            }
        });

        //maybe no play button?
        holder.playIcon.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTrackClick(t);
            }
        });

        holder.moreIcon.setOnClickListener(v -> {
            int adapterPos = holder.getBindingAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION) return;

            PopupMenu popupMenu = new PopupMenu(context, holder.moreIcon);
            popupMenu.getMenu().add("Edit BPM");
            popupMenu.getMenu().add("Delete");


            popupMenu.setOnMenuItemClickListener(item -> {
                String selected = item.getTitle().toString();

                switch (selected) {
                    case "Edit BPM":
                        showEditBpmDialog(t, adapterPos);
                        break;

                    case "Delete":
                        showDeleteConfirmDialog(t);
                        break;
                }

                return true;
            });

            popupMenu.show();
        });
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView bpm;
        ImageView playIcon;
        ImageView moreIcon;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_item_title);
            bpm   = itemView.findViewById(R.id.tv_item_bpm);
            playIcon = itemView.findViewById(R.id.iv_play);
            moreIcon = itemView.findViewById(R.id.iv_more);
        }
    }

    // pop up to edit bpm, then update to firebase the updated bpm
    private void showEditBpmDialog(Track track, int adapterPosition) {
        EditText input = new EditText(context);
        input.setHint("Enter new BPM");

        if (track.getBpm() > 0) {
            input.setText(String.valueOf(track.getBpm()));
        }

        new AlertDialog.Builder(context)
                .setTitle("Edit BPM")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String bpmStr = input.getText().toString().trim();
                    if (bpmStr.isEmpty()) return;

                    int newBpm;
                    try {
                        newBpm = Integer.parseInt(bpmStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(context, "Invalid BPM", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // get current user
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) {
                        Toast.makeText(context, "Please log in to save BPM", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String uid = user.getUid();

                    // users/{uid}/songSettings/{songId}/bpm
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .collection("songSettings")
                            .document(track.getId())
                            .set(Collections.singletonMap("bpm", newBpm), SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {

                                // Update local model
                                track.setBpm(newBpm);

                                // Update UI: refresh only this row
                                if (adapterPosition != RecyclerView.NO_POSITION) {
                                    notifyItemChanged(adapterPosition);
                                } else {
                                    notifyDataSetChanged();
                                }

                                Toast.makeText(context, "BPM updated", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(context, "Failed to update BPM", Toast.LENGTH_SHORT).show()
                            );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // remove the song from firebase
    private void deleteSongFromFirebase(Track track) {
        FirebaseFirestore.getInstance()
                .collection("songs")
                .document(track.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    tracks.remove(track);
                    notifyDataSetChanged();      // update UI
                    Toast.makeText(context, "Song deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show()
                );
    }

    private void showDeleteConfirmDialog(Track track) {
        new AlertDialog.Builder(context)
                .setTitle("Delete this song?")
                .setMessage("This will permanently remove \"" + track.getTitle() + "\" from the library. Are you sure?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteSongFromFirebase(track);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }
}
