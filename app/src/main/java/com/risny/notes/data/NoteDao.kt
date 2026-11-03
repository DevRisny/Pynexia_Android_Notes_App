package com.risny.notes.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Database access. Every read is a Flow, so the UI updates by itself when data changes. */
@Dao
interface NoteDao {

    /** Pinned notes first, then the most recently edited. */
    @Query("SELECT * FROM notes ORDER BY pinned DESC, updated_at DESC")
    fun observeAll(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun findById(id: Long): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("UPDATE notes SET pinned = :pinned, updated_at = :now WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean, now: Long)

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun count(): Int
}
