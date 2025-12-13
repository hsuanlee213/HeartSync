package edu.northeastern.group13project.data.remote;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.northeastern.group13project.data.model.ActivityMode;
import edu.northeastern.group13project.data.model.Song;

public class LibraryRepository {

    private final FirebaseFirestore db;

    public LibraryRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // callback definition
    public interface SongsCallback {
        void onSuccess(List<Song> songs);
        void onError(Exception e);
    }

    public interface ActivityModesCallback {
        void onSuccess(List<ActivityMode> modes);
        void onError(Exception e);
    }

    /**  Read songs collection (with per-user BPM override) */
    public void getAllSongs(final SongsCallback callback) {
        db.collection(FirestoreCollections.SONGS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // 1. read default bpm
                    List<Song> baseSongs = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Song song = doc.toObject(Song.class);
                        if (song != null) {
                            song.setId(doc.getId());
                            baseSongs.add(song);
                        }
                    }

                    // check if user login in successfully
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) {
                        // if not, return default
                        callback.onSuccess(baseSongs);
                        return;
                    }

                    String uid = user.getUid();

                    // read user bpm setting and cover bpm data
                    db.collection("users")
                            .document(uid)
                            .collection("songSettings")
                            .get()
                            .addOnSuccessListener(settingsSnapshot -> {
                                // build songId -> bpm override map
                                Map<String, Integer> bpmOverrides = new HashMap<>();
                                for (DocumentSnapshot doc : settingsSnapshot.getDocuments()) {
                                    Number bpmNumber = (Number) doc.get("bpm");
                                    if (bpmNumber != null) {
                                        bpmOverrides.put(doc.getId(), bpmNumber.intValue());
                                    }
                                }

                                // user bpm -> songs
                                for (Song song : baseSongs) {
                                    Integer overrideBpm = bpmOverrides.get(song.getId());
                                    if (overrideBpm != null) {
                                        // TODO: check song.bpm data type
                                        song.setBpm((long) overrideBpm);
                                    }
                                }

                                // return merged songs
                                callback.onSuccess(baseSongs);
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getAllActivityModes(final ActivityModesCallback callback) {
        db.collection(FirestoreCollections.ACTIVITY_MODES)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot querySnapshot) {
                        List<ActivityMode> result = new ArrayList<>();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            ActivityMode mode = doc.toObject(ActivityMode.class);
                            if (mode != null) {
                                mode.setId(doc.getId());
                                result.add(mode);
                            }
                        }
                        callback.onSuccess(result);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onError(e);
                    }
                });
    }
}