<div align="center">

# ✒️ Inkwell — Android Notes App

**A calm, fast notes app for Android, written in Kotlin with Room, MVVM and Material 3.**
Notes save by themselves, search updates as you type, and nothing is lost by accident.

[![CI](https://github.com/DevRisny/Pynexia_Android_Notes_App/actions/workflows/ci.yml/badge.svg)](https://github.com/DevRisny/Pynexia_Android_Notes_App/actions/workflows/ci.yml)
[![Screenshots](https://github.com/DevRisny/Pynexia_Android_Notes_App/actions/workflows/screenshots.yml/badge.svg)](https://github.com/DevRisny/Pynexia_Android_Notes_App/actions/workflows/screenshots.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?logo=kotlin&logoColor=white)
![Min SDK](https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white)
![Tests](https://img.shields.io/badge/unit%20tests-19%20passing-10b981)

**[⬇️ Download the APK](https://github.com/DevRisny/Pynexia_Android_Notes_App/releases/latest/download/Inkwell.apk)**

</div>

---

## 📱 Screens

<table>
<tr>
<td width="25%"><img src="docs/images/1-notes.png" alt="Notes list in a two-column staggered grid with a pinned welcome note"></td>
<td width="25%"><img src="docs/images/2-editor.png" alt="Editor showing the welcome note on a lavender background"></td>
<td width="25%"><img src="docs/images/3-new-note.png" alt="Writing a new note with the peach colour selected"></td>
<td width="25%"><img src="docs/images/4-search.png" alt="Search for rice showing only the groceries note"></td>
</tr>
<tr>
<td><sub><b>Notes</b>: pinned first, then newest</sub></td>
<td><sub><b>Editor</b>: word count and last edit</sub></td>
<td><sub><b>New note</b> with a colour</sub></td>
<td><sub><b>Live search</b> across title and text</sub></td>
</tr>
</table>

<sub>These are real screenshots: the <a href=".github/workflows/screenshots.yml">Screenshots workflow</a> installs the APK on an Android 14 emulator, drives it with <a href="scripts/capture.py">scripts/capture.py</a> and commits the images.</sub>

## ✨ Features

| | |
|---|---|
| 📝 **Create, edit, delete** | Full CRUD on a local Room (SQLite) database. |
| 💾 **Autosave** | No Save button. Leaving the editor saves the note; an empty new note is simply not kept. Saves run in an app-wide scope, so closing the screen never cuts a write in half. |
| ↩️ **Undo delete** | Swipe a card sideways, or tap Delete in the editor, and a Snackbar offers Undo, which restores the note with its original id and dates. |
| 📌 **Pin** | Long-press a card (or use the pin in the editor) to keep it at the top. |
| 🔍 **Live search** | Every word you type must match the title or text, case-insensitive. A "No matching notes" state explains empty results. |
| 🎨 **7 note colours** | Soft light backgrounds with a matching accent, applied to the card, the editor and the status bar. |
| 🧱 **Staggered grid** | Two columns on phones, three on tablets; cards size to their content. |
| 📤 **Share** | Send any note to WhatsApp, Gmail or any app through the Android share sheet. |
| 🕒 **Friendly times** | "Just now", "5 min ago", "Yesterday", then the date. |
| 🔄 **Survives rotation** | State lives in a ViewModel and a lifecycle-aware Flow. |
| 👋 **First-run notes** | A short welcome and a few sample notes, so the app never opens to a blank screen. |
| ♿ **Accessible** | Content descriptions on every card, icon and colour swatch; text colours meet WCAG AA. |

## 🏗️ Architecture

```
UI (Activities + ViewBinding)  ──observes──▶  NotesViewModel (StateFlow<NotesUiState>)
          │                                             │
          └──────────── user actions ─────────────────▶ │ combine(dao.observeAll(), query)
                                                        ▼
                                              NoteDao (Room, Flow + suspend)
                                                        ▼
                                                 SQLite  inkwell.db
```

```
app/src/main/java/com/risny/notes/
├── NotesApp.kt            Application with a long-lived coroutine scope for saves
├── data/
│   ├── Note.kt            Room entity (indexed on pinned and updated_at)
│   ├── NoteDao.kt         queries; reads are Flows, writes are suspend functions
│   ├── NoteDatabase.kt    thread-safe singleton
│   ├── NoteText.kt        pure logic: titles, previews, search, ordering, times, colours
│   └── SampleNotes.kt     first-run content
└── ui/
    ├── MainActivity.kt    list, search, swipe to delete, Undo, pin
    ├── NotesAdapter.kt    ListAdapter + DiffUtil, stable ids
    ├── NotesViewModel.kt  one UI state built from the database and the search query
    └── EditorActivity.kt  editor with autosave, colours, pin, share, delete
```

**Design choices**

- **Single source of truth.** The list never edits itself; it only renders what Room emits, so the screen and the database cannot disagree.
- **Logic kept out of Android classes.** Everything worth testing lives in `NoteText`, which runs on a plain JVM in milliseconds.
- **No lost writes.** Saves go through a `Mutex` so a quick pause and resume can never insert the same new note twice.

## 🧪 Tests and CI

```bash
./gradlew testDebugUnitTest     # 19 unit tests
./gradlew assembleDebug         # app/build/outputs/apk/debug/app-debug.apk
```

The tests cover title fallbacks, previews and truncation, word counts, multi-word search, pinned-then-newest ordering, every relative-time range, change detection for autosave, colour safety (every background must stay opaque and light) and the first-run samples.

On every push, [GitHub Actions](.github/workflows/ci.yml) runs the tests, builds the APK, keeps it as an artifact and publishes it to the [latest release](https://github.com/DevRisny/Pynexia_Android_Notes_App/releases/latest).

## 🚀 Run it

1. **On a phone:** download [Inkwell.apk](https://github.com/DevRisny/Pynexia_Android_Notes_App/releases/latest/download/Inkwell.apk), open it and allow installs from this source (Android 7.0 or newer).
2. **In Android Studio:** clone the repo, open the folder, wait for Gradle sync and press Run.

```bash
git clone https://github.com/DevRisny/Pynexia_Android_Notes_App.git
```

## 🧰 Tech stack

Kotlin · AndroidX · Room 2.6 with KSP · Lifecycle ViewModel and StateFlow · Coroutines · RecyclerView with ListAdapter · Material Components 3 · ViewBinding · JUnit 4 · GitHub Actions

---

<div align="center">

Built by **Junaideen Risny Suha** for the **Pynexia Android App Development Internship**, Week 2.

[LinkedIn](https://linkedin.com/in/risny-suha-8a6b66423) · [GitHub](https://github.com/DevRisny)

</div>
