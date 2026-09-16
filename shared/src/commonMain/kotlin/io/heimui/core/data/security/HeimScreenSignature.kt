package io.heimui.core.data.security

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Verifies screen signatures made with ES256 — ECDSA on the P-256 curve with SHA-256 — against the
 * public keys an app trusts.
 *
 * This is the verifier for production. The HMAC one it replaces needs the signing secret on the
 * device, and anything on a device can be extracted: whoever pulls it out of one installation can
 * sign a screen that every installation accepts. A public key cannot sign, so shipping it costs
 * nothing.
 *
 * The format is JWS (RFC 7515), so any JOSE library can produce it, in two forms:
 *
 * - **From a server** — the Studio, or your backend after it hydrates — as a detached JWS over the
 *   exact response bytes, in the `X-Heim-Signature` header: `<protected>..<signature>`.
 * - **From a bucket or a CDN**, which cannot add a header of its own, as the flattened JSON form:
 *   `{"protected": …, "payload": …, "signature": …}`, with the screen base64url-encoded inside.
 *
 * Both reduce to the same check, so a screen verifies identically whichever way it arrived, and
 * again when it is read back from the cache. The protected header must name `"alg": "ES256"` —
 * nothing else is accepted, `none` included — and a `kid` equal to the RFC 7638 thumbprint of one
 * of the trusted keys. A header carrying `crit` or `b64` is refused rather than half-understood.
 *
 * More than one key is normal: the Studio signs what it serves and what it writes to a bucket,
 * your backend signs what it hydrates, and during a rotation the outgoing and the incoming key are
 * both valid until every installation has the new one.
 *
 * The `publicKey` argument of [verify] is ignored: the keys are fixed at construction.
 *
 * @param trustedKeys P-256 public keys, as PEM (`-----BEGIN PUBLIC KEY-----`) or its bare base64
 *   body. Parsed here, so a malformed key fails at startup instead of rejecting every screen.
 * @throws IllegalArgumentException if [trustedKeys] is empty or an entry is not a P-256 public key.
 */
public class Es256SignatureVerifier(trustedKeys: Collection<String>) : HeimSignatureVerifier {

    private val keysById: Map<String, HeimSigningPublicKey>

    init {
        require(trustedKeys.isNotEmpty()) { "Es256SignatureVerifier needs at least one trusted public key." }
        keysById = trustedKeys.mapIndexed { index, text -> HeimSigningPublicKey.parse(text, index) }
            .associateBy { it.keyId }
    }

    /** The key ids this verifier accepts: the RFC 7638 thumbprints of the trusted keys. */
    public val keyIds: Set<String> get() = keysById.keys

    override fun verify(payloadBytes: ByteArray, signature: String?, publicKey: String?): Boolean =
        failureReason(payloadBytes, signature) == null

    /**
     * Why [signature] does not verify [payloadBytes], or null when it does.
     *
     * Worded for the person reading an error state or a log, and safe to show them: it names key
     * ids, which are public, and never echoes text from the signature beyond one that looks like
     * a key id.
     */
    internal fun failureReason(payloadBytes: ByteArray, signature: String?): String? {
        if (signature.isNullOrBlank()) return "the screen is not signed"
        val parsed = when (val result = HeimDetachedJws.parse(signature)) {
            is HeimDetachedJws.Parsed.Invalid -> return result.reason
            is HeimDetachedJws.Parsed.Valid -> result.jws
        }
        val key = keysById[parsed.keyId]
            ?: return "it was signed with key ${parsed.keyId}, which this app does not trust"
        val der = es256RawToDer(parsed.signature) ?: return "the signature is not a valid ES256 value"
        val signingInput = "${parsed.protectedHeader}.${HeimBase64.encodeUrl(payloadBytes)}".encodeToByteArray()
        return if (platformVerifyEs256(key.spki, key.point, signingInput, der)) {
            null
        } else {
            "the signature does not match the screen's content"
        }
    }
}

/**
 * ECDSA P-256 with SHA-256, by the platform's own cryptography: `java.security` on Android and the
 * Security framework on iOS. Neither is reimplemented here, on purpose — elliptic-curve arithmetic
 * is exactly the code that should come from a platform that has been audited for it.
 *
 * @param spki the key as X.509 SubjectPublicKeyInfo DER.
 * @param point the same key as an uncompressed point, `04 || X || Y`. Each platform imports
 *   whichever form it accepts.
 * @param derSignature the signature as an ASN.1 `SEQUENCE { r, s }`.
 */
