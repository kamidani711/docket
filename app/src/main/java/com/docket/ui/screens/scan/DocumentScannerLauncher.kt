package com.docket.ui.screens.scan

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.docket.ui.util.findActivity
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

/**
 * Launches ML Kit's Document Scanner (camera capture + auto edge-detection/crop + its own
 * gallery-import affordance + multi-page-in-one-session, all handled natively) and hands back
 * the captured page URIs. Deliberately [GmsDocumentScannerOptions.SCANNER_MODE_BASE] — capture
 * and basic crop only, no ML Kit review/filter UI — because reorder/delete/re-crop/filters are
 * all built fresh in our own Review screen (draggable corners, live filter thumbnails, etc. —
 * none of that exists in ML Kit's own reviewer). Running both would mean two review screens
 * back to back, which is the opposite of "faster than the competition."
 *
 * Requests CAMERA permission ourselves before launching rather than assuming the scanner
 * activity handles it — cheap insurance, and testable independently of Play services behavior.
 *
 * DELIBERATE V1 TRADEOFF — read before touching capture UX:
 * This launches a separate Google Play Services *system Activity*. We don't render a single
 * pixel of the live camera surface it shows — no control over its edge-detection overlay, its
 * shutter, its flash toggle, or its theming (light chrome regardless of our own dark capture
 * theme). [com.docket.ui.screens.scan.ScanScreen] is styled to feel like a custom camera screen
 * right up to the moment this launches, and picks back up once it returns.
 *
 * TODO(camera-v2): replace this with a real CameraX capture screen (camera-core/camera2/
 * lifecycle/view are already project dependencies, unused) plus OpenCV-based contour detection
 * for live edge tracking, once that's scoped and approved — see the chat write-up. That's the
 * only way to get animated corner brackets, an auto-capture progress ring, haptics-on-capture,
 * and an in-app flash toggle, none of which this Activity boundary can expose to us. Swapping
 * this function's internals for a CameraX flow (same `() -> Unit` return shape) is meant to be
 * the entire migration — everything upstream of it (ScanScreen, ScanSessionViewModel, Review)
 * only ever deals in captured page URIs and shouldn't need to change.
 */
@Composable
fun rememberDocumentScannerLauncher(
    onPagesCaptured: (List<Uri>) -> Unit,
    onError: (String) -> Unit = {}
): () -> Unit {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    val scanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val uris = scanResult?.pages?.map { it.imageUri }.orEmpty()
            if (uris.isEmpty()) {
                onError("No pages were captured.")
            } else {
                onPagesCaptured(uris)
            }
        }
        // RESULT_CANCELED just means the user backed out — not an error worth surfacing.
    }

    fun startScan() {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(SCAN_PAGE_LIMIT)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE)
            .build()

        GmsDocumentScanning.getClient(options)
            .getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                scanLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Couldn't open the scanner.")
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startScan() else onError("Camera permission is needed to scan.")
    }

    return {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (hasPermission) startScan() else permissionLauncher.launch(Manifest.permission.CAMERA)
    }
}

private const val SCAN_PAGE_LIMIT = 100
