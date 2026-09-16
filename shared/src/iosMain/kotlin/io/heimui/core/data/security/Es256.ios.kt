package io.heimui.core.data.security

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Security.SecKeyCreateWithData
import platform.Security.SecKeyVerifySignature
import platform.Security.kSecAttrKeyClass
import platform.Security.kSecAttrKeyClassPublic
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeECSECPrimeRandom
import platform.Security.kSecKeyAlgorithmECDSASignatureMessageX962SHA256

// The Security framework rather than CryptoKit: CryptoKit is Swift-only and cannot be called from
// Kotlin/Native. SecKey takes the key as an uncompressed point and the signature as X9.62 DER.
@OptIn(ExperimentalForeignApi::class)
internal actual fun platformVerifyEs256(
    spki: ByteArray,
    point: ByteArray,
    message: ByteArray,
    derSignature: ByteArray,
): Boolean {
    val keyData = point.toCFData()
    val messageData = message.toCFData()
    val signatureData = derSignature.toCFData()
    val attributes = CFDictionaryCreateMutable(null, 2, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
    try {
        if (keyData == null || messageData == null || signatureData == null || attributes == null) return false
        CFDictionaryAddValue(attributes, kSecAttrKeyType, kSecAttrKeyTypeECSECPrimeRandom)
        CFDictionaryAddValue(attributes, kSecAttrKeyClass, kSecAttrKeyClassPublic)
        val key = SecKeyCreateWithData(keyData, attributes, null) ?: return false
        try {
            return SecKeyVerifySignature(
                key,
                kSecKeyAlgorithmECDSASignatureMessageX962SHA256,
                messageData,
                signatureData,
                null,
            )
        } finally {
            CFRelease(key)
        }
    } finally {
        keyData?.let { CFRelease(it) }
        messageData?.let { CFRelease(it) }
        signatureData?.let { CFRelease(it) }
        attributes?.let { CFRelease(it) }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toCFData(): CFDataRef? =
    if (isEmpty()) {
        CFDataCreate(null, null, 0)
    } else {
        usePinned { CFDataCreate(null, it.addressOf(0).reinterpret(), size.toLong()) }
    }
