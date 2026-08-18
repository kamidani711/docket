package com.docket.ui.screens.documentdetail

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.docket.domain.model.Document
import com.docket.domain.model.ExportFormat
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [ExportSheet] also takes all state as parameters, no ViewModel — the second of the two UI
 * flows covered here (export, gated behind Premium for batch operations).
 *
 * Requires a device/emulator (`./gradlew connectedAndroidTest`) — not runnable in this
 * environment; see the chat write-up.
 */
@RunWith(AndroidJUnit4::class)
class ExportSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val pdfDocument = Document(
        id = 1L,
        title = "Receipt",
        folderId = null,
        createdAt = 0L,
        pageCount = 1,
        format = ExportFormat.PDF,
        pdfFilePath = "/tmp/doc.pdf",
        pageImagePaths = emptyList(),
        thumbnailPath = null
    )

    @Test
    fun premiumRequiredFailureShowsAnUnlockButton() {
        var unlockClicked = false

        composeTestRule.setContent {
            ExportSheet(
                document = pdfDocument,
                hasOcrText = true,
                isPremium = false,
                exportState = ExportUiState.Failed("This needs Premium to unlock.", isPremiumRequired = true),
                onExportPdf = { _, _, _ -> },
                onExportImages = { _, _, _ -> },
                onOpenPremium = { unlockClicked = true },
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("This needs Premium to unlock.").assertExists()
        composeTestRule.onNodeWithText("Unlock Premium").performClick()
        assert(unlockClicked) { "expected onOpenPremium to have been called" }
    }

    @Test
    fun ordinaryFailureShowsNoUnlockButton() {
        composeTestRule.setContent {
            ExportSheet(
                document = pdfDocument,
                hasOcrText = true,
                isPremium = true,
                exportState = ExportUiState.Failed("Export failed.", isPremiumRequired = false),
                onExportPdf = { _, _, _ -> },
                onExportImages = { _, _, _ -> },
                onOpenPremium = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Export failed.").assertExists()
        composeTestRule.onNodeWithText("Unlock Premium").assertDoesNotExist()
    }

    /** Regression guard: exporting used to leave the user with no way to act on the file --
     *  the sheet just said "Exported." and the one Share button lived on the screen underneath
     *  this (still-open, non-dismissed) modal, unreachable without dismissing the sheet first,
     *  which cleared the very state that button depended on. These actions now live directly in
     *  the sheet's Done state, so they must actually be there. */
    @Test
    fun doneStateShowsShareOpenAndSaveActions() {
        composeTestRule.setContent {
            ExportSheet(
                document = pdfDocument,
                hasOcrText = true,
                isPremium = true,
                exportState = ExportUiState.Done(files = listOf(File("/tmp/exported.pdf")), mimeType = "application/pdf"),
                onExportPdf = { _, _, _ -> },
                onExportImages = { _, _, _ -> },
                onOpenPremium = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Exported.").assertExists()
        composeTestRule.onNodeWithText("Share").assertExists()
        composeTestRule.onNodeWithText("Open").assertExists()
        composeTestRule.onNodeWithText("Save").assertExists()
    }
}
