package edu.northeastern.group13project.data.remote;

import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class FirebaseStorageManager {

    private final FirebaseStorage storage;
    private final StorageReference rootRef;

    public FirebaseStorageManager() {
        // default bucket from google-services.json
        storage = FirebaseStorage.getInstance();
        rootRef = storage.getReference();
    }

    public void getSongUrl(String path,
                           OnSuccessListener<Uri> onSuccess,
                           OnFailureListener onFailure) {

        StorageReference songRef = rootRef.child(path);

        songRef.getDownloadUrl()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }
}