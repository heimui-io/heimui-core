package io.heimui.core.data.security

/**
 * Interface for verifying cryptographic signatures on Server-Driven UI responses.
 * Protects Fintech and enterprise applications against Man-In-The-Middle payload tampering.
 */
public interface HeimSignatureVerifier {
    /**
     * Verifies that [payloadBytes] matches [signature] using the configured [publicKey] or secret.
     */
    public fun verify(payloadBytes: ByteArray, signature: String?, publicKey: String?): Boolean

    /**
     * Helper overload for string payloads.
     */
    public fun verify(payload: String, signature: String?, publicKey: String?): Boolean {
        return verify(payload.encodeToByteArray(), signature, publicKey)
    }
}

/**
 * Constant-time comparison to prevent timing attacks.
 */
public fun constantTimeEquals(a: String, b: String): Boolean {
    if (a.length != b.length) return false
    var result = 0
    for (i in a.indices) {
        result = result or (a[i].code xor b[i].code)
    }
    return result == 0
}

/**
 * Legacy verifier: HMAC-SHA256 over the payload bytes, compared with a hex digest.
 *
 * Kept so an app already configured with `HeimConfig.publicKey` goes on working. Do not start with
 * it. The secret it checks against is the same secret that signs, and it has to be on the device to
 * be checked there, so extracting it from one installation is enough to sign screens every
 * installation accepts. [Es256SignatureVerifier] closes that: the app holds only public keys.
 */
public class HmacSha256SignatureVerifier : HeimSignatureVerifier {
    override fun verify(payloadBytes: ByteArray, signature: String?, publicKey: String?): Boolean {
        if (signature.isNullOrBlank() || publicKey.isNullOrBlank() || payloadBytes.isEmpty()) {
            return false
        }
        val expectedSignature = computeHmacSha256Hex(payloadBytes, publicKey)
        return constantTimeEquals(signature.lowercase().trim(), expectedSignature.lowercase().trim())
    }

    private fun computeHmacSha256Hex(data: ByteArray, key: String): String {
        return HmacSha256.computeHex(data, key.encodeToByteArray())
    }
}

/**
 * Pure Kotlin SHA-256 and HMAC-SHA256 implementation for zero external dependencies and 100% KMP compatibility.
 */
public object HmacSha256 {
    private const val BLOCK_SIZE = 64

    public fun computeHex(data: ByteArray, key: ByteArray): String {
        val macBytes = compute(data, key)
        val hexChars = "0123456789abcdef"
        val result = StringBuilder(macBytes.size * 2)
        for (b in macBytes) {
            val i = b.toInt() and 0xFF
            result.append(hexChars[i ushr 4])
            result.append(hexChars[i and 0x0F])
        }
        return result.toString()
    }

    public fun compute(data: ByteArray, key: ByteArray): ByteArray {
        var formattedKey = key
        if (formattedKey.size > BLOCK_SIZE) {
            formattedKey = Sha256.hash(formattedKey)
        }
        if (formattedKey.size < BLOCK_SIZE) {
            val padded = ByteArray(BLOCK_SIZE)
            formattedKey.copyInto(padded)
            formattedKey = padded
        }

        val oKeyPad = ByteArray(BLOCK_SIZE) { i -> (formattedKey[i].toInt() xor 0x5c).toByte() }
        val iKeyPad = ByteArray(BLOCK_SIZE) { i -> (formattedKey[i].toInt() xor 0x36).toByte() }

        val innerHash = Sha256.hash(iKeyPad + data)
        return Sha256.hash(oKeyPad + innerHash)
    }
}

/**
 * Minimal pure Kotlin SHA-256 implementation.
 */
public object Sha256 {
    private val K = intArrayOf(
        0x428a2f98.toInt(), 0x71374491.toInt(), 0xb5c0fbcf.toInt(), 0xe9b5dba5.toInt(),
        0x3956c25b, 0x59f111f1, 0x923f82a4.toInt(), 0xab1c5ed5.toInt(),
        0xd807aa98.toInt(), 0x12835b01, 0x243185be, 0x550c7dc3,
        0x72be5d74, 0x80deb1fe.toInt(), 0x9bdc06a7.toInt(), 0xc19bf174.toInt(),
        0xe49b69c1.toInt(), 0xefbe4786.toInt(), 0x0fc19dc6, 0x240ca1cc,
        0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152.toInt(), 0xa831c66d.toInt(), 0xb00327c8.toInt(), 0xbf597fc7.toInt(),
        0xc6e00bf3.toInt(), 0xd5a79147.toInt(), 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
        0x650a7354, 0x766a0abb, 0x81c2c92e.toInt(), 0x92722c85.toInt(),
        0xa2bfe8a1.toInt(), 0xa81a664b.toInt(), 0xc24b8b70.toInt(), 0xc76c51a3.toInt(),
        0xd192e819.toInt(), 0xd6990624.toInt(), 0xf40e3585.toInt(), 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
        0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814.toInt(), 0x8cc70208.toInt(),
        0x90befffa.toInt(), 0xa4506ceb.toInt(), 0xbef9a3f7.toInt(), 0xc67178f2.toInt()
    )

