package com.heartbeatmusic.terminal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.heartbeatmusic.data.model.CollectionItem
import com.heartbeatmusic.data.model.SyncSession
import com.heartbeatmusic.data.remote.ArchiveRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ArchiveViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ArchiveRepository()

    private val pendingDeletion = mutableSetOf<String>()
    private val pendingCollectionDeletion = mutableSetOf<String>()

    private val _sessions = MutableStateFlow<List<SyncSession>>(emptyList())
    val sessions: StateFlow<List<SyncSession>> = _sessions.asStateFlow()

    private val _restoreTokens = MutableStateFlow<Map<String, Int>>(emptyMap())
    val restoreTokens: StateFlow<Map<String, Int>> = _restoreTokens.asStateFlow()

    private val _collectionRestoreTokens = MutableStateFlow<Map<String, Int>>(emptyMap())
    val collectionRestoreTokens: StateFlow<Map<String, Int>> = _collectionRestoreTokens.asStateFlow()

    private val _collection = MutableStateFlow<List<CollectionItem>>(emptyList())
    val collection: StateFlow<List<CollectionItem>> = _collection.asStateFlow()

    init {
        repository.sessionsFlow()
            .onEach { fromDb ->
                _sessions.value = fromDb.filter { it.id !in pendingDeletion }
            }
            .launchIn(viewModelScope)
        repository.collectionFlow()
            .onEach { fromDb ->
                _collection.value = fromDb.filter { it.id !in pendingCollectionDeletion }
            }
            .launchIn(viewModelScope)
    }

    fun removeSessionFromUI(sessionId: String) {
        pendingDeletion.add(sessionId)
        _sessions.value = _sessions.value.filter { it.id != sessionId }
    }

    fun restoreSession(session: SyncSession) {
        pendingDeletion.remove(session.id)
        val version = (_restoreTokens.value[session.id] ?: 0) + 1
        _restoreTokens.value = _restoreTokens.value + (session.id to version)
        val current = _sessions.value.toMutableList()
        if (current.none { it.id == session.id }) {
            current.add(session)
            _sessions.value = current.sortedByDescending { it.endTimestamp }
        }
    }

    fun deleteFromDb(sessionId: String) {
        pendingDeletion.remove(sessionId)
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSession(sessionId)
        }
    }

    fun addToCollection(songId: String, title: String, artist: String, mode: String, coverUrl: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addToCollection(songId, title, artist, mode, coverUrl)
        }
    }

    fun removeFromCollection(songId: String, mode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeFromCollection(songId, mode)
        }
    }

    /** Soft delete: remove from UI, show Snackbar. Call restoreCollectionItem for Undo, deleteCollectionFromDb on dismiss. */
    fun removeCollectionFromUI(item: CollectionItem) {
        pendingCollectionDeletion.add(item.id)
        _collection.value = _collection.value.filter { it.id != item.id }
    }

    fun restoreCollectionItem(item: CollectionItem) {
        pendingCollectionDeletion.remove(item.id)
        val version = (_collectionRestoreTokens.value[item.id] ?: 0) + 1
        _collectionRestoreTokens.value = _collectionRestoreTokens.value + (item.id to version)
        val current = _collection.value.toMutableList()
        if (current.none { it.id == item.id }) {
            current.add(item)
            _collection.value = current
        }
    }

    fun deleteCollectionFromDb(item: CollectionItem) {
        pendingCollectionDeletion.remove(item.id)
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeFromCollection(item.songId, item.mode)
        }
    }
}
