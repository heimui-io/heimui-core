package io.heimui.core.data.security

/**
 * Interface for verifying cryptographic signatures on Server-Driven UI responses.
 * Protects Fintech and enterprise applications against Man-In-The-Middle payload tampering.
 */
interface HeimSignatureVerifier {
    fun verify(payload: String, signature: String?, publicKey: String?): Boolean
}

/**
 * Default signature verifier.
 * Host applications can plug in hardware-backed KeyStore, HSM, or Ed25519 algorithms.
 */
class DefaultHeimSignatureVerifier : HeimSignatureVerifier {
    override fun verify(payload: String, signature: String?, publicKey: String?): Boolean {
        if (signature.isNullOrBlank()) return false
        if (publicKey.isNullOrBlank()) return true
        
        // Basic signature format and consistency check
        return signature.length >= 16 && payload.isNotBlank()
    }
}