    public fun hash(input: ByteArray): ByteArray {
        val originalBitLength = input.size.toLong() * 8
        val paddingLength = ((56 - (input.size + 1) % 64 + 64) % 64)
        val totalLength = input.size + 1 + paddingLength + 8
        val padded = ByteArray(totalLength)
        input.copyInto(padded)
        padded[input.size] = 0x80.toByte()

        for (i in 0..7) {
            padded[totalLength - 1 - i] = ((originalBitLength ushr (i * 8)) and 0xFF).toByte()
        }

        var h0 = 0x6a09e667
        var h1 = 0xbb67ae85.toInt()
        var h2 = 0x3c6ef372
        var h3 = 0xa54ff53a.toInt()
        var h4 = 0x510e527f
        var h5 = 0x9b05688c.toInt()
        var h6 = 0x1f83d9ab
        var h7 = 0x5be0cd19

        val w = IntArray(64)
        for (chunk in padded.indices step 64) {
            for (i in 0..15) {
                w[i] = ((padded[chunk + i * 4].toInt() and 0xFF) shl 24) or
                        ((padded[chunk + i * 4 + 1].toInt() and 0xFF) shl 16) or
                        ((padded[chunk + i * 4 + 2].toInt() and 0xFF) shl 8) or
                        (padded[chunk + i * 4 + 3].toInt() and 0xFF)
            }
            for (i in 16..63) {
                val s0 = (w[i - 15] ushr 7 or (w[i - 15] shl 25)) xor
                        (w[i - 15] ushr 18 or (w[i - 15] shl 14)) xor
                        (w[i - 15] ushr 3)
                val s1 = (w[i - 2] ushr 17 or (w[i - 2] shl 15)) xor
                        (w[i - 2] ushr 19 or (w[i - 2] shl 13)) xor
                        (w[i - 2] ushr 10)
                w[i] = w[i - 16] + s0 + w[i - 7] + s1
            }

            var a = h0
            var b = h1
            var c = h2
            var d = h3
            var e = h4
            var f = h5
            var g = h6
            var h = h7

            for (i in 0..63) {
                val s1 = (e ushr 6 or (e shl 26)) xor (e ushr 11 or (e shl 21)) xor (e ushr 25 or (e shl 7))
                val ch = (e and f) xor (e.inv() and g)
                val temp1 = h + s1 + ch + K[i] + w[i]
                val s0 = (a ushr 2 or (a shl 30)) xor (a ushr 13 or (a shl 19)) xor (a ushr 22 or (a shl 10))
                val maj = (a and b) xor (a and c) xor (b and c)
                val temp2 = s0 + maj

                h = g
                g = f
                f = e
                e = d + temp1
                d = c
                c = b
                b = a
                a = temp1 + temp2
            }

            h0 += a
            h1 += b
            h2 += c
            h3 += d
            h4 += e
            h5 += f
            h6 += g
            h7 += h
        }

        val result = ByteArray(32)
        val hashes = intArrayOf(h0, h1, h2, h3, h4, h5, h6, h7)
        for (i in 0..7) {
            result[i * 4] = (hashes[i] ushr 24).toByte()
            result[i * 4 + 1] = (hashes[i] ushr 16).toByte()
            result[i * 4 + 2] = (hashes[i] ushr 8).toByte()
            result[i * 4 + 3] = hashes[i].toByte()
        }
        return result
    }
}

/**
 * The verifier used when a configuration names neither trusted signing keys nor a verifier of its
 * own: the legacy HMAC-SHA256 check, kept for apps configured with `HeimConfig.publicKey`.
 *
 * Setting `HeimConfig.trustedSigningKeys` selects [Es256SignatureVerifier] instead, which is the
 * one to use. See [HmacSha256SignatureVerifier] for why this one is not.
 */
public class DefaultHeimSignatureVerifier : HeimSignatureVerifier by HmacSha256SignatureVerifier()
