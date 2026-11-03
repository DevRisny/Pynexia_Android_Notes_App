package com.risny.notes.ui

import android.app.Activity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.risny.notes.R
import com.risny.notes.data.Note
import com.risny.notes.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

/** The list of notes: search, staggered cards, pin, swipe to delete with Undo. */
class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private val vm: NotesViewModel by viewModels()
    private lateinit var adapter: NotesAdapter

    private val editor = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val deletedId = result.data?.getLongExtra(EditorActivity.EXTRA_DELETED_ID, 0L) ?: 0L
            if (deletedId != 0L) lifecycleScope.launch {
                vm.deleteById(deletedId)?.let(::offerUndo)
            }
            if (result.data?.getBooleanExtra(EditorActivity.EXTRA_DISCARDED, false) == true) {
                Snackbar.make(b.coordinator, R.string.empty_discarded, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        adapter = NotesAdapter(onOpen = ::open, onLongPress = ::togglePin)
        val wide = resources.configuration.screenWidthDp >= 600
        b.list.layoutManager = StaggeredGridLayoutManager(if (wide) 3 else 2, RecyclerView.VERTICAL)
        b.list.adapter = adapter
        attachSwipeToDelete()

        b.search.doAfterTextChanged { vm.setQuery(it?.toString().orEmpty()) }
        b.fab.setOnClickListener { editor.launch(EditorActivity.intent(this, null)) }
        b.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy > 8 && b.fab.isExtended) b.fab.shrink()
                if (dy < -8 && !b.fab.isExtended) b.fab.extend()
            }
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect(::render)
            }
        }
    }

    private fun render(state: NotesUiState) {
        adapter.submitList(state.notes)
        b.count.text = resources.getQuantityString(R.plurals.note_count, state.total, state.total)
        val empty = state.loaded && state.notes.isEmpty()
        b.empty.isVisible = empty
        b.list.isVisible = !empty
        if (empty) {
            val searching = state.query.isNotBlank()
            b.emptyTitle.setText(if (searching) R.string.no_results_title else R.string.empty_title)
            b.emptyBody.setText(if (searching) R.string.no_results_body else R.string.empty_body)
        }
    }

    private fun open(note: Note) = editor.launch(EditorActivity.intent(this, note.id))

    private fun togglePin(note: Note) {
        vm.togglePin(note)
        Snackbar.make(b.coordinator, if (note.pinned) R.string.unpinned else R.string.pinned, Snackbar.LENGTH_SHORT)
            .setAnchorView(b.fab)
            .show()
    }

    private fun offerUndo(note: Note) {
        Snackbar.make(b.coordinator, R.string.note_deleted, Snackbar.LENGTH_LONG)
            .setAnchorView(b.fab)
            .setAction(R.string.undo) { vm.restore(note) }
            .show()
    }

    private fun attachSwipeToDelete() {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val position = vh.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return
                val note = adapter.currentList[position]
                vm.delete(note)
                offerUndo(note)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(b.list)
    }
}
