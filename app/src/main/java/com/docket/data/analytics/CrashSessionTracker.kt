package com.docket.data.analytics

import android.content.Context
import java.io.File

/**
 * Crash-free-session tracking without a crash-reporting SDK: a plain marker file, written
 * synchronously (unlike DataStore, which is async and not guaranteed to flush before a crashing
 * process dies) so it reliably survives a hard crash.
 *
 * The heuristic: [markSessionActive] runs at app start; [markSessionEnded] runs when the app is
 * backgrounded (MainActivity.onStop — this is a single-Activity app, so Activity-level and
 * process-level "backgrounded" are effectively the same thing here). If the marker is still
 * present the *next* time the app starts, the previous session never reached a clean background
 * transition — either it crashed in the foreground, or the process was killed some other way.
 * That's a real limitation: OS-initiated kills for memory pressure while backgrounded are
 * already excluded by the onStop marker-clear, but a kill that races with app startup itself,
 * before onStop ever runs again, would be indistinguishable from a genuine crash. Good enough
 * for a rough local signal; not the guarantee a dedicated crash SDK would give.
 */
object CrashSessionTracker {
    private const val MARKER_FILE_NAME = "session_active"

    private fun markerFile(context: Context): File = File(context.filesDir, MARKER_FILE_NAME)

    fun wasPreviousSessionCrashed(context: Context): Boolean = markerFile(context).exists()

    fun markSessionActive(context: Context) {
        runCatching { markerFile(context).createNewFile() }
    }

    fun markSessionEnded(context: Context) {
        runCatching { markerFile(context).delete() }
    }
}
