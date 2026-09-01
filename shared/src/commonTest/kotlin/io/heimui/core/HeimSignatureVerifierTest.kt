package io.heimui.core

import io.heimui.core.data.security.DefaultHeimSignatureVerifier
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HeimSignatureVerifierTest {

    @Test
    fun `test default signature verifier checks validity`() {
        val verifier = DefaultHeimSignatureVerifier()

        // Valid signature
        assertTrue(verifier.verify(payload = "home_screen_payload", signature = "ed25519_sig_valid_123456789", publicKey = "pub_key_123"))

        // Null signature
        assertFalse(verifier.verify(payload = "home_screen_payload", signature = null, publicKey = "pub_key_123"))

        // Short / blank signature
        assertFalse(verifier.verify(payload = "home_screen_payload", signature = "short", publicKey = "pub_key_123"))
    }
}
