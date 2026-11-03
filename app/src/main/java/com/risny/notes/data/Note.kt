package com.risny.notes.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A single note stored in the Room database. */
@Entity(tableName = "notes", indices = [Index("pinned"), Index("updated_at")])
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val content: String = "",
    /** Index into [NoteColor.entries]. */
    val color: Int = 0,
    val pinned: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
) {
    val isBlank: Boolean get() = title.isBlank() && content.isBlank()
}