internal expect fun platformVerifyEs256(
    spki: ByteArray,
    point: ByteArray,
    message: ByteArray,
    derSignature: ByteArray,
): Boolean

/** A trusted P-256 public key, and the key id a signature names it by. */
internal class HeimSigningPublicKey private constructor(val spki: ByteArray) {

    /** `04 || X || Y`. */
    val point: ByteArray = spki.copyOfRange(SPKI_PREFIX.size, spki.size)

    /**
     * The RFC 7638 JWK thumbprint, which is what a signer puts in `kid`.
     *
     * Derived rather than configured, so a key and its id cannot be mismatched: an app lists
     * public keys and nothing else, and every JOSE library computes the same value.
     */
    val keyId: String = run {
        val x = HeimBase64.encodeUrl(point.copyOfRange(1, 33))
        val y = HeimBase64.encodeUrl(point.copyOfRange(33, 65))
        HeimBase64.encodeUrl(Sha256.hash("""{"crv":"P-256","kty":"EC","x":"$x","y":"$y"}""".encodeToByteArray()))
    }

    companion object {
        /**
         * The DER header every uncompressed P-256 SubjectPublicKeyInfo starts with: the
         * id-ecPublicKey and prime256v1 object identifiers, then the bit string holding the point.
         * Matching it byte for byte is stricter than parsing ASN.1, and there is nothing else a
         * valid key here could look like.
         */
        private val SPKI_PREFIX = byteArrayOf(
            0x30, 0x59, 0x30, 0x13, 0x06, 0x07, 0x2a, 0x86.toByte(), 0x48, 0xce.toByte(), 0x3d, 0x02, 0x01,
            0x06, 0x08, 0x2a, 0x86.toByte(), 0x48, 0xce.toByte(), 0x3d, 0x03, 0x01, 0x07, 0x03, 0x42, 0x00,
        )
        private const val POINT_SIZE = 65

        fun parse(text: String, index: Int): HeimSigningPublicKey {
            val position = "Trusted signing key #${index + 1}"
            require(!text.contains("PRIVATE KEY")) {
                "$position is a private key. An app needs only the public half, and a private key " +
                    "that has been in an app's source has to be treated as leaked: rotate it."
            }
            return parseOrNull(text) ?: throw IllegalArgumentException(
                "$position is not a P-256 public key. Paste the PEM block " +
                    "(-----BEGIN PUBLIC KEY-----) exactly as the Studio or your backend shows it."
            )
        }

        fun parseOrNull(text: String): HeimSigningPublicKey? {
            // A PEM pasted into a build config or an environment variable often arrives on one line,
            // with its newlines written as the two characters `\n`.
            val body = text
                .replace("\\n", "\n")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .filterNot { it.isWhitespace() }
                .trimEnd('=')
            val der = HeimBase64.decodeStandard(body) ?: return null
            if (der.size != SPKI_PREFIX.size + POINT_SIZE) return null
            if (!der.copyOfRange(0, SPKI_PREFIX.size).contentEquals(SPKI_PREFIX)) return null
            if (der[SPKI_PREFIX.size] != 0x04.toByte()) return null
            return HeimSigningPublicKey(der)
        }
    }
}

