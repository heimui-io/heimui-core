package io.heimui.core

import io.heimui.core.data.security.DefaultHeimSignatureVerifier
import io.heimui.core.data.security.HmacSha256
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HeimSignatureVerifierTest {

    @Test
    fun `test real cryptographic HMAC-SHA256 signature verification`() {
        val verifier = DefaultHeimSignatureVerifier()
        val key = "super-secret-enterprise-key"
        val payload = "{\"id\":\"screen_1\",\"title\":\"Checkout\"}"
        val validSignature = HmacSha256.computeHex(payload.encodeToByteArray(), key.encodeToByteArray())

        // 1. Valid signature passes
        assertTrue(verifier.verify(payload = payload, signature = validSignature, publicKey = key))

        // 2. Tampered payload with valid signature of original FAILS
        val tamperedPayload = "{\"id\":\"screen_1\",\"title\":\"Hacked Checkout\"}"
        assertFalse(verifier.verify(payload = tamperedPayload, signature = validSignature, publicKey = key))

        // 3. Fake 16-character 'AAAAAAAAAAAAAAAA' bypass FAILS
        assertFalse(verifier.verify(payload = payload, signature = "AAAAAAAAAAAAAAAA", publicKey = key))

        // 4. Null / blank signature or key FAILS
        assertFalse(verifier.verify(payload = payload, signature = null, publicKey = key))
        assertFalse(verifier.verify(payload = payload, signature = validSignature, publicKey = null))
        assertFalse(verifier.verify(payload = payload, signature = validSignature, publicKey = "wrong-key"))
    }
}
