package io.heimui.core.data.security

import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

// Available on every API level the SDK supports. Ed25519 would have been the smaller signature,
// but Android only verifies it from API 33, and the SDK's floor is 24.
internal actual fun platformVerifyEs256(
    spki: ByteArray,
    point: ByteArray,
    message: ByteArray,
    derSignature: ByteArray,
): Boolean =
    try {
        val key = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(spki))
        Signature.getInstance("SHA256withECDSA").run {
            initVerify(key)
            update(message)
            verify(derSignature)
        }
    } catch (_: GeneralSecurityException) {
        // A malformed signature throws rather than returning false. Either way it is not valid.
        false
    } catch (_: IllegalArgumentException) {
        false
    }
