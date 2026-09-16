package io.heimui.core

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import io.heimui.core.data.repository.MockHeimScreenRepository
import io.heimui.core.presentation.HeimScreen
import io.heimui.core.presentation.designsystem.HeimTheme
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The overlay and the two replaceable states.
 *
 * These cover decisions rather than plumbing. An overlay that shows a skeleton covers the screen
 * the user is working on with a shimmer; one that shows an error card interrupts them to report a
 * failure in something they never asked for. Both were choices, and a test is what stops the next
 * person from "fixing" them back.
 */
@OptIn(ExperimentalTestApi::class)
class HeimScreenOverlayUiTest {

    private fun screen(id: String, text: String) = """
        {"id":"$id","version":"1.0.0","root":
          {"type":"text","id":"${id}_t","text":"$text"}}
    """.trimIndent()

    private fun repo(vararg screens: Pair<String, String>) =
        MockHeimScreenRepository(jsonProvider = { id -> screens.toMap()[id] })

    @Test
    fun `overlay draws over the screen and both are visible`() = runComposeUiTest {
        setContent {
            HeimTheme {
                HeimScreen(
                    screenId = "home",
                    onAction = {},
                    repository = repo("home" to screen("home", "Behind"),
                                      "notif" to screen("notif", "On top")),
                    overlayScreenId = "notif"
                )
            }
        }
        waitForIdle()
        onNodeWithText("Behind").assertIsDisplayed()
        onNodeWithText("On top").assertIsDisplayed()
    }

    @Test
    fun `an overlay that cannot load shows nothing at all`() = runComposeUiTest {
        setContent {
            HeimTheme {
                HeimScreen(
                    screenId = "home",
                    onAction = {},
                    // The overlay id resolves to nothing, so its fetch fails.
                    repository = repo("home" to screen("home", "Behind")),
                    overlayScreenId = "missing"
                )
            }
        }
        waitForIdle()
        // The screen underneath is untouched, and no error card was put over it.
        onNodeWithText("Behind").assertIsDisplayed()
        assertEquals(0, onAllNodesWithTag("heim_error_view").fetchSemanticsNodes().size)
    }

    @Test
    fun `errorContent replaces the built-in card`() = runComposeUiTest {
        setContent {
            HeimTheme {
                HeimScreen(
                    screenId = "missing",
                    onAction = {},
                    repository = repo(),
                    errorContent = { _, _ -> Text("Our own wording") }
                )
            }
        }
        waitForIdle()
        onNodeWithText("Our own wording").assertIsDisplayed()
        assertEquals(0, onAllNodesWithTag("heim_error_view").fetchSemanticsNodes().size)
    }

    @Test
    fun `the built-in card is still there when nothing replaces it`() = runComposeUiTest {
        setContent {
            HeimTheme {
                HeimScreen(screenId = "missing", onAction = {}, repository = repo())
            }
        }
        waitForIdle()
        onNodeWithTag("heim_error_view").assertIsDisplayed()
    }
}