/** A detached JWS as carried in `X-Heim-Signature`: `<protected>..<signature>`. */
internal class HeimDetachedJws(
    val protectedHeader: String,
    val keyId: String,
    /** Raw `r || s`, 64 bytes, as JWS defines ES256. */
    val signature: ByteArray,
) {
    sealed interface Parsed {
        class Valid(val jws: HeimDetachedJws) : Parsed
        class Invalid(val reason: String) : Parsed
    }

    companion object {
        private val KEY_ID_SHAPE = Regex("[A-Za-z0-9_-]{1,64}")

        fun parse(value: String): Parsed {
            val parts = value.trim().split('.')
            if (parts.size != 3) return Parsed.Invalid("the signature is not a JWS")
            if (parts[1].isNotEmpty()) {
                return Parsed.Invalid("the signature carries a payload of its own; send a detached JWS")
            }

            val headerText = HeimBase64.decodeUrl(parts[0])?.decodeToString()
                ?: return Parsed.Invalid("the signature header is not base64url")
            // The header is attacker-supplied, so it goes through the same depth guard as a screen
            // before the parser recurses into it.
            if (!HeimPayloadGuard.isSafe(headerText)) {
                return Parsed.Invalid("the signature header is not a JSON object")
            }
            val header = runCatching { Json.parseToJsonElement(headerText) as? JsonObject }.getOrNull()
                ?: return Parsed.Invalid("the signature header is not a JSON object")

            if (header.stringField("alg") != "ES256") {
                return Parsed.Invalid("only ES256 signatures are accepted")
            }
            if ("crit" in header) {
                return Parsed.Invalid("the signature header lists extensions this SDK does not understand")
            }
            if ("b64" in header) {
                return Parsed.Invalid("unencoded JWS payloads are not supported")
            }
            val keyId = header.stringField("kid")
            if (keyId.isNullOrEmpty()) return Parsed.Invalid("the signature does not name its key (kid)")
            if (!KEY_ID_SHAPE.matches(keyId)) return Parsed.Invalid("the signature names a key id this app cannot have")

            val signature = HeimBase64.decodeUrl(parts[2])
                ?: return Parsed.Invalid("the signature is not base64url")
            if (signature.size != ES256_SIGNATURE_SIZE) {
                return Parsed.Invalid("an ES256 signature is 64 bytes (r || s); this one is ${signature.size}")
            }
            return Parsed.Valid(HeimDetachedJws(parts[0], keyId, signature))
        }

        private fun JsonObject.stringField(name: String): String? =
            (get(name) as? JsonPrimitive)?.takeIf { it.isString }?.content
    }
}

/**
 * A screen delivered as a flattened JWS, which is how a signed screen sits in a bucket: object
 * storage and most CDNs will not return a custom response header, so the signature travels inside
 * the object instead.
 */
internal class HeimSignedEnvelope(
    /** The screen's own bytes, which is what was signed. */
    val payload: ByteArray,
    /** The same signature in its detached form, so a cache stores and re-verifies one shape. */
    val detachedSignature: String,
) {
    companion object {
        /**
         * The screen inside [raw], or null when [raw] is not an envelope at all.
         *
         * Recognised without parsing a screen: an envelope is a flat object, and a screen never is,
         * since its root is an object of its own. That check stops at the first nested bracket, so
         * an ordinary screen costs a few bytes of scanning and is never parsed twice.
         *
         * @throws HeimMalformedEnvelopeException when [raw] is an envelope that cannot be opened.
         */
        fun unwrapOrNull(raw: ByteArray): HeimSignedEnvelope? {
            if (!isFlatJsonObject(raw)) return null
            val envelope = runCatching { Json.parseToJsonElement(raw.decodeToString()) as? JsonObject }
                .getOrNull() ?: return null
            val protectedHeader = envelope.string("protected")
            val payload = envelope.string("payload")
            val signature = envelope.string("signature")
            if (protectedHeader == null || payload == null || signature == null) return null

            // Strictly canonical base64url. A padded or otherwise re-encodable payload decodes to the
            // same screen but is not the string that was signed, and verifying the re-encoding would
            // mean verifying something the signer never saw.
            val bytes = HeimBase64.decodeUrl(payload)
                ?: throw HeimMalformedEnvelopeException("The signed screen's payload is not canonical base64url.")
            return HeimSignedEnvelope(bytes, "$protectedHeader..$signature")
        }

        private fun JsonObject.string(name: String): String? =
            (get(name) as? JsonPrimitive)?.takeIf { it.isString }?.content

        /** Whether [raw] is one JSON object with nothing nested in it, looked at byte by byte. */
        private fun isFlatJsonObject(raw: ByteArray): Boolean {
            var index = 0
            while (index < raw.size && raw[index].isJsonWhitespace()) index++
            if (index == raw.size || raw[index] != '{'.code.toByte()) return false
            var depth = 0
            var inString = false
            var escaped = false
            while (index < raw.size) {
                val byte = raw[index++]
                when {
                    escaped -> escaped = false
                    inString && byte == '\\'.code.toByte() -> escaped = true
                    byte == '"'.code.toByte() -> inString = !inString
                    inString -> Unit
                    byte == '{'.code.toByte() || byte == '['.code.toByte() -> if (++depth > 1) return false
                    byte == '}'.code.toByte() || byte == ']'.code.toByte() -> depth--
                }
            }
            return depth == 0
        }

        private fun Byte.isJsonWhitespace(): Boolean =
            this == ' '.code.toByte() || this == '\n'.code.toByte() || this == '\r'.code.toByte() || this == '\t'.code.toByte()
    }
}

