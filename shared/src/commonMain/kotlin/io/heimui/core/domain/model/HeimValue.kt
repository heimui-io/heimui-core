package io.heimui.core.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Type-safe, sealed value model for dynamic SDUI payloads, metadata, and custom component
 * parameters. Eliminates platform interop issues with Swift and prevents SerializationExceptions
 * in mixed payloads.
 */
@Serializable(with = HeimValueSerializer::class)
public sealed interface HeimValue {

    /**
     * Human/wire representation of the value.
     *
     * Integral numbers render without a decimal tail, matching [toJsonElement]. Returning
     * "500.0" here while the wire carries `500` would surface as a formatting bug the moment a
     * value is interpolated into a label.
     */
    public val asString: String?
        get() = when (this) {
            is Str -> value
            is Int64 -> value.toString()
            is Num -> if (value.isIntegral()) value.toLong().toString() else value.toString()
            is Bool -> value.toString()
            else -> null
        }

    public val asDouble: Double?
        get() = when (this) {
            is Num -> value
            is Int64 -> value.toDouble()
            is Str -> value.toDoubleOrNull()
            else -> null
        }

    public val asLong: Long?
        get() = when (this) {
            is Int64 -> value
            is Num -> if (value.isIntegral()) value.toLong() else null
            is Str -> value.toLongOrNull()
            else -> null
        }

    public val asBoolean: Boolean?
        get() = when (this) {
            is Bool -> value
            is Str -> value.toBooleanStrictOrNull()
            else -> null
        }

    public data class Str(val value: String) : HeimValue

    /**
     * Exact integer. Kept separate from [Num] because `Double` silently loses precision above
     * 2^53: an account number or a large id would round-trip to a different number.
     */
    public data class Int64(val value: Long) : HeimValue

    /** Fractional number. */
    public data class Num(val value: Double) : HeimValue

    public data class Bool(val value: Boolean) : HeimValue
    public data class Arr(val items: List<HeimValue>) : HeimValue
    public data class Obj(val fields: Map<String, HeimValue>) : HeimValue
    public data object Null : HeimValue

    public companion object {
        /** Lossy convenience bridge for host code. Unknown types fall back to `toString()`. */
        public fun from(value: Any?): HeimValue = when (value) {
            null -> Null
            is HeimValue -> value
            is String -> Str(value)
            is Long, is Int, is Short, is Byte -> Int64((value as Number).toLong())
            is Number -> Num(value.toDouble())
            is Boolean -> Bool(value)
            is List<*> -> Arr(value.map { from(it) })
            is Map<*, *> -> Obj(value.entries.associate { it.key.toString() to from(it.value) })
            else -> Str(value.toString())
        }

        public fun number(value: Long): HeimValue = Int64(value)
        public fun number(value: Double): HeimValue =
            if (value.isIntegral()) Int64(value.toLong()) else Num(value)
    }
}

private fun Double.isIntegral(): Boolean =
    !isNaN() && !isInfinite() && this % 1.0 == 0.0 &&
        this >= -9.007199254740992E15 && this <= 9.007199254740992E15

public fun JsonElement.toHeimValue(): HeimValue = when (this) {
    is JsonNull -> HeimValue.Null
    is JsonPrimitive -> when {
        isString -> HeimValue.Str(content)
        booleanOrNull != null -> HeimValue.Bool(booleanOrNull!!)
        // Long is tried BEFORE Double. The reverse order routed every integer through Double and
        // corrupted anything above 2^53 (9007199254740993 came back as ...992).
        longOrNull != null -> HeimValue.Int64(longOrNull!!)
        doubleOrNull != null -> HeimValue.Num(doubleOrNull!!)
        else -> HeimValue.Str(content)
    }
    is JsonArray -> HeimValue.Arr(map { it.toHeimValue() })
    is JsonObject -> HeimValue.Obj(mapValues { it.value.toHeimValue() })
}

public fun HeimValue.toJsonElement(): JsonElement = when (this) {
    is HeimValue.Null -> JsonNull
    is HeimValue.Str -> JsonPrimitive(value)
    is HeimValue.Int64 -> JsonPrimitive(value)
    is HeimValue.Num -> if (value.isIntegral()) JsonPrimitive(value.toLong()) else JsonPrimitive(value)
    is HeimValue.Bool -> JsonPrimitive(value)
    is HeimValue.Arr -> JsonArray(items.map { it.toJsonElement() })
    is HeimValue.Obj -> JsonObject(fields.mapValues { it.value.toJsonElement() })
}

public object HeimValueSerializer : KSerializer<HeimValue> {
    /**
     * Delegates to [JsonElement]'s descriptor rather than declaring an empty class descriptor:
     * this serializer encodes arbitrary JSON, and a `buildClassSerialDescriptor` with zero
     * elements misdescribes that to anything inspecting the schema.
     */
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun serialize(encoder: Encoder, value: HeimValue) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: error("HeimValueSerializer requires a JSON format")
        jsonEncoder.encodeJsonElement(value.toJsonElement())
    }

    override fun deserialize(decoder: Decoder): HeimValue {
        val jsonDecoder = decoder as? JsonDecoder
            ?: error("HeimValueSerializer requires a JSON format")
        return jsonDecoder.decodeJsonElement().toHeimValue()
    }
}
