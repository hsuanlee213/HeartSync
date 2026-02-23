package com.heartbeatmusic.data.remote;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import com.heartbeatmusic.data.model.ActivityMode;
import com.heartbeatmusic.data.model.Song;

public class MusicRepository {

    private final FirebaseFirestore db;

    public MusicRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public interface SongsCallback {
        void onSuccess(List<Song> songs);
        void onError(Exception e);
    }

    public interface ActivityModesCallback {
        void onSuccess(List<ActivityMode> modes);
        void onError(Exception e);
    }

    public void getAllSongs(final SongsCallback callback) {
        db.collection(FirestoreCollections.SONGS)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot querySnapshot) {
                        List<Song> result = new ArrayList<>();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Song song = doc.toObject(Song.class);
                            if (song != null) {
                                song.setId(doc.getId());
                                result.add(song);
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

    public void getSongsForMode(String modeTag, final SongsCallback callback) {
        db.collection(FirestoreCollections.SONGS)
                .whereArrayContains("tags", modeTag)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot querySnapshot) {
                        List<Song> result = new ArrayList<>();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Song song = doc.toObject(Song.class);
                            if (song != null) {
                                song.setId(doc.getId());
                                result.add(song);
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
