package io.heimui.core

import io.heimui.core.data.datasource.local.InMemoryHeimCacheDataSource
import io.heimui.core.data.datasource.remote.HeimRemoteDataSource
import io.heimui.core.data.datasource.remote.RemoteScreenResponse
import io.heimui.core.data.repository.HeimScreenRepositoryImpl
import io.heimui.core.data.security.Es256SignatureVerifier
import io.heimui.core.data.security.HeimSignedEnvelope
import io.heimui.core.data.security.es256RawToDer
import io.heimui.core.di.HeimConfig
import io.heimui.core.di.signatureVerifier
import io.heimui.core.di.verifiesSignatures
import io.heimui.core.domain.repository.HeimScreenResult
import io.heimui.core.domain.repository.HeimSubmitResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * ES256 screen signatures, against the vectors every other implementation is held to.
 *
 * The vectors were signed by Node and checked by Python before any of this existed, so a case
 * passing here means this SDK agrees with two unrelated implementations — not merely with itself.
 */
class HeimScreenSignatureTest {

    private val vectors = Json.parseToJsonElement(SIGNING_VECTORS_JSON).jsonObject
    private val publicKeyPem = vectors.text("publicKeyPem")
    private val verifier = Es256SignatureVerifier(listOf(publicKeyPem))
    private val cases = vectors.getValue("cases").jsonArray.map { it.jsonObject }

    private fun JsonObject.text(name: String): String = getValue(name).jsonPrimitive.content
    private fun case(name: String): JsonObject = cases.first { it.text("name") == name }

    private val headerCase get() = case("header: detached signature over the exact response bytes")
    private val envelopeCase get() = case("envelope: signed copy for a public bucket")

    // ---- the format -------------------------------------------------------------------------

    @Test
    fun `every conformance case gets the verdict the other implementations gave it`() {
        val disagreements = cases.mapNotNull { case ->
            val body = case.text("body").encodeToByteArray()
            val verdict = when (case.text("delivery")) {
                "header" -> verifier.verify(body, case["signature"]?.jsonPrimitive?.contentOrNull, null)
                else -> runCatching { HeimSignedEnvelope.unwrapOrNull(body) }.getOrNull()
                    ?.let { verifier.verify(it.payload, it.detachedSignature, null) }
                    ?: false
            }
            case.text("name").takeIf { verdict != case.getValue("valid").jsonPrimitive.boolean }
        }

        assertEquals(emptyList(), disagreements)
        assertTrue(cases.size >= 16, "the vector file lost cases: ${cases.size}")
    }

    @Test
    fun `a key is known by its RFC 7638 thumbprint as every JOSE library computes it`() {
        assertEquals(setOf(vectors.text("keyId")), verifier.keyIds)
    }

    @Test
    fun `an envelope opens to exactly the screen that was signed`() {
        val envelope = assertNotNull(HeimSignedEnvelope.unwrapOrNull(envelopeCase.text("body").encodeToByteArray()))

        assertEquals(envelopeCase.text("payload"), envelope.payload.decodeToString())
        assertTrue(envelope.detachedSignature.contains(".."), envelope.detachedSignature)
    }

    @Test
    fun `an ordinary screen or a partial envelope is not taken for one`() {
        assertNull(HeimSignedEnvelope.unwrapOrNull(headerCase.text("body").encodeToByteArray()))
        assertNull(HeimSignedEnvelope.unwrapOrNull("""{"protected":"a","payload":"b"}""".encodeToByteArray()))
        assertNull(HeimSignedEnvelope.unwrapOrNull("""{"ok":true}""".encodeToByteArray()))
    }

    @Test
    fun `a failure says which check failed`() {
        val body = headerCase.text("body")
        val signature = headerCase.text("signature")

        assertNull(verifier.failureReason(body.encodeToByteArray(), signature))
        assertEquals("the screen is not signed", verifier.failureReason(body.encodeToByteArray(), null))
        assertEquals(
            "the signature does not match the screen's content",
            verifier.failureReason("$body\n".encodeToByteArray(), signature),
        )
        val stranger = case("header: kid names a key the app does not trust")
        val reason = assertNotNull(verifier.failureReason(body.encodeToByteArray(), stranger.text("signature")))
        assertTrue(reason.contains("does not trust"), reason)
    }

    @Test
    fun `raw signature integers become minimal signed DER`() {
        // r = 1 is one content byte; s starts with its top bit set, so it needs a zero in front.
        val raw = ByteArray(64).also {
            it[31] = 1
            it[32] = 0x80.toByte()
        }
        val der = assertNotNull(es256RawToDer(raw))

        assertEquals(
            listOf<Byte>(0x30, 38, 0x02, 0x01, 0x01, 0x02, 0x21, 0x00, 0x80.toByte()),
            der.take(9),
        )
        assertEquals(40, der.size)
        assertNull(es256RawToDer(ByteArray(64)), "an r of zero is never a signature")
        assertNull(es256RawToDer(ByteArray(63)))
    }

