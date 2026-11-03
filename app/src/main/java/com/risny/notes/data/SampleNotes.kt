package com.risny.notes.data

/** A few friendly notes shown on the very first launch, so the app never opens to a blank page. */
object SampleNotes {
    fun create(now: Long = System.currentTimeMillis()): List<Note> {
        val minute = 60_000L
        return listOf(
            Note(
                title = "Welcome to Inkwell",
                content = "A calm place for quick thoughts.\n\n" +
                    "• Tap New note to write\n" +
                    "• Long-press a card to pin it to the top\n" +
                    "• Swipe a card sideways to delete it (Undo is one tap away)\n" +
                    "• Search finds words in titles and text\n\n" +
                    "Everything saves by itself.",
                color = NoteColor.LAVENDER.ordinal,
                pinned = true,
                createdAt = now - 3 * minute,
                updatedAt = now - 3 * minute,
            ),
            Note(
                title = "Groceries",
                content = "Red rice\nCurry leaves\nCoconut milk\nEggs\nTea",
                color = NoteColor.MINT.ordinal,
                createdAt = now - 90 * minute,
                updatedAt = now - 90 * minute,
            ),
            Note(
                title = "",
                content = "Small steps every day.\nProgress, not perfection.",
                color = NoteColor.SUN.ordinal,
                createdAt = now - 26 * 60 * minute,
                updatedAt = now - 26 * 60 * minute,
            ),
            Note(
                title = "App ideas",
                content = "A study timer that blocks distractions.\n" +
                    "Shared shopping list for the family.\n" +
                    "Budget tracker with monthly charts.",
                color = NoteColor.SKY.ordinal,
                createdAt = now - 4 * 24 * 60 * minute,
                updatedAt = now - 4 * 24 * 60 * minute,
            ),
        )
    }
}
