package com.risny.notes

import com.risny.notes.data.Note
import com.risny.notes.data.NoteColor
import com.risny.notes.data.NoteText
import com.risny.notes.data.SampleNotes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteTextTest {

    private val minute = 60_000L
    private val now = 1_800_000_000_000L

    // displayTitle

    @Test fun titleIsUsedWhenPresent() {
        assertEquals("Groceries", NoteText.displayTitle(Note(title = "  Groceries ", content = "Milk")))
    }

    @Test fun firstLineIsUsedWhenTitleIsEmpty() {
        assertEquals("Call the bank", NoteText.displayTitle(Note(content = "\n  Call the bank\nabout the card")))
    }

    @Test fun longFirstLineIsShortenedForTheTitle() {
        val title = NoteText.displayTitle(Note(content = "a".repeat(200)))
        assertEquals(60, title.length)
    }

    @Test fun emptyNoteGetsTheUntitledLabel() {
        assertEquals("Untitled", NoteText.displayTitle(Note()))
        assertEquals("Sans titre", NoteText.displayTitle(Note(content = "   "), "Sans titre"))
    }

    // preview

    @Test fun previewJoinsLinesAndSkipsBlankOnes() {
        assertEquals("Milk Eggs Tea", NoteText.preview(Note(title = "List", content = "Milk\n\nEggs\n  Tea  ")))
    }

    @Test fun previewSkipsTheLineAlreadyShownAsTitle() {
        assertEquals("second line", NoteText.preview(Note(content = "first line\nsecond line")))
    }

    @Test fun longPreviewIsCutWithAnEllipsis() {
        val text = NoteText.preview(Note(title = "T", content = "word ".repeat(100)), maxChars = 20)
        assertTrue(text.endsWith("…"))
        assertTrue(text.length <= 21)
    }

    // wordCount

    @Test fun wordsAreCountedAcrossSpacesAndLines() {
        assertEquals(0, NoteText.wordCount("   "))
        assertEquals(1, NoteText.wordCount("hello"))
        assertEquals(5, NoteText.wordCount("  one two\nthree\t four   five "))
    }

    // matches

    @Test fun searchIgnoresCaseAndChecksTitleAndContent() {
        val note = Note(title = "Trip to Kandy", content = "Book the TRAIN tickets")
        assertTrue(NoteText.matches(note, "kandy"))
        assertTrue(NoteText.matches(note, "train"))
        assertFalse(NoteText.matches(note, "bus"))
    }

    @Test fun everySearchWordMustMatch() {
        val note = Note(title = "Trip to Kandy", content = "Book the train tickets")
        assertTrue(NoteText.matches(note, "  train   kandy "))
        assertFalse(NoteText.matches(note, "train galle"))
    }

    @Test fun blankSearchMatchesEverything() {
        assertTrue(NoteText.matches(Note(), ""))
        assertTrue(NoteText.matches(Note(title = "x"), "   "))
    }

    // ORDER

    @Test fun pinnedNotesComeFirstThenNewest() {
        val oldPinned = Note(id = 1, pinned = true, updatedAt = now - 100 * minute)
        val newPinned = Note(id = 2, pinned = true, updatedAt = now - minute)
        val newest = Note(id = 3, updatedAt = now)
        val oldest = Note(id = 4, updatedAt = now - 1000 * minute)
        val sorted = listOf(oldest, newest, oldPinned, newPinned).sortedWith(NoteText.ORDER)
        assertEquals(listOf(2L, 1L, 3L, 4L), sorted.map { it.id })
    }

    // relativeTime

    @Test fun relativeTimeCoversEachRange() {
        assertEquals("Just now", NoteText.relativeTime(now - 30_000, now))
        assertEquals("1 min ago", NoteText.relativeTime(now - minute, now))
        assertEquals("59 min ago", NoteText.relativeTime(now - 59 * minute, now))
        assertEquals("1 h ago", NoteText.relativeTime(now - 60 * minute, now))
        assertEquals("23 h ago", NoteText.relativeTime(now - 23 * 60 * minute, now))
        assertEquals("Yesterday", NoteText.relativeTime(now - 25 * 60 * minute, now))
        assertEquals("6 days ago", NoteText.relativeTime(now - 6 * 24 * 60 * minute, now))
        assertNull(NoteText.relativeTime(now - 7 * 24 * 60 * minute, now))
    }

    @Test fun futureTimestampsAreTreatedAsNow() {
        assertEquals("Just now", NoteText.relativeTime(now + 10 * minute, now))
    }

    // hasChanges and isBlank

    @Test fun newNoteNeedsSavingOnlyWhenItHasText() {
        assertFalse(NoteText.hasChanges(null, Note(title = " ", content = "\n")))
        assertTrue(NoteText.hasChanges(null, Note(content = "idea")))
    }

    @Test fun savedNoteChangesAreDetectedButTimestampsAreIgnored() {
        val saved = Note(id = 7, title = "A", content = "B", updatedAt = now)
        assertFalse(NoteText.hasChanges(saved, saved.copy(updatedAt = now + minute)))
        assertTrue(NoteText.hasChanges(saved, saved.copy(content = "B2")))
        assertTrue(NoteText.hasChanges(saved, saved.copy(color = NoteColor.MINT.ordinal)))
        assertTrue(NoteText.hasChanges(saved, saved.copy(pinned = true)))
    }

    // NoteColor and samples

    @Test fun unknownColourIndexFallsBackToDefault() {
        assertEquals(NoteColor.DEFAULT, NoteColor.of(-1))
        assertEquals(NoteColor.DEFAULT, NoteColor.of(99))
        assertEquals(NoteColor.ROSE, NoteColor.of(NoteColor.ROSE.ordinal))
    }

    @Test fun everyColourIsOpaqueAndLight() {
        NoteColor.entries.forEach { c ->
            assertEquals("${c.label} background must be opaque", 0xFF, (c.background shr 24 and 0xFF).toInt())
            val r = (c.background shr 16 and 0xFF).toInt()
            val g = (c.background shr 8 and 0xFF).toInt()
            val bl = (c.background and 0xFF).toInt()
            assertTrue("${c.label} background must stay light", (r * 299 + g * 587 + bl * 114) / 1000 > 220)
        }
    }

    @Test fun sampleNotesAreValidAndOnePinned() {
        val samples = SampleNotes.create(now)
        assertEquals(4, samples.size)
        assertTrue(samples.none { it.isBlank })
        assertEquals(1, samples.count { it.pinned })
        assertTrue(samples.all { it.updatedAt <= now })
        assertEquals("Welcome to Inkwell", samples.sortedWith(NoteText.ORDER).first().title)
    }
}
