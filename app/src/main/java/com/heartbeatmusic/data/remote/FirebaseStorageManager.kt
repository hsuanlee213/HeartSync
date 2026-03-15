package com.heartbeatmusic.data.remote

import android.net.Uri
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.storage.FirebaseStorage

class FirebaseStorageManager {

    private val rootRef = FirebaseStorage.getInstance().reference

    fun getSongUrl(
        path: String,
        onSuccess: OnSuccessListener<Uri>,
        onFailure: OnFailureListener
    ) {
        rootRef.child(path)
            .downloadUrl
            .addOnSuccessListener(onSuccess)
            .addOnFailureListener(onFailure)
    }
}
