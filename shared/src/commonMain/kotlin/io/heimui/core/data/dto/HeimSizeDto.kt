package io.heimui.core.data.dto

import io.heimui.core.domain.model.component.HeimSize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Reads a component's `size` object, tolerating numbers sent as strings.
 *
 * Anything unparseable decodes to [HeimSize.None] rather than throwing: a malformed size is a
 * cosmetic defect, and refusing the whole screen over one would turn it into an outage.
 */
internal object HeimSizeSerializer : KSerializer<HeimSize> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("HeimSize")

    override fun deserialize(decoder: Decoder): HeimSize {
        val jsonDecoder = decoder as? JsonDecoder ?: return HeimSize.None
        val obj = jsonDecoder.decodeJsonElement() as? JsonObject ?: return HeimSize.None

        fun int(name: String): Int? = (obj[name] as? JsonPrimitive)?.let {
            it.content.toIntOrNull() ?: it.content.toDoubleOrNull()?.toInt()
        }
        fun float(name: String): Float? = (obj[name] as? JsonPrimitive)?.content?.toFloatOrNull()

        return HeimSize(
            width = int("width"),
            height = int("height"),
            minWidth = int("min_width"),
            minHeight = int("min_height"),
            aspectRatio = float("aspect_ratio"),
        )
    }

    override fun serialize(encoder: Encoder, value: HeimSize) {
        val jsonEncoder = encoder as? JsonEncoder ?: error("HeimSize requires a JSON encoder")
        jsonEncoder.encodeJsonElement(
            buildJsonObject {
                // Only what is set, so a round trip does not inflate the payload with nulls.
                value.width?.let { put("width", JsonPrimitive(it)) }
                value.height?.let { put("height", JsonPrimitive(it)) }
                value.minWidth?.let { put("min_width", JsonPrimitive(it)) }
                value.minHeight?.let { put("min_height", JsonPrimitive(it)) }
                value.aspectRatio?.let { put("aspect_ratio", JsonPrimitive(it)) }
            }
        )
    }
}

/** Serializable alias so DTO fields can declare `@Serializable(with = …)` consistently. */
internal typealias SizeSpec = @Serializable(with = HeimSizeSerializer::class) HeimSize
