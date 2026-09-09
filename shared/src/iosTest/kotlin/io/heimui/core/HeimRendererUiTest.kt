package io.heimui.core

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import io.heimui.core.data.mapper.toDomain
import io.heimui.core.data.serialization.HeimJson
import io.heimui.core.presentation.HeimScreenRenderer
import io.heimui.core.presentation.designsystem.HeimTheme
import io.heimui.core.presentation.state.HeimStateManager
import kotlin.test.Test
import kotlin.test.assertTrue

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

    @Test
    fun `a root container asking to centre is no longer top-aligned`() = runComposeUiTest {
        // The payload carried `arrangement: CENTER` all the way down and the screen still rendered
        // top-aligned: a column wraps its content height, so Arrangement.Center had nothing to
        // centre within.
        val centred = render(
            """
            {"id":"s","root":{"type":"container","id":"root","arrangement":"CENTER",
             "children":[{"type":"text","id":"only","text":"middle"}]}}
            """.trimIndent()
        )
        setContent {
            HeimTheme {
                HeimScreenRenderer(centred, HeimStateManager("s"), onAction = {})
            }
        }

        val root = onNodeWithTag("root").getUnclippedBoundsInRoot()
        val child = onNodeWithTag("only").getUnclippedBoundsInRoot()
        val rootHeight = root.bottom.value - root.top.value

        // The column now takes the viewport, so the child sits around the middle of it rather
        // than at its top edge.
        assertTrue(rootHeight > 200f, "root did not take the viewport: ${rootHeight}dp")
        assertTrue(
            child.top.value > rootHeight / 4f,
            "child sits at ${child.top}, still top-aligned within ${rootHeight}dp",
        )
    }

    @Test
    fun `a packed container still stacks its children from the top`() = runComposeUiTest {
        // The default arrangement takes a different layout path, and it is the one every existing
        // payload is on. It must not have moved.
        val packed = render(
            """
            {"id":"s","root":{"type":"container","id":"root",
             "children":[{"type":"text","id":"only","text":"top"}]}}
            """.trimIndent()
        )
        setContent {
            HeimTheme {
                HeimScreenRenderer(packed, HeimStateManager("s"), onAction = {})
            }
        }

        val root = onNodeWithTag("root").getUnclippedBoundsInRoot()
        val child = onNodeWithTag("only").getUnclippedBoundsInRoot()
        assertTrue(
            child.top.value - root.top.value < 1f,
            "packed child moved to ${child.top} from a root at ${root.top}",
        )
    }

    @Test
    fun `siblings in a row share the width instead of the first one taking it all`() =
        runComposeUiTest {
            // An unweighted container inside a row used to fill the row and push its siblings off
            // the screen.
            val screen = render(
                """
                {"id":"s","root":{"type":"container","id":"root","direction":"HORIZONTAL",
                 "children":[
                    {"type":"container","id":"left","children":[{"type":"text","id":"lt","text":"L"}]},
                    {"type":"container","id":"right","children":[{"type":"text","id":"rt","text":"R"}]}
                 ]}}
                """.trimIndent()
            )
            setContent {
                HeimTheme {
                    HeimScreenRenderer(screen, HeimStateManager("s"), onAction = {})
                }
            }

            val rootWidth = with(onNodeWithTag("root").getUnclippedBoundsInRoot()) {
                right.value - left.value
            }
            val leftWidth = with(onNodeWithTag("left").getUnclippedBoundsInRoot()) {
                right.value - left.value
            }
            onNodeWithText("L").assertIsDisplayed()
            onNodeWithText("R").assertIsDisplayed()
            assertTrue(
                leftWidth < rootWidth / 2f,
                "left took ${leftWidth}dp of a ${rootWidth}dp row",
            )
        }

    @Test
    fun `a column inside a row gives its own children the full column width`() = runComposeUiTest {
        // The regression the flag introduces if it is only ever set: latched on at the row, it
        // would follow the subtree down and stop this card from filling the column that holds it.
        val screen = render(
            """
            {"id":"s","root":{"type":"container","id":"root","direction":"HORIZONTAL",
             "children":[
                {"type":"container","id":"col","weight":1,"children":[
                    {"type":"container","id":"card","background_color":"#EEEEEE",
                     "children":[{"type":"text","id":"ct","text":"C"}]}
                ]},
                {"type":"container","id":"other","weight":1,"children":[
                    {"type":"text","id":"ot","text":"O"}
                ]}
             ]}}
            """.trimIndent()
        )
        setContent {
            HeimTheme {
                HeimScreenRenderer(screen, HeimStateManager("s"), onAction = {})
            }
        }

        val columnWidth = with(onNodeWithTag("col").getUnclippedBoundsInRoot()) {
            right.value - left.value
        }
        val cardWidth = with(onNodeWithTag("card").getUnclippedBoundsInRoot()) {
            right.value - left.value
        }
        assertTrue(
            cardWidth >= columnWidth - 1f,
            "card is ${cardWidth}dp inside a ${columnWidth}dp column",
        )
    }

    @Test
    fun `space_between pushes the footer to the bottom of the screen`() = runComposeUiTest {
        // The other half of the same bug: only CENTER was ever worked around, so a header-and-
        // footer screen -- the most common reason to reach for SPACE_BETWEEN -- still stacked
        // both at the top.
        val screen = render(
            """
            {"id":"s","root":{"type":"container","id":"root","arrangement":"SPACE_BETWEEN",
             "children":[
                {"type":"text","id":"header","text":"header"},
                {"type":"text","id":"footer","text":"footer"}
             ]}}
            """.trimIndent()
        )
        setContent {
            HeimTheme {
                HeimScreenRenderer(screen, HeimStateManager("s"), onAction = {})
            }
        }

        val root = onNodeWithTag("root").getUnclippedBoundsInRoot()
        val footer = onNodeWithTag("footer").getUnclippedBoundsInRoot()
        val rootHeight = root.bottom.value - root.top.value
        assertTrue(
            footer.top.value > rootHeight / 2f,
            "footer sits at ${footer.top} in a ${rootHeight}dp screen",
        )
    }

    @Test
    fun `a distributing container taller than the viewport still scrolls`() = runComposeUiTest {
        // The height a distributing arrangement needs is a minimum, not a fixed size. Making it
        // fixed would have traded a centring bug for a clipping one: content past the fold with
        // no way to reach it, which is the very thing the scrolling column was added to fix.
        val children = (1..40).joinToString(",") {
            """{"type":"text","id":"row_$it","text":"row $it"}"""
        }
        val screen = render(
            """{"id":"s","root":{"type":"container","id":"root","arrangement":"CENTER",
             "children":[$children]}}"""
        )
        setContent {
            HeimTheme {
                HeimScreenRenderer(screen, HeimStateManager("s"), onAction = {})
            }
        }

        // Fails outright if nothing above it scrolls.
        onNodeWithTag("row_40").performScrollTo().assertIsDisplayed()
    }
}