    // ---- the keys ---------------------------------------------------------------------------

    @Test
    fun `the public key is accepted however a build config mangles the PEM`() {
        val body = publicKeyPem.lines().filterNot { it.startsWith("-----") }.joinToString("")
        val escapedNewlines = publicKeyPem.replace("\n", "\\n")

        listOf(body, escapedNewlines, "  $publicKeyPem\n").forEach { form ->
            assertEquals(verifier.keyIds, Es256SignatureVerifier(listOf(form)).keyIds, form)
        }
    }

    @Test
    fun `a private key is refused with a warning that it has leaked`() {
        val error = assertFailsWith<IllegalArgumentException> {
            Es256SignatureVerifier(listOf(vectors.text("privateKeyPem")))
        }
        assertTrue(error.message.orEmpty().contains("private key"), error.message)
    }

    @Test
    fun `a key that is not an uncompressed P-256 key fails at construction`() {
        val body = publicKeyPem.lines().filterNot { it.startsWith("-----") }.joinToString("")
        // Character 20 falls inside the curve's object identifier.
        val otherCurve = body.replaceRange(20, 21, if (body[20] == 'A') "B" else "A")

        assertFailsWith<IllegalArgumentException> { Es256SignatureVerifier(listOf(otherCurve)) }
        assertFailsWith<IllegalArgumentException> { Es256SignatureVerifier(listOf("not a key")) }
        assertFailsWith<IllegalArgumentException> { Es256SignatureVerifier(emptyList()) }
    }

    // ---- configuration ----------------------------------------------------------------------

    @Test
    fun `a signature configuration that cannot work fails at startup not on the first screen`() {
        HeimUI.reset()
        val base = "https://api.heimui.io"

        assertFailsWith<IllegalArgumentException> {
            HeimUI.initialize(HeimConfig(baseUrl = base, verifySignatures = true))
        }
        assertFailsWith<IllegalArgumentException> {
            HeimUI.initialize(HeimConfig(baseUrl = base, trustedSigningKeys = setOf(publicKeyPem), publicKey = "secret"))
        }
        assertFailsWith<IllegalArgumentException> {
            HeimUI.initialize(HeimConfig(baseUrl = base, trustedSigningKeys = setOf("MFkwEwYHKoZIzj0CAQ")))
        }
        assertFalse(HeimUI.isInitialized)
    }

    @Test
    fun `trusting a key turns verification on by itself`() {
        val config = HeimConfig(baseUrl = "https://api.heimui.io", trustedSigningKeys = setOf(publicKeyPem))

        assertTrue(config.verifiesSignatures())
        assertIs<Es256SignatureVerifier>(config.signatureVerifier())
    }

    // ---- delivery ---------------------------------------------------------------------------

    private fun serving(
        body: String,
        signature: String? = null,
        cache: InMemoryHeimCacheDataSource = InMemoryHeimCacheDataSource(),
    ) = HeimScreenRepositoryImpl(
        remoteDataSource = HeimRemoteDataSource(
            httpClient = HttpClient(MockEngine { request ->
                if (request.headers[HttpHeaders.IfNoneMatch] != null) {
                    respond("", HttpStatusCode.NotModified)
                } else {
                    respond(
                        body,
                        HttpStatusCode.OK,
                        headersOf(
                            *listOfNotNull(
                                HttpHeaders.ETag to listOf("\"v1\""),
                                signature?.let { "X-Heim-Signature" to listOf(it) },
                            ).toTypedArray()
                        ),
                    )
                }
            }),
            baseUrl = "https://api.heimui.io",
        ),
        cacheDataSource = cache,
        signatureVerifier = verifier,
        verifySignatures = true,
    )

    @Test
    fun `a signed screen renders and its cached copy is verified again before it is shown`() = runTest {
        val cache = InMemoryHeimCacheDataSource()
        val repository = serving(headerCase.text("body"), headerCase.text("signature"), cache)

        assertIs<HeimScreenResult.Success>(repository.getScreen("login").toList().single())

        // Stale from the cache first, then confirmed by a 304: the copy passed verification again.
        val reopened = repository.getScreen("login").toList()
        assertEquals(2, reopened.size, reopened.toString())
        assertTrue(reopened.all { it is HeimScreenResult.Success }, reopened.toString())
    }

    @Test
    fun `a cached copy altered on the device is thrown away rather than shown`() = runTest {
        val cache = InMemoryHeimCacheDataSource()
        val repository = serving(headerCase.text("body"), headerCase.text("signature"), cache)
        repository.getScreen("login").toList()

        val key = "https://api.heimui.io/login"
        val entry = assertNotNull(cache.getScreen(key))
        cache.saveScreen(key, entry.screen, entry.etag, entry.signature, "${headerCase.text("body")} ".encodeToByteArray())

        // No stale emission and no If-None-Match: the altered entry was dropped, and the screen was
        // fetched again in full.
        val results = repository.getScreen("login").toList()
        assertFalse(assertIs<HeimScreenResult.Success>(results.single()).isStale)
    }

