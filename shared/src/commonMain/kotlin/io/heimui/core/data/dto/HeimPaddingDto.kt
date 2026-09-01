package io.heimui.core.data.dto

import io.heimui.core.domain.model.component.HeimPadding
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
 * Accepts `"padding": 16` and `"padding": { "top": 8, "horizontal": 16 }` alike.
 *
 * Written by hand rather than declared as a sealed hierarchy because the number form has to keep
 * working: every payload in the wild uses it, and a schema change that invalidates existing
 * content is not a schema change a server-driven system can afford.
 *
 * Anything unparseable decodes to [HeimPadding.None] rather than throwing. A malformed padding is
 * a cosmetic defect; refusing the whole screen over one would turn it into an outage.
 */
internal object HeimPaddingSerializer : KSerializer<HeimPadding> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("HeimPadding")

    override fun deserialize(decoder: Decoder): HeimPadding {
        val jsonDecoder = decoder as? JsonDecoder ?: return HeimPadding.None
        val element = jsonDecoder.decodeJsonElement()

        (element as? JsonPrimitive)?.let { primitive ->
            val value = primitive.content.toIntOrNull()
                ?: primitive.content.toDoubleOrNull()?.toInt()
                ?: return HeimPadding.None
            return HeimPadding.all(value)
        }

        val obj = element as? JsonObject ?: return HeimPadding.None
        fun side(name: String): Int? =
            (obj[name] as? JsonPrimitive)?.let {
                it.content.toIntOrNull() ?: it.content.toDoubleOrNull()?.toInt()
            }

        // Widest shorthand first, narrowest key last: an explicit side wins over `horizontal`,
        // which wins over `all`, regardless of the order the keys happen to appear in the JSON.
        val all = side("all")
        val horizontal = side("horizontal") ?: all
        val vertical = side("vertical") ?: all
        return HeimPadding(
            start = side("start") ?: horizontal ?: 0,
            top = side("top") ?: vertical ?: 0,
            end = side("end") ?: horizontal ?: 0,
            bottom = side("bottom") ?: vertical ?: 0,
        )
    }

    override fun serialize(encoder: Encoder, value: HeimPadding) {
        val jsonEncoder = encoder as? JsonEncoder ?: error("HeimPadding requires a JSON encoder")
        val uniform = value.start == value.top && value.top == value.end && value.end == value.bottom
        jsonEncoder.encodeJsonElement(
            if (uniform) {
                JsonPrimitive(value.start)
            } else {
                buildJsonObject {
                    // Only the sides that do anything, so a round trip does not inflate the payload.
                    if (value.start != 0) put("start", JsonPrimitive(value.start))
                    if (value.top != 0) put("top", JsonPrimitive(value.top))
                    if (value.end != 0) put("end", JsonPrimitive(value.end))
                    if (value.bottom != 0) put("bottom", JsonPrimitive(value.bottom))
                }
            }
        )
    }
}

/** Serializable alias so DTO fields can declare `@Serializable(with = …)` consistently. */
internal typealias PaddingSpec = @Serializable(with = HeimPaddingSerializer::class) HeimPadding
