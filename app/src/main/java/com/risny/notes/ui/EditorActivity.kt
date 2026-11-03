package com.risny.notes.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.risny.notes.NotesApp
import com.risny.notes.R
import com.risny.notes.data.Note
import com.risny.notes.data.NoteColor
import com.risny.notes.data.NoteDao
import com.risny.notes.data.NoteDatabase
import com.risny.notes.data.NoteText
import com.risny.notes.databinding.ActivityEditorBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Create or edit one note. There is no Save button: the note is saved whenever the
 * screen is left, and an empty new note is simply not kept.
 */
class EditorActivity : AppCompatActivity() {

    private lateinit var b: ActivityEditorBinding
    private lateinit var dao: NoteDao

    /** What is in the database right now (null for a note that was never saved). */
    @Volatile private var saved: Note? = null
    private var colour = NoteColor.DEFAULT
    private var pinned = false
    private var loaded = false
    private val saveLock = Mutex()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(b.root)
        dao = NoteDatabase.get(this).noteDao()

        b.toolbar.setNavigationOnClickListener { close() }
        b.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_pin -> { pinned = !pinned; renderPin(); true }
                R.id.action_share -> { share(); true }
                R.id.action_delete -> { delete(); true }
                else -> false
            }
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = close()
        })
        b.title.doAfterTextChanged { renderMeta() }
        b.content.doAfterTextChanged { renderMeta() }
        buildColourRow()

        val id = intent.getLongExtra(EXTRA_ID, 0L)
        if (savedInstanceState != null) {
            colour = NoteColor.of(savedInstanceState.getInt(STATE_COLOUR))
            pinned = savedInstanceState.getBoolean(STATE_PINNED)
        }
        lifecycleScope.launch {
            val existing = if (id != 0L) dao.findById(id) else null
            saved = existing
            if (existing != null && savedInstanceState == null) {
                b.title.setText(existing.title)
                b.content.setText(existing.content)
                colour = NoteColor.of(existing.color)
                pinned = existing.pinned
            }
            if (existing == null && savedInstanceState == null) b.content.requestFocus()
            loaded = true
            applyColour()
            renderPin()
            renderMeta()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_COLOUR, colour.ordinal)
        outState.putBoolean(STATE_PINNED, pinned)
    }

    override fun onPause() {
        super.onPause()
        if (!isFinishing) save()
    }

    /** The note as it is on screen now. */
    private fun edited(): Note {
        val base = saved ?: Note()
        return base.copy(
            title = b.title.text.toString().trim(),
            content = b.content.text.toString().trimEnd(),
            color = colour.ordinal,
            pinned = pinned,
        )
    }

    /** Saves in the app scope so the write finishes even after this screen is gone. */
    private fun save() {
        if (!loaded) return
        val note = edited()
        if (note.isBlank || !NoteText.hasChanges(saved, note)) return
        (application as NotesApp).appScope.launch {
            saveLock.withLock {
                val current = saved
                val stamped = note.copy(id = current?.id ?: 0L, updatedAt = System.currentTimeMillis())
                saved = if (current == null) stamped.copy(id = dao.insert(stamped)) else stamped.also { dao.update(it) }
            }
        }
    }

    private fun close() {
        val note = edited()
        val result = Intent()
        if (note.isBlank && saved != null) {
            // The user cleared an existing note: treat it as a delete the list can undo.
            result.putExtra(EXTRA_DELETED_ID, saved!!.id)
        } else if (note.isBlank && (b.title.text.isNotEmpty() || b.content.text.isNotEmpty())) {
            result.putExtra(EXTRA_DISCARDED, true)
        } else {
            save()
        }
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    private fun delete() {
        val existing = saved
        if (existing == null) {
            finish()
            return
        }
        // Keep the latest edits so Undo brings back exactly what was on screen.
        save()
        setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_DELETED_ID, existing.id))
        finish()
    }

    private fun share() {
        val note = edited()
        val text = listOf(note.title, note.content).filter { it.isNotBlank() }.joinToString("\n\n")
        if (text.isBlank()) return
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, note.title)
            .putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(send, getString(R.string.share)))
    }

    private fun buildColourRow() {
        val size = dp(28)
        NoteColor.entries.forEach { option ->
            val swatch = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = dp(10) }
                contentDescription = getString(R.string.colour_label, option.label)
                isClickable = true
                isFocusable = true
                setOnClickListener { colour = option; applyColour() }
                tag = option
            }
            b.colors.addView(swatch)
        }
    }

    private fun applyColour() {
        val background = if (colour == NoteColor.DEFAULT) ContextCompat.getColor(this, R.color.surface) else colour.background.toInt()
        b.editorRoot.setBackgroundColor(background)
        window.statusBarColor = background
        window.navigationBarColor = background
        for (i in 0 until b.colors.childCount) {
            val swatch = b.colors.getChildAt(i)
            val option = swatch.tag as NoteColor
            val selected = option == colour
            swatch.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(option.background.toInt())
                setStroke(
                    if (selected) dp(3) else dp(1),
                    if (selected) option.accent.toInt() else ContextCompat.getColor(this@EditorActivity, R.color.line),
                )
            }
            swatch.isSelected = selected
        }
    }

    private fun renderPin() {
        val item = b.toolbar.menu.findItem(R.id.action_pin) ?: return
        item.setIcon(if (pinned) R.drawable.ic_pin_filled else R.drawable.ic_pin)
        item.title = getString(if (pinned) R.string.unpin else R.string.pin)
    }

    private fun renderMeta() {
        val words = NoteText.wordCount(b.title.text.toString() + " " + b.content.text.toString())
        val wordText = resources.getQuantityString(R.plurals.word_count, words, words)
        val edited = saved?.let { NoteText.relativeTime(it.updatedAt) }
        b.meta.text = if (edited != null) "$wordText, edited ${edited.lowercase()}" else wordText
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_ID = "note_id"
        const val EXTRA_DELETED_ID = "deleted_id"
        const val EXTRA_DISCARDED = "discarded"
        private const val STATE_COLOUR = "colour"
        private const val STATE_PINNED = "pinned"

        fun intent(context: Context, id: Long?): Intent =
            Intent(context, EditorActivity::class.java).putExtra(EXTRA_ID, id ?: 0L)
    }
}
