package io.heimui.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.heimui.core.data.repository.MockHeimScreenRepository
import io.heimui.core.domain.evaluator.HeimValidationEngine
import io.heimui.core.domain.evaluator.HeimValidatorRegistry
import io.heimui.core.domain.model.HeimValue
import io.heimui.core.domain.model.component.ContainerComponent
import io.heimui.core.domain.repository.HeimScreenResult
import io.heimui.core.domain.model.validation.ValidationRule
import io.heimui.core.domain.model.validation.ValidationType
import io.heimui.core.presentation.state.HeimStateManager
import io.heimui.demo.DemoScreens
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests that must run on a real Android runtime, not the host JVM.
 *
 * Android delegates `java.util.regex` to ICU; host tests use the JVM engine and Kotlin/Native uses
 * a third. They disagree on details as small as whether a bare `}` is a literal. A pattern that
 * compiled cleanly behind 67 passing unit tests threw `PatternSyntaxException` at startup on a
 * device, because none of those tests ran where the app actually runs.
 *
 * Everything here exercises a platform-provided library — regex, text handling, JSON — so it can
 * only be trusted once it has executed on a device.
 *
 * Uses only the SDK's public API, exactly as a consumer would: `explicitApi` keeps the parser and
 * mappers internal, so this exercises the contract rather than the implementation.
 *
 * Run with: ./gradlew :androidApp:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class HeimAndroidRuntimeSmokeTest {

    @Test
    fun placeholderInterpolationWorksOnAndroidRuntime() {
        val state = HeimStateManager(screenId = "smoke")
        state.updateValue("name", "Julian")
        state.updateValue("amount", "500")

        val out = state.interpolatePayload(
            mapOf(
                "whole" to HeimValue.Str("{{state.name}}"),
                "embedded" to HeimValue.Str("Hola {{state.name}}, son {{state.amount}}"),
                "typed" to HeimValue.Str("{{state.amount}}"),
                "unterminated" to HeimValue.Str("roto {{state.name"),
                "literalBrace" to HeimValue.Str("a } b")
            )
        )

        assertEquals(HeimValue.Str("Julian"), out?.get("whole"))
        assertEquals(HeimValue.Str("Hola Julian, son 500"), out?.get("embedded"))
        assertEquals(HeimValue.Int64(500), out?.get("typed"))
        assertEquals(HeimValue.Str("roto {{state.name"), out?.get("unterminated"))
        assertEquals(HeimValue.Str("a } b"), out?.get("literalBrace"))
    }

    @Test
    fun builtInValidationPatternsCompileOnAndroidRuntime() {
        val registry = HeimValidatorRegistry()

        assertNull(
            HeimValidationEngine.validate(
                "user@heimui.io",
                listOf(ValidationRule(ValidationType.EMAIL, errorMessage = "bad email"))
            )
        )
        assertNotNull(
            HeimValidationEngine.validate(
                "not-an-email",
                listOf(ValidationRule(ValidationType.EMAIL, errorMessage = "bad email"))
            )
        )
        assertTrue(registry.isValid("IBAN", "DE89370400440532013000", null))
        assertTrue(registry.isValid("LUHN", "4539578763621486", null))

        // A malformed server-supplied pattern must degrade, never take down the process.
        assertNotNull(
            HeimValidationEngine.validate(
                "x",
                listOf(ValidationRule(ValidationType.REGEX, value = "([unclosed", errorMessage = "bad"))
            )
        )
    }

    @Test
    fun hostilePayloadDegradesOnAndroidRuntime() = runBlocking {
        val hostile = """
            {"id":"smoke","title":"Smoke","root":{"type":"container","id":"root","padding":-8,
             "children":[
                {"type":"text","id":"t","text":"hello","max_lines":0},
                {"type":"carousel_3d","id":"unknown_one"},
                {"type":"text","id":"dup","text":"a"},
                {"type":"text","id":"dup","text":"b"}
             ]}}
        """.trimIndent()

        val repo = MockHeimScreenRepository(jsonProvider = { hostile })
        val result = repo.getScreen("smoke").first()

        val success = result as? HeimScreenResult.Success
        assertNotNull("hostile payload must degrade, not fail the screen", success)
        val root = success!!.screen.root as ContainerComponent
        assertEquals(0, root.padding)                     // clamped before Compose sees it
        assertEquals(4, root.children.size)               // unknown type degraded, siblings intact
        assertTrue(success.screen.violations.isNotEmpty()) // and the repair was reported
    }

    @Test
    fun everyDemoScreenLoadsOnAndroidRuntime() = runBlocking {
        // The exact payloads the sample app renders. This is the check that would have caught the
        // startup crash: it walks the full parse -> map path for real content, on a real device.
        val repo = MockHeimScreenRepository(jsonProvider = { DemoScreens.getJson(it) })
        val ids = DemoScreens.screens.map { it.first }
        assertTrue("demo catalogue must not be empty", ids.isNotEmpty())

        ids.forEach { id ->
            val result = repo.getScreen(id).first()
            assertTrue(
                "screen '$id' failed to load: $result",
                result is HeimScreenResult.Success
            )
            // The catalogue key and the payload's own `id` are independent by design: the key is
            // how the demo app looks a screen up, the id is what the screen calls itself.
            val loaded = (result as HeimScreenResult.Success).screen
            assertTrue("screen '$id' has a blank id", loaded.id.isNotBlank())
            assertTrue("screen '$id' rendered nothing", loaded.root.id.isNotBlank())
        }
    }
}
