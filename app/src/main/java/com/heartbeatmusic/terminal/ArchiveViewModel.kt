package com.heartbeatmusic.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heartbeatmusic.data.model.CollectionItem
import com.heartbeatmusic.data.model.SyncSession
import com.heartbeatmusic.data.local.CollectionRepository
import com.heartbeatmusic.data.local.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val pendingDeletion: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val pendingCollectionDeletion: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private val _sessions = MutableStateFlow<List<SyncSession>>(emptyList())
    val sessions: StateFlow<List<SyncSession>> = _sessions.asStateFlow()

    private val _restoreTokens = MutableStateFlow<Map<String, Int>>(emptyMap())
    val restoreTokens: StateFlow<Map<String, Int>> = _restoreTokens.asStateFlow()

    private val _collection = MutableStateFlow<List<CollectionItem>>(emptyList())
    val collection: StateFlow<List<CollectionItem>> = _collection.asStateFlow()

    init {
        sessionRepository.sessionsFlow()
            .onEach { fromDb -> _sessions.value = fromDb.filter { it.id !in pendingDeletion } }
            .launchIn(viewModelScope)
        collectionRepository.collectionFlow()
            .onEach { list -> _collection.value = list.filter { it.id !in pendingCollectionDeletion } }
            .launchIn(viewModelScope)
        viewModelScope.launch(Dispatchers.IO) {
            collectionRepository.syncFromFirestore()
            sessionRepository.syncFromFirestore()
        }
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
            sessionRepository.deleteSession(sessionId)
        }
    }

    fun saveSession(session: SyncSession) {
        viewModelScope.launch(Dispatchers.IO) {
            sessionRepository.saveSession(session)
        }
    }

    fun addToCollection(songId: String, title: String, artist: String, mode: String, coverUrl: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            collectionRepository.addToCollection(songId, title, artist, mode, coverUrl)
        }
    }

    /** Hide item from UI immediately without touching DB — call before showing the UNDO snackbar. */
    fun removeFromCollectionUI(item: CollectionItem) {
        pendingCollectionDeletion.add(item.id)
        _collection.value = _collection.value.filter { it.id != item.id }
    }

    /** Restore item — call when user taps UNDO.
     *  Updates _collection.value immediately (synchronous, shows item at once) and
     *  re-inserts into Room in the background to keep the DB consistent. */
    fun restoreInCollection(item: CollectionItem) {
        pendingCollectionDeletion.remove(item.id)
        // Immediate UI update — CollectionContent observes this StateFlow directly,
        // so it recomposes as soon as this assignment runs on the main thread.
        val current = _collection.value.toMutableList()
        if (current.none { it.id == item.id }) {
            current.add(item)
            _collection.value = current.sortedByDescending { it.addedAt }
        }
        // Background Room + Firestore sync — pass original addedAt to preserve sort position.
        viewModelScope.launch(Dispatchers.IO) {
            collectionRepository.addToCollection(item.songId, item.title, item.artist, item.mode, item.coverUrl, item.addedAt)
        }
    }

    /** Persist deletion to DB + Firestore — call when UNDO snackbar is dismissed. */
    fun removeFromCollection(songId: String, mode: String) {
        val id = "${songId}_${mode}"
        pendingCollectionDeletion.remove(id)
        viewModelScope.launch(Dispatchers.IO) {
            collectionRepository.removeFromCollection(songId, mode)
        }
    }
}
