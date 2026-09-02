package io.heimui.core

import io.heimui.core.data.datasource.remote.HeimRemoteDataSource
import io.heimui.core.data.datasource.remote.RemoteSubmitResponse
import io.heimui.core.data.mapper.toDomain
import io.heimui.core.data.security.HeimPayloadGuard
import io.heimui.core.data.security.HeimPayloadRejectedException
import io.heimui.core.data.serialization.HeimJson
import io.heimui.core.domain.model.HeimValue
import io.heimui.core.domain.model.action.UnknownAction
import io.heimui.core.domain.model.component.ButtonComponent
import io.heimui.core.domain.model.component.ContainerComponent
import io.heimui.core.domain.model.component.HeimPadding
import io.heimui.core.domain.model.component.TextFieldComponent
import io.heimui.core.domain.model.component.LazyColumnComponent
import io.heimui.core.domain.model.component.TextComponent
import io.heimui.core.domain.model.component.UnknownComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class HeimSecurityAndResilienceTest {

    private fun deeplyNestedJson(depth: Int): String {
        val open = StringBuilder()
        val close = StringBuilder()
        repeat(depth) {
            open.append("""{"type":"container","id":"c$it","children":[""")
            close.append("]}")
        }
        return """{"id":"s","root":$open{"type":"text","id":"leaf","text":"x"}$close}"""
    }

    /**
     * Regression test for the SIGSEGV DoS.
     *
     * This must exercise the **JSON string** path, not a programmatically built DTO tree. The
     * stack overflow happens inside the parser, so a test that constructs DTOs in Kotlin skips
     * the very code that used to crash and reports a false pass.
     */
    @Test
    fun `deeply nested JSON is rejected before the parser can overflow the stack`() {
        // 1000 levels used to terminate the process with SIGSEGV on Kotlin/Native.
        assertFailsWith<HeimPayloadRejectedException> {
            HeimJson.decodeScreen(deeplyNestedJson(1000))
        }
        // Still safe far beyond that, and well under the 5 MB size limit.
        assertFailsWith<HeimPayloadRejectedException> {
            HeimJson.decodeScreen(deeplyNestedJson(50_000))
        }
    }

    @Test
    fun `payload guard accepts realistic nesting and counts only structural braces`() {
        assertIs<HeimPayloadGuard.Verdict.Accepted>(
            HeimPayloadGuard.inspect(deeplyNestedJson(20))
        )
        // Braces inside string literals must not inflate the measured depth.
        val bracesInText = """{"id":"s","root":{"type":"text","id":"t","text":"{{{{{{ literal }}}}}}"}}"""
        assertIs<HeimPayloadGuard.Verdict.Accepted>(HeimPayloadGuard.inspect(bracesInText))
        // An escaped quote must not desynchronise the string scanner.
        val escapedQuote = """{"id":"s","root":{"type":"text","id":"t","text":"a \" b {{{"}}"""
        assertIs<HeimPayloadGuard.Verdict.Accepted>(HeimPayloadGuard.inspect(escapedQuote))
    }

    @Test
    fun `unknown component and action types degrade and preserve their original type`() {
        val screen = HeimJson.decodeScreen(
            """
            {"id":"future","root":{"type":"container","id":"root","children":[
                {"type":"carousel_3d","id":"c1"},
                {"type":"button","id":"b1","title":"Go","actions":[{"type":"biometric_auth"}]}
            ]}}
            """.trimIndent()
        ).toDomain()

        val root = assertIs<ContainerComponent>(screen.root)
        val unknown = assertIs<UnknownComponent>(root.children[0])
        // The type name must survive: telemetry has to name the component the backend shipped.
        assertEquals("carousel_3d", unknown.originalType)

        val button = assertIs<ButtonComponent>(root.children[1])
        assertIs<UnknownAction>(button.actions.first())
    }

    @Test
    fun `malformed fields degrade the node instead of destroying the screen`() {
        // Missing required field.
        val missingText = HeimJson.decodeScreen("""{"id":"s","root":{"type":"text","id":"t"}}""").toDomain()
        assertEquals("", assertIs<TextComponent>(missingText.root).text)

        // Missing discriminator.
        val noType = HeimJson.decodeScreen("""{"id":"s","root":{"id":"t","text":"hi"}}""").toDomain()
        assertIs<UnknownComponent>(noType.root)

        // Unknown enum constant falls back to the declared default.
        val badEnum = HeimJson.decodeScreen(
            """{"id":"s","root":{"type":"container","id":"c","direction":"DIAGONAL"}}"""
        ).toDomain()
        assertIs<ContainerComponent>(badEnum.root)

        // Numeric field sent as garbage, and as a numeric string (the common backend bug).
        val garbage = HeimJson.decodeScreen(
            """{"id":"s","root":{"type":"container","id":"c","padding":"sixteen"}}"""
        ).toDomain()
        assertEquals(HeimPadding.None, assertIs<ContainerComponent>(garbage.root).padding)

        val stringNumber = HeimJson.decodeScreen(
            """{"id":"s","root":{"type":"container","id":"c","padding":"16"}}"""
        ).toDomain()
        assertEquals(HeimPadding.all(16), assertIs<ContainerComponent>(stringNumber.root).padding)
    }

    @Test
    fun `out of range dimensions are clamped so Compose cannot be handed invalid constraints`() {
        val screen = HeimJson.decodeScreen(
            """
            {"id":"s","root":{"type":"container","id":"c","padding":-40,"spacing":-9,
             "children":[{"type":"text","id":"t","text":"x","max_lines":0}]}}
            """.trimIndent()
        ).toDomain()

        val root = assertIs<ContainerComponent>(screen.root)
        assertEquals(HeimPadding.None, root.padding)   // Modifier.padding requires non-negative
        assertEquals(0, root.spacing)   // Arrangement.spacedBy requires non-negative
        assertEquals(1, assertIs<TextComponent>(root.children[0]).maxLines) // Text requires > 0
    }

    @Test
    fun `duplicate ids are disambiguated so LazyColumn keys cannot collide`() {
        val screen = HeimJson.decodeScreen(
            """
            {"id":"s","root":{"type":"lazy_column","id":"l","items":[
                {"type":"text","id":"dup","text":"a"},
                {"type":"text","id":"dup","text":"b"}
            ]}}
            """.trimIndent()
        ).toDomain()

        val list = assertIs<LazyColumnComponent>(screen.root)
        assertNotEquals(list.items[0].id, list.items[1].id)
    }

    @Test
    fun `cross origin form submission is refused as a result rather than a thrown exception`() = runTest {
        val dataSource = dataSourceWith(baseUrl = "https://api.mybank.com/sdui")

        // Throwing here would turn a hostile payload into a host-app crash: a DoS traded for an
        // exfiltration hole. The refusal must be a value the caller can render.
        val result = dataSource.submitForm(
            endpoint = "https://attacker.com/collect",
            method = "POST",
            payload = mapOf("amount" to HeimValue.Num(500.0))
        )
        val violation = assertIs<RemoteSubmitResponse.SecurityViolation>(result)
        assertTrue(violation.message.contains("attacker.com"))
    }

    @Test
    fun `submission policy blocks cleartext and foreign ports and traversal and unsafe methods`() = runTest {
        val dataSource = dataSourceWith(baseUrl = "https://api.mybank.com")

        assertIs<RemoteSubmitResponse.SecurityViolation>(
            dataSource.submitForm("http://api.mybank.com/x")
        )
        assertIs<RemoteSubmitResponse.SecurityViolation>(
            dataSource.submitForm("https://api.mybank.com:8443/x")
        )
        assertIs<RemoteSubmitResponse.SecurityViolation>(
            dataSource.submitForm("/../../evil")
        )
        assertIs<RemoteSubmitResponse.SecurityViolation>(
            dataSource.submitForm("/v1/ok", method = "DELETE")
        )
        // Same-origin relative path is the normal case and must still work.
        assertIs<RemoteSubmitResponse.Success>(dataSource.submitForm("/v1/ok"))
    }

    @Test
    fun `mixed type payloads serialize correctly`() = runTest {
        var body: String? = null
        val engine = MockEngine { request ->
            body = (request.body as TextContent).text
            respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
        }
        val dataSource = HeimRemoteDataSource(
            httpClient = HttpClient(engine) {
                install(ContentNegotiation) { json(HeimJson.instance) }
            },
            baseUrl = "https://api.mybank.com"
        )

        val result = dataSource.submitForm(
            endpoint = "/v1/transfer",
            payload = mapOf(
                "amount" to HeimValue.Num(500.0),
                "to" to HeimValue.Str("acc_1"),
                "urgent" to HeimValue.Bool(true)
            )
        )

        assertIs<RemoteSubmitResponse.Success>(result)
        assertTrue(body!!.contains("\"amount\":500"), "numbers must stay numbers: $body")
        assertTrue(body!!.contains("\"urgent\":true"), "booleans must stay booleans: $body")
    }

    private fun dataSourceWith(baseUrl: String): HeimRemoteDataSource {
        val engine = MockEngine {
            respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
        }
        return HeimRemoteDataSource(
            httpClient = HttpClient(engine) {
                install(ContentNegotiation) { json(HeimJson.instance) }
            },
            baseUrl = baseUrl,
            authTokenProvider = { "Bearer SECRET_USER_TOKEN" }
        )
    }
    @Test
    fun `a validation rule missing its message costs the rule and not the screen`() {
        // It used to cost the screen: `error_message` was required, so one forgotten string in
        // one rule failed the whole payload — every field, every button, all of it.
        val screen = HeimJson.decodeScreen(
            """{"id":"s","root":{"type":"text_field","id":"f","state_key":"name",
                 "validation_rules":[{"type":"REQUIRED"}]}}"""
        ).toDomain()

        val field = assertIs<TextFieldComponent>(screen.root)
        assertEquals(1, field.validationRules.size)
        // The rule still blocks submission; only the explanation is missing, so it gets a generic
        // one rather than an empty bubble.
        assertEquals("Invalid value", field.validationRules.single().errorMessage)
    }

    @Test
    fun `an unknown component keeps its original type for telemetry`() {
        // The type is preserved rather than discarded, which is what lets the backend team learn
        // *which* component this client could not render. Whether it is also drawn is a separate
        // decision — see HeimTheme's showDiagnostics, which is off outside development so a user
        // is never shown a red box and an internal id.
        val screen = HeimJson.decodeScreen(
            """{"id":"s","root":{"type":"container","id":"c","children":[
                 {"type":"hologram","id":"h1"}]}}"""
        ).toDomain()

        val unknown = assertIs<UnknownComponent>(
            assertIs<ContainerComponent>(screen.root).children.single()
        )
        assertEquals("hologram", unknown.originalType)
        assertEquals("h1", unknown.id)
    }

}
