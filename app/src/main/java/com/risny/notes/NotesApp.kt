package com.risny.notes

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Holds a scope that outlives single screens, so a save started while the editor
 * closes is never cancelled half way.
 */
class NotesApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
