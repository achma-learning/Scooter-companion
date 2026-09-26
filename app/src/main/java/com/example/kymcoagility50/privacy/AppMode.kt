package com.example.kymcoagility50.privacy

/**
 * The app has exactly two modes, chosen on first launch and changeable anytime in Settings.
 *
 * PRIVATE  — default. Fully offline. No account, no network call of any kind, no cloud
 *            initialization. All data (bookmarks, recent pages, settings, maintenance log)
 *            stays only in the local Room database.
 * SYNC     — the user has signed in with an account. Reading/search/bookmarks still work
 *            identically and stay local-first, but the maintenance logbook (date/km/notes
 *            entries only — nothing else) is pushed to and pulled from the cloud so it
 *            survives a reinstall / follows the user to another device.
 */
enum class AppMode { PRIVATE, SYNC }
