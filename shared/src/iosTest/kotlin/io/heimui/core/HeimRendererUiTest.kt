package io.heimui.core

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import io.heimui.core.data.mapper.toDomain
import io.heimui.core.data.serialization.HeimJson
import io.heimui.core.presentation.HeimScreenRenderer
import io.heimui.core.presentation.designsystem.HeimTheme
import io.heimui.core.presentation.state.HeimStateManager
import kotlin.test.Test

/**
 * End-to-end render tests for adversarial payloads.
 *
 * Lives in `iosTest` rather than `commonTest`: the Compose test harness needs a real UI runtime,
 * and the Android host-test target is a bare JVM (no `android.os.Build`), so these would need
 * Robolectric or an instrumented run to work there. The composable tree is identical on both
 * platforms, so running them on one is enough to cover the render logic.
 *
 * The mapper tests prove that a hostile value gets clamped. They do NOT prove that Compose
 * survives rendering the result -- and every one of these payloads used to crash at measure time,
 * not at parse time. Verifying the sanitisation without verifying the render left that loop open:
 * a future field added without clamping would still pass the mapper tests and still crash the app.
 */
@OptIn(ExperimentalTestApi::class)
class HeimRendererUiTest {

    private fun render(json: String) = HeimJson.decodeScreen(json).toDomain()

    @Test
    fun `negative padding and spacing render without crashing`() = runComposeUiTest {
        // Modifier.padding and Arrangement.spacedBy both throw on negative Dp.
        val screen = render(
            """
            {"id":"s","root":{"type":"container","id":"root","padding":-40,"spacing":-9,
             "children":[{"type":"text","id":"t","text":"survived"}]}}
            """.trimIndent()
        )
        setContent {
            HeimTheme {
                HeimScreenRenderer(
                    response = screen,
                    stateManager = HeimStateManager("s"),
                    onAction = {}
                )
            }
        }
        onNodeWithText("survived").assertIsDisplayed()
    }

    @Test
    fun `maxLines of zero renders without crashing`() = runComposeUiTest {
        // Text requires maxLines > 0.
        val screen = render(
            """{"id":"s","root":{"type":"text","id":"t","text":"survived","max_lines":0}}"""
        )
        setContent {
            HeimTheme {
                HeimScreenRenderer(screen, HeimStateManager("s"), onAction = {})
            }
        }
        onNodeWithText("survived").assertIsDisplayed()
    }

    @Test
    fun `duplicate ids in a lazy list do not collide as Compose keys`() = runComposeUiTest {
        // LazyColumn throws IllegalArgumentException on a repeated key.
        val screen = render(
            """
            {"id":"s","root":{"type":"lazy_column","id":"list","items":[
                {"type":"text","id":"dup","text":"first"},
                {"type":"text","id":"dup","text":"second"}
            ]}}
            """.trimIndent()
        )
        setContent {
            HeimTheme {
                HeimScreenRenderer(screen, HeimStateManager("s"), onAction = {})
            }
        }
        onNodeWithText("first").assertIsDisplayed()
        onNodeWithText("second").assertIsDisplayed()
    }

    @Test
    fun `an unknown component renders a fallback instead of taking down the screen`() =
        runComposeUiTest {
            val screen = render(
                """
                {"id":"s","root":{"type":"container","id":"root","children":[
                    {"type":"carousel_3d","id":"future"},
                    {"type":"text","id":"t","text":"sibling still renders"}
                ]}}
                """.trimIndent()
            )
            setContent {
                HeimTheme {
                    HeimScreenRenderer(screen, HeimStateManager("s"), onAction = {})
                }
            }
            // The unknown node degrades AND its siblings are unaffected.
            onNodeWithText("sibling still renders").assertIsDisplayed()
        }

    @Test
    fun `components carry their id as a test tag so hosts can address them`() = runComposeUiTest {
        val screen = render(
            """{"id":"s","root":{"type":"text","id":"welcome_title","text":"Hello"}}"""
        )
        setContent {
            HeimTheme {
                HeimScreenRenderer(screen, HeimStateManager("s"), onAction = {})
            }
        }
        onNodeWithTag("welcome_title").assertIsDisplayed()
    }

    @Test
    fun `visibleIf hides a node whose state key is absent`() = runComposeUiTest {
        // Fail-closed: an unresolved condition must not reveal the node.
        val screen = render(
            """
            {"id":"s","root":{"type":"container","id":"root","children":[
                {"type":"text","id":"admin","text":"admin panel","visible_if":"is_admin"},
                {"type":"text","id":"public","text":"public content"}
            ]}}
            """.trimIndent()
        )
        setContent {
            HeimTheme {
                HeimScreenRenderer(screen, HeimStateManager("s"), onAction = {})
            }
        }
        onNodeWithText("public content").assertIsDisplayed()
        onNodeWithTag("admin").assertDoesNotExist()
    }

    @Test
    fun `a deeply nested but legal tree renders`() = runComposeUiTest {
        val open = StringBuilder()
        val close = StringBuilder()
        repeat(20) {
            open.append("""{"type":"container","id":"c$it","scrollable":false,"children":[""")
            close.append("]}")
        }
        val screen = render(
            """{"id":"s","root":$open{"type":"text","id":"leaf","text":"deep"}$close}"""
        )
        setContent {
            HeimTheme {
                HeimScreenRenderer(screen, HeimStateManager("s"), onAction = {})
            }
        }
        onNodeWithText("deep").assertIsDisplayed()
    }
}
