package com.risny.notes.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.risny.notes.R
import com.risny.notes.data.Note
import com.risny.notes.data.NoteColor
import com.risny.notes.data.NoteText
import com.risny.notes.databinding.ItemNoteBinding
import java.text.DateFormat
import java.util.Date

/** Note cards. DiffUtil animates only the cards that really changed. */
class NotesAdapter(
    private val onOpen: (Note) -> Unit,
    private val onLongPress: (Note) -> Unit,
) : ListAdapter<Note, NotesAdapter.Holder>(Diff) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    inner class Holder(private val b: ItemNoteBinding) : RecyclerView.ViewHolder(b.root) {

        fun bind(note: Note) {
            val ctx = b.root.context
            val colour = NoteColor.of(note.color)
            val untitled = ctx.getString(R.string.title_hint)

            b.title.text = NoteText.displayTitle(note, untitled)
            val preview = NoteText.preview(note)
            b.preview.text = preview
            b.preview.isVisible = preview.isNotEmpty()
            b.pin.isVisible = note.pinned
            b.time.text = NoteText.relativeTime(note.updatedAt)
                ?: DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(note.updatedAt))

            b.card.setCardBackgroundColor(colour.background.toInt())
            b.card.strokeColor = if (colour == NoteColor.DEFAULT) {
                ContextCompat.getColor(ctx, R.color.line)
            } else {
                colour.background.toInt()
            }
            b.dot.backgroundTintList = ColorStateList.valueOf(colour.accent.toInt())
            b.pin.imageTintList = ColorStateList.valueOf(colour.accent.toInt())

            b.card.contentDescription = buildString {
                if (note.pinned) append(ctx.getString(R.string.pinned)).append(". ")
                append(b.title.text).append(". ").append(preview)
            }
            b.card.setOnClickListener { onOpen(note) }
            b.card.setOnLongClickListener { onLongPress(note); true }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(old: Note, new: Note) = old.id == new.id
        override fun areContentsTheSame(old: Note, new: Note) = old == new
    }
}
