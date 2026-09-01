package io.heimui.core.data.security

/**
 * Pre-parse structural guard for untrusted SDUI payloads.
 *
 * The recursive-descent parser in kotlinx.serialization consumes one native stack frame per
 * nesting level. On Kotlin/Native a stack overflow is a **SIGSEGV, not a catchable
 * StackOverflowError**, so a depth limit enforced inside the DTO -> domain mapper runs far too
 * late: the process is already dead by the time the mapper would see the tree.
 *
 * This guard therefore scans the raw characters *before* any deserialization happens. It is a
 * single linear pass with no allocation and no recursion, so it is safe to run on any payload
 * regardless of how hostile its shape is.
 */
internal object HeimPayloadGuard {

    /** Maximum `{`/`[` nesting accepted from the wire. Deeper payloads are rejected outright. */
    const val MAX_NESTING_DEPTH: Int = 64

    /** Maximum raw payload size in bytes. */
    const val MAX_PAYLOAD_BYTES: Int = 5 * 1024 * 1024

    sealed interface Verdict {
        data object Accepted : Verdict
        data class Rejected(val reason: String) : Verdict
    }

    /**
     * Validates size and structural depth of [raw] without deserializing it.
     *
     * String literals are skipped so that braces appearing inside text values (for example a
     * label containing `"{{"`) never inflate the measured depth, and backslash escapes are
     * honoured so a trailing `\"` cannot desynchronise the scanner.
     */
    fun inspect(
        raw: String,
        maxDepth: Int = MAX_NESTING_DEPTH,
        maxBytes: Int = MAX_PAYLOAD_BYTES
    ): Verdict {
        if (raw.length > maxBytes) {
            return Verdict.Rejected("Payload exceeds maximum allowed size of $maxBytes bytes")
        }

        var depth = 0
        var insideString = false
        var escaped = false

        for (char in raw) {
            if (insideString) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> insideString = false
                }
                continue
            }
            when (char) {
                '"' -> insideString = true
                '{', '[' -> {
                    depth++
                    if (depth > maxDepth) {
                        return Verdict.Rejected(
                            "Payload nesting exceeds maximum depth of $maxDepth"
                        )
                    }
                }
                '}', ']' -> depth--
            }
        }

        return Verdict.Accepted
    }

    /** Convenience predicate for call sites that only need a yes/no answer. */
    fun isSafe(raw: String): Boolean = inspect(raw) is Verdict.Accepted
}

/**
 * Thrown when an untrusted payload fails [HeimPayloadGuard] inspection.
 * Callers convert this into a domain-level error; it never escapes the data layer.
 */
internal class HeimPayloadRejectedException(message: String) : IllegalArgumentException(message)
