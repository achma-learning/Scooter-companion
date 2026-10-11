package com.example.kymcoagility50.privacy

/**
 * The app has exactly two modes, chosen on first launch and changeable anytime in Settings.
 *
 * PRIVATE  — default. No account, no cloud initialization, and the app never goes online by
 *            itself. All data (bookmarks, recent pages, settings, garage, maintenance log)
 *            stays only in the local Room database. The single exception is a public download
 *            the owner starts with a tap (today's fuel price, NetworkGuard.userRequested).
 * SYNC     — the user has signed in with an account. Reading/search/bookmarks still work
 *            identically and stay local-first, but the maintenance logbook (date, km, task
 *            ids, milestone, shop, cost, stamp and notes — nothing else) is pushed to and
 *            pulled from the cloud so it survives a reinstall / follows the user to another
 *            device.
 */
enum class AppMode { PRIVATE, SYNC }