    @Test
    fun `a screen that does not match its signature is refused saying why`() = runTest {
        val results = serving("${headerCase.text("body")}\n", headerCase.text("signature")).getScreen("login").toList()

        val error = assertIs<HeimScreenResult.Error>(results.single())
        assertTrue(error.message.contains("does not match"), error.message)
    }

    @Test
    fun `an unsigned screen is refused once signatures are on`() = runTest {
        val error = assertIs<HeimScreenResult.Error>(serving(headerCase.text("body")).getScreen("login").toList().single())
        assertTrue(error.message.contains("not signed"), error.message)
    }

    @Test
    fun `a sealed screen from a bucket verifies with no header at all`() = runTest {
        assertIs<HeimScreenResult.Success>(serving(envelopeCase.text("body")).getScreen("login").toList().single())
    }

    /**
     * Signing is a choice the app makes, and a sealed screen must not require it: a customer whose
     * Studio seals bucket copies may have an app that verifies nothing at all yet.
     */
    @Test
    fun `a sealed screen renders in an app that verifies nothing`() = runTest {
        val repository = HeimScreenRepositoryImpl(
            remoteDataSource = HeimRemoteDataSource(
                httpClient = HttpClient(MockEngine { respond(envelopeCase.text("body"), HttpStatusCode.OK) }),
                baseUrl = "https://api.heimui.io",
            ),
            cacheDataSource = InMemoryHeimCacheDataSource(),
        )

        val result = assertIs<HeimScreenResult.Success>(repository.getScreen("login").toList().single())
        assertEquals("login", result.screen.id)
    }

    @Test
    fun `an envelope that cannot be opened is an error never a screen`() = runTest {
        val padded = case("envelope: payload is padded base64 rather than base64url")
        val error = assertIs<HeimScreenResult.Error>(serving(padded.text("body")).getScreen("login").toList().single())
        assertTrue(error.message.contains("base64url"), error.message)
    }

    @Test
    fun `a public screen host is reachable and never sees a credential`() = runTest {
        val seen = mutableListOf<Pair<String, String?>>()
        val remote = HeimRemoteDataSource(
            httpClient = HttpClient(MockEngine { request ->
                seen += request.url.host to request.headers[HttpHeaders.Authorization]
                respond(envelopeCase.text("body"), HttpStatusCode.OK)
            }),
            baseUrl = "https://api.heimui.io",
            authTokenProvider = { "Bearer session-token" },
            publicScreenHosts = setOf("screens.example-cdn.com"),
        )

        val fromBucket = remote.fetchScreen("https://screens.example-cdn.com/public/@release/login.json")
        assertEquals(envelopeCase.text("payload"), assertIs<RemoteScreenResponse.Success>(fromBucket).rawBytes.decodeToString())
        assertIs<RemoteScreenResponse.Success>(remote.fetchScreen("login"))
        // Being public is not the same as being allowed: an unlisted host is still refused.
        assertIs<RemoteScreenResponse.Error>(remote.fetchScreen("https://elsewhere.example.com/login.json"))

        assertEquals(
            listOf("screens.example-cdn.com" to null, "api.heimui.io" to "Bearer session-token"),
            seen,
        )
    }

    @Test
    fun `a screen returned by a form submission is held to the same signature`() = runTest {
        fun submitting(body: String, signature: String? = null) = HeimScreenRepositoryImpl(
            remoteDataSource = HeimRemoteDataSource(
                httpClient = HttpClient(MockEngine {
                    respond(
                        body,
                        HttpStatusCode.OK,
                        headersOf(*listOfNotNull(signature?.let { "X-Heim-Signature" to listOf(it) }).toTypedArray()),
                    )
                }),
                baseUrl = "https://api.heimui.io",
            ),
            cacheDataSource = InMemoryHeimCacheDataSource(),
            signatureVerifier = verifier,
            verifySignatures = true,
        )

        val unsigned = submitting(headerCase.text("body")).submitForm("orders", "POST", null)
        val message = assertIs<HeimSubmitResult.Error>(unsigned).message
        assertTrue(message.contains("not signed"), message)

        assertIs<HeimSubmitResult.Success>(
            submitting(headerCase.text("body"), headerCase.text("signature")).submitForm("orders", "POST", null)
        )
        assertIs<HeimSubmitResult.Success>(submitting(envelopeCase.text("body")).submitForm("orders", "POST", null))
        // An acknowledgement that is not a screen has nothing to verify.
        assertIs<HeimSubmitResult.Success>(submitting("""{"ok":true}""").submitForm("orders", "POST", null))
    }
}
