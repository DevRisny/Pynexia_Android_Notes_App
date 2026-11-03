package com.risny.notes.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.risny.notes.data.Note
import com.risny.notes.data.NoteDatabase
import com.risny.notes.data.NoteText
import com.risny.notes.data.SampleNotes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the list screen shows, in one immutable snapshot. */
data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val total: Int = 0,
    val query: String = "",
    val loaded: Boolean = false,
)

class NotesViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = NoteDatabase.get(app).noteDao()
    private val query = MutableStateFlow("")

    /** The database stream and the search box, combined. Room re-emits on every change. */
    val state: StateFlow<NotesUiState> =
        combine(dao.observeAll(), query) { all, q ->
            NotesUiState(
                notes = all.filter { NoteText.matches(it, q) },
                total = all.size,
                query = q,
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotesUiState())

    init {
        seedOnFirstLaunch()
    }

    fun setQuery(text: String) {
        query.value = text
    }

    fun togglePin(note: Note) = viewModelScope.launch {
        dao.setPinned(note.id, !note.pinned, System.currentTimeMillis())
    }

    fun delete(note: Note) = viewModelScope.launch { dao.delete(note) }

    /** Puts a deleted note back exactly as it was, with the same id and timestamps. */
    fun restore(note: Note) = viewModelScope.launch { dao.insert(note) }

    /** Deletes by id (used when the editor asks for a delete) and returns the note for Undo. */
    suspend fun deleteById(id: Long): Note? {
        val note = dao.findById(id) ?: return null
        dao.delete(note)
        return note
    }

    private fun seedOnFirstLaunch() = viewModelScope.launch {
        val prefs = getApplication<Application>().getSharedPreferences("inkwell", Context.MODE_PRIVATE)
        if (prefs.getBoolean("seeded", false)) return@launch
        if (dao.count() == 0) SampleNotes.create().forEach { dao.insert(it) }
        prefs.edit().putBoolean("seeded", true).apply()
    }
}
