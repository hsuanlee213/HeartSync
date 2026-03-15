package com.heartbeatmusic

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class TrackAdapter(
    private val context: Context,
    private val tracks: MutableList<Track>,
    private val listener: OnTrackClickListener?
) : RecyclerView.Adapter<TrackAdapter.ViewHolder>() {

    interface OnTrackClickListener {
        fun onTrackClick(track: Track)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_track, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val t = tracks[position]
        holder.title.text = t.title
        holder.bpm.text = "${t.bpm} bpm"

        holder.itemView.setOnClickListener {
            if (listener != null) {
                listener.onTrackClick(t)
            } else {
                val intent = Intent(context, MainActivity::class.java).apply {
                    putExtra("track_title", t.title)
                    putExtra("track_url", t.url)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                context.startActivity(intent)
            }
        }

        holder.playIcon.setOnClickListener {
            listener?.onTrackClick(t)
        }

        holder.moreIcon.setOnClickListener { v ->
            val adapterPos = holder.bindingAdapterPosition
            if (adapterPos == RecyclerView.NO_POSITION) return@setOnClickListener

            PopupMenu(context, holder.moreIcon).apply {
                menu.add("Edit BPM")
                menu.add("Delete")
                setOnMenuItemClickListener { item ->
                    when (item.title.toString()) {
                        "Edit BPM" -> showEditBpmDialog(t, adapterPos)
                        "Delete" -> showDeleteConfirmDialog(t)
                    }
                    true
                }
                show()
            }
        }
    }

    override fun getItemCount(): Int = tracks.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tv_item_title)
        val bpm: TextView = itemView.findViewById(R.id.tv_item_bpm)
        val playIcon: ImageView = itemView.findViewById(R.id.iv_play)
        val moreIcon: ImageView = itemView.findViewById(R.id.iv_more)
    }

    private fun showEditBpmDialog(track: Track, adapterPosition: Int) {
        val input = EditText(context).apply {
            hint = "Enter new BPM"
            if (track.bpm > 0) setText(track.bpm.toString())
        }

        AlertDialog.Builder(context)
            .setTitle("Edit BPM")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val bpmStr = input.text.toString().trim()
                if (bpmStr.isEmpty()) return@setPositiveButton
                val newBpm = bpmStr.toIntOrNull() ?: run {
                    Toast.makeText(context, "Invalid BPM", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val user = FirebaseAuth.getInstance().currentUser ?: run {
                    Toast.makeText(context, "Please log in to save BPM", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                FirebaseFirestore.getInstance()
                    .collection("users").document(user.uid)
                    .collection("songSettings").document(track.id)
                    .set(mapOf("bpm" to newBpm), SetOptions.merge())
                    .addOnSuccessListener {
                        track.bpm = newBpm
                        if (adapterPosition != RecyclerView.NO_POSITION) {
                            notifyItemChanged(adapterPosition)
                        } else {
                            notifyDataSetChanged()
                        }
                        Toast.makeText(context, "BPM updated", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to update BPM", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSongFromFirebase(track: Track) {
        FirebaseFirestore.getInstance()
            .collection("songs").document(track.id)
            .delete()
            .addOnSuccessListener {
                tracks.remove(track)
                notifyDataSetChanged()
                Toast.makeText(context, "Song deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteConfirmDialog(track: Track) {
        AlertDialog.Builder(context)
            .setTitle("Delete this song?")
            .setMessage("This will permanently remove \"${track.title}\" from the library. Are you sure?")
            .setPositiveButton("Delete") { _, _ -> deleteSongFromFirebase(track) }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
