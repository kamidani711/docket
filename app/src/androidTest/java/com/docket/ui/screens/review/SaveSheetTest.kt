package com.docket.ui.screens.review

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.docket.domain.model.ExportFormat
import com.docket.domain.model.Folder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [SaveSheet] takes every bit of state as a parameter and every action as a plain lambda — no
 * ViewModel, no Hilt — so this needs nothing beyond a Compose test rule to exercise. Covers the
 * save step of the core scan flow (one of the two UI flows the brief asks for).
 *
 * Requires a device/emulator (`./gradlew connectedAndroidTest`) — not runnable in this
 * environment; see the chat write-up.
 */
@RunWith(AndroidJUnit4::class)
class SaveSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun saveButtonDisabledUntilNameIsEntered() {
        composeTestRule.setContent {
            SaveSheet(
                initialName = "",
                folders = emptyList(),
                isPdfImport = false,
                exportProgress = null,
                defaultFormat = ExportFormat.PDF,
                onCreateFolder = {},
                onSave = { _, _, _ -> },
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Save").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Name").performTextInput("Grocery receipt")
        composeTestRule.onNodeWithText("Save").assertIsEnabled()
    }

    @Test
    fun tappingNewRevealsTheFolderNameField() {
        composeTestRule.setContent {
            SaveSheet(
                initialName = "Doc",
                folders = listOf(Folder(id = 1L, name = "Taxes", parentFolderId = null, createdAt = 0L)),
                isPdfImport = false,
                exportProgress = null,
                defaultFormat = ExportFormat.PDF,
                onCreateFolder = {},
                onSave = { _, _, _ -> },
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("+ New").performClick()
        composeTestRule.onNodeWithText("New folder name").assertIsEnabled()
    }

    @Test
    fun savingReportsTheSelectedNameAndFormat() {
        var savedTitle: String? = null
        var savedFormat: ExportFormat? = null

        composeTestRule.setContent {
            SaveSheet(
                initialName = "Doc",
                folders = emptyList(),
                isPdfImport = false,
                exportProgress = null,
                defaultFormat = ExportFormat.PDF,
                onCreateFolder = {},
                onSave = { title, _, format ->
                    savedTitle = title
                    savedFormat = format
                },
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Separate images").performClick()
        composeTestRule.onNodeWithText("Save").performClick()

        assert(savedTitle == "Doc") { "expected title 'Doc', was $savedTitle" }
        assert(savedFormat == ExportFormat.IMAGE_SET) { "expected IMAGE_SET, was $savedFormat" }
    }
}
