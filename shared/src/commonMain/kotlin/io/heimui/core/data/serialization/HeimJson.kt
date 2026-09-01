package io.heimui.core.data.serialization

import io.heimui.core.data.dto.HeimActionDto
import io.heimui.core.data.dto.HeimComponentDto
import io.heimui.core.data.dto.HeimScreenResponseDto
import io.heimui.core.data.dto.UnknownActionDto
import io.heimui.core.data.dto.UnknownComponentDto
import io.heimui.core.data.security.HeimPayloadGuard
import io.heimui.core.data.security.HeimPayloadRejectedException
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * Single source of truth for JSON handling across the SDK.
 *
 * Every decode of untrusted input must go through [decodeScreen] rather than calling
 * [Json.decodeFromString] directly, so that the structural guard runs before the parser does.
 */
internal object HeimJson {

    /**
     * Unknown discriminators fall back to the Unknown* DTOs *and keep the type name they arrived
     * with*. Without capturing it, telemetry can only report "something unknown rendered", which
     * is not enough for a backend team to find the component they shipped too early.
     */
    private class TypeCapturingDeserializer<T : Any>(
        private val delegate: DeserializationStrategy<T>,
        private val originalType: String?,
        private val withType: (T, String?) -> T
    ) : DeserializationStrategy<T> {
        override val descriptor: SerialDescriptor = delegate.descriptor
        override fun deserialize(decoder: Decoder): T =
            withType(delegate.deserialize(decoder), originalType)
    }

    val serializersModule: SerializersModule = SerializersModule {
        polymorphic(HeimComponentDto::class) {
            defaultDeserializer { type ->
                TypeCapturingDeserializer(UnknownComponentDto.serializer(), type) { dto, t ->
                    dto.copy(originalType = t)
                }
            }
        }
        polymorphic(HeimActionDto::class) {
            defaultDeserializer { type ->
                TypeCapturingDeserializer(UnknownActionDto.serializer(), type) { dto, t ->
                    dto.copy(originalType = t)
                }
            }
        }
    }

    /**
     * Strict-but-tolerant configuration.
     *
     * - `isLenient = false`: malformed JSON is rejected rather than silently reinterpreted.
     *   Leniency widens the attack surface without buying forward compatibility.
     * - `coerceInputValues = true`: an unknown enum constant or an explicit null in a
     *   non-nullable field falls back to the declared default instead of failing the screen.
     * - `ignoreUnknownKeys = true`: newer servers may add fields older clients do not know.
     */
    val instance: Json = Json {
        ignoreUnknownKeys = true
        isLenient = false
        encodeDefaults = true
        coerceInputValues = true
        serializersModule = HeimJson.serializersModule
    }

    /**
     * Decodes a screen payload after enforcing [HeimPayloadGuard].
     *
     * @throws HeimPayloadRejectedException if the payload is too large or too deeply nested.
     *   This is thrown *before* the parser touches the input, which is what keeps a hostile
     *   payload from crashing the process on Kotlin/Native.
     */
    fun decodeScreen(raw: String): HeimScreenResponseDto {
        when (val verdict = HeimPayloadGuard.inspect(raw)) {
            is HeimPayloadGuard.Verdict.Rejected -> throw HeimPayloadRejectedException(verdict.reason)
            is HeimPayloadGuard.Verdict.Accepted -> Unit
        }
        return instance.decodeFromString(HeimScreenResponseDto.serializer(), raw)
    }

    /** Null-returning variant for cache and bundle reads, where a bad entry is not fatal. */
    fun decodeScreenOrNull(raw: String): HeimScreenResponseDto? = try {
        decodeScreen(raw)
    } catch (_: Throwable) {
        null
    }
}
