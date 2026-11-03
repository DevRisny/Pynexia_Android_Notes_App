package com.risny.notes.data

import java.util.Locale
import java.util.concurrent.TimeUnit

/** Note colours: a light card background and a stronger accent for chips and dots. */
enum class NoteColor(val background: Long, val accent: Long, val label: String) {
    DEFAULT(0xFFFFFFFF, 0xFF94A3B8, "White"),
    LAVENDER(0xFFEFEAFF, 0xFF7C5CFF, "Lavender"),
    SKY(0xFFE6F0FF, 0xFF2563FF, "Sky"),
    MINT(0xFFDDF7EC, 0xFF10B981, "Mint"),
    SUN(0xFFFFF4D6, 0xFFF59E0B, "Sun"),
    PEACH(0xFFFFE8DE, 0xFFFF7A59, "Peach"),
    ROSE(0xFFFFE4EC, 0xFFFF5C8A, "Rose");

    companion object {
        fun of(index: Int): NoteColor = entries.getOrElse(index) { DEFAULT }
    }
}

/** Pure text helpers, kept free of Android classes so they can be unit tested on the JVM. */
object NoteText {

    /** The title, or the first line of the content when there is no title. */
    fun displayTitle(note: Note, untitled: String = "Untitled"): String =
        note.title.trim().ifEmpty { note.content.trim().lineSequence().firstOrNull()?.take(60)?.trim().orEmpty() }.ifEmpty { untitled }

    /** Up to [maxChars] of the content on one line, skipping the line already used as the title. */
    fun preview(note: Note, maxChars: Int = 140): String {
        val lines = note.content.trim().lines().map { it.trim() }.filter { it.isNotEmpty() }
        val body = if (note.title.isBlank()) lines.drop(1) else lines
        val text = body.joinToString(" ")
        return if (text.length <= maxChars) text else text.take(maxChars).trimEnd() + "…"
    }

    fun wordCount(text: String): Int = text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }

    /** Case-insensitive search over title and content; every word must match. */
    fun matches(note: Note, query: String): Boolean {
        val words = query.trim().lowercase(Locale.ROOT).split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return true
        val hay = (note.title + " " + note.content).lowercase(Locale.ROOT)
        return words.all { hay.contains(it) }
    }

    /** True when the editor holds something different from what is saved (timestamps ignored). */
    fun hasChanges(saved: Note?, edited: Note): Boolean =
        if (saved == null) !edited.isBlank
        else saved.title != edited.title || saved.content != edited.content ||
            saved.color != edited.color || saved.pinned != edited.pinned

    /** Pinned first, then newest edit first (the same order as the database query). */
    val ORDER: Comparator<Note> = compareByDescending<Note> { it.pinned }.thenByDescending { it.updatedAt }

    /** "Just now", "5 min ago", "3 h ago", "Yesterday", "4 days ago", or null for older dates. */
    fun relativeTime(then: Long, now: Long = System.currentTimeMillis()): String? {
        val diff = (now - then).coerceAtLeast(0)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours h ago"
            days < 2 -> "Yesterday"
            days < 7 -> "$days days ago"
            else -> null
        }
    }
}
