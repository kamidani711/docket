package com.docket.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/** Shares one or more app-private files via the native share sheet, using [FileProvider] so
 *  the receiving app gets a scoped content:// Uri rather than a raw file:// one (blocked on
 *  our targetSdk — a file:// Uri handed to another app crashes with a FileUriExposedException).
 *  See the `<provider>` entry in AndroidManifest.xml + res/xml/file_paths.xml. */
fun shareFiles(context: Context, files: List<File>, mimeType: String) {
    if (files.isEmpty()) return

    val authority = "${context.packageName}.fileprovider"
    val uris = ArrayList(files.map { file -> FileProvider.getUriForFile(context, authority, file) })

    val sendIntent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uris.first())
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }
    }
    sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(sendIntent, null))
}

/** Opens a single app-private file in whatever app the user has for [mimeType] (a PDF reader,
 *  a gallery app, …) via the same [FileProvider] content:// Uri [shareFiles] uses. Shows a toast
 *  instead of crashing when nothing on the device can handle it. */
fun openFile(context: Context, file: File, mimeType: String) {
    val authority = "${context.packageName}.fileprovider"
    val uri = FileProvider.getUriForFile(context, authority, file)
    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(viewIntent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app found to open this file.", Toast.LENGTH_SHORT).show()
    }
}

/** Copies [source]'s bytes to a [destination] the user picked via the system "Save to…" picker
 *  (`ActivityResultContracts.CreateDocument`, launched by the caller) — the Storage Access
 *  Framework path for "let the user choose exactly where this goes," distinct from [shareFiles]
 *  handing the bytes to another app. Returns whether the copy succeeded, so the caller can show
 *  a failure message instead of a false "saved" toast. */
fun saveFileToUri(context: Context, source: File, destination: Uri): Boolean =
    try {
        context.contentResolver.openOutputStream(destination)?.use { out ->
            source.inputStream().use { it.copyTo(out) }
        } != null
    } catch (_: java.io.IOException) {
        false
    }
