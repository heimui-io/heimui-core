package io.heimui.core.data.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Number serializers that tolerate the single most common backend bug in SDUI payloads: a numeric
 * field emitted as a string (`"padding": "16"`), or as garbage (`"padding": "sixteen"`).
 *
 * Without these, one mistyped field aborts the deserialization of the entire screen. Degrading the
 * field is strictly better than losing the screen, and the mapper's sanitisation clamps whatever
 * comes out.
 */
internal object LenientIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LenientInt", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int) = encoder.encodeInt(value)

    override fun deserialize(decoder: Decoder): Int {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeInt()
        val primitive = jsonDecoder.decodeJsonElement().jsonPrimitive
        return primitive.content.toIntOrNull()
            ?: primitive.content.toDoubleOrNull()?.toInt()
            ?: 0
    }
}

internal object LenientNullableIntSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LenientNullableInt", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int?) {
        if (value == null) encoder.encodeNull() else encoder.encodeInt(value)
    }

    override fun deserialize(decoder: Decoder): Int? {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeInt()
        val element = jsonDecoder.decodeJsonElement()
        val primitive = element as? JsonPrimitive ?: return null
        if (primitive.content == "null") return null
        return primitive.content.toIntOrNull() ?: primitive.content.toDoubleOrNull()?.toInt()
    }
}

internal object LenientNullableFloatSerializer : KSerializer<Float?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LenientNullableFloat", PrimitiveKind.FLOAT)

    override fun serialize(encoder: Encoder, value: Float?) {
        if (value == null) encoder.encodeNull() else encoder.encodeFloat(value)
    }

    override fun deserialize(decoder: Decoder): Float? {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeFloat()
        val element = jsonDecoder.decodeJsonElement()
        val primitive = element as? JsonPrimitive ?: return null
        if (primitive.content == "null") return null
        return primitive.content.toFloatOrNull()
    }
}

internal object LenientBooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LenientBoolean", PrimitiveKind.BOOLEAN)

    override fun serialize(encoder: Encoder, value: Boolean) = encoder.encodeBoolean(value)

    override fun deserialize(decoder: Decoder): Boolean {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeBoolean()
        val content = jsonDecoder.decodeJsonElement().jsonPrimitive.content
        return content.equals("true", ignoreCase = true) || content == "1"
    }
}