/** A response that is a signed envelope but cannot be opened. Never a screen to render. */
internal class HeimMalformedEnvelopeException(message: String) : IllegalStateException(message)

private const val ES256_SIGNATURE_SIZE = 64

/**
 * A JWS ES256 signature (`r || s`, 32 bytes each) as the ASN.1 DER both platforms expect.
 *
 * @return null for a value that cannot be a signature: the wrong length, or an `r` or `s` of zero.
 */
internal fun es256RawToDer(raw: ByteArray): ByteArray? {
    if (raw.size != ES256_SIGNATURE_SIZE) return null
    val r = derInteger(raw, 0) ?: return null
    val s = derInteger(raw, 32) ?: return null
    // At most 2 + 33 bytes each, so the sequence length always fits the short form.
    return byteArrayOf(0x30, (r.size + s.size).toByte()) + r + s
}

private fun derInteger(raw: ByteArray, offset: Int): ByteArray? {
    val end = offset + 32
    var start = offset
    while (start < end && raw[start] == 0.toByte()) start++
    if (start == end) return null
    val magnitude = raw.copyOfRange(start, end)
    // DER integers are signed: a leading byte with its top bit set needs a zero in front of it.
    val body = if (magnitude[0].toInt() and 0x80 != 0) byteArrayOf(0) + magnitude else magnitude
    return byteArrayOf(0x02, body.size.toByte()) + body
}

/** Base64 without a dependency, strict where a signature needs it to be. */
internal object HeimBase64 {
    private const val URL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    private const val STANDARD = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    private val URL_VALUES = valuesOf(URL)
    private val STANDARD_VALUES = valuesOf(STANDARD)

    private fun valuesOf(alphabet: String): IntArray =
        IntArray(128) { -1 }.also { table -> alphabet.forEachIndexed { value, char -> table[char.code] = value } }

    /** Unpadded base64url, the encoding every part of a JWS uses. */
    fun encodeUrl(bytes: ByteArray): String {
        val out = StringBuilder((bytes.size * 4 + 2) / 3)
        var i = 0
        while (i + 3 <= bytes.size) {
            val n = (bytes[i].toInt() and 0xFF shl 16) or (bytes[i + 1].toInt() and 0xFF shl 8) or (bytes[i + 2].toInt() and 0xFF)
            out.append(URL[n ushr 18 and 63]).append(URL[n ushr 12 and 63]).append(URL[n ushr 6 and 63]).append(URL[n and 63])
            i += 3
        }
        when (bytes.size - i) {
            1 -> {
                val n = bytes[i].toInt() and 0xFF shl 16
                out.append(URL[n ushr 18 and 63]).append(URL[n ushr 12 and 63])
            }
            2 -> {
                val n = (bytes[i].toInt() and 0xFF shl 16) or (bytes[i + 1].toInt() and 0xFF shl 8)
                out.append(URL[n ushr 18 and 63]).append(URL[n ushr 12 and 63]).append(URL[n ushr 6 and 63])
            }
        }
        return out.toString()
    }

    /**
     * Unpadded, canonical base64url, or null.
     *
     * Canonical means the unused low bits of the last character are zero, so exactly one string
     * decodes to any given bytes. Without that, two different payload strings would verify as the
     * same signed content.
     */
    fun decodeUrl(text: String): ByteArray? = decode(text, URL_VALUES)

    /** The body of a PEM block: standard alphabet, padding and whitespace already removed. */
    fun decodeStandard(text: String): ByteArray? = decode(text, STANDARD_VALUES)

    private fun decode(text: String, values: IntArray): ByteArray? {
        if (text.isEmpty() || text.length % 4 == 1) return null
        val out = ByteArray(text.length * 3 / 4)
        var buffer = 0
        var bits = 0
        var index = 0
        for (char in text) {
            val value = if (char.code < 128) values[char.code] else -1
            if (value < 0) return null
            buffer = ((buffer shl 6) or value) and 0xFFFF
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out[index++] = (buffer ushr bits and 0xFF).toByte()
            }
        }
        if (bits > 0 && buffer and ((1 shl bits) - 1) != 0) return null
        return out
    }
}
